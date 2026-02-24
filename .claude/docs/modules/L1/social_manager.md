# SocialManager

- 模块 ID: `social_manager`
- 层级: L1
- 依赖: database_manager
- 状态: 🔲 待构建

---

## 职责

管理玩家间的社交关系数据，包括好友关系和仇人标记。负责好友请求的发送与确认流程、好友关系的双向写入与解除、仇人的自动标记与查询。提供关系查询能力（是否为好友、是否为仇人），管理好友上限约束。不涉及好友/仇人带来的具体业务效果（如浇水加速、偷菜加成），仅提供关系数据层。

## 事件

发布:
- `FriendAddedEvent` — 两名玩家成功建立好友关系时触发；数据: Player A UUID, Player B UUID
- `FriendRemovedEvent` — 好友关系被解除时触发；数据: Player A UUID, Player B UUID
- `EnemyMarkedEvent` — 一名玩家被自动标记为另一名玩家的仇人时触发；数据: 被偷者 UUID, 偷取者 UUID

## 交互

### → database_manager
社交管理调用数据库管理持久化好友关系、仇人标记与好友请求。

### 被调用
- ← debug_tool（详见 `modules/L3/debug_tool.md`）
- ← friend_interaction_manager（详见 `modules/L2/friend_interaction_manager.md`）
- ← steal_manager（详见 `modules/L2/steal_manager.md`）

## 关联模型

- [FriendRelation](../../foundation/model.md)
- [EnemyRecord](../../foundation/model.md)
- [FriendRequest](../../foundation/model.md)
