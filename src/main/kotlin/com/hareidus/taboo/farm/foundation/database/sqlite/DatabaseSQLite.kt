package com.hareidus.taboo.farm.foundation.database.sqlite

import com.hareidus.taboo.farm.foundation.config.MainConfig
import com.hareidus.taboo.farm.foundation.database.DatabaseType
import com.hareidus.taboo.farm.foundation.database.IDatabase
import com.hareidus.taboo.farm.foundation.model.*
import taboolib.common.platform.function.getDataFolder
import taboolib.module.database.*
import java.io.File
import java.util.UUID
import javax.sql.DataSource

/**
 * SQLite 数据库实现
 *
 * 所有表定义和 CRUD 实现集中在此类。
 */
class DatabaseSQLite : IDatabase {

    val host: Host<SQLite>
    override val type = DatabaseType.SQLITE
    override val dataSource: DataSource

    // ==================== 表定义 ====================

    private val playerDataTable: Table<Host<SQLite>, SQLite>
    private val offlineNotificationsTable: Table<Host<SQLite>, SQLite>
    private val plotsTable: Table<Host<SQLite>, SQLite>
    private val playerLevelsTable: Table<Host<SQLite>, SQLite>
    private val friendsTable: Table<Host<SQLite>, SQLite>
    private val enemiesTable: Table<Host<SQLite>, SQLite>
    private val friendRequestsTable: Table<Host<SQLite>, SQLite>
    private val stealRecordsTable: Table<Host<SQLite>, SQLite>
    private val stealCooldownsTable: Table<Host<SQLite>, SQLite>
    private val deployedTrapsTable: Table<Host<SQLite>, SQLite>

    init {
        val dbFile = File(getDataFolder(), "database/${MainConfig.sqliteFile}")
        dbFile.parentFile?.mkdirs()
        if (!dbFile.exists()) dbFile.createNewFile()

        host = dbFile.getHost()
        dataSource = host.createDataSource()

        playerDataTable = createPlayerDataTable()
        offlineNotificationsTable = createOfflineNotificationsTable()
        plotsTable = createPlotsTable()
        playerLevelsTable = createPlayerLevelsTable()
        friendsTable = createFriendsTable()
        enemiesTable = createEnemiesTable()
        friendRequestsTable = createFriendRequestsTable()
        stealRecordsTable = createStealRecordsTable()
        stealCooldownsTable = createStealCooldownsTable()
        deployedTrapsTable = createDeployedTrapsTable()

        createAllTables()
    }

    // ==================== 表创建方法 ====================

    private fun createPlayerDataTable() = Table("farm_player_data", host) {
        add { id() }
        add("uuid") { type(ColumnTypeSQLite.TEXT) }
        add("total_harvest") { type(ColumnTypeSQLite.INTEGER) }
        add("total_steal") { type(ColumnTypeSQLite.INTEGER) }
        add("total_stolen") { type(ColumnTypeSQLite.INTEGER) }
        add("total_coin_income") { type(ColumnTypeSQLite.INTEGER) }
        add("trap_triggered_count") { type(ColumnTypeSQLite.INTEGER) }
        add("consecutive_login_days") { type(ColumnTypeSQLite.INTEGER) }
        add("last_login_date") { type(ColumnTypeSQLite.INTEGER) }
    }

    private fun createOfflineNotificationsTable() = Table("farm_offline_notifications", host) {
        add { id() }
        add("player_uuid") { type(ColumnTypeSQLite.TEXT) }
        add("type") { type(ColumnTypeSQLite.TEXT) }
        add("data") { type(ColumnTypeSQLite.TEXT) }
        add("timestamp") { type(ColumnTypeSQLite.INTEGER) }
        add("read") { type(ColumnTypeSQLite.INTEGER) }
    }

