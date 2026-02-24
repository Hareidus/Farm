package com.hareidus.taboo.farm.peripheral.test

import org.bukkit.command.CommandSender
import taboolib.common.platform.command.CommandBody
import taboolib.common.platform.command.CommandHeader
import taboolib.common.platform.command.mainCommand
import taboolib.common.platform.command.subCommand
import taboolib.module.chat.colored

/**
 * 测试命令入口
 *
 * /farmtest          — 显示帮助
 * /farmtest all      — 运行全部测试
 * /farmtest <module> — 运行指定模块测试
 */
@CommandHeader(
    name = "farmtest",
    permission = "stealfarm.admin",
    description = "Farm 模块测试"
)
object TestCommand {

    @CommandBody
    val main = mainCommand {
        execute<CommandSender> { sender, _, _ ->
            sender.sendMessage(*buildList {
                add("&6===== Farm 测试命令 =====")
                add("&7/farmtest all &8- &f运行全部测试")
                add("&7/farmtest plot &8- &f地块管理器测试")
                add("&7/farmtest crop &8- &f作物管理器测试")
                add("&7/farmtest farmlevel &8- &f农场等级测试")
                add("&7/farmtest economy &8- &f经济管理器测试")
                add("&7/farmtest trap &8- &f陷阱管理器测试")
                add("&7/farmtest shop &8- &f商店管理器测试")
                add("&7/farmtest leaderboard &8- &f排行榜测试")
                add("&7/farmtest upgrade &8- &f升级管理器测试")
                add("&7/farmtest steal &8- &f偷菜管理器测试")
                add("&7/farmtest achievement &8- &f成就管理器测试")
            }.map { it.colored() }.toTypedArray())
        }
    }

    @CommandBody
    val all = subCommand {
        execute<CommandSender> { sender, _, _ ->
            sender.sendMessage("&e[Farm] 运行全部测试...".colored())
            val allResults = mutableListOf<TestFramework.TestResult>()
            allResults.addAll(PlotTestRunner.run())
            allResults.addAll(CropTestRunner.run())
            allResults.addAll(FarmLevelTestRunner.run())
            allResults.addAll(EconomyTestRunner.run())
            allResults.addAll(TrapTestRunner.run())
            allResults.addAll(ShopTestRunner.run())
            allResults.addAll(LeaderboardTestRunner.run())
            allResults.addAll(UpgradeTestRunner.run())
            allResults.addAll(StealTestRunner.run())
            allResults.addAll(AchievementTestRunner.run())
            val passed = allResults.count { it.passed }
            val failed = allResults.count { !it.passed }
            sender.sendMessage("&6[Farm] 全部测试完成: &a$passed 通过 &c$failed 失败 &7/ ${allResults.size} 总计".colored())
        }
    }

    @CommandBody
    val plot = subCommand {
        execute<CommandSender> { sender, _, _ ->
            PlotTestRunner.run()
            sender.sendMessage("&a[Farm] plot 测试完成，查看日志".colored())
        }
    }

    @CommandBody
    val crop = subCommand {
        execute<CommandSender> { sender, _, _ ->
            CropTestRunner.run()
            sender.sendMessage("&a[Farm] crop 测试完成，查看日志".colored())
        }
    }

    @CommandBody
    val farmlevel = subCommand {
        execute<CommandSender> { sender, _, _ ->
            FarmLevelTestRunner.run()
            sender.sendMessage("&a[Farm] farmlevel 测试完成，查看日志".colored())
        }
    }

    @CommandBody
    val economy = subCommand {
        execute<CommandSender> { sender, _, _ ->
            EconomyTestRunner.run()
            sender.sendMessage("&a[Farm] economy 测试完成，查看日志".colored())
        }
    }

    @CommandBody
    val trap = subCommand {
        execute<CommandSender> { sender, _, _ ->
            TrapTestRunner.run()
            sender.sendMessage("&a[Farm] trap 测试完成，查看日志".colored())
        }
    }

    @CommandBody
    val shop = subCommand {
        execute<CommandSender> { sender, _, _ ->
            ShopTestRunner.run()
            sender.sendMessage("&a[Farm] shop 测试完成，查看日志".colored())
        }
    }

    @CommandBody
    val leaderboard = subCommand {
        execute<CommandSender> { sender, _, _ ->
            LeaderboardTestRunner.run()
            sender.sendMessage("&a[Farm] leaderboard 测试完成，查看日志".colored())
        }
    }

    @CommandBody
    val upgrade = subCommand {
        execute<CommandSender> { sender, _, _ ->
            UpgradeTestRunner.run()
            sender.sendMessage("&a[Farm] upgrade 测试完成，查看日志".colored())
        }
    }

    @CommandBody
    val steal = subCommand {
        execute<CommandSender> { sender, _, _ ->
            StealTestRunner.run()
            sender.sendMessage("&a[Farm] steal 测试完成，查看日志".colored())
        }
    }

    @CommandBody
    val achievement = subCommand {
        execute<CommandSender> { sender, _, _ ->
            AchievementTestRunner.run()
            sender.sendMessage("&a[Farm] achievement 测试完成，查看日志".colored())
        }
    }
}
