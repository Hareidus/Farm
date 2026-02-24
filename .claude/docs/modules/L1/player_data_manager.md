# PlayerDataManager

- 模块 ID: `player_data_manager`
- 层级: L1
- 依赖: database_manager
- 状态: 🔲 待构建

---

## 职责

管理玩家的核心持久化数据，包括农场等级、累计金币收入、收获总量、偷菜总量、被偷总量、触发陷阱次数等统计维度。负责玩家上线时加载数据、下线时保存数据，提供玩家数据的查询与更新能力。同时管理玩家的离线通知队列（被偷通知等），在玩家上线时推送未读通知。

## 事件

发布:
- `PlayerDataLoadedEvent` — 玩家数据从数据库加载完成后触发；数据: Player, PlayerData
- `PlayerStatisticUpdateEvent` — 玩家某项统计数据发生变更时触发；数据: Player, 统计类型枚举, 旧值, 新值

监听:
- `PlayerJoinEvent` — 玩家加入服务器时加载玩家数据并推送离线通知；数据: Player
- `PlayerQuitEvent` — 玩家退出服务器时保存玩家数据；数据: Player

## 交互

### → database_manager
玩家数据管理调用数据库管理执行玩家数据和离线通知的加载、保存与更新。

### 被调用
- ← achievement_manager（详见 `modules/L2/achievement_manager.md`）
- ← admin_manager（详见 `modules/L2/admin_manager.md`）
- ← debug_tool（详见 `modules/L3/debug_tool.md`）
- ← farm_teleport_manager（详见 `modules/L2/farm_teleport_manager.md`）
- ← harvest_manager（详见 `modules/L2/harvest_manager.md`）
- ← leaderboard_manager（详见 `modules/L2/leaderboard_manager.md`）
- ← placeholder_expansion（详见 `modules/L3/placeholder_expansion.md`）
- ← shop_manager（详见 `modules/L2/shop_manager.md`）
- ← steal_manager（详见 `modules/L2/steal_manager.md`）
- ← upgrade_manager（详见 `modules/L2/upgrade_manager.md`）

## 关联模型

- [PlayerData](../../foundation/model.md)
- [OfflineNotification](../../foundation/model.md)
