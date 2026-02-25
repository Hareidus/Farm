# Farm

基于 TabooLib + Kotlin 的 Minecraft 农场经营插件，提供种植、偷菜、社交、升级、排行榜等完整玩法。

## 功能概览

- **个人农场** — 每位玩家在独立世界中拥有专属地块，支持多地块经营（农田/牧场/果园）
- **作物系统** — 原版 + 自定义头颅作物，基于时间戳的离线生长计算
- **偷菜 PvP** — 冷却 + 比例上限 + 仇人加成 + 陷阱触发 + 离线通知
- **好友互动** — 好友浇水加速、仇人复仇偷菜加成
- **农场升级** — 5 级进阶，解锁扩地、陷阱槽位、看门宠物、自动收割
- **经济商店** — Vault 集成，NPC 收购站出售作物
- **排行榜** — 种植之王 / 偷菜大盗 / 农场富翁 / 铁壁农场
- **成就系统** — 里程碑追踪 + 金币/种子/称号奖励
- **看门宠物** — 部署宠物巡逻，自动发现入侵者并施加惩罚
- **API 扩展** — FarmAPI 统一入口 + 8 个可取消 Pre 事件 + 运行时作物/陷阱注册 + 季节接口预留

## 环境要求

| 依赖 | 版本 |
|------|------|
| Spigot / Paper | 1.21+ |
| Java | 17+ |
| TabooLib | 6.2.4 |
| Kotlin | 2.2.0 |
| Vault | 1.7.1（可选，缺失则禁用经济） |
| PlaceholderAPI | 可选 |

## 构建

```bash
gradlew.bat build
```

产物位于 `build/libs/`。
## 架构

三层模块化架构，严格禁止反向依赖：

```
L1（数据层）→ L2（业务层）→ L3（工具层）
```

**L1 模块**: database, player_data, plot, crop, farm_level, economy, social, steal_record, trap, guard_pet

**L2 模块**: harvest, steal, shop, farm_teleport, friend_interaction, upgrade, leaderboard, achievement, admin

**L3 模块**: placeholder_expansion, migration_tool, debug_tool

## 命令

| 命令 | 说明 |
|------|------|
| `/farm` | 主菜单（升级/商店/社交/排行/偷菜） |
| `/farmtp` | 传送到自己的农场 |
| `/flb` | 查看排行榜 |
| `/farmadmin reset <玩家>` | 重置玩家农场 |
| `/farmadmin setlevel <玩家> <等级>` | 设置农场等级 |
| `/farmadmin reload` | 重载配置 |

## 配置文件

```
src/main/resources/
├── config.yml              # 主配置（数据库类型等）
├── lang/zh_CN.yml          # 语言文件
├── sounds.yml              # 音效配置
├── modules/l1/             # L1 模块配置（plot, crop, trap, farm_level...）
├── modules/l2/             # L2 模块配置（harvest, steal, shop...）
└── gui/                    # GUI 布局（shop, upgrade, leaderboard, social...）
```

## API

外部插件通过 `FarmAPI` 对象访问所有功能：

```kotlin
// 地块
FarmAPI.getPlotByOwner(uuid)
FarmAPI.getPlotsByOwner(uuid)       // 多地块
FarmAPI.getAllPlots()

// 作物
FarmAPI.getCropDefinition(id)
FarmAPI.registerCropDefinition(def) // 运行时注册
FarmAPI.isCropMature(crop)

// 陷阱
FarmAPI.registerTrapDefinition(def) // 运行时注册

// 经济 / 等级 / 社交 / 排行
FarmAPI.getBalance(player)
FarmAPI.getPlayerLevel(uuid)
FarmAPI.isFriend(a, b)
FarmAPI.getLeaderboard(category, limit)

// 季节系统（预留，注入实现即可启用）
FarmAPI.seasonProvider = MySeasonPlugin()
FarmAPI.getCurrentSeason()
FarmAPI.getSeasonModifier(cropTypeId)
```

### 事件系统

8 个可取消的 Pre 事件，允许外部插件拦截或修改行为：

| 事件 | 可修改字段 | 用途 |
|------|-----------|------|
| `PreCropPlantEvent` | — | 拦截种植（季节/地块限制） |
| `PreCropHarvestEvent` | `harvestAmount` | 修改产量、拦截收割 |
| `PreFarmUpgradeEvent` | — | 自定义升级条件 |
| `PreTrapDeployEvent` | — | 自定义部署条件 |
| `PreFarmTeleportEvent` | — | 传送拦截 |
| `PreCropSellEvent` | `sellPrice` | 修改售价、拦截出售 |
| `PreCropWaterEvent` | — | 拦截浇水 |
| `PreGuardPetDeployEvent` | — | 自定义部署条件 |

注册事件：`CropDefinitionRegisteredEvent`、`TrapDefinitionRegisteredEvent`

## 项目结构

```
src/main/kotlin/com/hareidus/taboo/farm/
├── foundation/
│   ├── api/            # FarmAPI 入口 + ISeasonProvider + Pre/Registry 事件
│   ├── config/         # 配置加载
│   ├── database/       # IDatabase + SQLite/MySQL 实现
│   ├── gui/            # GUI 框架
│   ├── model/          # 数据模型
│   └── sound/          # 音效管理
└── modules/
    ├── l1/             # 核心数据管理器
    ├── l2/             # 业务逻辑编排
    └── l3/             # 工具与扩展
```

## License

Private project.
