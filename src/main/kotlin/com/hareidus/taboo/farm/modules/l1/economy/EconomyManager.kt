package com.hareidus.taboo.farm.modules.l1.economy

import com.hareidus.taboo.farm.foundation.model.CropPrice
import org.bukkit.OfflinePlayer
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.function.info
import taboolib.common.platform.function.warning
import taboolib.module.configuration.Config
import taboolib.module.configuration.Configuration
import taboolib.platform.compat.depositBalance
import taboolib.platform.compat.getBalance
import taboolib.platform.compat.withdrawBalance

/**
 * 经济管理器 (L1)
 *
 * 职责：
 * 1. 对接 Vault 经济系统，提供存取款 API（通过 TabooLib BukkitHook）
 * 2. 管理作物收购价格配置
 *
 * 无模块依赖。
 */
object EconomyManager {

    @Config("modules/l1/economy.yml", autoReload = true)
    lateinit var config: Configuration
        private set

    /** 作物收购价格缓存 */
    private var cropPrices: Map<String, CropPrice> = emptyMap()

    /** Vault 是否可用 */
    var isVaultAvailable: Boolean = false
        private set

    @Awake(LifeCycle.ENABLE)
    fun init() {
        checkVault()
        loadCropPrices()
    }

    // ==================== Vault 检测 ====================

    private fun checkVault() {
        try {
            val plugin = org.bukkit.Bukkit.getPluginManager().getPlugin("Vault")
            isVaultAvailable = plugin != null
            if (isVaultAvailable) {
                info("已成功对接 Vault 经济系统")
            } else {
                warning("未检测到 Vault 经济插件，经济功能已禁用")
            }
        } catch (e: Exception) {
            warning("Vault 检测失败: ${e.message}")
            isVaultAvailable = false
        }
    }

    // ==================== 余额操作 ====================

    /** 查询玩家余额 */
    fun getBalance(player: OfflinePlayer): Double {
        if (!isVaultAvailable) return 0.0
        return player.getBalance()
    }

    /** 检查玩家是否有足够金币 */
    fun hasEnough(player: OfflinePlayer, amount: Double): Boolean {
        if (!isVaultAvailable) return false
        return player.getBalance() >= amount
    }

    /** 向玩家账户存入金币，返回是否成功 */
    fun deposit(player: OfflinePlayer, amount: Double): Boolean {
        if (!isVaultAvailable) return false
        return player.depositBalance(amount).transactionSuccess()
    }

    /** 从玩家账户扣除金币，返回是否成功 */
    fun withdraw(player: OfflinePlayer, amount: Double): Boolean {
        if (!isVaultAvailable) return false
        return player.withdrawBalance(amount).transactionSuccess()
    }

    // ==================== 作物价格 ====================

    /** 加载作物收购价格 */
    private fun loadCropPrices() {
        val section = config.getConfigurationSection("crop-prices")
        if (section == null) {
            warning("economy.yml 中未找到 crop-prices 配置节")
            return
        }
        cropPrices = section.getKeys(false).associate { cropId ->
            cropId to CropPrice(cropId, section.getDouble(cropId, 0.0))
        }
        info("已加载 ${cropPrices.size} 种作物收购价格")
    }

    /** 获取指定作物的收购单价 */
    fun getCropPrice(cropId: String): Double? {
        return cropPrices[cropId]?.sellPrice
    }

    /** 获取所有作物收购价格 */
    fun getAllCropPrices(): Map<String, Double> {
        return cropPrices.mapValues { it.value.sellPrice }
    }

    /** 重载配置 */
    fun reload() {
        config.reload()
        loadCropPrices()
    }
}
