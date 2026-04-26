-- 补丁：给 mat_category 表添加缺失的 status 字段
-- 执行时间：2026-04-22
-- 原因：Entity MaterialCategory 有 status 字段，但建表语句遗漏

ALTER TABLE mat_category 
    ADD COLUMN `status` INT DEFAULT 1 COMMENT '状态：1正常 0禁用' AFTER sort_order;
