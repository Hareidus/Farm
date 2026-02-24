# StealRecordManager

- 模块 ID: `steal_record_manager`
- 层级: L1
- 依赖: database_manager
- 状态: 🔲 待构建

---

## 职责

管理偷菜行为的记录与冷却状态。负责记录每次偷菜日志（偷取者、被偷者、作物种类、数量、时间），管理玩家对特定农场主的偷菜冷却计时，追踪单次访问中玩家对某地块已偷取的数量以判断是否达到比例上限。提供偷菜记录的查询能力供通知和排行榜使用。

## 事件

发布:
- `StealRecordCreatedEvent` — 一条偷菜记录被写入后触发；数据: StealRecord（含偷取者、被偷者、作物种类、数量、时间）

## 交互

### → database_manager
偷菜记录管理调用数据库管理持久化偷菜日志与冷却状态。

### 被调用
- ← debug_tool（详见 `modules/L3/debug_tool.md`）
- ← steal_manager（详见 `modules/L2/steal_manager.md`）

## 关联模型

- [StealRecord](../../foundation/model.md)
- [StealCooldown](../../foundation/model.md)
