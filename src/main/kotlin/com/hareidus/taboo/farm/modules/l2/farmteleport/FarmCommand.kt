package com.hareidus.taboo.farm.modules.l2.farmteleport

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import taboolib.common.platform.command.CommandBody
import taboolib.common.platform.command.CommandHeader
import taboolib.common.platform.command.mainCommand
import taboolib.common.platform.command.subCommand
import taboolib.platform.util.sendLang

/**
 * 农场主命令 (L2)
 *
 * /farm          — 传送到自己的农场
 * /farm visit <玩家> — 传送到指定玩家的农场
 */
@CommandHeader(
    name = "farm",
    permission = "stealfarm.use",
    description = "农场传送"
)
object FarmCommand {

    @CommandBody
    val main = mainCommand {
        execute<Player> { sender, _, _ ->
            FarmTeleportManager.teleportToOwnFarm(sender)
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
}
