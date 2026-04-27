-- =========================================================
-- 工厂账单明细表 完整初始化（建表 + 多计价模式 + 示例数据）
-- 在 MySQL 客户端中执行：
--   USE enterprise_ad;
--   SOURCE E:\dev\Project\ad-management-system-v2.2-main\database\init_bill_detail_full.sql;
-- =========================================================

USE enterprise_ad;

-- =========================================================
-- Step 1: 创建明细表（含多计价模式字段）
-- =========================================================
CREATE TABLE IF NOT EXISTS fac_factory_bill_detail (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    bill_id     BIGINT NOT NULL COMMENT '关联账单ID（fac_factory_bill.id）',
    bill_no     VARCHAR(50) COMMENT '账单编号（冗余，方便查询）',
    record_date DATE NOT NULL COMMENT '登记日期（如 2026-04-01）',
    item_name   VARCHAR(200) NOT NULL COMMENT '项目名称（如：名片印刷/宣传册）',
    spec        VARCHAR(200) COMMENT '规格说明',
    quantity    DECIMAL(10,2) DEFAULT 1 COMMENT '数量（按数量计价时使用）',
    unit        VARCHAR(20) COMMENT '单位（张/本/项/平方米）',
    unit_price  DECIMAL(15,2) DEFAULT 0 COMMENT '单价（按数量=每件单价，按面积=每㎡单价）',
    calc_mode   TINYINT NOT NULL DEFAULT 1 COMMENT '计价方式: 1=按数量 2=按面积(平方) 3=固定价格',
    length_val  DECIMAL(10,3) COMMENT '长度(m)，按面积计价时使用',
    width_val   DECIMAL(10,3) COMMENT '宽度(m)，按面积计价时使用',
    area_sq     DECIMAL(10,3) COMMENT '计算面积(㎡)，系统自动算',
    amount      DECIMAL(15,2) DEFAULT 0 COMMENT '小计金额（根据 calcMode 自动计算）',
    remark      VARCHAR(500) COMMENT '备注',
    creator_id  BIGINT,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted     TINYINT NOT NULL DEFAULT 0,
    INDEX idx_bill_id (bill_id),
    INDEX idx_bill_no (bill_no),
    INDEX idx_record_date (record_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工厂账单明细（每日登记记录，支持多计价模式）';

-- =========================================================
-- Step 2: 插入示例数据（bill_id=1，对应 FB202603001）
-- 计价模式说明：1=按数量  2=按面积(平方)  3=固定价格
-- =========================================================
INSERT INTO fac_factory_bill_detail 
(bill_id, bill_no, record_date, item_name, spec, quantity, unit, unit_price, calc_mode, length_val, width_val, area_sq, amount, remark) 
VALUES
-- 按数量计价（calc_mode=1）
(1, 'FB202603001', '2026-03-02', '名片印刷', '铜版纸 A4 双面彩印', 500, '张', 0.50, 1, NULL, NULL, NULL, 250.00, '加急单'),
(1, 'FB202603001', '2026-03-05', '宣传册印刷', 'A4 16P 100g铜版 覆膜', 1000, '本', 8.00, 1, NULL, NULL, NULL, 8000.00, ''),
(1, 'FB202603001', '2026-03-10', '画册设计', 'A4 24P 企业宣传画册', 500, '本', 15.00, 1, NULL, NULL, NULL, 7500.00, '含设计费'),
(1, 'FB202603001', '2026-03-15', '海报印刷', '60x90cm 157g铜版 覆亮膜', 200, '张', 12.00, 1, NULL, NULL, NULL, 2400.00, ''),
(1, 'FB202603001', '2026-03-20', '包装盒制作', '20x15x5cm 白卡纸 烫金', 300, '个', 3.50, 1, NULL, NULL, NULL, 1050.00, '含刀模费'),
(1, 'FB202603001', '2026-03-25', '不干胶标签', '50x30mm 铜版纸 卷装', 5000, '张', 0.12, 1, NULL, NULL, NULL, 600.00, ''),
-- 按面积计价（calc_mode=2，广告材料常用）
(1, 'FB202603001', '2026-03-28', '写真喷绘', '1.8x1.2m 灯箱布', NULL, '平方米', 45.00, 2, 1.800, 1.200, 2.160, 97.20, ''),
(1, 'FB202603001', '2026-03-29', '户外背胶', '2.0x1.5m 防水背胶', NULL, '平方米', 38.00, 2, 2.000, 1.500, 3.000, 114.00, '户外安装'),
(1, 'FB202603001', '2026-03-29', 'KT板展板', '1.2x0.8m 5mm KT板', NULL, '平方米', 25.00, 2, 1.200, 0.800, 0.960, 24.00, ''),
-- 固定价格（calc_mode=3，一口价）
(1, 'FB202603001', '2026-03-30', '设计服务费', 'VI设计基础部分', NULL, '项', 72000.00, 3, NULL, NULL, NULL, 72000.00, '含3套方案修改'),
(1, 'FB202603001', '2026-03-30', '安装服务费', '展厅整体安装调试', NULL, '项', 3500.00, 3, NULL, NULL, NULL, 3500.00, '含人工+运输');

-- =========================================================
-- Step 3: 验证数据
-- =========================================================
SELECT 
    id, 
    item_name, 
    CASE calc_mode
        WHEN 1 THEN CONCAT('按数量: ', quantity, unit, ' × ¥', unit_price)
        WHEN 2 THEN CONCAT('按面积: ', length_val, 'm×', width_val, 'm=', area_sq, '㎡ × ¥', unit_price, '/㎡')
        WHEN 3 THEN CONCAT('固定价: ¥', unit_price)
    END AS 计价详情,
    amount AS 小计
FROM fac_factory_bill_detail 
WHERE deleted = 0 AND bill_no = 'FB202603001'
ORDER BY record_date;