    private fun createPlotsTable() = Table("farm_plots", host) {
        add { id() }
        add("owner_uuid") { type(ColumnTypeSQLite.TEXT) }
        add("grid_x") { type(ColumnTypeSQLite.INTEGER) }
        add("grid_z") { type(ColumnTypeSQLite.INTEGER) }
        add("world_name") { type(ColumnTypeSQLite.TEXT) }
        add("min_x") { type(ColumnTypeSQLite.INTEGER) }
        add("min_z") { type(ColumnTypeSQLite.INTEGER) }
        add("max_x") { type(ColumnTypeSQLite.INTEGER) }
        add("max_z") { type(ColumnTypeSQLite.INTEGER) }
        add("size") { type(ColumnTypeSQLite.INTEGER) }
    }

    private fun createPlayerLevelsTable() = Table("farm_player_levels", host) {
        add { id() }
        add("player_uuid") { type(ColumnTypeSQLite.TEXT) }
        add("current_level") { type(ColumnTypeSQLite.INTEGER) }
    }

    private fun createFriendsTable() = Table("farm_friends", host) {
        add { id() }
        add("player_a") { type(ColumnTypeSQLite.TEXT) }
        add("player_b") { type(ColumnTypeSQLite.TEXT) }
        add("created_at") { type(ColumnTypeSQLite.INTEGER) }
    }

    private fun createEnemiesTable() = Table("farm_enemies", host) {
        add { id() }
        add("victim_uuid") { type(ColumnTypeSQLite.TEXT) }
        add("thief_uuid") { type(ColumnTypeSQLite.TEXT) }
        add("marked_at") { type(ColumnTypeSQLite.INTEGER) }
    }

    private fun createFriendRequestsTable() = Table("farm_friend_requests", host) {
        add { id() }
        add("sender_uuid") { type(ColumnTypeSQLite.TEXT) }
        add("receiver_uuid") { type(ColumnTypeSQLite.TEXT) }
        add("requested_at") { type(ColumnTypeSQLite.INTEGER) }
        add("status") { type(ColumnTypeSQLite.TEXT) }
    }

    private fun createStealRecordsTable() = Table("farm_steal_records", host) {
        add { id() }
        add("thief_uuid") { type(ColumnTypeSQLite.TEXT) }
        add("victim_uuid") { type(ColumnTypeSQLite.TEXT) }
        add("crop_type") { type(ColumnTypeSQLite.TEXT) }
        add("amount") { type(ColumnTypeSQLite.INTEGER) }
        add("timestamp") { type(ColumnTypeSQLite.INTEGER) }
    }

    private fun createStealCooldownsTable() = Table("farm_steal_cooldowns", host) {
        add("thief_uuid") { type(ColumnTypeSQLite.TEXT) }
        add("victim_uuid") { type(ColumnTypeSQLite.TEXT) }
        add("start_time") { type(ColumnTypeSQLite.INTEGER) }
        add("duration") { type(ColumnTypeSQLite.INTEGER) }
    }

    private fun createDeployedTrapsTable() = Table("farm_deployed_traps", host) {
        add { id() }
        add("plot_id") { type(ColumnTypeSQLite.INTEGER) }
        add("trap_type_id") { type(ColumnTypeSQLite.TEXT) }
        add("slot_index") { type(ColumnTypeSQLite.INTEGER) }
    }

    private fun createAllTables() {
        playerDataTable.workspace(dataSource) { createTable() }.run()
        offlineNotificationsTable.workspace(dataSource) { createTable() }.run()
        plotsTable.workspace(dataSource) { createTable() }.run()
        playerLevelsTable.workspace(dataSource) { createTable() }.run()
        friendsTable.workspace(dataSource) { createTable() }.run()
        enemiesTable.workspace(dataSource) { createTable() }.run()
        friendRequestsTable.workspace(dataSource) { createTable() }.run()
        stealRecordsTable.workspace(dataSource) { createTable() }.run()
        stealCooldownsTable.workspace(dataSource) { createTable() }.run()
        deployedTrapsTable.workspace(dataSource) { createTable() }.run()
    }

    override fun close() {
        // SQLite DataSource 由 TabooLib 管理
    }

    // ==================== player_data_manager ====================

