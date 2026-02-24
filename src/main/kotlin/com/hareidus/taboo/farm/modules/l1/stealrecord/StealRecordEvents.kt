package com.hareidus.taboo.farm.modules.l1.stealrecord

import com.hareidus.taboo.farm.foundation.model.StealRecord
import taboolib.platform.type.BukkitProxyEvent

/** 一条偷菜记录被写入后触发 */
class StealRecordCreatedEvent(
    val record: StealRecord
) : BukkitProxyEvent()
