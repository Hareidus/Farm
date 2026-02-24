package com.hareidus.taboo.farm.modules.l2.upgrade

import com.hareidus.taboo.farm.modules.l1.economy.EconomyManager
import com.hareidus.taboo.farm.modules.l1.farmlevel.FarmLevelManager
import com.hareidus.taboo.farm.modules.l1.plot.PlotManager
import com.hareidus.taboo.farm.modules.l1.trap.TrapManager
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import taboolib.common.platform.function.warning
import taboolib.platform.util.sendLang
import java.util.UUID

/**
 * 农场升级管理器 (L2)
 *
 * 职责：
 * 1. 编排农场升级流程（校验→扣费→升级→扩地→通知）
 * 2. 编排陷阱部署流程（校验槽位→扣费→部署→通知）
 * 3. 提供升级信息查询供 GUI 使用
 *
 * 依赖: FarmLevelManager, EconomyManager, PlotManager, TrapManager
 * 不直接访问 DatabaseManager。
 */
object UpgradeManager {

    // ==================== 升级信息查询 ====================

    /**
     * 获取玩家的升级信息快照
     *
     * @param uuid 玩家 UUID
     * @return 升级信息，玩家无地块时返回 null
     */
    fun getUpgradeInfo(uuid: UUID): UpgradeInfo? {
        PlotManager.getPlotByOwner(uuid) ?: return null
        val currentLevel = FarmLevelManager.getPlayerLevel(uuid)
        val maxLevel = FarmLevelManager.getMaxLevel()
        val nextLevelDef = FarmLevelManager.getDefinition(currentLevel + 1)
        val canAffordMoney = if (nextLevelDef != null) {
            val player = org.bukkit.Bukkit.getOfflinePlayer(uuid)
            EconomyManager.hasEnough(player, nextLevelDef.upgradeCostMoney)
        } else false

        val canAffordMaterials = if (nextLevelDef != null) {
            val onlinePlayer = org.bukkit.Bukkit.getPlayer(uuid)
            onlinePlayer != null && hasRequiredMaterials(onlinePlayer, nextLevelDef.upgradeCostMaterials)
        } else false

        return UpgradeInfo(
            currentLevel = currentLevel,
            maxLevel = maxLevel,
            nextLevelDef = nextLevelDef,
            canAffordMoney = canAffordMoney,
            canAffordMaterials = canAffordMaterials
        )
    }

    // ==================== 升级校验 ====================

    /**
     * 检查玩家是否满足升级条件（金币 + 材料）
     */
    fun canUpgrade(player: Player): Boolean {
        val uuid = player.uniqueId
        val currentLevel = FarmLevelManager.getPlayerLevel(uuid)
        val maxLevel = FarmLevelManager.getMaxLevel()
        if (currentLevel >= maxLevel) return false

        val nextDef = FarmLevelManager.getDefinition(currentLevel + 1) ?: return false
        if (!EconomyManager.hasEnough(player, nextDef.upgradeCostMoney)) return false
        if (!hasRequiredMaterials(player, nextDef.upgradeCostMaterials)) return false
        return true
    }

    // ==================== 执行升级 ====================

    /**
     * 执行农场升级
     *
     * 流程: 校验条件 → 扣除金币 → 扣除材料 → 更新等级 → 扩展地块 → 发布事件 → 通知玩家
     * @return 是否升级成功
     */
    fun performUpgrade(player: Player): Boolean {
        val uuid = player.uniqueId
        val currentLevel = FarmLevelManager.getPlayerLevel(uuid)
        val maxLevel = FarmLevelManager.getMaxLevel()

        if (currentLevel >= maxLevel) {
            player.sendLang("upgrade-already-max")
            return false
        }

        val nextDef = FarmLevelManager.getDefinition(currentLevel + 1)
        if (nextDef == null) {
            player.sendLang("upgrade-config-error")
            return false
        }

        // 校验金币
        if (!EconomyManager.hasEnough(player, nextDef.upgradeCostMoney)) {
            player.sendLang("upgrade-not-enough-money", nextDef.upgradeCostMoney)
            return false
        }

        // 校验材料
        if (!hasRequiredMaterials(player, nextDef.upgradeCostMaterials)) {
            player.sendLang("upgrade-not-enough-materials")
            return false
        }

        // 扣除金币
        if (!EconomyManager.withdraw(player, nextDef.upgradeCostMoney)) {
            player.sendLang("upgrade-withdraw-failed")
            return false
        }

        // 扣除材料
        removeRequiredMaterials(player, nextDef.upgradeCostMaterials)
        // 更新等级
        val newLevel = currentLevel + 1
        FarmLevelManager.setPlayerLevel(uuid, newLevel)

        // 扩展地块
        val plot = PlotManager.getPlotByOwner(uuid)
        if (plot != null && nextDef.plotSizeIncrease > 0) {
            PlotManager.expandPlot(plot.id, nextDef.plotSizeIncrease)
        }

        // 收集解锁功能列表
        val unlocked = buildUnlockedFeatures(currentLevel, newLevel)

        // 发布事件
        FarmUpgradedEvent(player, currentLevel, newLevel, unlocked).call()

        // 通知玩家
        player.sendLang("upgrade-success", newLevel)
        return true
    }

