# TrapManager

- 模块 ID: `trap_manager`
- 层级: L1
- 依赖: database_manager, economy_manager
- 状态: 🔲 待构建

---

## 职责

管理陷阱的类型定义与地块部署数据。负责从配置加载所有陷阱种类定义（名称、惩罚效果类型、触发概率、消耗材料等），管理每个地块已部署的陷阱数据（类型、槽位），提供陷阱触发判定（概率计算）和惩罚效果执行（减速、扣金币、强制传送）。管理陷阱槽位数量与农场等级的关联查询。

## 事件

发布:
- `TrapTriggeredEvent` — 偷菜者触发陷阱时发布；数据: 触发者 Player, 农场主 UUID, TrapDefinition, 惩罚效果描述
- `TrapDeployedEvent` — 玩家成功部署一个陷阱时触发；数据: Player, Plot, DeployedTrap

## 交互

### → database_manager
陷阱管理调用数据库管理持久化陷阱部署数据。

### → economy_manager
陷阱管理调用经济管理执行扣金币类惩罚效果。

### 被调用
- ← admin_manager（详见 `modules/L2/admin_manager.md`）
- ← debug_tool（详见 `modules/L3/debug_tool.md`）
- ← steal_manager（详见 `modules/L2/steal_manager.md`）
- ← upgrade_manager（详见 `modules/L2/upgrade_manager.md`）

## 关联模型

- [TrapDefinition](../../foundation/model.md)
- [DeployedTrap](../../foundation/model.md)
