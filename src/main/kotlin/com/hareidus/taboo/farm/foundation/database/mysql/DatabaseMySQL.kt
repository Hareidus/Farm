package com.hareidus.taboo.farm.foundation.database.mysql

import com.hareidus.taboo.farm.foundation.config.MainConfig
import com.hareidus.taboo.farm.foundation.database.DatabaseType
import com.hareidus.taboo.farm.foundation.database.IDatabase
import com.hareidus.taboo.farm.foundation.model.*
import taboolib.module.database.*
import java.util.UUID
import javax.sql.DataSource

/**
 * MySQL 数据库实现
 *
 * 所有表定义和 CRUD 实现集中在此类。
 * 新增数据领域时：
 * 1. 添加 Table 字段声明
 * 2. 在 init 中调用 createXxxTable()
 * 3. 在 createAllTables() 中添加建表
 * 4. 实现 IDatabase 中声明的 CRUD 方法
 */
class DatabaseMySQL : IDatabase {

    val host: Host<SQL>
    override val type = DatabaseType.MYSQL
    override val dataSource: DataSource

    // ==================== 表定义 ====================

    private val playerDataTable: Table<Host<SQL>, SQL>
    private val offlineNotificationsTable: Table<Host<SQL>, SQL>
    private val plotsTable: Table<Host<SQL>, SQL>
    private val playerLevelsTable: Table<Host<SQL>, SQL>
    private val friendsTable: Table<Host<SQL>, SQL>
    private val enemiesTable: Table<Host<SQL>, SQL>
    private val friendRequestsTable: Table<Host<SQL>, SQL>
    private val stealRecordsTable: Table<Host<SQL>, SQL>
    private val stealCooldownsTable: Table<Host<SQL>, SQL>
    private val deployedTrapsTable: Table<Host<SQL>, SQL>
    private val cropsTable: Table<Host<SQL>, SQL>
    private val playerAchievementsTable: Table<Host<SQL>, SQL>
    private val storageTable: Table<Host<SQL>, SQL>
    private val waterCooldownsTable: Table<Host<SQL>, SQL>

    init {
        host = HostSQL(
            MainConfig.mysqlHost,
            MainConfig.mysqlPort.toString(),
            MainConfig.mysqlUser,
            MainConfig.mysqlPassword,
            MainConfig.mysqlDatabase
        )
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
        cropsTable = createCropsTable()
        playerAchievementsTable = createPlayerAchievementsTable()
        storageTable = createStorageTable()
        waterCooldownsTable = createWaterCooldownsTable()

        createAllTables()
    }

    // ==================== 表创建方法 ====================

    private fun createPlayerDataTable() = Table("farm_player_data", host) {
        add { id() }
        add("uuid") { type(ColumnTypeSQL.VARCHAR, 36) }
        add("total_harvest") { type(ColumnTypeSQL.BIGINT) }
        add("total_steal") { type(ColumnTypeSQL.BIGINT) }
        add("total_stolen") { type(ColumnTypeSQL.BIGINT) }
        add("total_coin_income") { type(ColumnTypeSQL.BIGINT) }
        add("trap_triggered_count") { type(ColumnTypeSQL.INT) }
        add("consecutive_login_days") { type(ColumnTypeSQL.INT) }
        add("last_login_date") { type(ColumnTypeSQL.BIGINT) }
    }

    private fun createOfflineNotificationsTable() = Table("farm_offline_notifications", host) {
        add { id() }
        add("player_uuid") { type(ColumnTypeSQL.VARCHAR, 36) }
        add("type") { type(ColumnTypeSQL.VARCHAR, 32) }
        add("data") { type(ColumnTypeSQL.TEXT) }
        add("timestamp") { type(ColumnTypeSQL.BIGINT) }
        add("read") { type(ColumnTypeSQL.TINYINT) }
    }

