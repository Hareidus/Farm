package com.hareidus.taboo.farm.peripheral.test

import com.hareidus.taboo.farm.modules.l1.trap.TrapManager

/**
 * TrapManager 测试运行器
 *
 * 测试陷阱定义查询相关 API。
 */
object TrapTestRunner {

    fun run(): List<TestFramework.TestResult> = TestFramework.runSuite("trap") {

        runTest("getAllDefinitions.notEmpty") {
            val defs = TrapManager.getAllTrapDefinitions()
            assertTrue("getAllDefinitions.notEmpty", "getAllTrapDefinitions() should not be empty", defs.isNotEmpty())
        }

        runTest("getDefinition.exists") {
            val defs = TrapManager.getAllTrapDefinitions()
            val firstId = defs.first().id
            val def = TrapManager.getTrapDefinition(firstId)
            assertNotNull("getDefinition.exists", "getTrapDefinition('$firstId') should not be null", def)
        }

        runTest("getDefinition.notExists") {
            val def = TrapManager.getTrapDefinition("nonexistent_trap_xyz")
            assertEquals("getDefinition.notExists", "getTrapDefinition('nonexistent_trap_xyz') should be null", null, def)
        }

        runTest("triggerChance.range") {
            val defs = TrapManager.getAllTrapDefinitions()
            for (def in defs) {
                assertInRange("triggerChance.range", "triggerChance of '${def.id}' should be in [0.0, 1.0]", def.triggerChance, 0.0, 1.0)
            }
        }

        runTest("deployCost.nonNegative") {
            val defs = TrapManager.getAllTrapDefinitions()
            for (def in defs) {
                assertTrue("deployCost.nonNegative", "deployCostMoney of '${def.id}' should be >= 0", def.deployCostMoney >= 0.0)
            }
        }
    }
}
