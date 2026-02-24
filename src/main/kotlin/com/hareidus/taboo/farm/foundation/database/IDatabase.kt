package com.hareidus.taboo.farm.foundation.database

import javax.sql.DataSource

/**
 * 数据库接口（地基层）
 *
 * 所有数据库 CRUD 操作在此接口声明，由 SQLite 和 MySQL 两个实现类同时实现。
 * 各模块通过 DatabaseManager.database 访问，禁止直接创建实例。
 * 新增数据领域时，在此接口追加方法，并在两个实现类中同步实现。
 */
interface IDatabase {

    /** 数据库类型 */
    val type: DatabaseType

    /** 数据源 */
    val dataSource: DataSource

    /** 关闭数据库连接 */
    fun close()

    // ==================== 各模块 CRUD 方法由 Phase 2+ 逐步追加 ====================
}