    private fun createPlotsTable() = Table("farm_plots", host) {
        add { id() }
        add("owner_uuid") { type(ColumnTypeSQL.VARCHAR, 36) }
        add("grid_x") { type(ColumnTypeSQL.INT) }
        add("grid_z") { type(ColumnTypeSQL.INT) }
        add("world_name") { type(ColumnTypeSQL.VARCHAR, 64) }
        add("min_x") { type(ColumnTypeSQL.INT) }
        add("min_z") { type(ColumnTypeSQL.INT) }
        add("max_x") { type(ColumnTypeSQL.INT) }
        add("max_z") { type(ColumnTypeSQL.INT) }
        add("size") { type(ColumnTypeSQL.INT) }
    }

    private fun createPlayerLevelsTable() = Table("farm_player_levels", host) {
        add { id() }
        add("player_uuid") { type(ColumnTypeSQL.VARCHAR, 36) }
        add("current_level") { type(ColumnTypeSQL.INT) }
    }

    private fun createFriendsTable() = Table("farm_friends", host) {
        add { id() }
        add("player_a") { type(ColumnTypeSQL.VARCHAR, 36) }
        add("player_b") { type(ColumnTypeSQL.VARCHAR, 36) }
        add("created_at") { type(ColumnTypeSQL.BIGINT) }
    }

    private fun createEnemiesTable() = Table("farm_enemies", host) {
        add { id() }
        add("victim_uuid") { type(ColumnTypeSQL.VARCHAR, 36) }
        add("thief_uuid") { type(ColumnTypeSQL.VARCHAR, 36) }
        add("marked_at") { type(ColumnTypeSQL.BIGINT) }
    }

    private fun createFriendRequestsTable() = Table("farm_friend_requests", host) {
        add { id() }
        add("sender_uuid") { type(ColumnTypeSQL.VARCHAR, 36) }
        add("receiver_uuid") { type(ColumnTypeSQL.VARCHAR, 36) }
        add("requested_at") { type(ColumnTypeSQL.BIGINT) }
        add("status") { type(ColumnTypeSQL.VARCHAR, 16) }
    }

    private fun createStealRecordsTable() = Table("farm_steal_records", host) {
        add { id() }
        add("thief_uuid") { type(ColumnTypeSQL.VARCHAR, 36) }
        add("victim_uuid") { type(ColumnTypeSQL.VARCHAR, 36) }
        add("crop_type") { type(ColumnTypeSQL.VARCHAR, 64) }
        add("amount") { type(ColumnTypeSQL.INT) }
        add("timestamp") { type(ColumnTypeSQL.BIGINT) }
    }

    private fun createStealCooldownsTable() = Table("farm_steal_cooldowns", host) {
        add("thief_uuid") { type(ColumnTypeSQL.VARCHAR, 36) }
        add("victim_uuid") { type(ColumnTypeSQL.VARCHAR, 36) }
        add("start_time") { type(ColumnTypeSQL.BIGINT) }
        add("duration") { type(ColumnTypeSQL.BIGINT) }
    }

    private fun createDeployedTrapsTable() = Table("farm_deployed_traps", host) {
        add { id() }
        add("plot_id") { type(ColumnTypeSQL.BIGINT) }
        add("trap_type_id") { type(ColumnTypeSQL.VARCHAR, 64) }
        add("slot_index") { type(ColumnTypeSQL.INT) }
    }

    private fun createCropsTable() = Table("farm_crops", host) {
        add { id() }
        add("crop_type_id") { type(ColumnTypeSQL.VARCHAR, 64) }
        add("plot_id") { type(ColumnTypeSQL.BIGINT) }
        add("owner_uuid") { type(ColumnTypeSQL.VARCHAR, 36) }
        add("world_name") { type(ColumnTypeSQL.VARCHAR, 64) }
        add("x") { type(ColumnTypeSQL.INT) }
        add("y") { type(ColumnTypeSQL.INT) }
        add("z") { type(ColumnTypeSQL.INT) }
        add("planted_at") { type(ColumnTypeSQL.BIGINT) }
    }

    private fun createPlayerAchievementsTable() = Table("farm_player_achievements", host) {
        add { id() }
        add("player_uuid") { type(ColumnTypeSQL.VARCHAR, 36) }
        add("achievement_id") { type(ColumnTypeSQL.VARCHAR, 64) }
        add("current_progress") { type(ColumnTypeSQL.BIGINT) }
        add("completed") { type(ColumnTypeSQL.TINYINT) }
        add("completed_at") { type(ColumnTypeSQL.BIGINT) }
    }

