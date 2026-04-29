-- =====================================================
-- 账单表增加 bill_type 字段（区分工厂账单和客户账单）
-- 执行时间: 2026-04-29
-- =====================================================

-- 1. 新增 bill_type 字段，默认值 1（工厂账单，兼容已有数据）
ALTER TABLE fac_factory_bill
ADD COLUMN bill_type TINYINT NOT NULL DEFAULT 1 COMMENT '账单类型：1=工厂账单 2=客户账单' AFTER salesman_name;

-- 2. 新增索引，按客户ID和账单类型查询
ALTER TABLE fac_factory_bill
ADD INDEX idx_bill_type_customer (bill_type, customer_id);
