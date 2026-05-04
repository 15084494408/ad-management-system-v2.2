-- =====================================================
-- 补录历史订单收款到 fin_record 表
-- 背景：之前版本的 addPayment() 没有同步写入 fin_record，
--       导致历史已收款订单在"收款管理"和"统一流水"中看不到。
-- 执行方式：仅在已部署的旧数据库上执行一次，新部署无需执行。
-- 注意：执行前请先备份数据！
-- =====================================================

-- 1. 查看需要补录的订单数量（预览，不写入）
SELECT
    COUNT(*) AS total_count,
    SUM(o.paid_amount) AS total_amount
FROM ord_order o
WHERE o.deleted = 0
  AND o.paid_amount > 0
  AND NOT EXISTS (
    SELECT 1 FROM fin_record f
    WHERE f.deleted = 0
      AND f.type = 'income'
      AND f.category = '订单收款'
      AND f.related_id = o.id
  );

-- 2. 正式补录：为每个有已付金额但未在 fin_record 中有对应记录的订单创建一条收入流水
--    使用订单更新时间作为收款时间的近似值
INSERT INTO fin_record (
    record_no, type, category, amount,
    related_id, related_name, payment_method, remark,
    creator_id, create_time, deleted
)
SELECT
    CONCAT('ORD', UNIX_TIMESTAMP(o.update_time) * 1000 + o.id) AS record_no,
    'income' AS type,
    '订单收款' AS category,
    o.paid_amount AS amount,
    o.id AS related_id,
    o.order_no AS related_name,
    IFNULL(o.pay_method, '') AS payment_method,
    CONCAT('订单: ', IFNULL(o.title, ''), ' | 客户: ', IFNULL(o.customer_name, ''), ' [历史数据补录]') AS remark,
    o.creator_id,
    o.update_time AS create_time,
    0 AS deleted
FROM ord_order o
WHERE o.deleted = 0
  AND o.paid_amount > 0
  AND NOT EXISTS (
    SELECT 1 FROM fin_record f
    WHERE f.deleted = 0
      AND f.type = 'income'
      AND f.category = '订单收款'
      AND f.related_id = o.id
  );

-- 3. 验证补录结果
SELECT
    f.id, f.record_no, f.category, f.amount, f.related_id, f.related_name, f.remark, f.create_time
FROM fin_record f
WHERE f.category = '订单收款'
  AND f.remark LIKE '%历史数据补录%'
ORDER BY f.create_time DESC;
