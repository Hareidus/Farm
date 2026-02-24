package com.hareidus.taboo.farm.modules.l3.debug

import com.hareidus.taboo.farm.foundation.database.DatabaseManager
import com.hareidus.taboo.farm.modules.l1.crop.CropManager
import com.hareidus.taboo.farm.modules.l1.farmlevel.FarmLevelManager
import com.hareidus.taboo.farm.modules.l1.playerdata.PlayerDataManager
import com.hareidus.taboo.farm.modules.l1.plot.PlotManager
import com.hareidus.taboo.farm.modules.l1.social.SocialManager
import com.hareidus.taboo.farm.modules.l1.stealrecord.StealRecordManager
import com.hareidus.taboo.farm.modules.l1.trap.TrapManager
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import taboolib.common.platform.command.CommandBody
import taboolib.common.platform.command.CommandHeader
import taboolib.common.platform.command.mainCommand
import taboolib.common.platform.command.subCommand
import taboolib.module.chat.colored
import java.text.SimpleDateFormat
import java.util.Date

/**
 * 调试命令 (L3)
 *
 * 主命令: /farmdebug
 * 权限: stealfarm.debug
 *
 * 提供管理员调试命令，查看玩家实时农场状态、地块信息、作物列表、
 * 冷却状态、社交关系、陷阱部署，以及强制刷新作物生长、清除冷却等调试操作。
 */
@CommandHeader(
    name = "farmdebug",
    permission = "stealfarm.debug",
    description = "Farm 调试工具"
)
object DebugCommand {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")

    private fun formatTime(timestamp: Long): String {
        return if (timestamp <= 0) "N/A" else dateFormat.format(Date(timestamp))
    }

    private fun formatMs(ms: Long): String {
        val totalSec = ms / 1000
        val h = totalSec / 3600
        val m = (totalSec % 3600) / 60
        val s = totalSec % 60
        return "${h}h ${m}m ${s}s"
    }

    @CommandBody
    val main = mainCommand {
        execute<CommandSender> { sender, _, _ ->
            sender.sendMessage(*buildList {
                add("&6===== Farm Debug 命令帮助 =====")
                add("&7/farmdebug player <name> &8- &f查看玩家完整状态")
                add("&7/farmdebug plot <name> &8- &f查看玩家地块信息")
                add("&7/farmdebug crops <name> &8- &f查看作物列表与生长进度")
                add("&7/farmdebug traps <name> &8- &f查看陷阱部署")
                add("&7/farmdebug social <name> &8- &f查看好友/仇人关系")
                add("&7/farmdebug cooldown <thief> <victim> &8- &f查看偷菜冷却")
                add("&7/farmdebug refreshcrops <name> &8- &f强制刷新作物视觉")
                add("&7/farmdebug clearcooldown <thief> <victim> &8- &f清除偷菜冷却")
            }.map { it.colored() }.toTypedArray())
        }
    }

    @CommandBody
    val player = subCommand {
        dynamic("name") {
            suggestion<CommandSender> { _, _ ->
                Bukkit.getOnlinePlayers().map { it.name }
            }
            execute<CommandSender> { sender, context, _ ->
                val name = context["name"]
                val target = Bukkit.getOfflinePlayer(name)
                val uuid = target.uniqueId
                val data = PlayerDataManager.getPlayerData(uuid)
                val level = FarmLevelManager.getPlayerLevel(uuid)
                if (data == null) {
                    sender.sendMessage("&c未找到玩家 $name 的数据".colored())
                    return@execute
                }
                sender.sendMessage(*buildList {
                    add("&6===== 玩家调试信息: $name =====")
                    add("&7UUID: &f$uuid")
                    add("&7农场等级: &f$level")
                    add("&7收获总量: &f${data.totalHarvest}")
                    add("&7偷菜总量: &f${data.totalSteal}")
                    add("&7被偷总量: &f${data.totalStolen}")
                    add("&7金币收入: &f${data.totalCoinIncome}")
                    add("&7触发陷阱次数: &f${data.trapTriggeredCount}")
                    add("&7连续登录天数: &f${data.consecutiveLoginDays}")
                    add("&7最后登录: &f${formatTime(data.lastLoginDate)}")
                }.map { it.colored() }.toTypedArray())
            }
        }
    }

