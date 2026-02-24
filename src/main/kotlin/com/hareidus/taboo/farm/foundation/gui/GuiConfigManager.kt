package com.hareidus.taboo.farm.foundation.gui

import EasyLib.EasyGui.EasyGuiConfig.GuiConfig.GuiConfig
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.function.info
import taboolib.module.configuration.Config
import taboolib.module.configuration.Configuration

/**
 * GUI 配置管理器
 *
 * 集中管理所有 GUI 的 YAML 配置文件和 GuiConfig 实例。
 * 每新增一个 GUI，需在此添加 @Config 字段 + GuiConfig 字段 + init + reload。
 */
object GuiConfigManager {

    // ==================== Shop GUI ====================
    @Config("gui/shop.yml", autoReload = true)
    lateinit var shopConfig: Configuration
        private set
    var shopGuiConfig: GuiConfig? = null
        private set

    // ==================== Leaderboard GUI ====================
    @Config("gui/leaderboard.yml", autoReload = true)
    lateinit var leaderboardConfig: Configuration
        private set
    var leaderboardGuiConfig: GuiConfig? = null
        private set

    // ==================== Upgrade GUI ====================
    @Config("gui/upgrade.yml", autoReload = true)
    lateinit var upgradeConfig: Configuration
        private set
    var upgradeGuiConfig: GuiConfig? = null
        private set

    // ==================== Trap Deploy GUI ====================
    @Config("gui/trap_deploy.yml", autoReload = true)
    lateinit var trapDeployConfig: Configuration
        private set
    var trapDeployGuiConfig: GuiConfig? = null
        private set

    // ==================== Social GUI ====================
    @Config("gui/social.yml", autoReload = true)
    lateinit var socialConfig: Configuration
        private set
    var socialGuiConfig: GuiConfig? = null
        private set

    // ==================== 初始化 ====================

    @Awake(LifeCycle.ENABLE)
    fun initializeGuiConfigs() {
        shopGuiConfig = shopConfig.file?.let { GuiConfig(it) }
        leaderboardGuiConfig = leaderboardConfig.file?.let { GuiConfig(it) }
        upgradeGuiConfig = upgradeConfig.file?.let { GuiConfig(it) }
        trapDeployGuiConfig = trapDeployConfig.file?.let { GuiConfig(it) }
        socialGuiConfig = socialConfig.file?.let { GuiConfig(it) }
        info("[Farm] GUI 配置管理器已初始化 (5 个 GUI)")
    }

    // ==================== 重载 ====================

    fun reload() {
        shopConfig.reload()
        leaderboardConfig.reload()
        upgradeConfig.reload()
        trapDeployConfig.reload()
        socialConfig.reload()
        initializeGuiConfigs()
    }
}
