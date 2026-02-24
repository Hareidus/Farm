# ShopManager

- 模块 ID: `shop_manager`
- 层级: L2
- 依赖: economy_manager, player_data_manager, crop_manager
- 状态: 🔲 待构建

---

## 职责

管理NPC收购站的交互与出售流程。处理玩家右键收购NPC时打开收购站GUI，展示玩家背包中可出售的作物及其配置单价，处理出售操作时的金币发放（通过Vault）、物品扣除、金币收入统计累加。

## 事件

发布:
- `CropSoldEvent` — 玩家成功出售作物后触发；数据: 玩家UUID、作物种类、数量、总金额

监听:
- `PlayerInteractEntityEvent` — 玩家右键点击收购NPC时触发；数据: 玩家对象、NPC实体

## 交互

### → economy_manager
商店模块调用经济管理查询作物单价、通过Vault向玩家发放出售金币。

### → player_data_manager
商店模块调用玩家数据管理累加金币收入统计。
- 涉及事件: PlayerStatisticUpdateEvent

### → crop_manager
商店模块调用作物管理获取作物定义信息以识别背包中可出售作物。

### → achievement_manager
商店模块发布CropSoldEvent，成就模块可监听用于金币收入类成就检查。
- 涉及事件: CropSoldEvent

### → leaderboard_manager
商店模块发布CropSoldEvent，排行榜模块监听后标记金币收入维度缓存需刷新。
- 涉及事件: CropSoldEvent
