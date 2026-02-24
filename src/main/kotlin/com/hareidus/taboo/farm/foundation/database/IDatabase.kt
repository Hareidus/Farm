package com.hareidus.taboo.farm.foundation.database

import com.hareidus.taboo.farm.foundation.model.*
import java.util.UUID
import javax.sql.DataSource

/**
 * 数据库接口（地基层）
 *
 * 所有数据库 CRUD 操作在此接口声明，由 SQLite 和 MySQL 两个实现类同时实现。
 * 各模块通过 DatabaseManager.database 访问，禁止直接创建实例。
 */
interface IDatabase {

    val type: DatabaseType
    val dataSource: DataSource
    fun close()

    // ==================== player_data_manager ====================

    fun getPlayerData(uuid: UUID): PlayerData?
    fun insertPlayerData(uuid: UUID): Boolean
    fun updatePlayerData(data: PlayerData): Boolean
    fun getUnreadNotifications(uuid: UUID): List<OfflineNotification>
    fun insertNotification(uuid: UUID, type: NotificationType, data: String, timestamp: Long): Boolean
    fun markNotificationsRead(uuid: UUID): Boolean
    fun deleteNotifications(uuid: UUID): Boolean

    // ==================== plot_manager ====================

    fun getPlotByOwner(uuid: UUID): Plot?
    fun getPlotById(id: Long): Plot?
    fun insertPlot(plot: Plot): Long
    fun updatePlotSize(plotId: Long, minX: Int, minZ: Int, maxX: Int, maxZ: Int, size: Int): Boolean
    fun deletePlot(plotId: Long): Boolean
    fun getAllPlots(): List<Plot>

    // ==================== farm_level_manager ====================

    fun getPlayerFarmLevel(uuid: UUID): PlayerFarmLevel?
    fun insertPlayerFarmLevel(uuid: UUID, level: Int): Boolean
    fun updatePlayerFarmLevel(uuid: UUID, level: Int): Boolean
    // ==================== social_manager ====================

    fun getFriends(uuid: UUID): List<FriendRelation>
    fun insertFriend(playerA: UUID, playerB: UUID): Boolean
    fun deleteFriend(playerA: UUID, playerB: UUID): Boolean
    fun isFriend(playerA: UUID, playerB: UUID): Boolean
    fun getEnemies(uuid: UUID): List<EnemyRecord>
    fun insertEnemy(victimUUID: UUID, thiefUUID: UUID): Boolean
    fun deleteEnemy(victimUUID: UUID, thiefUUID: UUID): Boolean
    fun isEnemy(victimUUID: UUID, thiefUUID: UUID): Boolean
    fun getPendingFriendRequests(uuid: UUID): List<FriendRequest>
    fun insertFriendRequest(senderUUID: UUID, receiverUUID: UUID): Boolean
    fun updateFriendRequestStatus(id: Long, status: FriendRequestStatus): Boolean

    // ==================== steal_record_manager ====================

    fun insertStealRecord(thiefUUID: UUID, victimUUID: UUID, cropType: String, amount: Int, timestamp: Long): Boolean
    fun getStealRecordsByVictim(victimUUID: UUID, limit: Int = 50): List<StealRecord>
    fun getStealRecordsByThief(thiefUUID: UUID, limit: Int = 50): List<StealRecord>
    fun getStealCooldown(thiefUUID: UUID, victimUUID: UUID): StealCooldown?
    fun setStealCooldown(thiefUUID: UUID, victimUUID: UUID, startTime: Long, duration: Long): Boolean
    fun removeStealCooldown(thiefUUID: UUID, victimUUID: UUID): Boolean

    // ==================== trap_manager ====================

    fun getDeployedTraps(plotId: Long): List<DeployedTrap>
    fun deployTrap(plotId: Long, trapTypeId: String, slotIndex: Int): Boolean
    fun removeTrap(plotId: Long, slotIndex: Int): Boolean
    fun removeAllTraps(plotId: Long): Boolean
}
