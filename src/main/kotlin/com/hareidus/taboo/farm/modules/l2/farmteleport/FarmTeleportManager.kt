package com.hareidus.taboo.farm.modules.l2.farmteleport

import com.hareidus.taboo.farm.modules.l1.crop.CropManager
import com.hareidus.taboo.farm.modules.l1.plot.PlotManager
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerChangedWorldEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack
import taboolib.common.platform.event.SubscribeEvent
import taboolib.common.platform.function.info
import taboolib.common.platform.function.warning
import taboolib.module.configuration.Config
import taboolib.module.configuration.Configuration
import taboolib.platform.util.sendLang
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * 玩家当前所处农场状态
 */
data class FarmVisitState(
    val plotId: Long,
    val ownerUUID: UUID,
    val isOwnFarm: Boolean
)

/**
 * 农场传送管理器 (L2)
 *
 * 职责：
 * 1. 新玩家首次进入: 分配地块 → 生成地形 → 发放新手礼包 → 传送 → 欢迎消息
 * 2. 老玩家回到自己农场: 刷新作物生长 → 传送 → 发布 PlayerEnterOwnFarmEvent
 * 3. 传送到他人农场: 权限校验 → 刷新作物 → 传送 → 发布 PlayerEnterOtherFarmEvent
 * 4. 维护玩家当前所处农场状态
 * 5. 监听世界切换事件处理离开农场
 *
 * 依赖: PlotManager, CropManager, PlayerDataManager
 */
object FarmTeleportManager {

    @Config("modules/l2/farm_teleport.yml", autoReload = true)
    lateinit var config: Configuration
        private set

    /** 玩家当前农场状态追踪 */
    private val visitStates = ConcurrentHashMap<UUID, FarmVisitState>()

    // ==================== 配置读取 ====================

    private val teleportBlock: Material
        get() = Material.matchMaterial(config.getString("teleport-block", "END_PORTAL_FRAME")!!)
            ?: Material.END_PORTAL_FRAME

    private val permissionTeleport: String
        get() = config.getString("permission-teleport", "stealfarm.teleport")!!

    private val teleportDelay: Long
        get() = config.getLong("teleport-delay", 0)

    private val starterKit: Map<Material, Int>
        get() {
            val section = config.getConfigurationSection("starter-kit") ?: return emptyMap()
            return section.getKeys(false).mapNotNull { key ->
                val material = Material.matchMaterial(key) ?: return@mapNotNull null
                val amount = section.getInt(key, 0)
                if (amount > 0) material to amount else null
            }.toMap()
        }

    // ==================== 查询 API ====================

    /** 获取玩家当前农场访问状态 */
    fun getVisitState(uuid: UUID): FarmVisitState? {
        return visitStates[uuid]
    }

    /** 判断玩家是否在农场世界中 */
    fun isInFarmWorld(player: Player): Boolean {
        return player.world.name == PlotManager.worldName
    }

    /** 判断玩家是否在他人农场中 */
    fun isInOtherFarm(uuid: UUID): Boolean {
        val state = visitStates[uuid] ?: return false
        return !state.isOwnFarm
    }

    // ==================== 传送到自己农场 ====================

    /**
     * 传送玩家到自己的农场
     *
     * 新玩家: 分配地块 → 生成地形 → 发放新手礼包 → 传送 → 欢迎消息
     * 老玩家: 刷新作物生长 → 传送 → 发布事件
     */
    fun teleportToOwnFarm(player: Player) {
        val uuid = player.uniqueId
        var plot = PlotManager.getPlotByOwner(uuid)
        val isNewPlayer = plot == null

        if (isNewPlayer) {
            // 新玩家：分配地块
            plot = PlotManager.allocatePlot(uuid)
            info("[Farm] 新玩家 ${player.name} 分配地块 #${plot.id}")
        }

        plot!!

        // 刷新作物生长视觉
        CropManager.updateAllCropsInPlot(plot.id)

        // 计算传送目标
        val (cx, cy, cz) = PlotManager.getPlotCenter(plot)
        val world = Bukkit.getWorld(PlotManager.worldName)
        if (world == null) {
            warning("[Farm] 农场世界 '${PlotManager.worldName}' 不存在，无法传送")
            return
        }
        val targetLocation = Location(world, cx + 0.5, cy.toDouble(), cz + 0.5)

        // 执行传送
        if (teleportDelay > 0) {
            player.sendLang("farmteleport-teleporting")
            taboolib.common.platform.function.submit(delay = teleportDelay) {
                if (player.isOnline) {
                    doTeleportOwn(player, targetLocation, plot, isNewPlayer)
                }
            }
        } else {
            doTeleportOwn(player, targetLocation, plot, isNewPlayer)
        }
    }

    /**
     * 执行传送到自己农场的最终步骤
     */
    private fun doTeleportOwn(player: Player, location: Location, plot: com.hareidus.taboo.farm.foundation.model.Plot, isNewPlayer: Boolean) {
        player.teleport(location)

        // 更新状态追踪
        visitStates[player.uniqueId] = FarmVisitState(
            plotId = plot.id,
            ownerUUID = player.uniqueId,
            isOwnFarm = true
        )

        if (isNewPlayer) {
            // 发放新手礼包
            giveStarterKit(player)
            player.sendLang("farmteleport-starter-kit")

            // 发布新地块分配事件
            FarmPlotAssignedEvent(player, plot).call()

            // 欢迎消息（复合消息：音效 + 标题 + 文本）
            player.sendLang("farmteleport-welcome")
        } else {
            // 老玩家回到农场
            player.sendLang("farmteleport-return-own")

            // 发布进入自己农场事件
            PlayerEnterOwnFarmEvent(player, plot.id).call()
        }

        info("[Farm] 玩家 ${player.name} 已传送到自己的农场 #${plot.id}")
    }

