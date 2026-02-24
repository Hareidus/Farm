# Farm 构建指南

> 严格按模块划分和依赖关系开发。不得擅自合并模块或跳过依赖。

包名: `com.hareidus.taboo.farm`

## 模块清单

| 模块 ID | 名称 | 层级 | 依赖 |
|---------|------|------|------|
| database_manager | DatabaseManager | L1 | — |
| player_data_manager | PlayerDataManager | L1 | database_manager |
| plot_manager | PlotManager | L1 | database_manager |
| crop_manager | CropManager | L1 | database_manager, plot_manager |
| economy_manager | EconomyManager | L1 | — |
| social_manager | SocialManager | L1 | database_manager |
| steal_record_manager | StealRecordManager | L1 | database_manager |
| trap_manager | TrapManager | L1 | database_manager, economy_manager |
| farm_level_manager | FarmLevelManager | L1 | database_manager |
| farm_teleport_manager | FarmTeleportManager | L2 | plot_manager, crop_manager, player_data_manager |
| harvest_manager | HarvestManager | L2 | crop_manager, plot_manager, player_data_manager, farm_level_manager |
| steal_manager | StealManager | L2 | crop_manager, plot_manager, steal_record_manager, trap_manager, social_manager, player_data_manager, farm_level_manager, economy_manager |
| shop_manager | ShopManager | L2 | economy_manager, player_data_manager, crop_manager |
| friend_interaction_manager | FriendInteractionManager | L2 | social_manager, crop_manager, plot_manager |
| upgrade_manager | UpgradeManager | L2 | farm_level_manager, economy_manager, plot_manager, trap_manager, player_data_manager |
| leaderboard_manager | LeaderboardManager | L2 | player_data_manager, database_manager |
| achievement_manager | AchievementManager | L2 | player_data_manager, database_manager, economy_manager |
| admin_manager | AdminManager | L2 | plot_manager, crop_manager, trap_manager, farm_level_manager, player_data_manager |
| placeholder_expansion | PlaceholderExpansion | L3 | player_data_manager, farm_level_manager, leaderboard_manager, achievement_manager, plot_manager |
| migration_tool | MigrationTool | L3 | database_manager, plot_manager, crop_manager |
| debug_tool | DebugTool | L3 | player_data_manager, plot_manager, crop_manager, steal_record_manager, social_manager, trap_manager, farm_level_manager |

## 依赖图

```
  database_manager
  player_data_manager --> database_manager
  plot_manager --> database_manager
  crop_manager --> database_manager
  crop_manager --> plot_manager
  economy_manager
  social_manager --> database_manager
  steal_record_manager --> database_manager
  trap_manager --> database_manager
  trap_manager --> economy_manager
  farm_level_manager --> database_manager
  farm_teleport_manager --> plot_manager
  farm_teleport_manager --> crop_manager
  farm_teleport_manager --> player_data_manager
  harvest_manager --> crop_manager
  harvest_manager --> plot_manager
  harvest_manager --> player_data_manager
  harvest_manager --> farm_level_manager
  steal_manager --> crop_manager
  steal_manager --> plot_manager
  steal_manager --> steal_record_manager
  steal_manager --> trap_manager
  steal_manager --> social_manager
  steal_manager --> player_data_manager
  steal_manager --> farm_level_manager
  steal_manager --> economy_manager
  shop_manager --> economy_manager
  shop_manager --> player_data_manager
  shop_manager --> crop_manager
  friend_interaction_manager --> social_manager
  friend_interaction_manager --> crop_manager
  friend_interaction_manager --> plot_manager
  upgrade_manager --> farm_level_manager
  upgrade_manager --> economy_manager
  upgrade_manager --> plot_manager
  upgrade_manager --> trap_manager
  upgrade_manager --> player_data_manager
  leaderboard_manager --> player_data_manager
  leaderboard_manager --> database_manager
  achievement_manager --> player_data_manager
  achievement_manager --> database_manager
  achievement_manager --> economy_manager
  admin_manager --> plot_manager
  admin_manager --> crop_manager
  admin_manager --> trap_manager
  admin_manager --> farm_level_manager
  admin_manager --> player_data_manager
  placeholder_expansion --> player_data_manager
  placeholder_expansion --> farm_level_manager
  placeholder_expansion --> leaderboard_manager
  placeholder_expansion --> achievement_manager
  placeholder_expansion --> plot_manager
  migration_tool --> database_manager
  migration_tool --> plot_manager
  migration_tool --> crop_manager
  debug_tool --> player_data_manager
  debug_tool --> plot_manager
  debug_tool --> crop_manager
  debug_tool --> steal_record_manager
  debug_tool --> social_manager
  debug_tool --> trap_manager
  debug_tool --> farm_level_manager
```

