package com.hareidus.cobble.foundation.database

import com.hareidus.cobble.foundation.database.impl.MySQLDatabase
import com.hareidus.cobble.foundation.database.impl.SQLiteDatabase
import com.hareidus.cobble.foundation.config.MainConfig
import taboolib.common.platform.function.info

/**
 * 数据库工厂
 * 根据配置创建对应的数据库实现
 */
object DatabaseFactory {

    fun createDatabase(): IDatabase {
        val type = DatabaseType.fromString(MainConfig.databaseType)
        info("正在初始化数据库，类型: $type")

        return when (type) {
            DatabaseType.MYSQL -> MySQLDatabase()
            DatabaseType.SQLITE -> SQLiteDatabase()
        }
    }
}
