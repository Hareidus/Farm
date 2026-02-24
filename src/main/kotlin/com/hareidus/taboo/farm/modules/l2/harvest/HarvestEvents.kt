package com.hareidus.taboo.farm.modules.l2.harvest

import com.hareidus.taboo.farm.foundation.model.CropInstance
import org.bukkit.entity.Player
import taboolib.platform.type.BukkitProxyEvent

/**
 * 玩家对作物使用骨粉加速后触发
 */
class CropBonemeledEvent(
    val player: Player,
    val crop: CropInstance,
    val newGrowthStage: Int
) : BukkitProxyEvent()
