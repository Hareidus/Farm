package com.hareidus.taboo.farm.modules.l2.upgrade

import EasyLib.EasyGui.EasyGuiBuilder.INormalGuiBuilder
import EasyLib.EasyGui.EasyGuiConfig.GuiConfig.GuiConfig
import com.hareidus.taboo.farm.foundation.gui.GuiConfigManager
import com.hareidus.taboo.farm.foundation.gui.GuiNavigator
import com.hareidus.taboo.farm.modules.l1.farmlevel.FarmLevelManager
import com.hareidus.taboo.farm.modules.l1.plot.PlotManager
import com.hareidus.taboo.farm.modules.l1.trap.TrapManager
import org.bukkit.entity.Player
import taboolib.module.chat.colored
import taboolib.platform.util.sendLang

/**
 * 陷阱部署 GUI
 *
 * 展示当前陷阱槽位使用情况，支持在空槽位部署陷阱。
 * 通过 UpgradeManager 执行陷阱部署操作。
 */
class TrapDeployGui(
    config: GuiConfig,
    player: Player
) : INormalGuiBuilder(config, player) {

    override fun open() {
        GuiNavigator.push(thisPlayer, "trap_deploy") { TrapDeployGui(config, thisPlayer).open() }
        buildAndOpen { }
    }

    override fun mapIconsToFunctions() {
        mapIconsToFunctionWay { key, function ->
            when (function) {
                "info" -> setInfoIcon(key)
                "slot0" -> setSlotIcon(key, 0)
                "slot1" -> setSlotIcon(key, 1)
                "slot2" -> setSlotIcon(key, 2)
                "back" -> setBackIcon(key)
                else -> setDefaultIcon(key)
            }
        }
    }

    // ==================== Icon Handlers ====================

    private fun setInfoIcon(key: Char) {
        val uuid = thisPlayer.uniqueId
        val level = FarmLevelManager.getPlayerLevel(uuid)
        val maxSlots = FarmLevelManager.getTrapSlots(level)
        val plot = PlotManager.getPlotByOwner(uuid)
        val usedSlots = if (plot != null) TrapManager.getDeployedTraps(plot.id).size else 0

        setIcon(key) { k, itemStack ->
            itemStack.itemMeta = itemStack.itemMeta?.apply {
                lore = lore?.map { line ->
                    line.replace("%used%", usedSlots.toString())
                        .replace("%max%", maxSlots.toString())
                        .colored()
                }
                setDisplayName(displayName
                    ?.replace("%used%", usedSlots.toString())
                    ?.replace("%max%", maxSlots.toString())
                    ?.colored())
            }
            getCustomChestImpl().set(k, itemStack) {
                isCancelled = true
            }
        }
    }

    private fun setSlotIcon(key: Char, slotIndex: Int) {
        val uuid = thisPlayer.uniqueId
        val plot = PlotManager.getPlotByOwner(uuid)
        val deployed = if (plot != null) TrapManager.getDeployedTraps(plot.id) else emptyList()
        val trapInSlot = deployed.find { it.slotIndex == slotIndex }
        val trapDef = trapInSlot?.let { TrapManager.getTrapDefinition(it.trapTypeId) }
        val trapName = trapDef?.name ?: ""
        val trapChance = trapDef?.triggerChance?.let { "${(it * 100).toInt()}%" } ?: ""
        val trapPenalty = trapDef?.penaltyType?.name ?: ""

        setIcon(key) { k, itemStack ->
            itemStack.itemMeta = itemStack.itemMeta?.apply {
                lore = lore?.map { line ->
                    line.replace("%trap_name%", trapName)
                        .replace("%trap_chance%", trapChance)
                        .replace("%trap_penalty%", trapPenalty)
                        .replace("%slot_index%", slotIndex.toString())
                        .colored()
                }
                setDisplayName(displayName
                    ?.replace("%trap_name%", trapName)
                    ?.replace("%slot_index%", slotIndex.toString())
                    ?.colored())
            }
            getCustomChestImpl().set(k, itemStack) {
                isCancelled = true
                if (trapInSlot != null) {
                    // Slot occupied, just show info (no action)
                    return@set
                }
                // Empty slot: pick first available trap definition and deploy
                val allDefs = TrapManager.getAllTrapDefinitions()
                if (allDefs.isEmpty()) {
                    thisPlayer.sendLang("upgrade-trap-no-types")
                    return@set
                }
                val selectedDef = allDefs.first()
                UpgradeManager.deployTrap(thisPlayer, selectedDef.id, slotIndex)
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
            val config = GuiConfigManager.trapDeployGuiConfig
            if (config != null) {
                TrapDeployGui(config, player).open()
            } else {
                player.sendLang("gui-config-error")
            }
        }
    }
}
