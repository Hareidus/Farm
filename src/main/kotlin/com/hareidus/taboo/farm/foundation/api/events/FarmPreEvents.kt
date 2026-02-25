package com.hareidus.taboo.farm.foundation.api.events

import com.hareidus.taboo.farm.foundation.model.CropInstance
import com.hareidus.taboo.farm.foundation.model.Plot
import org.bukkit.entity.Player
import taboolib.platform.type.BukkitProxyEvent
import java.util.UUID

/**
 * 种植前事件（可取消）
 * 触发位置: HarvestManager 种植逻辑前
 * 用途: 拦截种植（季节限制、地块类型限制）
 */
class PreCropPlantEvent(
    val player: Player,
    val cropTypeId: String,
    val plotId: Long,
    val x: Int,
    val y: Int,
    val z: Int
) : BukkitProxyEvent() {
    override val allowCancelled: Boolean get() = true
}

/**
 * 收割前事件（可取消）
 * 触发位置: HarvestManager 收割逻辑前
 * 用途: 修改产量（季节加成）、拦截收割
 */
class PreCropHarvestEvent(
    val player: Player,
    val crop: CropInstance,
    var harvestAmount: Int
) : BukkitProxyEvent() {
    override val allowCancelled: Boolean get() = true
}

/**
 * 农场升级前事件（可取消）
 * 触发位置: UpgradeManager.performUpgrade() 前
 * 用途: 自定义升级条件
 */
class PreFarmUpgradeEvent(
    val player: Player,
    val currentLevel: Int,
    val targetLevel: Int
) : BukkitProxyEvent() {
    override val allowCancelled: Boolean get() = true
}
/**
 * 陷阱部署前事件（可取消）
 * 触发位置: UpgradeManager.deployTrap() 前
 * 用途: 自定义部署条件
 */
class PreTrapDeployEvent(
    val player: Player,
    val trapTypeId: String,
    val slotIndex: Int,
    val plotId: Long
) : BukkitProxyEvent() {
    override val allowCancelled: Boolean get() = true
}

/**
 * 农场传送前事件（可取消）
 * 触发位置: FarmTeleportManager 传送前
 * 用途: 传送拦截（战斗中禁止等）
 */
class PreFarmTeleportEvent(
    val player: Player,
    val targetUUID: UUID,
    val isOwnFarm: Boolean
) : BukkitProxyEvent() {
    override val allowCancelled: Boolean get() = true
}

/**
 * 出售作物前事件（可取消）
 * 触发位置: ShopManager.sellCrops() 前
 * 用途: 修改售价（季节浮动）、拦截出售
 */
class PreCropSellEvent(
    val player: Player,
    val cropTypeId: String,
    val amount: Int,
    var sellPrice: Double
) : BukkitProxyEvent() {
    override val allowCancelled: Boolean get() = true
}

/**
 * 浇水前事件（可取消）
 * 触发位置: FriendInteractionManager.waterCrop() 前
 * 用途: 拦截浇水
 */
class PreCropWaterEvent(
    val player: Player,
    val cropId: Long,
    val ownerUUID: UUID
) : BukkitProxyEvent() {
    override val allowCancelled: Boolean get() = true
}

/**
 * 看门宠物部署前事件（可取消）
 * 触发位置: GuardPetGui.handleDeploy() 前
 * 用途: 自定义部署条件
 */
class PreGuardPetDeployEvent(
    val player: Player,
    val plotId: Long,
    val petTypeId: String
) : BukkitProxyEvent() {
    override val allowCancelled: Boolean get() = true
}
