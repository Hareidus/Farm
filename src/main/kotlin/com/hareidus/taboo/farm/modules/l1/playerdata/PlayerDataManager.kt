package com.hareidus.taboo.farm.modules.l1.playerdata

import com.hareidus.taboo.farm.foundation.database.DatabaseManager
import com.hareidus.taboo.farm.foundation.model.NotificationType
import com.hareidus.taboo.farm.foundation.model.OfflineNotification
import com.hareidus.taboo.farm.foundation.model.PlayerData
import com.hareidus.taboo.farm.foundation.model.StatisticType
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import taboolib.common.platform.event.SubscribeEvent
import taboolib.common.platform.function.info
import taboolib.common.platform.function.warning
import taboolib.platform.type.BukkitProxyEvent
import taboolib.platform.util.sendLang
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * 玩家数据加载完成事件
 *
 * 玩家数据从数据库加载（或新建）完成后触发。
 */
class PlayerDataLoadedEvent(
    val playerUUID: UUID,
    val playerData: PlayerData
) : BukkitProxyEvent()

/**
 * 玩家统计数据变更事件
 *
 * 某项统计维度发生变更时触发。
 */
class PlayerStatisticUpdateEvent(
    val playerUUID: UUID,
    val statisticType: StatisticType,
    val oldValue: Number,
    val newValue: Number
) : BukkitProxyEvent()

/**
 * 玩家数据管理器 (L1)
 *
 * 职责：
 * 1. 缓存在线玩家的 PlayerData
 * 2. 玩家上线时加载数据并推送离线通知
 * 3. 玩家下线时保存数据并清除缓存
 * 4. 提供统计数据更新与离线通知管理 API
 *
 * 依赖：database_manager
 */
object PlayerDataManager {

    /** 在线玩家数据缓存 */
    private val cache = ConcurrentHashMap<UUID, PlayerData>()

    // ==================== 查询 API ====================

    /** 从缓存获取玩家数据（仅在线玩家） */
    fun getPlayerData(uuid: UUID): PlayerData? {
        return cache[uuid]
    }

    /** 获取所有缓存中的玩家数据（用于排行榜等批量查询） */
    fun getAllCachedData(): Map<UUID, PlayerData> {
        return cache.toMap()
    }

    // ==================== 统计更新 API ====================

    /**
     * 更新玩家统计数据
     *
     * @param uuid 玩家 UUID
     * @param type 统计维度
     * @param delta 增量值（Long 用于整数维度，会被转换为对应类型）
     */
    fun updateStatistic(uuid: UUID, type: StatisticType, delta: Long) {
        val data = cache[uuid] ?: return
        val oldValue: Number
        val newValue: Number
        when (type) {
            StatisticType.TOTAL_HARVEST -> {
                oldValue = data.totalHarvest
                data.totalHarvest += delta
                newValue = data.totalHarvest
            }
            StatisticType.TOTAL_STEAL -> {
                oldValue = data.totalSteal
                data.totalSteal += delta
                newValue = data.totalSteal
            }
            StatisticType.TOTAL_STOLEN -> {
                oldValue = data.totalStolen
                data.totalStolen += delta
                newValue = data.totalStolen
            }
            StatisticType.TOTAL_COIN_INCOME -> {
                oldValue = data.totalCoinIncome
                data.totalCoinIncome += delta.toDouble()
                newValue = data.totalCoinIncome
            }
            StatisticType.TRAP_TRIGGERED_COUNT -> {
                oldValue = data.trapTriggeredCount
                data.trapTriggeredCount += delta.toInt()
                newValue = data.trapTriggeredCount
            }
        }
        PlayerStatisticUpdateEvent(uuid, type, oldValue, newValue).call()
    }

    /**
     * 更新玩家统计数据（Double 增量，用于金币收入等）
     */
    fun updateStatistic(uuid: UUID, type: StatisticType, delta: Double) {
        val data = cache[uuid] ?: return
        val oldValue: Number
        val newValue: Number
        when (type) {
            StatisticType.TOTAL_COIN_INCOME -> {
                oldValue = data.totalCoinIncome
                data.totalCoinIncome += delta
                newValue = data.totalCoinIncome
            }
            else -> {
                updateStatistic(uuid, type, delta.toLong())
                return
            }
        }
        PlayerStatisticUpdateEvent(uuid, type, oldValue, newValue).call()
    }

    // ==================== 离线通知 API ====================

    /** 添加离线通知（目标玩家不在线时写入数据库） */
    fun addNotification(uuid: UUID, type: NotificationType, data: String) {
        val timestamp = System.currentTimeMillis()
        try {
            DatabaseManager.database.insertNotification(uuid, type, data, timestamp)
        } catch (e: Exception) {
            warning("[Farm] 写入离线通知失败 [$uuid]: ${e.message}")
        }
    }

