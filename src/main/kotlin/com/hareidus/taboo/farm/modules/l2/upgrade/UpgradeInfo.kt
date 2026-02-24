package com.hareidus.taboo.farm.modules.l2.upgrade

import com.hareidus.taboo.farm.foundation.model.FarmLevelDefinition

/**
 * 升级信息快照
 *
 * 封装玩家当前等级状态与下一级升级条件，供 GUI 展示使用。
 */
data class UpgradeInfo(
    /** 当前农场等级 */
    val currentLevel: Int,
    /** 配置中的最大等级 */
    val maxLevel: Int,
    /** 下一级等级定义（已满级时为 null） */
    val nextLevelDef: FarmLevelDefinition?,
    /** 玩家金币是否满足下一级升级消耗 */
    val canAffordMoney: Boolean,
    /** 玩家背包材料是否满足下一级升级消耗 */
    val canAffordMaterials: Boolean
)
