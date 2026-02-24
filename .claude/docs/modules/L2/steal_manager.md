# StealManager

- 模块 ID: `steal_manager`
- 层级: L2
- 依赖: crop_manager, plot_manager, steal_record_manager, trap_manager, social_manager, player_data_manager, farm_level_manager, economy_manager
- 状态: 🔲 待构建

---

## 职责

管理偷菜的核心业务流程。当玩家在他人农场右键成熟作物时，协调冷却检查、比例上限计算（结合农场防护等级和仇人加成）、陷阱触发判定、作物产出发放、偷菜记录写入、双方统计更新、仇人自动标记、以及被偷通知的实时推送或离线入队。统一处理偷菜被冷却阻止、达到比例上限阻止、触发陷阱惩罚等分支流程。

## 事件

发布:
- `CropStolenEvent` — 玩家成功偷取一株作物后触发；数据: 偷取者UUID、被偷者UUID、作物种类、产出数量
- `StealCooldownStartedEvent` — 玩家对某农场主的偷菜达到上限开始冷却时触发；数据: 偷取者UUID、被偷者UUID、冷却结束时间
- `TrapTriggeredEvent` — 偷菜者触发陷阱后触发；数据: 偷取者UUID、农场主UUID、陷阱类型、惩罚效果

监听:
- `PlayerInteractEvent` — 玩家在他人农场右键点击成熟作物时触发；数据: 玩家对象、交互方块位置
- `PlayerEnterOtherFarmEvent` — 玩家进入他人农场时初始化偷菜会话状态；数据: 访问者UUID、农场主UUID

## 交互

### → crop_manager
偷菜模块调用作物管理校验成熟度、计算产出、移除被偷作物方块与数据。
- 涉及事件: CropRemovedEvent

### → plot_manager
偷菜模块调用地块管理查询交互位置所属农场主及地块信息。

### → steal_record_manager
偷菜模块调用记录管理检查冷却状态、写入偷菜日志、追踪本次已偷数量。
- 涉及事件: StealRecordCreatedEvent

### → trap_manager
偷菜模块调用陷阱管理查询地块已部署陷阱、执行概率判定与惩罚效果。
- 涉及事件: TrapTriggeredEvent

### → social_manager
偷菜模块调用社交管理查询仇人关系计算加成，偷菜成功后调用自动标记仇人。
- 涉及事件: EnemyMarkedEvent

### → player_data_manager
偷菜模块调用玩家数据管理累加双方统计（偷菜量/被偷量/陷阱次数），写入离线通知队列。
- 涉及事件: PlayerStatisticUpdateEvent

### → farm_level_manager
偷菜模块调用等级管理查询农场防护等级以计算被偷比例上限。

### → economy_manager
偷菜触发扣金币类陷阱时，偷菜模块调用经济管理执行金币扣除。

### → achievement_manager
偷菜模块发布CropStolenEvent和TrapTriggeredEvent，成就模块监听后更新偷菜类和陷阱类成就进度。
- 涉及事件: CropStolenEvent, TrapTriggeredEvent

### → leaderboard_manager
偷菜模块发布CropStolenEvent，排行榜模块监听后标记偷菜和被偷维度缓存需刷新。
- 涉及事件: CropStolenEvent

### 被调用
- ← farm_teleport_manager（详见 `modules/L2/farm_teleport_manager.md`）
