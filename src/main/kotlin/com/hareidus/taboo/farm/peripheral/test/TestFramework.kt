package com.hareidus.taboo.farm.peripheral.test

import taboolib.common.platform.function.info

/**
 * 服务器内测试框架
 *
 * 提供 runSuite / runTest / 断言方法，输出 [TEST] 格式日志。
 */
object TestFramework {

    data class TestResult(
        val name: String,
        val passed: Boolean,
        val message: String
    )

    fun runSuite(prefix: String, block: SuiteContext.() -> Unit): List<TestResult> {
        info("[TEST] START $prefix")
        val ctx = SuiteContext(prefix)
        ctx.block()
        val passed = ctx.results.count { it.passed }
        val failed = ctx.results.count { !it.passed }
        val total = ctx.results.size
        info("[TEST] SUMMARY $prefix: passed=$passed failed=$failed total=$total")
        return ctx.results
    }

    class SuiteContext(private val prefix: String) {
        val results = mutableListOf<TestResult>()

        fun runTest(name: String, block: () -> Unit) {
            try {
                block()
                results.add(TestResult("$prefix.$name", true, "OK"))
                info("[TEST] PASS $prefix.$name")
            } catch (e: AssertionError) {
                results.add(TestResult("$prefix.$name", false, e.message ?: ""))
                info("[TEST] FAIL $prefix.$name: ${e.message}")
            } catch (e: Exception) {
                results.add(TestResult("$prefix.$name", false, "${e::class.simpleName}: ${e.message}"))
                info("[TEST] ERROR $prefix.$name: ${e::class.simpleName}: ${e.message}")
            }
        }

        fun assertEquals(testName: String, desc: String, expected: Any?, actual: Any?) {
            if (expected != actual) {
                throw AssertionError("$desc | expected=$expected actual=$actual")
            }
        }

        fun assertTrue(testName: String, desc: String, value: Boolean) {
            if (!value) {
                throw AssertionError("$desc | expected=true actual=false")
            }
        }

        fun assertFalse(testName: String, desc: String, value: Boolean) {
            if (value) {
                throw AssertionError("$desc | expected=false actual=true")
            }
        }

        fun assertInRange(testName: String, desc: String, value: Number, min: Number, max: Number) {
            val v = value.toDouble()
            if (v < min.toDouble() || v > max.toDouble()) {
                throw AssertionError("$desc | value=$value not in [$min, $max]")
            }
        }

        fun assertGreaterThan(testName: String, desc: String, value: Number, threshold: Number) {
            if (value.toDouble() <= threshold.toDouble()) {
                throw AssertionError("$desc | value=$value not > $threshold")
            }
        }

        fun assertNotNull(testName: String, desc: String, value: Any?) {
            if (value == null) {
                throw AssertionError("$desc | expected non-null but got null")
            }
        }
    }
}
