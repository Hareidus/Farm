package com.hareidus.taboo.farm.foundation.database

/**
 * 数据库类型枚举
 */
enum class DatabaseType {
    SQLITE,
    MYSQL;

    companion object {
        fun fromString(value: String): DatabaseType {
            return when (value.uppercase()) {
                "MYSQL" -> MYSQL
                else -> SQLITE
            }
        }
    }
}
