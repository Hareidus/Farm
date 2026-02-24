package com.hareidus.taboo.farm.modules.l2.shop

import com.hareidus.taboo.farm.foundation.model.StatisticType
import com.hareidus.taboo.farm.modules.l1.crop.CropManager
import com.hareidus.taboo.farm.modules.l1.economy.EconomyManager
import com.hareidus.taboo.farm.modules.l1.playerdata.PlayerDataManager
import org.bukkit.Material
import org.bukkit.entity.Player
import taboolib.common.platform.function.warning
import taboolib.module.chat.colored
import taboolib.module.configuration.Config
import taboolib.module.configuration.Configuration
import taboolib.platform.util.sendLang

/**
 * 商店管理器 (L2)
 *
 * 职责：
 * 1. 提供 sellCrops API 供 GUI 或命令调用
 * 2. 扫描玩家背包中可出售的作物（匹配 harvestItemId）
 * 3. 计算出售总价（数量 x EconomyManager.getCropPrice）
 * 4. 通过 EconomyManager.deposit 发放金币
 * 5. 扣除背包中对应作物物品
 * 6. 累加 PlayerDataManager.updateStatistic(TOTAL_COIN_INCOME)
 * 7. 发布 CropSoldEvent
 *
 * 依赖: EconomyManager, PlayerDataManager, CropManager
 */
object ShopManager {

    @Config("modules/l2/shop.yml", autoReload = true)
    lateinit var config: Configuration
        private set

    // ==================== 配置读取 ====================

    /** 收购NPC自定义名称 */
    private val npcName: String
        get() = config.getString("npc-name", "&6[收购站]")!!.colored()

    /** 单次最大出售数量 */
    private val maxSellAmount: Int
        get() = config.getInt("max-sell-amount", 64)

    // ==================== NPC 识别 ====================

    /**
     * 判断实体是否为收购站NPC（通过自定义名称匹配）
     */
    fun isShopNpc(entityCustomName: String?): Boolean {
        if (entityCustomName == null) return false
        return entityCustomName == npcName
    }

    // ==================== 可出售作物查询 ====================

    /**
     * 扫描玩家背包中可出售的作物及数量
     *
     * @return Map<cropTypeId, amount>
     */
    fun getSellableCrops(player: Player): Map<String, Int> {
        val result = mutableMapOf<String, Int>()
        val allDefs = CropManager.getAllCropDefinitions()
        val harvestIdToCropId = allDefs.associate { it.harvestItemId to it.id }

        for (item in player.inventory.contents) {
            if (item == null || item.type == Material.AIR) continue
            val materialName = item.type.name
            val cropTypeId = harvestIdToCropId[materialName] ?: continue
            // 只列出有收购价格的作物
            EconomyManager.getCropPrice(cropTypeId) ?: continue
            result[cropTypeId] = (result[cropTypeId] ?: 0) + item.amount
        }
        return result
    }

    // ==================== 核心出售 API ====================

    /**
     * 出售指定作物
     *
     * 流程: 校验参数 → 校验背包 → 计算总价 → 发放金币 → 扣除物品 → 累加统计 → 发布事件
     *
     * @param player 出售者
     * @param cropTypeId 作物类型 ID
     * @param amount 出售数量
     * @return 是否出售成功
     */
    fun sellCrops(player: Player, cropTypeId: String, amount: Int): Boolean {
        val uuid = player.uniqueId

        // 校验 Vault
        if (!EconomyManager.isVaultAvailable) {
            player.sendLang("shop-sell-failed-vault")
            return false
        }

        // 校验数量
        if (amount <= 0 || amount > maxSellAmount) {
            player.sendLang("shop-sell-failed-amount-invalid", maxSellAmount)
            return false
        }

        // 校验作物定义
        val cropDef = CropManager.getCropDefinition(cropTypeId)
        if (cropDef == null) {
            player.sendLang("shop-sell-failed-crop-unknown", cropTypeId)
            return false
        }

        // 校验收购价格
        val unitPrice = EconomyManager.getCropPrice(cropTypeId)
        if (unitPrice == null || unitPrice <= 0.0) {
            player.sendLang("shop-sell-failed-no-price", cropDef.name)
            return false
        }

        // 校验背包中物品数量
        val harvestMaterial = Material.matchMaterial(cropDef.harvestItemId)
        if (harvestMaterial == null) {
            warning("[Farm] 作物 $cropTypeId 的 harvestItemId=${cropDef.harvestItemId} 无法匹配 Material")
            player.sendLang("shop-sell-failed-crop-unknown", cropTypeId)
            return false
        }

        val ownedAmount = countMaterialInInventory(player, harvestMaterial)
        if (ownedAmount <= 0) {
            player.sendLang("shop-sell-failed-no-item", cropDef.name)
            return false
        }
        if (ownedAmount < amount) {
            player.sendLang("shop-sell-failed-not-enough", cropDef.name, amount, ownedAmount)
            return false
        }

        // 计算总价
        val totalPrice = unitPrice * amount

        // 发放金币
        if (!EconomyManager.deposit(player, totalPrice)) {
            player.sendLang("shop-sell-failed-deposit")
            return false
        }

        // 扣除背包物品
        removeMaterialFromInventory(player, harvestMaterial, amount)

        // 累加金币收入统计
        PlayerDataManager.updateStatistic(uuid, StatisticType.TOTAL_COIN_INCOME, totalPrice)

        // 发布事件
        CropSoldEvent(uuid, cropTypeId, amount, totalPrice).call()

        // 通知玩家
        player.sendLang("shop-sell-success", amount, cropDef.name, String.format("%.2f", totalPrice))
        return true
    }

    // ==================== 内部工具方法 ====================

    /**
     * 统计玩家背包中指定 Material 的总数量
     */
    private fun countMaterialInInventory(player: Player, material: Material): Int {
        var count = 0
        for (item in player.inventory.contents) {
            if (item != null && item.type == material) {
                count += item.amount
            }
        }
        return count
    }

    /**
     * 从玩家背包中移除指定数量的 Material
     */
    private fun removeMaterialFromInventory(player: Player, material: Material, amount: Int) {
        var remaining = amount
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

    /** 重载配置 */
    fun reload() {
        config.reload()
    }
}
