package com.hareidus.taboo.farm.foundation.api.events

import com.hareidus.taboo.farm.foundation.model.CropDefinition
import com.hareidus.taboo.farm.foundation.model.TrapDefinition
import taboolib.platform.type.BukkitProxyEvent

/** 作物定义被运行时注册后触发 */
class CropDefinitionRegisteredEvent(
    val definition: CropDefinition
) : BukkitProxyEvent()

/** 陷阱定义被运行时注册后触发 */
class TrapDefinitionRegisteredEvent(
    val definition: TrapDefinition
) : BukkitProxyEvent()
