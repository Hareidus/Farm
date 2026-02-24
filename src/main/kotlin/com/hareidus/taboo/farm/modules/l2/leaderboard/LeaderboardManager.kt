package com.hareidus.taboo.farm.modules.l2.leaderboard

import com.hareidus.taboo.farm.foundation.database.DatabaseManager
import com.hareidus.taboo.farm.foundation.event.LeaderboardRefreshedEvent
import com.hareidus.taboo.farm.foundation.model.LeaderboardEntry
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.function.info
import taboolib.common.platform.function.submit
import taboolib.common.platform.function.warning
import taboolib.module.configuration.Config
import taboolib.module.configuration.Configuration
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * 排行榜管理器 (L2)
 *
 * 职责：
 * 1. 定期从数据库查询各维度排名数据并缓存
 * 2. 提供排行榜查询 API（按类别、分页、玩家排名）
 * 3. 刷新完成后发布 LeaderboardRefreshedEvent
 *
 * 依赖: database_manager, player_data_manager
 */
object LeaderboardManager {

    @Config("modules/l2/leaderboard.yml", autoReload = true)
    lateinit var config: Configuration
        private set

    /** 类别 -> 排行榜条目列表（已按排名排序） */
    private val cache = ConcurrentHashMap<String, List<LeaderboardEntry>>()

    /** 类别 -> DB 列名 映射（从配置加载） */
    private var categoryColumns: Map<String, String> = emptyMap()

    /** 类别 -> 显示名称 映射 */
    private var categoryDisplayNames: Map<String, String> = emptyMap()

    /** 周期刷新任务引用 */
    private var refreshTask: taboolib.common.platform.service.PlatformExecutor.PlatformTask? = null

    @Awake(LifeCycle.ENABLE)
    fun init() {
        loadCategories()
        startRefreshTask()
        info("[Farm] 排行榜管理器已启动")
    }

    @Awake(LifeCycle.DISABLE)
    fun shutdown() {
        refreshTask?.cancel()
        cache.clear()
    }

    // ==================== 配置加载 ====================

    /** 从配置文件加载类别定义 */
    private fun loadCategories() {
        val section = config.getConfigurationSection("categories")
        if (section == null) {
            warning("[Farm] leaderboard.yml 中未找到 categories 配置节")
            return
        }
        val columns = mutableMapOf<String, String>()
        val names = mutableMapOf<String, String>()
        section.getKeys(false).forEach { key ->
            val sub = section.getConfigurationSection(key) ?: return@forEach
            columns[key] = sub.getString("column", key)!!
            names[key] = sub.getString("display-name", key)!!
        }
        categoryColumns = columns
        categoryDisplayNames = names
        info("[Farm] 已加载 ${categoryColumns.size} 个排行榜类别")
    }

    /** 获取配置的刷新间隔（tick） */
    private fun getRefreshIntervalTicks(): Long {
        return config.getLong("refresh-interval-seconds", 300) * 20L
    }

    /** 获取每页条目数 */
    fun getEntriesPerPage(): Int {
        return config.getInt("entries-per-page", 10)
    }

    // ==================== 定时刷新 ====================

    /** 启动周期刷新任务 */
    private fun startRefreshTask() {
        refreshTask?.cancel()
        val intervalTicks = getRefreshIntervalTicks()
        // 首次延迟 1 秒后刷新，之后按配置周期
        refreshTask = submit(async = true, delay = 20L, period = intervalTicks) {
            refreshAll()
        }
    }

    /** 刷新所有类别的排行榜缓存 */
    fun refreshAll() {
        categoryColumns.forEach { (category, column) ->
            refreshCategory(category, column)
        }
    }

    /** 刷新单个类别的排行榜缓存 */
    private fun refreshCategory(category: String, column: String) {
        try {
            val limit = config.getInt("entries-per-page", 10) * 10
            val entries = DatabaseManager.database.getTopPlayers(column, limit)
            cache[category] = entries
            LeaderboardRefreshedEvent(category, System.currentTimeMillis()).call()
        } catch (e: Exception) {
            warning("[Farm] 刷新排行榜 [$category] 失败: ${e.message}")
        }
    }

    // ==================== 查询 API ====================

    /**
     * 获取指定类别的排行榜数据
     * @param category 类别 ID（harvest / steal / wealth / defense）
     * @param limit 返回条目数量上限
     */
    fun getLeaderboard(category: String, limit: Int = 10): List<LeaderboardEntry> {
        val entries = cache[category] ?: return emptyList()
        return entries.take(limit)
    }

    /**
     * 获取指定类别的分页数据
     * @param category 类别 ID
     * @param page 页码（从 1 开始）
     */
    fun getLeaderboardPage(category: String, page: Int): List<LeaderboardEntry> {
        val entries = cache[category] ?: return emptyList()
        val perPage = getEntriesPerPage()
        val start = (page - 1).coerceAtLeast(0) * perPage
        if (start >= entries.size) return emptyList()
        return entries.subList(start, (start + perPage).coerceAtMost(entries.size))
    }

    /**
     * 获取指定类别的总页数
     */
    fun getTotalPages(category: String): Int {
        val entries = cache[category] ?: return 0
        val perPage = getEntriesPerPage()
        return (entries.size + perPage - 1) / perPage
    }

    /**
     * 获取玩家在指定类别中的排名
     * @return 排名（从 1 开始），未上榜返回 null
     */
    fun getPlayerRank(uuid: UUID, category: String): Int? {
        val entries = cache[category] ?: return null
        val entry = entries.firstOrNull { it.playerUUID == uuid }
        return entry?.rank
    }

    /** 获取所有已注册的类别 ID */
    fun getCategories(): Set<String> {
        return categoryColumns.keys
    }

    /** 获取类别的显示名称 */
    fun getCategoryDisplayName(category: String): String {
        return categoryDisplayNames[category] ?: category
    }

    // ==================== 重载 ====================

    /** 重载配置并重启刷新任务 */
    fun reload() {
        config.reload()
        loadCategories()
        startRefreshTask()
    }
}
