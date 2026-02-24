package com.hareidus.taboo.farm.modules.l2.friendinteraction

import taboolib.platform.type.BukkitProxyEvent
import java.util.UUID

/**
 * 好友成功为作物浇水后触发
 */
class CropWateredEvent(
    val watererUUID: UUID,
    val ownerUUID: UUID,
    val cropTypeId: String,
    val cropX: Int,
    val cropY: Int,
    val cropZ: Int,
    val newGrowthStage: Int
) : BukkitProxyEvent()
