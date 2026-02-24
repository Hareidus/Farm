# CropManager

- 模块 ID: `crop_manager`
- 层级: L1
- 依赖: database_manager, plot_manager
- 状态: 🔲 待构建

---

## 职责

管理作物的定义、生命周期与物理渲染。负责从配置加载所有作物类型定义（原版与自定义），包括生长阶段数、各阶段时长、产出范围、头颅材质等属性。管理地块内每株作物的数据（种类、位置、种植时间戳），提供基于真实时间戳的生长阶段计算，执行作物方块/头颅的放置与阶段更新渲染，处理作物的种植写入与收割移除。支持时间戳偏移以实现加速生长。

## 事件

发布:
- `CropPlantedEvent` — 一株作物被成功种植时触发；数据: Player, CropInstance, Plot
- `CropHarvestedEvent` — 一株作物被收割时触发（含自动收割和手动收割）；数据: Player（可为null表示自动收割）, CropInstance, 产出物品列表, 是否自动收割
- `CropGrowthUpdatedEvent` — 作物生长阶段因时间推进或加速而变更时触发；数据: CropInstance, 旧阶段, 新阶段, 加速原因（自然/骨粉/浇水）
- `CropRemovedEvent` — 一株作物数据被移除时触发（被偷、收割、重置等）；数据: CropInstance, 移除原因枚举

## 交互

### → database_manager
作物管理调用数据库管理持久化作物种植、移除与时间戳更新。

### → plot_manager
作物管理调用地块管理查询作物所属地块边界与归属信息。

### 被调用
- ← admin_manager（详见 `modules/L2/admin_manager.md`）
- ← debug_tool（详见 `modules/L3/debug_tool.md`）
- ← farm_teleport_manager（详见 `modules/L2/farm_teleport_manager.md`）
- ← friend_interaction_manager（详见 `modules/L2/friend_interaction_manager.md`）
- ← harvest_manager（详见 `modules/L2/harvest_manager.md`）
- ← migration_tool（详见 `modules/L3/migration_tool.md`）
- ← shop_manager（详见 `modules/L2/shop_manager.md`）
- ← steal_manager（详见 `modules/L2/steal_manager.md`）

## 关联模型

- [CropDefinition](../../foundation/model.md)
- [CropInstance](../../foundation/model.md)
