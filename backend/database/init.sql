-- =========================================================
-- 企业广告管理系统 数据库初始化脚本（完整版）
-- 数据库名: enterprise_ad
-- 执行前请先创建数据库: CREATE DATABASE enterprise_ad DEFAULT CHARSET utf8mb4;
-- 版本: v2.4 (2026-04-28 逻辑删除表唯一索引改造)
-- 变更:
--   - [v2.2] 新增 fac_factory_bill_detail 表（工厂账单明细，支持3种计价模式）
--   - [v2.2] crm_customer 增加 customer_type / factory_type 字段（合并客户+工厂）
--   - [v2.2] 删除 fac_factory 表（已合并到 crm_customer）
--   - [v2.2] fac_factory_bill 增加 customer_id 字段
--   - [v2.2] mat_stock_log 包含 operator_id 字段
--   - [v2.2] mat_category 包含 status 字段
--   - [v2.2] 补全权限码
--   - [v2.3] 清除所有假数据INSERT，仅保留系统基础数据+零售客户
--   - [v2.3] 前端：客户列表新增超级管理员删除按钮
--   - [v2.3] 前端：工作台未收款订单提醒改为两列网格布局
--   - [v2.4] 逻辑删除表唯一索引改为条件唯一索引（CASE WHEN deleted=0）
--           涉及 11 张表：sys_user/sys_role/sys_dict/mem_member/ord_order/
--           fac_factory_bill/fin_record/fin_quote/fin_invoice/mat_material/
--           sq_requirement/sys_notice_setting
-- =========================================================

USE enterprise_ad;

-- =========================================================
-- 1. 系统管理模块（9 张表）
-- =========================================================

-- 1.1 系统用户表
CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
    username VARCHAR(50) NOT NULL COMMENT '用户名',
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
    UNIQUE INDEX uk_user_username ((CASE WHEN deleted = 0 THEN username ELSE NULL END)),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户表';

