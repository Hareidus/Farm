# DatabaseManager

- 模块 ID: `database_manager`
- 层级: L1
- 依赖: 无
- 状态: 🔲 待构建

---

## 职责

管理插件的数据库连接生命周期，提供 SQLite/MySQL 双驱动支持，定义所有数据表结构，封装通用的 CRUD 操作接口。作为全插件唯一的持久化出口，所有需要读写数据库的模块都通过它执行操作。

## 交互

### 被调用
- ← achievement_manager（详见 `modules/L2/achievement_manager.md`）
- ← crop_manager（详见 `modules/L1/crop_manager.md`）
- ← farm_level_manager（详见 `modules/L1/farm_level_manager.md`）
- ← leaderboard_manager（详见 `modules/L2/leaderboard_manager.md`）
- ← migration_tool（详见 `modules/L3/migration_tool.md`）
- ← player_data_manager（详见 `modules/L1/player_data_manager.md`）
- ← plot_manager（详见 `modules/L1/plot_manager.md`）
- ← social_manager（详见 `modules/L1/social_manager.md`）
- ← steal_record_manager（详见 `modules/L1/steal_record_manager.md`）
- ← trap_manager（详见 `modules/L1/trap_manager.md`）