    override fun getPlayerData(uuid: UUID): PlayerData? {
        return playerDataTable.select(dataSource) {
            where("uuid" eq uuid.toString())
            limit(1)
        }.firstOrNull {
            PlayerData(
                uuid = UUID.fromString(getString("uuid")),
                totalHarvest = getLong("total_harvest"),
                totalSteal = getLong("total_steal"),
                totalStolen = getLong("total_stolen"),
                totalCoinIncome = getLong("total_coin_income") / 100.0,
                trapTriggeredCount = getInt("trap_triggered_count"),
                consecutiveLoginDays = getInt("consecutive_login_days"),
                lastLoginDate = getLong("last_login_date")
            )
        }
    }

    override fun insertPlayerData(uuid: UUID): Boolean {
        return playerDataTable.insert(dataSource,
            "uuid", "total_harvest", "total_steal", "total_stolen",
            "total_coin_income", "trap_triggered_count", "consecutive_login_days", "last_login_date"
        ) {
            value(uuid.toString(), 0L, 0L, 0L, 0L, 0, 0, 0L)
        } > 0
    }

    override fun updatePlayerData(data: PlayerData): Boolean {
        return playerDataTable.update(dataSource) {
            where("uuid" eq data.uuid.toString())
            set("total_harvest", data.totalHarvest)
            set("total_steal", data.totalSteal)
            set("total_stolen", data.totalStolen)
            set("total_coin_income", (data.totalCoinIncome * 100).toLong())
            set("trap_triggered_count", data.trapTriggeredCount)
            set("consecutive_login_days", data.consecutiveLoginDays)
            set("last_login_date", data.lastLoginDate)
        } > 0
    }

    // ==================== notifications ====================

    override fun getUnreadNotifications(uuid: UUID): List<OfflineNotification> {
        return offlineNotificationsTable.select(dataSource) {
            where("player_uuid" eq uuid.toString())
            where("read" eq 0)
            order("timestamp", false)
        }.map {
            OfflineNotification(
                id = getLong("id"),
                playerUUID = UUID.fromString(getString("player_uuid")),
                type = NotificationType.valueOf(getString("type")),
                data = getString("data"),
                timestamp = getLong("timestamp"),
                read = getInt("read") == 1
            )
        }
    }

    override fun insertNotification(uuid: UUID, type: NotificationType, data: String, timestamp: Long): Boolean {
        return offlineNotificationsTable.insert(dataSource,
            "player_uuid", "type", "data", "timestamp", "read"
        ) {
            value(uuid.toString(), type.name, data, timestamp, 0)
        } > 0
    }

    override fun markNotificationsRead(uuid: UUID): Boolean {
        return offlineNotificationsTable.update(dataSource) {
            where("player_uuid" eq uuid.toString())
            where("read" eq 0)
            set("read", 1)
        } >= 0
    }

    override fun deleteNotifications(uuid: UUID): Boolean {
        return offlineNotificationsTable.delete(dataSource) {
            where("player_uuid" eq uuid.toString())
        } >= 0
    }

    // ==================== plot_manager ====================

    override fun getPlotByOwner(uuid: UUID): Plot? {
        return plotsTable.select(dataSource) {
            where("owner_uuid" eq uuid.toString())
            limit(1)
        }.firstOrNull {
            Plot(
                id = getLong("id"),
                ownerUUID = UUID.fromString(getString("owner_uuid")),
                gridX = getInt("grid_x"),
                gridZ = getInt("grid_z"),
                worldName = getString("world_name"),
                minX = getInt("min_x"),
                minZ = getInt("min_z"),
                maxX = getInt("max_x"),
                maxZ = getInt("max_z"),
                size = getInt("size")
            )
        }
    }

