# FarmTeleportManager

- 模块 ID: `farm_teleport_manager`
- 层级: L2
- 依赖: plot_manager, crop_manager, player_data_manager
- 状态: 🔲 待构建

---

## 职责

管理农场世界的传送逻辑。处理主世界传送点的交互检测，协调新玩家首次进入时的地块分配与新手礼包发放，老玩家回到自己农场时触发作物生长刷新与离线通知推送，以及玩家传送到他人农场时的权限校验与目标地块加载。统一管理玩家当前所处农场的状态标记（自己的/他人的）。

## 事件

发布:
- `FarmPlotAssignedEvent` — 新玩家首次被分配地块后触发；数据: 玩家UUID、地块坐标、地块ID
- `PlayerEnterOwnFarmEvent` — 玩家传送回自己农场后触发；数据: 玩家UUID、地块ID
- `PlayerEnterOtherFarmEvent` — 玩家传送到他人农场后触发；数据: 访问者UUID、农场主UUID、地块ID
- `PlayerLeaveFarmEvent` — 玩家离开农场世界时触发；数据: 玩家UUID、离开的地块ID

监听:
- `PlayerInteractEvent` — 玩家右键交互主世界传送点方块/NPC时触发；数据: 交互玩家、交互位置
- `PlayerJoinEvent` — 玩家上线时检查是否有未读被偷通知；数据: 玩家对象

## 交互

### → plot_manager
传送模块调用地块管理查询玩家地块归属、分配新地块、加载目标地块区域。
- 涉及事件: PlotAllocatedEvent, FarmPlotAssignedEvent

### → crop_manager
玩家回到农场时，传送模块调用作物管理刷新离线生长进度并更新方块材质。
- 涉及事件: CropGrowthUpdatedEvent

### → player_data_manager
传送模块调用玩家数据管理加载数据、推送离线通知、初始化新玩家记录。
- 涉及事件: PlayerDataLoadedEvent

### → harvest_manager
玩家回到自己农场时发布PlayerEnterOwnFarmEvent，收割模块监听后触发自动收割检测。
- 涉及事件: PlayerEnterOwnFarmEvent

### → steal_manager
玩家进入他人农场时发布PlayerEnterOtherFarmEvent，偷菜模块监听后初始化偷菜会话状态。
- 涉及事件: PlayerEnterOtherFarmEvent

### → friend_interaction_manager
玩家进入他人农场时发布PlayerEnterOtherFarmEvent，好友交互模块监听后判断是否启用浇水功能。
- 涉及事件: PlayerEnterOtherFarmEvent
