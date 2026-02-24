package com.hareidus.taboo.farm.modules.l3.placeholder

import com.hareidus.taboo.farm.modules.l1.farmlevel.FarmLevelManager
import com.hareidus.taboo.farm.modules.l1.playerdata.PlayerDataManager
import com.hareidus.taboo.farm.modules.l1.plot.PlotManager
import com.hareidus.taboo.farm.modules.l2.achievement.AchievementManager
import com.hareidus.taboo.farm.modules.l2.leaderboard.LeaderboardManager
import org.bukkit.entity.Player
import taboolib.platform.compat.PlaceholderExpansion

/**
 * StealFarm PlaceholderAPI 扩展 (L3)
 *
 * 前缀: stealfarm
 * 向外部插件暴露农场相关变量。
 *
 * 依赖: PlayerDataManager, FarmLevelManager, PlotManager, LeaderboardManager, AchievementManager
 */
object FarmPlaceholderExpansion : PlaceholderExpansion {

    override val identifier: String = "stealfarm"

    override val enabled: Boolean = true

    override fun onPlaceholderRequest(player: Player?, args: String): String {
        if (player == null) return "0"
        val uuid = player.uniqueId

        return when (args) {
            // 农场等级
            "level" -> FarmLevelManager.getPlayerLevel(uuid).toString()

            // 收获总量
            "total_harvest" -> PlayerDataManager.getPlayerData(uuid)?.totalHarvest?.toString() ?: "0"

            // 偷菜总量
            "total_steal" -> PlayerDataManager.getPlayerData(uuid)?.totalSteal?.toString() ?: "0"

            // 被偷总量
            "total_stolen" -> PlayerDataManager.getPlayerData(uuid)?.totalStolen?.toString() ?: "0"

            // 金币收入（Double 保留 2 位小数）
            "total_coin_income" -> PlayerDataManager.getPlayerData(uuid)
                ?.totalCoinIncome?.let { "%.2f".format(it) } ?: "0.00"

            // 触发陷阱次数
            "trap_triggered" -> PlayerDataManager.getPlayerData(uuid)?.trapTriggeredCount?.toString() ?: "0"

            // 地块尺寸
            "plot_size" -> PlotManager.getPlotByOwner(uuid)?.size?.toString() ?: "0"

            // 排名（未上榜返回 "-"）
            "rank_harvest" -> LeaderboardManager.getPlayerRank(uuid, "harvest")?.toString() ?: "-"
            "rank_steal" -> LeaderboardManager.getPlayerRank(uuid, "steal")?.toString() ?: "-"
            "rank_wealth" -> LeaderboardManager.getPlayerRank(uuid, "wealth")?.toString() ?: "-"
            "rank_defense" -> LeaderboardManager.getPlayerRank(uuid, "defense")?.toString() ?: "-"

            // 已完成成就数
            "achievements" -> AchievementManager.getPlayerAchievements(uuid)
                .count { it.completed }.toString()

            else -> ""
        }
    }
}
