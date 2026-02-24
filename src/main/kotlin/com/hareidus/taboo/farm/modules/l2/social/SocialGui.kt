package com.hareidus.taboo.farm.modules.l2.social

import EasyLib.EasyGui.EasyGuiBuilder.INormalGuiBuilder
import EasyLib.EasyGui.EasyGuiConfig.GuiConfig.GuiConfig
import com.hareidus.taboo.farm.foundation.gui.GuiConfigManager
import com.hareidus.taboo.farm.foundation.gui.GuiNavigator
import com.hareidus.taboo.farm.foundation.model.EnemyRecord
import com.hareidus.taboo.farm.foundation.model.FriendRelation
import com.hareidus.taboo.farm.foundation.model.FriendRequest
import com.hareidus.taboo.farm.modules.l1.social.SocialManager
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import taboolib.module.chat.colored
import taboolib.platform.util.sendLang

/**
 * 社交系统 GUI (L2)
 *
 * 功能：
 * - friends: 展示好友数量，点击列出好友名单
 * - enemies: 展示仇人数量，点击列出仇人名单
 * - requests: 展示待处理请求数量，点击自动接受第一个请求
 * - back: 返回上一级
 */
class SocialGui(
    config: GuiConfig,
    player: Player
) : INormalGuiBuilder(config, player) {

    private val friends: List<FriendRelation> = SocialManager.getFriends(thisPlayer.uniqueId)
    private val enemies: List<EnemyRecord> = SocialManager.getEnemies(thisPlayer.uniqueId)
    private val pendingRequests: List<FriendRequest> = SocialManager.getPendingRequests(thisPlayer.uniqueId)

    override fun open() {
        GuiNavigator.push(thisPlayer, "social-gui") { SocialGui(config, thisPlayer).open() }
        buildAndOpen { }
    }

    override fun mapIconsToFunctions() {
        mapIconsToFunctionWay { key, function ->
            when (function) {
                "friends" -> setFriendsIcon(key)
                "enemies" -> setEnemiesIcon(key)
                "requests" -> setRequestsIcon(key)
                "back" -> setBackIcon(key)
                else -> setDefaultIcon(key)
            }
        }
    }

    // ==================== Icon Handlers ====================

    private fun setFriendsIcon(key: Char) {
        setIcon(key) { k, itemStack ->
            itemStack.itemMeta = itemStack.itemMeta?.apply {
                lore = lore?.map { line ->
                    line.replace("%count%", friends.size.toString()).colored()
                }
            }
            getCustomChestImpl().set(k, itemStack) {
                isCancelled = true
                thisPlayer.closeInventory()
                if (friends.isEmpty()) {
                    thisPlayer.sendLang("social-gui-friend-list-empty")
                } else {
                    thisPlayer.sendLang("social-gui-friend-list-header")
                    friends.forEach { relation ->
                        val friendUUID = if (relation.playerA == thisPlayer.uniqueId) relation.playerB else relation.playerA
                        val friendName = Bukkit.getOfflinePlayer(friendUUID).name ?: friendUUID.toString()
                        thisPlayer.sendLang("social-gui-friend-entry", friendName)
                    }
                }
            }
        }
    }

    private fun setEnemiesIcon(key: Char) {
        setIcon(key) { k, itemStack ->
            itemStack.itemMeta = itemStack.itemMeta?.apply {
                lore = lore?.map { line ->
                    line.replace("%count%", enemies.size.toString()).colored()
                }
            }
            getCustomChestImpl().set(k, itemStack) {
                isCancelled = true
                thisPlayer.closeInventory()
                if (enemies.isEmpty()) {
                    thisPlayer.sendLang("social-gui-enemy-list-empty")
                } else {
                    thisPlayer.sendLang("social-gui-enemy-list-header")
                    enemies.forEach { record ->
                        val enemyName = Bukkit.getOfflinePlayer(record.thiefUUID).name ?: record.thiefUUID.toString()
                        thisPlayer.sendLang("social-gui-enemy-entry", enemyName)
                    }
                }
            }
        }
    }

    private fun setRequestsIcon(key: Char) {
        setIcon(key) { k, itemStack ->
            itemStack.itemMeta = itemStack.itemMeta?.apply {
                lore = lore?.map { line ->
                    line.replace("%count%", pendingRequests.size.toString()).colored()
                }
            }
            getCustomChestImpl().set(k, itemStack) {
                isCancelled = true
                if (pendingRequests.isEmpty()) {
                    thisPlayer.sendLang("social-gui-no-pending-requests")
                } else {
                    val first = pendingRequests.first()
                    val accepted = SocialManager.acceptFriendRequest(first.id, thisPlayer.uniqueId)
                    val senderName = Bukkit.getOfflinePlayer(first.senderUUID).name ?: first.senderUUID.toString()
                    if (accepted) {
                        thisPlayer.sendLang("social-gui-request-accepted", senderName)
                    } else {
                        thisPlayer.sendLang("social-gui-request-failed", senderName)
                    }
                    // Refresh GUI to update counts
                    thisPlayer.closeInventory()
                    SocialGui(config, thisPlayer).open()
                }
            }
        }
    }

    private fun setBackIcon(key: Char) {
        setIcon(key) { k, itemStack ->
            getCustomChestImpl().set(k, itemStack) {
                isCancelled = true
                thisPlayer.closeInventory()
                if (!GuiNavigator.back(thisPlayer)) {
                    thisPlayer.sendLang("gui-back")
                }
            }
        }
    }

    companion object {
        fun open(player: Player) {
            val config = GuiConfigManager.socialGuiConfig
            if (config != null) {
                SocialGui(config, player).open()
            } else {
                player.sendLang("gui-config-error")
            }
        }
    }
}
