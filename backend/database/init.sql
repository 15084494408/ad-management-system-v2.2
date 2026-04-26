-- =========================================================
-- 企业广告管理系统 数据库初始化脚本
-- 数据库名: enterprise_ad
-- 执行前请先创建数据库: CREATE DATABASE enterprise_ad DEFAULT CHARSET utf8mb4;
-- =========================================================

USE enterprise_ad;

-- =========================================================
-- 1. 系统管理模块（用户/角色/权限）
-- =========================================================

-- 系统用户表
CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    password VARCHAR(255) NOT NULL COMMENT '密码（BCrypt加密）',
    real_name VARCHAR(100) COMMENT '真实姓名',
    phone VARCHAR(20) COMMENT '手机号',
    email VARCHAR(100) COMMENT '邮箱',
    avatar VARCHAR(500) COMMENT '头像URL',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0禁用 1正常',
    create_by BIGINT COMMENT '创建人ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by BIGINT COMMENT '更新人ID',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删 1已删',
    INDEX idx_username (username),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户表';

-- 系统角色表
CREATE TABLE IF NOT EXISTS sys_role (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '角色ID',
    role_name VARCHAR(50) NOT NULL COMMENT '角色名称',
    role_code VARCHAR(50) NOT NULL UNIQUE COMMENT '角色编码',
    description VARCHAR(255) COMMENT '描述',
    sort INT DEFAULT 0 COMMENT '排序',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0禁用 1正常',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    UNIQUE INDEX idx_role_code (role_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统角色表';

-- 系统权限表（菜单 + 按钮/接口权限）
CREATE TABLE IF NOT EXISTS sys_permission (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '权限ID',
    parent_id BIGINT NOT NULL DEFAULT 0 COMMENT '父ID（0为根）',
    name VARCHAR(100) NOT NULL COMMENT '权限名称',
    type VARCHAR(20) NOT NULL DEFAULT 'menu' COMMENT '类型：menu/button/interface',
    path VARCHAR(255) COMMENT '路由路径',
    component VARCHAR(255) COMMENT '前端组件路径',
    icon VARCHAR(100) COMMENT '图标',
    permission_code VARCHAR(100) COMMENT '权限码（如 order:create）',
    sort INT DEFAULT 0 COMMENT '排序',
    visible TINYINT DEFAULT 1 COMMENT '是否显示：0隐藏 1显示',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0禁用 1正常',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    INDEX idx_parent_id (parent_id),
    INDEX idx_permission_code (permission_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统权限表';

-- 用户-角色关联表
CREATE TABLE IF NOT EXISTS sys_user_role (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    UNIQUE INDEX idx_user_role (user_id, role_id),
    INDEX idx_user_id (user_id),
    INDEX idx_role_id (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关联表';

-- 角色-权限关联表
CREATE TABLE IF NOT EXISTS sys_role_permission (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    UNIQUE INDEX idx_role_permission (role_id, permission_id),
    INDEX idx_role_id (role_id),
    INDEX idx_permission_id (permission_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色权限关联表';

-- =========================================================
-- 2. 业务模块（订单/客户/会员/工厂/财务）
-- =========================================================

-- 客户表
CREATE TABLE IF NOT EXISTS crm_customer (
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

-- 客户标签表
CREATE TABLE IF NOT EXISTS customer_tag (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL COMMENT '标签名称',
    color VARCHAR(50) COMMENT '标签颜色',
    icon VARCHAR(100) COMMENT '标签图标',
    description VARCHAR(500) COMMENT '标签说明',
    sort INT DEFAULT 0 COMMENT '排序',
    status INT DEFAULT 1 COMMENT '状态 1正常 0禁用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0 COMMENT '逻辑删除',
    INDEX idx_sort (sort)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客户标签表';

-- 客户等级表
CREATE TABLE IF NOT EXISTS customer_level (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL COMMENT '等级名称',
    level INT NOT NULL COMMENT '等级权重（数值越大等级越高）',
    min_amount DECIMAL(12,2) DEFAULT 0 COMMENT '消费门槛',
    discount INT DEFAULT 100 COMMENT '折扣比例（如90=9折，100=原价）',
    description VARCHAR(500) COMMENT '等级说明',
    sort INT DEFAULT 0 COMMENT '排序',
    status INT DEFAULT 1 COMMENT '状态 1正常 0禁用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0 COMMENT '逻辑删除',
    INDEX idx_level (level)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客户等级表';

-- 初始客户等级
INSERT INTO customer_level (name, level, min_amount, discount, description, sort) VALUES
('钻石会员', 90, 100000.00, 85, '顶级会员，享受85折优惠', 1),
('金牌会员', 70, 50000.00, 90, '高级会员，享受9折优惠', 2),
('银牌会员', 50, 20000.00, 95, '中级会员，享受95折优惠', 3),
('普通会员', 10, 0.00, 100, '基础会员，无折扣', 4);

-- 会员表
CREATE TABLE IF NOT EXISTS mem_member (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    member_name VARCHAR(200) NOT NULL COMMENT '会员名称',
    contact_person VARCHAR(100) COMMENT '联系人',
    phone VARCHAR(20) NOT NULL COMMENT '手机号',
    level VARCHAR(20) DEFAULT '铜牌' COMMENT '等级：铜牌/银牌/金牌/钻石',
    balance DECIMAL(15,2) DEFAULT 0 COMMENT '账户余额（预存金额）',
    total_recharge DECIMAL(15,2) DEFAULT 0 COMMENT '累计充值',
    total_consume DECIMAL(15,2) DEFAULT 0 COMMENT '累计消费',
    status TINYINT DEFAULT 1 COMMENT '状态：1正常 0禁用',
    creator_id BIGINT,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    UNIQUE INDEX idx_phone (phone),
    INDEX idx_member_name (member_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会员表';

-- 会员流水表
CREATE TABLE IF NOT EXISTS mem_member_transaction (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    member_id BIGINT NOT NULL,
    type VARCHAR(20) NOT NULL COMMENT '类型：recharge充值 consume消费',
    amount DECIMAL(15,2) NOT NULL COMMENT '金额（正数）',
    balance_before DECIMAL(15,2) COMMENT '变动前余额',
    balance_after DECIMAL(15,2) COMMENT '变动后余额',
    order_id BIGINT COMMENT '关联订单ID（消费时）',
    remark VARCHAR(500) COMMENT '备注',
    creator_id BIGINT,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    INDEX idx_member_id (member_id),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会员流水表';

-- 订单主表
CREATE TABLE IF NOT EXISTS ord_order (
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

-- 订单物料明细
CREATE TABLE IF NOT EXISTS ord_order_material (
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

-- 工厂表
CREATE TABLE IF NOT EXISTS fac_factory (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    factory_name VARCHAR(200) NOT NULL COMMENT '工厂名称',
    contact_person VARCHAR(100) COMMENT '联系人',
    phone VARCHAR(20) COMMENT '电话',
    address VARCHAR(500) COMMENT '地址',
    type VARCHAR(50) COMMENT '类型：印刷/包装/广告制作',
    status TINYINT DEFAULT 1 COMMENT '状态：1正常 0暂停',
    creator_id BIGINT,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    INDEX idx_factory_name (factory_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工厂表';

-- 工厂账单表
CREATE TABLE IF NOT EXISTS fac_factory_bill (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    bill_no VARCHAR(50) NOT NULL UNIQUE COMMENT '账单编号',
    factory_id BIGINT NOT NULL COMMENT '工厂ID',
    factory_name VARCHAR(200) COMMENT '工厂名称（冗余）',
    month VARCHAR(20) NOT NULL COMMENT '账单月份（如 2026年04月）',
    total_amount DECIMAL(15,2) NOT NULL DEFAULT 0 COMMENT '账单总额',
    paid_amount DECIMAL(15,2) DEFAULT 0 COMMENT '已付金额',
    status TINYINT DEFAULT 1 COMMENT '状态：1未对账 2已对账 3部分付款 4已结清',
    reconcile_file VARCHAR(500) COMMENT '对账文件URL',
    remark VARCHAR(500) COMMENT '备注',
    creator_id BIGINT,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    INDEX idx_factory_id (factory_id),
    INDEX idx_month (month),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工厂账单表';

-- 财务流水表
CREATE TABLE IF NOT EXISTS fin_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    record_no VARCHAR(50) NOT NULL UNIQUE COMMENT '流水号',
    type VARCHAR(20) NOT NULL COMMENT '类型：income收入 expense支出',
    category VARCHAR(50) COMMENT '类别：订单收入/充值收入/退款/采购支出/工资/房租',
    amount DECIMAL(15,2) NOT NULL COMMENT '金额',
    related_id BIGINT COMMENT '关联ID（订单ID/会员ID）',
    related_name VARCHAR(200) COMMENT '关联名称',
    payment_method VARCHAR(50) COMMENT '支付方式：现金/转账/微信/支付宝',
    remark VARCHAR(500) COMMENT '备注',
    creator_id BIGINT,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    INDEX idx_type (type),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='财务流水表';

-- =========================================================
-- 3. 初始数据
-- =========================================================

-- 初始管理员用户（密码: 123456）
INSERT INTO sys_user (username, password, real_name, phone, status) VALUES
('admin', '$2b$12$MPNCwxVVTbgUfTtoplfF.e4rCXbD7zb6529bQGIrzVRlQe6MoYYIS', '系统管理员', '13800138000', 1),
('finance', '$2b$12$MPNCwxVVTbgUfTtoplfF.e4rCXbD7zb6529bQGIrzVRlQe6MoYYIS', '财务小王', '13800138001', 1),
('operator', '$2b$12$MPNCwxVVTbgUfTtoplfF.e4rCXbD7zb6529bQGIrzVRlQe6MoYYIS', '操作员小李', '13800138002', 1);

-- 初始角色
INSERT INTO sys_role (role_name, role_code, description, sort, status) VALUES
('超级管理员', 'SUPER_ADMIN', '拥有所有权限', 1, 1),
('管理员', 'ADMIN', '除系统配置外全部权限', 2, 1),
('财务', 'FINANCE', '财务和订单查看', 3, 1),
('操作员', 'OPERATOR', '订单增删改', 4, 1),
('访客', 'VIEWER', '只读权限', 5, 1);

-- 给 admin 分配 SUPER_ADMIN 角色
INSERT INTO sys_user_role (user_id, role_id) VALUES (1, 1);
INSERT INTO sys_user_role (user_id, role_id) VALUES (2, 3);
INSERT INTO sys_user_role (user_id, role_id) VALUES (3, 4);

-- 初始权限（菜单）
INSERT INTO sys_permission (id, parent_id, name, type, path, component, icon, sort, status) VALUES
-- 一级菜单
(1, 0, '仪表盘', 'menu', '/dashboard', 'dashboard/index', '📊', 1, 1),
(2, 0, '订单管理', 'menu', '/orders', 'order/index', '📋', 2, 1),
(3, 0, '客户管理', 'menu', '/customers', 'customer/index', '👥', 3, 1),
(4, 0, '会员管理', 'menu', '/members', 'member/index', '🎫', 4, 1),
(5, 0, '工厂账单', 'menu', '/factory', 'factory/index', '🏭', 5, 1),
(6, 0, '财务管理', 'menu', '/finance', 'finance/index', '💰', 6, 1),
(7, 0, '系统管理', 'menu', '/system', 'system/index', '⚙️', 7, 1);

-- 二级菜单和按钮权限
INSERT INTO sys_permission (parent_id, name, type, permission_code, sort, status) VALUES
-- 仪表盘按钮
(1, '查看仪表盘', 'button', 'dashboard:view', 1, 1),
-- 订单按钮
(2, '查看订单', 'button', 'order:list', 1, 1),
(2, '新建订单', 'button', 'order:create', 2, 1),
(2, '编辑订单', 'button', 'order:edit', 3, 1),
(2, '删除订单', 'button', 'order:delete', 4, 1),
-- 客户按钮
(3, '查看客户', 'button', 'customer:list', 1, 1),
(3, '新建客户', 'button', 'customer:create', 2, 1),
(3, '编辑客户', 'button', 'customer:edit', 3, 1),
(3, '删除客户', 'button', 'customer:delete', 4, 1),
-- 会员按钮
(4, '查看会员', 'button', 'member:list', 1, 1),
(4, '新建会员', 'button', 'member:create', 2, 1),
(4, '编辑会员', 'button', 'member:edit', 3, 1),
(4, '删除会员', 'button', 'member:delete', 4, 1),
(4, '会员充值', 'button', 'member:recharge', 5, 1),
-- 工厂按钮
(5, '查看账单', 'button', 'factory:list', 1, 1),
(5, '新建账单', 'button', 'factory:create', 2, 1),
(5, '编辑账单', 'button', 'factory:edit', 3, 1),
(5, '删除账单', 'button', 'factory:delete', 4, 1),
-- 财务按钮
(6, '查看财务', 'button', 'finance:view', 1, 1),
(6, '编辑财务', 'button', 'finance:edit', 2, 1),
-- 系统管理按钮
(7, '用户管理', 'button', 'system:user', 1, 1);

-- SUPER_ADMIN 角色拥有所有权限
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT 1, id FROM sys_permission WHERE deleted = 0;

-- ADMIN 角色拥有除系统管理外的所有权限
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT 2, id FROM sys_permission WHERE deleted = 0 AND id != 36;

-- FINANCE 角色
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT 3, id FROM sys_permission WHERE deleted = 0 AND (
    permission_code LIKE 'order:list' OR
    permission_code LIKE 'customer:list' OR
    permission_code LIKE 'finance:%' OR
    permission_code LIKE 'dashboard:view');

-- OPERATOR 角色
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT 4, id FROM sys_permission WHERE deleted = 0 AND (
    permission_code LIKE 'order:%' OR
    permission_code LIKE 'customer:%' OR
    permission_code LIKE 'dashboard:view');

-- 初始工厂数据
INSERT INTO fac_factory (factory_name, contact_person, phone, address, type, status) VALUES
('杭州印刷一厂', '张经理', '0571-88881001', '浙江省杭州市西湖区文三路123号', '印刷', 1),
('上海印刷集团', '李总监', '021-55551002', '上海市浦东新区张江高科路456号', '印刷', 1),
('北京印刷厂', '王主任', '010-66661003', '北京市朝阳区望京西路789号', '印刷', 1),
('深圳包装公司', '陈经理', '0755-88881004', '广东省深圳市南山区科技园路101号', '包装', 1),
('广州印务', '刘经理', '020-88881005', '广东省广州市天河区珠江新城花城大道88号', '广告制作', 1);

-- 初始工厂账单
INSERT INTO fac_factory_bill (bill_no, factory_id, factory_name, month, total_amount, paid_amount, status) VALUES
('FB202603001', 1, '杭州印刷一厂', '2026年03月', 86500.00, 60000.00, 1),
('FB202603002', 1, '杭州印刷一厂', '2026年02月', 72000.00, 72000.00, 4),
('FB202603003', 2, '上海印刷集团', '2026年03月', 120000.00, 50000.00, 3),
('FB202603004', 2, '上海印刷集团', '2026年02月', 98000.00, 98000.00, 4),
('FB202603005', 3, '北京印刷厂', '2026年03月', 56000.00, 0.00, 1),
('FB202603006', 4, '深圳包装公司', '2026年03月', 78000.00, 30000.00, 3),
('FB202603007', 5, '广州印务', '2026年03月', 43000.00, 43000.00, 4);

-- 初始会员
INSERT INTO mem_member (member_name, contact_person, phone, level, balance, total_recharge, status) VALUES
('杭州苏润阀门厂', '苏高于', '19818226202', '金牌', 50000.00, 100000.00, 1),
('浙江华新传媒', '王总', '13900139001', '钻石', 80000.00, 200000.00, 1),
('上海蓝图设计', '李总监', '13800138010', '银牌', 30000.00, 50000.00, 1);

-- 初始会员充值记录
INSERT INTO mem_member_transaction (member_id, type, amount, balance_before, balance_after, remark) VALUES
(1, 'recharge', 100000.00, 0.00, 50000.00, '初始充值'),
(2, 'recharge', 200000.00, 0.00, 80000.00, '初始充值'),
(3, 'recharge', 50000.00, 0.00, 30000.00, '初始充值');

-- 初始客户
INSERT INTO crm_customer (customer_name, contact_person, phone, address, industry, total_amount, order_count, level) VALUES
('杭州苏润阀门厂', '苏高于', '19818226202', '浙江台州', '工业制造', 120000.00, 15, 2),
('浙江华新传媒', '王总', '13900139001', '浙江杭州', '广告传媒', 200000.00, 25, 3),
('上海蓝图设计', '李总监', '13800138010', '上海', '设计服务', 80000.00, 10, 2),
('深圳智联招聘', '张HR', '13900139002', '广东深圳', '人力资源', 50000.00, 8, 1);

-- 初始订单
INSERT INTO ord_order (order_no, customer_id, customer_name, title, order_type, total_amount, paid_amount, status, payment_status, contact_person, contact_phone, create_time) VALUES
('ORD20260418001', 1, '杭州苏润阀门厂', '名片印刷500张 + 宣传册1000本', 1, 8500.00, 5000.00, 2, 2, '苏高于', '19818226202', NOW()),
('ORD20260418002', 2, '浙江华新传媒', '户外广告牌设计制作', 2, 35000.00, 20000.00, 2, 2, '王总', '13900139001', NOW()),
('ORD20260418003', 3, '上海蓝图设计', '企业VI全套设计', 3, 15000.00, 15000.00, 3, 3, '李总监', '13800138010', NOW());

-- 初始财务记录
INSERT INTO fin_record (record_no, type, category, amount, related_name, payment_method, remark) VALUES
('FIN20260418001', 'income', '订单收入', 15000.00, 'ORD20260418003', '转账', '企业VI设计尾款'),
('FIN20260418002', 'income', '充值收入', 10000.00, '杭州苏润阀门厂', '转账', '会员充值'),
('FIN20260418003', 'expense', '采购支出', 5000.00, '杭州印刷一厂', '转账', '印刷材料采购'),
('FIN20260418004', 'income', '订单收入', 20000.00, 'ORD20260418002', '微信', '户外广告预付款');

-- =========================================================
-- 4. 会员等级配置（可扩展）
-- =========================================================
-- 铜牌：累计充值 0~50000，享9.5折
-- 银牌：累计充值 50001~100000，享9折
-- 金牌：累计充值 100001~200000，享8.5折
-- 钻石：累计充值 200000以上，享8折

-- =========================================================
-- 5. 消息通知模块
-- =========================================================

-- 通知表
CREATE TABLE IF NOT EXISTS sys_notice (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    title       VARCHAR(200) NOT NULL COMMENT '通知标题',
    content     TEXT COMMENT '通知内容',
    type        VARCHAR(50) COMMENT '类型：system/system_order/finance/warning',
    level       TINYINT DEFAULT 1 COMMENT '级别：1普通 2重要 3紧急',
    user_id     BIGINT COMMENT '接收用户ID（null表示全部）',
    user_ids    VARCHAR(500) COMMENT '指定多个用户ID逗号分隔',
    is_read     TINYINT NOT NULL DEFAULT 0 COMMENT '是否已读：0未读 1已读',
    read_user_id BIGINT COMMENT '读取用户ID',
    read_time   DATETIME COMMENT '读取时间',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    deleted     TINYINT NOT NULL DEFAULT 0,
    INDEX idx_user_id (user_id),
    INDEX idx_is_read (is_read),
    INDEX idx_type (type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统通知表';

-- 通知设置表
CREATE TABLE IF NOT EXISTS sys_notice_setting (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id         BIGINT NOT NULL UNIQUE COMMENT '用户ID',
    order_notify    TINYINT DEFAULT 1 COMMENT '订单通知：1开启 0关闭',
    finance_notify  TINYINT DEFAULT 1 COMMENT '财务通知',
    system_notify   TINYINT DEFAULT 1 COMMENT '系统通知',
    warning_notify  TINYINT DEFAULT 1 COMMENT '预警通知',
    create_time     DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME,
    deleted         TINYINT NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户通知设置表';

-- 初始通知数据
INSERT INTO sys_notice (title, content, type, level, create_time) VALUES
('系统上线通知', '企业广告管理系统已正式上线，欢迎使用！', 'system', 2, NOW()),
('订单ORD20260418001已确认', '客户杭州苏润阀门厂的订单（名片印刷500张）已确认，请及时安排生产。', 'system_order', 1, NOW()),
('财务收款提醒', '收到客户浙江华新传媒转账2万元，请核对并更新订单状态。', 'finance', 1, NOW());

-- =========================================================
-- 6. 物料管理模块
-- =========================================================

-- 物料分类表
CREATE TABLE IF NOT EXISTS mat_category (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    name        VARCHAR(100) NOT NULL COMMENT '分类名称',
    code        VARCHAR(50) COMMENT '分类编码',
    parent_id   BIGINT DEFAULT 0 COMMENT '父分类ID',
    sort_order  INT DEFAULT 0,
    status      INT DEFAULT 1 COMMENT '状态：1正常 0禁用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    deleted     TINYINT NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='物料分类表';

-- 物料表
CREATE TABLE IF NOT EXISTS mat_material (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    name            VARCHAR(200) NOT NULL COMMENT '物料名称',
    code            VARCHAR(100) UNIQUE COMMENT '物料编码',
    category_id     BIGINT COMMENT '分类ID',
    category_name   VARCHAR(100) COMMENT '分类名称',
    spec            VARCHAR(200) COMMENT '规格型号',
    unit            VARCHAR(20) COMMENT '计量单位',
    price           DECIMAL(15,2) COMMENT '零售价',
    cost_price      DECIMAL(15,2) COMMENT '成本价',
    factory_price   DECIMAL(15,2) COMMENT '工厂价',
    stock_quantity  INT DEFAULT 0 COMMENT '库存数量',
    warning_quantity INT DEFAULT 10 COMMENT '预警库存',
    min_quantity    INT DEFAULT 5 COMMENT '最小库存',
    status          TINYINT DEFAULT 1 COMMENT '状态：1正常 0禁用',
    remark          VARCHAR(500),
    create_time     DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME,
    deleted         TINYINT NOT NULL DEFAULT 0,
    INDEX idx_category (category_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='物料库存表';

-- 库存变动记录表
CREATE TABLE IF NOT EXISTS mat_stock_log (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    material_id     BIGINT NOT NULL,
    material_name   VARCHAR(200),
    change_type     TINYINT COMMENT '变动类型：1入库 2出库 3调整',
    quantity        INT COMMENT '变动数量（负数为出库）',
    before_stock    INT COMMENT '变动前库存',
    after_stock     INT COMMENT '变动后库存',
    unit_price      DECIMAL(15,2),
    total_price     DECIMAL(15,2),
    remark          VARCHAR(500),
    operator_name   VARCHAR(100),
    create_time     DATETIME DEFAULT CURRENT_TIMESTAMP,
    deleted         TINYINT NOT NULL DEFAULT 0,
    INDEX idx_material (material_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库存变动日志';

-- 初始物料分类
INSERT INTO mat_category (name, code, sort_order) VALUES
('印刷材料', 'PRINT', 1),
('广告材料', 'AD', 2),
('耗材', 'CONSUMABLE', 3),
('包装材料', 'PACKAGE', 4);

-- 初始物料数据
INSERT INTO mat_material (name, code, category_id, category_name, spec, unit, price, cost_price, stock_quantity, warning_quantity, min_quantity, status) VALUES
('铜版纸 A4', 'MAT001', 1, '印刷材料', 'A4 105g', '张', 0.50, 0.30, 5000, 500, 200, 1),
('铜版纸 A3', 'MAT002', 1, '印刷材料', 'A3 105g', '张', 0.90, 0.55, 3000, 300, 100, 1),
('哑粉纸 A4', 'MAT003', 1, '印刷材料', 'A4 128g', '张', 0.60, 0.38, 2000, 200, 100, 1),
('KT板 60x90cm', 'MAT004', 2, '广告材料', '60x90cm 5mm', '张', 15.00, 8.00, 200, 30, 10, 1),
('写真布 1m宽', 'MAT005', 2, '广告材料', '1m宽 灯箱布', '米', 12.00, 7.00, 500, 50, 20, 1),
('墨盒（黑色）', 'MAT006', 3, '耗材', '通用型', '个', 85.00, 55.00, 20, 5, 2, 1),
('装订胶', 'MAT007', 3, '耗材', '500ml', '瓶', 35.00, 20.00, 15, 5, 3, 1);

-- =========================================================
-- 7. 设计广场模块
-- =========================================================

-- 需求单表
CREATE TABLE IF NOT EXISTS sq_requirement (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    req_no          VARCHAR(50) UNIQUE COMMENT '需求单号',
    title           VARCHAR(200) NOT NULL COMMENT '需求标题',
    description     TEXT COMMENT '需求描述',
    category        VARCHAR(50) COMMENT '类别：logo/banner/poster/card/vi',
    budget          DECIMAL(15,2) COMMENT '预算',
    deadline        DATE COMMENT '截止日期',
    status          TINYINT DEFAULT 1 COMMENT '状态：1招募中 2已选定 3进行中 4已完成 5已取消',
    publisher_id    BIGINT COMMENT '发布人ID',
    publisher_name  VARCHAR(100),
    selected_designer_id BIGINT,
    selected_designer_name VARCHAR(100),
    final_price     DECIMAL(15,2),
    create_time     DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME,
    deleted         TINYINT NOT NULL DEFAULT 0,
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设计广场需求单';

-- 设计师申请表
CREATE TABLE IF NOT EXISTS sq_application (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    requirement_id  BIGINT NOT NULL COMMENT '需求ID',
    designer_id     BIGINT COMMENT '设计师用户ID',
    designer_name   VARCHAR(100) COMMENT '设计师姓名',
    proposal        TEXT COMMENT '申请说明/提案',
    quoted_price    DECIMAL(15,2) COMMENT '报价',
    status          TINYINT DEFAULT 1 COMMENT '状态：1待审核 2已接受 3已拒绝',
    reject_reason   VARCHAR(500),
    create_time     DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME,
    deleted         TINYINT NOT NULL DEFAULT 0,
    INDEX idx_requirement (requirement_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设计师申请表';

-- 设计师收入记录表
CREATE TABLE IF NOT EXISTS sq_income (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    requirement_id  BIGINT COMMENT '需求单ID',
    designer_id     BIGINT,
    designer_name   VARCHAR(100),
    amount          DECIMAL(15,2),
    status          TINYINT DEFAULT 1 COMMENT '状态：1待打款 2已打款',
    remark          VARCHAR(500),
    create_time     DATETIME DEFAULT CURRENT_TIMESTAMP,
    deleted         TINYINT NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设计师收入记录';

-- =========================================================
-- 8. 设计文件模块
-- =========================================================

-- 设计文件表
CREATE TABLE IF NOT EXISTS des_file (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    name            VARCHAR(200) NOT NULL COMMENT '文件名',
    original_name   VARCHAR(200) COMMENT '原始文件名',
    path            VARCHAR(500) COMMENT '存储路径',
    url             VARCHAR(500) COMMENT '访问URL',
    size            BIGINT COMMENT '文件大小（字节）',
    extension       VARCHAR(50) COMMENT '文件扩展名',
    mime_type       VARCHAR(100) COMMENT 'MIME类型',
    order_id        BIGINT COMMENT '关联订单ID',
    uploader_id     BIGINT COMMENT '上传人ID',
    uploader_name   VARCHAR(100) COMMENT '上传人',
    version         INT DEFAULT 1 COMMENT '版本号',
    description     VARCHAR(500) COMMENT '文件描述',
    status          TINYINT DEFAULT 1 COMMENT '状态：1待审核 2通过 3驳回',
    remark          VARCHAR(500) COMMENT '审核备注',
    create_time     DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME,
    deleted         TINYINT NOT NULL DEFAULT 0,
    INDEX idx_order (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设计文件表';

-- 设计文件版本表
CREATE TABLE IF NOT EXISTS des_file_version (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    file_id     BIGINT NOT NULL COMMENT '文件ID',
    version     INT COMMENT '版本号',
    file_url    VARCHAR(500),
    file_size   BIGINT,
    remark      VARCHAR(500),
    creator_id  BIGINT,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    deleted     TINYINT NOT NULL DEFAULT 0,
    INDEX idx_file (file_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设计文件版本记录';

-- =========================================================
-- 9. 系统管理扩展
-- =========================================================

-- 数据字典表
CREATE TABLE IF NOT EXISTS sys_dict (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    name        VARCHAR(100) NOT NULL COMMENT '字典名称',
    code        VARCHAR(100) NOT NULL UNIQUE COMMENT '字典编码',
    description VARCHAR(500) COMMENT '描述',
    sort_order  INT DEFAULT 0,
    status      TINYINT DEFAULT 1,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME,
    deleted     TINYINT NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据字典表';

-- 数据字典项表
CREATE TABLE IF NOT EXISTS sys_dict_item (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    dict_id     BIGINT NOT NULL COMMENT '字典ID',
    label       VARCHAR(100) NOT NULL COMMENT '标签',
    value       VARCHAR(100) NOT NULL COMMENT '值',
    sort_order  INT DEFAULT 0,
    status      TINYINT DEFAULT 1,
    remark      VARCHAR(500),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    deleted     TINYINT NOT NULL DEFAULT 0,
    INDEX idx_dict (dict_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据字典项';

-- 操作日志表
CREATE TABLE IF NOT EXISTS sys_operation_log (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id         BIGINT COMMENT '操作用户ID',
    username        VARCHAR(100) COMMENT '操作用户名',
    module          VARCHAR(100) COMMENT '操作模块',
    action          VARCHAR(200) COMMENT '操作类型',
    description     VARCHAR(500) COMMENT '操作描述',
    method          VARCHAR(200) COMMENT '请求方法',
    url             VARCHAR(500) COMMENT '请求URL',
    ip              VARCHAR(50) COMMENT '操作IP',
    user_agent      VARCHAR(500) COMMENT 'UserAgent',
    param           TEXT COMMENT '请求参数',
    result          TEXT COMMENT '响应结果（摘要）',
    status          TINYINT DEFAULT 1 COMMENT '1成功 0失败',
    duration        BIGINT COMMENT '耗时（毫秒）',
    create_time     DATETIME DEFAULT CURRENT_TIMESTAMP,
    deleted         TINYINT NOT NULL DEFAULT 0,
    INDEX idx_user (user_id),
    INDEX idx_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志表';

-- 初始数据字典
INSERT INTO sys_dict (name, code, description, sort_order, status) VALUES
('订单类型', 'order_type', '订单类型枚举', 1, 1),
('订单状态', 'order_status', '订单状态枚举', 2, 1),
('支付方式', 'payment_method', '支付方式枚举', 3, 1),
('客户等级', 'customer_level', '客户等级枚举', 4, 1);

INSERT INTO sys_dict_item (dict_id, label, value, sort_order) VALUES
(1, '印刷', '1', 1), (1, '广告', '2', 2), (1, '设计', '3', 3),
(2, '待确认', '1', 1), (2, '进行中', '2', 2), (2, '已完成', '3', 3), (2, '已取消', '4', 4),
(3, '现金', 'cash', 1), (3, '转账', 'transfer', 2), (3, '微信', 'wechat', 3), (3, '支付宝', 'alipay', 4),
(4, '普通', '1', 1), (4, '银牌', '2', 2), (4, '金牌', '3', 3), (4, '钻石', '4', 4);

-- =========================================================
-- 补充表：按钮管理、数据备份、报价管理、发票管理
-- =========================================================

-- 按钮权限资源表
CREATE TABLE IF NOT EXISTS sys_button (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    name            VARCHAR(100) NOT NULL COMMENT '按钮名称',
    permission      VARCHAR(200) COMMENT '权限标识，如 system:user:add',
    type            VARCHAR(20) DEFAULT 'button' COMMENT '类型：button/menu/api',
    parent_id       BIGINT DEFAULT 0 COMMENT '上级菜单ID',
    sort            INT DEFAULT 0 COMMENT '排序',
    status          TINYINT DEFAULT 1 COMMENT '1启用 0停用',
    create_time     DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT NOT NULL DEFAULT 0,
    INDEX idx_parent (parent_id),
    INDEX idx_permission (permission)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='按钮权限资源表';

-- 数据备份记录表
CREATE TABLE IF NOT EXISTS sys_backup (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    file_name       VARCHAR(255) NOT NULL COMMENT '备份文件名',
    file_path       VARCHAR(500) COMMENT '文件存储路径',
    file_size       BIGINT DEFAULT 0 COMMENT '文件大小（字节）',
    type            VARCHAR(20) DEFAULT 'manual' COMMENT '类型：auto/manual',
    status          VARCHAR(20) DEFAULT 'success' COMMENT '状态：success/failed',
    remark          TEXT COMMENT '备注',
    operator_id     BIGINT COMMENT '操作人ID',
    operator_name   VARCHAR(50) COMMENT '操作人名称',
    create_time     DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT NOT NULL DEFAULT 0,
    INDEX idx_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据备份记录表';

-- 报价记录表
CREATE TABLE IF NOT EXISTS fin_quote (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    quote_no        VARCHAR(50) NOT NULL COMMENT '报价编号',
    customer_name   VARCHAR(100) COMMENT '客户名称',
    project_name    VARCHAR(200) COMMENT '项目名称',
    total_amount    DECIMAL(12,2) DEFAULT 0 COMMENT '报价金额',
    discount        DECIMAL(5,2) DEFAULT 100 COMMENT '折扣百分比',
    final_amount    DECIMAL(12,2) DEFAULT 0 COMMENT '最终金额',
    status          VARCHAR(20) DEFAULT 'pending' COMMENT '状态：pending/accepted/rejected/expired',
    valid_until     VARCHAR(20) COMMENT '有效期至',
    remark          TEXT COMMENT '备注',
    creator_id      BIGINT COMMENT '创建人ID',
    create_time     DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT NOT NULL DEFAULT 0,
    UNIQUE INDEX idx_quote_no (quote_no),
    INDEX idx_customer (customer_name),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报价记录表';

-- 发票记录表
CREATE TABLE IF NOT EXISTS fin_invoice (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    invoice_no      VARCHAR(50) NOT NULL COMMENT '发票编号',
    customer_name   VARCHAR(100) COMMENT '客户名称',
    type            VARCHAR(20) DEFAULT 'normal' COMMENT '类型：special专票/normal普票/receipt收据',
    amount          DECIMAL(12,2) DEFAULT 0 COMMENT '发票金额',
    tax_rate        DECIMAL(5,2) DEFAULT 0 COMMENT '税率',
    tax_amount      DECIMAL(12,2) DEFAULT 0 COMMENT '税额',
    status          VARCHAR(20) DEFAULT 'pending' COMMENT '状态：completed已开具/pending待开具/cancelled已作废',
    issue_date      VARCHAR(20) COMMENT '开具日期',
    remark          TEXT COMMENT '备注',
    creator_id      BIGINT COMMENT '创建人ID',
    create_time     DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT NOT NULL DEFAULT 0,
    UNIQUE INDEX idx_invoice_no (invoice_no),
    INDEX idx_customer (customer_name),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='发票记录表';

