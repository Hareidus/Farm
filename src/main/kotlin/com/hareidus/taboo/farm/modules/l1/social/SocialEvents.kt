package com.hareidus.taboo.farm.modules.l1.social

import org.bukkit.event.Event
import org.bukkit.event.HandlerList
import java.util.UUID

/** 两名玩家成功建立好友关系时触发 */
class FriendAddedEvent(
    val playerA: UUID,
    val playerB: UUID
) : Event() {
    override fun getHandlers(): HandlerList = handlerList
    companion object {
        @JvmStatic
        val handlerList = HandlerList()
    }
}

/** 好友关系被解除时触发 */
class FriendRemovedEvent(
    val playerA: UUID,
    val playerB: UUID
) : Event() {
    override fun getHandlers(): HandlerList = handlerList
    companion object {
        @JvmStatic
        val handlerList = HandlerList()
    }
}

/** 一名玩家被自动标记为另一名玩家的仇人时触发 */
class EnemyMarkedEvent(
    val victimUUID: UUID,
    val thiefUUID: UUID
) : Event() {
    override fun getHandlers(): HandlerList = handlerList
    companion object {
        @JvmStatic
        val handlerList = HandlerList()
    }
}
