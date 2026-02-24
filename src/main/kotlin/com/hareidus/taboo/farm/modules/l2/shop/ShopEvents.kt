package com.hareidus.taboo.farm.modules.l2.shop

import taboolib.platform.type.BukkitProxyEvent
import java.util.UUID

/**
 * 玩家成功出售作物后触发
 *
 * @param playerUUID 出售者 UUID
 * @param cropTypeId 作物类型 ID
 * @param amount 出售数量
 * @param totalPrice 出售总金额
 */
class CropSoldEvent(
    val playerUUID: UUID,
    val cropTypeId: String,
    val amount: Int,
    val totalPrice: Double
) : BukkitProxyEvent()
