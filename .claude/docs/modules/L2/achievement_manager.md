# AchievementManager

- 模块 ID: `achievement_manager`
- 层级: L2
- 依赖: player_data_manager, database_manager, economy_manager
- 状态: 🔲 待构建

---

## 职责

管理成就的定义、进度追踪与奖励发放。从配置加载所有成就的触发条件、阈值与奖励内容（金币、物品、称号）。监听各业务事件（种植、收获、偷菜、被偷、触发陷阱等），更新玩家成就进度，判定是否达成阈值，达成后标记完成状态、发放奖励、解锁称号前缀并通知玩家。

## 事件

发布:
- `AchievementUnlockedEvent` — 玩家达成成就后触发；数据: 玩家UUID、成就ID、成就名称、奖励内容

监听:
- `CropPlantedEvent` — 玩家种植作物时检查种植类成就进度；数据: 玩家UUID、作物种类
- `CropHarvestedEvent` — 玩家收获作物时检查收获类成就进度；数据: 玩家UUID、作物种类、产出数量
- `CropStolenEvent` — 偷菜成功时检查偷菜类成就进度；数据: 偷取者UUID、被偷者UUID、数量
- `TrapTriggeredEvent` — 触发陷阱时检查陷阱类成就进度；数据: 偷取者UUID、陷阱类型
- `FarmUpgradedEvent` — 农场升级时检查升级类成就进度；数据: 玩家UUID、新等级
- `CropWateredEvent` — 浇水时检查社交类成就进度；数据: 浇水者UUID

## 交互

### → player_data_manager
成就模块调用玩家数据管理查询成就进度、写入完成状态。

### → database_manager
成就模块调用数据库管理持久化成就进度与完成记录。

### → economy_manager
成就达成时，成就模块调用经济管理发放金币奖励。

### 被调用
- ← friend_interaction_manager（详见 `modules/L2/friend_interaction_manager.md`）
- ← harvest_manager（详见 `modules/L2/harvest_manager.md`）
- ← placeholder_expansion（详见 `modules/L3/placeholder_expansion.md`）
- ← shop_manager（详见 `modules/L2/shop_manager.md`）
- ← steal_manager（详见 `modules/L2/steal_manager.md`）
- ← upgrade_manager（详见 `modules/L2/upgrade_manager.md`）

## 关联模型

- [AchievementDefinition](../../foundation/model.md)
- [PlayerAchievement](../../foundation/model.md)
