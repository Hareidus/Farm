package com.hareidus.taboo.farm.foundation.database

import taboolib.module.database.*
import javax.sql.DataSource

/**
 * 建表契约接口
 *
 * 各业务模块在 data/ 目录下实现此接口，声明本模块需要的数据库表。
 * 地基层 DatabaseManager 在启动时聚合所有实现并统一建表。
 *
 * 由于 TabooLib 的 SQLite 和 MySQL 使用不同的类型系统，
 * 模块需要分别提供两种实现。
 *
 * 使用示例（模块内 data/ExampleTables.kt）：
 * ```kotlin
 * object ExampleTables : TableDefinition {
 *
 *     lateinit var exampleTable: Table<*, *>
 *         private set
 *
 *     override fun createSQLiteTables(host: Host<SQLite>, dataSource: DataSource) {
 *         exampleTable = Table("my_example", host) {
 *             add("id") { type(ColumnTypeSQLite.INTEGER) }
 *             add("name") { type(ColumnTypeSQLite.TEXT) }
 *         }
 *         exampleTable.workspace(dataSource) { createTable() }.run()
 *     }
 *
 *     override fun createMySQLTables(host: Host<SQL>, dataSource: DataSource) {
 *         exampleTable = Table("my_example", host) {
 *             add("id") { type(ColumnTypeSQL.INT) }
 *             add("name") { type(ColumnTypeSQL.VARCHAR, 128) }
 *         }
 *         exampleTable.workspace(dataSource) { createTable() }.run()
 *     }
 * }
 * ```
 */
interface TableDefinition {

    /**
     * 为 SQLite 创建表
     */
    fun createSQLiteTables(host: Host<SQLite>, dataSource: DataSource)

    /**
     * 为 MySQL 创建表
     */
    fun createMySQLTables(host: Host<SQL>, dataSource: DataSource)
}
