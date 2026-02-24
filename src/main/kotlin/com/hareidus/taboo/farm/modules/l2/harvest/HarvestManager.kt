package com.hareidus.taboo.farm.modules.l2.harvest

import com.hareidus.taboo.farm.foundation.database.DatabaseManager
import com.hareidus.taboo.farm.foundation.model.CropInstance
import com.hareidus.taboo.farm.foundation.model.CropRemoveReason
import com.hareidus.taboo.farm.foundation.model.StatisticType
import com.hareidus.taboo.farm.modules.l1.crop.CropHarvestedEvent
import com.hareidus.taboo.farm.modules.l1.crop.CropManager
import com.hareidus.taboo.farm.modules.l2.farmteleport.PlayerEnterOwnFarmEvent
import com.hareidus.taboo.farm.modules.l1.farmlevel.FarmLevelManager
import com.hareidus.taboo.farm.modules.l1.playerdata.PlayerDataManager
import com.hareidus.taboo.farm.modules.l1.plot.PlotManager
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.entity.Player
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.event.EventPriority
import taboolib.common.platform.event.SubscribeEvent
import taboolib.common.platform.function.info
import taboolib.common.platform.function.warning
import taboolib.module.configuration.Config
import taboolib.module.configuration.Configuration
import taboolib.platform.util.sendLang

/**
 * 收割管理器 (L2)
 *
 * 职责：
 * 1. 监听 PlayerInteractEvent，处理种植、收割、骨粉加速
 * 2. 监听 PlayerEnterOwnFarmEvent，处理自动收割
 * 3. 累加玩家收获统计
 *
 * 依赖: crop_manager, plot_manager, player_data_manager, farm_level_manager
 */
object HarvestManager {

    @Config("modules/l2/harvest.yml", autoReload = true)
    lateinit var config: Configuration
        private set

    @Awake(LifeCycle.ENABLE)
    fun init() {
        info("[Farm] 收割管理器已启动")
    }

    // ==================== 配置读取 ====================

    private fun getBonemealAccelerationMs(): Long {
        return config.getLong("bonemeal-acceleration-ms", 60000)
    }

    private fun getHarvestParticle(): Particle {
        return try {
            Particle.valueOf(config.getString("harvest-particle", "HAPPY_VILLAGER")!!)
        } catch (_: Exception) {
            Particle.HAPPY_VILLAGER
        }
    }

    private fun getBonemealParticle(): Particle {
        return try {
            Particle.valueOf(config.getString("bonemeal-particle", "HAPPY_VILLAGER")!!)
        } catch (_: Exception) {
            Particle.HAPPY_VILLAGER
        }
    }

    private fun getParticleCount(): Int {
        return config.getInt("particle-count", 10)
    }

    // ==================== 事件监听: PlayerInteractEvent ====================

    @SubscribeEvent(priority = EventPriority.NORMAL, ignoreCancelled = true)
    fun onPlayerInteract(e: PlayerInteractEvent) {
        if (e.action != Action.RIGHT_CLICK_BLOCK) return
        if (e.hand != EquipmentSlot.HAND) return
        val block = e.clickedBlock ?: return
        val player = e.player
        val worldName = block.world.name

        // 必须在农场世界
        if (worldName != PlotManager.worldName) return

        val blockX = block.x
        val blockY = block.y
        val blockZ = block.z
        val itemInHand = player.inventory.itemInMainHand

        // 判断交互类型
        val cropAtPos = CropManager.getCropAtPosition(worldName, blockX, blockY, blockZ)

        if (cropAtPos != null) {
            // 点击的是已有作物
            handleCropInteraction(e, player, cropAtPos, itemInHand)
        } else if (isFarmland(block)) {
            // 点击的是耕地且无作物 -> 尝试种植
            handlePlanting(e, player, block, itemInHand)
        }
    }

    // ==================== 种植逻辑 ====================

