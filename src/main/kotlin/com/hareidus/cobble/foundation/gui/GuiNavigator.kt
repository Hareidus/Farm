package com.hareidus.cobble.foundation.gui

import org.bukkit.entity.Player
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.Stack

/**
 * GUI 导航管理器
 * 使用栈结构存储玩家的 GUI 访问历史，支持返回上一级
 */
object GuiNavigator {

    /** 玩家 GUI 历史栈 */
    private val historyStacks = ConcurrentHashMap<UUID, Stack<GuiEntry>>()

    /** 当前处于 GUI 中的玩家集合 */
    private val activeGuiPlayers = ConcurrentHashMap.newKeySet<UUID>()

    fun isInGui(player: Player): Boolean = player.uniqueId in activeGuiPlayers

    data class GuiEntry(
        val guiId: String,
        val opener: () -> Unit
    )

    fun push(player: Player, guiId: String, opener: () -> Unit) {
        val stack = historyStacks.getOrPut(player.uniqueId) { Stack() }
        // 如果栈顶已经是同一类型的界面，不重复入栈（刷新场景）
        if (stack.isNotEmpty() && stack.peek().guiId == guiId) {
            return
        }
        stack.push(GuiEntry(guiId, opener))
        activeGuiPlayers.add(player.uniqueId)
    }

    fun back(player: Player): Boolean {
        val stack = historyStacks[player.uniqueId] ?: return false
        if (stack.isNotEmpty()) {
            stack.pop()
        }
        if (stack.isNotEmpty()) {
            val previous = stack.pop()
            previous.opener()
            return true
        }
        activeGuiPlayers.remove(player.uniqueId)
        return false
    }

    fun clear(player: Player) {
        historyStacks.remove(player.uniqueId)
        activeGuiPlayers.remove(player.uniqueId)
    }

    fun depth(player: Player): Int {
        return historyStacks[player.uniqueId]?.size ?: 0
    }

    fun onPlayerQuit(player: Player) {
        historyStacks.remove(player.uniqueId)
        activeGuiPlayers.remove(player.uniqueId)
    }
}
