package com.hareidus.taboo.farm.modules.l2.shop

import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerInteractEntityEvent
import taboolib.common.platform.event.SubscribeEvent

/**
 * 收购站 NPC 交互监听器
 *
 * 当玩家右键点击收购站 NPC 时，打开商店 GUI。
 */
object ShopNpcListener {

    @SubscribeEvent
    fun onInteractEntity(e: PlayerInteractEntityEvent) {
        if (!ShopManager.isShopNpc(e.rightClicked.customName)) return
        e.isCancelled = true
        ShopGui.open(e.player)
    }
}
