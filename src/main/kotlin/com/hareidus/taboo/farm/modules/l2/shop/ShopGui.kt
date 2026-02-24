package com.hareidus.taboo.farm.modules.l2.shop

import EasyLib.EasyGui.EasyGuiBuilder.IPageableGuiBuilder
import EasyLib.EasyGui.EasyGuiConfig.GuiConfig.GuiConfig
import com.hareidus.taboo.farm.foundation.gui.GuiConfigManager
import com.hareidus.taboo.farm.foundation.gui.GuiNavigator
import com.hareidus.taboo.farm.modules.l1.crop.CropManager
import com.hareidus.taboo.farm.modules.l1.economy.EconomyManager
import org.bukkit.entity.Player
import taboolib.library.xseries.XMaterial
import taboolib.module.chat.colored
import taboolib.platform.util.buildItem
import taboolib.platform.util.sendLang

/**
 * 商店收购站 GUI（分页）
 *
 * 展示玩家背包中可出售的作物列表，点击即出售全部该类型作物。
 *
 * Structure 约束：
 * - 继承 IPageableGuiBuilder<SellableCropEntry>
 * - 实现 open(), setupElement(), elementGenerateItem(), mapIconsToFunctions()
 * - 禁止使用 buildMenu、手动分页、直接数据库访问
 */
class ShopGui(
    config: GuiConfig,
    player: Player
) : IPageableGuiBuilder<ShopGui.SellableCropEntry>(config, player) {

    /** 可出售作物条目 */
    data class SellableCropEntry(
        val cropTypeId: String,
        val cropName: String,
        val amount: Int,
        val unitPrice: Double
    )

    override fun open() {
        // [必须] 先推入导航栈
        GuiNavigator.push(thisPlayer, "shop") { ShopGui(config, thisPlayer).open() }

        // [必须] 再构建并打开，注册元素点击事件
        buildAndOpen {
            chestImpl.onClick { event, element ->
                event.isCancelled = true
                ShopManager.sellCrops(thisPlayer, element.cropTypeId, element.amount)
                // 重新打开 GUI 刷新数据
                ShopGui(config, thisPlayer).open()
            }
        }
    }

    override fun setupElement() {
        chestImpl.elements {
            val sellableMap = ShopManager.getSellableCrops(thisPlayer)
            sellableMap.mapNotNull { (cropTypeId, amount) ->
                val cropDef = CropManager.getCropDefinition(cropTypeId) ?: return@mapNotNull null
                val unitPrice = EconomyManager.getCropPrice(cropTypeId) ?: return@mapNotNull null
                SellableCropEntry(cropTypeId, cropDef.name, amount, unitPrice)
            }
        }
    }

    override fun elementGenerateItem() {
        chestImpl.onGenerate { player, element, index, slot ->
            val totalPrice = element.unitPrice * element.amount
            val harvestId = CropManager.getCropDefinition(element.cropTypeId)?.harvestItemId ?: "WHEAT"
            val material = try {
                XMaterial.matchXMaterial(harvestId).orElse(XMaterial.WHEAT)
            } catch (_: Exception) {
                XMaterial.WHEAT
            }
            buildItem(material) {
                name = "&f${element.cropName}".colored()
                lore.addAll(listOf(
                    "&7背包数量: &f${element.amount}".colored(),
                    "&7单价: &f${String.format("%.2f", element.unitPrice)} &7金币".colored(),
                    "&7总价: &f${String.format("%.2f", totalPrice)} &7金币".colored(),
                    "",
                    "&e点击全部出售".colored()
                ))
            }
        }
    }

    override fun mapIconsToFunctions() {
        mapIconsToFunctionWay { key, function ->
            when (function) {
                "item" -> elementSlotByKey(key)
                "next" -> setNextIcon(key)
                "last" -> setLastIcon(key)
                "back" -> setBackIcon(key)
                else -> setDefaultIcon(key)
            }
        }
    }

    // ==================== 图标处理方法 ====================

    private fun setBackIcon(key: Char) {
        setIcon(key) { k, itemStack ->
            getCustomChestImpl().set(k, itemStack) {
                isCancelled = true
                thisPlayer.closeInventory()
                if (!GuiNavigator.back(thisPlayer)) {
                    thisPlayer.sendLang("gui-back")
                }
            }
        }
    }

    companion object {
        fun open(player: Player) {
            val config = GuiConfigManager.shopGuiConfig
            if (config != null) {
                ShopGui(config, player).open()
            } else {
                player.sendLang("gui-config-error")
            }
        }
    }
}
