# L1 模块 API 摘要（供 L2 Agent 参考）

包名前缀: `com.hareidus.taboo.farm.modules.l1`

## PlotManager (plot)
```kotlin
object PlotManager {
    val worldName: String  // 农场世界名
    fun getPlotByOwner(uuid: UUID): Plot?
    fun getPlotById(plotId: Long): Plot?
    fun getPlotByPosition(worldName: String, x: Int, z: Int): Plot?
    fun isInPlot(worldName: String, x: Int, z: Int, plot: Plot): Boolean
    fun isInOwnPlot(player: Player): Boolean
    fun getPlotOwnerAt(worldName: String, x: Int, z: Int): UUID?
    fun getAllPlots(): List<Plot>
    fun allocatePlot(uuid: UUID): Plot
    fun expandPlot(plotId: Long, sizeIncrease: Int)
    fun resetPlot(plotId: Long)
    fun generatePlotTerrain(plot: Plot)
    fun deletePlot(plotId: Long): Boolean
    fun getPlotCenter(plot: Plot): Triple<Int, Int, Int>
    fun reload()
}
// Events: PlotAllocatedEvent(player, plot), PlotExpandedEvent(plot, oldSize, newSize)
```

## CropManager (crop)
```kotlin
object CropManager {
    fun getCropDefinition(id: String): CropDefinition?
    fun getAllCropDefinitions(): List<CropDefinition>
    fun getCropTypeIdBySeed(seedItemId: String): String?
    fun getCropsByPlot(plotId: Long): List<CropInstance>
    fun getCropAtPosition(worldName: String, x: Int, y: Int, z: Int): CropInstance?
    fun getCropById(cropId: Long): CropInstance?
    fun calculateGrowthStage(crop: CropInstance): Int
    fun isMature(crop: CropInstance): Boolean
    fun calculateHarvestAmount(cropDef: CropDefinition): Int
    fun plantCrop(player: Player, cropTypeId: String, plotId: Long, location: Location): CropInstance?
    fun removeCrop(cropId: Long, reason: CropRemoveReason): Boolean
    fun removeAllCropsByPlot(plotId: Long): Boolean
    fun accelerateGrowth(cropId: Long, milliseconds: Long): Boolean
    fun updateCropVisuals(crop: CropInstance)
    fun updateAllCropsInPlot(plotId: Long)
    fun reload()
}
// Events: CropPlantedEvent, CropHarvestedEvent, CropGrowthUpdatedEvent, CropRemovedEvent
```

## EconomyManager (economy)
```kotlin
object EconomyManager {
    var isVaultAvailable: Boolean
    fun getBalance(player: OfflinePlayer): Double
    fun hasEnough(player: OfflinePlayer, amount: Double): Boolean
    fun deposit(player: OfflinePlayer, amount: Double): Boolean
    fun withdraw(player: OfflinePlayer, amount: Double): Boolean
    fun getCropPrice(cropId: String): Double?
    fun getAllCropPrices(): Map<String, Double>
    fun reload()
}
```

## PlayerDataManager (playerdata)
```kotlin
object PlayerDataManager {
    fun getPlayerData(uuid: UUID): PlayerData?
    fun getAllCachedData(): Map<UUID, PlayerData>
    fun updateStatistic(uuid: UUID, type: StatisticType, delta: Long)
    fun updateStatistic(uuid: UUID, type: StatisticType, delta: Double)
    fun addNotification(uuid: UUID, type: NotificationType, data: String)
    fun getUnreadNotifications(uuid: UUID): List<OfflineNotification>
    fun markNotificationsRead(uuid: UUID)
    fun pushOfflineNotifications(player: Player)
    fun resetPlayerData(uuid: UUID)
    fun reload()
}
// Events: PlayerDataLoadedEvent(playerUUID, playerData), PlayerStatisticUpdateEvent(playerUUID, statisticType, oldValue, newValue)
// Auto: onPlayerJoin loads data + pushes notifications, onPlayerQuit saves + clears cache
```

## FarmLevelManager (farmlevel)
```kotlin
object FarmLevelManager {
    fun getDefinition(level: Int): FarmLevelDefinition?
    fun getMaxLevel(): Int
    fun getPlayerLevel(uuid: UUID): Int
    fun setPlayerLevel(uuid: UUID, level: Int)
    fun getProtectionReduction(level: Int): Double
    fun isAutoHarvestUnlocked(level: Int): Boolean
    fun getTrapSlots(level: Int): Int
    fun invalidateCache(uuid: UUID)
    fun reload()
}
```

