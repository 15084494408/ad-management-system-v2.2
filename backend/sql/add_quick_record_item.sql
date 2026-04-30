-- 快速记账物料明细 + 按面积计价 迁移脚本
-- 执行时间：2026-04-30
-- 说明：新增 fin_record_item 表，mat_material 增加 pricing_type 字段

-- 1. mat_material 新增 pricing_type 字段
-- MySQL 不支持 ADD COLUMN IF NOT EXISTS，先检查列是否存在
SET @dbname = DATABASE();
SET @tablename = 'mat_material';
SET @columnname = 'pricing_type';
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE
      (TABLE_SCHEMA = @dbname)
      AND (TABLE_NAME = @tablename)
      AND (COLUMN_NAME = @columnname)
  ) > 0,
  'SELECT 1',
  CONCAT('ALTER TABLE ', @tablename, ' ADD COLUMN ', @columnname, ' TINYINT DEFAULT 0 COMMENT ''计价方式：0按数量 1按面积'' AFTER factory_price;')
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- 2. 新增 fin_record_item 明细表
CREATE TABLE IF NOT EXISTS fin_record_item (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    record_id       BIGINT NOT NULL COMMENT '关联流水ID',
    material_id     BIGINT NOT NULL COMMENT '物料ID',
    material_name   VARCHAR(200) NOT NULL COMMENT '物料名称',
    pricing_type    TINYINT DEFAULT 0 COMMENT '计价方式：0按数量 1按面积',
    quantity        INT DEFAULT 0 COMMENT '数量（按数量计价时使用）',
    width           DECIMAL(12,2) DEFAULT NULL COMMENT '宽度（按面积计价时使用）',
    height          DECIMAL(12,2) DEFAULT NULL COMMENT '高度（按面积计价时使用）',
    area            DECIMAL(12,2) DEFAULT NULL COMMENT '面积（宽×高）',
    unit_price      DECIMAL(15,2) NOT NULL COMMENT '单价',
    total_price     DECIMAL(15,2) NOT NULL COMMENT '小计金额（向上取整）',
    create_time     DATETIME DEFAULT CURRENT_TIMESTAMP,
    deleted         TINYINT NOT NULL DEFAULT 0,
    INDEX idx_record_id (record_id),
    INDEX idx_material_id (material_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='财务流水物料明细表';
