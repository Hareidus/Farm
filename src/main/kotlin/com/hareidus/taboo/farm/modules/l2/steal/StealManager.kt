package com.hareidus.taboo.farm.modules.l2.steal

import com.hareidus.taboo.farm.foundation.model.*
import com.hareidus.taboo.farm.modules.l1.crop.CropManager
import com.hareidus.taboo.farm.modules.l1.economy.EconomyManager
import com.hareidus.taboo.farm.modules.l1.farmlevel.FarmLevelManager
import com.hareidus.taboo.farm.modules.l1.playerdata.PlayerDataManager
import com.hareidus.taboo.farm.modules.l1.plot.PlotManager
import com.hareidus.taboo.farm.modules.l1.social.SocialManager
import com.hareidus.taboo.farm.modules.l1.stealrecord.StealRecordManager
import com.hareidus.taboo.farm.modules.l1.trap.TrapManager
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.event.SubscribeEvent
import taboolib.common.platform.function.info
import taboolib.common.platform.function.warning
import taboolib.module.configuration.Config
import taboolib.module.configuration.Configuration
import taboolib.platform.util.sendLang
import java.util.UUID

/**
 * 偷菜管理器 (L2)
 *
 * 职责：
 * 1. 监听 PlayerInteractEvent，当玩家在他人农场右键成熟作物时触发偷菜流程
 * 2. 流程: 权限校验 → 冷却检查 → 比例上限计算 → 陷阱判定 → 产出发放 → 记录 → 统计 → 仇人标记 → 通知
 * 3. 处理冷却阻止、比例上限阻止、陷阱触发等分支
 *
 * 依赖: CropManager, PlotManager, StealRecordManager, TrapManager,
 *       SocialManager, PlayerDataManager, FarmLevelManager, EconomyManager
 */
object StealManager {

    @Config("modules/l2/steal.yml", autoReload = true)
    lateinit var config: Configuration
        private set

    private var baseStealRatio: Double = 0.3
    private var enemyBonusRatio: Double = 0.1
    private var permissionSteal: String = "stealfarm.steal"

    @Awake(LifeCycle.ENABLE)
    fun init() {
        loadConfig()
        info("[Farm] 偷菜管理器已初始化 (基础比例: $baseStealRatio, 仇人加成: $enemyBonusRatio)")
    }

    private fun loadConfig() {
        baseStealRatio = config.getDouble("base-steal-ratio", 0.3)
        enemyBonusRatio = config.getDouble("enemy-bonus-ratio", 0.1)
        permissionSteal = config.getString("permission-steal", "stealfarm.steal")!!
    }

    // ==================== 事件监听 ====================

    /**
     * 监听玩家右键交互事件
     * 当玩家在他人农场右键点击成熟作物时触发偷菜流程
     */
    @SubscribeEvent
    fun onPlayerInteract(e: PlayerInteractEvent) {
        if (e.action != Action.RIGHT_CLICK_BLOCK) return
        val block = e.clickedBlock ?: return
        val player = e.player
        val worldName = block.world.name

        // 检查是否在农场世界
        if (worldName != PlotManager.worldName) return

        // 检查点击位置是否有作物
        val crop = CropManager.getCropAtPosition(worldName, block.x, block.y, block.z) ?: return

        // 检查是否在他人地块（非自己的）
        val plotOwner = PlotManager.getPlotOwnerAt(worldName, block.x, block.z) ?: return
        if (plotOwner == player.uniqueId) return

        // 取消原始交互
        e.isCancelled = true

        // 执行偷菜流程
        attemptSteal(player, crop, plotOwner)
    }

    // ==================== 偷菜核心流程 ====================

    /**
     * 尝试偷取一株作物
     *
     * 流程:
     * 1. 权限校验
     * 2. 成熟度校验
     * 3. 冷却检查
     * 4. 比例上限检查
     * 5. 陷阱触发判定
     * 6. 产出发放
     * 7. 移除作物
     * 8. 记录日志
     * 9. 更新双方统计
     * 10. 标记仇人
     * 11. 通知被偷者
     */
    private fun attemptSteal(player: Player, crop: CropInstance, victimUUID: UUID) {
        val thiefUUID = player.uniqueId

        // 1. 权限校验
        if (!player.hasPermission(permissionSteal)) {
            player.sendLang("steal-no-permission")
            return
        }

        // 2. 成熟度校验
        if (!CropManager.isMature(crop)) {
            player.sendLang("steal-crop-not-mature")
            return
        }

        // 3. 冷却检查
        if (StealRecordManager.isOnCooldown(thiefUUID, victimUUID)) {
            val remaining = StealRecordManager.getCooldownRemaining(thiefUUID, victimUUID)
            player.sendLang("steal-cooldown-remaining", formatCooldown(remaining))
            return
        }

        // 4. 比例上限检查
        val plot = PlotManager.getPlotByOwner(victimUUID) ?: return
        val stealLimit = calculateStealLimit(thiefUUID, victimUUID, plot.id)
        val alreadyStolen = StealRecordManager.getVisitStealCount(thiefUUID, victimUUID)

        if (alreadyStolen >= stealLimit) {
            // 达到上限，开始冷却
            StealRecordManager.startCooldown(thiefUUID, victimUUID)
            StealRecordManager.resetVisitStealCount(thiefUUID, victimUUID)
            val cooldownEnd = System.currentTimeMillis() +
                StealRecordManager.getCooldownRemaining(thiefUUID, victimUUID)
            StealCooldownStartedEvent(thiefUUID, victimUUID, cooldownEnd).call()
            player.sendLang("steal-limit-reached")
            return
        }

        // 5. 陷阱触发判定
        val triggeredTrap = TrapManager.checkTrapTrigger(plot.id)
        if (triggeredTrap != null) {
            handleTrapTriggered(player, triggeredTrap, victimUUID)
            // 陷阱触发后本次偷菜仍然继续（惩罚 + 偷取并行）
        }

        // 6. 计算产出
        val cropDef = CropManager.getCropDefinition(crop.cropTypeId)
        if (cropDef == null) {
            warning("[Farm] 偷菜时找不到作物定义: ${crop.cropTypeId}")
            return
        }
        val amount = CropManager.calculateHarvestAmount(cropDef)

        // 7. 移除作物
        val removed = CropManager.removeCrop(crop.id, CropRemoveReason.STOLEN)
        if (!removed) {
            player.sendLang("steal-remove-failed")
            return
        }

        // 8. 发放产出到偷取者背包
        giveHarvestItems(player, cropDef.harvestItemId, amount)

        // 9. 记录偷菜日志
        StealRecordManager.recordSteal(thiefUUID, victimUUID, crop.cropTypeId, amount)
        StealRecordManager.incrementVisitStealCount(thiefUUID, victimUUID)

        // 10. 更新双方统计
        PlayerDataManager.updateStatistic(thiefUUID, StatisticType.TOTAL_STEAL, amount.toLong())
        PlayerDataManager.updateStatistic(victimUUID, StatisticType.TOTAL_STOLEN, amount.toLong())

        // 11. 标记仇人
        SocialManager.markEnemy(victimUUID, thiefUUID)

        // 12. 通知
        notifyVictim(thiefUUID, victimUUID, cropDef.name, amount)

        // 13. 发布事件
        CropStolenEvent(thiefUUID, victimUUID, crop.cropTypeId, amount).call()

        // 14. 通知偷取者
        player.sendLang("steal-success", amount, cropDef.name)
    }

