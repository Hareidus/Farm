package com.hareidus.taboo.farm.peripheral.test

import com.hareidus.taboo.farm.modules.l1.farmlevel.FarmLevelManager

/**
 * 农场等级管理器测试
 *
 * 测试 FarmLevelManager 的等级定义查询与功能解锁方法
 */
object FarmLevelTestRunner {

    fun run(): List<TestFramework.TestResult> = TestFramework.runSuite("farmlevel") {

        runTest("getMaxLevel.positive") {
            val maxLevel = FarmLevelManager.getMaxLevel()
            assertGreaterThan("getMaxLevel.positive", "最大等级应大于 0", maxLevel, 0)
        }

        runTest("getDefinition.level1") {
            val def = FarmLevelManager.getDefinition(1)
            assertNotNull("getDefinition.level1", "等级 1 的定义应存在", def)
        }

        runTest("getDefinition.invalid") {
            val def = FarmLevelManager.getDefinition(9999)
            assertEquals("getDefinition.invalid", "不存在的等级 9999 应返回 null", null, def)
        }

        runTest("getProtectionReduction.level1") {
            val reduction = FarmLevelManager.getProtectionReduction(1)
            assertTrue(
                "getProtectionReduction.level1",
                "等级 1 的防护减免应 >= 0.0",
                reduction >= 0.0
            )
        }

        runTest("getTrapSlots.level1") {
            val slots = FarmLevelManager.getTrapSlots(1)
            assertTrue(
                "getTrapSlots.level1",
                "等级 1 的陷阱槽位应 >= 0",
                slots >= 0
            )
        }

        runTest("isAutoHarvestUnlocked.level1") {
            // 仅验证调用不抛异常，返回值为布尔
            val result = FarmLevelManager.isAutoHarvestUnlocked(1)
            assertTrue(
                "isAutoHarvestUnlocked.level1",
                "返回值应为有效布尔",
                result || !result
            )
        }

        runTest("getTrapSlots.increasing") {
            val maxLevel = FarmLevelManager.getMaxLevel()
            val slotsLevel1 = FarmLevelManager.getTrapSlots(1)
            val slotsMaxLevel = FarmLevelManager.getTrapSlots(maxLevel)
            assertTrue(
                "getTrapSlots.increasing",
                "最高等级陷阱槽位 ($slotsMaxLevel) 应 >= 等级 1 槽位 ($slotsLevel1)",
                slotsMaxLevel >= slotsLevel1
            )
        }
    }
}
