package com.hareidus.taboo.farm.foundation.gui


import EasyLib.Utils.infoType
import EasyLib.function.FunctionManager
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import taboolib.module.chat.colored
import taboolib.module.nms.getI18nName
import taboolib.platform.util.asLangText
import taboolib.platform.util.asLangTextList

/**
 * Matcher 条件渲染器
 *
 * 将 Matcher 条件字符串列表渲染为人类可读的 lore 文本。
 * 只读操作，不扣资源。实际校验+扣资源由 Service/Manager 层负责。
 *
 * 用法：在 GUI setIcon 回调中调用 expandRequest() 展开 {request} 占位符。
 */
object MatcherDisplayRenderer {

    /**
     * 展开 lore 中的 {request} 占位符为条件文本列表
     *
     * @param lore 原始 lore 行列表
     * @param matchers Matcher 条件字符串列表（如 "@vault:500", "@item:material:OAK_LOG-16"）
     * @param player 用于判断条件满足状态和读取 lang
     * @return 展开后的 lore 列表
     */
    fun expandRequest(
        lore: List<String>,
        matchers: List<String>,
        player: Player
    ): List<String> {
        val idx = lore.indexOfFirst { it.contains("{request}") }
        if (idx == -1) return lore

        val lines = if (matchers.isEmpty()) {
            listOf(player.asLangText("request-display-none").colored())
        } else {
            val rendered = matchers.map { renderSingle(it, player) }
            val (met, unmet) = rendered.partition { isMetLine(it) }
            unmet + met
        }

        return lore.subList(0, idx) + lines + lore.subList(idx + 1, lore.size)
    }
    /** 判断渲染后的行是否为「已满足」（包含删除线格式码 §m） */
    private fun isMetLine(line: String): Boolean = line.contains("§m")

    private fun renderSingle(matcher: String, player: Player): String {
        val met = try {
            FunctionManager.getMatcherManager()
                .checkMatcherSingleTextWithoutAction(matcher, player, infoType.None)
        } catch (_: Exception) {
            false
        }
        val status = player.asLangText(
            if (met) "request-status-satisfy" else "request-status-not-satisfy"
        )
        val prefix = matcher.substringBefore(":").removePrefix("@")
        val body = matcher.substringAfter(":")

        val text = when (prefix) {
            "vault" -> renderTemplate(player, "request-display-vault",
                mapOf("value" to body))
            "item" -> renderItemMatcher(body, player)
            "permission" -> renderTemplate(player, "request-display-permission",
                mapOf("value" to body))
            "papi" -> renderPapiMatcher(body, player)
            else -> renderTemplate(player, "request-display-unknown",
                mapOf("raw" to matcher))
        }

        val formatKey = if (met) "request-format-satisfy" else "request-format-not-satisfy"
        return player.asLangText(formatKey)
            .replace("{status}", status)
            .replace("{text}", text)
            .colored()
    }

    private fun renderItemMatcher(body: String, player: Player): String {
        val lastDash = body.lastIndexOf('-')
        val (expr, amount) = if (lastDash > 0)
            body.substring(0, lastDash) to body.substring(lastDash + 1)
        else body to "1"

        val name = if (expr.startsWith("material:")) {
            val mat = expr.removePrefix("material:")
            ItemStack(Material.valueOf(mat)).getI18nName()
        } else expr

        return renderTemplate(player, "request-display-item",
            mapOf("name" to name, "amount" to amount))
    }

    private fun renderPapiMatcher(body: String, player: Player): String {
        val parts = body.trim().split(" ")
        if (parts.size == 3) {
            var (lhs, op, rhs) = parts
            op = resolveMap(player.asLangTextList("request-operator-map"), op) ?: op
            lhs = resolveMap(player.asLangTextList("request-placeholder-map"), lhs) ?: lhs
            return renderTemplate(player, "request-display-papi",
                mapOf("lhs" to lhs, "op" to op, "rhs" to rhs))
        }
        return renderTemplate(player, "request-display-unknown",
            mapOf("raw" to "@papi:$body"))
    }

    private fun renderTemplate(
        player: Player, langKey: String, vars: Map<String, String>
    ): String {
        var text = player.asLangText(langKey)
        vars.forEach { (k, v) -> text = text.replace("{$k}", v) }
        return text.colored()
    }

    private fun resolveMap(list: List<String>, key: String): String? =
        list.firstNotNullOfOrNull {
            val parts = it.split("->", limit = 2)

            if (parts.size == 2 && parts[0] == key) parts[1] else null
        }
}
