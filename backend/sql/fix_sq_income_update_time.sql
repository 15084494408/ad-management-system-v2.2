-- =====================================================
-- 修复 sq_income 表缺少 update_time 字段
-- 执行时间: 2026-04-29
-- =====================================================

ALTER TABLE sq_income
ADD COLUMN update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间' AFTER create_time;