    /** 获取玩家未读离线通知 */
    fun getUnreadNotifications(uuid: UUID): List<OfflineNotification> {
        return try {
            DatabaseManager.database.getUnreadNotifications(uuid)
        } catch (e: Exception) {
            warning("[Farm] 读取离线通知失败 [$uuid]: ${e.message}")
            emptyList()
        }
    }

    // ==================== 数据持久化（内部） ====================

    /** 从数据库加载玩家数据，不存在则新建 */
    private fun loadPlayerData(uuid: UUID): PlayerData? {
        return try {
            var data = DatabaseManager.database.getPlayerData(uuid)
            if (data == null) {
                DatabaseManager.database.insertPlayerData(uuid)
                data = DatabaseManager.database.getPlayerData(uuid)
            }
            data
        } catch (e: Exception) {
            warning("[Farm] 加载玩家数据失败 [$uuid]: ${e.message}")
            null
        }
    }

    /** 将缓存中的玩家数据保存到数据库 */
    private fun savePlayerData(uuid: UUID) {
        val data = cache[uuid] ?: return
        try {
            DatabaseManager.database.updatePlayerData(data)
        } catch (e: Exception) {
            warning("[Farm] 保存玩家数据失败 [$uuid]: ${e.message}")
        }
    }

    /** 标记玩家所有通知为已读 */
    fun markNotificationsRead(uuid: UUID) {
        try {
            DatabaseManager.database.markNotificationsRead(uuid)
        } catch (e: Exception) {
            warning("[Farm] 标记通知已读失败 [$uuid]: ${e.message}")
        }
    }

    /** 推送未读离线通知给在线玩家 */
    private fun pushUnreadNotifications(player: org.bukkit.entity.Player) {
        val notifications = getUnreadNotifications(player.uniqueId)
        if (notifications.isEmpty()) {
            player.sendLang("playerdata-notification-none")
            return
        }
        player.sendLang("playerdata-notification-header", notifications.size)
        for (notification in notifications) {
            when (notification.type) {
                NotificationType.STOLEN -> {
                    val parts = notification.data.split("|")
                    if (parts.size >= 3) {
                        player.sendLang("playerdata-notification-stolen", parts[0], parts[1], parts[2])
                    }
                }
                NotificationType.TRAP_TRIGGERED -> {
                    player.sendLang("playerdata-notification-trap", notification.data)
                }
                NotificationType.FRIEND_REQUEST -> {
                    player.sendLang("playerdata-notification-friend", notification.data)
                }
                NotificationType.WATERED -> {
                    player.sendLang("playerdata-notification-watered", notification.data)
                }
            }
        }
        markNotificationsRead(player.uniqueId)
    }

    // ==================== 管理 API（供 admin_manager 等调用） ====================

    /** 重置玩家数据（管理员操作） */
    fun resetPlayerData(uuid: UUID) {
        val data = cache[uuid]
        if (data != null) {
            data.totalHarvest = 0
            data.totalSteal = 0
            data.totalStolen = 0
            data.totalCoinIncome = 0.0
            data.trapTriggeredCount = 0
            data.consecutiveLoginDays = 0
        }
        try {
            val freshData = PlayerData(uuid)
            DatabaseManager.database.updatePlayerData(freshData)
            if (cache.containsKey(uuid)) {
                cache[uuid] = freshData
            }
            DatabaseManager.database.deleteNotifications(uuid)
        } catch (e: Exception) {
            warning("[Farm] 重置玩家数据失败 [$uuid]: ${e.message}")
        }
    }

    // ==================== 事件监听 ====================

    @SubscribeEvent
    fun onPlayerJoin(e: PlayerJoinEvent) {
        val player = e.player
        val uuid = player.uniqueId
        val data = loadPlayerData(uuid)
        if (data == null) {
            warning("[Farm] 无法加载玩家数据: ${player.name} ($uuid)")
            return
        }
        // 更新连续登录天数
        val today = System.currentTimeMillis() / 86400000L
        val lastDay = data.lastLoginDate / 86400000L
        if (today - lastDay == 1L) {
            data.consecutiveLoginDays += 1
        } else if (today - lastDay > 1L) {
            data.consecutiveLoginDays = 1
        }
        data.lastLoginDate = System.currentTimeMillis()
        cache[uuid] = data
        PlayerDataLoadedEvent(uuid, data).call()
        player.sendLang("playerdata-loaded")
        info("[Farm] 已加载玩家数据: ${player.name}")
        // 延迟推送离线通知（等待玩家完全进入）
        taboolib.common.platform.function.submit(delay = 20L) {
            if (player.isOnline) {
                pushUnreadNotifications(player)
            }
        }
    }

    @SubscribeEvent
    fun onPlayerQuit(e: PlayerQuitEvent) {
        val uuid = e.player.uniqueId
        savePlayerData(uuid)
        cache.remove(uuid)
    }
}
