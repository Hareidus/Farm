package com.hareidus.taboo.farm.foundation.model

/** 陷阱种类定义（从 config 加载） */
data class TrapDefinition(
    val id: String,
    val name: String,
    val penaltyType: TrapPenaltyType,
    val triggerChance: Double,
    val deployCostMoney: Double,
    val deployCostMaterials: Map<String, Int>,
    val conditions: List<String>,
    val penaltyValue: Double,
    val source: String = "config"  // "config" | "external"
)

/** 地块内已部署的陷阱实例 */
data class DeployedTrap(
    val id: Long = 0,
    val plotId: Long,
    val trapTypeId: String,
    val slotIndex: Int
)
