package com.hareidus.taboo.farm.modules.l2.achievement

import com.hareidus.taboo.farm.foundation.database.DatabaseManager
import com.hareidus.taboo.farm.foundation.model.AchievementDefinition
import com.hareidus.taboo.farm.foundation.model.PlayerAchievement
import com.hareidus.taboo.farm.modules.l1.economy.EconomyManager
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.function.info
import taboolib.common.platform.function.warning
import taboolib.module.configuration.Config
import taboolib.module.configuration.Configuration
import taboolib.platform.util.sendLang
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * 成就管理器 (L2)
 *
 * 职责：
 * 1. 从配置加载成就定义（触发条件、阈值、奖励）
 * 2. 追踪玩家成就进度，判定是否达成
 * 3. 达成后发放奖励（金币、物品）并通知玩家
 *
 * 依赖: database_manager, economy_manager
 */
object AchievementManager {

    @Config("modules/l2/achievement.yml", autoReload = true)
    lateinit var config: Configuration
        private set

    /** 成就定义缓存: id -> definition */
    private var definitions: Map<String, AchievementDefinition> = emptyMap()

    /** 按触发类型索引: triggerType -> list of definitions */
    private var triggerIndex: Map<String, List<AchievementDefinition>> = emptyMap()

    /** 玩家成就缓存: uuid -> (achievementId -> PlayerAchievement) */
    private val playerCache = ConcurrentHashMap<UUID, MutableMap<String, PlayerAchievement>>()

    @Awake(LifeCycle.ENABLE)
    fun init() {
        loadDefinitions()
    }

    // ==================== 配置加载 ====================

    /** 从配置文件加载成就定义 */
    private fun loadDefinitions() {
        val section = config.getConfigurationSection("achievements")
        if (section == null) {
            warning("[Farm] achievement.yml 中未找到 achievements 配置节")
            return
        }
        definitions = section.getKeys(false).mapNotNull { id ->
            val sub = section.getConfigurationSection(id) ?: return@mapNotNull null
            val rewardItems = sub.getConfigurationSection("reward-items")
                ?.getKeys(false)
                ?.associate { mat -> mat to sub.getConfigurationSection("reward-items")!!.getInt(mat, 0) }
                ?: emptyMap()
            id to AchievementDefinition(
                id = id,
                name = sub.getString("name", id)!!,
                description = sub.getString("description", "")!!,
                triggerType = sub.getString("trigger-type", "")!!,
                threshold = sub.getLong("threshold", 1),
                rewardMoney = sub.getDouble("reward-money", 0.0),
                rewardItems = rewardItems,
                titlePrefix = sub.getString("title-prefix", "")!!
            )
        }.toMap()
        triggerIndex = definitions.values.groupBy { it.triggerType }
        info("[Farm] 已加载 ${definitions.size} 个成就定义")
    }

    // ==================== 成就定义查询 ====================

    /** 获取指定成就定义 */
    fun getDefinition(id: String): AchievementDefinition? {
        return definitions[id]
    }

    /** 获取所有成就定义 */
    fun getAllDefinitions(): List<AchievementDefinition> {
        return definitions.values.toList()
    }

    // ==================== 玩家成就查询 ====================

    /** 获取玩家所有成就进度（优先缓存） */
    fun getPlayerAchievements(uuid: UUID): List<PlayerAchievement> {
        return getOrLoadCache(uuid).values.toList()
    }

    /** 判断玩家是否已完成指定成就 */
    fun isCompleted(uuid: UUID, achievementId: String): Boolean {
        return getOrLoadCache(uuid)[achievementId]?.completed ?: false
    }

    // ==================== 进度检查与更新 ====================

    /**
     * 检查并更新成就进度
     *
     * 当业务事件触发时调用此方法，传入触发类型和当前累计值。
     * 系统会遍历该触发类型下所有成就定义，更新进度，达到阈值则自动完成。
     *
     * @param player 触发事件的玩家
     * @param triggerType 触发类型（如 TOTAL_HARVEST, TOTAL_STEAL 等）
     * @param newValue 该维度的最新累计值
     */
    fun checkAndUpdateProgress(player: Player, triggerType: String, newValue: Long) {
        val uuid = player.uniqueId
        val cache = getOrLoadCache(uuid)
        val relatedDefs = triggerIndex[triggerType] ?: return

        for (def in relatedDefs) {
            val existing = cache[def.id]
            // 已完成的成就跳过
            if (existing?.completed == true) continue

            // 确保 DB 中有记录
            if (existing == null) {
                DatabaseManager.database.insertPlayerAchievement(uuid, def.id)
                cache[def.id] = PlayerAchievement(
                    playerUUID = uuid,
                    achievementId = def.id,
                    currentProgress = 0,
                    completed = false,
                    completedAt = null
                )
            }

            val record = cache[def.id]!!
            record.currentProgress = newValue

            if (newValue >= def.threshold) {
                completeAchievement(player, def.id)
            } else {
                DatabaseManager.database.updatePlayerAchievement(
                    uuid, def.id, newValue, false, null
                )
            }
        }
    }

    // ==================== 成就完成与奖励 ====================

    /**
     * 标记成就完成并发放奖励
     *
     * @param player 完成成就的玩家
     * @param achievementId 成就 ID
     */
    fun completeAchievement(player: Player, achievementId: String) {
        val uuid = player.uniqueId
        val def = definitions[achievementId] ?: return
        val cache = getOrLoadCache(uuid)
        val record = cache[achievementId] ?: return

        // 已完成则跳过
        if (record.completed) return

        val now = System.currentTimeMillis()
        record.completed = true
        record.completedAt = now
        record.currentProgress = def.threshold

        // 持久化
        DatabaseManager.database.updatePlayerAchievement(
            uuid, achievementId, def.threshold, true, now
        )

        // 发放金币奖励
        if (def.rewardMoney > 0) {
            EconomyManager.deposit(player, def.rewardMoney)
        }

        // 发放物品奖励
        for ((materialName, amount) in def.rewardItems) {
            if (amount <= 0) continue
            val material = Material.matchMaterial(materialName) ?: continue
            val item = ItemStack(material, amount)
            val leftover = player.inventory.addItem(item)
            // 背包满则掉落到脚下
            for (drop in leftover.values) {
                player.world.dropItemNaturally(player.location, drop)
            }
        }

        // 通知玩家
        player.sendLang("achievement-unlocked", def.name, def.titlePrefix)
        if (def.rewardMoney > 0) {
            player.sendLang("achievement-reward-money", def.rewardMoney)
        }
    }

    // ==================== 缓存管理 ====================

    /** 获取或加载玩家成就缓存 */
    private fun getOrLoadCache(uuid: UUID): MutableMap<String, PlayerAchievement> {
        return playerCache.getOrPut(uuid) {
            val list = DatabaseManager.database.getPlayerAchievements(uuid)
            val map = ConcurrentHashMap<String, PlayerAchievement>()
            for (pa in list) {
                map[pa.achievementId] = pa
            }
            map
        }
    }

    /** 清除指定玩家的成就缓存 */
    fun invalidateCache(uuid: UUID) {
        playerCache.remove(uuid)
    }

    /** 清除所有玩家成就缓存 */
    fun invalidateAllCache() {
        playerCache.clear()
    }

    /** 重载配置 */
    fun reload() {
        config.reload()
        loadDefinitions()
        invalidateAllCache()
    }
}