    // ==================== 传送到他人农场 ====================

    /**
     * 传送玩家到他人农场
     *
     * 流程: 权限校验 → 目标地块校验 → 刷新作物 → 传送 → 发布事件
     *
     * @param player 访问者
     * @param targetUUID 目标农场主 UUID
     * @return 是否传送成功
     */
    fun teleportToOtherFarm(player: Player, targetUUID: UUID): Boolean {
        val uuid = player.uniqueId

        // 权限校验
        if (!player.hasPermission(permissionTeleport)) {
            player.sendLang("farmteleport-no-permission")
            return false
        }

        // 不能传送到自己的农场（应使用 teleportToOwnFarm）
        if (targetUUID == uuid) {
            teleportToOwnFarm(player)
            return true
        }

        // 目标地块校验
        val targetPlot = PlotManager.getPlotByOwner(targetUUID)
        if (targetPlot == null) {
            val targetName = Bukkit.getOfflinePlayer(targetUUID).name ?: targetUUID.toString()
            player.sendLang("farmteleport-target-no-plot", targetName)
            return false
        }

        // 刷新目标地块作物生长
        CropManager.updateAllCropsInPlot(targetPlot.id)

        // 计算传送目标
        val (cx, cy, cz) = PlotManager.getPlotCenter(targetPlot)
        val world = Bukkit.getWorld(PlotManager.worldName)
        if (world == null) {
            warning("[Farm] 农场世界 '${PlotManager.worldName}' 不存在，无法传送")
            return false
        }
        val targetLocation = Location(world, cx + 0.5, cy.toDouble(), cz + 0.5)

        // 执行传送
        if (teleportDelay > 0) {
            player.sendLang("farmteleport-teleporting")
            taboolib.common.platform.function.submit(delay = teleportDelay) {
                if (player.isOnline) {
                    doTeleportOther(player, targetLocation, targetPlot, targetUUID)
                }
            }
        } else {
            doTeleportOther(player, targetLocation, targetPlot, targetUUID)
        }
        return true
    }

    /**
     * 执行传送到他人农场的最终步骤
     */
    private fun doTeleportOther(
        player: Player,
        location: Location,
        targetPlot: com.hareidus.taboo.farm.foundation.model.Plot,
        ownerUUID: UUID
    ) {
        player.teleport(location)

        // 更新状态追踪
        visitStates[player.uniqueId] = FarmVisitState(
            plotId = targetPlot.id,
            ownerUUID = ownerUUID,
            isOwnFarm = false
        )

        val ownerName = Bukkit.getOfflinePlayer(ownerUUID).name ?: ownerUUID.toString()
        player.sendLang("farmteleport-enter-other", ownerName)

        // 发布进入他人农场事件
        PlayerEnterOtherFarmEvent(player, ownerUUID, targetPlot.id).call()

        info("[Farm] 玩家 ${player.name} 已传送到 $ownerName 的农场 #${targetPlot.id}")
    }

    // ==================== 新手礼包 ====================

    /** 发放新手种子礼包到玩家背包 */
    private fun giveStarterKit(player: Player) {
        for ((material, amount) in starterKit) {
            val item = ItemStack(material, amount)
            val leftover = player.inventory.addItem(item)
            if (leftover.isNotEmpty()) {
                // 背包满了，掉落到地上
                for (drop in leftover.values) {
                    player.world.dropItemNaturally(player.location, drop)
                }
            }
        }
    }

    // ==================== 状态清理 ====================

    /** 清除玩家的农场访问状态 */
    fun clearVisitState(uuid: UUID) {
        visitStates.remove(uuid)
    }

    // ==================== 事件监听 ====================

    /**
     * 监听玩家右键交互传送点方块
     * 在主世界中右键指定方块类型触发传送到自己农场
     */
    @SubscribeEvent
    fun onPlayerInteract(e: PlayerInteractEvent) {
        if (e.action != Action.RIGHT_CLICK_BLOCK) return
        val block = e.clickedBlock ?: return
        val player = e.player

        // 只在非农场世界中触发（主世界传送点）
        if (player.world.name == PlotManager.worldName) return

        // 检查方块类型是否为传送点方块
        if (block.type != teleportBlock) return

        e.isCancelled = true
        teleportToOwnFarm(player)
    }

    /**
     * 监听玩家切换世界，处理离开农场
     */
    @SubscribeEvent
    fun onWorldChange(e: PlayerChangedWorldEvent) {
        val player = e.player
        val uuid = player.uniqueId
        val oldWorldName = e.from.name

        // 从农场世界离开
        if (oldWorldName == PlotManager.worldName) {
            val state = visitStates[uuid]
            if (state != null) {
                PlayerLeaveFarmEvent(player, state.plotId).call()
                visitStates.remove(uuid)
                player.sendLang("farmteleport-leave-farm")
            }
        }
    }

    // ==================== 重载 ====================

    /** 重载配置 */
    fun reload() {
        config.reload()
    }
}
