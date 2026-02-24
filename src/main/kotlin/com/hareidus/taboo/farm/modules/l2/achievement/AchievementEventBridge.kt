package com.hareidus.taboo.farm.modules.l2.achievement

import com.hareidus.taboo.farm.modules.l1.crop.CropHarvestedEvent
import com.hareidus.taboo.farm.modules.l2.shop.CropSoldEvent
import com.hareidus.taboo.farm.modules.l2.steal.CropStolenEvent
import com.hareidus.taboo.farm.modules.l2.steal.TrapTriggeredEvent
import com.hareidus.taboo.farm.modules.l1.playerdata.PlayerDataManager
import org.bukkit.Bukkit
import org.bukkit.event.player.PlayerJoinEvent
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.event.SubscribeEvent
import taboolib.common.platform.function.info

object AchievementEventBridge {

    @Awake(LifeCycle.ENABLE)
    fun init() {
        info("[Farm] AchievementEventBridge enabled.")
    }

    @SubscribeEvent
    fun onCropHarvested(event: CropHarvestedEvent) {
        val player = event.player ?: return
        val data = PlayerDataManager.getPlayerData(player.uniqueId) ?: return
        AchievementManager.checkAndUpdateProgress(player, "TOTAL_HARVEST", data.totalHarvest)
    }

    @SubscribeEvent
    fun onCropStolen(event: CropStolenEvent) {
        // Check thief's TOTAL_STEAL
        val thief = Bukkit.getPlayer(event.thiefUUID)
        if (thief != null) {
            val thiefData = PlayerDataManager.getPlayerData(event.thiefUUID)
            if (thiefData != null) {
                AchievementManager.checkAndUpdateProgress(thief, "TOTAL_STEAL", thiefData.totalSteal)
            }
        }
        // Check victim's TOTAL_STOLEN
        val victim = Bukkit.getPlayer(event.victimUUID)
        if (victim != null) {
            val victimData = PlayerDataManager.getPlayerData(event.victimUUID)
            if (victimData != null) {
                AchievementManager.checkAndUpdateProgress(victim, "TOTAL_STOLEN", victimData.totalStolen)
            }
        }
    }

    @SubscribeEvent
    fun onTrapTriggered(event: TrapTriggeredEvent) {
        val thief = Bukkit.getPlayer(event.thiefUUID) ?: return
        val data = PlayerDataManager.getPlayerData(event.thiefUUID) ?: return
        AchievementManager.checkAndUpdateProgress(thief, "TRAP_TRIGGERED_COUNT", data.trapTriggeredCount.toLong())
    }

    @SubscribeEvent
    fun onCropSold(event: CropSoldEvent) {
        val player = Bukkit.getPlayer(event.playerUUID) ?: return
        val data = PlayerDataManager.getPlayerData(event.playerUUID) ?: return
        AchievementManager.checkAndUpdateProgress(player, "TOTAL_COIN_INCOME", data.totalCoinIncome.toLong())
    }

    @SubscribeEvent
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player
        val data = PlayerDataManager.getPlayerData(player.uniqueId) ?: return
        AchievementManager.checkAndUpdateProgress(player, "CONSECUTIVE_LOGIN_DAYS", data.consecutiveLoginDays.toLong())
    }
}