## Phase 构建顺序

- Phase 0: 模型层（共享数据模型定义） → （模型定义）
- Phase 1: L1 基础模块（零依赖） → database_manager, economy_manager
- Phase 2: L1 基础模块（深度 1） → farm_level_manager, player_data_manager, plot_manager, social_manager, steal_record_manager, trap_manager
- Phase 3: L1 基础模块（深度 2） → crop_manager
- Phase 4: L2 业务模块（深度 0） → achievement_manager, admin_manager, farm_teleport_manager, friend_interaction_manager, harvest_manager, leaderboard_manager, shop_manager, steal_manager, upgrade_manager
- Phase 5: L3 边缘模块 → debug_tool, migration_tool, placeholder_expansion

## 行为链路（功能验收标尺）

> 以下链路是用户审批通过的玩家行为流程。每条链路的每个节点必须可走通。
> 模块构建完成后，沿链路逐节点验证：触发 → 响应 → 数据流转 → 最终结果。

1. **新玩家首次进入农场并获得地块** (`new-player-first-farm-entry`)
   玩家在主世界找到农场传送点（NPC/交互方块） → 玩家右键交互传送点 → 系统检测玩家是否拥有农场地块 → 系统在农场世界网格中计算下一个可用地块坐标 → 系统生成地块地面和边界 → 系统将地块坐标绑定到玩家数据 → 系统将玩家传送至新地块中心 → 系统发放新手种子礼包到玩家背包 → 系统发送欢迎提示消息

2. **老玩家传送回自己的农场** (`existing-player-teleport-own-farm`)
   玩家右键交互主世界传送点 → 系统检测玩家已拥有地块 → 系统加载玩家地块区域 → 系统根据当前时间戳与每株作物种植时间戳的差值计算离线生长进度 → 系统更新地块内所有作物的生长阶段（替换方块/头颅材质） → 系统将玩家传送至地块中心 → 系统检查是否有未读的被偷通知并推送

3. **玩家在自己地块种植作物** (`plant-crop-on-own-plot`)
   玩家手持种子物品 → 玩家右键点击地块内的耕地方块 → 系统校验点击位置是否在玩家自己的地块边界内 → 系统校验该位置是否为空（未种植） → 系统识别种子类型（原版/自定义） → 系统在该位置放置作物初始阶段方块（原版作物用原版方块，自定义作物放置第一阶段头颅） → 系统记录作物数据：种类、位置、种植时间戳 → 系统扣除玩家背包中的种子物品

4. **作物自然生长并被玩家收割** (`crop-growth-and-harvest`)
   玩家进入地块区域触发加载 → 系统遍历地块内所有作物，根据当前时间戳减去种植时间戳计算已过时间 → 系统将已过时间映射到作物配置的生长阶段 → 系统更新未成熟作物的方块/头颅材质到对应阶段 → 玩家右键点击一株已成熟的作物 → 系统校验该作物确实处于成熟阶段 → 系统根据作物配置计算产出数量（随机范围） → 系统将产出物品放入玩家背包 → 系统移除该位置的作物方块，恢复为耕地 → 系统删除该作物的数据记录 → 系统累加玩家收获总量统计

