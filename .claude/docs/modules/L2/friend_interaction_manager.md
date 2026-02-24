# FriendInteractionManager

- 模块 ID: `friend_interaction_manager`
- 层级: L2
- 依赖: social_manager, crop_manager, plot_manager
- 状态: 🔲 待构建

---

## 职责

管理好友关系带来的业务交互效果。处理好友浇水加速逻辑：校验好友关系、浇水冷却检查、作物时间戳偏移、生长阶段刷新、冷却记录写入。提供好友/仇人管理GUI的构建与交互处理（查看列表、添加好友、确认请求、传送到好友/仇人农场）。

## 事件

发布:
- `CropWateredEvent` — 好友成功为作物浇水后触发；数据: 浇水者UUID、农场主UUID、作物种类、作物位置、新生长阶段

监听:
- `PlayerInteractEvent` — 玩家在好友农场右键未成熟作物进行浇水时触发；数据: 玩家对象、交互方块位置
- `PlayerEnterOtherFarmEvent` — 进入他人农场时判断是否为好友以启用浇水功能；数据: 访问者UUID、农场主UUID

## 交互

### → social_manager
好友交互模块调用社交管理校验好友关系、处理好友请求发送与确认、查询仇人列表。
- 涉及事件: FriendAddedEvent, FriendRemovedEvent

### → crop_manager
好友浇水时，交互模块调用作物管理执行时间戳偏移与生长阶段刷新。
- 涉及事件: CropGrowthUpdatedEvent

### → plot_manager
好友交互模块调用地块管理校验浇水位置归属及查询好友/仇人地块用于传送。

### → achievement_manager
好友交互模块发布CropWateredEvent，成就模块监听后更新社交类成就进度。
- 涉及事件: CropWateredEvent

### 被调用
- ← farm_teleport_manager（详见 `modules/L2/farm_teleport_manager.md`）

## 关联模型

- [WaterCooldown](../../foundation/model.md)
