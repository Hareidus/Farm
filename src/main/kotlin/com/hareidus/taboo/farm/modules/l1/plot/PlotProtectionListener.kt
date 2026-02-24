package com.hareidus.taboo.farm.modules.l1.plot

import com.hareidus.taboo.farm.modules.l2.farmteleport.FarmTeleportManager
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.entity.EntityInteractEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerMoveEvent
import taboolib.common.platform.event.SubscribeEvent
import taboolib.platform.util.sendLang

/**
 * 农场世界地块保护监听器 (L1)
 *
 * 规则：
 * - 农场世界内，主人可以自由操作自己的地块
 * - 访客不能破坏/放置方块
 * - 地块外区域全部禁止（除非有 admin 权限）
 * - 防止踩踏耕地（跳跃将 FARMLAND 变 DIRT）
 * - 玩家不能走出当前所在地块边界
 */
object PlotProtectionListener {

    @SubscribeEvent
    fun onBlockBreak(e: BlockBreakEvent) {
        val player = e.player
        val block = e.block
        if (block.world.name != PlotManager.worldName) return
        if (player.hasPermission("stealfarm.admin")) return

        val plot = PlotManager.getPlotByPosition(block.world.name, block.x, block.z)
        if (plot == null) {
            e.isCancelled = true
            return
        }
        if (plot.ownerUUID != player.uniqueId) {
            e.isCancelled = true
            player.sendLang("plot-protection-no-break")
        }
    }

    @SubscribeEvent
    fun onBlockPlace(e: BlockPlaceEvent) {
        val player = e.player
        val block = e.block
        if (block.world.name != PlotManager.worldName) return
        if (player.hasPermission("stealfarm.admin")) return

        val plot = PlotManager.getPlotByPosition(block.world.name, block.x, block.z)
        if (plot == null) {
            e.isCancelled = true
            return
        }
        if (plot.ownerUUID != player.uniqueId) {
            e.isCancelled = true
            player.sendLang("plot-protection-no-place")
        }
    }

    /**
     * 防止踩踏耕地（玩家跳跃踩踏 FARMLAND → DIRT）
     * 取消 PHYSICAL 交互即可阻止
     */
    @SubscribeEvent
    fun onFarmlandTrample(e: PlayerInteractEvent) {
        if (e.action != Action.PHYSICAL) return
        val block = e.clickedBlock ?: return
        if (block.world.name != PlotManager.worldName) return
        if (block.type != Material.FARMLAND) return
        // 农场世界内一律禁止踩踏耕地
        e.isCancelled = true
    }

    /**
     * 防止实体（如生物）踩踏耕地
     */
    @SubscribeEvent
    fun onEntityTrample(e: EntityInteractEvent) {
        val block = e.block
        if (block.world.name != PlotManager.worldName) return
        if (block.type != Material.FARMLAND) return
        e.isCancelled = true
    }

    /**
     * 玩家移动边界约束
     *
     * 规则：
     * - 玩家在农场世界中必须待在某个地块范围内
     * - 走出边界时拉回到上一个合法位置
     */
    @SubscribeEvent
    fun onPlayerMove(e: PlayerMoveEvent) {
        val to = e.to ?: return
        val from = e.from
        if (to.world?.name != PlotManager.worldName) return
        if (e.player.hasPermission("stealfarm.admin")) return

        // 只检查水平移动（优化性能）
        if (from.blockX == to.blockX && from.blockZ == to.blockZ) return

        // 获取玩家当前应在的地块
        val state = FarmTeleportManager.getVisitState(e.player.uniqueId)
        if (state == null) return // 没有访问状态，不限制（可能刚进世界）

        val plot = PlotManager.getPlotById(state.plotId) ?: return

        // 检查目标位置是否在地块范围内（边界围栏上也算合法）
        val margin = 1 // 允许站在围栏上
        if (to.blockX < plot.minX - margin || to.blockX > plot.maxX + margin
            || to.blockZ < plot.minZ - margin || to.blockZ > plot.maxZ + margin) {
            // 拉回到 from 位置
            e.setTo(from)
        }
    }
}