    // ==================== 陷阱部署 ====================

    /**
     * 检查玩家是否有可用的陷阱槽位
     */
    fun canDeployTrap(uuid: UUID): Boolean {
        val plot = PlotManager.getPlotByOwner(uuid) ?: return false
        val level = FarmLevelManager.getPlayerLevel(uuid)
        val maxSlots = FarmLevelManager.getTrapSlots(level)
        val deployed = TrapManager.getDeployedTraps(plot.id)
        return deployed.size < maxSlots
    }

    /**
     * 部署陷阱到指定槽位
     *
     * 流程: 校验槽位 → 校验陷阱定义 → 校验消耗 → 扣费 → 部署 → 发布事件 → 通知
     * @return 是否部署成功
     */
    fun deployTrap(player: Player, trapTypeId: String, slotIndex: Int): Boolean {
        val uuid = player.uniqueId
        val plot = PlotManager.getPlotByOwner(uuid)
        if (plot == null) {
            player.sendLang("upgrade-no-plot")
            return false
        }

        // 校验槽位上限
        val level = FarmLevelManager.getPlayerLevel(uuid)
        val maxSlots = FarmLevelManager.getTrapSlots(level)
        if (slotIndex < 0 || slotIndex >= maxSlots) {
            player.sendLang("upgrade-trap-slot-invalid", maxSlots)
            return false
        }

        // 校验槽位是否已占用
        val deployed = TrapManager.getDeployedTraps(plot.id)
        if (deployed.any { it.slotIndex == slotIndex }) {
            player.sendLang("upgrade-trap-slot-occupied", slotIndex)
            return false
        }

        // 校验陷阱定义
        val trapDef = TrapManager.getTrapDefinition(trapTypeId)
        if (trapDef == null) {
            player.sendLang("upgrade-trap-type-invalid")
            return false
        }

        // 校验金币
        if (trapDef.deployCostMoney > 0 && !EconomyManager.hasEnough(player, trapDef.deployCostMoney)) {
            player.sendLang("upgrade-trap-not-enough-money", trapDef.deployCostMoney)
            return false
        }

        // 校验材料
        if (!hasRequiredMaterials(player, trapDef.deployCostMaterials)) {
            player.sendLang("upgrade-trap-not-enough-materials")
            return false
        }

        // 扣除金币
        if (trapDef.deployCostMoney > 0) {
            if (!EconomyManager.withdraw(player, trapDef.deployCostMoney)) {
                player.sendLang("upgrade-withdraw-failed")
                return false
            }
        }

        // 扣除材料
        removeRequiredMaterials(player, trapDef.deployCostMaterials)

        // 部署陷阱
        val success = TrapManager.deployTrap(plot.id, trapTypeId, slotIndex)
        if (!success) {
            warning("[Farm] 陷阱部署写入失败: plot=${plot.id}, type=$trapTypeId, slot=$slotIndex")
            player.sendLang("upgrade-trap-deploy-failed")
            return false
        }

        // 发布事件
        TrapDeployedEvent(uuid, trapTypeId, slotIndex).call()

        // 通知玩家
        player.sendLang("upgrade-trap-deployed", trapDef.name, slotIndex)
        return true
    }

    // ==================== 内部工具方法 ====================

    /**
     * 检查玩家背包是否包含所需材料
     */
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

    /**
     * 从玩家背包扣除所需材料
     */
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

    /**
     * 对比新旧等级，收集本次升级解锁的功能名称列表
     */
    private fun buildUnlockedFeatures(oldLevel: Int, newLevel: Int): List<String> {
        val oldDef = FarmLevelManager.getDefinition(oldLevel)
        val newDef = FarmLevelManager.getDefinition(newLevel) ?: return emptyList()
        val features = mutableListOf<String>()

        val oldSlots = oldDef?.trapSlots ?: 0
        if (newDef.trapSlots > oldSlots) {
            features.add("trap_slots:${newDef.trapSlots}")
        }

        val oldDeco = oldDef?.decorationSlots ?: 0
        if (newDef.decorationSlots > oldDeco) {
            features.add("decoration_slots:${newDef.decorationSlots}")
        }

        val oldProtection = oldDef?.protectionLevel ?: 0
        if (newDef.protectionLevel > oldProtection) {
            features.add("protection:${newDef.protectionLevel}")
        }

        if (newDef.autoHarvestUnlocked && oldDef?.autoHarvestUnlocked != true) {
            features.add("auto_harvest")
        }

        if (newDef.plotSizeIncrease > 0) {
            features.add("plot_expand:${newDef.plotSizeIncrease}")
        }

        return features
    }
}
