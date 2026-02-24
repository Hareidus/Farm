package com.hareidus.taboo.farm.foundation.model

/** 离线通知类型 */
enum class NotificationType {
    STOLEN,
    TRAP_TRIGGERED,
    FRIEND_REQUEST,
    WATERED
}

/** 陷阱惩罚效果类型 */
enum class TrapPenaltyType {
    SLOWNESS,
    MONEY_DEDUCTION,
    FORCE_TELEPORT
}

/** 玩家统计维度 */
enum class StatisticType {
    TOTAL_HARVEST,
    TOTAL_STEAL,
    TOTAL_STOLEN,
    TOTAL_COIN_INCOME,
    TRAP_TRIGGERED_COUNT
}

/** 好友请求状态 */
enum class FriendRequestStatus {
    PENDING,
    ACCEPTED,
    REJECTED,
    EXPIRED
}

/** 作物移除原因 */
enum class CropRemoveReason {
    HARVESTED,
    STOLEN,
    ADMIN_RESET,
    AUTO_HARVESTED
}

/** 作物生长加速原因 */
enum class GrowthAccelerateReason {
    NATURAL,
    BONEMEAL,
    WATERING
}