    // ==================== 比例上限计算 ====================

    /**
     * 计算偷取者对某地块的可偷数量上限
     *
     * 公式: 成熟作物总数 × (基础比例 - 防护等级降低 + 仇人加成)
     * 最终比例不低于 0，上限至少为 1（如果有成熟作物）
     */
    private fun calculateStealLimit(thiefUUID: UUID, victimUUID: UUID, plotId: Long): Int {
        val crops = CropManager.getCropsByPlot(plotId)
        val matureCount = crops.count { CropManager.isMature(it) }
        if (matureCount <= 0) return 0

        val victimLevel = FarmLevelManager.getPlayerLevel(victimUUID)
        val protectionReduction = FarmLevelManager.getProtectionReduction(victimLevel)

        var ratio = baseStealRatio - protectionReduction
        if (SocialManager.isEnemy(thiefUUID, victimUUID)) {
            ratio += enemyBonusRatio
        }
        ratio = ratio.coerceAtLeast(0.0)

        val limit = (matureCount * ratio).toInt()
        return if (limit <= 0 && matureCount > 0 && ratio > 0) 1 else limit
    }

    // ==================== 陷阱处理 ====================

    /**
     * 处理陷阱触发：执行惩罚 + 记录统计 + 发布事件
     */
    private fun handleTrapTriggered(player: Player, trap: TrapDefinition, farmOwnerUUID: UUID) {
        val thiefUUID = player.uniqueId

        // 执行惩罚效果
        TrapManager.executePenalty(player, trap)

        // 累加偷取者的陷阱触发次数
        PlayerDataManager.updateStatistic(thiefUUID, StatisticType.TRAP_TRIGGERED_COUNT, 1L)

        // 通知偷取者
        player.sendLang("steal-trap-triggered")

        // 发布事件
        TrapTriggeredEvent(thiefUUID, farmOwnerUUID, trap.id, trap.penaltyType.name).call()
    }

    // ==================== 产出发放 ====================

    /**
     * 将偷取的作物产出放入玩家背包
     */
    private fun giveHarvestItems(player: Player, harvestItemId: String, amount: Int) {
        val material = Material.matchMaterial(harvestItemId) ?: run {
            warning("[Farm] 无法识别收获物品材质: $harvestItemId")
            return
        }
        val item = ItemStack(material, amount)
        val leftover = player.inventory.addItem(item)
        if (leftover.isNotEmpty()) {
            // 背包满了，掉落到地上
            for (drop in leftover.values) {
                player.world.dropItemNaturally(player.location, drop)
            }
        }
    }

    // ==================== 通知 ====================

    /**
     * 通知被偷者：在线直接推送，离线写入通知队列
     */
    private fun notifyVictim(thiefUUID: UUID, victimUUID: UUID, cropName: String, amount: Int) {
        val thiefName = Bukkit.getOfflinePlayer(thiefUUID).name ?: thiefUUID.toString()
        val victim = Bukkit.getPlayer(victimUUID)
        if (victim != null && victim.isOnline) {
            victim.sendLang("steal-victim-online-notify", thiefName, amount, cropName)
        } else {
            PlayerDataManager.addNotification(
                victimUUID,
                NotificationType.STOLEN,
                "$thiefName|$cropName|$amount"
            )
        }
    }

    // ==================== 工具方法 ====================

    /**
     * 将毫秒冷却时间格式化为可读字符串
     */
    private fun formatCooldown(millis: Long): String {
        val totalSeconds = millis / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return buildString {
            if (hours > 0) append("${hours}h")
            if (minutes > 0) append("${minutes}m")
            if (seconds > 0 || (hours == 0L && minutes == 0L)) append("${seconds}s")
        }
    }

    // ==================== 重载 ====================

    fun reload() {
        config.reload()
        loadConfig()
    }
}