    override fun getPlotById(id: Long): Plot? {
        return plotsTable.select(dataSource) {
            where("id" eq id)
            limit(1)
        }.firstOrNull {
            Plot(
                id = getLong("id"),
                ownerUUID = UUID.fromString(getString("owner_uuid")),
                gridX = getInt("grid_x"),
                gridZ = getInt("grid_z"),
                worldName = getString("world_name"),
                minX = getInt("min_x"),
                minZ = getInt("min_z"),
                maxX = getInt("max_x"),
                maxZ = getInt("max_z"),
                size = getInt("size")
            )
        }
    }

    override fun insertPlot(plot: Plot): Long {
        plotsTable.insert(dataSource,
            "owner_uuid", "grid_x", "grid_z", "world_name",
            "min_x", "min_z", "max_x", "max_z", "size"
        ) {
            value(
                plot.ownerUUID.toString(), plot.gridX, plot.gridZ, plot.worldName,
                plot.minX, plot.minZ, plot.maxX, plot.maxZ, plot.size
            )
        }
        return plotsTable.select(dataSource) {
            where("owner_uuid" eq plot.ownerUUID.toString())
            limit(1)
        }.firstOrNull { getLong("id") } ?: -1L
    }

    override fun updatePlotSize(plotId: Long, minX: Int, minZ: Int, maxX: Int, maxZ: Int, size: Int): Boolean {
        return plotsTable.update(dataSource) {
            where("id" eq plotId)
            set("min_x", minX)
            set("min_z", minZ)
            set("max_x", maxX)
            set("max_z", maxZ)
            set("size", size)
        } > 0
    }

    override fun deletePlot(plotId: Long): Boolean {
        return plotsTable.delete(dataSource) {
            where("id" eq plotId)
        } > 0
    }

    override fun getAllPlots(): List<Plot> {
        return plotsTable.select(dataSource) {
        }.map {
            Plot(
                id = getLong("id"),
                ownerUUID = UUID.fromString(getString("owner_uuid")),
                gridX = getInt("grid_x"),
                gridZ = getInt("grid_z"),
                worldName = getString("world_name"),
                minX = getInt("min_x"),
                minZ = getInt("min_z"),
                maxX = getInt("max_x"),
                maxZ = getInt("max_z"),
                size = getInt("size")
            )
        }
    }

    // ==================== farm_level_manager ====================

    override fun getPlayerFarmLevel(uuid: UUID): PlayerFarmLevel? {
        return playerLevelsTable.select(dataSource) {
            where("player_uuid" eq uuid.toString())
            limit(1)
        }.firstOrNull {
            PlayerFarmLevel(
                playerUUID = UUID.fromString(getString("player_uuid")),
                currentLevel = getInt("current_level")
            )
        }
    }

    override fun insertPlayerFarmLevel(uuid: UUID, level: Int): Boolean {
        return playerLevelsTable.insert(dataSource, "player_uuid", "current_level") {
            value(uuid.toString(), level)
        } > 0
    }

    override fun updatePlayerFarmLevel(uuid: UUID, level: Int): Boolean {
        return playerLevelsTable.update(dataSource) {
            where("player_uuid" eq uuid.toString())
            set("current_level", level)
        } > 0
    }

    // ==================== social_manager ====================

    override fun getFriends(uuid: UUID): List<FriendRelation> {
        val uuidStr = uuid.toString()
        val listA = friendsTable.select(dataSource) {
            where("player_a" eq uuidStr)
        }.map {
            FriendRelation(
                id = getLong("id"),
                playerA = UUID.fromString(getString("player_a")),
                playerB = UUID.fromString(getString("player_b")),
                createdAt = getLong("created_at")
            )
        }
        val listB = friendsTable.select(dataSource) {
            where("player_b" eq uuidStr)
        }.map {
            FriendRelation(
                id = getLong("id"),
                playerA = UUID.fromString(getString("player_a")),
                playerB = UUID.fromString(getString("player_b")),
                createdAt = getLong("created_at")
            )
        }
        return listA + listB
    }

    override fun insertFriend(playerA: UUID, playerB: UUID): Boolean {
        return friendsTable.insert(dataSource, "player_a", "player_b", "created_at") {
            value(playerA.toString(), playerB.toString(), System.currentTimeMillis())
        } > 0
    }

