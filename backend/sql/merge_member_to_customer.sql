-- =========================================================
-- 会员合并到客户表 - 数据迁移脚本
-- 执行前请备份数据库！
-- 适用：MySQL 5.7 / 8.0
-- =========================================================

-- Step 1: crm_customer 表增加会员字段
-- 使用存储过程安全添加列（列已存在则跳过）
DROP PROCEDURE IF EXISTS merge_add_customer_member_columns;
DELIMITER $$
CREATE PROCEDURE merge_add_customer_member_columns()
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'crm_customer' AND COLUMN_NAME = 'is_member') THEN
        ALTER TABLE crm_customer ADD COLUMN is_member TINYINT NOT NULL DEFAULT 0 COMMENT '是否为会员：0否 1是' AFTER `status`;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'crm_customer' AND COLUMN_NAME = 'member_level') THEN
        ALTER TABLE crm_customer ADD COLUMN member_level VARCHAR(20) DEFAULT 'normal' COMMENT '会员等级：normal/silver/gold/diamond' AFTER is_member;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'crm_customer' AND COLUMN_NAME = 'balance') THEN
        ALTER TABLE crm_customer ADD COLUMN balance DECIMAL(15,2) DEFAULT 0 COMMENT '会员余额' AFTER member_level;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'crm_customer' AND COLUMN_NAME = 'total_recharge') THEN
        ALTER TABLE crm_customer ADD COLUMN total_recharge DECIMAL(15,2) DEFAULT 0 COMMENT '累计充值' AFTER balance;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'crm_customer' AND COLUMN_NAME = 'total_consume') THEN
        ALTER TABLE crm_customer ADD COLUMN total_consume DECIMAL(15,2) DEFAULT 0 COMMENT '累计消费（会员余额消费）' AFTER total_recharge;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'crm_customer' AND INDEX_NAME = 'idx_is_member') THEN
        ALTER TABLE crm_customer ADD INDEX idx_is_member (is_member);
    END IF;
END$$
DELIMITER ;
CALL merge_add_customer_member_columns();
DROP PROCEDURE IF EXISTS merge_add_customer_member_columns;

-- Step 2: mem_member_transaction 表增加 customer_id 字段
DROP PROCEDURE IF EXISTS merge_add_transaction_customer_id;
DELIMITER $$
CREATE PROCEDURE merge_add_transaction_customer_id()
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'mem_member_transaction' AND COLUMN_NAME = 'customer_id') THEN
        ALTER TABLE mem_member_transaction ADD COLUMN customer_id BIGINT COMMENT '客户ID（关联 crm_customer.id）' AFTER id;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'mem_member_transaction' AND INDEX_NAME = 'idx_customer_id') THEN
        ALTER TABLE mem_member_transaction ADD INDEX idx_customer_id (customer_id);
    END IF;
END$$
DELIMITER ;
CALL merge_add_transaction_customer_id();
DROP PROCEDURE IF EXISTS merge_add_transaction_customer_id;

-- Step 3: 将 mem_member 数据合并到 crm_customer
-- 策略：按 phone 匹配 -> 匹配到则更新，未匹配到则创建新客户记录
UPDATE crm_customer c
INNER JOIN mem_member m ON c.phone = m.phone AND c.deleted = 0 AND m.deleted = 0
SET
    c.is_member = 1,
    c.member_level = COALESCE(NULLIF(m.level, ''), 'normal'),
    c.balance = COALESCE(m.balance, 0),
    c.total_recharge = COALESCE(m.total_recharge, 0),
    c.total_consume = COALESCE(m.total_consume, 0),
    c.update_time = NOW();

-- Step 3b: 对于 mem_member 中 phone 为空或未匹配到的会员，创建新客户记录
INSERT INTO crm_customer (customer_name, contact_person, phone, customer_type, is_member, member_level, balance, total_recharge, total_consume, total_amount, order_count, status, create_time, update_time)
SELECT
    m.member_name,
    m.contact_person,
    m.phone,
    1,
    1,
    COALESCE(NULLIF(m.level, ''), 'normal'),
    COALESCE(m.balance, 0),
    COALESCE(m.total_recharge, 0),
    COALESCE(m.total_consume, 0),
    COALESCE(m.total_recharge, 0) + COALESCE(m.total_consume, 0),
    0,
    COALESCE(m.status, 1),
    COALESCE(m.create_time, NOW()),
    NOW()
FROM mem_member m
WHERE m.deleted = 0
  AND NOT EXISTS (
    SELECT 1 FROM crm_customer c
    WHERE c.phone = m.phone AND c.deleted = 0 AND c.is_member = 1
  )
  AND m.id NOT IN (
    SELECT mm.id FROM mem_member mm
    INNER JOIN crm_customer c ON mm.phone = c.phone AND c.deleted = 0
  );

-- Step 4: 回填 mem_member_transaction.customer_id
-- 4a: 有 phone 的会员，通过 phone 找到合并后的客户
UPDATE mem_member_transaction t
INNER JOIN mem_member m ON t.member_id = m.id
INNER JOIN crm_customer c ON c.phone = m.phone AND c.deleted = 0 AND c.is_member = 1
SET t.customer_id = c.id;

-- 4b: 无 phone 的会员，通过上一步新建的客户记录关联（按 member_name 匹配）
UPDATE mem_member_transaction t
INNER JOIN mem_member m ON t.member_id = m.id
INNER JOIN crm_customer c ON c.customer_name = m.member_name AND c.deleted = 0 AND c.is_member = 1 AND c.phone IS NULL
SET t.customer_id = c.id
WHERE t.customer_id IS NULL;

-- Step 5: 回填 ord_order 中只有 member_id 没有 customer_id 的订单
UPDATE ord_order o
INNER JOIN mem_member m ON o.member_id = m.id
INNER JOIN crm_customer c ON c.phone = m.phone AND c.deleted = 0 AND c.is_member = 1
SET o.customer_id = c.id
WHERE o.customer_id IS NULL AND o.member_id IS NOT NULL AND o.deleted = 0;

-- Step 6: 将 mem_member 标记为已迁移（软删除，保留备份）
UPDATE mem_member SET deleted = 1, update_time = NOW() WHERE deleted = 0;

-- =========================================================
-- 迁移完成后的验证查询（手动执行检查）
-- =========================================================
-- SELECT COUNT(*) AS merged_members FROM crm_customer WHERE is_member = 1 AND deleted = 0;
-- SELECT COUNT(*) AS orphan_transactions FROM mem_member_transaction WHERE customer_id IS NULL AND deleted = 0;
-- SELECT COUNT(*) AS orders_without_customer FROM ord_order WHERE customer_id IS NULL AND deleted = 0;
