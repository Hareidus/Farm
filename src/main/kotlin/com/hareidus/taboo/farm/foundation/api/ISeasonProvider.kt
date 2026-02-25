package com.hareidus.taboo.farm.foundation.api

import com.hareidus.taboo.farm.foundation.model.Season
import com.hareidus.taboo.farm.foundation.model.SeasonModifier
import com.hareidus.taboo.farm.foundation.model.WeatherState

/**
 * 季节系统提供者接口（预留）
 *
 * 外部插件可实现此接口并注入到 FarmAPI.seasonProvider，
 * 为农场系统提供季节/天气数据。
 */
interface ISeasonProvider {

    /** 获取当前季节 */
    fun getCurrentSeason(): Season

    /** 获取当前天气/季节状态快照 */
    fun getWeatherState(): WeatherState

    /** 获取指定作物在当前季节的修饰符 */
    fun getModifier(cropTypeId: String): SeasonModifier

    /** 获取指定作物在指定季节的修饰符 */
    fun getModifier(cropTypeId: String, season: Season): SeasonModifier
}