    override fun deleteFriend(playerA: UUID, playerB: UUID): Boolean {
        val aStr = playerA.toString()
        val bStr = playerB.toString()
        val deleted1 = friendsTable.delete(dataSource) {
            where("player_a" eq aStr)
            where("player_b" eq bStr)
        }
        val deleted2 = friendsTable.delete(dataSource) {
            where("player_a" eq bStr)
            where("player_b" eq aStr)
        }
        return (deleted1 + deleted2) > 0
    }

    override fun isFriend(playerA: UUID, playerB: UUID): Boolean {
        val aStr = playerA.toString()
        val bStr = playerB.toString()
        val found1 = friendsTable.select(dataSource) {
            where("player_a" eq aStr); where("player_b" eq bStr); limit(1)
        }.firstOrNull { true }
        if (found1 != null) return true
        val found2 = friendsTable.select(dataSource) {
            where("player_a" eq bStr); where("player_b" eq aStr); limit(1)
        }.firstOrNull { true }
        return found2 != null
    }

    override fun getEnemies(uuid: UUID): List<EnemyRecord> {
        return enemiesTable.select(dataSource) {
            where("victim_uuid" eq uuid.toString())
        }.map {
            EnemyRecord(
                id = getLong("id"),
                victimUUID = UUID.fromString(getString("victim_uuid")),
                thiefUUID = UUID.fromString(getString("thief_uuid")),
                markedAt = getLong("marked_at")
            )
        }
    }

    override fun insertEnemy(victimUUID: UUID, thiefUUID: UUID): Boolean {
        return enemiesTable.insert(dataSource, "victim_uuid", "thief_uuid", "marked_at") {
            value(victimUUID.toString(), thiefUUID.toString(), System.currentTimeMillis())
        } > 0
    }

    override fun deleteEnemy(victimUUID: UUID, thiefUUID: UUID): Boolean {
        return enemiesTable.delete(dataSource) {
            where("victim_uuid" eq victimUUID.toString())
            where("thief_uuid" eq thiefUUID.toString())
        } > 0
    }

    override fun isEnemy(victimUUID: UUID, thiefUUID: UUID): Boolean {
        return enemiesTable.select(dataSource) {
            where("victim_uuid" eq victimUUID.toString())
            where("thief_uuid" eq thiefUUID.toString())
            limit(1)
        }.firstOrNull { true } != null
    }

    override fun getPendingFriendRequests(uuid: UUID): List<FriendRequest> {
        return friendRequestsTable.select(dataSource) {
            where("receiver_uuid" eq uuid.toString())
            where("status" eq FriendRequestStatus.PENDING.name)
            order("requested_at", false)
        }.map {
            FriendRequest(
                id = getLong("id"),
                senderUUID = UUID.fromString(getString("sender_uuid")),
                receiverUUID = UUID.fromString(getString("receiver_uuid")),
                requestedAt = getLong("requested_at"),
                status = FriendRequestStatus.valueOf(getString("status"))
            )
        }
    }

    override fun insertFriendRequest(senderUUID: UUID, receiverUUID: UUID): Boolean {
        return friendRequestsTable.insert(dataSource,
            "sender_uuid", "receiver_uuid", "requested_at", "status"
        ) {
            value(senderUUID.toString(), receiverUUID.toString(), System.currentTimeMillis(), FriendRequestStatus.PENDING.name)
        } > 0
    }

    override fun updateFriendRequestStatus(id: Long, status: FriendRequestStatus): Boolean {
        return friendRequestsTable.update(dataSource) {
            where("id" eq id)
            set("status", status.name)
        } > 0
    }

    // ==================== steal_record_manager ====================

    override fun insertStealRecord(thiefUUID: UUID, victimUUID: UUID, cropType: String, amount: Int, timestamp: Long): Boolean {
        return stealRecordsTable.insert(dataSource,
            "thief_uuid", "victim_uuid", "crop_type", "amount", "timestamp"
        ) {
            value(thiefUUID.toString(), victimUUID.toString(), cropType, amount, timestamp)
        } > 0
    }

