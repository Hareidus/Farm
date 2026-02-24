package com.hareidus.taboo.farm.modules.l1.crop

import com.hareidus.taboo.farm.foundation.database.DatabaseManager
import com.hareidus.taboo.farm.foundation.model.*
import com.hareidus.taboo.farm.modules.l1.plot.PlotManager
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.Skull
import org.bukkit.block.data.Ageable
import org.bukkit.entity.Player
import org.bukkit.profile.PlayerProfile
import java.net.URL
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.function.info
import taboolib.common.platform.function.warning
import taboolib.module.configuration.Config
import taboolib.module.configuration.Configuration
import taboolib.platform.util.sendLang
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ThreadLocalRandom

/**
 * 作物管理器 (L1)
 *
 * 职责：
 * 1. 从配置加载所有作物类型定义（原版与自定义）
 * 2. 管理地块内每株作物的数据（种类、位置、种植时间戳）
 * 3. 基于真实时间戳的生长阶段计算
 * 4. 作物方块/头颅的放置与阶段更新渲染
 * 5. 作物的种植写入与收割移除
 * 6. 时间戳偏移实现加速生长
 *
 * 依赖: database_manager, plot_manager
 */
object CropManager {

    @Config("modules/l1/crop.yml", autoReload = true)
    lateinit var config: Configuration
        private set

    /** 作物定义缓存: id -> CropDefinition */
    private var cropDefinitions: Map<String, CropDefinition> = emptyMap()

    /** seedItemId -> cropTypeId 反查表 */
    private val seedToCropMap = ConcurrentHashMap<String, String>()

    // ==================== 初始化 ====================

    @Awake(LifeCycle.ENABLE)
    fun init() {
        loadCropDefinitions()
    }

    /** 从 crop.yml 加载所有作物类型定义 */
    private fun loadCropDefinitions() {
        val section = config.getConfigurationSection("crops")
        if (section == null) {
            warning("[Farm] crop.yml 中未找到 crops 配置节")
            return
        }
        seedToCropMap.clear()
        cropDefinitions = section.getKeys(false).mapNotNull { id ->
            val sub = section.getConfigurationSection(id) ?: return@mapNotNull null
            val stagesList = sub.getMapList("stages")
            if (stagesList.isEmpty()) {
                warning("[Farm] 作物 $id 未配置 stages")
                return@mapNotNull null
            }
            val stages = stagesList.mapIndexed { index, map ->
                CropStage(
                    stageIndex = index,
                    duration = (map["duration"] as Number).toLong(),
                    material = map["material"] as String
                )
            }
            val totalGrowthTime = stages.sumOf { it.duration }
            val seedItemId = sub.getString("seed-item") ?: return@mapNotNull null
            val def = CropDefinition(
                id = id,
                name = sub.getString("name") ?: id,
                isCustom = sub.getBoolean("custom", false),
                stages = stages,
                harvestMinAmount = sub.getInt("harvest-min", 1),
                harvestMaxAmount = sub.getInt("harvest-max", 1),
                seedItemId = seedItemId,
                harvestItemId = sub.getString("harvest-item") ?: return@mapNotNull null,
                totalGrowthTime = totalGrowthTime
            )
            seedToCropMap[seedItemId] = id
            id to def
        }.toMap()
        info("[Farm] 已加载 ${cropDefinitions.size} 种作物定义")
    }

    // ==================== 定义查询 ====================

    /** 获取指定作物定义 */
    fun getCropDefinition(id: String): CropDefinition? {
        return cropDefinitions[id]
    }

    /** 获取所有作物定义 */
    fun getAllCropDefinitions(): List<CropDefinition> {
        return cropDefinitions.values.toList()
    }

    /** 根据种子物品 ID 查找对应的作物类型 ID */
    fun getCropTypeIdBySeed(seedItemId: String): String? {
        return seedToCropMap[seedItemId]
    }

    // ==================== 数据查询 ====================

    /** 获取指定地块的所有作物 */
    fun getCropsByPlot(plotId: Long): List<CropInstance> {
        return DatabaseManager.database.getCropsByPlot(plotId)
    }

    /** 根据世界坐标获取作物 */
    fun getCropAtPosition(worldName: String, x: Int, y: Int, z: Int): CropInstance? {
        return DatabaseManager.database.getCropByPosition(worldName, x, y, z)
    }

    /** 根据 ID 获取作物 */
    fun getCropById(cropId: Long): CropInstance? {
        return DatabaseManager.database.getCropById(cropId)
    }

    // ==================== 生长计算 ====================