5. **玩家使用骨粉加速自己作物生长** (`bonemeal-accelerate-growth`)
   玩家手持骨粉右键点击自己地块内未成熟的作物 → 系统校验该作物未成熟 → 系统将该作物的种植时间戳向前偏移一个配置的加速时长 → 系统重新计算生长阶段 → 系统更新作物方块/头颅材质到新阶段 → 系统扣除一个骨粉 → 系统播放骨粉粒子效果

6. **玩家向NPC出售作物获得金币** (`sell-crops-to-npc`)
   玩家在农场世界右键点击收购NPC → 系统打开收购站GUI，展示玩家背包中可出售的作物及其单价 → 玩家点击GUI中要出售的作物 → 系统计算出售总价（数量 × 配置单价） → 系统通过Vault接口向玩家账户增加金币 → 系统从玩家背包扣除对应作物物品 → 系统累加玩家金币收入统计 → 系统发送出售成功消息

7. **玩家传送到其他玩家的农场** (`teleport-to-other-farm`)
   玩家执行命令或通过导航GUI选择目标玩家 → 系统校验目标玩家拥有农场地块 → 系统校验玩家拥有 stealfarm.teleport 权限 → 系统加载目标地块区域 → 系统计算目标地块内作物的当前生长状态 → 系统将玩家传送至目标地块 → 系统标记玩家当前处于他人农场

8. **偷菜成功流程** (`steal-crop-success`)
   玩家在他人农场右键点击一株成熟作物 → 系统校验玩家拥有 stealfarm.steal 权限 → 系统检查玩家对该农场主的偷菜冷却是否已过（默认4小时） → 系统计算该地块当前成熟作物总数 → 系统计算本次可偷比例上限（基础比例 - 农场防护等级降低量 + 仇人加成） → 系统计算玩家本次已偷数量是否未达上限 → 系统将该作物产出放入玩家背包 → 系统移除该位置作物方块并恢复耕地 → 系统记录偷菜日志（偷取者、被偷者、作物种类、数量、时间） → 系统累加偷取者的偷菜总量统计 → 系统累加被偷者的被偷总量统计 → 系统自动将偷取者标记为被偷者的仇人 → 若被偷者在线则实时推送通知，否则存入离线通知队列

9. **偷菜被冷却时间阻止** (`steal-crop-cooldown-blocked`)
   玩家在他人农场右键点击一株成熟作物 → 系统检查偷菜冷却时间 → 系统发现冷却未结束 → 系统向玩家发送冷却剩余时间提示 → 操作被拒绝，作物不受影响

10. **偷菜达到比例上限被阻止** (`steal-crop-ratio-limit-reached`)
   玩家在他人农场右键点击一株成熟作物 → 系统校验冷却已过 → 系统计算本次已偷数量已达到比例上限 → 系统向玩家发送已达偷取上限提示 → 系统开始对该农场主的偷菜冷却计时 → 操作被拒绝

11. **偷菜触发陷阱受到惩罚** (`steal-trigger-trap`)
   玩家在他人农场右键偷取成熟作物 → 系统判定该地块存在已部署的陷阱 → 系统根据陷阱触发概率进行判定 → 陷阱触发成功 → 系统根据陷阱类型施加惩罚效果（减速/扣金币/强制传送回自己农场） → 系统向偷菜者发送触发陷阱提示 → 系统向农场主记录陷阱触发日志 → 系统累加偷菜者的触发陷阱次数统计

12. **添加好友并为好友农场浇水加速** (`add-friend-and-water-crops`)
   玩家A执行添加好友命令/GUI操作指定玩家B → 系统向玩家B发送好友请求 → 玩家B确认接受好友请求 → 系统在双方数据中互相写入好友关系 → 玩家A传送到玩家B的农场 → 玩家A右键点击玩家B地块内未成熟的作物进行浇水 → 系统校验玩家A与玩家B是好友关系 → 系统校验浇水冷却是否已过 → 系统将该作物种植时间戳向前偏移配置的浇水加速时长 → 系统更新作物生长阶段和方块材质 → 系统记录浇水冷却开始时间 → 系统向双方发送浇水成功提示

