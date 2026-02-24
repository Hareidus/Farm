package com.hareidus.taboo.farm.modules.l1.plot

import com.hareidus.taboo.farm.foundation.database.DatabaseManager
import com.hareidus.taboo.farm.foundation.model.Plot
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.WorldCreator
import org.bukkit.entity.Player
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.function.info
import taboolib.common.platform.function.warning
import taboolib.module.configuration.Config
import taboolib.module.configuration.Configuration
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * 地块管理器 (L1)
 *
 * 职责：
 * 1. 管理农场 void 世界的创建与加载
 * 2. 网格坐标计算与地块分配
 * 3. 地块地面和边界方块的生成与扩展
 * 4. 地块归属权查询（坐标→玩家、玩家→地块）
 * 5. 地块物理区域的重置
 *
 * 依赖: database_manager
 */
object PlotManager {

    @Config("modules/l1/plot.yml", autoReload = true)
    lateinit var config: Configuration
        private set

    /** ownerUUID -> Plot 缓存 */
    private val plotsByOwner = ConcurrentHashMap<UUID, Plot>()

    /** plotId -> Plot 缓存 */
    private val plotsById = ConcurrentHashMap<Long, Plot>()

    /** 已分配的网格坐标集合，用于快速判重 */
    private val occupiedGrids = ConcurrentHashMap.newKeySet<Pair<Int, Int>>()

    /** 下一个分配序号（顺序分配） */
    private var nextAllocationIndex = 0

    // ==================== 配置读取 ====================

    val worldName: String get() = config.getString("world-name", "farm_world")!!
    private val gridSpacing: Int get() = config.getInt("grid-spacing", 64)
    private val initialPlotSize: Int get() = config.getInt("initial-plot-size", 16)
    private val borderMaterial: Material
        get() = Material.matchMaterial(config.getString("border-material", "OAK_FENCE")!!) ?: Material.OAK_FENCE
    private val groundMaterial: Material
        get() = Material.matchMaterial(config.getString("ground-material", "FARMLAND")!!) ?: Material.FARMLAND
    private val pathMaterial: Material
        get() = Material.matchMaterial(config.getString("path-material", "GRASS_BLOCK")!!) ?: Material.GRASS_BLOCK
    private val baseY: Int get() = config.getInt("base-y", 64)

    // ==================== 初始化 ====================

    @Awake(LifeCycle.ENABLE)
    fun init() {
        ensureFarmWorld()
        loadAllPlots()
    }

    /** 确保农场世界已加载，若不存在则创建 */
    private fun ensureFarmWorld() {
        if (Bukkit.getWorld(worldName) == null) {
            val creator = WorldCreator(worldName)
                .generator(FarmWorldGenerator())
                .environment(World.Environment.NORMAL)
                .generateStructures(false)
            Bukkit.createWorld(creator)
            info("[Farm] 农场世界 '$worldName' 已创建")
        } else {
            info("[Farm] 农场世界 '$worldName' 已加载")
        }
    }

    /** 从数据库加载所有地块到内存缓存 */
    private fun loadAllPlots() {
        plotsByOwner.clear()
        plotsById.clear()
        occupiedGrids.clear()
        val plots = DatabaseManager.database.getAllPlots()
        for (plot in plots) {
            plotsByOwner[plot.ownerUUID] = plot
            plotsById[plot.id] = plot
            occupiedGrids.add(plot.gridX to plot.gridZ)
        }
        nextAllocationIndex = plots.size
        info("[Farm] 已加载 ${plots.size} 个农场地块")
    }

    // ==================== 查询 API ====================

    /** 根据玩家 UUID 获取其地块 */
    fun getPlotByOwner(uuid: UUID): Plot? {
        return plotsByOwner[uuid]
    }

    /** 根据地块 ID 获取地块 */
    fun getPlotById(plotId: Long): Plot? {
        return plotsById[plotId]
    }

    /** 根据世界坐标查找所在地块 */
    fun getPlotByPosition(worldName: String, x: Int, z: Int): Plot? {
        if (worldName != this.worldName) return null
        return plotsByOwner.values.firstOrNull { plot ->
            x in plot.minX..plot.maxX && z in plot.minZ..plot.maxZ
        }
    }

    /** 判断指定坐标是否在某个地块内 */
    fun isInPlot(worldName: String, x: Int, z: Int, plot: Plot): Boolean {
        return worldName == plot.worldName
                && x in plot.minX..plot.maxX
                && z in plot.minZ..plot.maxZ
    }

