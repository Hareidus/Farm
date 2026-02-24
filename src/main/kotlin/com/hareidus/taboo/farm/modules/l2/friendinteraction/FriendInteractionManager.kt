package com.hareidus.taboo.farm.modules.l2.friendinteraction

import com.hareidus.taboo.farm.foundation.database.DatabaseManager
import com.hareidus.taboo.farm.foundation.model.NotificationType
import com.hareidus.taboo.farm.modules.l1.crop.CropManager
import com.hareidus.taboo.farm.modules.l1.playerdata.PlayerDataManager
import com.hareidus.taboo.farm.modules.l1.plot.PlotManager
import com.hareidus.taboo.farm.modules.l1.social.SocialManager
import org.bukkit.Bukkit
import org.bukkit.Particle
import org.bukkit.entity.Player
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
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
 * 好友交互管理器 (L2)
 *
 * 职责：
 * 1. 好友浇水加速：玩家在好友农场右键未成熟作物 → 校验好友关系 → 检查冷却 → 加速生长 → 记录冷却 → 通知双方
 * 2. 提供 waterCrop(player, cropId) API
 * 3. 提供浇水冷却查询 API
 *
 * 依赖: SocialManager, CropManager, PlotManager, DatabaseManager(仅 waterCooldown CRUD)
 */
object FriendInteractionManager {

    @Config("modules/l2/friend_interaction.yml", autoReload = true)
    lateinit var config: Configuration
        private set

    // ==================== 配置读取 ====================

    /** 浇水加速时长（毫秒） */
    private val waterAccelerationMs: Long
        get() = config.getLong("water-acceleration-ms", 120000)

    /** 浇水冷却时长（毫秒） */
    private val waterCooldownMs: Long
        get() = config.getLong("water-cooldown-ms", 3600000)

    /** 浇水粒子效果 */
    private val waterParticle: Particle
        get() {
            val name = config.getString("water-particle", "SPLASH") ?: "SPLASH"
            return try {
                Particle.valueOf(name)
            } catch (_: Exception) {
                Particle.SPLASH
            }
        }

    // ==================== 初始化 ====================

    @Awake(LifeCycle.ENABLE)
    fun init() {
        info("[Farm] 好友交互管理器已加载 (加速: ${waterAccelerationMs}ms, 冷却: ${waterCooldownMs}ms)")
    }

    // ==================== 事件监听 ====================

    /**
     * 监听玩家右键交互事件
     * 判断条件: 在他人农场 + 右键方块 + 该位置有未成熟作物 + 是好友关系 + 冷却已过
     */
    @SubscribeEvent
    fun onPlayerInteract(e: PlayerInteractEvent) {
        if (e.action != Action.RIGHT_CLICK_BLOCK) return
        val block = e.clickedBlock ?: return
        val player = e.player
        val worldName = block.world.name
        val x = block.x
        val y = block.y
        val z = block.z

        // 检查是否在农场世界
        if (worldName != PlotManager.worldName) return

        // 查找该位置的地块主人
        val ownerUUID = PlotManager.getPlotOwnerAt(worldName, x, z)
        if (ownerUUID == null) return

        // 不能在自己的农场浇水
        if (ownerUUID == player.uniqueId) return

        // 检查该位置是否有作物
        val crop = CropManager.getCropAtPosition(worldName, x, y, z) ?: return

        // 检查作物是否已成熟
        if (CropManager.isMature(crop)) return

        // 校验好友关系
        if (!SocialManager.isFriend(player.uniqueId, ownerUUID)) {
            player.sendLang("friendinteraction-water-not-friend")
            return
        }

        // 检查浇水冷却
        val cooldown = DatabaseManager.database.getWaterCooldown(player.uniqueId, ownerUUID)
        if (cooldown != null && cooldown.cooldownEndTime > System.currentTimeMillis()) {
            val remaining = formatCooldownTime(cooldown.cooldownEndTime - System.currentTimeMillis())
            val ownerName = Bukkit.getOfflinePlayer(ownerUUID).name ?: ownerUUID.toString()
            player.sendLang("friendinteraction-water-cooldown", ownerName, remaining)
            e.isCancelled = true
            return
        }

        // 执行浇水
        e.isCancelled = true
        waterCrop(player, crop.id)
    }

    // ==================== 公开 API ====================

