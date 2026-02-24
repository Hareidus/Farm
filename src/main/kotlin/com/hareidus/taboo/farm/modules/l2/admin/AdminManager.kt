package com.hareidus.taboo.farm.modules.l2.admin

import com.hareidus.taboo.farm.modules.l1.crop.CropManager
import com.hareidus.taboo.farm.modules.l1.farmlevel.FarmLevelManager
import com.hareidus.taboo.farm.modules.l1.playerdata.PlayerDataManager
import com.hareidus.taboo.farm.modules.l1.plot.PlotManager
import com.hareidus.taboo.farm.modules.l1.trap.TrapManager
import org.bukkit.entity.Player
import taboolib.common.platform.function.info
import taboolib.common.platform.function.warning
import taboolib.platform.util.sendLang
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * 管理员管理器 (L2)
 *
 * 职责：
 * 1. 重置玩家农场（二次确认机制）
 * 2. 设置玩家农场等级
 * 3. 重载所有模块配置
 *
 * 依赖: PlotManager, CropManager, TrapManager, FarmLevelManager, PlayerDataManager
 * 权限节点: stealfarm.admin
 */
object AdminManager {

    private const val PERMISSION = "stealfarm.admin"
    private const val CONFIRM_TIMEOUT_MS = 5000L

    /** 二次确认记录: adminUUID -> (targetUUID, timestamp) */
    private val confirmations = ConcurrentHashMap<UUID, Pair<UUID, Long>>()

    // ==================== 权限校验 ====================

    private fun hasPermission(player: Player): Boolean {
        return player.hasPermission(PERMISSION)
    }

    // ==================== 重置农场 ====================

    /**
     * 重置玩家农场（带二次确认）
     *
     * 首次调用记录确认时间戳，5秒内再次调用同一目标才执行重置。
     * 流程: 清除作物 → 清除陷阱 → 重置等级 → 重置地块 → 重置玩家数据 → 发布事件
     *
     * @param admin 执行操作的管理员
     * @param targetUUID 目标玩家 UUID
     * @param targetName 目标玩家名称（用于消息显示）
     */
    fun resetFarm(admin: Player, targetUUID: UUID, targetName: String) {
        if (!hasPermission(admin)) {
            admin.sendLang("admin-no-permission")
            return
        }

        val plot = PlotManager.getPlotByOwner(targetUUID)
        if (plot == null) {
            admin.sendLang("admin-target-no-plot", targetName)
            return
        }

        // 二次确认机制
        val now = System.currentTimeMillis()
        val pending = confirmations[admin.uniqueId]
        if (pending == null || pending.first != targetUUID || now - pending.second > CONFIRM_TIMEOUT_MS) {
            confirmations[admin.uniqueId] = targetUUID to now
            admin.sendLang("admin-reset-confirm", targetName)
            return
        }

        // 确认通过，清除确认记录
        confirmations.remove(admin.uniqueId)

        // 执行重置流程
        try {
            executeReset(admin.uniqueId, targetUUID, plot.id, targetName)
            admin.sendLang("admin-reset-success", targetName)
        } catch (e: Exception) {
            warning("[Farm] 重置农场异常 [target=$targetUUID]: ${e.message}")
            admin.sendLang("admin-reset-failed", targetName)
        }
    }

    /**
     * 执行实际的重置操作（内部方法）
     */
    private fun executeReset(adminUUID: UUID, targetUUID: UUID, plotId: Long, targetName: String) {
        // 1. 清除作物
        CropManager.removeAllCropsByPlot(plotId)
        info("[Farm] 管理员重置: 已清除地块 #$plotId 的所有作物")

        // 2. 清除陷阱
        TrapManager.removeAllTraps(plotId)
        info("[Farm] 管理员重置: 已清除地块 #$plotId 的所有陷阱")

        // 3. 重置等级为 1
        FarmLevelManager.setPlayerLevel(targetUUID, 1)
        FarmLevelManager.invalidateCache(targetUUID)
        info("[Farm] 管理员重置: 已重置玩家 $targetName 的农场等级为 1")

        // 4. 重置地块（恢复初始尺寸和地形）
        PlotManager.resetPlot(plotId)
        info("[Farm] 管理员重置: 已重置地块 #$plotId 为初始状态")

        // 5. 重置玩家数据（统计归零、通知清除）
        PlayerDataManager.resetPlayerData(targetUUID)
        info("[Farm] 管理员重置: 已重置玩家 $targetName 的数据")

        // 6. 发布事件
        FarmResetEvent(adminUUID, targetUUID).call()
        info("[Farm] 管理员重置完成: admin=$adminUUID, target=$targetUUID")
    }

    // ==================== 设置等级 ====================

    /**
     * 设置玩家农场等级
     *
     * @param admin 执行操作的管理员
     * @param targetUUID 目标玩家 UUID
     * @param targetName 目标玩家名称
     * @param level 目标等级
     */
    fun setPlayerLevel(admin: Player, targetUUID: UUID, targetName: String, level: Int) {
        if (!hasPermission(admin)) {
            admin.sendLang("admin-no-permission")
            return
        }

        val maxLevel = FarmLevelManager.getMaxLevel()
        if (level < 1 || level > maxLevel) {
            admin.sendLang("admin-setlevel-invalid", level, maxLevel)
            return
        }

        val oldLevel = FarmLevelManager.getPlayerLevel(targetUUID)
        FarmLevelManager.setPlayerLevel(targetUUID, level)
        FarmLevelManager.invalidateCache(targetUUID)

        // 同步地块大小：计算目标等级应有的累计扩展量并应用
        val plot = PlotManager.getPlotByOwner(targetUUID)
        if (plot != null) {
            val targetTotalIncrease = (2..level).sumOf { lvl ->
                FarmLevelManager.getDefinition(lvl)?.plotSizeIncrease ?: 0
            }
            val initialSize = PlotManager.config.getInt("initial-plot-size", 16)
            val expectedSize = initialSize + targetTotalIncrease
            if (plot.size != expectedSize) {
                val diff = expectedSize - plot.size
                if (diff > 0) {
                    PlotManager.expandPlot(plot.id, diff)
                } else {
                    // 降级场景：重置地块到目标尺寸
                    PlotManager.resetPlot(plot.id)
                    if (targetTotalIncrease > 0) {
                        PlotManager.expandPlot(plot.id, targetTotalIncrease)
                    }
                }
                info("[Farm] 管理员设置等级: 地块 #${plot.id} 尺寸同步为 $expectedSize")
            }
        }

        admin.sendLang("admin-setlevel-success", targetName, level)
        info("[Farm] 管理员 ${admin.name} 将玩家 $targetName 的农场等级设置为 $level")
    }

    // ==================== 重载配置 ====================

    /**
     * 重载所有模块配置
     *
     * @param admin 执行操作的管理员
     */
    fun reloadAllConfigs(admin: Player) {
        if (!hasPermission(admin)) {
            admin.sendLang("admin-no-permission")
            return
        }

        PlotManager.reload()
        CropManager.reload()
        TrapManager.reload()
        FarmLevelManager.reload()

        admin.sendLang("admin-reload-success")
        info("[Farm] 管理员 ${admin.name} 已重载所有模块配置")
    }
}