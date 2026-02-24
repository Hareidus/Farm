package com.hareidus.taboo.farm.peripheral.test

import com.hareidus.taboo.farm.modules.l1.farmlevel.FarmLevelManager
import com.hareidus.taboo.farm.modules.l2.upgrade.UpgradeManager
import java.util.UUID

/**
 * UpgradeManager 测试运行器
 *
 * 测试升级信息查询、陷阱部署校验等公开 API。
 */
object UpgradeTestRunner {

    fun run(): List<TestFramework.TestResult> = TestFramework.runSuite("upgrade") {

        runTest("getUpgradeInfo.noPlot") {
            val info = UpgradeManager.getUpgradeInfo(UUID.randomUUID())
            assertEquals("getUpgradeInfo.noPlot", "getUpgradeInfo(randomUUID) should be null (no plot)", null, info)
        }

        runTest("canDeployTrap.noPlot") {
            val result = UpgradeManager.canDeployTrap(UUID.randomUUID())
            assertFalse("canDeployTrap.noPlot", "canDeployTrap(randomUUID) should be false (no plot)", result)
        }

        runTest("upgradeInfo.maxLevelConsistency") {
            val maxLevel = FarmLevelManager.getMaxLevel()
            val def = FarmLevelManager.getDefinition(maxLevel)
            assertNotNull("upgradeInfo.maxLevelConsistency", "getDefinition(maxLevel=$maxLevel) should not be null", def)
            // Verify no definition exists above maxLevel
            val aboveMax = FarmLevelManager.getDefinition(maxLevel + 1)
            assertEquals("upgradeInfo.maxLevelConsistency", "getDefinition(maxLevel+1) should be null", null, aboveMax)
        }
    }
}
