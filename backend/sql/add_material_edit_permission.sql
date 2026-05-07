-- ============================================================
-- 修复：新增物料管理「编辑」权限 material:edit
-- 
-- 问题：全量审计修复时 MaterialController 各方法加了
--       @PreAuthorize("hasAuthority('material:edit')")
--       但 material:edit 权限码未在 sys_permission/sys_button 中创建
--       导致所有角色（含 admin）调用分类CUD/物料CUD/出入库均报 Access Denied
--
-- 影响接口（7个端点）：
--   POST   /material/category          新增分类
--   PUT    /material/category/{id}      更新分类
--   DELETE /material/category/{id}      删除分类
--   POST   /material                   新增物料
--   PUT    /material/{id}              更新物料
--   DELETE /material/{id}              删除物料
--   POST   /material/stock-in          入库
--   POST   /material/stock-out         出库
--   PUT    /material/stock-in/{orderNo}/receive  确认入库
-- ============================================================

-- 1. 确保 sys_permission 中存在 material:edit（使用存储过程防重复）
DROP PROCEDURE IF EXISTS add_material_edit_permission;
DELIMITER $$
CREATE PROCEDURE add_material_edit_permission()
BEGIN
    DECLARE permission_exists INT;

    -- 检查是否已存在
    SELECT COUNT(1) INTO permission_exists FROM sys_permission
    WHERE permission_code = 'material:edit' AND deleted = 0;

    IF permission_exists = 0 THEN
        -- 获取物料管理菜单ID（parent_id）
        SET @menu_id = NULL;
        SELECT id INTO @menu_id FROM sys_permission
        WHERE permission_code = 'material:view' AND deleted = 0
        LIMIT 1;

        IF @menu_id IS NOT NULL THEN
            SET @parent_id = (SELECT parent_id FROM sys_permission WHERE id = @menu_id AND deleted = 0);
        ELSE
            SET @parent_id = 0;
        END IF;

        -- 获取当前最大 sort 值
        SET @max_sort = 0;
        SELECT COALESCE(MAX(sort), 0) INTO @max_sort FROM sys_permission
        WHERE parent_id = @parent_id AND deleted = 0;

        -- 插入权限
        INSERT INTO sys_permission (parent_id, name, type, permission_code, sort, visible, status, create_time, update_time, deleted)
        VALUES (@parent_id, '编辑物料', 'button', 'material:edit', @max_sort + 1, 1, 1, NOW(), NOW(), 0);

        SET @new_permission_id = LAST_INSERT_ID();
        SELECT CONCAT('已创建 material:edit 权限 (id=', @new_permission_id, ')') AS message;

        -- 绑定到所有现有角色
        INSERT INTO sys_role_permission (role_id, permission_id)
        SELECT r.id, @new_permission_id
        FROM sys_role r
        WHERE r.deleted = 0
          AND NOT EXISTS (
              SELECT 1 FROM sys_role_permission rp
              WHERE rp.role_id = r.id AND rp.permission_id = @new_permission_id
          );

        SELECT CONCAT('已绑定到 ', ROW_COUNT(), ' 个角色') AS message;
    ELSE
        SELECT 'material:edit 权限已存在，跳过创建' AS message;
    END IF;
END$$
DELIMITER ;

CALL add_material_edit_permission();
DROP PROCEDURE IF EXISTS add_material_edit_permission;

-- 2. 确保 sys_button 中存在编辑物料按钮（使用存储过程防重复）
DROP PROCEDURE IF EXISTS add_material_edit_button;
DELIMITER $$
CREATE PROCEDURE add_material_edit_button()
BEGIN
    DECLARE button_exists INT;

    SELECT COUNT(1) INTO button_exists FROM sys_button
    WHERE permission = 'material:edit' AND deleted = 0;

    IF button_exists = 0 THEN
        SET @max_sort = 0;
        SELECT COALESCE(MAX(sort), 29) INTO @max_sort FROM sys_button WHERE deleted = 0;

        INSERT INTO sys_button (name, permission, type, parent_id, sort, status, create_time, update_time, deleted)
        VALUES ('编辑物料', 'material:edit', 'button', 0, @max_sort + 1, 1, NOW(), NOW(), 0);

        SELECT CONCAT('已创建编辑物料按钮 (sort=', @max_sort + 1, ')') AS message;
    ELSE
        SELECT '编辑物料按钮已存在，跳过创建' AS message;
    END IF;
END$$
DELIMITER ;

CALL add_material_edit_button();
DROP PROCEDURE IF EXISTS add_material_edit_button;
