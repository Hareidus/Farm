package com.hareidus.taboo.farm.peripheral.test

import com.hareidus.taboo.farm.foundation.model.Plot
import com.hareidus.taboo.farm.modules.l1.plot.PlotManager
import java.util.UUID

/**
 * 地块管理器测试
 *
 * 测试 PlotManager 的纯计算方法：isInPlot、getPlotCenter
 */
object PlotTestRunner {

    fun run(): List<TestFramework.TestResult> = TestFramework.runSuite("plot") {

        val testUUID = UUID.randomUUID()
        val plot = Plot(
            id = 1L,
            ownerUUID = testUUID,
            gridX = 0,
            gridZ = 0,
            worldName = "farm_world",
            minX = 0,
            minZ = 0,
            maxX = 10,
            maxZ = 10,
            size = 10
        )

        runTest("isInPlot.inside") {
            val result = PlotManager.isInPlot("farm_world", 5, 5, plot)
            assertTrue("isInPlot.inside", "坐标 (5,5) 应在地块 (0,0)-(10,10) 内", result)
        }

        runTest("isInPlot.outside") {
            val result = PlotManager.isInPlot("farm_world", 15, 15, plot)
            assertFalse("isInPlot.outside", "坐标 (15,15) 应在地块 (0,0)-(10,10) 外", result)
        }

        runTest("isInPlot.boundary") {
            val resultMin = PlotManager.isInPlot("farm_world", 0, 0, plot)
            assertTrue("isInPlot.boundary", "边界坐标 (0,0) 应在地块内", resultMin)
            val resultMax = PlotManager.isInPlot("farm_world", 10, 10, plot)
            assertTrue("isInPlot.boundary", "边界坐标 (10,10) 应在地块内", resultMax)
        }

        runTest("isInPlot.wrongWorld") {
            val result = PlotManager.isInPlot("other_world", 5, 5, plot)
            assertFalse("isInPlot.wrongWorld", "不同世界名应返回 false", result)
        }

        runTest("getPlotCenter.basic") {
            val center = PlotManager.getPlotCenter(plot)
            // getPlotCenter 基于 gridX * gridSpacing 计算，gridX=0 → centerX=0
            assertNotNull("getPlotCenter.basic", "返回值不应为 null", center)
            assertEquals("getPlotCenter.basic", "gridX=0 时中心 X 应为 0", 0, center.first)
            assertEquals("getPlotCenter.basic", "gridZ=0 时中心 Z 应为 0", 0, center.third)
        }
    }
}