13. **查看仇人列表并复仇偷菜获得加成** (`view-enemy-list-and-revenge`)
   玩家打开好友/仇人管理GUI → 系统展示仇人列表（自动标记的偷过自己的玩家） → 玩家选择一个仇人并点击传送到其农场 → 系统将玩家传送至仇人农场 → 玩家右键偷取仇人农场的成熟作物 → 系统检测目标为仇人关系 → 系统应用仇人偷取比例加成（配置值） → 偷取成功，玩家获得加成后的偷取上限

14. **玩家升级农场地块** (`upgrade-farm-plot`)
   玩家打开农场升级GUI → 系统展示当前等级、下一级所需金币/材料、解锁内容预览 → 玩家点击升级按钮 → 系统校验玩家金币和材料是否满足要求 → 系统通过Vault扣除金币并扣除背包材料 → 系统更新玩家农场等级数据 → 系统扩展地块物理边界（增加耕地面积） → 系统解锁对应功能（陷阱槽位/装饰位/防护等级提升/自动收割） → 系统发送升级成功消息并展示新解锁内容

15. **玩家在农场部署陷阱** (`deploy-trap-on-farm`)
   玩家打开陷阱配置GUI → 系统展示已解锁的陷阱槽位和可用陷阱种类 → 玩家选择一种陷阱类型 → 玩家选择部署到某个空闲槽位 → 系统校验玩家是否有足够的陷阱槽位（由农场等级决定） → 系统校验玩家是否拥有部署所需的材料/金币 → 系统扣除消耗并将陷阱数据写入地块配置 → 系统发送陷阱部署成功提示

16. **最高级农场自动收割作物** (`auto-harvest-max-level`)
   系统检测玩家农场等级已解锁自动收割功能 → 玩家地块被加载时系统遍历所有作物 → 系统发现有作物根据时间戳计算已成熟 → 系统自动计算产出数量 → 系统将产出存入农场仓库（而非玩家背包） → 系统移除成熟作物方块并恢复耕地 → 系统累加玩家收获总量统计

17. **玩家查看排行榜** (`view-leaderboard`)
   玩家执行排行榜命令或点击GUI入口 → 系统打开排行榜分类GUI（种植之王/偷菜大盗/农场富翁/铁壁农场） → 玩家选择一个排行榜类别 → 系统从数据库查询对应维度的排名数据 → 系统渲染分页排行榜GUI展示排名、玩家名、数值 → 玩家可翻页浏览

18. **玩家达成成就并解锁称号奖励** (`unlock-achievement-and-title`)
   玩家完成某个行为（种植/收获/偷菜/被偷/触发陷阱/连续登录收菜） → 系统事件触发后检查该行为关联的成就条件 → 系统查询玩家当前成就进度数据 → 系统判定进度达到成就阈值 → 系统标记该成就为已完成 → 系统发放成就奖励（金币/稀有种子/装饰物品） → 系统解锁对应称号前缀 → 系统向玩家发送成就达成通知（标题/音效）

19. **离线玩家上线后收到被偷通知** (`offline-steal-notification`)
   玩家A离线期间被玩家B偷菜 → 系统将偷菜记录写入玩家A的离线通知队列 → 玩家A上线 → 系统检测玩家A有未读偷菜通知 → 系统向玩家A推送被偷详情（谁偷了什么、数量、时间） → 系统标记通知为已读

20. **管理员重置玩家农场** (`admin-reset-farm`)
   管理员执行重置农场命令并指定目标玩家 → 系统校验执行者拥有 stealfarm.admin 权限 → 系统清除目标玩家的农场等级、作物数据、陷阱配置、装饰配置 → 系统将地块区域方块恢复为初始状态 → 系统重置地块大小为初始值 → 系统发送重置完成确认消息

## 架构审计

