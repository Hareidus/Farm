package com.hareidus.taboo.farm.peripheral.test

import com.hareidus.taboo.farm.modules.l2.shop.ShopManager
import taboolib.module.chat.colored

/**
 * ShopManager 测试运行器
 *
 * 测试 NPC 识别相关 API。
 */
object ShopTestRunner {

    fun run(): List<TestFramework.TestResult> = TestFramework.runSuite("shop") {

        runTest("isShopNpc.null") {
            val result = ShopManager.isShopNpc(null)
            assertFalse("isShopNpc.null", "isShopNpc(null) should be false", result)
        }

        runTest("isShopNpc.empty") {
            val result = ShopManager.isShopNpc("")
            assertFalse("isShopNpc.empty", "isShopNpc('') should be false", result)
        }

        runTest("isShopNpc.random") {
            val result = ShopManager.isShopNpc("RandomName123")
            assertFalse("isShopNpc.random", "isShopNpc('RandomName123') should be false", result)
        }

        runTest("isShopNpc.valid") {
            val rawName = ShopManager.config.getString("npc-name", "&6[收购站]")!!
            val coloredName = rawName.colored()
            val result = ShopManager.isShopNpc(coloredName)
            assertTrue("isShopNpc.valid", "isShopNpc(coloredNpcName) should be true", result)
        }
    }
}
