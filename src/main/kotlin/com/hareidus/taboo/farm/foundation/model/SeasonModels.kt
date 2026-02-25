package com.hareidus.taboo.farm.foundation.model

/** 季节枚举 */
enum class Season {
    SPRING,
    SUMMER,
    AUTUMN,
    WINTER
}

/** 季节对作物的修饰符 */
data class SeasonModifier(
    val season: Season,
    val cropTypeId: String,
    val growthMultiplier: Double = 1.0,
    val harvestMultiplier: Double = 1.0,
    val priceMultiplier: Double = 1.0,
    val canPlant: Boolean = true
)

/** 天气/季节状态快照 */
data class WeatherState(
    val season: Season,
    val dayInSeason: Int,
    val totalDays: Int
)