    override fun getStealRecordsByVictim(victimUUID: UUID, limit: Int): List<StealRecord> {
        return stealRecordsTable.select(dataSource) {
            where("victim_uuid" eq victimUUID.toString())
            order("timestamp", false)
            limit(limit)
        }.map {
            StealRecord(
                id = getLong("id"),
                thiefUUID = UUID.fromString(getString("thief_uuid")),
                victimUUID = UUID.fromString(getString("victim_uuid")),
                cropType = getString("crop_type"),
                amount = getInt("amount"),
                timestamp = getLong("timestamp")
            )
        }
    }

    override fun getStealRecordsByThief(thiefUUID: UUID, limit: Int): List<StealRecord> {
        return stealRecordsTable.select(dataSource) {
            where("thief_uuid" eq thiefUUID.toString())
            order("timestamp", false)
            limit(limit)
        }.map {
            StealRecord(
                id = getLong("id"),
                thiefUUID = UUID.fromString(getString("thief_uuid")),
                victimUUID = UUID.fromString(getString("victim_uuid")),
                cropType = getString("crop_type"),
                amount = getInt("amount"),
                timestamp = getLong("timestamp")
            )
        }
    }

    override fun getStealCooldown(thiefUUID: UUID, victimUUID: UUID): StealCooldown? {
        return stealCooldownsTable.select(dataSource) {
            where("thief_uuid" eq thiefUUID.toString())
            where("victim_uuid" eq victimUUID.toString())
            limit(1)
        }.firstOrNull {
            StealCooldown(
                thiefUUID = UUID.fromString(getString("thief_uuid")),
                victimUUID = UUID.fromString(getString("victim_uuid")),
                startTime = getLong("start_time"),
                duration = getLong("duration")
            )
        }
    }

    override fun setStealCooldown(thiefUUID: UUID, victimUUID: UUID, startTime: Long, duration: Long): Boolean {
        val existing = getStealCooldown(thiefUUID, victimUUID)
        return if (existing != null) {
            stealCooldownsTable.update(dataSource) {
                where("thief_uuid" eq thiefUUID.toString())
                where("victim_uuid" eq victimUUID.toString())
                set("start_time", startTime)
                set("duration", duration)
            } > 0
        } else {
            stealCooldownsTable.insert(dataSource,
                "thief_uuid", "victim_uuid", "start_time", "duration"
            ) {
                value(thiefUUID.toString(), victimUUID.toString(), startTime, duration)
            } > 0
        }
    }

    override fun removeStealCooldown(thiefUUID: UUID, victimUUID: UUID): Boolean {
        return stealCooldownsTable.delete(dataSource) {
            where("thief_uuid" eq thiefUUID.toString())
            where("victim_uuid" eq victimUUID.toString())
        } > 0
    }

    // ==================== trap_manager ====================

    override fun getDeployedTraps(plotId: Long): List<DeployedTrap> {
        return deployedTrapsTable.select(dataSource) {
            where("plot_id" eq plotId)
        }.map {
            DeployedTrap(
                id = getLong("id"),
                plotId = getLong("plot_id"),
                trapTypeId = getString("trap_type_id"),
                slotIndex = getInt("slot_index")
            )
        }
    }

    override fun deployTrap(plotId: Long, trapTypeId: String, slotIndex: Int): Boolean {
        return deployedTrapsTable.insert(dataSource, "plot_id", "trap_type_id", "slot_index") {
            value(plotId, trapTypeId, slotIndex)
        } > 0
    }

    override fun removeTrap(plotId: Long, slotIndex: Int): Boolean {
        return deployedTrapsTable.delete(dataSource) {
            where("plot_id" eq plotId)
            where("slot_index" eq slotIndex)
        } > 0
    }

    override fun removeAllTraps(plotId: Long): Boolean {
        return deployedTrapsTable.delete(dataSource) {
            where("plot_id" eq plotId)
        } >= 0
    }
}
