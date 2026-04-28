-- ============================================================
-- 修复 fin_quote 和 fin_quote_detail 表（v2.4 升级补丁）
-- 执行前请备份数据！
-- ============================================================

-- 1. fin_quote 表：补充新版本 init.sql 中有但旧数据库缺少的列
ALTER TABLE fin_quote
  ADD COLUMN IF NOT EXISTS customer_id BIGINT COMMENT '客户ID' AFTER remark,
  ADD COLUMN IF NOT EXISTS company_id BIGINT COMMENT '报价公司ID' AFTER customer_id,
  ADD COLUMN IF NOT EXISTS tax_rate DECIMAL(5,2) DEFAULT 0 COMMENT '税率%' AFTER company_id,
  ADD COLUMN IF NOT EXISTS tax_amount DECIMAL(12,2) DEFAULT 0 COMMENT '税额' AFTER tax_rate,
  ADD COLUMN IF NOT EXISTS quote_date VARCHAR(20) COMMENT '报价日期' AFTER tax_amount;

-- 重建唯一索引（条件唯一），如有普通索引则先删后加
-- ALTER TABLE fin_quote DROP INDEX IF EXISTS uk_quote_no;
-- ALTER TABLE fin_quote ADD UNIQUE INDEX uk_quote_no ((CASE WHEN deleted = 0 THEN quote_no ELSE NULL END));

-- 2. fin_quote_detail 表：旧数据库可能完全没有此表，直接建
CREATE TABLE IF NOT EXISTS fin_quote_detail (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    quote_id BIGINT NOT NULL COMMENT '关联报价ID',
    material_name VARCHAR(200) NOT NULL COMMENT '物料名称',
    spec VARCHAR(200) COMMENT '规格',
    unit VARCHAR(20) COMMENT '单位',
    quantity DECIMAL(10,2) DEFAULT 1 COMMENT '数量',
    unit_price DECIMAL(15,2) DEFAULT 0 COMMENT '单价',
    amount DECIMAL(15,2) DEFAULT 0 COMMENT '小计金额',
    remark VARCHAR(500) COMMENT '备注',
    is_custom TINYINT DEFAULT 0 COMMENT '是否手动物料(0=否,1=是)',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    INDEX idx_quote_id (quote_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报价物料明细表';
