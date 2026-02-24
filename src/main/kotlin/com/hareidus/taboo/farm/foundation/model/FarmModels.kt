package com.hareidus.taboo.farm.foundation.model

import java.util.UUID

/** 农场地块实体 */
data class Plot(
    val id: Long = 0,
    val ownerUUID: UUID,
    val gridX: Int,
    val gridZ: Int,
    val worldName: String,
    var minX: Int,
    var minZ: Int,
    var maxX: Int,
    var maxZ: Int,
    var size: Int
)

/** 农场等级配置定义（从 config 加载） */
data class FarmLevelDefinition(
    val level: Int,
    val upgradeCostMoney: Double,
    val upgradeCostMaterials: Map<String, Int>,
    val conditions: List<String>,
    val plotSizeIncrease: Int,
    val trapSlots: Int,
    val decorationSlots: Int,
    val protectionLevel: Int,
    val stealRatioReduction: Double,
    val autoHarvestUnlocked: Boolean
)

/** 农场仓库条目（自动收割产出存储） */
data class FarmStorage(
    val id: Long = 0,
    val playerUUID: UUID,
    val itemType: String,
    var amount: Int
)
