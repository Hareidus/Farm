package com.hareidus.taboo.farm.modules.l2.upgrade

import EasyLib.EasyGui.EasyGuiBuilder.INormalGuiBuilder
import EasyLib.EasyGui.EasyGuiConfig.GuiConfig.GuiConfig
import com.hareidus.taboo.farm.foundation.gui.GuiConfigManager
import com.hareidus.taboo.farm.foundation.gui.GuiNavigator
import org.bukkit.entity.Player
import taboolib.module.chat.colored
import taboolib.platform.util.sendLang

/**
 * 农场升级 GUI
 *
 * 展示当前等级信息、升级按钮、陷阱入口、返回按钮。
 * 通过 UpgradeManager 查询升级信息并执行升级操作。
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
        val costMoney = info?.nextLevelDef?.upgradeCostMoney?.toString() ?: "-"
        val costMaterials = info?.nextLevelDef?.upgradeCostMaterials
            ?.entries?.joinToString(", ") { "${it.key} x${it.value}" } ?: "-"
        val plotIncrease = info?.nextLevelDef?.plotSizeIncrease?.toString() ?: "-"

        setIcon(key) { k, itemStack ->
            itemStack.itemMeta = itemStack.itemMeta?.apply {
                lore = lore?.map { line ->
                    line.replace("%level%", level)
                        .replace("%max_level%", maxLevel)
                        .replace("%cost_money%", costMoney)
                        .replace("%cost_materials%", costMaterials)
                        .replace("%plot_increase%", plotIncrease)
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
        setIcon(key) { k, itemStack ->
            getCustomChestImpl().set(k, itemStack) {
                isCancelled = true
                val info = UpgradeManager.getUpgradeInfo(thisPlayer.uniqueId)
                if (info != null && info.currentLevel >= info.maxLevel) {
                    thisPlayer.sendLang("upgrade-already-max")
                    return@set
                }
                if (UpgradeManager.canUpgrade(thisPlayer)) {
                    UpgradeManager.performUpgrade(thisPlayer)
                    thisPlayer.closeInventory()
                    UpgradeGui.open(thisPlayer)
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
