package com.hareidus.taboo.farm.foundation.database.sqlite

import com.hareidus.taboo.farm.foundation.config.MainConfig
import com.hareidus.taboo.farm.foundation.database.DatabaseType
import com.hareidus.taboo.farm.foundation.database.IDatabase
import taboolib.common.platform.function.getDataFolder
import taboolib.module.database.*
import java.io.File
import javax.sql.DataSource

/**
 * SQLite 数据库实现
 *
 * 所有表定义和 CRUD 实现集中在此类。
 * 新增数据领域时：
 * 1. 添加 Table 字段声明
 * 2. 在 init 中调用 createXxxTable()
 * 3. 在 createAllTables() 中添加建表
 * 4. 实现 IDatabase 中声明的 CRUD 方法
 */
class DatabaseSQLite : IDatabase {

    val host: Host<SQLite>
    override val type = DatabaseType.SQLITE
    override val dataSource: DataSource

    // ==================== 表定义（各模块逐步追加） ====================

    init {
        val dbFile = File(getDataFolder(), "database/${MainConfig.sqliteFile}")
        dbFile.parentFile?.mkdirs()
        if (!dbFile.exists()) dbFile.createNewFile()

        host = dbFile.getHost()
        dataSource = host.createDataSource()

        createAllTables()
    }

    private fun createAllTables() {
        // 各模块的表在此统一创建
    }

    override fun close() {
        // SQLite DataSource 由 TabooLib 管理
    }
}
