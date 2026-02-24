package com.hareidus.taboo.farm.modules.l2.upgrade

import EasyLib.EasyGui.EasyGuiBuilder.INormalGuiBuilder
import EasyLib.EasyGui.EasyGuiConfig.GuiConfig.GuiConfig
import com.hareidus.taboo.farm.foundation.gui.GuiConfigManager
import com.hareidus.taboo.farm.foundation.gui.GuiNavigator
import com.hareidus.taboo.farm.foundation.gui.MatcherDisplayRenderer
import com.hareidus.taboo.farm.modules.l1.farmlevel.FarmLevelManager
import org.bukkit.entity.Player
import taboolib.module.chat.colored
import taboolib.platform.util.asLangText
import taboolib.platform.util.sendLang

/**
 * 农场升级 GUI
 *
 * 展示当前等级信息、升级条件（Matcher 渲染）、陷阱入口、返回按钮。
 */
class UpgradeGui(
    config: GuiConfig,
    player: Player
) : INormalGuiBuilder(config, player) {

    override fun open() {
        GuiNavigator.push(thisPlayer, "upgrade") { UpgradeGui(config, thisPlayer).open() }
        buildAndOpen { }
    }

    override fun mapIconsToFunctions() {
        mapIconsToFunctionWay { key, function ->
            when (function) {
                "info" -> setInfoIcon(key)
                "upgrade" -> setUpgradeIcon(key)
                "traps" -> setTrapsIcon(key)
                "back" -> setBackIcon(key)
                else -> setDefaultIcon(key)
            }
        }
    }

    // ==================== Icon Handlers ====================

    private fun setInfoIcon(key: Char) {
        val info = UpgradeManager.getUpgradeInfo(thisPlayer.uniqueId)
        val level = info?.currentLevel?.toString() ?: "0"
        val maxLevel = info?.maxLevel?.toString() ?: "?"
        val currentDef = info?.let { FarmLevelManager.getDefinition(it.currentLevel) }
        val plotIncrease = info?.nextLevelDef?.plotSizeIncrease?.toString() ?: "-"
        val trapSlots = currentDef?.trapSlots?.toString() ?: "0"
        val protection = currentDef?.protectionLevel?.toString() ?: "0"
        val autoHarvest = if (currentDef?.autoHarvestUnlocked == true) "&a已解锁" else "&c未解锁"

        setIcon(key) { k, itemStack ->
            itemStack.itemMeta = itemStack.itemMeta?.apply {
                lore = lore?.map { line ->
                    line.replace("%level%", level)
                        .replace("%max_level%", maxLevel)
                        .replace("%plot_increase%", plotIncrease)
                        .replace("%trap_slots%", trapSlots)
                        .replace("%protection%", protection)
                        .replace("%auto_harvest%", autoHarvest)
                        .colored()
                }
                setDisplayName(displayName
                    ?.replace("%level%", level)
                    ?.replace("%max_level%", maxLevel)
                    ?.colored())
            }
            getCustomChestImpl().set(k, itemStack) {
                isCancelled = true
            }
        }
    }

    private fun setUpgradeIcon(key: Char) {
        val info = UpgradeManager.getUpgradeInfo(thisPlayer.uniqueId)
        val nextLevel = if (info != null) (info.currentLevel + 1).toString() else "?"
        val matchers = info?.nextLevelDef?.conditions ?: emptyList()
        val isMaxLevel = info != null && info.currentLevel >= info.maxLevel

        setIcon(key) { k, itemStack ->
            itemStack.itemMeta = itemStack.itemMeta?.apply {
                setDisplayName(displayName
                    ?.replace("%next_level%", nextLevel)
                    ?.colored())
                lore = if (isMaxLevel) {
                    listOf(thisPlayer.asLangText("request-display-max-level").colored())
                } else {
                    MatcherDisplayRenderer.expandRequest(
                        lore ?: emptyList(), matchers, thisPlayer
                    )
                }
            }
            getCustomChestImpl().set(k, itemStack) {
                isCancelled = true
                if (isMaxLevel) {
                    thisPlayer.sendLang("upgrade-already-max")
                    return@set
                }
                if (UpgradeManager.canUpgrade(thisPlayer)) {
                    UpgradeManager.performUpgrade(thisPlayer)
                    thisPlayer.closeInventory()
                    UpgradeGui.open(thisPlayer)
                } else {
                    thisPlayer.sendLang("upgrade-gui-cannot-upgrade")
                }
            }
        }
    }

    private fun setTrapsIcon(key: Char) {
        setIcon(key) { k, itemStack ->
            getCustomChestImpl().set(k, itemStack) {
                isCancelled = true
                thisPlayer.closeInventory()
                TrapDeployGui.open(thisPlayer)
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
            val config = GuiConfigManager.upgradeGuiConfig
            if (config != null) {
                UpgradeGui(config, player).open()
            } else {
                player.sendLang("gui-config-error")
            }
        }
    }
}
