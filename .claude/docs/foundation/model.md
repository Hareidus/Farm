# 共享数据模型目录

| 模型名 | 所属模块 | 说明 |
|--------|----------|------|
| PlayerData | player_data_manager | 玩家核心数据聚合，包含农场等级引用、各维度累计统计值（收获量、偷菜量、被偷量、金币收入、陷阱触发次数） |
| OfflineNotification | player_data_manager | 玩家离线期间积累的待推送通知条目，包含通知类型、关联数据和时间戳 |
| Plot | plot_manager | 农场地块实体，包含地块ID、所属玩家UUID、网格坐标、世界坐标边界、当前尺寸 |
| CropDefinition | crop_manager | 作物类型定义，包含作物ID、名称、是否自定义、各生长阶段时长与材质、产出数量范围 |
| CropInstance | crop_manager | 地块内一株具体作物的运行时数据，包含作物类型引用、世界坐标、种植时间戳、所属地块ID |
| CropPrice | economy_manager | 作物收购价格条目，映射作物ID到NPC收购单价 |
| FriendRelation | social_manager | 双向好友关系记录，包含双方玩家UUID和建立时间 |
| EnemyRecord | social_manager | 仇人标记记录，包含被偷者UUID、偷取者UUID和标记时间 |
| FriendRequest | social_manager | 待处理的好友请求，包含发起者UUID、接收者UUID、请求时间和状态 |
| StealRecord | steal_record_manager | 单次偷菜行为的完整日志，包含偷取者、被偷者、作物种类、数量和时间戳 |
| StealCooldown | steal_record_manager | 玩家对特定农场主的偷菜冷却状态，包含冷却开始时间和持续时长 |
| TrapDefinition | trap_manager | 陷阱种类定义，包含陷阱ID、名称、惩罚效果类型、触发概率、部署消耗 |
| DeployedTrap | trap_manager | 地块内已部署的陷阱实例，包含陷阱类型引用、所属地块ID和槽位编号 |
| FarmLevelDefinition | farm_level_manager | 农场等级配置定义，包含等级编号、升级消耗、面积增量、解锁功能项和防护等级对应的被偷比例上限 |
| PlayerFarmLevel | farm_level_manager | 玩家当前农场等级数据，包含玩家UUID、当前等级和已解锁功能集合 |
| FarmStorage | harvest_manager | 农场仓库，存储自动收割产出的作物物品，玩家可通过GUI取出 |
| WaterCooldown | friend_interaction_manager | 好友浇水冷却记录，追踪玩家对特定农场主的浇水冷却状态与结束时间 |
| LeaderboardEntry | leaderboard_manager | 排行榜条目，包含玩家标识、排名维度、数值与排名位次的缓存数据 |
| AchievementDefinition | achievement_manager | 成就定义，描述成就的触发条件类型、阈值、奖励内容与关联称号 |
| PlayerAchievement | achievement_manager | 玩家成就进度，记录每个成就的当前进度值与是否已完成状态 |