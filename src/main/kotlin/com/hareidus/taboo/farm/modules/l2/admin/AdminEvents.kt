package com.hareidus.taboo.farm.modules.l2.admin

import taboolib.platform.type.BukkitProxyEvent
import java.util.UUID

/**
 * 管理员重置玩家农场后触发
 *
 * @param adminUUID 执行重置的管理员 UUID
 * @param targetUUID 被重置的目标玩家 UUID
 */
class FarmResetEvent(
    val adminUUID: UUID,
    val targetUUID: UUID
) : BukkitProxyEvent()
