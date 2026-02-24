package com.hareidus.taboo.farm.modules.l1.crop

import com.hareidus.taboo.farm.foundation.model.CropInstance
import com.hareidus.taboo.farm.foundation.model.CropRemoveReason
import com.hareidus.taboo.farm.foundation.model.GrowthAccelerateReason
import com.hareidus.taboo.farm.foundation.model.Plot
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import taboolib.platform.type.BukkitProxyEvent

/**
 * 一株作物被成功种植时触发
 */
class CropPlantedEvent(
    val player: Player,
    val crop: CropInstance,
    val plot: Plot
) : BukkitProxyEvent()

/**
 * 一株作物被收割时触发（含自动收割和手动收割）
 */
class CropHarvestedEvent(
    val player: Player?,
    val crop: CropInstance,
    val harvestItems: List<ItemStack>,
    val isAutoHarvest: Boolean
) : BukkitProxyEvent()

/**
 * 作物生长阶段因时间推进或加速而变更时触发
 */
class CropGrowthUpdatedEvent(
    val crop: CropInstance,
    val oldStage: Int,
    val newStage: Int,
    val reason: GrowthAccelerateReason
) : BukkitProxyEvent()

/**
 * 一株作物数据被移除时触发（被偷、收割、重置等）
 */
class CropRemovedEvent(
    val crop: CropInstance,
    val reason: CropRemoveReason
) : BukkitProxyEvent()
