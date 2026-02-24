package com.hareidus.taboo.farm.peripheral.test

import com.hareidus.taboo.farm.foundation.model.CropInstance
import com.hareidus.taboo.farm.modules.l1.crop.CropManager
import java.util.UUID

/**
 * 作物管理器测试
 *
 * 测试 CropManager 的定义查询与生长计算方法
 */
object CropTestRunner {

    fun run(): List<TestFramework.TestResult> = TestFramework.runSuite("crop") {

        runTest("getAllDefinitions.notEmpty") {
            val defs = CropManager.getAllCropDefinitions()
            assertTrue("getAllDefinitions.notEmpty", "作物定义列表不应为空", defs.isNotEmpty())
        }

        runTest("getCropDefinition.exists") {
            val defs = CropManager.getAllCropDefinitions()
            assertTrue("getCropDefinition.exists", "至少存在一个作物定义", defs.isNotEmpty())
            val firstId = defs.first().id
            val def = CropManager.getCropDefinition(firstId)
            assertNotNull("getCropDefinition.exists", "按 ID 查询已有作物定义应非 null", def)
        }

        runTest("getCropDefinition.notExists") {
            val def = CropManager.getCropDefinition("nonexistent_crop_xyz")
            assertEquals("getCropDefinition.notExists", "不存在的作物 ID 应返回 null", null, def)
        }

        runTest("calculateHarvestAmount.range") {
            val defs = CropManager.getAllCropDefinitions()
            assertTrue("calculateHarvestAmount.range", "需要至少一个作物定义", defs.isNotEmpty())
            val cropDef = defs.first()
            repeat(20) { i ->
                val amount = CropManager.calculateHarvestAmount(cropDef)
                assertInRange(
                    "calculateHarvestAmount.range",
                    "第 ${i + 1} 次收获量应在 [${cropDef.harvestMinAmount}, ${cropDef.harvestMaxAmount}]",
                    amount,
                    cropDef.harvestMinAmount,
                    cropDef.harvestMaxAmount
                )
            }
        }

        runTest("calculateGrowthStage.justPlanted") {
            val defs = CropManager.getAllCropDefinitions()
            assertTrue("calculateGrowthStage.justPlanted", "需要至少一个作物定义", defs.isNotEmpty())
            val crop = CropInstance(
                id = 0L,
                cropTypeId = defs.first().id,
                plotId = 0L,
                ownerUUID = UUID.randomUUID(),
                worldName = "farm_world",
                x = 0, y = 64, z = 0,
                plantedAt = System.currentTimeMillis()
            )
            val stage = CropManager.calculateGrowthStage(crop)
            assertEquals("calculateGrowthStage.justPlanted", "刚种植的作物应处于阶段 0", 0, stage)
        }

        runTest("isMature.justPlanted") {
            val defs = CropManager.getAllCropDefinitions()
            assertTrue("isMature.justPlanted", "需要至少一个作物定义", defs.isNotEmpty())
            val crop = CropInstance(
                id = 0L,
                cropTypeId = defs.first().id,
                plotId = 0L,
                ownerUUID = UUID.randomUUID(),
                worldName = "farm_world",
                x = 0, y = 64, z = 0,
                plantedAt = System.currentTimeMillis()
            )
            val mature = CropManager.isMature(crop)
            assertFalse("isMature.justPlanted", "刚种植的作物不应成熟", mature)
        }
    }
}
