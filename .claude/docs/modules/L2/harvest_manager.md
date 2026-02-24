# HarvestManager

- 模块 ID: `harvest_manager`
- 层级: L2
- 依赖: crop_manager, plot_manager, player_data_manager, farm_level_manager
- 状态: 🔲 待构建

---

## 职责

管理玩家在自己地块上的作物种植与收割行为。处理种植时的地块归属校验、空位检测、种子扣除与作物数据创建；处理收割时的成熟度校验、产出计算、物品发放与作物数据清除。同时处理骨粉加速逻辑（时间戳偏移与阶段刷新）。负责累加玩家收获总量统计。管理最高级农场的自动收割逻辑，在地块加载时将成熟作物产出存入农场仓库。

## 事件

发布:
- `CropPlantedEvent` — 玩家成功种植一株作物后触发；数据: 玩家UUID、作物种类、种植位置
- `CropHarvestedEvent` — 玩家成功收割一株作物后触发；数据: 玩家UUID、作物种类、产出数量、是否自动收割
- `CropBonemeledEvent` — 玩家对作物使用骨粉加速后触发；数据: 玩家UUID、作物种类、作物位置、新生长阶段

监听:
- `PlayerInteractEvent` — 玩家右键点击自己地块内的耕地（种植）或成熟作物（收割）或未成熟作物（骨粉）时触发；数据: 玩家对象、交互方块位置、手持物品
- `PlayerEnterOwnFarmEvent` — 玩家回到自己农场时触发自动收割检测；数据: 玩家UUID、地块ID

## 交互

### → crop_manager
收割模块调用作物管理执行种植写入、成熟度校验、收割移除、骨粉时间戳偏移与阶段刷新。
- 涉及事件: CropPlantedEvent, CropHarvestedEvent, CropGrowthUpdatedEvent, CropRemovedEvent

### → plot_manager
收割模块调用地块管理校验交互位置是否在玩家自己地块内、检测空位。

### → player_data_manager
收割模块调用玩家数据管理累加收获总量统计。
- 涉及事件: PlayerStatisticUpdateEvent

### → farm_level_manager
收割模块调用等级管理查询是否解锁自动收割功能。

### → achievement_manager
收割模块发布CropPlantedEvent和CropHarvestedEvent，成就模块监听后更新种植类和收获类成就进度。
- 涉及事件: CropPlantedEvent, CropHarvestedEvent

### → leaderboard_manager
收割模块发布CropHarvestedEvent，排行榜模块监听后标记收获维度缓存需刷新。
- 涉及事件: CropHarvestedEvent

### 被调用
- ← farm_teleport_manager（详见 `modules/L2/farm_teleport_manager.md`）

## 关联模型

- [FarmStorage](../../foundation/model.md)
