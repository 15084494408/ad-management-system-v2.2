-- patch_mat_stock_log.sql
-- 修复库存日志表缺少 operator_id 字段的问题
-- 执行时间: 2026-04-26

-- 添加 operator_id 字段（如果不存在）
ALTER TABLE mat_stock_log 
ADD COLUMN IF NOT EXISTS operator_id BIGINT DEFAULT NULL COMMENT '操作人ID' 
AFTER remark;

-- 验证表结构
DESCRIBE mat_stock_log;