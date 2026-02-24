# FarmLevelManager

- 模块 ID: `farm_level_manager`
- 层级: L1
- 依赖: database_manager
- 状态: 🔲 待构建

---

## 职责

管理农场等级体系的定义与玩家农场等级数据。负责从配置加载每级的升级消耗（金币/材料）、面积增量、解锁功能项（陷阱槽位数、装饰位数、防护等级、是否解锁自动收割）。提供玩家当前等级查询、升级条件校验、等级数据更新能力。管理防护等级对应的被偷比例上限映射。

## 事件

发布:
- `FarmLevelUpEvent` — 玩家农场等级提升时触发；数据: Player, 旧等级, 新等级, 新解锁功能列表

## 交互

### → database_manager
等级管理调用数据库管理持久化玩家农场等级与解锁功能数据。

### 被调用
- ← admin_manager（详见 `modules/L2/admin_manager.md`）
- ← debug_tool（详见 `modules/L3/debug_tool.md`）
- ← harvest_manager（详见 `modules/L2/harvest_manager.md`）
- ← placeholder_expansion（详见 `modules/L3/placeholder_expansion.md`）
- ← steal_manager（详见 `modules/L2/steal_manager.md`）
- ← upgrade_manager（详见 `modules/L2/upgrade_manager.md`）

## 关联模型

- [FarmLevelDefinition](../../foundation/model.md)
- [PlayerFarmLevel](../../foundation/model.md)
