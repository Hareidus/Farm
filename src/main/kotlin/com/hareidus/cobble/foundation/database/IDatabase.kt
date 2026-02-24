package com.hareidus.cobble.foundation.database

import javax.sql.DataSource

/**
 * 数据库接口（地基层）
 *
 * 只负责连接管理，不含任何业务 CRUD。
 * 业务操作由各模块的 Repository 自行实现。
 */
interface IDatabase {

    /** 数据库类型 */
    val type: DatabaseType

    /** 数据源（各模块 Repository 通过此获取连接） */
    val dataSource: DataSource

    /** 关闭数据库连接 */
    fun close()
}
