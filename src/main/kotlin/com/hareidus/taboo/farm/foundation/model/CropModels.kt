package com.hareidus.taboo.farm.foundation.model

import java.util.UUID

/** 作物生长阶段定义 */
data class CropStage(
    val stageIndex: Int,
    val duration: Long,
    val material: String
)

/** 作物类型定义（从 config 加载） */
data class CropDefinition(
    val id: String,
    val name: String,
    val isCustom: Boolean,
    val stages: List<CropStage>,
    val harvestMinAmount: Int,
    val harvestMaxAmount: Int,
    val seedItemId: String,
    val harvestItemId: String,
    val totalGrowthTime: Long,
    val source: String = "config"  // "config" | "external"
)

/** 地块内一株具体作物的运行时数据 */
data class CropInstance(
    val id: Long = 0,
    val cropTypeId: String,
    val plotId: Long,
    val ownerUUID: UUID,
    val worldName: String,
    val x: Int,
    val y: Int,
    val z: Int,
    var plantedAt: Long
)

/** 作物收购价格条目（从 config 加载） */
data class CropPrice(
    val cropId: String,
    val sellPrice: Double
)
