-- 修复用户角色显示 + 设计师下拉为空
-- 执行时间：2026-04-30
-- 说明：
--   1. 给用户 user_id=3（操作员小美）追加设计师角色，使设计师下拉有数据
--   2. 后端 SysUser 实体新增 roleIds 字段，用户列表 API 会自动填充

-- 1. 给操作员小美（user_id=3）分配设计师角色（role_id=5）
-- 使用 INSERT IGNORE 避免重复插入
INSERT IGNORE INTO sys_user_role (user_id, role_id) VALUES (3, 5);

-- 2. 可选：如果你想让 admin 用户也出现在设计师列表，取消下面注释
-- INSERT IGNORE INTO sys_user_role (user_id, role_id) VALUES (1, 5);
