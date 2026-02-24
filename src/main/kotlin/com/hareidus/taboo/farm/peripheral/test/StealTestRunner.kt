package com.hareidus.taboo.farm.peripheral.test

import com.hareidus.taboo.farm.modules.l2.steal.StealManager

/**
 * StealManager 测试运行器
 *
 * 测试偷菜管理器的配置值合法性。
 */
object StealTestRunner {

    fun run(): List<TestFramework.TestResult> = TestFramework.runSuite("steal") {

        runTest("config.baseStealRatio") {
            val ratio = StealManager.config.getDouble("base-steal-ratio", 0.0)
            assertGreaterThan("config.baseStealRatio", "base-steal-ratio should be > 0", ratio, 0.0)
            assertTrue("config.baseStealRatio", "base-steal-ratio should be <= 1.0", ratio <= 1.0)
        }

        runTest("config.enemyBonusRatio") {
            val bonus = StealManager.config.getDouble("enemy-bonus-ratio", -1.0)
            assertTrue("config.enemyBonusRatio", "enemy-bonus-ratio should be >= 0", bonus >= 0.0)
            assertTrue("config.enemyBonusRatio", "enemy-bonus-ratio should be <= 1.0", bonus <= 1.0)
        }

        runTest("config.permissionSteal") {
            val perm = StealManager.config.getString("permission-steal", "")!!
            assertTrue("config.permissionSteal", "permission-steal should not be blank", perm.isNotBlank())
        }
    }
}
