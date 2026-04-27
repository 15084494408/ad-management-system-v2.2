-- =========================================================
-- 工厂账单明细表升级：支持多计价模式（按数量/按面积/固定价）
-- 执行前确保: USE enterprise_ad;
-- =========================================================

-- 1. 新增字段
ALTER TABLE fac_factory_bill_detail
  ADD COLUMN calc_mode TINYINT NOT NULL DEFAULT 1 COMMENT '计价方式: 1=按数量 2=按面积(平方) 3=固定价格' AFTER unit_price,
  ADD COLUMN length_val DECIMAL(10,3) COMMENT '长度(m)，按面积计价时使用' AFTER calc_mode,
  ADD COLUMN width_val DECIMAL(10,3) COMMENT '宽度(m)，按面积计价时使用' AFTER length_val,
  ADD COLUMN area_sq DECIMAL(10,3) COMMENT '计算面积(㎡)，系统自动算' AFTER width_val;

-- 2. 更新示例数据：给现有记录标注正确的计价模式
UPDATE fac_factory_bill_detail SET calc_mode = 1 WHERE item_name IN ('名片印刷','宣传册印刷','画册设计','海报印刷','包装盒制作','不干胶标签');
UPDATE fac_factory_bill_detail SET calc_mode = 2, length_val = 1.8, width_val = 1.2, area_sq = 2.16 WHERE item_name = '写真喷绘';
UPDATE fac_factory_bill_detail SET calc_mode = 3 WHERE item_name = '设计服务费';

-- 3. 验证
SELECT id, item_name, calc_mode,
  CASE calc_mode
    WHEN 1 THEN '按数量'
    WHEN 2 THEN CONCAT('按面积(', area_sq, '㎡)')
    WHEN 3 THEN '固定价格'
    ELSE '未知'
  END AS 计价方式,
  quantity, unit, unit_price, amount
FROM fac_factory_bill_detail WHERE deleted = 0;
