package com.hareidus.taboo.farm.foundation.config

import taboolib.module.configuration.Config
import taboolib.module.configuration.Configuration

/**
 * 主配置管理（地基层）
 *
 * 只包含通用基础设施配置（数据库、插件前缀等）。
 * 业务模块的配置由各模块自己的 ConfigLoader 管理。
 */
object MainConfig {

    @Config("config.yml", autoReload = true)
    lateinit var config: Configuration
        private set

    // ==================== 数据库配置 ====================

    val databaseType: String
        get() = config.getString("database.type", "sqlite") ?: "sqlite"

    val sqliteFile: String
        get() = config.getString("database.sqlite.file", "data.db") ?: "data.db"

    val mysqlHost: String
        get() = config.getString("database.mysql.host", "localhost") ?: "localhost"

    val mysqlPort: Int
        get() = config.getInt("database.mysql.port", 3306)

    val mysqlUser: String
        get() = config.getString("database.mysql.user", "root") ?: "root"

    val mysqlPassword: String
        get() = config.getString("database.mysql.password", "") ?: ""

    val mysqlDatabase: String
        get() = config.getString("database.mysql.database", "farm") ?: "farm"

    // ==================== 通用配置 ====================

    val pluginPrefix: String
        get() = config.getString("general.prefix", "&b[Farm] &f") ?: "&b[Farm] &f"

    // ==================== 方法 ====================

    fun reload() {
        config.reload()
    }
}
