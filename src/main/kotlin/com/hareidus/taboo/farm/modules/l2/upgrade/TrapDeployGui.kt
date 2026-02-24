package com.hareidus.taboo.farm.modules.l2.upgrade

import EasyLib.EasyGui.EasyGuiBuilder.INormalGuiBuilder
import EasyLib.EasyGui.EasyGuiConfig.GuiConfig.GuiConfig
import com.hareidus.taboo.farm.foundation.gui.GuiConfigManager
import com.hareidus.taboo.farm.foundation.gui.GuiNavigator
import com.hareidus.taboo.farm.foundation.gui.MatcherDisplayRenderer
import com.hareidus.taboo.farm.modules.l1.farmlevel.FarmLevelManager
import com.hareidus.taboo.farm.modules.l1.plot.PlotManager
import com.hareidus.taboo.farm.modules.l1.trap.TrapManager
import org.bukkit.entity.Player
import taboolib.module.chat.colored
import taboolib.platform.util.sendLang

/**
 * 陷阱部署 GUI
 *
 * 展示当前陷阱槽位使用情况，空槽位展示下一个可部署陷阱的条件。
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
        val level = FarmLevelManager.getPlayerLevel(uuid)
        val maxSlots = FarmLevelManager.getTrapSlots(level)
        val plot = PlotManager.getPlotByOwner(uuid)
        val deployed = if (plot != null) TrapManager.getDeployedTraps(plot.id) else emptyList()
        val trapInSlot = deployed.find { it.slotIndex == slotIndex }
        val trapDef = trapInSlot?.let { TrapManager.getTrapDefinition(it.trapTypeId) }
        val isLocked = slotIndex >= maxSlots

        // 确定要展示的陷阱（空槽位展示第一个可用类型）
        val allDefs = TrapManager.getAllTrapDefinitions()
        val nextTrapDef = if (trapInSlot == null && !isLocked) allDefs.firstOrNull() else null

        val status = when {
            isLocked -> "&c[未解锁]"
            trapDef != null -> "&a[${trapDef.name}]"
            else -> "&7[空闲]"
        }
        val info = when {
            isLocked -> "&7需要更高农场等级解锁"
            trapDef != null -> "&7陷阱: &f${trapDef.name}\n&7触发率: &f${(trapDef.triggerChance * 100).toInt()}%\n&7惩罚: &f${trapDef.penaltyType.name}"
            nextTrapDef != null -> "&7可部署: &f${nextTrapDef.name}\n&7触发率: &f${(nextTrapDef.triggerChance * 100).toInt()}%"
            else -> "&7无可用陷阱类型"
        }
        val action = when {
            isLocked -> "槽位未解锁"
            trapDef != null -> "已部署"
            nextTrapDef != null -> "点击部署"
            else -> ""
        }
        val conditions = nextTrapDef?.conditions ?: emptyList()

        setIcon(key) { k, itemStack ->
            itemStack.itemMeta = itemStack.itemMeta?.apply {
                setDisplayName(displayName
                    ?.replace("%trap_status%", status)
                    ?.colored())
                // 先替换文本占位符
                lore = lore?.flatMap { line ->
                    if (line.contains("%trap_info%")) {
                        info.split("\n").map { it.colored() }
                    } else {
                        listOf(line.replace("%trap_action%", action).colored())
                    }
                }
                // 再展开 {request}
                lore = MatcherDisplayRenderer.expandRequest(
                    lore ?: emptyList(), conditions, thisPlayer
                )
            }
            getCustomChestImpl().set(k, itemStack) {
                isCancelled = true
                if (isLocked) {
                    thisPlayer.sendLang("trap-gui-slot-locked")
                    return@set
                }
                if (trapInSlot != null) return@set
                if (nextTrapDef == null) {
                    thisPlayer.sendLang("upgrade-trap-no-types")
                    return@set
                }
                UpgradeManager.deployTrap(thisPlayer, nextTrapDef.id, slotIndex)
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
