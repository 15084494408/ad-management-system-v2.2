-- 客户管理 + 订单管理 重新初始化
-- 在 IDEA Database 控制台或 Navicat 中执行

USE enterprise_ad;

-- 删除旧表（先删有依赖的物料表）
DROP TABLE IF EXISTS ord_order_material;
DROP TABLE IF EXISTS ord_order;
DROP TABLE IF EXISTS crm_customer;

-- 重建客户表
CREATE TABLE crm_customer (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    customer_name VARCHAR(200) NOT NULL COMMENT '客户名称',
    contact_person VARCHAR(100) COMMENT '联系人',
    phone VARCHAR(20) COMMENT '手机号',
    telephone VARCHAR(20) COMMENT '座机',
    email VARCHAR(100) COMMENT '邮箱',
    address VARCHAR(500) COMMENT '地址',
    industry VARCHAR(100) COMMENT '行业',
    total_amount DECIMAL(15,2) DEFAULT 0 COMMENT '累计消费金额',
    order_count INT DEFAULT 0 COMMENT '订单数量',
    level TINYINT DEFAULT 1 COMMENT '等级：1普通 2VIP 3战略',
    status TINYINT DEFAULT 1 COMMENT '状态：1正常 0禁用',
    creator_id BIGINT,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    INDEX idx_customer_name (customer_name),
    INDEX idx_phone (phone)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客户表';

-- 重建订单主表（含新字段）
CREATE TABLE ord_order (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_no VARCHAR(50) NOT NULL UNIQUE COMMENT '订单编号',
    customer_id BIGINT COMMENT '客户ID',
    customer_name VARCHAR(200) COMMENT '客户名称（冗余）',
    member_id BIGINT COMMENT '会员ID',
    title VARCHAR(500) NOT NULL COMMENT '订单标题',
    description TEXT COMMENT '订单描述',
    order_type TINYINT DEFAULT 1 COMMENT '类型：1印刷 2广告 3设计',
    total_amount DECIMAL(15,2) NOT NULL DEFAULT 0 COMMENT '订单总额',
    paid_amount DECIMAL(15,2) DEFAULT 0 COMMENT '已付金额',
    discount_amount DECIMAL(15,2) DEFAULT 0 COMMENT '优惠金额',
    status TINYINT DEFAULT 1 COMMENT '状态：1待确认 2进行中 3已完成 4已取消',
    payment_status TINYINT DEFAULT 1 COMMENT '支付：1未付 2部分付 3已付清',
    contact_person VARCHAR(100) COMMENT '联系人',
    contact_phone VARCHAR(20) COMMENT '联系电话',
    delivery_address VARCHAR(500) COMMENT '交付地址',
    delivery_date DATE COMMENT '交付日期',
    designer_name VARCHAR(100) COMMENT '设计师',
    priority TINYINT DEFAULT 1 COMMENT '优先级：1普通 2紧急 3加急',
    source TINYINT DEFAULT 1 COMMENT '来源：1门店创建 2设计广场',
    quote_amount DECIMAL(15,2) DEFAULT 0 COMMENT '报价金额',
    deposit_amount DECIMAL(15,2) DEFAULT 0 COMMENT '定金金额',
    pay_method VARCHAR(50) COMMENT '付款方式：全款/预付50%/预付30%/货到付款/会员余额抵扣',
    invoice_type VARCHAR(50) COMMENT '开票类型：无需开票/增值税普通发票/增值税专用发票',
    remark TEXT COMMENT '备注',
    creator_id BIGINT,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    INDEX idx_order_no (order_no),
    INDEX idx_customer_id (customer_id),
    INDEX idx_customer_name (customer_name),
    INDEX idx_status (status),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单主表';

-- 重建订单物料明细表
CREATE TABLE ord_order_material (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    material_name VARCHAR(200) NOT NULL COMMENT '物料名称',
    spec VARCHAR(200) COMMENT '规格',
    unit VARCHAR(20) COMMENT '单位',
    quantity DECIMAL(10,2) DEFAULT 1 COMMENT '数量',
    unit_price DECIMAL(15,2) DEFAULT 0 COMMENT '单价',
    amount DECIMAL(15,2) DEFAULT 0 COMMENT '小计金额',
    remark VARCHAR(500) COMMENT '备注',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    INDEX idx_order_id (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单物料明细表';

-- 插入初始客户
INSERT INTO crm_customer (customer_name, contact_person, phone, address, industry, total_amount, order_count, level) VALUES
('杭州苏润阀门厂', '苏高于', '19818226202', '浙江台州', '工业制造', 120000.00, 15, 2),
('浙江华新传媒', '王总', '13900139001', '浙江杭州', '广告传媒', 200000.00, 25, 3),
('上海蓝图设计', '李总监', '13800138010', '上海', '设计服务', 80000.00, 10, 2),
('深圳智联招聘', '张HR', '13900139002', '广东深圳', '人力资源', 50000.00, 8, 1);

-- 插入初始订单（含新字段）
INSERT INTO ord_order (order_no, customer_id, customer_name, title, order_type, total_amount, paid_amount, discount_amount, status, payment_status, contact_person, contact_phone, delivery_address, delivery_date, designer_name, priority, source, quote_amount, deposit_amount, pay_method, invoice_type, remark, create_time) VALUES
('DD20260422001', 1, '杭州苏润阀门厂', '名片印刷500张 + 宣传册1000本', 1, 8500.00, 5000.00, 0, 2, 2, '苏高于', '19818226202', '浙江台州玉环市', '2026-04-25', '设计师小林', 1, 1, 8500.00, 3000.00, '预付50%', '增值税普通发票', '客户要求铜版纸双面印刷', NOW()),
('DD20260422002', 2, '浙江华新传媒', '户外广告牌设计制作', 2, 35000.00, 20000.00, 1000.00, 2, 2, '王总', '13900139001', '浙江杭州市西湖区', '2026-05-10', '设计师小陈', 2, 1, 36000.00, 10000.00, '预付30%', '增值税专用发票', '含安装服务', NOW()),
('DD20260422003', 3, '上海蓝图设计', '企业VI全套设计', 3, 15000.00, 15000.00, 0, 3, 3, '李总监', '13800138010', '上海市浦东新区', NULL, '设计师小王', 1, 1, 15000.00, 5000.00, '全款', '无需开票', '含Logo+名片+信封+信纸', NOW()),
('DD20260422004', 1, '杭州苏润阀门厂', '产品画册设计印刷2000本', 1, 12000.00, 0, 500.00, 1, 1, '苏高于', '19818226202', '浙江台州玉环市', '2026-05-01', NULL, 1, 1, 12500.00, 0, '货到付款', '无需开票', '客户提供素材', NOW()),
('DD20260422005', 4, '深圳智联招聘', '招聘海报设计与印刷', 1, 3200.00, 3200.00, 0, 3, 3, '张HR', '13900139002', '广东深圳市南山区', '2026-04-20', '设计师小林', 1, 1, 3200.00, 1500.00, '全款', '无需开票', 'A3尺寸KT板', NOW());

-- 插入订单物料明细
INSERT INTO ord_order_material (order_id, material_name, spec, unit, quantity, unit_price, amount) VALUES
(1, '铜版纸名片', '90x54mm 300g', '张', 500, 2.00, 1000.00),
(1, '铜版纸宣传册', 'A4 200g', '本', 1000, 7.50, 7500.00),
(2, '户外写真', '3m x 5m', '张', 2, 5000.00, 10000.00),
(2, '安装费', '含人工', '次', 1, 5000.00, 5000.00),
(2, '桁架租赁', '3m x 3m', '套', 1, 10000.00, 10000.00),
(3, 'VI设计', '全套', '套', 1, 15000.00, 15000.00),
(4, '铜版纸画册', 'A4 250g', '本', 2000, 5.50, 11000.00),
(4, '装订费', '胶装', '本', 2000, 0.50, 1000.00),
(5, 'KT板海报', 'A3 5mm', '张', 50, 15.00, 750.00),
(5, '设计费', 'A3海报', '张', 1, 500.00, 500.00),
(5, '写真输出', 'A3 高清', '张', 50, 39.00, 1950.00);
