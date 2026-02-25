package com.hareidus.taboo.farm.modules.l2.guardpet

import EasyLib.EasyGui.EasyGuiBuilder.INormalGuiBuilder
import EasyLib.EasyGui.EasyGuiConfig.GuiConfig.GuiConfig
import com.hareidus.taboo.farm.foundation.gui.GuiConfigManager
import com.hareidus.taboo.farm.foundation.gui.GuiNavigator
import com.hareidus.taboo.farm.foundation.gui.MatcherDisplayRenderer
import com.hareidus.taboo.farm.foundation.api.events.PreGuardPetDeployEvent
import com.hareidus.taboo.farm.foundation.sound.SoundManager
import com.hareidus.taboo.farm.modules.l1.economy.EconomyManager
import com.hareidus.taboo.farm.modules.l1.farmlevel.FarmLevelManager
import com.hareidus.taboo.farm.modules.l1.guardpet.GuardPetManager
import com.hareidus.taboo.farm.modules.l1.plot.PlotManager
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import taboolib.module.chat.colored
import taboolib.platform.util.sendLang

/**
 * 看门宠物 GUI
 *
 * 展示宠物信息、部署/升级按钮（条件渲染）。
 */
class GuardPetGui(
    config: GuiConfig,
    player: Player
) : INormalGuiBuilder(config, player) {

    override fun open() {
        GuiNavigator.push(thisPlayer, "guard_pet") { GuardPetGui(config, thisPlayer).open() }
        buildAndOpen { }
    }

    override fun mapIconsToFunctions() {
        mapIconsToFunctionWay { key, function ->
            when (function) {
                "info" -> setInfoIcon(key)
                "pet" -> setPetIcon(key)
                "back" -> setBackIcon(key)
                else -> setDefaultIcon(key)
            }
        }
    }

    // ==================== Icon Handlers ====================

    private fun setInfoIcon(key: Char) {
        val uuid = thisPlayer.uniqueId
        val level = FarmLevelManager.getPlayerLevel(uuid)
        val maxSlots = FarmLevelManager.getGuardPetSlots(level)
        val plot = PlotManager.getPlotByOwner(uuid)
        val hasPet = if (plot != null) GuardPetManager.getDeployedPet(plot.id) != null else false
        val used = if (hasPet) 1 else 0

        setIcon(key) { k, itemStack ->
            itemStack.itemMeta = itemStack.itemMeta?.apply {
                lore = lore?.map { line ->
                    line.replace("%used%", used.toString())
                        .replace("%max%", maxSlots.toString())
                        .colored()
                }
                setDisplayName(displayName
                    ?.replace("%used%", used.toString())
                    ?.replace("%max%", maxSlots.toString())
                    ?.colored())
            }
            getCustomChestImpl().set(k, itemStack) {
                isCancelled = true
            }
        }
    }

    private fun setPetIcon(key: Char) {
        val uuid = thisPlayer.uniqueId
        val level = FarmLevelManager.getPlayerLevel(uuid)
        val maxSlots = FarmLevelManager.getGuardPetSlots(level)
        val plot = PlotManager.getPlotByOwner(uuid)
        val deployedPet = if (plot != null) GuardPetManager.getDeployedPet(plot.id) else null
        val petDef = deployedPet?.let { GuardPetManager.getDefinition(it.petTypeId) }
        val levelDef = deployedPet?.let { GuardPetManager.getLevelDef(it.petTypeId, it.level) }
        val isDeployed = deployedPet != null
        val hasSlot = maxSlots > 0
        val isMaxLevel = petDef != null && deployedPet != null && deployedPet.level >= petDef.maxLevel

        val cfg = config as GuardPetGuiConfig

        val subPlaceholders = mutableMapOf(
            "%pet_level%" to (deployedPet?.level?.toString() ?: "0"),
            "%pet_max_level%" to (petDef?.maxLevel?.toString() ?: "5"),
            "%detect_chance%" to (levelDef?.let { (it.detectChance * 100).toInt().toString() } ?: "0"),
            "%patrol_radius%" to (levelDef?.patrolRadius?.toString() ?: "0")
        )

        fun String.applySub(): String {
            var result = this
            subPlaceholders.forEach { (k, v) -> result = result.replace(k, v) }
            return result
        }

        val status = when {
            isDeployed -> cfg.statusDeployed.applySub()
            else -> cfg.statusNone.applySub()
        }
        val infoLines = when {
            !hasSlot -> cfg.infoNoSlot.map { it.applySub() }
            isDeployed -> cfg.infoDeployed.map { it.applySub() }
            else -> cfg.infoCanDeploy.map { it.applySub() }
        }
        val action = when {
            !hasSlot -> cfg.actionNoSlot
            isDeployed && isMaxLevel -> cfg.actionDeployedMax
            isDeployed -> cfg.actionDeployed
            hasSlot -> cfg.actionCanDeploy
            else -> cfg.actionNone
        }

        // 条件列表：部署条件或升级条件
        val conditions = when {
            !hasSlot -> emptyList()
            isDeployed && !isMaxLevel -> {
                val nextLevel = deployedPet!!.level + 1
                GuardPetManager.getLevelDef(deployedPet.petTypeId, nextLevel)?.upgradeConditions ?: emptyList()
            }
            !isDeployed && hasSlot -> {
                val firstDef = GuardPetManager.getAllDefinitions().firstOrNull()
                firstDef?.deployConditions ?: emptyList()
            }
            else -> emptyList()
        }

        setIcon(key) { k, itemStack ->
            itemStack.itemMeta = itemStack.itemMeta?.apply {
                setDisplayName(displayName
                    ?.replace("%pet_status%", status)
                    ?.colored())
                lore = lore?.flatMap { line ->
                    if (line.contains("%pet_info%")) {
                        infoLines.map { it.colored() }
                    } else {
                        listOf(line.replace("%pet_action%", action).colored())
                    }
                }
                lore = MatcherDisplayRenderer.expandRequest(
                    lore ?: emptyList(), conditions, thisPlayer
                )
            }
            getCustomChestImpl().set(k, itemStack) {
                isCancelled = true
                if (!hasSlot) {
                    thisPlayer.sendLang("guardpet-no-slots")
                    return@set
                }
                if (isDeployed) {
                    if (isMaxLevel) {
                        thisPlayer.sendLang("guardpet-max-level")
                        return@set
                    }
                    // 升级宠物
                    handleUpgrade(plot!!.id, deployedPet!!)
                } else {
                    // 部署宠物
                    handleDeploy(plot!!.id)
                }
            }
        }
    }

    private fun handleDeploy(plotId: Long) {
        val firstDef = GuardPetManager.getAllDefinitions().firstOrNull()
        if (firstDef == null) {
            thisPlayer.sendLang("guardpet-no-types")
            return
        }

        // 校验金币
        if (firstDef.deployCostMoney > 0 && !EconomyManager.hasEnough(thisPlayer, firstDef.deployCostMoney)) {
            thisPlayer.sendLang("guardpet-not-enough-money", firstDef.deployCostMoney)
            return
        }

        // 校验材料
        if (!hasRequiredMaterials(thisPlayer, firstDef.deployCostMaterials)) {
            thisPlayer.sendLang("guardpet-not-enough-materials")
            return
        }

        // 触发部署前事件（可被外部插件取消）
        val preDeployEvent = PreGuardPetDeployEvent(thisPlayer, plotId, firstDef.id)
        preDeployEvent.call()
        if (preDeployEvent.isCancelled) return

        // 扣除金币
        if (firstDef.deployCostMoney > 0) {
            if (!EconomyManager.withdraw(thisPlayer, firstDef.deployCostMoney)) {
                thisPlayer.sendLang("guardpet-withdraw-failed")
                return
            }
        }

        // 扣除材料
        removeRequiredMaterials(thisPlayer, firstDef.deployCostMaterials)

        val success = GuardPetManager.deployPet(plotId, thisPlayer.uniqueId, firstDef.id)
        if (success) {
            thisPlayer.sendLang("guardpet-deploy-success")
            SoundManager.play(thisPlayer, "guardpet-deploy")
            GuardPetEntityManager.spawnPetForPlot(plotId)
            GuardPetDeployedEvent(thisPlayer.uniqueId, plotId, firstDef.id).call()
        }
        thisPlayer.closeInventory()
        GuardPetGui.open(thisPlayer)
    }

    private fun handleUpgrade(plotId: Long, pet: com.hareidus.taboo.farm.foundation.model.DeployedGuardPet) {
        val nextLevel = pet.level + 1
        val levelDef = GuardPetManager.getLevelDef(pet.petTypeId, nextLevel)
        if (levelDef == null) {
            thisPlayer.sendLang("guardpet-max-level")
            return
        }

        // 校验金币
        if (levelDef.upgradeCostMoney > 0 && !EconomyManager.hasEnough(thisPlayer, levelDef.upgradeCostMoney)) {
            thisPlayer.sendLang("guardpet-not-enough-money", levelDef.upgradeCostMoney)
            return
        }

        // 校验材料
        if (!hasRequiredMaterials(thisPlayer, levelDef.upgradeCostMaterials)) {
            thisPlayer.sendLang("guardpet-not-enough-materials")
            return
        }

        // 扣除金币
        if (levelDef.upgradeCostMoney > 0) {
            if (!EconomyManager.withdraw(thisPlayer, levelDef.upgradeCostMoney)) {
                thisPlayer.sendLang("guardpet-withdraw-failed")
                return
            }
        }

        // 扣除材料
        removeRequiredMaterials(thisPlayer, levelDef.upgradeCostMaterials)

        val oldLevel = pet.level
        val success = GuardPetManager.upgradePet(plotId)
        if (success) {
            thisPlayer.sendLang("guardpet-upgrade-success")
            SoundManager.play(thisPlayer, "guardpet-upgrade")
            GuardPetEntityManager.despawnPetForPlot(plotId)
            GuardPetEntityManager.spawnPetForPlot(plotId)
            GuardPetUpgradedEvent(thisPlayer.uniqueId, plotId, oldLevel, oldLevel + 1).call()
        }
        thisPlayer.closeInventory()
        GuardPetGui.open(thisPlayer)
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

    // ==================== 材料校验工具 ====================

    private fun hasRequiredMaterials(player: Player, materials: Map<String, Int>): Boolean {
        for ((materialId, requiredAmount) in materials) {
            if (requiredAmount <= 0) continue
            val material = Material.matchMaterial(materialId) ?: return false
            if (!player.inventory.containsAtLeast(ItemStack(material), requiredAmount)) {
                return false
            }
        }
        return true
    }

    private fun removeRequiredMaterials(player: Player, materials: Map<String, Int>) {
        for ((materialId, requiredAmount) in materials) {
            if (requiredAmount <= 0) continue
            val material = Material.matchMaterial(materialId) ?: continue
            var remaining = requiredAmount
            for (item in player.inventory.contents) {
                if (item == null || item.type != material) continue
                if (item.amount <= remaining) {
                    remaining -= item.amount
                    item.amount = 0
                } else {
                    item.amount -= remaining
                    remaining = 0
                }
                if (remaining <= 0) break
            }
        }
    }

    companion object {
        fun open(player: Player) {
            val config = GuiConfigManager.guardPetGuiConfig
            if (config != null) {
                GuardPetGui(config, player).open()
            } else {
                player.sendLang("gui-config-error")
            }
        }
    }
}
