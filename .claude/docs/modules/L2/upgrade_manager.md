# UpgradeManager

- 模块 ID: `upgrade_manager`
- 层级: L2
- 依赖: farm_level_manager, economy_manager, plot_manager, trap_manager, player_data_manager
- 状态: 🔲 待构建

---

## 职责

管理农场升级的业务流程。提供升级GUI展示当前等级信息、下一级消耗与解锁内容预览。处理升级操作时的金币/材料校验与扣除、等级数据更新、地块物理边界扩展、功能解锁写入。同时管理陷阱部署GUI与部署流程：展示已解锁槽位与可用陷阱种类、校验槽位与消耗、写入部署数据。

## 事件

发布:
- `FarmUpgradedEvent` — 玩家成功升级农场后触发；数据: 玩家UUID、旧等级、新等级、解锁功能列表
- `TrapDeployedEvent` — 玩家成功部署陷阱后触发；数据: 玩家UUID、陷阱类型、槽位编号

## 交互

### → farm_level_manager
升级模块调用等级管理查询当前等级、校验升级条件、更新等级数据。
- 涉及事件: FarmLevelUpEvent

### → economy_manager
升级模块调用经济管理校验并扣除升级所需金币。

### → plot_manager
升级模块调用地块管理扩展地块物理边界、增加耕地面积。
- 涉及事件: PlotExpandedEvent

### → trap_manager
升级模块调用陷阱管理展示可用陷阱种类、校验槽位与消耗、写入部署数据。
- 涉及事件: TrapDeployedEvent

### → player_data_manager
升级模块调用玩家数据管理扣除背包材料、更新玩家功能解锁状态。

### → achievement_manager
升级模块发布FarmUpgradedEvent，成就模块监听后更新升级类成就进度。
- 涉及事件: FarmUpgradedEvent
