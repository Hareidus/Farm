package com.hareidus.taboo.farm.modules.l2.steal

import taboolib.platform.type.BukkitProxyEvent
import java.util.UUID

/**
 * 玩家成功偷取一株作物后触发
 */
class CropStolenEvent(
    val thiefUUID: UUID,
    val victimUUID: UUID,
    val cropType: String,
    val amount: Int
) : BukkitProxyEvent()

/**
 * 玩家对某农场主的偷菜达到上限，开始冷却时触发
 */
class StealCooldownStartedEvent(
    val thiefUUID: UUID,
    val victimUUID: UUID,
    val cooldownEndTime: Long
) : BukkitProxyEvent()

/**
 * 偷菜者触发陷阱后触发
 */
class TrapTriggeredEvent(
    val thiefUUID: UUID,
    val farmOwnerUUID: UUID,
    val trapTypeId: String,
    val penaltyType: String
) : BukkitProxyEvent()
