package com.hareidus.taboo.farm.foundation.database

import com.hareidus.taboo.farm.foundation.database.impl.MySQLDatabase
import com.hareidus.taboo.farm.foundation.database.impl.SQLiteDatabase
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.function.info
import taboolib.common.platform.function.warning

/**
 * 数据库管理器
 *
 * 职责：
 * 1. 生命周期管理（启动时创建连接，关闭时断开）
 * 2. 聚合所有模块的 TableDefinition，统一建表
 * 3. 向外暴露 database 实例供各模块 Repository 使用
 *
 * 使用方式：
 * - 各模块在加载时调用 registerTable() 注册自己的 TableDefinition
 * - Repository 通过 DatabaseManager.database.dataSource 获取连接
 */
object DatabaseManager {

    private lateinit var _database: IDatabase
    var initialized: Boolean = false
        private set

    /** 已注册的表定义（启动前由各模块注册） */
    private val tableDefinitions = mutableListOf<TableDefinition>()

    /** 获取数据库实例 */
    val database: IDatabase
        get() {
            if (!initialized) {
                throw IllegalStateException("数据库尚未初始化")
            }
            return _database
        }

    /**
     * 注册模块的表定义
     * 必须在数据库初始化之前调用（通常在模块的 companion object 或 @Awake(CONST) 中）
     */
    fun registerTable(definition: TableDefinition) {
        tableDefinitions.add(definition)
    }

    @Awake(LifeCycle.ENABLE)
    fun init() {
        try {
            _database = DatabaseFactory.createDatabase()
            createAllTables()
            initialized = true
            info("数据库初始化完成，共建 ${tableDefinitions.size} 组模块表")
        } catch (e: Exception) {
            warning("数据库初始化失败: ${e.message}")
            e.printStackTrace()
        }
    }

    @Awake(LifeCycle.DISABLE)
    fun close() {
        if (initialized) {
            try {
                _database.close()
                info("数据库已关闭")
            } catch (e: Exception) {
                warning("数据库关闭失败: ${e.message}")
            }
        }
    }

    /**
     * 根据数据库类型，调用各模块 TableDefinition 的对应建表方法
     */
    private fun createAllTables() {
        val db = _database
        tableDefinitions.forEach { definition ->
            when (db) {
                is SQLiteDatabase -> definition.createSQLiteTables(db.host, db.dataSource)
                is MySQLDatabase -> definition.createMySQLTables(db.host, db.dataSource)
            }
        }
    }
}
