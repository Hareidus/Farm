package com.hareidus.taboo.farm.modules.l2.leaderboard

import EasyLib.EasyGui.EasyGuiBuilder.IPageableGuiBuilder
import EasyLib.EasyGui.EasyGuiConfig.GuiConfig.GuiConfig
import com.hareidus.taboo.farm.foundation.gui.GuiConfigManager
import com.hareidus.taboo.farm.foundation.gui.GuiNavigator
import com.hareidus.taboo.farm.foundation.model.LeaderboardEntry
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.inventory.meta.SkullMeta
import taboolib.library.xseries.XMaterial
import taboolib.module.chat.colored
import taboolib.platform.util.buildItem
import taboolib.platform.util.sendLang

/**
 * 排行榜分页 GUI (L2)
 *
 * 展示指定类别的排行榜数据，支持翻页、切换类别、返回。
 */
class LeaderboardGui(
    config: GuiConfig,
    player: Player,
    private val category: String
) : IPageableGuiBuilder<LeaderboardEntry>(config, player) {

    override fun open() {
        GuiNavigator.push(thisPlayer, "leaderboard_$category") {
            LeaderboardGui(config, thisPlayer, category).open()
        }
        buildAndOpen {
            chestImpl.onClick { event, element ->
                event.isCancelled = true
            }
        }
    }

    override fun setupElement() {
        chestImpl.elements {
            LeaderboardManager.getLeaderboard(category, 100)
        }
    }

    override fun elementGenerateItem() {
        chestImpl.onGenerate { player, element, index, slot ->
            val categoryName = LeaderboardManager.getCategoryDisplayName(category)
            buildItem(XMaterial.PLAYER_HEAD) {
                name = "&f#${element.rank} ${element.playerName}".colored()
                lore.addAll(listOf(
                    "&7类别: &f$categoryName".colored(),
                    "&7数值: &f${element.value}".colored()
                ))
                skullOwner = element.playerName
            }
        }
    }

    override fun mapIconsToFunctions() {
        mapIconsToFunctionWay { key, function ->
            when (function) {
                "item" -> elementSlotByKey(key)
                "next" -> setNextIcon(key)
                "last" -> setLastIcon(key)
                "back" -> setBackIcon(key)
                "category" -> setCategoryIcon(key)
                else -> setDefaultIcon(key)
            }
        }
    }

    private fun setCategoryIcon(key: Char) {
        val displayName = LeaderboardManager.getCategoryDisplayName(category)
        setIcon(key) { k, itemStack ->
            itemStack.itemMeta = itemStack.itemMeta?.apply {
                lore = lore?.map { line ->
                    line.replace("%category%", displayName).colored()
                }
                setDisplayName(this.displayName
                    ?.replace("%category%", displayName)
                    ?.colored())
            }
            getCustomChestImpl().set(k, itemStack) {
                isCancelled = true
                val categories = LeaderboardManager.getCategories().toList()
                if (categories.isEmpty()) return@set
                val currentIndex = categories.indexOf(category)
                val nextIndex = (currentIndex + 1) % categories.size
                thisPlayer.closeInventory()
                LeaderboardGui.open(thisPlayer, categories[nextIndex])
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
        fun open(player: Player, category: String = "harvest") {
            val config = GuiConfigManager.leaderboardGuiConfig
            if (config != null) {
                LeaderboardGui(config, player, category).open()
            } else {
                player.sendLang("gui-config-error")
            }
        }
    }
}
