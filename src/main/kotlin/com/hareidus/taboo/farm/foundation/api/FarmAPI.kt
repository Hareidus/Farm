package com.hareidus.taboo.farm.foundation.api

import com.hareidus.taboo.farm.foundation.model.*
import com.hareidus.taboo.farm.modules.l1.crop.CropManager
import com.hareidus.taboo.farm.modules.l1.economy.EconomyManager
import com.hareidus.taboo.farm.modules.l1.farmlevel.FarmLevelManager
import com.hareidus.taboo.farm.modules.l1.guardpet.GuardPetManager
import com.hareidus.taboo.farm.modules.l1.plot.PlotManager
import com.hareidus.taboo.farm.modules.l1.social.SocialManager
import com.hareidus.taboo.farm.modules.l1.trap.TrapManager
import com.hareidus.taboo.farm.modules.l2.leaderboard.LeaderboardManager
import org.bukkit.OfflinePlayer
import java.util.UUID

/**
 * Farm 插件统一 API 入口
 *
 * 外部插件通过此对象访问 Farm 的所有公开功能。
 * 每个方法委托到对应的 L1/L2 Manager 单例。
 */
object FarmAPI {

    // ==================== Season Provider ====================

    /** 季节系统提供者（外部插件可注入实现） */
    var seasonProvider: ISeasonProvider? = null

    // ==================== Plot ====================

    fun getPlotByOwner(uuid: UUID): Plot? = PlotManager.getPlotByOwner(uuid)
    fun getPlotById(plotId: Long): Plot? = PlotManager.getPlotById(plotId)
    fun getPlotsByOwner(uuid: UUID): List<Plot> = PlotManager.getPlotsByOwner(uuid)
    fun getPlotsByOwnerAndType(uuid: UUID, type: PlotType): List<Plot> = PlotManager.getPlotsByOwnerAndType(uuid, type)
    fun getAllPlots(): List<Plot> = PlotManager.getAllPlots()
    fun getMaxPlots(uuid: UUID): Int = PlotManager.getMaxPlots(uuid)
    // ==================== Crop ====================

    fun getCropDefinition(id: String): CropDefinition? = CropManager.getCropDefinition(id)
    fun getAllCropDefinitions(): List<CropDefinition> = CropManager.getAllCropDefinitions()
    fun registerCropDefinition(def: CropDefinition): Boolean = CropManager.registerCropDefinition(def)
    fun unregisterCropDefinition(id: String): Boolean = CropManager.unregisterCropDefinition(id)
    fun getCropsByPlot(plotId: Long): List<CropInstance> = CropManager.getCropsByPlot(plotId)
    fun isCropMature(crop: CropInstance): Boolean = CropManager.isMature(crop)

    // ==================== Trap ====================

    fun getTrapDefinition(id: String): TrapDefinition? = TrapManager.getTrapDefinition(id)
    fun getAllTrapDefinitions(): List<TrapDefinition> = TrapManager.getAllTrapDefinitions()
    fun registerTrapDefinition(def: TrapDefinition): Boolean = TrapManager.registerTrapDefinition(def)
    fun unregisterTrapDefinition(id: String): Boolean = TrapManager.unregisterTrapDefinition(id)

    // ==================== Economy ====================

    fun getBalance(player: OfflinePlayer): Double = EconomyManager.getBalance(player)
    fun hasEnough(player: OfflinePlayer, amount: Double): Boolean = EconomyManager.hasEnough(player, amount)

    // ==================== Farm Level ====================

    fun getPlayerLevel(uuid: UUID): Int = FarmLevelManager.getPlayerLevel(uuid)
    fun getMaxLevel(): Int = FarmLevelManager.getMaxLevel()

    // ==================== Social ====================

    fun isFriend(a: UUID, b: UUID): Boolean = SocialManager.isFriend(a, b)
    fun isEnemy(victimUUID: UUID, thiefUUID: UUID): Boolean = SocialManager.isEnemy(victimUUID, thiefUUID)

    // ==================== Leaderboard ====================

    fun getLeaderboard(category: String, limit: Int): List<LeaderboardEntry> =
        LeaderboardManager.getLeaderboard(category, limit)

    // ==================== Guard Pet ====================

    fun getDeployedPet(plotId: Long): DeployedGuardPet? = GuardPetManager.getDeployedPet(plotId)

    // ==================== Season (预留) ====================

    fun getCurrentSeason(): Season? = seasonProvider?.getCurrentSeason()
    fun getSeasonModifier(cropTypeId: String): SeasonModifier? = seasonProvider?.getModifier(cropTypeId)

    // ==================== Plot Merge/Split (预留) ====================

    fun mergePlots(plotIds: List<Long>): Plot? = PlotManager.mergePlots(plotIds)
    fun splitPlot(plotId: Long, count: Int): List<Plot>? = PlotManager.splitPlot(plotId, count)
}
