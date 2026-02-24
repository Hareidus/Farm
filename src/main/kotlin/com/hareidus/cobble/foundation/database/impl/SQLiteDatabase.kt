package com.hareidus.cobble.foundation.database.impl

import com.hareidus.cobble.foundation.config.MainConfig
import com.hareidus.cobble.foundation.database.DatabaseType
import com.hareidus.cobble.foundation.database.IDatabase
import taboolib.common.platform.function.getDataFolder
import taboolib.module.database.Host
import taboolib.module.database.SQLite
import java.io.File
import javax.sql.DataSource

/**
 * SQLite 数据库实现
 *
 * 只负责连接管理，不含任何业务表定义和 CRUD。
 * 表定义由各模块的 TableDefinition 实现提供，
 * 通过 DatabaseManager 在启动时统一建表。
 */
class SQLiteDatabase : IDatabase {

    /** SQLite Host（建表时需要） */
    val host: Host<SQLite>

    override val type = DatabaseType.SQLITE
    override val dataSource: DataSource

    init {
        val dbFile = File(getDataFolder(), "database/${MainConfig.sqliteFile}")
        dbFile.parentFile?.mkdirs()
        if (!dbFile.exists()) dbFile.createNewFile()

        host = dbFile.getHost()
        dataSource = host.createDataSource()
    }

    override fun close() {
        // SQLite DataSource 由 TabooLib 管理，无需手动关闭
    }
}
