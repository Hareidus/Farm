package com.hareidus.taboo.farm.foundation.model

import java.util.UUID

/** 玩家核心数据聚合 */
data class PlayerData(
    val uuid: UUID,
    var totalHarvest: Long = 0,
    var totalSteal: Long = 0,
    var totalStolen: Long = 0,
    var totalCoinIncome: Double = 0.0,
    var trapTriggeredCount: Int = 0,
    var consecutiveLoginDays: Int = 0,
    var lastLoginDate: Long = 0L
)

/** 玩家离线通知条目 */
data class OfflineNotification(
    val id: Long = 0,
    val playerUUID: UUID,
    val type: NotificationType,
    val data: String,
    val timestamp: Long,
    var read: Boolean = false
)

/** 玩家农场等级数据 */
data class PlayerFarmLevel(
    val playerUUID: UUID,
    var currentLevel: Int = 1
)

/** 玩家成就进度 */
data class PlayerAchievement(
    val id: Long = 0,
    val playerUUID: UUID,
    val achievementId: String,
    var currentProgress: Long = 0,
    var completed: Boolean = false,
    var completedAt: Long? = null
)
