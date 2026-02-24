package com.hareidus.taboo.farm.foundation.model

import java.util.UUID

/** 成就定义（从 config 加载） */
data class AchievementDefinition(
    val id: String,
    val name: String,
    val description: String,
    val triggerType: String,
    val threshold: Long,
    val rewardMoney: Double,
    val rewardItems: Map<String, Int>,
    val titlePrefix: String
)

/** 排行榜条目（缓存数据） */
data class LeaderboardEntry(
    val playerUUID: UUID,
    val playerName: String,
    val category: String,
    val value: Long,
    val rank: Int
)
