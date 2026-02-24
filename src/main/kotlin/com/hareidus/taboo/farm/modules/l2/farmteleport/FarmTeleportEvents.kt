package com.hareidus.taboo.farm.modules.l2.farmteleport

import com.hareidus.taboo.farm.foundation.model.Plot
import org.bukkit.entity.Player
import taboolib.platform.type.BukkitProxyEvent
import java.util.UUID

/**
 * 新玩家首次被分配地块后触发
 */
class FarmPlotAssignedEvent(
    val player: Player,
    val plot: Plot
) : BukkitProxyEvent()

/**
 * 玩家传送回自己农场后触发
 */
class PlayerEnterOwnFarmEvent(
    val player: Player,
    val plotId: Long
) : BukkitProxyEvent()

/**
 * 玩家传送到他人农场后触发
 */
class PlayerEnterOtherFarmEvent(
    val visitorPlayer: Player,
    val ownerUUID: UUID,
    val plotId: Long
) : BukkitProxyEvent()

/**
 * 玩家离开农场世界时触发
 */
class PlayerLeaveFarmEvent(
    val player: Player,
    val plotId: Long
) : BukkitProxyEvent()
