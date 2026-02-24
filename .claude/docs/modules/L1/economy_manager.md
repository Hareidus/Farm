# EconomyManager

- 模块 ID: `economy_manager`
- 层级: L1
- 依赖: 无
- 状态: 🔲 待构建

---

## 职责

封装与 Vault 经济插件的对接，提供统一的金币查询、增加、扣除能力。隔离外部经济插件的 API 差异，使业务模块无需直接依赖 Vault。同时管理作物收购价格表的加载与查询。

## 交互

### 被调用
- ← achievement_manager（详见 `modules/L2/achievement_manager.md`）
- ← shop_manager（详见 `modules/L2/shop_manager.md`）
- ← steal_manager（详见 `modules/L2/steal_manager.md`）
- ← trap_manager（详见 `modules/L1/trap_manager.md`）
- ← upgrade_manager（详见 `modules/L2/upgrade_manager.md`）

## 关联模型

- [CropPrice](../../foundation/model.md)
