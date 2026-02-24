# MigrationTool

- 模块 ID: `migration_tool`
- 层级: L3
- 依赖: database_manager, plot_manager, crop_manager
- 状态: 🔲 待构建

---

## 职责

提供数据迁移与修复能力。支持数据库表结构版本升级时的自动迁移脚本执行，以及管理员手动触发的数据完整性检查与修复（如孤立作物记录清理、地块数据与物理方块不一致的修复）。仅通过管理员命令触发，不被任何业务模块依赖。

## 交互

### → database_manager
迁移工具调用数据库管理执行表结构升级脚本与数据完整性修复。

### → plot_manager
迁移工具调用地块管理修复地块数据与物理方块不一致问题。

### → crop_manager
迁移工具调用作物管理清理孤立作物记录。