    @CommandBody
    val plot = subCommand {
        dynamic("name") {
            suggestion<CommandSender> { _, _ ->
                Bukkit.getOnlinePlayers().map { it.name }
            }
            execute<CommandSender> { sender, context, _ ->
                val name = context["name"]
                val uuid = Bukkit.getOfflinePlayer(name).uniqueId
                val plot = PlotManager.getPlotByOwner(uuid)
                if (plot == null) {
                    sender.sendMessage("&c玩家 $name 没有地块".colored())
                    return@execute
                }
                val center = PlotManager.getPlotCenter(plot)
                sender.sendMessage(*buildList {
                    add("&6===== 地块调试信息: $name =====")
                    add("&7地块ID: &f${plot.id}")
                    add("&7世界: &f${plot.worldName}")
                    add("&7网格坐标: &f(${plot.gridX}, ${plot.gridZ})")
                    add("&7边界: &f(${plot.minX},${plot.minZ}) ~ (${plot.maxX},${plot.maxZ})")
                    add("&7尺寸: &f${plot.size}")
                    add("&7中心: &f(${center.first}, ${center.second}, ${center.third})")
                }.map { it.colored() }.toTypedArray())
            }
        }
    }

    @CommandBody
    val crops = subCommand {
        dynamic("name") {
            suggestion<CommandSender> { _, _ ->
                Bukkit.getOnlinePlayers().map { it.name }
            }
            execute<CommandSender> { sender, context, _ ->
                val name = context["name"]
                val uuid = Bukkit.getOfflinePlayer(name).uniqueId
                val plot = PlotManager.getPlotByOwner(uuid)
                if (plot == null) {
                    sender.sendMessage("&c玩家 $name 没有地块".colored())
                    return@execute
                }
                val cropList = CropManager.getCropsByPlot(plot.id)
                if (cropList.isEmpty()) {
                    sender.sendMessage("&e玩家 $name 的地块内没有作物".colored())
                    return@execute
                }
                sender.sendMessage("&6===== 作物调试信息: $name (共${cropList.size}株) =====".colored())
                for (crop in cropList) {
                    val def = CropManager.getCropDefinition(crop.cropTypeId)
                    val stage = CropManager.calculateGrowthStage(crop)
                    val mature = CropManager.isMature(crop)
                    val totalStages = def?.stages?.size ?: 0
                    val matureTag = if (mature) "&a[成熟]" else "&e[生长中]"
                    sender.sendMessage(*buildList {
                        add("&7 #${crop.id} &f${def?.name ?: crop.cropTypeId} $matureTag")
                        add("&7   位置: &f(${crop.x},${crop.y},${crop.z}) &7阶段: &f${stage + 1}/$totalStages")
                        add("&7   种植时间: &f${formatTime(crop.plantedAt)}")
                    }.map { it.colored() }.toTypedArray())
                }
            }
        }
    }

    @CommandBody
    val traps = subCommand {
        dynamic("name") {
            suggestion<CommandSender> { _, _ ->
                Bukkit.getOnlinePlayers().map { it.name }
            }
            execute<CommandSender> { sender, context, _ ->
                val name = context["name"]
                val uuid = Bukkit.getOfflinePlayer(name).uniqueId
                val plot = PlotManager.getPlotByOwner(uuid)
                if (plot == null) {
                    sender.sendMessage("&c玩家 $name 没有地块".colored())
                    return@execute
                }
                val trapList = TrapManager.getDeployedTraps(plot.id)
                if (trapList.isEmpty()) {
                    sender.sendMessage("&e玩家 $name 的地块内没有陷阱".colored())
                    return@execute
                }
                val maxSlots = FarmLevelManager.getTrapSlots(FarmLevelManager.getPlayerLevel(uuid))
                sender.sendMessage("&6===== 陷阱调试信息: $name (${trapList.size}/$maxSlots 槽位) =====".colored())
                for (trap in trapList) {
                    val def = TrapManager.getTrapDefinition(trap.trapTypeId)
                    sender.sendMessage(
                        "&7 槽位#${trap.slotIndex}: &f${def?.name ?: trap.trapTypeId} &7触发率: &f${def?.triggerChance ?: "?"}".colored()
                    )
                }
            }
        }
    }