    /**
     * 处理种植行为
     * 玩家手持种子右键耕地 -> 地块归属校验 -> 空位检测 -> 种植 -> 扣除种子
     */
    private fun handlePlanting(
        e: PlayerInteractEvent,
        player: Player,
        block: org.bukkit.block.Block,
        itemInHand: ItemStack
    ) {
        if (itemInHand.type == Material.AIR) return

        // 识别种子类型
        val seedItemId = itemInHand.type.name
        val cropTypeId = CropManager.getCropTypeIdBySeed(seedItemId) ?: return

        // 地块归属校验
        val plot = PlotManager.getPlotByOwner(player.uniqueId)
        if (plot == null || !PlotManager.isInPlot(
                block.world.name, block.x, block.z, plot
            )
        ) {
            player.sendLang("harvest-not-own-plot")
            e.isCancelled = true
            return
        }

        // 种植位置是耕地上方一格
        val plantLocation = block.location.clone().add(0.0, 1.0, 0.0)
        val plantX = plantLocation.blockX
        val plantY = plantLocation.blockY
        val plantZ = plantLocation.blockZ

        // 空位检测
        if (CropManager.getCropAtPosition(block.world.name, plantX, plantY, plantZ) != null) {
            player.sendLang("harvest-position-occupied")
            e.isCancelled = true
            return
        }

        // 调用 CropManager 种植
        val crop = CropManager.plantCrop(player, cropTypeId, plot.id, plantLocation)
        if (crop != null) {
            // 扣除种子
            if (itemInHand.amount > 1) {
                itemInHand.amount -= 1
            } else {
                player.inventory.setItemInMainHand(ItemStack(Material.AIR))
            }
            val def = CropManager.getCropDefinition(cropTypeId)
            player.sendLang("harvest-plant-success", def?.name ?: cropTypeId)
        }
        e.isCancelled = true
    }

    // ==================== 作物交互逻辑 ====================

    /**
     * 处理对已有作物的交互
     * - 手持骨粉 + 未成熟 -> 骨粉加速
     * - 空手/其他 + 成熟 -> 收割
     */
    private fun handleCropInteraction(
        e: PlayerInteractEvent,
        player: Player,
        crop: CropInstance,
        itemInHand: ItemStack
    ) {
        // 必须是自己地块上的作物才能收割/骨粉
        val plot = PlotManager.getPlotByOwner(player.uniqueId)
        if (plot == null || crop.plotId != plot.id) {
            // 不是自己的地块，不处理（可能是偷菜，由 steal_manager 处理）
            return
        }

        val isMature = CropManager.isMature(crop)

        if (itemInHand.type == Material.BONE_MEAL && !isMature) {
            handleBonemeal(e, player, crop, itemInHand)
        } else if (itemInHand.type == Material.BONE_MEAL && isMature) {
            player.sendLang("harvest-bonemeal-already-mature")
            e.isCancelled = true
        } else if (isMature) {
            handleHarvest(e, player, crop)
        } else {
            // 未成熟且非骨粉，不处理
            player.sendLang("harvest-crop-not-mature")
            e.isCancelled = true
        }
    }

    // ==================== 收割逻辑 ====================

    /**
     * 处理手动收割
     * 成熟度校验 -> 产出计算 -> 物品发放 -> 移除作物 -> 累加统计
     */
    private fun handleHarvest(
        e: PlayerInteractEvent,
        player: Player,
        crop: CropInstance
    ) {
        val cropDef = CropManager.getCropDefinition(crop.cropTypeId)
        if (cropDef == null) {
            warning("[Farm] 收割时找不到作物定义: ${crop.cropTypeId}")
            return
        }

        // 计算产出
        val amount = CropManager.calculateHarvestAmount(cropDef)
        val harvestMaterial = Material.matchMaterial(cropDef.harvestItemId)
        if (harvestMaterial == null) {
            warning("[Farm] 无法识别收获物品材质: ${cropDef.harvestItemId}")
            return
        }

        val harvestItem = ItemStack(harvestMaterial, amount)
        val harvestItems = listOf(harvestItem)

        // 发放物品到玩家背包
        val leftover = player.inventory.addItem(harvestItem)
        for (drop in leftover.values) {
            player.world.dropItemNaturally(player.location, drop)
        }

        // 移除作物
        CropManager.removeCrop(crop.id, CropRemoveReason.HARVESTED)

        // 触发事件
        CropHarvestedEvent(player, crop, harvestItems, false).call()

        // 累加统计
        PlayerDataManager.updateStatistic(player.uniqueId, StatisticType.TOTAL_HARVEST, amount.toLong())

        // 播放粒子
        val loc = player.world.getBlockAt(crop.x, crop.y, crop.z).location.add(0.5, 0.5, 0.5)
        player.world.spawnParticle(getHarvestParticle(), loc, getParticleCount(), 0.3, 0.3, 0.3, 0.0)

        player.sendLang("harvest-success", cropDef.name, amount)
        e.isCancelled = true
    }