    /**
     * 计算作物当前生长阶段索引
     * 基于 (当前时间 - 种植时间) 与各阶段 duration 的累加比较
     * @return 阶段索引 (0-based)，最大为 stages.size - 1
     */
    fun calculateGrowthStage(crop: CropInstance): Int {
        val def = getCropDefinition(crop.cropTypeId) ?: return 0
        val elapsed = System.currentTimeMillis() - crop.plantedAt
        if (elapsed <= 0) return 0
        var accumulated = 0L
        for (stage in def.stages) {
            accumulated += stage.duration
            if (elapsed < accumulated) {
                return stage.stageIndex
            }
        }
        return def.stages.size - 1
    }

    /** 判断作物是否已成熟（处于最后一个阶段） */
    fun isMature(crop: CropInstance): Boolean {
        val def = getCropDefinition(crop.cropTypeId) ?: return false
        return calculateGrowthStage(crop) >= def.stages.size - 1
    }

    /** 计算收获数量（在 min 和 max 之间随机） */
    fun calculateHarvestAmount(cropDef: CropDefinition): Int {
        return if (cropDef.harvestMinAmount >= cropDef.harvestMaxAmount) {
            cropDef.harvestMinAmount
        } else {
            ThreadLocalRandom.current().nextInt(cropDef.harvestMinAmount, cropDef.harvestMaxAmount + 1)
        }
    }

    // ==================== 种植 ====================

    /**
     * 种植作物
     * 校验 → 放置方块 → 写入数据库 → 触发事件
     * @return 种植成功返回 CropInstance，失败返回 null
     */
    fun plantCrop(player: Player, cropTypeId: String, plotId: Long, location: Location): CropInstance? {
        val def = getCropDefinition(cropTypeId)
        if (def == null) {
            player.sendLang("crop-type-not-found", cropTypeId)
            return null
        }
        val plot = PlotManager.getPlotById(plotId)
        if (plot == null) {
            player.sendLang("plot-not-found")
            return null
        }
        val worldName = location.world?.name ?: return null
        val x = location.blockX
        val y = location.blockY
        val z = location.blockZ

        // 校验位置是否在地块内
        if (!PlotManager.isInPlot(worldName, x, z, plot)) {
            player.sendLang("crop-not-in-plot")
            return null
        }
        // 校验位置是否已有作物
        if (getCropAtPosition(worldName, x, y, z) != null) {
            player.sendLang("crop-position-occupied")
            return null
        }

        val plantedAt = System.currentTimeMillis()

        // 放置初始阶段方块
        val firstStage = def.stages.firstOrNull() ?: return null
        placeBlockForStage(location, def, firstStage)

        // 写入数据库
        val cropId = DatabaseManager.database.insertCrop(
            cropTypeId, plotId, player.uniqueId, worldName, x, y, z, plantedAt
        )
        if (cropId < 0) {
            warning("[Farm] 作物写入数据库失败: $cropTypeId at ($x,$y,$z)")
            return null
        }

        val crop = CropInstance(
            id = cropId,
            cropTypeId = cropTypeId,
            plotId = plotId,
            ownerUUID = player.uniqueId,
            worldName = worldName,
            x = x, y = y, z = z,
            plantedAt = plantedAt
        )

        // 触发事件
        CropPlantedEvent(player, crop, plot).call()
        return crop
    }

    // ==================== 移除 ====================

    /**
     * 移除作物
     * 移除方块 → 删除数据库记录 → 触发事件
     */
    fun removeCrop(cropId: Long, reason: CropRemoveReason): Boolean {
        val crop = DatabaseManager.database.getCropById(cropId) ?: return false
        // 移除作物方块（恢复为空气，下方耕地不受影响）
        val world = Bukkit.getWorld(crop.worldName)
        if (world != null) {
            val block = world.getBlockAt(crop.x, crop.y, crop.z)
            block.type = Material.AIR
        }
        val removed = DatabaseManager.database.removeCrop(cropId)
        if (removed) {
            CropRemovedEvent(crop, reason).call()
        }
        return removed
    }

    /** 移除指定地块的所有作物（含方块清除） */
    fun removeAllCropsByPlot(plotId: Long): Boolean {
        val crops = getCropsByPlot(plotId)
        for (crop in crops) {
            val world = Bukkit.getWorld(crop.worldName) ?: continue
            world.getBlockAt(crop.x, crop.y, crop.z).type = Material.AIR
            CropRemovedEvent(crop, CropRemoveReason.ADMIN_RESET).call()
        }
        return DatabaseManager.database.removeAllCropsByPlot(plotId)
    }