- ⚠️ 层级违规: harvest_manager 和 crop_manager 都声明发布 CropPlantedEvent 和 CropHarvestedEvent。根据职责划分，harvest_manager 是业务编排层（L2），crop_manager 是数据管理层（L1）。同一事件不应由两个模块各自声明发布，需明确唯一发布源（应由 harvest_manager 发布，或由 crop_manager 在被调用时发布，但不能两者都声明）
- ⚠️ 层级违规: steal_manager 和 trap_manager 都声明发布 TrapTriggeredEvent。steal_manager 的职责描述说它协调陷阱触发判定，而 trap_manager 的职责说它提供陷阱触发判定和惩罚效果执行。需明确该事件的唯一发布者
- ⚠️ 层级违规: upgrade_manager 和 trap_manager 都声明发布 TrapDeployedEvent。upgrade_manager 的职责包含陷阱部署流程，trap_manager 的职责也包含管理部署数据。同一部署事件不应有两个发布源
- ⚠️ 层级违规: upgrade_manager 发布 FarmUpgradedEvent，而 farm_level_manager 发布 FarmLevelUpEvent，两者语义高度重叠（农场升级成功）。achievement_manager 只监听 FarmUpgradedEvent，FarmLevelUpEvent 无人监听。需明确升级成功事件是否应统一为一个
- ⚠️ 层级违规: trap_manager 的职责声明它执行惩罚效果（减速、扣金币、强制传送），其中强制传送回自己农场需要查询玩家地块位置（plot_manager）并执行传送，但 trap_manager 的依赖中没有 plot_manager。行为链 steal-trigger-trap 明确要求强制传送回自己农场作为惩罚效果
- ⚠️ 未闭合事件: 事件 'PlayerDataLoadedEvent'（由 player_data_manager 发布）无任何模块监听
- ⚠️ 未闭合事件: 事件 'PlayerStatisticUpdateEvent'（由 player_data_manager 发布）无任何模块监听
- ⚠️ 未闭合事件: 事件 'PlotAllocatedEvent'（由 plot_manager 发布）无任何模块监听
- ⚠️ 未闭合事件: 事件 'PlotExpandedEvent'（由 plot_manager 发布）无任何模块监听
- ⚠️ 未闭合事件: 事件 'CropGrowthUpdatedEvent'（由 crop_manager 发布）无任何模块监听
- ⚠️ 未闭合事件: 事件 'CropRemovedEvent'（由 crop_manager 发布）无任何模块监听
- ⚠️ 未闭合事件: 事件 'FriendAddedEvent'（由 social_manager 发布）无任何模块监听
- ⚠️ 未闭合事件: 事件 'FriendRemovedEvent'（由 social_manager 发布）无任何模块监听
- ⚠️ 未闭合事件: 事件 'EnemyMarkedEvent'（由 social_manager 发布）无任何模块监听
- ⚠️ 未闭合事件: 事件 'StealRecordCreatedEvent'（由 steal_record_manager 发布）无任何模块监听
- ⚠️ 未闭合事件: 事件 'TrapDeployedEvent'（由 upgrade_manager 发布）无任何模块监听
- ⚠️ 未闭合事件: 事件 'FarmLevelUpEvent'（由 farm_level_manager 发布）无任何模块监听
- ⚠️ 未闭合事件: 事件 'FarmPlotAssignedEvent'（由 farm_teleport_manager 发布）无任何模块监听
- ⚠️ 未闭合事件: 事件 'PlayerLeaveFarmEvent'（由 farm_teleport_manager 发布）无任何模块监听
- ⚠️ 未闭合事件: 事件 'CropBonemeledEvent'（由 harvest_manager 发布）无任何模块监听
- ⚠️ 未闭合事件: 事件 'StealCooldownStartedEvent'（由 steal_manager 发布）无任何模块监听
- ⚠️ 未闭合事件: 事件 'LeaderboardRefreshedEvent'（由 leaderboard_manager 发布）无任何模块监听
- ⚠️ 未闭合事件: 事件 'AchievementUnlockedEvent'（由 achievement_manager 发布）无任何模块监听
- ⚠️ 未闭合事件: 事件 'FarmResetEvent'（由 admin_manager 发布）无任何模块监听
