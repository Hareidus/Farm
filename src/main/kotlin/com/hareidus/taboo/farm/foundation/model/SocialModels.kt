package com.hareidus.taboo.farm.foundation.model

import java.util.UUID

/** 双向好友关系记录 */
data class FriendRelation(
    val id: Long = 0,
    val playerA: UUID,
    val playerB: UUID,
    val createdAt: Long
)

/** 仇人标记记录 */
data class EnemyRecord(
    val id: Long = 0,
    val victimUUID: UUID,
    val thiefUUID: UUID,
    val markedAt: Long
)

/** 待处理的好友请求 */
data class FriendRequest(
    val id: Long = 0,
    val senderUUID: UUID,
    val receiverUUID: UUID,
    val requestedAt: Long,
    var status: FriendRequestStatus = FriendRequestStatus.PENDING
)

/** 好友浇水冷却记录 */
data class WaterCooldown(
    val watererUUID: UUID,
    val targetUUID: UUID,
    val cooldownEndTime: Long
)
