package com.hareidus.taboo.farm.foundation.database.mysql

import com.hareidus.taboo.farm.foundation.config.MainConfig
import com.hareidus.taboo.farm.foundation.database.DatabaseType
import com.hareidus.taboo.farm.foundation.database.IDatabase
import taboolib.module.database.*
import javax.sql.DataSource

/**
 * MySQL 数据库实现
 *
 * 所有表定义和 CRUD 实现集中在此类。
 * 新增数据领域时：
 * 1. 添加 Table 字段声明
 * 2. 在 init 中调用 createXxxTable()
 * 3. 在 createAllTables() 中添加建表
 * 4. 实现 IDatabase 中声明的 CRUD 方法
 */
class DatabaseMySQL : IDatabase {

    val host: Host<SQL>
    override val type = DatabaseType.MYSQL
    override val dataSource: DataSource

    // ==================== 表定义（各模块逐步追加） ====================

    init {
        host = HostSQL(
            MainConfig.mysqlHost,
            MainConfig.mysqlPort.toString(),
            MainConfig.mysqlUser,
            MainConfig.mysqlPassword,
            MainConfig.mysqlDatabase
        )
        dataSource = host.createDataSource()

        createAllTables()
    }

    private fun createAllTables() {
        // 各模块的表在此统一创建
    }

    override fun close() {
        // MySQL DataSource 由 TabooLib 管理
    }
}
