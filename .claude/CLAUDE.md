# CLAUDE.md

> MPGA 自动生成。详细规格见 `.claude/docs/` 下各文件。

## 项目信息

- 插件名: Farm
- 包名: `com.hareidus.taboo.farm`

## 你的角色

你是 **Farm** 项目的首席构建 Agent。你的职责：
- 严格按照 `.claude/docs/` 中的模块规格和 Phase 顺序进行开发
- 写代码前必须通过 MCP 查询技术栈用法，不得凭记忆替换
- 遵守 L1→L2→L3 分层约束和零硬编码规范
- 每个模块完成后编写对应的 TestRunner 并验证
- 遇到不确定的实现细节时，主动查询知识库而非猜测

## 开发环境

本项目在 **Windows** 下开发，请使用对应的命令行工具：
- 终端: PowerShell（优先）或 CMD，不要使用 Unix-only 命令
- 路径分隔符: `\`，注意 Kotlin/Java 字符串中的转义
- 编码: UTF-8（确保 Gradle 和源码文件均为 UTF-8）
- 构建: `gradlew.bat build`（不是 `./gradlew`）
- 运行服务器: `gradlew.bat runServer`（如模板支持）
- Git: `git` 命令通用，无需特殊处理

## 启动流程

### Phase 0 — 项目初始化（必须最先完成）

1. **阅读文档**: 先完整阅读 `docs/_index.md` 和 `docs/modules/` 下所有模块文档，理解项目全貌
2. **修改 build.gradle.kts**:
   - 将 `group` 改为 `com.hareidus.taboo.farm`
   - 将插件名/artifact 改为 `Farm`
   - 检查依赖项，去掉模板中不需要的部分（根据技术栈决定）
   - 确认 Kotlin/Java 版本、Minecraft 目标版本
3. **向用户确认**: 将修改内容列出，等待用户审核通过后再进入下一阶段

### Phase 1+ — 模块构建

审核通过后，启动 AgentTeam 协作构建：
1. 载入 `.claude/` 下的 agent-skills（vibe-build、vibe-docs、vibe-test、no-hardcode）
2. 按下方 Phase 顺序逐阶段构建模块
3. 每个 Phase 内的模块可并行开发

## 技术栈

写代码前必须通过 MCP 查询用法，不得替换：

- `taboolib/basic-tech` → `get_detail(id="taboolib/basic-tech")`
- `taboolib/expanding-technology` → `get_detail(id="taboolib/expanding-technology")`
- `easylib/EasyGui` → `get_detail(id="easylib/EasyGui")`
- `easylib/Function` → `get_detail(id="easylib/Function")`
- `easylib/Utils` → `get_detail(id="easylib/Utils")`
- `vector-displays/terminal` → `get_detail(id="vector-displays/terminal")`
- `vector-displays/widget` → `get_detail(id="vector-displays/widget")`
- `vector-displays/element` → `get_detail(id="vector-displays/element")`
- `command/taboolib-command` → `get_structure(category="command")`
- `config/yaml-config` → `get_structure(category="config")`
- `database/SQL` → `get_structure(category="database")`
- `gui/normal-gui` → `get_structure(category="gui")`
- `gui/pageable-gui` → `get_structure(category="gui")`
- `language/language-system` → `get_structure(category="language")`
- `vector/vector-displays-pattern` → `get_structure(category="vector")`
- `gui-function-bindable-icon` → `get_detail(id="best-practice/gui-function-bindable-icon")`
- `storage-virtual-backpack` → `get_detail(id="best-practice/storage-virtual-backpack")`

Structure 优先级高于 RAG。先查 Structure，再查 RAG。
写模块前推荐先调 `get_build_kit(topic="...")` 一次性拉齐该领域的完整上下文。

## MCP 工具速查

| 工具 | 用途 | 示例 |
|------|------|------|
| `get_build_kit` | 写模块前一次性获取完整构建上下文（Structure + RAG + 约束 + 最佳实践） | `get_build_kit(topic="gui")` |
| `get_structure` | 获取架构约束（最高优先级） | `get_structure(category="gui")` |
| `get_detail` | 获取 RAG/最佳实践完整文档 | `get_detail(id="easylib/function")` |
| `search_knowledge` | 关键词搜索知识库 | `search_knowledge(query="pageable gui")` |
| `resolve_dependencies` | 查看知识单元的依赖链 | `resolve_dependencies(id="easylib/easy-gui")` |
| `list_structures` | 列出所有可用架构约束 | `list_structures()` |
| `search_constraints` | 按库名查疑难杂症 | `search_constraints(library="taboolib")` |
| `get_constraint` | 读取具体疑难杂症 | `get_constraint(library="taboolib", filename="xxx.md")` |
| `submit_issue` | 提交踩坑记录到知识库 inbox | `submit_issue(library="easylib", title="...", description="...")` |

## 模块与 Phase

- `database_manager` (L1)
- `player_data_manager` (L1) → database_manager
- `plot_manager` (L1) → database_manager
- `crop_manager` (L1) → database_manager, plot_manager
- `economy_manager` (L1)
- `social_manager` (L1) → database_manager
- `steal_record_manager` (L1) → database_manager
- `trap_manager` (L1) → database_manager, economy_manager
- `farm_level_manager` (L1) → database_manager
- `farm_teleport_manager` (L2) → plot_manager, crop_manager, player_data_manager
- `harvest_manager` (L2) → crop_manager, plot_manager, player_data_manager, farm_level_manager
- `steal_manager` (L2) → crop_manager, plot_manager, steal_record_manager, trap_manager, social_manager, player_data_manager, farm_level_manager, economy_manager
- `shop_manager` (L2) → economy_manager, player_data_manager, crop_manager
- `friend_interaction_manager` (L2) → social_manager, crop_manager, plot_manager
- `upgrade_manager` (L2) → farm_level_manager, economy_manager, plot_manager, trap_manager, player_data_manager
- `leaderboard_manager` (L2) → player_data_manager, database_manager
- `achievement_manager` (L2) → player_data_manager, database_manager, economy_manager
- `admin_manager` (L2) → plot_manager, crop_manager, trap_manager, farm_level_manager, player_data_manager
- `placeholder_expansion` (L3) → player_data_manager, farm_level_manager, leaderboard_manager, achievement_manager, plot_manager
- `migration_tool` (L3) → database_manager, plot_manager, crop_manager
- `debug_tool` (L3) → player_data_manager, plot_manager, crop_manager, steal_record_manager, social_manager, trap_manager, farm_level_manager

- Phase 0: 共享数据模型
- Phase 1: database_manager, economy_manager
- Phase 2: farm_level_manager, player_data_manager, plot_manager, social_manager, steal_record_manager, trap_manager
- Phase 3: crop_manager
- Phase 4: achievement_manager, admin_manager, farm_teleport_manager, friend_interaction_manager, harvest_manager, leaderboard_manager, shop_manager, steal_manager, upgrade_manager
- Phase 5: debug_tool, migration_tool, placeholder_expansion

详见 `docs/_index.md`（全景）和 `docs/modules/` 下各模块文档（规格）。

## 测试

| 模块 | 前缀 | TestRunner |
|------|------|------------|
| database_manager | database | DatabaseTestRunner |
| player_data_manager | playerdata | PlayerDataTestRunner |
| plot_manager | plot | PlotTestRunner |
| crop_manager | crop | CropTestRunner |
| economy_manager | economy | EconomyTestRunner |
| social_manager | social | SocialTestRunner |
| steal_record_manager | stealrecord | StealRecordTestRunner |
| trap_manager | trap | TrapTestRunner |
| farm_level_manager | farmlevel | FarmLevelTestRunner |
| farm_teleport_manager | farmteleport | FarmTeleportTestRunner |
| harvest_manager | harvest | HarvestTestRunner |
| steal_manager | steal | StealTestRunner |
| shop_manager | shop | ShopTestRunner |
| friend_interaction_manager | friendinteraction | FriendInteractionTestRunner |
| upgrade_manager | upgrade | UpgradeTestRunner |
| leaderboard_manager | leaderboard | LeaderboardTestRunner |
| achievement_manager | achievement | AchievementTestRunner |
| admin_manager | admin | AdminTestRunner |

## 链路验收

全部 Phase 完成后，逐条验证以下行为链路是否可走通：

1. **新玩家首次进入农场并获得地块**: 玩家在主世界找到农场传送点（NPC/交互方块） → 玩家右键交互传送点 → 系统检测玩家是否拥有农场地块 → 系统在农场世界网格中计算下一个可用地块坐标 → 系统生成地块地面和边界 → 系统将地块坐标绑定到玩家数据 → 系统将玩家传送至新地块中心 → 系统发放新手种子礼包到玩家背包 → 系统发送欢迎提示消息
2. **老玩家传送回自己的农场**: 玩家右键交互主世界传送点 → 系统检测玩家已拥有地块 → 系统加载玩家地块区域 → 系统根据当前时间戳与每株作物种植时间戳的差值计算离线生长进度 → 系统更新地块内所有作物的生长阶段（替换方块/头颅材质） → 系统将玩家传送至地块中心 → 系统检查是否有未读的被偷通知并推送
3. **玩家在自己地块种植作物**: 玩家手持种子物品 → 玩家右键点击地块内的耕地方块 → 系统校验点击位置是否在玩家自己的地块边界内 → 系统校验该位置是否为空（未种植） → 系统识别种子类型（原版/自定义） → 系统在该位置放置作物初始阶段方块（原版作物用原版方块，自定义作物放置第一阶段头颅） → 系统记录作物数据：种类、位置、种植时间戳 → 系统扣除玩家背包中的种子物品
4. **作物自然生长并被玩家收割**: 玩家进入地块区域触发加载 → 系统遍历地块内所有作物，根据当前时间戳减去种植时间戳计算已过时间 → 系统将已过时间映射到作物配置的生长阶段 → 系统更新未成熟作物的方块/头颅材质到对应阶段 → 玩家右键点击一株已成熟的作物 → 系统校验该作物确实处于成熟阶段 → 系统根据作物配置计算产出数量（随机范围） → 系统将产出物品放入玩家背包 → 系统移除该位置的作物方块，恢复为耕地 → 系统删除该作物的数据记录 → 系统累加玩家收获总量统计
5. **玩家使用骨粉加速自己作物生长**: 玩家手持骨粉右键点击自己地块内未成熟的作物 → 系统校验该作物未成熟 → 系统将该作物的种植时间戳向前偏移一个配置的加速时长 → 系统重新计算生长阶段 → 系统更新作物方块/头颅材质到新阶段 → 系统扣除一个骨粉 → 系统播放骨粉粒子效果
6. **玩家向NPC出售作物获得金币**: 玩家在农场世界右键点击收购NPC → 系统打开收购站GUI，展示玩家背包中可出售的作物及其单价 → 玩家点击GUI中要出售的作物 → 系统计算出售总价（数量 × 配置单价） → 系统通过Vault接口向玩家账户增加金币 → 系统从玩家背包扣除对应作物物品 → 系统累加玩家金币收入统计 → 系统发送出售成功消息
7. **玩家传送到其他玩家的农场**: 玩家执行命令或通过导航GUI选择目标玩家 → 系统校验目标玩家拥有农场地块 → 系统校验玩家拥有 stealfarm.teleport 权限 → 系统加载目标地块区域 → 系统计算目标地块内作物的当前生长状态 → 系统将玩家传送至目标地块 → 系统标记玩家当前处于他人农场
8. **偷菜成功流程**: 玩家在他人农场右键点击一株成熟作物 → 系统校验玩家拥有 stealfarm.steal 权限 → 系统检查玩家对该农场主的偷菜冷却是否已过（默认4小时） → 系统计算该地块当前成熟作物总数 → 系统计算本次可偷比例上限（基础比例 - 农场防护等级降低量 + 仇人加成） → 系统计算玩家本次已偷数量是否未达上限 → 系统将该作物产出放入玩家背包 → 系统移除该位置作物方块并恢复耕地 → 系统记录偷菜日志（偷取者、被偷者、作物种类、数量、时间） → 系统累加偷取者的偷菜总量统计 → 系统累加被偷者的被偷总量统计 → 系统自动将偷取者标记为被偷者的仇人 → 若被偷者在线则实时推送通知，否则存入离线通知队列
9. **偷菜被冷却时间阻止**: 玩家在他人农场右键点击一株成熟作物 → 系统检查偷菜冷却时间 → 系统发现冷却未结束 → 系统向玩家发送冷却剩余时间提示 → 操作被拒绝，作物不受影响
10. **偷菜达到比例上限被阻止**: 玩家在他人农场右键点击一株成熟作物 → 系统校验冷却已过 → 系统计算本次已偷数量已达到比例上限 → 系统向玩家发送已达偷取上限提示 → 系统开始对该农场主的偷菜冷却计时 → 操作被拒绝
11. **偷菜触发陷阱受到惩罚**: 玩家在他人农场右键偷取成熟作物 → 系统判定该地块存在已部署的陷阱 → 系统根据陷阱触发概率进行判定 → 陷阱触发成功 → 系统根据陷阱类型施加惩罚效果（减速/扣金币/强制传送回自己农场） → 系统向偷菜者发送触发陷阱提示 → 系统向农场主记录陷阱触发日志 → 系统累加偷菜者的触发陷阱次数统计
12. **添加好友并为好友农场浇水加速**: 玩家A执行添加好友命令/GUI操作指定玩家B → 系统向玩家B发送好友请求 → 玩家B确认接受好友请求 → 系统在双方数据中互相写入好友关系 → 玩家A传送到玩家B的农场 → 玩家A右键点击玩家B地块内未成熟的作物进行浇水 → 系统校验玩家A与玩家B是好友关系 → 系统校验浇水冷却是否已过 → 系统将该作物种植时间戳向前偏移配置的浇水加速时长 → 系统更新作物生长阶段和方块材质 → 系统记录浇水冷却开始时间 → 系统向双方发送浇水成功提示
13. **查看仇人列表并复仇偷菜获得加成**: 玩家打开好友/仇人管理GUI → 系统展示仇人列表（自动标记的偷过自己的玩家） → 玩家选择一个仇人并点击传送到其农场 → 系统将玩家传送至仇人农场 → 玩家右键偷取仇人农场的成熟作物 → 系统检测目标为仇人关系 → 系统应用仇人偷取比例加成（配置值） → 偷取成功，玩家获得加成后的偷取上限
14. **玩家升级农场地块**: 玩家打开农场升级GUI → 系统展示当前等级、下一级所需金币/材料、解锁内容预览 → 玩家点击升级按钮 → 系统校验玩家金币和材料是否满足要求 → 系统通过Vault扣除金币并扣除背包材料 → 系统更新玩家农场等级数据 → 系统扩展地块物理边界（增加耕地面积） → 系统解锁对应功能（陷阱槽位/装饰位/防护等级提升/自动收割） → 系统发送升级成功消息并展示新解锁内容
15. **玩家在农场部署陷阱**: 玩家打开陷阱配置GUI → 系统展示已解锁的陷阱槽位和可用陷阱种类 → 玩家选择一种陷阱类型 → 玩家选择部署到某个空闲槽位 → 系统校验玩家是否有足够的陷阱槽位（由农场等级决定） → 系统校验玩家是否拥有部署所需的材料/金币 → 系统扣除消耗并将陷阱数据写入地块配置 → 系统发送陷阱部署成功提示
16. **最高级农场自动收割作物**: 系统检测玩家农场等级已解锁自动收割功能 → 玩家地块被加载时系统遍历所有作物 → 系统发现有作物根据时间戳计算已成熟 → 系统自动计算产出数量 → 系统将产出存入农场仓库（而非玩家背包） → 系统移除成熟作物方块并恢复耕地 → 系统累加玩家收获总量统计
17. **玩家查看排行榜**: 玩家执行排行榜命令或点击GUI入口 → 系统打开排行榜分类GUI（种植之王/偷菜大盗/农场富翁/铁壁农场） → 玩家选择一个排行榜类别 → 系统从数据库查询对应维度的排名数据 → 系统渲染分页排行榜GUI展示排名、玩家名、数值 → 玩家可翻页浏览
18. **玩家达成成就并解锁称号奖励**: 玩家完成某个行为（种植/收获/偷菜/被偷/触发陷阱/连续登录收菜） → 系统事件触发后检查该行为关联的成就条件 → 系统查询玩家当前成就进度数据 → 系统判定进度达到成就阈值 → 系统标记该成就为已完成 → 系统发放成就奖励（金币/稀有种子/装饰物品） → 系统解锁对应称号前缀 → 系统向玩家发送成就达成通知（标题/音效）
19. **离线玩家上线后收到被偷通知**: 玩家A离线期间被玩家B偷菜 → 系统将偷菜记录写入玩家A的离线通知队列 → 玩家A上线 → 系统检测玩家A有未读偷菜通知 → 系统向玩家A推送被偷详情（谁偷了什么、数量、时间） → 系统标记通知为已读
20. **管理员重置玩家农场**: 管理员执行重置农场命令并指定目标玩家 → 系统校验执行者拥有 stealfarm.admin 权限 → 系统清除目标玩家的农场等级、作物数据、陷阱配置、装饰配置 → 系统将地块区域方块恢复为初始状态 → 系统重置地块大小为初始值 → 系统发送重置完成确认消息

每条链路从第一个节点触发，沿箭头逐步验证，直到最终结果。不可走通的链路 = 功能缺陷。

## 约束速记

- L1 ← L2 ← L3，禁止反向依赖
- 模块间通过 API 接口调用，禁止引用内部实现
- 零硬编码：文本 → sendLang，数值 → config，资源 ID → 配置
- 模块完成 = 代码 + TestRunner
