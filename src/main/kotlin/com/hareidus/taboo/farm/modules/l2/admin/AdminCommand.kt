package com.hareidus.taboo.farm.modules.l2.admin

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import taboolib.common.platform.command.CommandBody
import taboolib.common.platform.command.CommandHeader
import taboolib.common.platform.command.mainCommand
import taboolib.common.platform.command.subCommand
import taboolib.module.chat.colored
import taboolib.platform.util.sendLang

/**
 * 管理员命令 (L2)
 *
 * /farmadmin                        — 显示帮助
 * /farmadmin setlevel <玩家> <等级>  — 设置玩家农场等级
 * /farmadmin reset <玩家>           — 重置玩家农场（二次确认）
 * /farmadmin reload                 — 重载所有模块配置
 */
@CommandHeader(
    name = "farmadmin",
    aliases = ["fa"],
    permission = "stealfarm.admin",
    description = "Farm 管理员工具"
)
object AdminCommand {

    @CommandBody
    val main = mainCommand {
        execute<Player> { sender, _, _ ->
            sender.sendMessage(*listOf(
                "&6===== Farm 管理员命令 =====",
                "&7/fa setlevel <玩家> <等级> &8- &f设置农场等级",
                "&7/fa reset <玩家> &8- &f重置农场（需二次确认）",
                "&7/fa reload &8- &f重载所有配置"
            ).map { it.colored() }.toTypedArray())
        }
    }

    @CommandBody
    val setlevel = subCommand {
        dynamic("player") {
            suggestion<Player> { _, _ ->
                Bukkit.getOnlinePlayers().map { it.name }
            }
            dynamic("level") {
                suggestion<Player> { _, _ ->
                    (1..10).map { it.toString() }
                }
                execute<Player> { sender, context, _ ->
                    val targetName = context["player"]
                    val target = Bukkit.getOfflinePlayer(targetName)
                    if (target.name == null && !target.hasPlayedBefore()) {
                        sender.sendLang("admin-player-not-found", targetName)
                        return@execute
                    }
                    val level = context["level"].toIntOrNull()
                    if (level == null) {
                        sender.sendMessage("&c无效的等级数值".colored())
                        return@execute
                    }
                    AdminManager.setPlayerLevel(sender, target.uniqueId, targetName, level)
                }
            }
        }
    }

    @CommandBody
    val reset = subCommand {
        dynamic("player") {
            suggestion<Player> { _, _ ->
                Bukkit.getOnlinePlayers().map { it.name }
            }
            execute<Player> { sender, context, _ ->
                val targetName = context["player"]
                val target = Bukkit.getOfflinePlayer(targetName)
                if (target.name == null && !target.hasPlayedBefore()) {
                    sender.sendLang("admin-player-not-found", targetName)
                    return@execute
                }
                AdminManager.resetFarm(sender, target.uniqueId, targetName)
            }
        }
    }

    @CommandBody
    val reload = subCommand {
        execute<Player> { sender, _, _ ->
            AdminManager.reloadAllConfigs(sender)
        }
    }
}