    // ==================== 加速生长 ====================

    /**
     * 加速作物生长
     * 将 plantedAt 向前偏移指定毫秒数，然后更新视觉
     */
    fun accelerateGrowth(cropId: Long, milliseconds: Long): Boolean {
        val crop = DatabaseManager.database.getCropById(cropId) ?: return false
        val oldStage = calculateGrowthStage(crop)
        val newPlantedAt = crop.plantedAt - milliseconds
        val updated = DatabaseManager.database.updateCropPlantedAt(cropId, newPlantedAt)
        if (!updated) return false
        crop.plantedAt = newPlantedAt
        val newStage = calculateGrowthStage(crop)
        if (oldStage != newStage) {
            updateCropVisuals(crop)
            CropGrowthUpdatedEvent(crop, oldStage, newStage, GrowthAccelerateReason.NATURAL).call()
        }
        return true
    }

    // ==================== 视觉更新 ====================

    /** 更新单株作物的方块/头颅到当前生长阶段 */
    fun updateCropVisuals(crop: CropInstance) {
        val def = getCropDefinition(crop.cropTypeId) ?: return
        val stageIndex = calculateGrowthStage(crop)
        val stage = def.stages.getOrNull(stageIndex) ?: return
        val world = Bukkit.getWorld(crop.worldName) ?: return
        val location = Location(world, crop.x.toDouble(), crop.y.toDouble(), crop.z.toDouble())
        placeBlockForStage(location, def, stage)
    }

    /** 重新计算并更新地块内所有作物的视觉 */
    fun updateAllCropsInPlot(plotId: Long) {
        val crops = getCropsByPlot(plotId)
        for (crop in crops) {
            updateCropVisuals(crop)
        }
    }

    // ==================== 方块放置 ====================

    /**
     * 根据作物定义和阶段放置对应方块
     * - 原版作物: 解析 "MATERIAL[age=N]" 格式设置方块
     * - 自定义作物: 放置玩家头颅并设置 base64 材质
     */
    private fun placeBlockForStage(location: Location, def: CropDefinition, stage: CropStage) {
        val block = location.block
        if (def.isCustom) {
            placeCustomHead(block, stage.material)
        } else {
            placeVanillaCrop(block, stage.material)
        }
    }

    /**
     * 放置原版作物方块
     * 解析格式: "MATERIAL[age=N]"
     */
    private fun placeVanillaCrop(block: org.bukkit.block.Block, materialStr: String) {
        val regex = Regex("""(\w+)\[age=(\d+)]""")
        val match = regex.matchEntire(materialStr)
        if (match != null) {
            val matName = match.groupValues[1]
            val age = match.groupValues[2].toIntOrNull() ?: 0
            val material = Material.matchMaterial(matName)
            if (material != null) {
                block.type = material
                val blockData = block.blockData
                if (blockData is Ageable) {
                    blockData.age = age.coerceAtMost(blockData.maximumAge)
                    block.blockData = blockData
                }
            } else {
                warning("[Farm] 无法识别原版作物材质: $matName")
            }
        } else {
            // 无 age 参数，直接设置材质
            val material = Material.matchMaterial(materialStr)
            if (material != null) {
                block.type = material
            } else {
                warning("[Farm] 无法识别作物材质: $materialStr")
            }
        }
    }

    /**
     * 放置自定义头颅方块
     * 使用 base64 材质值设置玩家头颅
     */
    private fun placeCustomHead(block: org.bukkit.block.Block, base64Texture: String) {
        block.type = Material.PLAYER_HEAD
        val skull = block.state as? Skull ?: return
        try {
            val decoded = String(java.util.Base64.getDecoder().decode(base64Texture))
            // 从 base64 JSON 中提取 URL
            val urlRegex = Regex(""""url"\s*:\s*"(https?://[^"]+)"""")
            val urlMatch = urlRegex.find(decoded)
            if (urlMatch != null) {
                val textureUrl = urlMatch.groupValues[1]
                val profile: PlayerProfile = Bukkit.createPlayerProfile(UUID.randomUUID(), "crop_head")
                val textures = profile.textures
                textures.skin = URL(textureUrl)
                profile.setTextures(textures)
                skull.ownerProfile = profile
                skull.update(true, false)
            } else {
                warning("[Farm] 无法从 base64 中解析材质 URL")
            }
        } catch (e: Exception) {
            warning("[Farm] 无法设置头颅材质: ${e.message}")
        }
    }

    // ==================== 重载 ====================

    /** 重载配置 */
    fun reload() {
        config.reload()
        loadCropDefinitions()
    }
}