## SocialManager (social)
```kotlin
object SocialManager {
    val maxFriends: Int
    val friendRequestExpiryMs: Long
    fun getFriends(uuid: UUID): List<FriendRelation>
    fun isFriend(a: UUID, b: UUID): Boolean
    fun addFriend(a: UUID, b: UUID): Boolean
    fun removeFriend(a: UUID, b: UUID): Boolean
    fun getPendingRequests(uuid: UUID): List<FriendRequest>
    fun sendFriendRequest(sender: UUID, receiver: UUID): Boolean
    fun acceptFriendRequest(requestId: Long, receiverUUID: UUID): Boolean
    fun rejectFriendRequest(requestId: Long, receiverUUID: UUID): Boolean
    fun getEnemies(uuid: UUID): List<EnemyRecord>
    fun isEnemy(victim: UUID, thief: UUID): Boolean
    fun markEnemy(victimUUID: UUID, thiefUUID: UUID): Boolean
    fun removeEnemy(victim: UUID, thief: UUID): Boolean
    fun reload()
}
// Events: FriendAddedEvent, FriendRemovedEvent, EnemyMarkedEvent
```

## StealRecordManager (stealrecord)
```kotlin
object StealRecordManager {
    fun recordSteal(thief: UUID, victim: UUID, cropType: String, amount: Int): Boolean
    fun isOnCooldown(thief: UUID, victim: UUID): Boolean
    fun getCooldownRemaining(thief: UUID, victim: UUID): Long
    fun startCooldown(thief: UUID, victim: UUID)
    fun getVisitStealCount(thief: UUID, victim: UUID): Int
    fun incrementVisitStealCount(thief: UUID, victim: UUID)
    fun resetVisitStealCount(thief: UUID, victim: UUID)
    fun getRecentStealsAgainst(victim: UUID, limit: Int = 20): List<StealRecord>
    fun getRecentStealsBy(thief: UUID, limit: Int = 20): List<StealRecord>
    fun reload()
}
// Events: StealRecordCreatedEvent
```

## TrapManager (trap)
```kotlin
object TrapManager {
    fun getTrapDefinition(id: String): TrapDefinition?
    fun getAllTrapDefinitions(): List<TrapDefinition>
    fun getDeployedTraps(plotId: Long): List<DeployedTrap>
    fun deployTrap(plotId: Long, trapTypeId: String, slotIndex: Int): Boolean
    fun removeTrap(plotId: Long, slotIndex: Int): Boolean
    fun removeAllTraps(plotId: Long): Boolean
    fun checkTrapTrigger(plotId: Long): TrapDefinition?
    fun executePenalty(player: Player, trap: TrapDefinition)
    fun reload()
}
```

## DatabaseManager (database)
```kotlin
object DatabaseManager {
    val database: IDatabase  // 通过此字段访问所有 CRUD
    // IDatabase 包含: crop/achievement/leaderboard/storage/waterCooldown 等全部 CRUD
    // L2 模块不应直接调用 DatabaseManager，应通过 L1 Manager API
}
```

## 已完成的 L2 模块

### AchievementManager (achievement)
```kotlin
object AchievementManager {
    fun getDefinition(id: String): AchievementDefinition?
    fun getAllDefinitions(): List<AchievementDefinition>
    fun getPlayerAchievements(uuid: UUID): List<PlayerAchievement>
    fun isCompleted(uuid: UUID, achievementId: String): Boolean
    fun checkAndUpdateProgress(player: Player, triggerType: String, newValue: Long)
    fun completeAchievement(player: Player, achievementId: String)
    fun invalidateCache(uuid: UUID)
    fun reload()
}
```

### LeaderboardManager (leaderboard)
```kotlin
object LeaderboardManager {
    fun getLeaderboard(category: String, limit: Int = 10): List<LeaderboardEntry>
    fun getLeaderboardPage(category: String, page: Int): List<LeaderboardEntry>
    fun getTotalPages(category: String): Int
    fun getPlayerRank(uuid: UUID, category: String): Int?
    fun getCategories(): Set<String>
    fun getCategoryDisplayName(category: String): String
    fun refreshAll()
    fun reload()
}
```

### UpgradeManager (upgrade)
```kotlin
object UpgradeManager {
    fun getUpgradeInfo(uuid: UUID): UpgradeInfo?
    fun canUpgrade(player: Player): Boolean
    fun performUpgrade(player: Player): Boolean
    fun canDeployTrap(uuid: UUID): Boolean
    fun deployTrap(player: Player, trapTypeId: String, slotIndex: Int): Boolean
}
// Events: FarmUpgradedEvent, TrapDeployedEvent
```
