-- =========================================================
-- 企业广告管理系统 v2.2 字段修复脚本
-- 执行日期: 2026-04-30
-- 说明: 修复代码中使用但数据库中缺失的字段
-- =========================================================

-- 1. ord_order 表添加缺失字段
-- 场景: 代码中使用了 total_cost 和 designer_commission，但数据库表未定义

ALTER TABLE ord_order
ADD COLUMN total_cost DECIMAL(15,2) DEFAULT 0 COMMENT '订单总成本' AFTER total_amount,
ADD COLUMN designer_commission DECIMAL(15,2) DEFAULT 0 COMMENT '设计师提成' AFTER total_cost;

-- 2. ord_order_material 表添加 unit_cost 字段
-- 场景: 代码中使用了 unit_cost 字段记录物料成本，但数据库表未定义

ALTER TABLE ord_order_material
ADD COLUMN unit_cost DECIMAL(15,2) DEFAULT 0 COMMENT '单位成本' AFTER amount;

-- 验证修复
-- SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS
-- WHERE TABLE_SCHEMA = 'your_database_name' AND TABLE_NAME = 'ord_order';

-- SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS
-- WHERE TABLE_SCHEMA = 'your_database_name' AND TABLE_NAME = 'ord_order_material';
