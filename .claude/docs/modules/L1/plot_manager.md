# PlotManager

- 模块 ID: `plot_manager`
- 层级: L1
- 依赖: database_manager
- 状态: 🔲 待构建

---

## 职责

管理农场世界的地块分配与物理结构。负责农场 void 世界的生成（自定义 ChunkGenerator），网格坐标计算与地块分配算法，地块地面和边界方块的生成与扩展，地块归属权查询（坐标→玩家、玩家→地块），以及地块物理区域的重置。提供判断某个位置是否在某玩家地块内的能力。

## 事件

发布:
- `PlotAllocatedEvent` — 新地块被分配给玩家时触发；数据: Player, Plot（含地块坐标和边界信息）
- `PlotExpandedEvent` — 地块物理边界因升级而扩展时触发；数据: Player, Plot, 旧尺寸, 新尺寸

## 交互

### → database_manager
地块管理调用数据库管理持久化地块分配、归属绑定与尺寸变更。

### 被调用
- ← admin_manager（详见 `modules/L2/admin_manager.md`）
- ← crop_manager（详见 `modules/L1/crop_manager.md`）
- ← debug_tool（详见 `modules/L3/debug_tool.md`）
- ← farm_teleport_manager（详见 `modules/L2/farm_teleport_manager.md`）
- ← friend_interaction_manager（详见 `modules/L2/friend_interaction_manager.md`）
- ← harvest_manager（详见 `modules/L2/harvest_manager.md`）
- ← migration_tool（详见 `modules/L3/migration_tool.md`）
- ← placeholder_expansion（详见 `modules/L3/placeholder_expansion.md`）
- ← steal_manager（详见 `modules/L2/steal_manager.md`）
- ← upgrade_manager（详见 `modules/L2/upgrade_manager.md`）

## 关联模型

- [Plot](../../foundation/model.md)
