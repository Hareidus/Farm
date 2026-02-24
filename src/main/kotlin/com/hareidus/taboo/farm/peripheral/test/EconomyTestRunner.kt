package com.hareidus.taboo.farm.peripheral.test

import com.hareidus.taboo.farm.modules.l1.economy.EconomyManager

/**
 * EconomyManager 测试运行器
 *
 * 测试作物价格查询相关 API。
 */
object EconomyTestRunner {

    fun run(): List<TestFramework.TestResult> = TestFramework.runSuite("economy") {

        runTest("getAllCropPrices.notEmpty") {
            val prices = EconomyManager.getAllCropPrices()
            assertTrue("getAllCropPrices.notEmpty", "getAllCropPrices() should not be empty", prices.isNotEmpty())
        }

        runTest("getCropPrice.exists") {
            val prices = EconomyManager.getAllCropPrices()
            val firstKey = prices.keys.first()
            val price = EconomyManager.getCropPrice(firstKey)
            assertNotNull("getCropPrice.exists", "getCropPrice('$firstKey') should not be null", price)
        }

        runTest("getCropPrice.notExists") {
            val price = EconomyManager.getCropPrice("nonexistent_crop_xyz")
            assertEquals("getCropPrice.notExists", "getCropPrice('nonexistent_crop_xyz') should be null", null, price)
        }

        runTest("getCropPrice.positive") {
            val prices = EconomyManager.getAllCropPrices()
            for ((cropId, price) in prices) {
                assertGreaterThan("getCropPrice.positive", "price of '$cropId' should be > 0", price, 0.0)
            }
        }
    }
}