    @CommandBody
    val social = subCommand {
        dynamic("name") {
            suggestion<CommandSender> { _, _ ->
                Bukkit.getOnlinePlayers().map { it.name }
            }
            execute<CommandSender> { sender, context, _ ->
                val name = context["name"]
                val uuid = Bukkit.getOfflinePlayer(name).uniqueId
                val friends = SocialManager.getFriends(uuid)
                val enemies = SocialManager.getEnemies(uuid)
                sender.sendMessage("&6===== 社交调试信息: $name =====".colored())
                if (friends.isEmpty()) {
                    sender.sendMessage("&7好友: &f无".colored())
                } else {
                    sender.sendMessage("&7好友 (${friends.size}):".colored())
                    for (f in friends) {
                        val otherUUID = if (f.playerA == uuid) f.playerB else f.playerA
                        val otherName = Bukkit.getOfflinePlayer(otherUUID).name ?: otherUUID.toString()
                        sender.sendMessage("&7 - &f$otherName &7(${formatTime(f.createdAt)})".colored())
                    }
                }
                if (enemies.isEmpty()) {
                    sender.sendMessage("&7仇人: &f无".colored())
                } else {
                    sender.sendMessage("&7仇人 (${enemies.size}):".colored())
                    for (e in enemies) {
                        val thiefName = Bukkit.getOfflinePlayer(e.thiefUUID).name ?: e.thiefUUID.toString()
                        sender.sendMessage("&7 - &f$thiefName &7(${formatTime(e.markedAt)})".colored())
                    }
                }
            }
        }
    }

    @CommandBody
    val cooldown = subCommand {
        dynamic("thief") {
            suggestion<CommandSender> { _, _ ->
                Bukkit.getOnlinePlayers().map { it.name }
            }
            dynamic("victim") {
                suggestion<CommandSender> { _, _ ->
                    Bukkit.getOnlinePlayers().map { it.name }
                }
                execute<CommandSender> { sender, context, _ ->
                    val thiefName = context["thief"]
                    val victimName = context["victim"]
                    val thiefUUID = Bukkit.getOfflinePlayer(thiefName).uniqueId
                    val victimUUID = Bukkit.getOfflinePlayer(victimName).uniqueId
                    val onCooldown = StealRecordManager.isOnCooldown(thiefUUID, victimUUID)
                    val remaining = StealRecordManager.getCooldownRemaining(thiefUUID, victimUUID)
                    sender.sendMessage(*buildList {
                        add("&6===== 偷菜冷却调试: $thiefName -> $victimName =====")
                        add("&7冷却中: &f${if (onCooldown) "&c是" else "&a否"}")
                        if (onCooldown) {
                            add("&7剩余时间: &f${formatMs(remaining)}")
                        }
                        val visitCount = StealRecordManager.getVisitStealCount(thiefUUID, victimUUID)
                        add("&7本次访问已偷: &f$visitCount")
                    }.map { it.colored() }.toTypedArray())
                }
            }
        }
    }

    @CommandBody
    val refreshcrops = subCommand {
        dynamic("name") {
            suggestion<CommandSender> { _, _ ->
                Bukkit.getOnlinePlayers().map { it.name }
            }
            execute<CommandSender> { sender, context, _ ->
                val name = context["name"]
                val uuid = Bukkit.getOfflinePlayer(name).uniqueId
                val plot = PlotManager.getPlotByOwner(uuid)
                if (plot == null) {
                    sender.sendMessage("&c玩家 $name 没有地块".colored())
                    return@execute
                }
                CropManager.updateAllCropsInPlot(plot.id)
                val count = CropManager.getCropsByPlot(plot.id).size
                sender.sendMessage("&a已强制刷新玩家 $name 地块内 $count 株作物的视觉".colored())
            }
        }
    }

    @CommandBody
    val clearcooldown = subCommand {
        dynamic("thief") {
            suggestion<CommandSender> { _, _ ->
                Bukkit.getOnlinePlayers().map { it.name }
            }
            dynamic("victim") {
                suggestion<CommandSender> { _, _ ->
                    Bukkit.getOnlinePlayers().map { it.name }
                }
                execute<CommandSender> { sender, context, _ ->
                    val thiefName = context["thief"]
                    val victimName = context["victim"]
                    val thiefUUID = Bukkit.getOfflinePlayer(thiefName).uniqueId
                    val victimUUID = Bukkit.getOfflinePlayer(victimName).uniqueId
                    val removed = DatabaseManager.database.removeStealCooldown(thiefUUID, victimUUID)
                    if (removed) {
                        sender.sendMessage("&a已清除 $thiefName -> $victimName 的偷菜冷却".colored())
                    } else {
                        sender.sendMessage("&e该组合不存在冷却记录".colored())
                    }
                }
            }
        }
    }
}
