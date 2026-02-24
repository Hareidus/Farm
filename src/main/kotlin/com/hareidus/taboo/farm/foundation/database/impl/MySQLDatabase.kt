package com.hareidus.taboo.farm.foundation.database.impl

import com.hareidus.taboo.farm.foundation.config.MainConfig
import com.hareidus.taboo.farm.foundation.database.DatabaseType
import com.hareidus.taboo.farm.foundation.database.IDatabase
import taboolib.module.database.Host
import taboolib.module.database.HostSQL
import taboolib.module.database.SQL
import javax.sql.DataSource

/**
 * MySQL 数据库实现
 *
 * 只负责连接管理，不含任何业务表定义和 CRUD。
 * 表定义由各模块的 TableDefinition 实现提供，
 * 通过 DatabaseManager 在启动时统一建表。
 */
class MySQLDatabase : IDatabase {

    /** MySQL Host（建表时需要） */
    val host: Host<SQL>

    override val type = DatabaseType.MYSQL
    override val dataSource: DataSource

    init {
        host = HostSQL(
            MainConfig.mysqlHost,
            MainConfig.mysqlPort.toString(),
            MainConfig.mysqlUser,
            MainConfig.mysqlPassword,
            MainConfig.mysqlDatabase
        )
        dataSource = host.createDataSource()
    }

    override fun close() {
        // MySQL DataSource 由 TabooLib 管理，无需手动关闭
    }
}
