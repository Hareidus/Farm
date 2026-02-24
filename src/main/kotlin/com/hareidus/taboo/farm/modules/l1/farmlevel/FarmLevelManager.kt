package com.hareidus.taboo.farm.modules.l1.farmlevel

import com.hareidus.taboo.farm.foundation.database.DatabaseManager
import com.hareidus.taboo.farm.foundation.model.FarmLevelDefinition
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.function.info
import taboolib.common.platform.function.warning
import taboolib.module.configuration.Config
import taboolib.module.configuration.Configuration
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * 农场等级管理器 (L1)
 *
 * 职责：
 * 1. 从配置加载农场等级定义（升级消耗、面积增量、解锁功能）
 * 2. 管理玩家农场等级数据（DB + 内存缓存）
 * 3. 提供等级相关查询 API（防护减免、自动收割、陷阱槽位等）
 *
 * 依赖: database_manager
 */
object FarmLevelManager {

    @Config("modules/l1/farm_level.yml", autoReload = true)
    lateinit var config: Configuration
        private set

    /** 等级定义缓存: level -> definition */
    private var definitions: Map<Int, FarmLevelDefinition> = emptyMap()

    /** 玩家等级缓存: uuid -> level */
    private val playerLevelCache = ConcurrentHashMap<UUID, Int>()

    @Awake(LifeCycle.ENABLE)
    fun init() {
        loadDefinitions()
    }

    // ==================== 配置加载 ====================

    /** 从配置文件加载等级定义 */
    private fun loadDefinitions() {
        val section = config.getConfigurationSection("levels")
        if (section == null) {
            warning("[Farm] farm_level.yml 中未找到 levels 配置节")
            return
        }
        definitions = section.getKeys(false).mapNotNull { key ->
            val level = key.toIntOrNull() ?: return@mapNotNull null
            val sub = section.getConfigurationSection(key) ?: return@mapNotNull null
            val materials = sub.getConfigurationSection("upgrade-cost-materials")
                ?.getKeys(false)
                ?.associate { mat -> mat to sub.getConfigurationSection("upgrade-cost-materials")!!.getInt(mat, 0) }
                ?: emptyMap()
            level to FarmLevelDefinition(
                level = level,
                upgradeCostMoney = sub.getDouble("upgrade-cost-money", 0.0),
                upgradeCostMaterials = materials,
                plotSizeIncrease = sub.getInt("plot-size-increase", 0),
                trapSlots = sub.getInt("trap-slots", 0),
                decorationSlots = sub.getInt("decoration-slots", 0),
                protectionLevel = sub.getInt("protection-level", 0),
                stealRatioReduction = sub.getDouble("steal-ratio-reduction", 0.0),
                autoHarvestUnlocked = sub.getBoolean("auto-harvest", false)
            )
        }.toMap()
        info("[Farm] 已加载 ${definitions.size} 个农场等级定义")
    }

    // ==================== 等级定义查询 ====================

    /** 获取指定等级的定义 */
    fun getDefinition(level: Int): FarmLevelDefinition? {
        return definitions[level]
    }

    /** 获取最大等级 */
    fun getMaxLevel(): Int {
        return definitions.keys.maxOrNull() ?: 1
    }

    // ==================== 玩家等级 ====================

    /** 获取玩家农场等级（优先缓存，缓存未命中则查 DB） */
    fun getPlayerLevel(uuid: UUID): Int {
        return playerLevelCache.getOrPut(uuid) {
            DatabaseManager.database.getPlayerFarmLevel(uuid)?.currentLevel ?: 1
        }
    }

    /** 设置玩家农场等级（更新 DB + 缓存） */
    fun setPlayerLevel(uuid: UUID, level: Int) {
        val existing = DatabaseManager.database.getPlayerFarmLevel(uuid)
        if (existing == null) {
            DatabaseManager.database.insertPlayerFarmLevel(uuid, level)
        } else {
            DatabaseManager.database.updatePlayerFarmLevel(uuid, level)
        }
        playerLevelCache[uuid] = level
    }

    // ==================== 等级功能查询 ====================

    /** 获取指定等级的被偷比例减免值 */
    fun getProtectionReduction(level: Int): Double {
        return definitions[level]?.stealRatioReduction ?: 0.0
    }

    /** 指定等级是否解锁自动收割 */
    fun isAutoHarvestUnlocked(level: Int): Boolean {
        return definitions[level]?.autoHarvestUnlocked ?: false
    }

    /** 获取指定等级的陷阱槽位数 */
    fun getTrapSlots(level: Int): Int {
        return definitions[level]?.trapSlots ?: 0
    }

    // ==================== 缓存管理 ====================

    /** 清除指定玩家的等级缓存 */
    fun invalidateCache(uuid: UUID) {
        playerLevelCache.remove(uuid)
    }

    /** 清除所有玩家等级缓存 */
    fun invalidateAllCache() {
        playerLevelCache.clear()
    }

    /** 重载配置 */
    fun reload() {
        config.reload()
        loadDefinitions()
        invalidateAllCache()
    }
}
