-- ============================================================
-- 迁移脚本：零售客户升级为系统级公共客户
-- 版本：v3.2
-- 日期：2026-04-30
-- 说明：
--   1. crm_customer 新增客户类型 TYPE_RETAIL = 3（系统级公共客户）
--   2. 将已有的 '零售客户' 记录 customer_type 从 1 更新为 3
--   3. 如果不存在零售客户记录，则自动创建
-- ============================================================

-- 1. 将已有的零售客户升级为系统级类型
UPDATE crm_customer
SET customer_type = 3,
    update_time = NOW()
WHERE customer_name = '零售客户'
  AND deleted = 0
  AND customer_type != 3;

-- 2. 如果不存在零售客户，则自动创建（兼容手动清理的情况）
INSERT INTO crm_customer (customer_name, customer_type, total_amount, order_count, level, status,
    is_member, balance, total_recharge, total_consume, create_time, update_time, deleted)
SELECT '零售客户', 3, 0, 0, 1, 1, 0, 0, 0, 0, NOW(), NOW(), 0
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM crm_customer WHERE customer_name = '零售客户' AND deleted = 0 AND customer_type = 3
);

-- 3. 更新 init.sql 中的注释参考：
-- customer_type: 1=普通客户 2=工厂客户 3=零售客户（系统级公共客户）