    // ==================== 骨粉加速逻辑 ====================

    /**
     * 处理骨粉加速
     * 未成熟校验 -> 加速生长 -> 扣除骨粉 -> 播放粒子 -> 触发事件
     */
    private fun handleBonemeal(
        e: PlayerInteractEvent,
        player: Player,
        crop: CropInstance,
        itemInHand: ItemStack
    ) {
        val accelerationMs = getBonemealAccelerationMs()

        // 调用 CropManager 加速
        val success = CropManager.accelerateGrowth(crop.id, accelerationMs)
        if (!success) {
            warning("[Farm] 骨粉加速失败: cropId=${crop.id}")
            return
        }

        // 扣除骨粉
        if (itemInHand.amount > 1) {
            itemInHand.amount -= 1
        } else {
            player.inventory.setItemInMainHand(ItemStack(Material.AIR))
        }

        // 播放粒子
        val loc = player.world.getBlockAt(crop.x, crop.y, crop.z).location.add(0.5, 0.5, 0.5)
        player.world.spawnParticle(getBonemealParticle(), loc, getParticleCount(), 0.3, 0.3, 0.3, 0.0)

        // 计算新阶段并触发事件
        val updatedCrop = CropManager.getCropById(crop.id)
        val newStage = if (updatedCrop != null) CropManager.calculateGrowthStage(updatedCrop) else 0
        CropBonemeledEvent(player, crop, newStage).call()

        player.sendLang("harvest-bonemeal-success")
        e.isCancelled = true
    }

    // ==================== 自动收割逻辑 ====================

    /**
     * 监听玩家进入自己农场事件
     * 检查是否解锁自动收割 -> 遍历成熟作物 -> 产出存入仓库 -> 移除作物 -> 累加统计
     */
    @SubscribeEvent
    fun onPlayerEnterOwnFarm(e: PlayerEnterOwnFarmEvent) {
        val player = e.player
        val uuid = player.uniqueId
        val plotId = e.plotId

        // 检查是否解锁自动收割
        val level = FarmLevelManager.getPlayerLevel(uuid)
        if (!FarmLevelManager.isAutoHarvestUnlocked(level)) return

        // 遍历地块内所有作物
        val crops = CropManager.getCropsByPlot(plotId)
        var autoHarvestCount = 0

        for (crop in crops) {
            if (!CropManager.isMature(crop)) continue

            val cropDef = CropManager.getCropDefinition(crop.cropTypeId) ?: continue
            val amount = CropManager.calculateHarvestAmount(cropDef)

            // 产出存入农场仓库
            DatabaseManager.database.insertOrUpdateFarmStorage(uuid, cropDef.harvestItemId, amount)

            // 移除作物
            CropManager.removeCrop(crop.id, CropRemoveReason.AUTO_HARVESTED)

            // 触发事件（自动收割，player 传入但标记为自动）
            val harvestItem = ItemStack(
                Material.matchMaterial(cropDef.harvestItemId) ?: Material.WHEAT, amount
            )
            CropHarvestedEvent(player, crop, listOf(harvestItem), true).call()

            // 累加统计
            PlayerDataManager.updateStatistic(uuid, StatisticType.TOTAL_HARVEST, amount.toLong())
            autoHarvestCount++
        }

        if (autoHarvestCount > 0) {
            player.sendLang("harvest-auto-harvest-stored", autoHarvestCount)
        }
    }

    // ==================== 工具方法 ====================

    /** 判断方块是否为耕地 */
    private fun isFarmland(block: org.bukkit.block.Block): Boolean {
        return block.type == Material.FARMLAND
    }

    // ==================== 重载 ====================

    /** 重载配置 */
    fun reload() {
        config.reload()
    }
}
