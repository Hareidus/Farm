package com.hareidus.taboo.farm.foundation.database

import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.function.info
import taboolib.common.platform.function.warning

/**
 * 数据库管理器（单例）
 *
 * 职责：
 * 1. 生命周期管理（启动时创建连接，关闭时断开）
 * 2. 向外暴露 database 实例供各模块 Service 使用
 *
 * 使用方式：
 * - Service 层通过 DatabaseManager.database 访问 IDatabase 接口
 * - 禁止在 GUI/Command 层直接访问
 */
object DatabaseManager {

    private lateinit var _database: IDatabase
    var initialized: Boolean = false
        private set

    /** 获取数据库实例 */
    val database: IDatabase
        get() {
            if (!initialized) {
                throw IllegalStateException("数据库尚未初始化")
            }
            return _database
        }

    @Awake(LifeCycle.ENABLE)
    fun init() {
        try {
            _database = DatabaseFactory.createDatabase()
            initialized = true
            info("[Farm] 数据库初始化完成")
        } catch (e: Exception) {
            warning("[Farm] 数据库初始化失败: ${e.message}")
            e.printStackTrace()
        }
    }

    @Awake(LifeCycle.DISABLE)
    fun close() {
        if (initialized) {
            try {
                _database.close()
                info("[Farm] 数据库已关闭")
            } catch (e: Exception) {
                warning("[Farm] 数据库关闭失败: ${e.message}")
            }
        }
    }
}
