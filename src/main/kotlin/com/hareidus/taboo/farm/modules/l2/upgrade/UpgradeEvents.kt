package com.hareidus.taboo.farm.modules.l2.upgrade

import org.bukkit.entity.Player
import taboolib.platform.type.BukkitProxyEvent
import java.util.UUID

/**
 * 玩家成功升级农场后触发
 */
class FarmUpgradedEvent(
    val player: Player,
    val oldLevel: Int,
    val newLevel: Int,
    val unlockedFeatures: List<String>
) : BukkitProxyEvent()

/**
 * 玩家成功部署陷阱后触发
 */
class TrapDeployedEvent(
    val playerUUID: UUID,
    val trapTypeId: String,
    val slotIndex: Int
) : BukkitProxyEvent()