    /** 判断玩家是否在自己的地块内 */
    fun isInOwnPlot(player: Player): Boolean {
        val loc = player.location
        val plot = getPlotByOwner(player.uniqueId) ?: return false
        return loc.world?.name == plot.worldName
                && loc.blockX in plot.minX..plot.maxX
                && loc.blockZ in plot.minZ..plot.maxZ
    }

    /** 获取指定坐标所在地块的主人 UUID */
    fun getPlotOwnerAt(worldName: String, x: Int, z: Int): UUID? {
        return getPlotByPosition(worldName, x, z)?.ownerUUID
    }

    /** 获取所有已分配地块 */
    fun getAllPlots(): List<Plot> {
        return plotsByOwner.values.toList()
    }

    // ==================== 分配 ====================

    /**
     * 为玩家分配新地块
     * 使用顺序网格算法：按螺旋序列计算下一个可用网格坐标
     */
    fun allocatePlot(uuid: UUID): Plot {
        check(getPlotByOwner(uuid) == null) { "玩家已拥有地块" }

        val (gridX, gridZ) = calculateNextGridPosition()
        val size = initialPlotSize
        val centerX = gridX * gridSpacing
        val centerZ = gridZ * gridSpacing
        val minX = centerX - size
        val minZ = centerZ - size
        val maxX = centerX + size
        val maxZ = centerZ + size

        val plot = Plot(
            id = 0,
            ownerUUID = uuid,
            gridX = gridX,
            gridZ = gridZ,
            worldName = worldName,
            minX = minX,
            minZ = minZ,
            maxX = maxX,
            maxZ = maxZ,
            size = size
        )

        val insertedId = DatabaseManager.database.insertPlot(plot)
        val savedPlot = plot.copy(id = insertedId)

        // 更新缓存
        plotsByOwner[uuid] = savedPlot
        plotsById[insertedId] = savedPlot
        occupiedGrids.add(gridX to gridZ)
        nextAllocationIndex++

        // 生成地形
        generatePlotTerrain(savedPlot)

        // 触发事件
        val player = Bukkit.getPlayer(uuid)
        if (player != null) {
            PlotAllocatedEvent(player, savedPlot).call()
        }

        info("[Farm] 为玩家 $uuid 分配地块 #$insertedId (grid=$gridX,$gridZ)")
        return savedPlot
    }

    /**
     * 螺旋序列网格分配算法
     * 按 (0,0) -> (1,0) -> (1,1) -> (0,1) -> (-1,1) -> (-1,0) -> (-1,-1) -> (0,-1) -> (1,-1) -> (2,-1) ...
     */
    private fun calculateNextGridPosition(): Pair<Int, Int> {
        var x = 0
        var z = 0
        var dx = 1
        var dz = 0
        var segmentLength = 1
        var segmentPassed = 0
        var turnsCount = 0

        repeat(nextAllocationIndex) {
            x += dx
            z += dz
            segmentPassed++
            if (segmentPassed == segmentLength) {
                segmentPassed = 0
                // 顺时针旋转: (1,0)->(0,1)->(-1,0)->(0,-1)
                val temp = dx
                dx = -dz
                dz = temp
                turnsCount++
                if (turnsCount % 2 == 0) {
                    segmentLength++
                }
            }
        }

        // 如果该位置已被占用（异常情况），继续寻找
        while (occupiedGrids.contains(x to z)) {
            x += dx
            z += dz
            segmentPassed++
            if (segmentPassed == segmentLength) {
                segmentPassed = 0
                val temp = dx
                dx = -dz
                dz = temp
                turnsCount++
                if (turnsCount % 2 == 0) {
                    segmentLength++
                }
            }
        }

        return x to z
    }

    // ==================== 扩展 ====================

    /** 扩展地块边界 */
    fun expandPlot(plotId: Long, sizeIncrease: Int) {
        val plot = plotsById[plotId] ?: run {
            warning("[Farm] 扩展地块失败: 地块 #$plotId 不存在")
            return
        }
        val oldSize = plot.size
        val newSize = oldSize + sizeIncrease
        val centerX = plot.gridX * gridSpacing
        val centerZ = plot.gridZ * gridSpacing
        val newMinX = centerX - newSize
        val newMinZ = centerZ - newSize
        val newMaxX = centerX + newSize
        val newMaxZ = centerZ + newSize

        DatabaseManager.database.updatePlotSize(plotId, newMinX, newMinZ, newMaxX, newMaxZ, newSize)

        // 更新内存
        plot.minX = newMinX
        plot.minZ = newMinZ
        plot.maxX = newMaxX
        plot.maxZ = newMaxZ
        plot.size = newSize

        // 生成扩展区域地形
        generatePlotTerrain(plot)

        // 触发事件
        val player = Bukkit.getPlayer(plot.ownerUUID)
        if (player != null) {
            PlotExpandedEvent(player, plot, oldSize, newSize).call()
        }

        info("[Farm] 地块 #$plotId 已扩展: $oldSize -> $newSize")
    }

