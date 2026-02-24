package com.hareidus.taboo.farm.modules.l3.migration

import com.hareidus.taboo.farm.foundation.database.DatabaseManager
import taboolib.common.platform.command.CommandBody
import taboolib.common.platform.command.CommandHeader
import taboolib.common.platform.command.mainCommand
import taboolib.common.platform.command.subCommand
import taboolib.common.platform.function.submit
import org.bukkit.command.CommandSender

/**
 * 数据迁移与修复命令 (L3)
 *
 * 职责：
 * 1. 数据完整性检查（孤立作物、孤立地块、位置冲突、边界越界）
 * 2. 数据修复（清理孤立记录、去重冲突位置）
 * 3. 数据库版本查询
 *
 * 依赖: DatabaseManager, PlotManager, CropManager（通过 SQL 直查）
 * 权限节点: stealfarm.admin
 */
@CommandHeader(
    name = "farmmigrate",
    permission = "stealfarm.admin",
    description = "Farm 数据迁移与修复工具"
)
object MigrationCommand {

    /** 数据完整性问题记录 */
    private data class IntegrityIssue(
        val type: IssueType,
        val description: String,
        val affectedIds: List<Long> = emptyList()
    )

    private enum class IssueType {
        ORPHAN_CROP,
        ORPHAN_PLOT,
        POSITION_CONFLICT,
        OUT_OF_BOUNDS
    }

    // ==================== 主命令 ====================

    @CommandBody
    val main = mainCommand {
        execute<CommandSender> { sender, _, _ ->
            sender.sendMessage("§6=== Farm 数据迁移工具 ===")
            sender.sendMessage("§7/farmmigrate check §8- §f检查数据完整性")
            sender.sendMessage("§7/farmmigrate fix §8- §f修复数据问题")
            sender.sendMessage("§7/farmmigrate version §8- §f显示数据库版本")
        }
    }

    // ==================== check 子命令 ====================

    @CommandBody(permission = "stealfarm.admin")
    val check = subCommand {
        execute<CommandSender> { sender, _, _ ->
            sender.sendMessage("§e[Farm] §f正在扫描数据完整性...")
            submit(async = true) {
                val issues = runIntegrityCheck()
                submit {
                    if (issues.isEmpty()) {
                        sender.sendMessage("§a[Farm] §f数据完整性检查通过，未发现问题。")
                        return@submit
                    }
                    sender.sendMessage("§6=== 数据完整性报告 ===")
                    for (issue in issues) {
                        val prefix = when (issue.type) {
                            IssueType.ORPHAN_CROP -> "§c[孤立作物]"
                            IssueType.ORPHAN_PLOT -> "§e[孤立地块]"
                            IssueType.POSITION_CONFLICT -> "§c[位置冲突]"
                            IssueType.OUT_OF_BOUNDS -> "§c[越界作物]"
                        }
                        sender.sendMessage("$prefix §f${issue.description}")
                        if (issue.affectedIds.isNotEmpty()) {
                            val idPreview = issue.affectedIds.take(10).joinToString(", ")
                            val suffix = if (issue.affectedIds.size > 10) " ..." else ""
                            sender.sendMessage("  §7ID: $idPreview$suffix")
                        }
                    }
                    sender.sendMessage("§7共 §f${issues.size} §7类问题。使用 §f/farmmigrate fix §7进行修复。")
                }
            }
        }
    }

    // ==================== fix 子命令 ====================

    @CommandBody(permission = "stealfarm.admin")
    val fix = subCommand {
        execute<CommandSender> { sender, _, _ ->
            sender.sendMessage("§e[Farm] §f正在检查数据问题...")
            submit(async = true) {
                val issues = runIntegrityCheck()
                submit {
                    if (issues.isEmpty()) {
                        sender.sendMessage("§a[Farm] §f未发现数据问题，无需修复。")
                        return@submit
                    }
                    sender.sendMessage("§e[Farm] §f发现 §c${issues.size} §f个问题，开始修复...")
                }
                val fixResults = executeFixAll(issues)
                submit {
                    for (result in fixResults) {
                        sender.sendMessage(result)
                    }
                    sender.sendMessage("§a[Farm] §f修复完成。")
                }
            }
        }
    }

