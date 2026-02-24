package com.hareidus.taboo.farm.modules.l2.farmteleport

import com.hareidus.taboo.farm.modules.l2.leaderboard.LeaderboardGui
import com.hareidus.taboo.farm.modules.l2.shop.ShopGui
import com.hareidus.taboo.farm.modules.l2.social.SocialGui
import com.hareidus.taboo.farm.modules.l2.upgrade.UpgradeGui
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import taboolib.common.platform.command.CommandBody
import taboolib.common.platform.command.CommandHeader
import taboolib.common.platform.command.mainCommand
import taboolib.common.platform.command.subCommand
import taboolib.module.chat.colored
import taboolib.platform.util.sendLang

/**
 * 农场主命令 (L2)
 *
 * /farm              — 传送到自己的农场
 * /farm visit <玩家>  — 传送到指定玩家的农场
 * /farm upgrade      — 打开农场升级 GUI
 * /farm shop         — 打开商店 GUI
 * /farm social       — 打开社交 GUI
 * /farm leaderboard  — 打开排行榜 GUI
 * /farm help         — 显示帮助
 */
@CommandHeader(
    name = "farm",
    permission = "stealfarm.use",
    description = "农场系统"
)
object FarmCommand {

    @CommandBody
    val main = mainCommand {
        execute<Player> { sender, _, _ ->
            FarmTeleportManager.teleportToOwnFarm(sender)
        }
    }

    @CommandBody
    val help = subCommand {
        execute<Player> { sender, _, _ ->
            sender.sendMessage(*listOf(
                "&6===== 农场命令 =====",
                "&7/farm &8- &f传送到自己的农场",
                "&7/farm visit <玩家> &8- &f访问他人农场",
                "&7/farm upgrade &8- &f农场升级",
                "&7/farm shop &8- &f作物收购站",
                "&7/farm social &8- &f好友与仇人",
                "&7/farm leaderboard &8- &f排行榜",
            ).map { it.colored() }.toTypedArray())
        }
    }

    @CommandBody
    val visit = subCommand {
        dynamic("player") {
            suggestion<Player> { _, _ ->
                Bukkit.getOnlinePlayers().map { it.name }
            }
            execute<Player> { sender, context, _ ->
                val targetName = context["player"]
                val target = Bukkit.getOfflinePlayer(targetName)
                if (target.name == null && !target.hasPlayedBefore()) {
                    sender.sendLang("farm-cmd-player-not-found", targetName)
                    return@execute
                }
                FarmTeleportManager.teleportToOtherFarm(sender, target.uniqueId)
            }
        }
    }

    @CommandBody
    val upgrade = subCommand {
        execute<Player> { sender, _, _ ->
            UpgradeGui.open(sender)
        }
    }

    @CommandBody
    val shop = subCommand {
        execute<Player> { sender, _, _ ->
            ShopGui.open(sender)
        }
    }

    @CommandBody
    val social = subCommand {
        execute<Player> { sender, _, _ ->
            SocialGui.open(sender)
        }
    }

    @CommandBody
    val leaderboard = subCommand {
        execute<Player> { sender, _, _ ->
            LeaderboardGui.open(sender)
        }
    }
}
