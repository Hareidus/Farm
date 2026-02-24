package com.hareidus.taboo.farm.modules.l2.leaderboard

import org.bukkit.entity.Player
import taboolib.common.platform.command.CommandBody
import taboolib.common.platform.command.CommandHeader
import taboolib.common.platform.command.mainCommand
import taboolib.common.platform.command.subCommand

/**
 * 排行榜命令 (L2)
 *
 * 主命令: /farmleaderboard (别名: /flb)
 * 权限: stealfarm.leaderboard
 *
 * 子命令:
 * - /flb           → 打开默认排行榜 GUI (harvest)
 * - /flb <category> → 打开指定类别排行榜 GUI
 */
@CommandHeader(
    name = "farmleaderboard",
    aliases = ["flb"],
    permission = "stealfarm.leaderboard",
    description = "Farm 排行榜"
)
object LeaderboardCommand {

    @CommandBody
    val main = mainCommand {
        execute<Player> { sender, _, _ ->
            LeaderboardGui.open(sender)
        }
    }

    @CommandBody
    val category = subCommand {
        dynamic("category") {
            suggestion<Player> { _, _ ->
                LeaderboardManager.getCategories().toList()
            }
            execute<Player> { sender, context, _ ->
                LeaderboardGui.open(sender, context["category"])
            }
        }
    }
}
