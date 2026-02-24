package com.hareidus.taboo.farm.peripheral.test

import com.hareidus.taboo.farm.modules.l2.achievement.AchievementManager

/**
 * AchievementManager 测试运行器
 *
 * 测试成就定义加载与查询 API。
 */
object AchievementTestRunner {

    fun run(): List<TestFramework.TestResult> = TestFramework.runSuite("achievement") {

        runTest("getAllDefinitions.notEmpty") {
            val defs = AchievementManager.getAllDefinitions()
            assertTrue("getAllDefinitions.notEmpty", "getAllDefinitions() should not be empty", defs.isNotEmpty())
        }

        runTest("getDefinition.exists") {
            val firstDef = AchievementManager.getAllDefinitions().first()
            val found = AchievementManager.getDefinition(firstDef.id)
            assertNotNull("getDefinition.exists", "getDefinition('${firstDef.id}') should not be null", found)
        }

        runTest("getDefinition.notExists") {
            val found = AchievementManager.getDefinition("nonexistent_achievement_xyz")
            assertEquals("getDefinition.notExists", "getDefinition('nonexistent_achievement_xyz') should be null", null, found)
        }

        runTest("threshold.positive") {
            val defs = AchievementManager.getAllDefinitions()
            for (def in defs) {
                assertGreaterThan("threshold.positive", "threshold of '${def.id}' should be > 0", def.threshold, 0L)
            }
        }

        runTest("triggerType.notBlank") {
            val defs = AchievementManager.getAllDefinitions()
            for (def in defs) {
                assertTrue("triggerType.notBlank", "triggerType of '${def.id}' should not be blank", def.triggerType.isNotBlank())
            }
        }
    }
}
