package com.hareidus.taboo.farm.modules.l1.trap

import com.hareidus.taboo.farm.foundation.database.DatabaseManager
import com.hareidus.taboo.farm.foundation.model.DeployedTrap
import com.hareidus.taboo.farm.foundation.model.TrapDefinition
import com.hareidus.taboo.farm.foundation.model.TrapPenaltyType
import com.hareidus.taboo.farm.modules.l1.economy.EconomyManager
import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.function.info
import taboolib.common.platform.function.warning
import taboolib.module.configuration.Config
import taboolib.module.configuration.Configuration
import taboolib.platform.util.sendLang

/**
 * 陷阱管理器 (L1)
 *
 * 职责：
 * 1. 从配置加载陷阱种类定义
 * 2. 管理地块已部署陷阱的 CRUD
 * 3. 提供陷阱触发判定（概率计算）
 * 4. 执行惩罚效果（减速/扣金币/强制传送占位）
 *
 * 依赖: database_manager, economy_manager
 */
object TrapManager {

    @Config("modules/l1/trap.yml", autoReload = true)
    lateinit var config: Configuration
        private set

    /** 陷阱定义缓存: id -> TrapDefinition */
    private var trapDefinitions: Map<String, TrapDefinition> = emptyMap()

    @Awake(LifeCycle.ENABLE)
    fun init() {
        loadTrapDefinitions()
    }

    // ==================== 配置加载 ====================

    /** 从 trap.yml 加载所有陷阱种类定义 */
    private fun loadTrapDefinitions() {
        val section = config.getConfigurationSection("traps")
        if (section == null) {
            warning("trap.yml 中未找到 traps 配置节")
            return
        }
        trapDefinitions = section.getKeys(false).mapNotNull { id ->
            val sub = section.getConfigurationSection(id) ?: return@mapNotNull null
            val penaltyTypeStr = sub.getString("penalty-type") ?: return@mapNotNull null
            val penaltyType = try {
                TrapPenaltyType.valueOf(penaltyTypeStr)
            } catch (_: IllegalArgumentException) {
                warning("陷阱 $id 的 penalty-type 无效: $penaltyTypeStr")
                return@mapNotNull null
            }
            val materials = mutableMapOf<String, Int>()
            sub.getConfigurationSection("deploy-cost-materials")?.let { matSection ->
                matSection.getKeys(false).forEach { mat ->
                    materials[mat] = matSection.getInt(mat, 0)
                }
            }
            id to TrapDefinition(
                id = id,
                name = sub.getString("name") ?: id,
                penaltyType = penaltyType,
                triggerChance = sub.getDouble("trigger-chance", 0.0),
                deployCostMoney = sub.getDouble("deploy-cost-money", 0.0),
                deployCostMaterials = materials,
                penaltyValue = sub.getDouble("penalty-value", 0.0)
            )
        }.toMap()
        info("已加载 ${trapDefinitions.size} 种陷阱定义")
    }

    // ==================== 定义查询 ====================

    /** 获取指定陷阱定义 */
    fun getTrapDefinition(id: String): TrapDefinition? {
        return trapDefinitions[id]
    }

    /** 获取所有陷阱定义 */
    fun getAllTrapDefinitions(): List<TrapDefinition> {
        return trapDefinitions.values.toList()
    }

    // ==================== 部署管理 ====================

    /** 获取指定地块已部署的所有陷阱 */
    fun getDeployedTraps(plotId: Long): List<DeployedTrap> {
        return DatabaseManager.database.getDeployedTraps(plotId)
    }

    /** 在指定地块的槽位部署陷阱，返回是否成功 */
    fun deployTrap(plotId: Long, trapTypeId: String, slotIndex: Int): Boolean {
        if (getTrapDefinition(trapTypeId) == null) {
            warning("尝试部署不存在的陷阱类型: $trapTypeId")
            return false
        }
        return DatabaseManager.database.deployTrap(plotId, trapTypeId, slotIndex)
    }

    /** 移除指定地块指定槽位的陷阱 */
    fun removeTrap(plotId: Long, slotIndex: Int): Boolean {
        return DatabaseManager.database.removeTrap(plotId, slotIndex)
    }

    /** 移除指定地块的所有陷阱 */
    fun removeAllTraps(plotId: Long): Boolean {
        return DatabaseManager.database.removeAllTraps(plotId)
    }
    // ==================== 触发判定 ====================

    /**
     * 检查指定地块是否有陷阱被触发
     * 遍历地块已部署陷阱，按各自触发概率判定
     * @return 触发的陷阱定义，无触发返回 null
     */
    fun checkTrapTrigger(plotId: Long): TrapDefinition? {
        val deployed = getDeployedTraps(plotId)
        if (deployed.isEmpty()) return null
        for (trap in deployed) {
            val definition = getTrapDefinition(trap.trapTypeId) ?: continue
            if (Math.random() < definition.triggerChance) {
                return definition
            }
        }
        return null
    }

    // ==================== 惩罚执行 ====================

    /**
     * 对玩家执行陷阱惩罚效果
     * - SLOWNESS: 施加缓慢药水效果
     * - MONEY_DEDUCTION: 通过 EconomyManager 扣除金币
     * - FORCE_TELEPORT: 标记需要强制传送（由 L2 steal_manager 处理实际传送）
     */
    fun executePenalty(player: Player, trap: TrapDefinition) {
        when (trap.penaltyType) {
            TrapPenaltyType.SLOWNESS -> {
                val ticks = (trap.penaltyValue * 20).toInt()
                player.addPotionEffect(
                    PotionEffect(PotionEffectType.SLOWNESS, ticks, 1)
                )
                player.sendLang("trap-triggered-slowness", trap.penaltyValue.toInt())
            }
            TrapPenaltyType.MONEY_DEDUCTION -> {
                EconomyManager.withdraw(player, trap.penaltyValue)
                player.sendLang("trap-triggered-money", trap.penaltyValue)
            }
            TrapPenaltyType.FORCE_TELEPORT -> {
                // 实际传送逻辑由 L2 层处理，此处仅发送提示
                player.sendLang("trap-triggered-teleport")
            }
        }
    }

    // ==================== 重载 ====================

    /** 重载配置 */
    fun reload() {
        config.reload()
        loadTrapDefinitions()
    }
}
