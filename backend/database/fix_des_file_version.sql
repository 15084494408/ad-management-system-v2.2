-- =============================================================
-- 修复：des_file_version 表结构对齐实体 FileVersion 字段
-- 问题：实体新增了 path / url / changeDesc / operator* 字段，
-- 但旧表是 file_url / file_size / remark / creator_id 结构
-- 执行日期：2026-05-04
-- 
-- ★ 安全版本：使用存储过程检查列是否存在，可重复执行
-- =============================================================

-- 0. 仅在需要时执行存储过程
DROP PROCEDURE IF EXISTS migrate_des_file_version;

DELIMITER $$

CREATE PROCEDURE migrate_des_file_version()
BEGIN
    -- 1. 添加 path
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = 'enterprise_ad' AND TABLE_NAME = 'des_file_version'
        AND COLUMN_NAME = 'path') THEN
        ALTER TABLE des_file_version
            ADD COLUMN `path` VARCHAR(500) COMMENT '版本文件路径' AFTER `version`;
    END IF;

    -- 2. 重命名 file_url → url（或跳过，如果 url 已存在）
    IF EXISTS (SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = 'enterprise_ad' AND TABLE_NAME = 'des_file_version'
        AND COLUMN_NAME = 'file_url')
        AND NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = 'enterprise_ad' AND TABLE_NAME = 'des_file_version'
            AND COLUMN_NAME = 'url') THEN
        ALTER TABLE des_file_version
            CHANGE COLUMN `file_url` `url` VARCHAR(500) COMMENT '版本文件URL';
    END IF;

    -- 3. 重命名 file_size → size
    IF EXISTS (SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = 'enterprise_ad' AND TABLE_NAME = 'des_file_version'
        AND COLUMN_NAME = 'file_size')
        AND NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = 'enterprise_ad' AND TABLE_NAME = 'des_file_version'
            AND COLUMN_NAME = 'size') THEN
        ALTER TABLE des_file_version
            CHANGE COLUMN `file_size` `size` BIGINT COMMENT '版本文件大小';
    END IF;

    -- 4. 重命名 remark → change_desc
    IF EXISTS (SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = 'enterprise_ad' AND TABLE_NAME = 'des_file_version'
        AND COLUMN_NAME = 'remark')
        AND NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = 'enterprise_ad' AND TABLE_NAME = 'des_file_version'
            AND COLUMN_NAME = 'change_desc') THEN
        ALTER TABLE des_file_version
            CHANGE COLUMN `remark` `change_desc` VARCHAR(500) COMMENT '变更说明';
    END IF;

    -- 5. 重命名 creator_id → operator_id
    IF EXISTS (SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = 'enterprise_ad' AND TABLE_NAME = 'des_file_version'
        AND COLUMN_NAME = 'creator_id')
        AND NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = 'enterprise_ad' AND TABLE_NAME = 'des_file_version'
            AND COLUMN_NAME = 'operator_id') THEN
        ALTER TABLE des_file_version
            CHANGE COLUMN `creator_id` `operator_id` BIGINT COMMENT '操作人ID';
    END IF;

    -- 6. 添加 operator_name
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = 'enterprise_ad' AND TABLE_NAME = 'des_file_version'
        AND COLUMN_NAME = 'operator_name') THEN
        ALTER TABLE des_file_version
            ADD COLUMN `operator_name` VARCHAR(100) COMMENT '操作人' AFTER `operator_id`;
    END IF;
END$$

DELIMITER ;

-- 执行迁移
CALL migrate_des_file_version();

-- 清理
DROP PROCEDURE IF EXISTS migrate_des_file_version;
