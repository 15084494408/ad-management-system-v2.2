-- =====================================================
-- 权限补丁：补全 init_backup.sql 遗漏的权限码
-- 执行时机：已用 init_backup.sql 初始化数据库后
-- 执行方式：在 Navicat / DBeaver / MySQL Workbench 中执行
-- =====================================================

USE enterprise_ad;

-- -------------------------------------------------------
-- 1. 补充缺失的权限记录（INSERT IGNORE 防止重复插入）
-- -------------------------------------------------------
INSERT IGNORE INTO sys_permission (parent_id, name, type, permission_code, sort, visible, status, deleted) VALUES
-- 系统管理子权限（parent_id=7 对应"系统管理"菜单）
(7, '角色权限',   'button', 'system:role',   2, 1, 1, 0),
(7, '操作日志',   'button', 'system:log',    3, 1, 1, 0),
(7, '数据字典',   'button', 'system:dict',   4, 1, 1, 0),
(7, '数据备份',   'button', 'system:backup', 5, 1, 1, 0),
(7, '公告管理',   'button', 'system:notice', 6, 1, 1, 0),
(7, '按钮管理',   'button', 'system:menu',   7, 1, 1, 0),
-- 物料管理（parent_id=0 顶级菜单，如有则改为实际 parent_id）
(0, '物料管理',   'menu',   'material:view', 8, 1, 1, 0),
-- 设计文件（parent_id=0 顶级菜单）
(0, '设计文件',   'menu',   'design:file',   9, 1, 1, 0),
-- 统计报表（parent_id=0 顶级菜单）
(0, '统计报表',   'menu',   'statistics:view', 10, 1, 1, 0),
-- 广场管理（parent_id=0 顶级菜单）
(0, '广场管理',   'menu',   'square:manage', 11, 1, 1, 0);

-- -------------------------------------------------------
-- 2. 将上面新增权限全部授权给 SUPER_ADMIN（role_id=1）
--    INSERT IGNORE 防止重复
-- -------------------------------------------------------
INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT 1, id FROM sys_permission
WHERE permission_code IN (
    'system:role', 'system:log', 'system:dict', 'system:backup',
    'system:notice', 'system:menu',
    'material:view', 'design:file', 'statistics:view', 'square:manage'
)
AND deleted = 0;

-- -------------------------------------------------------
-- 3. 同时授权给 ADMIN（role_id=2）
-- -------------------------------------------------------
INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT 2, id FROM sys_permission
WHERE permission_code IN (
    'system:role', 'system:log', 'system:dict',
    'system:notice', 'system:menu',
    'material:view', 'design:file', 'statistics:view', 'square:manage'
    -- 注意：system:backup 不给 ADMIN，只给 SUPER_ADMIN
)
AND deleted = 0;

-- -------------------------------------------------------
-- 4. 验证：查看 SUPER_ADMIN 当前拥有的所有权限码
-- -------------------------------------------------------
SELECT p.permission_code
FROM sys_permission p
INNER JOIN sys_role_permission rp ON p.id = rp.permission_id
WHERE rp.role_id = 1
  AND p.deleted = 0
  AND p.status = 1
  AND p.permission_code IS NOT NULL
  AND p.permission_code != ''
ORDER BY p.permission_code;
