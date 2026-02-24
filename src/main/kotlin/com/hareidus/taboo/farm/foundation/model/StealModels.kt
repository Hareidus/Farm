package com.hareidus.taboo.farm.foundation.model

import java.util.UUID

/** 单次偷菜行为日志 */
data class StealRecord(
    val id: Long = 0,
    val thiefUUID: UUID,
    val victimUUID: UUID,
    val cropType: String,
    val amount: Int,
    val timestamp: Long
)

/** 玩家对特定农场主的偷菜冷却状态 */
data class StealCooldown(
    val thiefUUID: UUID,
    val victimUUID: UUID,
    val startTime: Long,
    val duration: Long
) {
    /** 冷却是否已结束 */
    fun isExpired(): Boolean = System.currentTimeMillis() >= startTime + duration
    /** 剩余冷却时间（毫秒） */
    fun remainingTime(): Long = maxOf(0, startTime + duration - System.currentTimeMillis())
}