-- 1.2 系统角色表
CREATE TABLE IF NOT EXISTS sys_role (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '角色ID',
    role_name VARCHAR(50) NOT NULL COMMENT '角色名称',
    role_code VARCHAR(50) NOT NULL COMMENT '角色编码',
    description VARCHAR(255) COMMENT '描述',
    sort INT DEFAULT 0 COMMENT '排序',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0禁用 1正常',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    UNIQUE INDEX uk_role_code ((CASE WHEN deleted = 0 THEN role_code ELSE NULL END))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统角色表';

-- 1.3 系统权限表（菜单 + 按钮/接口权限）
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

-- 1.4 用户-角色关联表
CREATE TABLE IF NOT EXISTS sys_user_role (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    UNIQUE INDEX idx_user_role (user_id, role_id),
    INDEX idx_user_id (user_id),
    INDEX idx_role_id (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关联表';

-- 1.5 角色-权限关联表
CREATE TABLE IF NOT EXISTS sys_role_permission (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    UNIQUE INDEX idx_role_permission (role_id, permission_id),
    INDEX idx_role_id (role_id),
    INDEX idx_permission_id (permission_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色权限关联表';

-- 1.6 按钮权限资源表
CREATE TABLE IF NOT EXISTS sys_button (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL COMMENT '按钮名称',
    permission VARCHAR(200) COMMENT '权限标识，如 system:user:add',
    type VARCHAR(20) DEFAULT 'button' COMMENT '类型：button/menu/api',
    parent_id BIGINT DEFAULT 0 COMMENT '上级菜单ID',
    sort INT DEFAULT 0 COMMENT '排序',
    status TINYINT DEFAULT 1 COMMENT '1启用 0停用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    INDEX idx_parent (parent_id),
    INDEX idx_permission (permission)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='按钮权限资源表';

-- 1.7 数据字典表
CREATE TABLE IF NOT EXISTS sys_dict (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL COMMENT '字典名称',
    code VARCHAR(100) NOT NULL COMMENT '字典编码',
    description VARCHAR(500) COMMENT '描述',
    sort_order INT DEFAULT 0,
    status TINYINT DEFAULT 1,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME,
    deleted TINYINT NOT NULL DEFAULT 0,
    UNIQUE INDEX uk_dict_code ((CASE WHEN deleted = 0 THEN code ELSE NULL END))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据字典表';

-- 1.8 数据字典项表
CREATE TABLE IF NOT EXISTS sys_dict_item (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    dict_id BIGINT NOT NULL COMMENT '字典ID',
    label VARCHAR(100) NOT NULL COMMENT '标签',
    value VARCHAR(100) NOT NULL COMMENT '值',
    sort_order INT DEFAULT 0,
    status TINYINT DEFAULT 1,
    remark VARCHAR(500),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    INDEX idx_dict (dict_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据字典项';

-- 1.9 操作日志表
CREATE TABLE IF NOT EXISTS sys_operation_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT COMMENT '操作用户ID',
    username VARCHAR(100) COMMENT '操作用户名',
    module VARCHAR(100) COMMENT '操作模块',
    action VARCHAR(200) COMMENT '操作类型',
    description VARCHAR(500) COMMENT '操作描述',
    method VARCHAR(200) COMMENT '请求方法',
    url VARCHAR(500) COMMENT '请求URL',
    ip VARCHAR(50) COMMENT '操作IP',
    user_agent VARCHAR(500) COMMENT 'UserAgent',
    param TEXT COMMENT '请求参数',
    result TEXT COMMENT '响应结果（摘要）',
    status TINYINT DEFAULT 1 COMMENT '1成功 0失败',
    duration BIGINT COMMENT '耗时（毫秒）',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    INDEX idx_user (user_id),
    INDEX idx_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志表';


-- =========================================================
-- 2. 客户管理模块（3 张表）
-- 注意：已合并原 fac_factory 表，crm_customer 增加 customer_type / factory_type
-- =========================================================

-- 2.1 客户表（含工厂客户字段）
CREATE TABLE IF NOT EXISTS crm_customer (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    customer_name VARCHAR(200) NOT NULL COMMENT '客户名称',
    contact_person VARCHAR(100) COMMENT '联系人',
    phone VARCHAR(20) COMMENT '手机号',
    telephone VARCHAR(20) COMMENT '座机',
    email VARCHAR(100) COMMENT '邮箱',
    address VARCHAR(500) COMMENT '地址',
    industry VARCHAR(100) COMMENT '行业',
    customer_type TINYINT NOT NULL DEFAULT 1 COMMENT '客户类型：1=普通客户 2=工厂客户',
    factory_type VARCHAR(50) COMMENT '工厂类型（印刷/包装/广告制作，仅工厂客户）',
    total_amount DECIMAL(15,2) DEFAULT 0 COMMENT '累计消费金额',
    order_count INT DEFAULT 0 COMMENT '订单数量',
    level TINYINT DEFAULT 1 COMMENT '等级：1普通 2VIP 3战略',
    status TINYINT DEFAULT 1 COMMENT '状态：1正常 0禁用',
    creator_id BIGINT,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    INDEX idx_customer_name (customer_name),
    INDEX idx_phone (phone),
    INDEX idx_customer_type (customer_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客户表（含普通客户+工厂客户）';

-- 2.2 客户标签表
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

-- 2.3 客户等级表
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


-- =========================================================
-- 3. 会员管理模块（2 张表）
-- =========================================================

-- 3.1 会员表
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
    UNIQUE INDEX uk_member_phone ((CASE WHEN deleted = 0 THEN phone ELSE NULL END)),
    INDEX idx_member_name (member_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会员表';

-- 3.2 会员流水表
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


-- =========================================================
-- 4. 订单管理模块（2 张表）
-- =========================================================

-- 4.1 订单主表
CREATE TABLE IF NOT EXISTS ord_order (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_no VARCHAR(50) NOT NULL COMMENT '订单编号',
    customer_id BIGINT COMMENT '客户ID',
    customer_name VARCHAR(200) COMMENT '客户名称（冗余）',
    member_id BIGINT COMMENT '会员ID',
    title VARCHAR(500) NOT NULL COMMENT '订单标题',
    description TEXT COMMENT '订单描述',
    order_type TINYINT DEFAULT 1 COMMENT '类型：1印刷 2广告 3设计',
    total_amount DECIMAL(15,2) NOT NULL DEFAULT 0 COMMENT '订单总额',
    paid_amount DECIMAL(15,2) DEFAULT 0 COMMENT '已付金额',
    discount_amount DECIMAL(15,2) DEFAULT 0 COMMENT '优惠金额',
    rounding_amount DECIMAL(15,2) DEFAULT 0 COMMENT '抹零金额',
    status TINYINT DEFAULT 1 COMMENT '状态：1待确认 2进行中 3已完成 4已取消',
    payment_status TINYINT DEFAULT 1 COMMENT '支付：1未付 2部分付 3已付清 4已抹零结清',
    contact_person VARCHAR(100) COMMENT '联系人',
    contact_phone VARCHAR(20) COMMENT '联系电话',
    delivery_address VARCHAR(500) COMMENT '交付地址',
    delivery_date DATE COMMENT '交付日期',
    designer_name VARCHAR(100) COMMENT '设计师',
    designer_id BIGINT COMMENT '设计师用户ID（关联sys_user）',
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
    UNIQUE INDEX uk_order_no ((CASE WHEN deleted = 0 THEN order_no ELSE NULL END)),
    INDEX idx_customer_id (customer_id),
    INDEX idx_customer_name (customer_name),
    INDEX idx_status (status),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单主表';

-- 4.2 订单物料明细表
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


-- =========================================================
-- 5. 工厂账单模块（3 张表）
-- 注意：fac_factory 已删除，数据合并到 crm_customer（customer_type=2）
-- =========================================================

-- 5.1 工厂账单主表（增加 customer_id 字段）
CREATE TABLE IF NOT EXISTS fac_factory_bill (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    bill_no VARCHAR(50) NOT NULL COMMENT '账单编号',
    -- 以下两个字段保留兼容性，但主要使用 customer_id
    factory_id BIGINT COMMENT '原工厂ID（保留兼容）',
    factory_name VARCHAR(200) COMMENT '工厂名称（冗余）',
    -- 新增：关联合并后的客户ID
    customer_id BIGINT COMMENT '客户ID（关联crm_customer，customer_type=2为工厂客户）',
    -- 业务员信息
    salesman_id BIGINT COMMENT '业务员ID（关联fac_factory_salesman）',
    salesman_name VARCHAR(100) COMMENT '业务员姓名（冗余）',
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
    UNIQUE INDEX uk_bill_no ((CASE WHEN deleted = 0 THEN bill_no ELSE NULL END)),
    INDEX idx_factory_id (factory_id),
    INDEX idx_customer_id (customer_id),
    INDEX idx_month (month),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工厂账单表';

-- 5.2 工厂账单明细表（每日登记记录，支持3种计价模式）
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

-- 5.3 工厂业务员表
CREATE TABLE IF NOT EXISTS fac_factory_salesman (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL COMMENT '业务员姓名',
    phone VARCHAR(20) COMMENT '联系电话',
    email VARCHAR(100) COMMENT '邮箱',
    factory_id BIGINT COMMENT '所属工厂ID（关联crm_customer.id where customer_type=2）',
    status TINYINT DEFAULT 1 COMMENT '状态：1启用 0禁用',
    remark VARCHAR(500) COMMENT '备注',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    INDEX idx_factory_id (factory_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工厂业务员表';


-- =========================================================
-- 6. 财务管理模块（3 张表）
-- =========================================================

-- 6.1 财务流水表
CREATE TABLE IF NOT EXISTS fin_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    record_no VARCHAR(50) NOT NULL COMMENT '流水号',
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
    UNIQUE INDEX uk_record_no ((CASE WHEN deleted = 0 THEN record_no ELSE NULL END)),
    INDEX idx_type (type),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='财务流水表';

-- 6.2 报价记录表
CREATE TABLE IF NOT EXISTS fin_quote (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    quote_no VARCHAR(50) NOT NULL COMMENT '报价编号',
    customer_name VARCHAR(100) COMMENT '客户名称',
    project_name VARCHAR(200) COMMENT '项目名称',
    total_amount DECIMAL(12,2) DEFAULT 0 COMMENT '报价金额',
    discount DECIMAL(5,2) DEFAULT 100 COMMENT '折扣百分比',
    final_amount DECIMAL(12,2) DEFAULT 0 COMMENT '最终金额',
    status VARCHAR(20) DEFAULT 'pending' COMMENT '状态：pending/accepted/rejected/expired',
    valid_until VARCHAR(20) COMMENT '有效期至',
    remark TEXT COMMENT '备注',
    customer_id BIGINT COMMENT '客户ID',
    company_id BIGINT DEFAULT NULL COMMENT '报价公司ID',
    tax_rate DECIMAL(5,2) DEFAULT 0 COMMENT '税率%',
    tax_amount DECIMAL(12,2) DEFAULT 0 COMMENT '税额',
    quote_date VARCHAR(20) COMMENT '报价日期',
    creator_id BIGINT COMMENT '创建人ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    UNIQUE INDEX uk_quote_no ((CASE WHEN deleted = 0 THEN quote_no ELSE NULL END)),
    INDEX idx_customer (customer_name),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报价记录表';

-- 6.3 发票记录表
CREATE TABLE IF NOT EXISTS fin_invoice (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    invoice_no VARCHAR(50) NOT NULL COMMENT '发票编号',
    customer_name VARCHAR(100) COMMENT '客户名称',
    type VARCHAR(20) DEFAULT 'normal' COMMENT '类型：special专票/normal普票/receipt收据',
    amount DECIMAL(12,2) DEFAULT 0 COMMENT '发票金额',
    tax_rate DECIMAL(5,2) DEFAULT 0 COMMENT '税率',
    tax_amount DECIMAL(12,2) DEFAULT 0 COMMENT '税额',
    status VARCHAR(20) DEFAULT 'pending' COMMENT '状态：completed已开具/pending待开具/cancelled已作废',
    issue_date VARCHAR(20) COMMENT '开具日期',
    remark TEXT COMMENT '备注',
    creator_id BIGINT COMMENT '创建人ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    UNIQUE INDEX uk_invoice_no ((CASE WHEN deleted = 0 THEN invoice_no ELSE NULL END)),
    INDEX idx_customer (customer_name),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='发票记录表';

-- 6.4 设计师提成表
CREATE TABLE IF NOT EXISTS fin_designer_commission (
    id               BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id         BIGINT COMMENT '关联订单ID',
    order_no         VARCHAR(50) COMMENT '订单编号（冗余）',
    designer_id      BIGINT NOT NULL COMMENT '设计师用户ID（关联sys_user）',
    designer_name    VARCHAR(100) NOT NULL COMMENT '设计师姓名',
    base_amount      DECIMAL(15,2) NOT NULL DEFAULT 0 COMMENT '计算基数（订单金额）',
    commission_rate  DECIMAL(5,2) NOT NULL DEFAULT 0 COMMENT '提成比例（如 5.00 代表 5%）',
    commission_amount DECIMAL(15,2) NOT NULL DEFAULT 0 COMMENT '提成金额',
    status           TINYINT DEFAULT 1 COMMENT '状态：1待结算 2已结算 3已打款',
    settle_time      DATETIME COMMENT '结算时间',
    remark           VARCHAR(500) COMMENT '备注',
    creator_id       BIGINT,
    create_time      DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted          TINYINT NOT NULL DEFAULT 0,
    INDEX idx_order_id (order_id),
    INDEX idx_designer_id (designer_id),
    INDEX idx_status (status),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设计师提成表';

-- =========================================================
-- 7. 物料管理模块（3 张表）
-- =========================================================

-- 7.1 物料分类表（含 status 字段）
CREATE TABLE IF NOT EXISTS mat_category (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    name        VARCHAR(100) NOT NULL COMMENT '分类名称',
    code        VARCHAR(50) COMMENT '分类编码',
    icon        VARCHAR(20) DEFAULT '📦' COMMENT '分类图标',
    parent_id   BIGINT DEFAULT 0 COMMENT '父分类ID',
    sort_order  INT DEFAULT 0,
    description VARCHAR(500) COMMENT '分类说明',
    status      INT DEFAULT 1 COMMENT '状态：1正常 0禁用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    deleted     TINYINT NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='物料分类表';

-- 7.2 物料库存表
CREATE TABLE IF NOT EXISTS mat_material (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    name            VARCHAR(200) NOT NULL COMMENT '物料名称',
    code            VARCHAR(100) COMMENT '物料编码',
    category_id     BIGINT COMMENT '分类ID',
    category_name   VARCHAR(100) COMMENT '分类名称（冗余）',
    paper_type      VARCHAR(20) COMMENT '纸张类型：A4/A3/SRA3 等',
    paper_spec      VARCHAR(50) COMMENT '纸张材质：80g双胶/128g铜版/157g铜版 等',
    colour_type     TINYINT DEFAULT 0 COMMENT '色彩类型：0黑白 1彩色',
    paper_group     VARCHAR(50) COMMENT '纸张分组标识（同尺寸+材质共享库存），如 A4-128G-TB',
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
    UNIQUE INDEX uk_material_code ((CASE WHEN deleted = 0 THEN code ELSE NULL END)),
    INDEX idx_category (category_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='物料库存表';

-- 7.3 库存变动日志表（含 operator_id 字段）
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
    operator_id     BIGINT COMMENT '操作人ID',
    operator_name   VARCHAR(100) COMMENT '操作人姓名',
    create_time     DATETIME DEFAULT CURRENT_TIMESTAMP,
    deleted         TINYINT NOT NULL DEFAULT 0,
    INDEX idx_material (material_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库存变动日志';


-- =========================================================
-- 8. 设计广场模块（3 张表）
-- =========================================================

-- 8.1 设计需求单表
CREATE TABLE IF NOT EXISTS sq_requirement (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    req_no          VARCHAR(50) COMMENT '需求单号',
    title           VARCHAR(200) NOT NULL COMMENT '需求标题',
    description     TEXT COMMENT '需求描述',
    category        VARCHAR(50) COMMENT '类别：logo/banner/poster/card/vi',
    budget          DECIMAL(15,2) COMMENT '预算',
    budget_desc     VARCHAR(500) COMMENT '预算说明',
    deadline        DATE COMMENT '截止日期',
    attachment      TEXT COMMENT '附件JSON',
    customer_id     BIGINT COMMENT '关联客户ID',
    customer_name   VARCHAR(100) COMMENT '关联客户名称',
    status          TINYINT DEFAULT 1 COMMENT '状态：1招募中 2已选定 3进行中 4已完成 5已取消',
    view_count      INT DEFAULT 0 COMMENT '浏览次数',
    apply_count     INT DEFAULT 0 COMMENT '申请次数',
    publisher_id    BIGINT COMMENT '发布人ID',
    publisher_name  VARCHAR(100),
    designer_id     BIGINT COMMENT '承接设计师ID',
    designer_name   VARCHAR(100) COMMENT '承接设计师名称',
    selected_designer_id BIGINT,
    selected_designer_name VARCHAR(100),
    final_price     DECIMAL(15,2),
    create_time     DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME,
    deleted         TINYINT NOT NULL DEFAULT 0,
    UNIQUE INDEX uk_req_no ((CASE WHEN deleted = 0 THEN req_no ELSE NULL END)),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设计广场需求单';

-- 8.2 设计师申请表
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

-- 8.3 设计师收入记录表
CREATE TABLE IF NOT EXISTS sq_income (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    requirement_id  BIGINT COMMENT '需求单ID',
    title           VARCHAR(200) COMMENT '需求标题',
    quoted_price    DECIMAL(15,2) COMMENT '承接价格',
    platform_fee    DECIMAL(15,2) COMMENT '平台手续费',
    actual_income   DECIMAL(15,2) COMMENT '实际收入',
    designer_id     BIGINT,
    designer_name   VARCHAR(100),
    amount          DECIMAL(15,2),
    status          TINYINT DEFAULT 1 COMMENT '状态：1待打款 2已打款',
    remark          VARCHAR(500),
    create_time     DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME,
    deleted         TINYINT NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设计师收入记录';


-- =========================================================
-- 9. 设计文件模块（2 张表）
-- =========================================================

-- 9.1 设计文件表
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

-- 9.2 设计文件版本记录表
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
-- 10. 通知模块（2 张表）
-- =========================================================

-- 10.1 系统通知表
CREATE TABLE IF NOT EXISTS sys_notice (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    title       VARCHAR(200) NOT NULL COMMENT '通知标题',
    content     TEXT COMMENT '通知内容',
    type        VARCHAR(50) COMMENT '类型：system/order/finance/warning',
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

-- 10.2 用户通知设置表
CREATE TABLE IF NOT EXISTS sys_notice_setting (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id         BIGINT NOT NULL COMMENT '用户ID',
    order_notify    TINYINT DEFAULT 1 COMMENT '订单通知：1开启 0关闭',
    finance_notify  TINYINT DEFAULT 1 COMMENT '财务通知',
    system_notify   TINYINT DEFAULT 1 COMMENT '系统通知',
    warning_notify  TINYINT DEFAULT 1 COMMENT '预警通知',
    create_time     DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME,
    deleted         TINYINT NOT NULL DEFAULT 0,
    UNIQUE INDEX uk_notice_setting_user ((CASE WHEN deleted = 0 THEN user_id ELSE NULL END))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户通知设置表';


-- =========================================================
-- 11. 系统维护模块（1 张表）
-- =========================================================

-- 11.1 数据备份记录表
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


-- =========================================================
-- 7. 公司信息表（支持多公司）
-- =========================================================

CREATE TABLE IF NOT EXISTS sys_company (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    company_name VARCHAR(200) NOT NULL COMMENT '公司名称',
    address VARCHAR(500) COMMENT '地址',
    phone VARCHAR(50) COMMENT '电话',
    fax VARCHAR(50) COMMENT '传真',
    email VARCHAR(100) COMMENT '邮箱',
    bank_name VARCHAR(200) COMMENT '开户银行',
    bank_account VARCHAR(50) COMMENT '银行账号',
    tax_no VARCHAR(50) COMMENT '税号',
    logo_url VARCHAR(500) COMMENT 'Logo URL',
    is_default TINYINT DEFAULT 0 COMMENT '是否默认公司(1=是)',
    status TINYINT DEFAULT 1 COMMENT '状态:0=禁用,1=启用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='公司信息表';

-- 公司初始数据
INSERT INTO sys_company (company_name, address, phone, fax, email, bank_name, bank_account, tax_no, is_default, status) VALUES
('示例广告传媒有限公司', '上海市静安区某某路123号', '021-88888888', '021-88888889', 'contact@example.com', '中国工商银行上海静安支行', '6222021234567890123', '91310000MA1K12345B', 1, 1);


-- =========================================================
-- 8. 报价物料明细子表
-- =========================================================

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


-- =========================================================
-- 12. 初始数据
-- =========================================================

-- ------------------- 12.1 系统管理员用户（密码: 123456） -------------------
INSERT INTO sys_user (username, password, real_name, phone, status) VALUES
('admin', '$2b$12$MPNCwxVVTbgUfTtoplfF.e4rCXbD7zb6529bQGIrzVRlQe6MoYYIS', '系统管理员', '13800138000', 1),
('finance', '$2b$12$MPNCwxVVTbgUfTtoplfF.e4rCXbD7zb6529bQGIrzVRlQe6MoYYIS', '财务小王', '13800138001', 1),
('operator', '$2b$12$MPNCwxVVTbgUfTtoplfF.e4rCXbD7zb6529bQGIrzVRlQe6MoYYIS', '操作员小李', '13800138002', 1);

-- ------------------- 12.2 初始角色 -------------------
INSERT INTO sys_role (role_name, role_code, description, sort, status) VALUES
('超级管理员', 'SUPER_ADMIN', '拥有所有权限', 1, 1),
('管理员', 'ADMIN', '除系统配置外全部权限', 2, 1),
('财务', 'FINANCE', '财务和订单查看', 3, 1),
('操作员', 'OPERATOR', '订单增删改', 4, 1),
('设计师', 'DESIGNER', '设计师，负责设计和订单承接', 5, 1),
('访客', 'VIEWER', '只读权限', 6, 1);

-- ------------------- 12.3 用户-角色分配 -------------------
INSERT INTO sys_user_role (user_id, role_id) VALUES (1, 1);
INSERT INTO sys_user_role (user_id, role_id) VALUES (2, 3);
INSERT INTO sys_user_role (user_id, role_id) VALUES (3, 4);

-- ------------------- 12.4.1 按钮资源初始数据 -------------------
INSERT INTO sys_button (name, permission, type, parent_id, sort, status) VALUES
('查看仪表盘', 'dashboard:view', 'button', 0, 1, 1),
('查看订单', 'order:list', 'button', 0, 2, 1),
('新建订单', 'order:create', 'button', 0, 3, 1),
('编辑订单', 'order:edit', 'button', 0, 4, 1),
('删除订单', 'order:delete', 'button', 0, 5, 1),
('查看客户', 'customer:list', 'button', 0, 6, 1),
('新建客户', 'customer:create', 'button', 0, 7, 1),
('编辑客户', 'customer:edit', 'button', 0, 8, 1),
('删除客户', 'customer:delete', 'button', 0, 9, 1),
('查看会员', 'member:list', 'button', 0, 10, 1),
('新建会员', 'member:create', 'button', 0, 11, 1),
('编辑会员', 'member:edit', 'button', 0, 12, 1),
('删除会员', 'member:delete', 'button', 0, 13, 1),
('会员充值', 'member:recharge', 'button', 0, 14, 1),
('查看账单', 'factory:list', 'button', 0, 15, 1),
('新建账单', 'factory:create', 'button', 0, 16, 1),
('编辑账单', 'factory:edit', 'button', 0, 17, 1),
('删除账单', 'factory:delete', 'button', 0, 18, 1),
('查看财务', 'finance:view', 'button', 0, 19, 1),
('编辑财务', 'finance:edit', 'button', 0, 20, 1),
('用户管理', 'system:user', 'button', 0, 21, 1),
('角色权限', 'system:role', 'button', 0, 22, 1),
('操作日志', 'system:log', 'button', 0, 23, 1),
('数据字典', 'system:dict', 'button', 0, 24, 1),
('数据备份', 'system:backup', 'button', 0, 25, 1),
('公告管理', 'system:notice', 'button', 0, 26, 1),
('按钮管理', 'system:menu', 'button', 0, 27, 1),
('公司管理', 'system:config', 'button', 0, 28, 1),
('查看物料', 'material:view', 'button', 0, 29, 1),
('查看文件', 'design:file', 'button', 0, 30, 1),
('查看报表', 'statistics:view', 'button', 0, 31, 1),
('查看广场', 'square:manage', 'button', 0, 32, 1);

-- ------------------- 12.4.2 初始权限（菜单 + 按钮 + 补丁权限码） -------------------
INSERT INTO sys_permission (id, parent_id, name, type, path, component, icon, sort, visible, status) VALUES
-- 一级菜单（id 1~11）
(1, 0, '仪表盘', 'menu', '/dashboard', 'dashboard/index', '📊', 1, 1, 1),
(2, 0, '订单管理', 'menu', '/orders', 'order/index', '📋', 2, 1, 1),
(3, 0, '客户管理', 'menu', '/customers', 'customer/index', '👥', 3, 1, 1),
(4, 0, '会员管理', 'menu', '/members', 'member/index', '🎫', 4, 1, 1),
(5, 0, '工厂账单', 'menu', '/factory', 'factory/index', '🏭', 5, 1, 1),
(6, 0, '财务管理', 'menu', '/finance', 'finance/index', '💰', 6, 1, 1),
(7, 0, '系统管理', 'menu', '/system', 'system/index', '⚙️', 7, 1, 1),
(8, 0, '物料管理', 'menu', '/material', 'material/index', '📦', 8, 1, 1),
(9, 0, '设计文件', 'menu', '/design', 'design/file', '🎨', 9, 1, 1),
(10, 0, '统计报表', 'menu', '/statistics', 'statistics/index', '📈', 10, 1, 1),
(11, 0, '广场管理', 'menu', '/square', 'square/index', '✏️', 11, 1, 1);

-- 二级按钮权限（id 12~37）
INSERT INTO sys_permission (parent_id, name, type, permission_code, sort, visible, status) VALUES
-- 仪表盘按钮
(1,  '查看仪表盘',   'button', 'dashboard:view',    1, 1, 1),
-- 订单按钮
(2,  '查看订单',     'button', 'order:list',         1, 1, 1),
(2,  '新建订单',     'button', 'order:create',       2, 1, 1),
(2,  '编辑订单',     'button', 'order:edit',         3, 1, 1),
(2,  '删除订单',     'button', 'order:delete',       4, 1, 1),
-- 客户按钮
(3,  '查看客户',     'button', 'customer:list',      1, 1, 1),
(3,  '新建客户',     'button', 'customer:create',    2, 1, 1),
(3,  '编辑客户',     'button', 'customer:edit',      3, 1, 1),
(3,  '删除客户',     'button', 'customer:delete',    4, 1, 1),
-- 会员按钮
(4,  '查看会员',     'button', 'member:list',        1, 1, 1),
(4,  '新建会员',     'button', 'member:create',      2, 1, 1),
(4,  '编辑会员',     'button', 'member:edit',        3, 1, 1),
(4,  '删除会员',     'button', 'member:delete',      4, 1, 1),
(4,  '会员充值',     'button', 'member:recharge',    5, 1, 1),
-- 工厂账单按钮
(5,  '查看账单',     'button', 'factory:list',       1, 1, 1),
(5,  '新建账单',     'button', 'factory:create',     2, 1, 1),
(5,  '编辑账单',     'button', 'factory:edit',       3, 1, 1),
(5,  '删除账单',     'button', 'factory:delete',     4, 1, 1),
-- 财务按钮
(6,  '查看财务',     'button', 'finance:view',       1, 1, 1),
(6,  '编辑财务',     'button', 'finance:edit',       2, 1, 1),
-- 系统管理按钮
(7,  '用户管理',     'button', 'system:user',        1, 1, 1),
(7,  '角色权限',     'button', 'system:role',        2, 1, 1),
(7,  '操作日志',     'button', 'system:log',         3, 1, 1),
(7,  '数据字典',     'button', 'system:dict',        4, 1, 1),
(7,  '数据备份',     'button', 'system:backup',      5, 1, 1),
(7,  '公告管理',     'button', 'system:notice',      6, 1, 1),
(7,  '按钮管理',     'button', 'system:menu',        7, 1, 1),
-- 物料管理按钮
(8,  '查看物料',     'button', 'material:view',      1, 1, 1),
-- 设计文件按钮
(9,  '查看文件',     'button', 'design:file',        1, 1, 1),
-- 统计报表按钮
(10, '查看报表',     'button', 'statistics:view',    1, 1, 1),
-- 广场管理按钮
(11, '查看广场',     'button', 'square:manage',      1, 1, 1);

-- ------------------- 12.5 角色权限分配 -------------------

-- SUPER_ADMIN(role_id=1): 所有权限
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT 1, id FROM sys_permission WHERE deleted = 0;

-- ADMIN(role_id=2): 除"数据备份"外的所有权限
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT 2, id FROM sys_permission WHERE deleted = 0 AND permission_code != 'system:backup';

-- FINANCE(role_id=3): 查看 + 财务相关
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT 3, id FROM sys_permission WHERE deleted = 0 AND (
    permission_code LIKE 'order:list' OR
    permission_code LIKE 'customer:list' OR
    permission_code LIKE 'finance:%' OR
    permission_code LIKE 'dashboard:view'
);

-- OPERATOR(role_id=4): 订单 + 客户 + 仪表盘
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT 4, id FROM sys_permission WHERE deleted = 0 AND (
    permission_code LIKE 'order:%' OR
    permission_code LIKE 'customer:%' OR
    permission_code LIKE 'dashboard:view'
);


-- ------------------- 12.6 零售客户（系统默认客户，用于门店零散订单） -------------------
INSERT INTO crm_customer (customer_name, contact_person, phone, industry, customer_type, total_amount, order_count, level, status) VALUES
('零售客户', NULL, NULL, '零售', 1, 0.00, 0, 1, 1);


-- ------------------- 12.7 客户等级初始数据 -------------------


-- ------------------- 12.8 会员初始数据（无） -------------------
-- 会员数据由用户手动创建


-- ------------------- 12.9 订单初始数据（无） -------------------
-- 订单数据由用户手动创建


-- ------------------- 12.10 工厂账单（关联到合并后的工厂客户） -------------------
-- 账单数据由用户手动创建


-- ------------------- 12.11 财务记录（无） -------------------
-- 财务流水由业务操作自动生成


-- ------------------- 12.12 物料分类与物料 -------------------
INSERT INTO mat_category (name, code, icon, sort_order) VALUES
('印刷材料', 'PRINT', '📄', 1),
('广告材料', 'AD', '🖼️', 2),
('耗材', 'CONSUMABLE', '📦', 3),
('包装材料', 'PACKAGE', '📦', 4);

INSERT INTO mat_material (name, code, category_id, category_name, spec, unit, price, cost_price, stock_quantity, warning_quantity, min_quantity, status) VALUES
('铜版纸 A4', 'MAT001', 1, '印刷材料', 'A4 105g', '张', 0.50, 0.30, 5000, 500, 200, 1),
('铜版纸 A3', 'MAT002', 1, '印刷材料', 'A3 105g', '张', 0.90, 0.55, 3000, 300, 100, 1),
('哑粉纸 A4', 'MAT003', 1, '印刷材料', 'A4 128g', '张', 0.60, 0.38, 2000, 200, 100, 1),
('KT板 60x90cm', 'MAT004', 2, '广告材料', '60x90cm 5mm', '张', 15.00, 8.00, 200, 30, 10, 1),
('写真布 1m宽', 'MAT005', 2, '广告材料', '1m宽 灯箱布', '米', 12.00, 7.00, 500, 50, 20, 1),
('墨盒（黑色）', 'MAT006', 3, '耗材', '通用型', '个', 85.00, 55.00, 20, 5, 2, 1),
('装订胶', 'MAT007', 3, '耗材', '500ml', '瓶', 35.00, 20.00, 15, 5, 3, 1);


-- ------------------- 12.13 通知初始数据（无） -------------------
-- 通知由系统运行时自动生成


-- ------------------- 12.14 数据字典 -------------------
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
-- 完成！v2.3 共 32 张表（干净初始状态）
--
-- 初始数据说明：
--   系统用户: admin/123456, finance/123456, operator/123456
--   角色: SUPER_ADMIN, ADMIN, FINANCE, OPERATOR, VIEWER
--   零售客户: crm_customer 中 1 条（用于门店零散订单）
--   业务数据(订单/账单/会员/财务): 均为空，由用户手动创建
--
-- 表清单：
--   系统管理(9): sys_user, sys_role, sys_permission, sys_user_role,
--               sys_role_permission, sys_button, sys_dict, sys_dict_item, sys_operation_log
--   客户管理(3): crm_customer(含工厂), customer_tag, customer_level
--   会员管理(2): mem_member, mem_member_transaction
--   订单管理(2): ord_order, ord_order_material
--   工厂账单(3): fac_factory_bill, fac_factory_bill_detail, fac_factory_salesman
--   财务管理(3): fin_record, fin_quote, fin_invoice
--   物料管理(3): mat_category, mat_material, mat_stock_log
--   设计广场(3): sq_requirement, sq_application, sq_income
--   设计文件(2): des_file, des_file_version
--   通知模块(2): sys_notice, sys_notice_setting
--   系统维护(1): sys_backup
-- =========================================================


-- === 增量同步 ===
-- 2026-04-28 15:57:48 自动追加
INSERT INTO `sys_permission` (`id`, `parent_id`, `name`, `type`, `path`, `component`, `icon`, `permission_code`, `sort`, `visible`, `status`, `create_time`, `update_time`, `deleted`) VALUES (12, 1, '查看仪表盘', 'button', NULL, NULL, NULL, 'dashboard:view', 1, 1, 1, '2026-04-28 15:30:01', '2026-04-28 15:30:01', 0);
INSERT INTO `sys_permission` (`id`, `parent_id`, `name`, `type`, `path`, `component`, `icon`, `permission_code`, `sort`, `visible`, `status`, `create_time`, `update_time`, `deleted`) VALUES (13, 2, '查看订单', 'button', NULL, NULL, NULL, 'order:list', 1, 1, 1, '2026-04-28 15:30:01', '2026-04-28 15:30:01', 0);
INSERT INTO `sys_permission` (`id`, `parent_id`, `name`, `type`, `path`, `component`, `icon`, `permission_code`, `sort`, `visible`, `status`, `create_time`, `update_time`, `deleted`) VALUES (14, 2, '新建订单', 'button', NULL, NULL, NULL, 'order:create', 2, 1, 1, '2026-04-28 15:30:01', '2026-04-28 15:30:01', 0);
INSERT INTO `sys_permission` (`id`, `parent_id`, `name`, `type`, `path`, `component`, `icon`, `permission_code`, `sort`, `visible`, `status`, `create_time`, `update_time`, `deleted`) VALUES (15, 2, '编辑订单', 'button', NULL, NULL, NULL, 'order:edit', 3, 1, 1, '2026-04-28 15:30:01', '2026-04-28 15:30:01', 0);
INSERT INTO `sys_permission` (`id`, `parent_id`, `name`, `type`, `path`, `component`, `icon`, `permission_code`, `sort`, `visible`, `status`, `create_time`, `update_time`, `deleted`) VALUES (16, 2, '删除订单', 'button', NULL, NULL, NULL, 'order:delete', 4, 1, 1, '2026-04-28 15:30:01', '2026-04-28 15:30:01', 0);
INSERT INTO `sys_permission` (`id`, `parent_id`, `name`, `type`, `path`, `component`, `icon`, `permission_code`, `sort`, `visible`, `status`, `create_time`, `update_time`, `deleted`) VALUES (17, 3, '查看客户', 'button', NULL, NULL, NULL, 'customer:list', 1, 1, 1, '2026-04-28 15:30:01', '2026-04-28 15:30:01', 0);
INSERT INTO `sys_permission` (`id`, `parent_id`, `name`, `type`, `path`, `component`, `icon`, `permission_code`, `sort`, `visible`, `status`, `create_time`, `update_time`, `deleted`) VALUES (18, 3, '新建客户', 'button', NULL, NULL, NULL, 'customer:create', 2, 1, 1, '2026-04-28 15:30:01', '2026-04-28 15:30:01', 0);
INSERT INTO `sys_permission` (`id`, `parent_id`, `name`, `type`, `path`, `component`, `icon`, `permission_code`, `sort`, `visible`, `status`, `create_time`, `update_time`, `deleted`) VALUES (19, 3, '编辑客户', 'button', NULL, NULL, NULL, 'customer:edit', 3, 1, 1, '2026-04-28 15:30:01', '2026-04-28 15:30:01', 0);
INSERT INTO `sys_permission` (`id`, `parent_id`, `name`, `type`, `path`, `component`, `icon`, `permission_code`, `sort`, `visible`, `status`, `create_time`, `update_time`, `deleted`) VALUES (20, 3, '删除客户', 'button', NULL, NULL, NULL, 'customer:delete', 4, 1, 1, '2026-04-28 15:30:01', '2026-04-28 15:30:01', 0);
INSERT INTO `sys_permission` (`id`, `parent_id`, `name`, `type`, `path`, `component`, `icon`, `permission_code`, `sort`, `visible`, `status`, `create_time`, `update_time`, `deleted`) VALUES (21, 4, '查看会员', 'button', NULL, NULL, NULL, 'member:list', 1, 1, 1, '2026-04-28 15:30:01', '2026-04-28 15:30:01', 0);
INSERT INTO `sys_permission` (`id`, `parent_id`, `name`, `type`, `path`, `component`, `icon`, `permission_code`, `sort`, `visible`, `status`, `create_time`, `update_time`, `deleted`) VALUES (22, 4, '新建会员', 'button', NULL, NULL, NULL, 'member:create', 2, 1, 1, '2026-04-28 15:30:01', '2026-04-28 15:30:01', 0);
INSERT INTO `sys_permission` (`id`, `parent_id`, `name`, `type`, `path`, `component`, `icon`, `permission_code`, `sort`, `visible`, `status`, `create_time`, `update_time`, `deleted`) VALUES (23, 4, '编辑会员', 'button', NULL, NULL, NULL, 'member:edit', 3, 1, 1, '2026-04-28 15:30:01', '2026-04-28 15:30:01', 0);
INSERT INTO `sys_permission` (`id`, `parent_id`, `name`, `type`, `path`, `component`, `icon`, `permission_code`, `sort`, `visible`, `status`, `create_time`, `update_time`, `deleted`) VALUES (24, 4, '删除会员', 'button', NULL, NULL, NULL, 'member:delete', 4, 1, 1, '2026-04-28 15:30:01', '2026-04-28 15:30:01', 0);
INSERT INTO `sys_permission` (`id`, `parent_id`, `name`, `type`, `path`, `component`, `icon`, `permission_code`, `sort`, `visible`, `status`, `create_time`, `update_time`, `deleted`) VALUES (25, 4, '会员充值', 'button', NULL, NULL, NULL, 'member:recharge', 5, 1, 1, '2026-04-28 15:30:01', '2026-04-28 15:30:01', 0);
INSERT INTO `sys_permission` (`id`, `parent_id`, `name`, `type`, `path`, `component`, `icon`, `permission_code`, `sort`, `visible`, `status`, `create_time`, `update_time`, `deleted`) VALUES (26, 5, '查看账单', 'button', NULL, NULL, NULL, 'factory:list', 1, 1, 1, '2026-04-28 15:30:01', '2026-04-28 15:30:01', 0);
INSERT INTO `sys_permission` (`id`, `parent_id`, `name`, `type`, `path`, `component`, `icon`, `permission_code`, `sort`, `visible`, `status`, `create_time`, `update_time`, `deleted`) VALUES (27, 5, '新建账单', 'button', NULL, NULL, NULL, 'factory:create', 2, 1, 1, '2026-04-28 15:30:01', '2026-04-28 15:30:01', 0);
INSERT INTO `sys_permission` (`id`, `parent_id`, `name`, `type`, `path`, `component`, `icon`, `permission_code`, `sort`, `visible`, `status`, `create_time`, `update_time`, `deleted`) VALUES (28, 5, '编辑账单', 'button', NULL, NULL, NULL, 'factory:edit', 3, 1, 1, '2026-04-28 15:30:01', '2026-04-28 15:30:01', 0);
INSERT INTO `sys_permission` (`id`, `parent_id`, `name`, `type`, `path`, `component`, `icon`, `permission_code`, `sort`, `visible`, `status`, `create_time`, `update_time`, `deleted`) VALUES (29, 5, '删除账单', 'button', NULL, NULL, NULL, 'factory:delete', 4, 1, 1, '2026-04-28 15:30:01', '2026-04-28 15:30:01', 0);
INSERT INTO `sys_permission` (`id`, `parent_id`, `name`, `type`, `path`, `component`, `icon`, `permission_code`, `sort`, `visible`, `status`, `create_time`, `update_time`, `deleted`) VALUES (30, 6, '查看财务', 'button', NULL, NULL, NULL, 'finance:view', 1, 1, 1, '2026-04-28 15:30:01', '2026-04-28 15:30:01', 0);
INSERT INTO `sys_permission` (`id`, `parent_id`, `name`, `type`, `path`, `component`, `icon`, `permission_code`, `sort`, `visible`, `status`, `create_time`, `update_time`, `deleted`) VALUES (31, 6, '编辑财务', 'button', NULL, NULL, NULL, 'finance:edit', 2, 1, 1, '2026-04-28 15:30:01', '2026-04-28 15:30:01', 0);
INSERT INTO `sys_permission` (`id`, `parent_id`, `name`, `type`, `path`, `component`, `icon`, `permission_code`, `sort`, `visible`, `status`, `create_time`, `update_time`, `deleted`) VALUES (32, 7, '用户管理', 'button', NULL, NULL, NULL, 'system:user', 1, 1, 1, '2026-04-28 15:30:01', '2026-04-28 15:30:01', 0);
INSERT INTO `sys_permission` (`id`, `parent_id`, `name`, `type`, `path`, `component`, `icon`, `permission_code`, `sort`, `visible`, `status`, `create_time`, `update_time`, `deleted`) VALUES (33, 7, '角色权限', 'button', NULL, NULL, NULL, 'system:role', 2, 1, 1, '2026-04-28 15:30:01', '2026-04-28 15:30:01', 0);
INSERT INTO `sys_permission` (`id`, `parent_id`, `name`, `type`, `path`, `component`, `icon`, `permission_code`, `sort`, `visible`, `status`, `create_time`, `update_time`, `deleted`) VALUES (34, 7, '操作日志', 'button', NULL, NULL, NULL, 'system:log', 3, 1, 1, '2026-04-28 15:30:01', '2026-04-28 15:30:01', 0);
INSERT INTO `sys_permission` (`id`, `parent_id`, `name`, `type`, `path`, `component`, `icon`, `permission_code`, `sort`, `visible`, `status`, `create_time`, `update_time`, `deleted`) VALUES (35, 7, '数据字典', 'button', NULL, NULL, NULL, 'system:dict', 4, 1, 1, '2026-04-28 15:30:01', '2026-04-28 15:30:01', 0);
INSERT INTO `sys_permission` (`id`, `parent_id`, `name`, `type`, `path`, `component`, `icon`, `permission_code`, `sort`, `visible`, `status`, `create_time`, `update_time`, `deleted`) VALUES (36, 7, '数据备份', 'button', NULL, NULL, NULL, 'system:backup', 5, 1, 1, '2026-04-28 15:30:01', '2026-04-28 15:30:01', 0);
INSERT INTO `sys_permission` (`id`, `parent_id`, `name`, `type`, `path`, `component`, `icon`, `permission_code`, `sort`, `visible`, `status`, `create_time`, `update_time`, `deleted`) VALUES (37, 7, '公告管理', 'button', NULL, NULL, NULL, 'system:notice', 6, 1, 1, '2026-04-28 15:30:01', '2026-04-28 15:30:01', 0);
INSERT INTO `sys_permission` (`id`, `parent_id`, `name`, `type`, `path`, `component`, `icon`, `permission_code`, `sort`, `visible`, `status`, `create_time`, `update_time`, `deleted`) VALUES (38, 7, '按钮管理', 'button', NULL, NULL, NULL, 'system:menu', 7, 1, 1, '2026-04-28 15:30:01', '2026-04-28 15:30:01', 0);
INSERT INTO `sys_permission` (`id`, `parent_id`, `name`, `type`, `path`, `component`, `icon`, `permission_code`, `sort`, `visible`, `status`, `create_time`, `update_time`, `deleted`) VALUES (39, 8, '查看物料', 'button', NULL, NULL, NULL, 'material:view', 1, 1, 1, '2026-04-28 15:30:01', '2026-04-28 15:30:01', 0);
INSERT INTO `sys_permission` (`id`, `parent_id`, `name`, `type`, `path`, `component`, `icon`, `permission_code`, `sort`, `visible`, `status`, `create_time`, `update_time`, `deleted`) VALUES (40, 9, '查看文件', 'button', NULL, NULL, NULL, 'design:file', 1, 1, 1, '2026-04-28 15:30:01', '2026-04-28 15:30:01', 0);
INSERT INTO `sys_permission` (`id`, `parent_id`, `name`, `type`, `path`, `component`, `icon`, `permission_code`, `sort`, `visible`, `status`, `create_time`, `update_time`, `deleted`) VALUES (41, 10, '查看报表', 'button', NULL, NULL, NULL, 'statistics:view', 1, 1, 1, '2026-04-28 15:30:01', '2026-04-28 15:30:01', 0);
INSERT INTO `sys_permission` (`id`, `parent_id`, `name`, `type`, `path`, `component`, `icon`, `permission_code`, `sort`, `visible`, `status`, `create_time`, `update_time`, `deleted`) VALUES (42, 11, '查看广场', 'button', NULL, NULL, NULL, 'square:manage', 1, 1, 1, '2026-04-28 15:30:01', '2026-04-28 15:30:01', 0);
INSERT INTO `sys_permission` (`id`, `parent_id`, `name`, `type`, `path`, `component`, `icon`, `permission_code`, `sort`, `visible`, `status`, `create_time`, `update_time`, `deleted`) VALUES (43, 7, '公司管理', 'button', NULL, NULL, NULL, 'system:config', 28, 1, 1, '2026-04-28 15:32:37', '2026-04-28 15:32:37', 0);
INSERT INTO `sys_company` (`id`, `company_name`, `address`, `phone`, `fax`, `email`, `bank_name`, `bank_account`, `tax_no`, `logo_url`, `is_default`, `status`, `create_time`, `update_time`, `deleted`) VALUES (1, '台州新原力广告传媒有限公司', '浙江省台州市椒江区台州湾新区', '15084494408', '021-88888889', '2991404309@qq.com', '中国建设银行股份有限公司台州湾新区支行', '33050111438300000786', '91331001MAK6XBJU5Q', '', 1, 1, '2026-04-28 15:32:04', '2026-04-28 15:41:21', 0);
INSERT INTO `sys_dict` (`id`, `name`, `code`, `description`, `sort_order`, `status`, `create_time`, `update_time`, `deleted`) VALUES (1, '订单类型', 'order_type', '订单类型枚举', 1, 1, '2026-04-28 15:30:01', NULL, 0);
INSERT INTO `sys_dict` (`id`, `name`, `code`, `description`, `sort_order`, `status`, `create_time`, `update_time`, `deleted`) VALUES (2, '订单状态', 'order_status', '订单状态枚举', 2, 1, '2026-04-28 15:30:01', NULL, 0);
INSERT INTO `sys_dict` (`id`, `name`, `code`, `description`, `sort_order`, `status`, `create_time`, `update_time`, `deleted`) VALUES (3, '支付方式', 'payment_method', '支付方式枚举', 3, 1, '2026-04-28 15:30:01', NULL, 0);
INSERT INTO `sys_dict` (`id`, `name`, `code`, `description`, `sort_order`, `status`, `create_time`, `update_time`, `deleted`) VALUES (4, '客户等级', 'customer_level', '客户等级枚举', 4, 1, '2026-04-28 15:30:01', NULL, 0);
INSERT INTO `sys_button` (`id`, `name`, `permission`, `type`, `parent_id`, `sort`, `status`, `create_time`, `update_time`, `deleted`) VALUES (1, '查看仪表盘', 'dashboard:view', 'button', 0, 1, 1, '2026-04-28 15:30:01', '2026-04-28 15:30:01', 0);
INSERT INTO `sys_button` (`id`, `name`, `permission`, `type`, `parent_id`, `sort`, `status`, `create_time`, `update_time`, `deleted`) VALUES (2, '查看订单', 'order:list', 'button', 0, 2, 1, '2026-04-28 15:30:01', '2026-04-28 15:30:01', 0);
INSERT INTO `sys_button` (`id`, `name`, `permission`, `type`, `parent_id`, `sort`, `status`, `create_time`, `update_time`, `deleted`) VALUES (3, '新建订单', 'order:create', 'button', 0, 3, 1, '2026-04-28 15:30:01', '2026-04-28 15:30:01', 0);
INSERT INTO `sys_button` (`id`, `name`, `permission`, `type`, `parent_id`, `sort`, `status`, `create_time`, `update_time`, `deleted`) VALUES (4, '编辑订单', 'order:edit', 'button', 0, 4, 1, '2026-04-28 15:30:01', '2026-04-28 15:30:01', 0);
INSERT INTO `sys_button` (`id`, `name`, `permission`, `type`, `parent_id`, `sort`, `status`, `create_time`, `update_time`, `deleted`) VALUES (5, '删除订单', 'order:delete', 'button', 0, 5, 1, '2026-04-28 15:30:01', '2026-04-28 15:30:01', 0);
INSERT INTO `sys_button` (`id`, `name`, `permission`, `type`, `parent_id`, `sort`, `status`, `create_time`, `update_time`, `deleted`) VALUES (6, '查看客户', 'customer:list', 'button', 0, 6, 1, '2026-04-28 15:30:01', '2026-04-28 15:30:01', 0);
INSERT INTO `sys_button` (`id`, `name`, `permission`, `type`, `parent_id`, `sort`, `status`, `create_time`, `update_time`, `deleted`) VALUES (7, '新建客户', 'customer:create', 'button', 0, 7, 1, '2026-04-28 15:30:01', '2026-04-28 15:30:01', 0);
INSERT INTO `sys_button` (`id`, `name`, `permission`, `type`, `parent_id`, `sort`, `status`, `create_time`, `update_time`, `deleted`) VALUES (8, '编辑客户', 'customer:edit', 'button', 0, 8, 1, '2026-04-28 15:30:01', '2026-04-28 15:30:01', 0);
INSERT INTO `sys_button` (`id`, `name`, `permission`, `type`, `parent_id`, `sort`, `status`, `create_time`, `update_time`, `deleted`) VALUES (9, '删除客户', 'customer:delete', 'button', 0, 9, 1, '2026-04-28 15:30:01', '2026-04-28 15:30:01', 0);
INSERT INTO `sys_button` (`id`, `name`, `permission`, `type`, `parent_id`, `sort`, `status`, `create_time`, `update_time`, `deleted`) VALUES (10, '查看会员', 'member:list', 'button', 0, 10, 1, '2026-04-28 15:30:01', '2026-04-28 15:30:01', 0);
INSERT INTO `sys_button` (`id`, `name`, `permission`, `type`, `parent_id`, `sort`, `status`, `create_time`, `update_time`, `deleted`) VALUES (11, '新建会员', 'member:create', 'button', 0, 11, 1, '2026-04-28 15:30:01', '2026-04-28 15:30:01', 0);
INSERT INTO `sys_button` (`id`, `name`, `permission`, `type`, `parent_id`, `sort`, `status`, `create_time`, `update_time`, `deleted`) VALUES (12, '编辑会员', 'member:edit', 'button', 0, 12, 1, '2026-04-28 15:30:01', '2026-04-28 15:30:01', 0);
INSERT INTO `sys_button` (`id`, `name`, `permission`, `type`, `parent_id`, `sort`, `status`, `create_time`, `update_time`, `deleted`) VALUES (13, '删除会员', 'member:delete', 'button', 0, 13, 1, '2026-04-28 15:30:01', '2026-04-28 15:30:01', 0);
INSERT INTO `sys_button` (`id`, `name`, `permission`, `type`, `parent_id`, `sort`, `status`, `create_time`, `update_time`, `deleted`) VALUES (14, '会员充值', 'member:recharge', 'button', 0, 14, 1, '2026-04-28 15:30:01', '2026-04-28 15:30:01', 0);
INSERT INTO `sys_button` (`id`, `name`, `permission`, `type`, `parent_id`, `sort`, `status`, `create_time`, `update_time`, `deleted`) VALUES (15, '查看账单', 'factory:list', 'button', 0, 15, 1, '2026-04-28 15:30:01', '2026-04-28 15:30:01', 0);
INSERT INTO `sys_button` (`id`, `name`, `permission`, `type`, `parent_id`, `sort`, `status`, `create_time`, `update_time`, `deleted`) VALUES (16, '新建账单', 'factory:create', 'button', 0, 16, 1, '2026-04-28 15:30:01', '2026-04-28 15:30:01', 0);
INSERT INTO `sys_button` (`id`, `name`, `permission`, `type`, `parent_id`, `sort`, `status`, `create_time`, `update_time`, `deleted`) VALUES (17, '编辑账单', 'factory:edit', 'button', 0, 17, 1, '2026-04-28 15:30:01', '2026-04-28 15:30:01', 0);
INSERT INTO `sys_button` (`id`, `name`, `permission`, `type`, `parent_id`, `sort`, `status`, `create_time`, `update_time`, `deleted`) VALUES (18, '删除账单', 'factory:delete', 'button', 0, 18, 1, '2026-04-28 15:30:01', '2026-04-28 15:30:01', 0);
INSERT INTO `sys_button` (`id`, `name`, `permission`, `type`, `parent_id`, `sort`, `status`, `create_time`, `update_time`, `deleted`) VALUES (19, '查看财务', 'finance:view', 'button', 0, 19, 1, '2026-04-28 15:30:01', '2026-04-28 15:30:01', 0);
INSERT INTO `sys_button` (`id`, `name`, `permission`, `type`, `parent_id`, `sort`, `status`, `create_time`, `update_time`, `deleted`) VALUES (20, '编辑财务', 'finance:edit', 'button', 0, 20, 1, '2026-04-28 15:30:01', '2026-04-28 15:30:01', 0);
INSERT INTO `sys_button` (`id`, `name`, `permission`, `type`, `parent_id`, `sort`, `status`, `create_time`, `update_time`, `deleted`) VALUES (21, '用户管理', 'system:user', 'button', 0, 21, 1, '2026-04-28 15:30:01', '2026-04-28 15:30:01', 0);
INSERT INTO `sys_button` (`id`, `name`, `permission`, `type`, `parent_id`, `sort`, `status`, `create_time`, `update_time`, `deleted`) VALUES (22, '角色权限', 'system:role', 'button', 0, 22, 1, '2026-04-28 15:30:01', '2026-04-28 15:30:01', 0);
INSERT INTO `sys_button` (`id`, `name`, `permission`, `type`, `parent_id`, `sort`, `status`, `create_time`, `update_time`, `deleted`) VALUES (23, '操作日志', 'system:log', 'button', 0, 23, 1, '2026-04-28 15:30:01', '2026-04-28 15:30:01', 0);
INSERT INTO `sys_button` (`id`, `name`, `permission`, `type`, `parent_id`, `sort`, `status`, `create_time`, `update_time`, `deleted`) VALUES (24, '数据字典', 'system:dict', 'button', 0, 24, 1, '2026-04-28 15:30:01', '2026-04-28 15:30:01', 0);
INSERT INTO `sys_button` (`id`, `name`, `permission`, `type`, `parent_id`, `sort`, `status`, `create_time`, `update_time`, `deleted`) VALUES (25, '数据备份', 'system:backup', 'button', 0, 25, 1, '2026-04-28 15:30:01', '2026-04-28 15:30:01', 0);
INSERT INTO `sys_button` (`id`, `name`, `permission`, `type`, `parent_id`, `sort`, `status`, `create_time`, `update_time`, `deleted`) VALUES (26, '公告管理', 'system:notice', 'button', 0, 26, 1, '2026-04-28 15:30:01', '2026-04-28 15:30:01', 0);
INSERT INTO `sys_button` (`id`, `name`, `permission`, `type`, `parent_id`, `sort`, `status`, `create_time`, `update_time`, `deleted`) VALUES (27, '按钮管理', 'system:menu', 'button', 0, 27, 1, '2026-04-28 15:30:01', '2026-04-28 15:30:01', 0);
INSERT INTO `sys_button` (`id`, `name`, `permission`, `type`, `parent_id`, `sort`, `status`, `create_time`, `update_time`, `deleted`) VALUES (28, '查看物料', 'material:view', 'button', 0, 28, 1, '2026-04-28 15:30:01', '2026-04-28 15:30:01', 0);
INSERT INTO `sys_button` (`id`, `name`, `permission`, `type`, `parent_id`, `sort`, `status`, `create_time`, `update_time`, `deleted`) VALUES (29, '查看文件', 'design:file', 'button', 0, 29, 1, '2026-04-28 15:30:01', '2026-04-28 15:30:01', 0);
INSERT INTO `sys_button` (`id`, `name`, `permission`, `type`, `parent_id`, `sort`, `status`, `create_time`, `update_time`, `deleted`) VALUES (30, '查看报表', 'statistics:view', 'button', 0, 30, 1, '2026-04-28 15:30:01', '2026-04-28 15:30:01', 0);
INSERT INTO `sys_button` (`id`, `name`, `permission`, `type`, `parent_id`, `sort`, `status`, `create_time`, `update_time`, `deleted`) VALUES (31, '查看广场', 'square:manage', 'button', 0, 31, 1, '2026-04-28 15:30:01', '2026-04-28 15:30:01', 0);

-- =========================================================
-- 设计师提成模块 & 多公司支持（2026-04-28 19:57）
-- =========================================================

-- 设计师提成配置表
CREATE TABLE IF NOT EXISTS `designer_commission_config` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `designer_id` BIGINT NOT NULL COMMENT '设计师用户ID',
  `designer_name` VARCHAR(100) COMMENT '设计师姓名（冗余）',
  `commission_rate` DECIMAL(5,2) DEFAULT 0.00 COMMENT '提成比例（%）',
  `enabled` TINYINT DEFAULT 1 COMMENT '是否启用：0否 1是',
  `updated_by` BIGINT COMMENT '最后修改人',
  `updated_time` DATETIME COMMENT '最后修改时间',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `deleted` TINYINT DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_designer` (`designer_id`),
  INDEX `idx_designer` (`designer_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='设计师提成比例配置';

-- sys_company 支持字段（公司规模、状态等扩展）
ALTER TABLE `sys_company` ADD COLUMN IF NOT EXISTS `contact_person` VARCHAR(100) COMMENT '联系人' AFTER `email`;
ALTER TABLE `sys_company` ADD COLUMN IF NOT EXISTS `company_type` VARCHAR(50) COMMENT '公司类型：headquarters/branch' AFTER `contact_person`;

-- 用户归属公司（支持多公司）
ALTER TABLE `sys_user` ADD COLUMN IF NOT EXISTS `company_id` BIGINT COMMENT '所属公司ID' AFTER `status`;
ALTER TABLE `sys_user` ADD COLUMN IF NOT EXISTS `department` VARCHAR(100) COMMENT '部门' AFTER `company_id`;

-- sys_button 新增公司管理按钮
INSERT INTO `sys_button` (`id`, `name`, `permission`, `type`, `parent_id`, `sort`, `status`, `create_time`, `update_time`, `deleted`) VALUES (32, '设计师提成', 'designer:commission', 'button', 0, 32, 1, '2026-04-28 19:57:00', '2026-04-28 19:57:00', 0);

-- =========================================================
-- 待办工作台模块（2026-04-28 20:15）
-- =========================================================

-- 待办事项表
CREATE TABLE IF NOT EXISTS `todo_item` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `customer_name` VARCHAR(200) NOT NULL COMMENT '客户名称',
  `contact_phone` VARCHAR(20) COMMENT '联系电话',
  `contact_person` VARCHAR(100) COMMENT '联系人',
  `dimensions` VARCHAR(500) COMMENT '量好的尺寸',
  `requirements` TEXT COMMENT '客户需求描述',
  `status` TINYINT DEFAULT 1 COMMENT '状态：1新收集 2分析中 3待确认 4已转订单',
  `quote_amount` DECIMAL(15,2) COMMENT 'AI报价金额',
  `quote_detail` TEXT COMMENT 'AI报价明细（JSON）',
  `order_id` BIGINT COMMENT '关联的正式订单ID',
  `order_no` VARCHAR(50) COMMENT '关联的正式订单编号',
  `priority` TINYINT DEFAULT 1 COMMENT '优先级：1普通 2紧急 3加急',
  `source` VARCHAR(50) COMMENT '来源：门店/电话/微信/上门',
  `remark` TEXT COMMENT '内部备注',
  `creator_id` BIGINT COMMENT '创建人',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` TINYINT DEFAULT 0,
  PRIMARY KEY (`id`),
  INDEX `idx_status` (`status`),
  INDEX `idx_customer` (`customer_name`),
  INDEX `idx_order` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='待办事项/需求收集';

-- 示例待办数据
INSERT INTO `todo_item` (`customer_name`, `contact_phone`, `dimensions`, `requirements`, `status`, `priority`, `source`, `creator_id`) VALUES
('张三（奶茶店）', '13900001111', '门头招牌：宽80cm×高120cm×1块', '亚克力发光字，白天晚上两用，红色为主色调，要求3天内完成', 1, 2, '上门量尺', 1),
('李四（服装店）', '13900002222', '橱窗贴：宽60cm×高90cm×3张', '高清车贴，过膜，3天内制作完成', 1, 1, '门店', 1),
('王五（餐厅）', '13900003333', '桌卡：宽10cm×高14cm×50个', 'pvc透明桌卡，双面印刷，含设计费', 2, 1, '电话', 1);