    private fun createStorageTable() = Table("farm_storage", host) {
        add { id() }
        add("player_uuid") { type(ColumnTypeSQL.VARCHAR, 36) }
        add("item_type") { type(ColumnTypeSQL.VARCHAR, 64) }
        add("amount") { type(ColumnTypeSQL.INT) }
    }

    private fun createWaterCooldownsTable() = Table("farm_water_cooldowns", host) {
        add("waterer_uuid") { type(ColumnTypeSQL.VARCHAR, 36) }
        add("target_uuid") { type(ColumnTypeSQL.VARCHAR, 36) }
        add("cooldown_end_time") { type(ColumnTypeSQL.BIGINT) }
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
        cropsTable.workspace(dataSource) { createTable() }.run()
        playerAchievementsTable.workspace(dataSource) { createTable() }.run()
        storageTable.workspace(dataSource) { createTable() }.run()
        waterCooldownsTable.workspace(dataSource) { createTable() }.run()
    }

    override fun close() {
        // MySQL DataSource 由 TabooLib 管理
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
            "total_coin_income", "trap_triggered_count",
            "consecutive_login_days", "last_login_date"
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
        val fromA = friendsTable.select(dataSource) {
            where("player_a" eq uuidStr)
        }.map {
            FriendRelation(
                id = getLong("id"),
                playerA = UUID.fromString(getString("player_a")),
                playerB = UUID.fromString(getString("player_b")),
                createdAt = getLong("created_at")
            )
        }
        val fromB = friendsTable.select(dataSource) {
            where("player_b" eq uuidStr)
        }.map {
            FriendRelation(
                id = getLong("id"),
                playerA = UUID.fromString(getString("player_a")),
                playerB = UUID.fromString(getString("player_b")),
                createdAt = getLong("created_at")
            )
        }
        return fromA + fromB
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
        val found = friendsTable.select(dataSource) {
            where("player_a" eq aStr)
            where("player_b" eq bStr)
            limit(1)
        }.firstOrNull { true }
        if (found != null) return true
        return friendsTable.select(dataSource) {
            where("player_a" eq bStr)
            where("player_b" eq aStr)
            limit(1)
        }.firstOrNull { true } != null
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
            value(senderUUID.toString(), receiverUUID.toString(),
                System.currentTimeMillis(), FriendRequestStatus.PENDING.name)
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
        val thiefStr = thiefUUID.toString()
        val victimStr = victimUUID.toString()
        val existing = getStealCooldown(thiefUUID, victimUUID)
        return if (existing != null) {
            stealCooldownsTable.update(dataSource) {
                where("thief_uuid" eq thiefStr)
                where("victim_uuid" eq victimStr)
                set("start_time", startTime)
                set("duration", duration)
            } > 0
        } else {
            stealCooldownsTable.insert(dataSource,
                "thief_uuid", "victim_uuid", "start_time", "duration"
            ) {
                value(thiefStr, victimStr, startTime, duration)
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
            order("slot_index")
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

    // ==================== crop_manager ====================

    override fun insertCrop(cropTypeId: String, plotId: Long, ownerUUID: UUID, worldName: String, x: Int, y: Int, z: Int, plantedAt: Long): Long {
        cropsTable.insert(dataSource,
            "crop_type_id", "plot_id", "owner_uuid", "world_name", "x", "y", "z", "planted_at"
        ) {
            value(cropTypeId, plotId, ownerUUID.toString(), worldName, x, y, z, plantedAt)
        }
        return cropsTable.select(dataSource) {
            where("owner_uuid" eq ownerUUID.toString())
            where("world_name" eq worldName)
            where("x" eq x)
            where("y" eq y)
            where("z" eq z)
            limit(1)
        }.firstOrNull { getLong("id") } ?: -1L
    }

    override fun getCropsByPlot(plotId: Long): List<CropInstance> {
        return cropsTable.select(dataSource) {
            where("plot_id" eq plotId)
        }.map {
            CropInstance(
                id = getLong("id"),
                cropTypeId = getString("crop_type_id"),
                plotId = getLong("plot_id"),
                ownerUUID = UUID.fromString(getString("owner_uuid")),
                worldName = getString("world_name"),
                x = getInt("x"),
                y = getInt("y"),
                z = getInt("z"),
                plantedAt = getLong("planted_at")
            )
        }
    }

    override fun getCropByPosition(worldName: String, x: Int, y: Int, z: Int): CropInstance? {
        return cropsTable.select(dataSource) {
            where("world_name" eq worldName)
            where("x" eq x)
            where("y" eq y)
            where("z" eq z)
            limit(1)
        }.firstOrNull {
            CropInstance(
                id = getLong("id"),
                cropTypeId = getString("crop_type_id"),
                plotId = getLong("plot_id"),
                ownerUUID = UUID.fromString(getString("owner_uuid")),
                worldName = getString("world_name"),
                x = getInt("x"),
                y = getInt("y"),
                z = getInt("z"),
                plantedAt = getLong("planted_at")
            )
        }
    }

    override fun getCropById(id: Long): CropInstance? {
        return cropsTable.select(dataSource) {
            where("id" eq id)
            limit(1)
        }.firstOrNull {
            CropInstance(
                id = getLong("id"),
                cropTypeId = getString("crop_type_id"),
                plotId = getLong("plot_id"),
                ownerUUID = UUID.fromString(getString("owner_uuid")),
                worldName = getString("world_name"),
                x = getInt("x"),
                y = getInt("y"),
                z = getInt("z"),
                plantedAt = getLong("planted_at")
            )
        }
    }

    override fun removeCrop(id: Long): Boolean {
        return cropsTable.delete(dataSource) {
            where("id" eq id)
        } > 0
    }

    override fun removeAllCropsByPlot(plotId: Long): Boolean {
        return cropsTable.delete(dataSource) {
            where("plot_id" eq plotId)
        } >= 0
    }

    override fun updateCropPlantedAt(id: Long, plantedAt: Long): Boolean {
        return cropsTable.update(dataSource) {
            where("id" eq id)
            set("planted_at", plantedAt)
        } > 0
    }

    // ==================== achievement_manager ====================

    override fun getPlayerAchievements(uuid: UUID): List<PlayerAchievement> {
        return playerAchievementsTable.select(dataSource) {
            where("player_uuid" eq uuid.toString())
        }.map {
            PlayerAchievement(
                id = getLong("id"),
                playerUUID = UUID.fromString(getString("player_uuid")),
                achievementId = getString("achievement_id"),
                currentProgress = getLong("current_progress"),
                completed = getInt("completed") == 1,
                completedAt = getLong("completed_at").let { if (it == 0L) null else it }
            )
        }
    }

    override fun getPlayerAchievement(uuid: UUID, achievementId: String): PlayerAchievement? {
        return playerAchievementsTable.select(dataSource) {
            where("player_uuid" eq uuid.toString())
            where("achievement_id" eq achievementId)
            limit(1)
        }.firstOrNull {
            PlayerAchievement(
                id = getLong("id"),
                playerUUID = UUID.fromString(getString("player_uuid")),
                achievementId = getString("achievement_id"),
                currentProgress = getLong("current_progress"),
                completed = getInt("completed") == 1,
                completedAt = getLong("completed_at").let { if (it == 0L) null else it }
            )
        }
    }

    override fun insertPlayerAchievement(uuid: UUID, achievementId: String): Boolean {
        return playerAchievementsTable.insert(dataSource,
            "player_uuid", "achievement_id", "current_progress", "completed", "completed_at"
        ) {
            value(uuid.toString(), achievementId, 0L, 0, 0L)
        } > 0
    }

    override fun updatePlayerAchievement(uuid: UUID, achievementId: String, progress: Long, completed: Boolean, completedAt: Long?): Boolean {
        return playerAchievementsTable.update(dataSource) {
            where("player_uuid" eq uuid.toString())
            where("achievement_id" eq achievementId)
            set("current_progress", progress)
            set("completed", if (completed) 1 else 0)
            set("completed_at", completedAt ?: 0L)
        } > 0
    }

    // ==================== leaderboard_manager ====================

    override fun getTopPlayers(statisticColumn: String, limit: Int): List<LeaderboardEntry> {
        val allowedColumns = setOf("total_harvest", "total_steal", "total_stolen", "total_coin_income", "trap_triggered_count")
        if (statisticColumn !in allowedColumns) return emptyList()
        val results = mutableListOf<LeaderboardEntry>()
        dataSource.connection.use { conn ->
            conn.prepareStatement("SELECT uuid, $statisticColumn FROM farm_player_data ORDER BY $statisticColumn DESC LIMIT ?").use { stmt ->
                stmt.setInt(1, limit)
                val rs = stmt.executeQuery()
                var rank = 1
                while (rs.next()) {
                    results.add(LeaderboardEntry(
                        playerUUID = UUID.fromString(rs.getString("uuid")),
                        playerName = "",
                        category = statisticColumn,
                        value = rs.getLong(statisticColumn),
                        rank = rank++
                    ))
                }
            }
        }
        return results
    }

    // ==================== harvest_manager (FarmStorage) ====================

    override fun getFarmStorage(uuid: UUID): List<FarmStorage> {
        return storageTable.select(dataSource) {
            where("player_uuid" eq uuid.toString())
        }.map {
            FarmStorage(
                id = getLong("id"),
                playerUUID = UUID.fromString(getString("player_uuid")),
                itemType = getString("item_type"),
                amount = getInt("amount")
            )
        }
    }

    override fun insertOrUpdateFarmStorage(uuid: UUID, itemType: String, amount: Int): Boolean {
        val uuidStr = uuid.toString()
        val existing = storageTable.select(dataSource) {
            where("player_uuid" eq uuidStr)
            where("item_type" eq itemType)
            limit(1)
        }.firstOrNull { getInt("amount") }
        return if (existing != null) {
            storageTable.update(dataSource) {
                where("player_uuid" eq uuidStr)
                where("item_type" eq itemType)
                set("amount", existing + amount)
            } > 0
        } else {
            storageTable.insert(dataSource, "player_uuid", "item_type", "amount") {
                value(uuidStr, itemType, amount)
            } > 0
        }
    }

    override fun clearFarmStorage(uuid: UUID): Boolean {
        return storageTable.delete(dataSource) {
            where("player_uuid" eq uuid.toString())
        } >= 0
    }

    // ==================== friend_interaction_manager (WaterCooldown) ====================

    override fun getWaterCooldown(watererUUID: UUID, targetUUID: UUID): WaterCooldown? {
        return waterCooldownsTable.select(dataSource) {
            where("waterer_uuid" eq watererUUID.toString())
            where("target_uuid" eq targetUUID.toString())
            limit(1)
        }.firstOrNull {
            WaterCooldown(
                watererUUID = UUID.fromString(getString("waterer_uuid")),
                targetUUID = UUID.fromString(getString("target_uuid")),
                cooldownEndTime = getLong("cooldown_end_time")
            )
        }
    }

    override fun setWaterCooldown(watererUUID: UUID, targetUUID: UUID, cooldownEndTime: Long): Boolean {
        val watererStr = watererUUID.toString()
        val targetStr = targetUUID.toString()
        val existing = getWaterCooldown(watererUUID, targetUUID)
        return if (existing != null) {
            waterCooldownsTable.update(dataSource) {
                where("waterer_uuid" eq watererStr)
                where("target_uuid" eq targetStr)
                set("cooldown_end_time", cooldownEndTime)
            } > 0
        } else {
            waterCooldownsTable.insert(dataSource,
                "waterer_uuid", "target_uuid", "cooldown_end_time"
            ) {
                value(watererStr, targetStr, cooldownEndTime)
            } > 0
        }
    }

    override fun removeWaterCooldown(watererUUID: UUID, targetUUID: UUID): Boolean {
        return waterCooldownsTable.delete(dataSource) {
            where("waterer_uuid" eq watererUUID.toString())
            where("target_uuid" eq targetUUID.toString())
        } > 0
    }
}