    // ==================== version 子命令 ====================

    @CommandBody(permission = "stealfarm.admin")
    val version = subCommand {
        execute<CommandSender> { sender, _, _ ->
            submit(async = true) {
                val info = getDatabaseVersionInfo()
                submit {
                    for (line in info) {
                        sender.sendMessage(line)
                    }
                }
            }
        }
    }

    // ==================== 完整性检查逻辑 ====================

    private fun runIntegrityCheck(): List<IntegrityIssue> {
        val issues = mutableListOf<IntegrityIssue>()
        val ds = DatabaseManager.database.dataSource

        ds.connection.use { conn ->
            // 1. 孤立作物: crop 的 plot_id 在 plots 表中不存在
            conn.prepareStatement(
                "SELECT c.id, c.crop_type_id, c.plot_id FROM farm_crops c LEFT JOIN farm_plots p ON c.plot_id = p.id WHERE p.id IS NULL"
            ).use { stmt ->
                stmt.executeQuery().use { rs ->
                    val orphanIds = mutableListOf<Long>()
                    while (rs.next()) {
                        orphanIds.add(rs.getLong("id"))
                    }
                    if (orphanIds.isNotEmpty()) {
                        issues.add(IntegrityIssue(
                            type = IssueType.ORPHAN_CROP,
                            description = "发现 ${orphanIds.size} 条孤立作物记录（plot_id 无效）",
                            affectedIds = orphanIds
                        ))
                    }
                }
            }

            // 2. 孤立地块: plot 的 owner_uuid 在 player_data 表中不存在
            conn.prepareStatement(
                "SELECT p.id, p.owner_uuid FROM farm_plots p LEFT JOIN farm_player_data pd ON p.owner_uuid = pd.uuid WHERE pd.uuid IS NULL"
            ).use { stmt ->
                stmt.executeQuery().use { rs ->
                    val orphanIds = mutableListOf<Long>()
                    while (rs.next()) {
                        orphanIds.add(rs.getLong("id"))
                    }
                    if (orphanIds.isNotEmpty()) {
                        issues.add(IntegrityIssue(
                            type = IssueType.ORPHAN_PLOT,
                            description = "发现 ${orphanIds.size} 个孤立地块（玩家数据不存在）",
                            affectedIds = orphanIds
                        ))
                    }
                }
            }

            // 3. 作物位置冲突: 同一坐标有多条作物记录
            conn.prepareStatement(
                """SELECT world_name, x, y, z, COUNT(*) as cnt, GROUP_CONCAT(id) as ids
                   FROM farm_crops
                   GROUP BY world_name, x, y, z
                   HAVING cnt > 1""".trimIndent()
            ).use { stmt ->
                stmt.executeQuery().use { rs ->
                    val duplicateIds = mutableListOf<Long>()
                    var conflictCount = 0
                    while (rs.next()) {
                        conflictCount++
                        val idStr = rs.getString("ids")
                        val ids = idStr.split(",").mapNotNull { it.trim().toLongOrNull() }
                        // 保留最大 id（最新），其余标记为需清理
                        if (ids.size > 1) {
                            duplicateIds.addAll(ids.sorted().dropLast(1))
                        }
                    }
                    if (duplicateIds.isNotEmpty()) {
                        issues.add(IntegrityIssue(
                            type = IssueType.POSITION_CONFLICT,
                            description = "发现 $conflictCount 个坐标存在重复作物记录（共 ${duplicateIds.size} 条需清理）",
                            affectedIds = duplicateIds
                        ))
                    }
                }
            }

            // 4. 作物边界检查: 作物坐标超出所属地块边界
            conn.prepareStatement(
                """SELECT c.id, c.x, c.z, p.min_x, p.min_z, p.max_x, p.max_z
                   FROM farm_crops c
                   INNER JOIN farm_plots p ON c.plot_id = p.id
                   WHERE c.x < p.min_x OR c.x > p.max_x OR c.z < p.min_z OR c.z > p.max_z""".trimIndent()
            ).use { stmt ->
                stmt.executeQuery().use { rs ->
                    val outIds = mutableListOf<Long>()
                    while (rs.next()) {
                        outIds.add(rs.getLong("id"))
                    }
                    if (outIds.isNotEmpty()) {
                        issues.add(IntegrityIssue(
                            type = IssueType.OUT_OF_BOUNDS,
                            description = "发现 ${outIds.size} 条作物坐标超出地块边界",
                            affectedIds = outIds
                        ))
                    }
                }
            }
        }

        return issues
    }

