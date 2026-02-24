package com.hareidus.taboo.farm.modules.l1.stealrecord

import com.hareidus.taboo.farm.foundation.database.DatabaseManager
import com.hareidus.taboo.farm.foundation.model.StealRecord
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.event.SubscribeEvent
import taboolib.common.platform.function.info
import taboolib.module.configuration.Config
import taboolib.module.configuration.Configuration
import org.bukkit.event.player.PlayerQuitEvent
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * 偷菜记录管理器 (L1)
 *
 * 职责：
 * 1. 记录偷菜行为日志（偷取者、被偷者、作物种类、数量、时间）
 * 2. 管理玩家对特定农场主的偷菜冷却计时
 * 3. 追踪单次访问中玩家对某地块已偷取的数量
 * 4. 提供偷菜记录查询能力
 *
 * 依赖: database_manager
 */
object StealRecordManager {

    @Config("modules/l1/steal_record.yml", autoReload = true)
    lateinit var config: Configuration
        private set

    /** 单次访问偷取计数（内存）: key = "thiefUUID_victimUUID" -> count */
    private val visitStealCounts = ConcurrentHashMap<String, Int>()

    /** 冷却时长（毫秒），从配置读取 */
    private var cooldownDurationMs: Long = 4 * 60 * 60 * 1000L

    /** 单次查询最大记录数 */
    private var maxRecordsPerQuery: Int = 50

    @Awake(LifeCycle.ENABLE)
    fun init() {
        loadConfig()
        info("[Farm] 偷菜记录管理器已初始化 (冷却: ${config.getLong("cooldown-duration-hours", 4)}h, 查询上限: $maxRecordsPerQuery)")
    }

    private fun loadConfig() {
        cooldownDurationMs = config.getLong("cooldown-duration-hours", 4) * 60 * 60 * 1000L
        maxRecordsPerQuery = config.getInt("max-records-per-query", 50)
    }

    // ==================== 记录偷菜 ====================

    /**
     * 记录一次偷菜行为并触发 StealRecordCreatedEvent
     * @return true 表示记录成功
     */
    fun recordSteal(thief: UUID, victim: UUID, cropType: String, amount: Int): Boolean {
        val timestamp = System.currentTimeMillis()
        val success = DatabaseManager.database.insertStealRecord(thief, victim, cropType, amount, timestamp)
        if (success) {
            val record = StealRecord(
                thiefUUID = thief,
                victimUUID = victim,
                cropType = cropType,
                amount = amount,
                timestamp = timestamp
            )
            StealRecordCreatedEvent(record).call()
        }
        return success
    }

    // ==================== 冷却管理 ====================

    /** 检查偷菜者对某农场主是否处于冷却中 */
    fun isOnCooldown(thief: UUID, victim: UUID): Boolean {
        val cooldown = DatabaseManager.database.getStealCooldown(thief, victim) ?: return false
        if (cooldown.isExpired()) {
            DatabaseManager.database.removeStealCooldown(thief, victim)
            return false
        }
        return true
    }

    /** 获取剩余冷却时间（毫秒），无冷却返回 0 */
    fun getCooldownRemaining(thief: UUID, victim: UUID): Long {
        val cooldown = DatabaseManager.database.getStealCooldown(thief, victim) ?: return 0L
        val remaining = cooldown.remainingTime()
        if (remaining <= 0) {
            DatabaseManager.database.removeStealCooldown(thief, victim)
            return 0L
        }
        return remaining
    }

    /** 开始对某农场主的偷菜冷却计时 */
    fun startCooldown(thief: UUID, victim: UUID) {
        val startTime = System.currentTimeMillis()
        DatabaseManager.database.setStealCooldown(thief, victim, startTime, cooldownDurationMs)
    }

    // ==================== 访问计数（内存） ====================

    /** 获取本次访问中偷菜者对某农场主已偷取的数量 */
    fun getVisitStealCount(thief: UUID, victim: UUID): Int {
        return visitStealCounts[visitKey(thief, victim)] ?: 0
    }

    /** 累加本次访问的偷取计数 */
    fun incrementVisitStealCount(thief: UUID, victim: UUID) {
        val key = visitKey(thief, victim)
        visitStealCounts.merge(key, 1) { old, inc -> old + inc }
    }

    /** 重置本次访问的偷取计数 */
    fun resetVisitStealCount(thief: UUID, victim: UUID) {
        visitStealCounts.remove(visitKey(thief, victim))
    }

    // ==================== 记录查询 ====================

    /** 查询某玩家被偷的最近记录 */
    fun getRecentStealsAgainst(victim: UUID, limit: Int = 20): List<StealRecord> {
        val cappedLimit = limit.coerceAtMost(maxRecordsPerQuery)
        return DatabaseManager.database.getStealRecordsByVictim(victim, cappedLimit)
    }

    /** 查询某玩家偷取他人的最近记录 */
    fun getRecentStealsBy(thief: UUID, limit: Int = 20): List<StealRecord> {
        val cappedLimit = limit.coerceAtMost(maxRecordsPerQuery)
        return DatabaseManager.database.getStealRecordsByThief(thief, cappedLimit)
    }

    // ==================== 工具方法 ====================

    private fun visitKey(thief: UUID, victim: UUID): String {
        return "${thief}_${victim}"
    }

    /** 重载配置 */
    fun reload() {
        config.reload()
        loadConfig()
    }

    /** 玩家退出时清理该玩家相关的访问计数 */
    @SubscribeEvent
    fun onPlayerQuit(e: PlayerQuitEvent) {
        val uuid = e.player.uniqueId.toString()
        visitStealCounts.keys.removeIf { it.startsWith(uuid) }
    }
}
