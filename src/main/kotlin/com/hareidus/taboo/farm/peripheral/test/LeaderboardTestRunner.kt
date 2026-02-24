package com.hareidus.taboo.farm.peripheral.test

import com.hareidus.taboo.farm.modules.l2.leaderboard.LeaderboardManager

/**
 * LeaderboardManager 测试运行器
 *
 * 测试排行榜类别查询、分页计算等纯函数 API。
 */
object LeaderboardTestRunner {

    fun run(): List<TestFramework.TestResult> = TestFramework.runSuite("leaderboard") {

        runTest("getCategories.notEmpty") {
            val categories = LeaderboardManager.getCategories()
            assertTrue("getCategories.notEmpty", "getCategories() should not be empty", categories.isNotEmpty())
        }

        runTest("getCategoryDisplayName.exists") {
            val firstCategory = LeaderboardManager.getCategories().first()
            val displayName = LeaderboardManager.getCategoryDisplayName(firstCategory)
            assertTrue("getCategoryDisplayName.exists", "getCategoryDisplayName('$firstCategory') should not be empty", displayName.isNotEmpty())
        }

        runTest("getEntriesPerPage.positive") {
            val perPage = LeaderboardManager.getEntriesPerPage()
            assertGreaterThan("getEntriesPerPage.positive", "getEntriesPerPage() should be > 0", perPage, 0)
        }

        runTest("getTotalPages.nonNegative") {
            val firstCategory = LeaderboardManager.getCategories().first()
            val totalPages = LeaderboardManager.getTotalPages(firstCategory)
            assertTrue("getTotalPages.nonNegative", "getTotalPages('$firstCategory') should be >= 0", totalPages >= 0)
        }

        runTest("getLeaderboardPage.invalidPage") {
            val firstCategory = LeaderboardManager.getCategories().first()
            val entries = LeaderboardManager.getLeaderboardPage(firstCategory, 99999)
            assertTrue("getLeaderboardPage.invalidPage", "getLeaderboardPage(page=99999) should be empty", entries.isEmpty())
        }

        runTest("getLeaderboardPage.pageOne") {
            val firstCategory = LeaderboardManager.getCategories().first()
            val entries = LeaderboardManager.getLeaderboardPage(firstCategory, 1)
            val perPage = LeaderboardManager.getEntriesPerPage()
            assertTrue("getLeaderboardPage.pageOne", "getLeaderboardPage(page=1).size should be <= $perPage", entries.size <= perPage)
        }
    }
}