    /**
     * 为指定作物浇水
     *
     * 流程: 校验作物存在 → 校验未成熟 → 校验好友关系 → 校验冷却 → 加速生长 → 记录冷却 → 播放粒子 → 通知双方 → 发布事件
     *
     * @param player 浇水者
     * @param cropId 作物 ID
     * @return 是否浇水成功
     */
    fun waterCrop(player: Player, cropId: Long): Boolean {
        val crop = CropManager.getCropById(cropId)
        if (crop == null) {
            player.sendLang("friendinteraction-water-no-crop")
            return false
        }

        val ownerUUID = crop.ownerUUID

        // 不能给自己的作物浇水
        if (ownerUUID == player.uniqueId) {
            player.sendLang("friendinteraction-water-own-farm")
            return false
        }

        // 校验作物未成熟
        if (CropManager.isMature(crop)) {
            player.sendLang("friendinteraction-water-already-mature")
            return false
        }

        // 校验好友关系
        if (!SocialManager.isFriend(player.uniqueId, ownerUUID)) {
            player.sendLang("friendinteraction-water-not-friend")
            return false
        }

        // 校验浇水冷却
        val cooldown = DatabaseManager.database.getWaterCooldown(player.uniqueId, ownerUUID)
        if (cooldown != null && cooldown.cooldownEndTime > System.currentTimeMillis()) {
            val remaining = formatCooldownTime(cooldown.cooldownEndTime - System.currentTimeMillis())
            val ownerName = Bukkit.getOfflinePlayer(ownerUUID).name ?: ownerUUID.toString()
            player.sendLang("friendinteraction-water-cooldown", ownerName, remaining)
            return false
        }

        // 执行加速生长
        val accelerated = CropManager.accelerateGrowth(cropId, waterAccelerationMs)
        if (!accelerated) {
            player.sendLang("friendinteraction-water-failed")
            return false
        }

        // 记录浇水冷却
        val cooldownEndTime = System.currentTimeMillis() + waterCooldownMs
        DatabaseManager.database.setWaterCooldown(player.uniqueId, ownerUUID, cooldownEndTime)

        // 播放浇水粒子效果
        playWaterParticle(player, crop)

        // 通知浇水者
        val ownerName = Bukkit.getOfflinePlayer(ownerUUID).name ?: ownerUUID.toString()
        val accelerationSeconds = waterAccelerationMs / 1000
        player.sendLang("friendinteraction-water-success", ownerName, accelerationSeconds)

        // 通知农场主
        notifyOwner(player, ownerUUID)

        // 计算新生长阶段并发布事件
        val updatedCrop = CropManager.getCropById(cropId)
        val newStage = if (updatedCrop != null) CropManager.calculateGrowthStage(updatedCrop) else 0
        CropWateredEvent(
            watererUUID = player.uniqueId,
            ownerUUID = ownerUUID,
            cropTypeId = crop.cropTypeId,
            cropX = crop.x,
            cropY = crop.y,
            cropZ = crop.z,
            newGrowthStage = newStage
        ).call()

        return true
    }

    /**
     * 查询浇水冷却剩余时间
     *
     * @param watererUUID 浇水者 UUID
     * @param targetUUID 农场主 UUID
     * @return 剩余冷却毫秒数，无冷却或已过期返回 0
     */
    fun getWaterCooldownRemaining(watererUUID: UUID, targetUUID: UUID): Long {
        val cooldown = DatabaseManager.database.getWaterCooldown(watererUUID, targetUUID) ?: return 0
        val remaining = cooldown.cooldownEndTime - System.currentTimeMillis()
        return if (remaining > 0) remaining else 0
    }

    /**
     * 检查浇水冷却是否生效中
     *
     * @param watererUUID 浇水者 UUID
     * @param targetUUID 农场主 UUID
     * @return true 表示冷却中，false 表示可以浇水
     */
    fun isOnWaterCooldown(watererUUID: UUID, targetUUID: UUID): Boolean {
        return getWaterCooldownRemaining(watererUUID, targetUUID) > 0
    }

    // ==================== 内部工具方法 ====================

    /**
     * 通知农场主被浇水
     * 在线直接 sendLang，离线写入通知队列
     */
    private fun notifyOwner(waterer: Player, ownerUUID: UUID) {
        val watererName = waterer.name
        val owner = Bukkit.getPlayer(ownerUUID)
        if (owner != null && owner.isOnline) {
            owner.sendLang("friendinteraction-water-notify-owner", watererName)
        } else {
            PlayerDataManager.addNotification(ownerUUID, NotificationType.WATERED, watererName)
        }
    }

    /**
     * 在作物位置播放浇水粒子效果
     */
    private fun playWaterParticle(player: Player, crop: com.hareidus.taboo.farm.foundation.model.CropInstance) {
        val world = Bukkit.getWorld(crop.worldName) ?: return
        val location = org.bukkit.Location(
            world,
            crop.x + 0.5,
            crop.y + 0.5,
            crop.z + 0.5
        )
        try {
            world.spawnParticle(waterParticle, location, 30, 0.3, 0.3, 0.3, 0.05)
        } catch (e: Exception) {
            warning("[Farm] 播放浇水粒子失败: ${e.message}")
        }
    }

    /**
     * 格式化冷却时间为可读字符串
     */
    private fun formatCooldownTime(millis: Long): String {
        val totalSeconds = millis / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return when {
            hours > 0 -> "${hours}h${minutes}m${seconds}s"
            minutes > 0 -> "${minutes}m${seconds}s"
            else -> "${seconds}s"
        }
    }

    // ==================== 重载 ====================

    /** 重载配置 */
    fun reload() {
        config.reload()
        info("[Farm] 好友交互管理器配置已重载")
    }
}
