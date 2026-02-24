package com.hareidus.taboo.farm.foundation.event

import taboolib.platform.type.BukkitProxyEvent

/**
 * 排行榜数据刷新完成事件
 *
 * 当排行榜缓存从数据库重新加载后触发。
 * 供下游模块（如 placeholder_expansion）监听使用。
 */
class LeaderboardRefreshedEvent(
    val category: String,
    val refreshTime: Long
) : BukkitProxyEvent()