    // ==================== 修复逻辑 ====================

    private fun executeFixAll(issues: List<IntegrityIssue>): List<String> {
        val results = mutableListOf<String>()
        val ds = DatabaseManager.database.dataSource

        ds.connection.use { conn ->
            for (issue in issues) {
                when (issue.type) {
                    IssueType.ORPHAN_CROP -> {
                        if (issue.affectedIds.isNotEmpty()) {
                            val placeholders = issue.affectedIds.joinToString(",") { "?" }
                            conn.prepareStatement("DELETE FROM farm_crops WHERE id IN ($placeholders)").use { stmt ->
                                issue.affectedIds.forEachIndexed { index, id ->
                                    stmt.setLong(index + 1, id)
                                }
                                val deleted = stmt.executeUpdate()
                                results.add("§a[修复] §f清理孤立作物: $deleted 条")
                            }
                        }
                    }
                    IssueType.ORPHAN_PLOT -> {
                        // 孤立地块仅报告，不自动删除（可能是玩家数据尚未加载）
                        results.add("§e[跳过] §f孤立地块 ${issue.affectedIds.size} 个（仅报告，需人工确认）")
                    }
                    IssueType.POSITION_CONFLICT -> {
                        if (issue.affectedIds.isNotEmpty()) {
                            val placeholders = issue.affectedIds.joinToString(",") { "?" }
                            conn.prepareStatement("DELETE FROM farm_crops WHERE id IN ($placeholders)").use { stmt ->
                                issue.affectedIds.forEachIndexed { index, id ->
                                    stmt.setLong(index + 1, id)
                                }
                                val deleted = stmt.executeUpdate()
                                results.add("§a[修复] §f清理重复位置作物: $deleted 条（保留最新记录）")
                            }
                        }
                    }
                    IssueType.OUT_OF_BOUNDS -> {
                        if (issue.affectedIds.isNotEmpty()) {
                            val placeholders = issue.affectedIds.joinToString(",") { "?" }
                            conn.prepareStatement("DELETE FROM farm_crops WHERE id IN ($placeholders)").use { stmt ->
                                issue.affectedIds.forEachIndexed { index, id ->
                                    stmt.setLong(index + 1, id)
                                }
                                val deleted = stmt.executeUpdate()
                                results.add("§a[修复] §f清理越界作物: $deleted 条")
                            }
                        }
                    }
                }
            }
        }

        return results
    }

    // ==================== 版本信息 ====================

    private fun getDatabaseVersionInfo(): List<String> {
        val lines = mutableListOf<String>()
        lines.add("§6=== Farm 数据库信息 ===")
        lines.add("§7数据库类型: §f${DatabaseManager.database.type.name}")

        val ds = DatabaseManager.database.dataSource
        ds.connection.use { conn ->
            val meta = conn.metaData
            lines.add("§7驱动: §f${meta.driverName} ${meta.driverVersion}")

            // 统计各表记录数
            val tables = listOf(
                "farm_player_data" to "玩家数据",
                "farm_plots" to "地块",
                "farm_crops" to "作物",
                "farm_friends" to "好友关系",
                "farm_enemies" to "仇人",
                "farm_steal_records" to "偷菜记录",
                "farm_deployed_traps" to "已部署陷阱",
                "farm_player_levels" to "玩家等级",
                "farm_player_achievements" to "成就进度",
                "farm_storage" to "农场仓库"
            )

            lines.add("§7--- 表记录统计 ---")
            for ((tableName, displayName) in tables) {
                try {
                    conn.prepareStatement("SELECT COUNT(*) FROM $tableName").use { stmt ->
                        stmt.executeQuery().use { rs ->
                            if (rs.next()) {
                                lines.add("§7$displayName ($tableName): §f${rs.getInt(1)} 条")
                            }
                        }
                    }
                } catch (_: Exception) {
                    lines.add("§7$displayName ($tableName): §c表不存在")
                }
            }
        }

        return lines
    }
}