    // ==================== 重置 ====================

    /** 重置地块到初始状态（清除方块、恢复初始尺寸） */
    fun resetPlot(plotId: Long) {
        val plot = plotsById[plotId] ?: run {
            warning("[Farm] 重置地块失败: 地块 #$plotId 不存在")
            return
        }
        val world = Bukkit.getWorld(plot.worldName) ?: return

        // 清除当前区域所有方块
        clearPlotBlocks(world, plot)

        // 恢复初始尺寸
        val centerX = plot.gridX * gridSpacing
        val centerZ = plot.gridZ * gridSpacing
        val size = initialPlotSize
        val newMinX = centerX - size
        val newMinZ = centerZ - size
        val newMaxX = centerX + size
        val newMaxZ = centerZ + size

        DatabaseManager.database.updatePlotSize(plotId, newMinX, newMinZ, newMaxX, newMaxZ, size)

        plot.minX = newMinX
        plot.minZ = newMinZ
        plot.maxX = newMaxX
        plot.maxZ = newMaxZ
        plot.size = size

        // 重新生成初始地形
        generatePlotTerrain(plot)

        info("[Farm] 地块 #$plotId 已重置为初始状态")
    }

    /** 清除地块区域内所有方块 */
    private fun clearPlotBlocks(world: World, plot: Plot) {
        val y = baseY
        for (x in plot.minX..plot.maxX) {
            for (z in plot.minZ..plot.maxZ) {
                world.getBlockAt(x, y, z).type = Material.AIR
                world.getBlockAt(x, y + 1, z).type = Material.AIR
                world.getBlockAt(x, y + 2, z).type = Material.AIR
            }
        }
    }

    // ==================== 地形生成 ====================

    /** 生成地块地形：两层地基 + 耕地地面 + 围栏边界 */
    fun generatePlotTerrain(plot: Plot) {
        val world = Bukkit.getWorld(plot.worldName) ?: run {
            warning("[Farm] 生成地形失败: 世界 '${plot.worldName}' 不存在")
            return
        }
        val y = baseY

        for (x in plot.minX..plot.maxX) {
            for (z in plot.minZ..plot.maxZ) {
                val isBorder = x == plot.minX || x == plot.maxX || z == plot.minZ || z == plot.maxZ
                // 底层地基（y-1）：泥土，防止挖穿
                world.getBlockAt(x, y - 1, z).type = Material.DIRT
                if (isBorder) {
                    // 边界：路径方块 + 围栏
                    world.getBlockAt(x, y, z).type = pathMaterial
                    world.getBlockAt(x, y + 1, z).type = borderMaterial
                } else {
                    // 内部：耕地（玩家可挖开一格放水）
                    world.getBlockAt(x, y, z).type = groundMaterial
                }
            }
        }
    }

    // ==================== 删除 ====================

    /** 删除地块（从数据库和缓存中移除） */
    fun deletePlot(plotId: Long): Boolean {
        val plot = plotsById[plotId] ?: return false
        val world = Bukkit.getWorld(plot.worldName)
        if (world != null) {
            clearPlotBlocks(world, plot)
        }
        DatabaseManager.database.deletePlot(plotId)
        plotsByOwner.remove(plot.ownerUUID)
        plotsById.remove(plotId)
        occupiedGrids.remove(plot.gridX to plot.gridZ)
        info("[Farm] 地块 #$plotId 已删除")
        return true
    }

    // ==================== 工具方法 ====================

    /** 获取地块中心坐标 */
    fun getPlotCenter(plot: Plot): Triple<Int, Int, Int> {
        val centerX = plot.gridX * gridSpacing
        val centerZ = plot.gridZ * gridSpacing
        return Triple(centerX, baseY + 1, centerZ)
    }

    /** 重载配置 */
    fun reload() {
        config.reload()
        loadAllPlots()
    }
}
