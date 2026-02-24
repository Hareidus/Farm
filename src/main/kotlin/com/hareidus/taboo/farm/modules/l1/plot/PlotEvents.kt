package com.hareidus.taboo.farm.modules.l1.plot

import com.hareidus.taboo.farm.foundation.model.Plot
import org.bukkit.entity.Player
import taboolib.platform.type.BukkitProxyEvent

/**
 * 新地块被分配给玩家时触发
 */
class PlotAllocatedEvent(
    val player: Player,
    val plot: Plot
) : BukkitProxyEvent()

/**
 * 地块物理边界因升级而扩展时触发
 */
class PlotExpandedEvent(
    val player: Player,
    val plot: Plot,
    val oldSize: Int,
    val newSize: Int
) : BukkitProxyEvent()
