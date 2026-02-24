package com.hareidus.taboo.farm.foundation.event

import com.hareidus.taboo.farm.foundation.model.StealRecord
import taboolib.platform.type.BukkitProxyEvent

/**
 * 偷菜记录创建事件
 *
 * 当一条偷菜记录被成功写入数据库后触发。
 * 供通知系统、成就系统等下游模块监听使用。
 */
class StealRecordCreatedEvent(val record: StealRecord) : BukkitProxyEvent()
