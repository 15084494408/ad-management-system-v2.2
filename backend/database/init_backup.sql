-- phpMyAdmin SQL Dump
-- version 5.2.1
-- https://www.phpmyadmin.net/
--
-- 主机： 127.0.0.1
-- 生成日期： 2026-04-19 19:33:04
-- 服务器版本： 10.4.32-MariaDB
-- PHP 版本： 8.2.12

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- 数据库： `enterprise_ad`
--

-- --------------------------------------------------------

--
-- 表的结构 `crm_customer`
--

CREATE TABLE `crm_customer` (
  `id` bigint(20) NOT NULL,
  `customer_name` varchar(200) NOT NULL COMMENT '客户名称',
  `contact_person` varchar(100) DEFAULT NULL COMMENT '联系人',
  `phone` varchar(20) DEFAULT NULL COMMENT '手机号',
  `telephone` varchar(20) DEFAULT NULL COMMENT '座机',
  `email` varchar(100) DEFAULT NULL COMMENT '邮箱',
  `address` varchar(500) DEFAULT NULL COMMENT '地址',
  `industry` varchar(100) DEFAULT NULL COMMENT '行业',
  `total_amount` decimal(15,2) DEFAULT 0.00 COMMENT '累计消费金额',
  `order_count` int(11) DEFAULT 0 COMMENT '订单数量',
  `level` tinyint(4) DEFAULT 1 COMMENT '等级：1普通 2VIP 3战略',
  `status` tinyint(4) DEFAULT 1 COMMENT '状态：1正常 0禁用',
  `creator_id` bigint(20) DEFAULT NULL,
  `create_time` datetime DEFAULT current_timestamp(),
  `update_time` datetime DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  `deleted` tinyint(4) NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='客户表';

--
-- 转存表中的数据 `crm_customer`
--

INSERT INTO `crm_customer` (`id`, `customer_name`, `contact_person`, `phone`, `telephone`, `email`, `address`, `industry`, `total_amount`, `order_count`, `level`, `status`, `creator_id`, `create_time`, `update_time`, `deleted`) VALUES
(1, '杭州苏润阀门厂', '苏高于', '19818226202', NULL, NULL, '浙江台州', '工业制造', 120000.00, 15, 2, 1, NULL, '2026-04-20 00:48:53', '2026-04-20 00:48:53', 0),
(2, '浙江华新传媒', '王总', '13900139001', NULL, NULL, '浙江杭州', '广告传媒', 200000.00, 25, 3, 1, NULL, '2026-04-20 00:48:53', '2026-04-20 00:48:53', 0),
(3, '上海蓝图设计', '李总监', '13800138010', NULL, NULL, '上海', '设计服务', 80000.00, 10, 2, 1, NULL, '2026-04-20 00:48:53', '2026-04-20 00:48:53', 0),
(4, '深圳智联招聘', '张HR', '13900139002', NULL, NULL, '广东深圳', '人力资源', 50000.00, 8, 1, 1, NULL, '2026-04-20 00:48:53', '2026-04-20 00:48:53', 0);

-- --------------------------------------------------------

--
-- 表的结构 `fac_factory`
--

CREATE TABLE `fac_factory` (
  `id` bigint(20) NOT NULL,
  `factory_name` varchar(200) NOT NULL COMMENT '工厂名称',
  `contact_person` varchar(100) DEFAULT NULL COMMENT '联系人',
  `phone` varchar(20) DEFAULT NULL COMMENT '电话',
  `address` varchar(500) DEFAULT NULL COMMENT '地址',
  `type` varchar(50) DEFAULT NULL COMMENT '类型：印刷/包装/广告制作',
  `status` tinyint(4) DEFAULT 1 COMMENT '状态：1正常 0暂停',
  `creator_id` bigint(20) DEFAULT NULL,
  `create_time` datetime DEFAULT current_timestamp(),
  `update_time` datetime DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  `deleted` tinyint(4) NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='工厂表';

--
-- 转存表中的数据 `fac_factory`
--

INSERT INTO `fac_factory` (`id`, `factory_name`, `contact_person`, `phone`, `address`, `type`, `status`, `creator_id`, `create_time`, `update_time`, `deleted`) VALUES
(1, '杭州印刷一厂', '张经理', '0571-88881001', '浙江省杭州市西湖区文三路123号', '印刷', 1, NULL, '2026-04-20 00:48:52', '2026-04-20 00:48:52', 0),
(2, '上海印刷集团', '李总监', '021-55551002', '上海市浦东新区张江高科路456号', '印刷', 1, NULL, '2026-04-20 00:48:52', '2026-04-20 00:48:52', 0),
(3, '北京印刷厂', '王主任', '010-66661003', '北京市朝阳区望京西路789号', '印刷', 1, NULL, '2026-04-20 00:48:52', '2026-04-20 00:48:52', 0),
(4, '深圳包装公司', '陈经理', '0755-88881004', '广东省深圳市南山区科技园路101号', '包装', 1, NULL, '2026-04-20 00:48:52', '2026-04-20 00:48:52', 0),
(5, '广州印务', '刘经理', '020-88881005', '广东省广州市天河区珠江新城花城大道88号', '广告制作', 1, NULL, '2026-04-20 00:48:52', '2026-04-20 00:48:52', 0);

-- --------------------------------------------------------

--
-- 表的结构 `fac_factory_bill`
--

CREATE TABLE `fac_factory_bill` (
  `id` bigint(20) NOT NULL,
  `bill_no` varchar(50) NOT NULL COMMENT '账单编号',
  `factory_id` bigint(20) NOT NULL COMMENT '工厂ID',
  `factory_name` varchar(200) DEFAULT NULL COMMENT '工厂名称（冗余）',
  `month` varchar(20) NOT NULL COMMENT '账单月份（如 2026年04月）',
  `total_amount` decimal(15,2) NOT NULL DEFAULT 0.00 COMMENT '账单总额',
  `paid_amount` decimal(15,2) DEFAULT 0.00 COMMENT '已付金额',
  `status` tinyint(4) DEFAULT 1 COMMENT '状态：1未对账 2已对账 3部分付款 4已结清',
  `reconcile_file` varchar(500) DEFAULT NULL COMMENT '对账文件URL',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `creator_id` bigint(20) DEFAULT NULL,
  `create_time` datetime DEFAULT current_timestamp(),
  `update_time` datetime DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  `deleted` tinyint(4) NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='工厂账单表';

--
-- 转存表中的数据 `fac_factory_bill`
--

INSERT INTO `fac_factory_bill` (`id`, `bill_no`, `factory_id`, `factory_name`, `month`, `total_amount`, `paid_amount`, `status`, `reconcile_file`, `remark`, `creator_id`, `create_time`, `update_time`, `deleted`) VALUES
(1, 'FB202603001', 1, '杭州印刷一厂', '2026年03月', 86500.00, 60000.00, 1, NULL, NULL, NULL, '2026-04-20 00:48:53', '2026-04-20 00:48:53', 0),
(2, 'FB202603002', 1, '杭州印刷一厂', '2026年02月', 72000.00, 72000.00, 4, NULL, NULL, NULL, '2026-04-20 00:48:53', '2026-04-20 00:48:53', 0),
(3, 'FB202603003', 2, '上海印刷集团', '2026年03月', 120000.00, 50000.00, 3, NULL, NULL, NULL, '2026-04-20 00:48:53', '2026-04-20 00:48:53', 0),
(4, 'FB202603004', 2, '上海印刷集团', '2026年02月', 98000.00, 98000.00, 4, NULL, NULL, NULL, '2026-04-20 00:48:53', '2026-04-20 00:48:53', 0),
(5, 'FB202603005', 3, '北京印刷厂', '2026年03月', 56000.00, 0.00, 1, NULL, NULL, NULL, '2026-04-20 00:48:53', '2026-04-20 00:48:53', 0),
(6, 'FB202603006', 4, '深圳包装公司', '2026年03月', 78000.00, 30000.00, 3, NULL, NULL, NULL, '2026-04-20 00:48:53', '2026-04-20 00:48:53', 0),
(7, 'FB202603007', 5, '广州印务', '2026年03月', 43000.00, 43000.00, 4, NULL, NULL, NULL, '2026-04-20 00:48:53', '2026-04-20 00:48:53', 0);

-- --------------------------------------------------------

--
-- 表的结构 `fin_record`
--

CREATE TABLE `fin_record` (
  `id` bigint(20) NOT NULL,
  `record_no` varchar(50) NOT NULL COMMENT '流水号',
  `type` varchar(20) NOT NULL COMMENT '类型：income收入 expense支出',
  `category` varchar(50) DEFAULT NULL COMMENT '类别：订单收入/充值收入/退款/采购支出/工资/房租',
  `amount` decimal(15,2) NOT NULL COMMENT '金额',
  `related_id` bigint(20) DEFAULT NULL COMMENT '关联ID（订单ID/会员ID）',
  `related_name` varchar(200) DEFAULT NULL COMMENT '关联名称',
  `payment_method` varchar(50) DEFAULT NULL COMMENT '支付方式：现金/转账/微信/支付宝',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `creator_id` bigint(20) DEFAULT NULL,
  `create_time` datetime DEFAULT current_timestamp(),
  `deleted` tinyint(4) NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='财务流水表';

--
-- 转存表中的数据 `fin_record`
--

INSERT INTO `fin_record` (`id`, `record_no`, `type`, `category`, `amount`, `related_id`, `related_name`, `payment_method`, `remark`, `creator_id`, `create_time`, `deleted`) VALUES
(1, 'FIN20260418001', 'income', '订单收入', 15000.00, NULL, 'ORD20260418003', '转账', '企业VI设计尾款', NULL, '2026-04-20 00:48:54', 0),
(2, 'FIN20260418002', 'income', '充值收入', 10000.00, NULL, '杭州苏润阀门厂', '转账', '会员充值', NULL, '2026-04-20 00:48:54', 0),
(3, 'FIN20260418003', 'expense', '采购支出', 5000.00, NULL, '杭州印刷一厂', '转账', '印刷材料采购', NULL, '2026-04-20 00:48:54', 0),
(4, 'FIN20260418004', 'income', '订单收入', 20000.00, NULL, 'ORD20260418002', '微信', '户外广告预付款', NULL, '2026-04-20 00:48:54', 0);

-- --------------------------------------------------------

--
-- 表的结构 `mem_member`
--

CREATE TABLE `mem_member` (
  `id` bigint(20) NOT NULL,
  `member_name` varchar(200) NOT NULL COMMENT '会员名称',
  `contact_person` varchar(100) DEFAULT NULL COMMENT '联系人',
  `phone` varchar(20) NOT NULL COMMENT '手机号',
  `level` varchar(20) DEFAULT '铜牌' COMMENT '等级：铜牌/银牌/金牌/钻石',
  `balance` decimal(15,2) DEFAULT 0.00 COMMENT '账户余额（预存金额）',
  `total_recharge` decimal(15,2) DEFAULT 0.00 COMMENT '累计充值',
  `total_consume` decimal(15,2) DEFAULT 0.00 COMMENT '累计消费',
  `status` tinyint(4) DEFAULT 1 COMMENT '状态：1正常 0禁用',
  `creator_id` bigint(20) DEFAULT NULL,
  `create_time` datetime DEFAULT current_timestamp(),
  `update_time` datetime DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  `deleted` tinyint(4) NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='会员表';

--
-- 转存表中的数据 `mem_member`
--

INSERT INTO `mem_member` (`id`, `member_name`, `contact_person`, `phone`, `level`, `balance`, `total_recharge`, `total_consume`, `status`, `creator_id`, `create_time`, `update_time`, `deleted`) VALUES
(1, '杭州苏润阀门厂', '苏高于', '19818226202', '金牌', 50000.00, 100000.00, 0.00, 1, NULL, '2026-04-20 00:48:53', '2026-04-20 00:48:53', 0),
(2, '浙江华新传媒', '王总', '13900139001', '钻石', 80000.00, 200000.00, 0.00, 1, NULL, '2026-04-20 00:48:53', '2026-04-20 00:48:53', 0),
(3, '上海蓝图设计', '李总监', '13800138010', '银牌', 30000.00, 50000.00, 0.00, 1, NULL, '2026-04-20 00:48:53', '2026-04-20 00:48:53', 0);

-- --------------------------------------------------------

--
-- 表的结构 `mem_member_transaction`
--

CREATE TABLE `mem_member_transaction` (
  `id` bigint(20) NOT NULL,
  `member_id` bigint(20) NOT NULL,
  `type` varchar(20) NOT NULL COMMENT '类型：recharge充值 consume消费',
  `amount` decimal(15,2) NOT NULL COMMENT '金额（正数）',
  `balance_before` decimal(15,2) DEFAULT NULL COMMENT '变动前余额',
  `balance_after` decimal(15,2) DEFAULT NULL COMMENT '变动后余额',
  `order_id` bigint(20) DEFAULT NULL COMMENT '关联订单ID（消费时）',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `creator_id` bigint(20) DEFAULT NULL,
  `create_time` datetime DEFAULT current_timestamp(),
  `deleted` tinyint(4) NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='会员流水表';

--
-- 转存表中的数据 `mem_member_transaction`
--

INSERT INTO `mem_member_transaction` (`id`, `member_id`, `type`, `amount`, `balance_before`, `balance_after`, `order_id`, `remark`, `creator_id`, `create_time`, `deleted`) VALUES
(1, 1, 'recharge', 100000.00, 0.00, 50000.00, NULL, '初始充值', NULL, '2026-04-20 00:48:53', 0),
(2, 2, 'recharge', 200000.00, 0.00, 80000.00, NULL, '初始充值', NULL, '2026-04-20 00:48:53', 0),
(3, 3, 'recharge', 50000.00, 0.00, 30000.00, NULL, '初始充值', NULL, '2026-04-20 00:48:53', 0);

-- --------------------------------------------------------

--
-- 表的结构 `ord_order`
--

CREATE TABLE `ord_order` (
  `id` bigint(20) NOT NULL,
  `order_no` varchar(50) NOT NULL COMMENT '订单编号',
  `customer_id` bigint(20) DEFAULT NULL COMMENT '客户ID',
  `customer_name` varchar(200) DEFAULT NULL COMMENT '客户名称（冗余）',
  `member_id` bigint(20) DEFAULT NULL COMMENT '会员ID',
  `title` varchar(500) NOT NULL COMMENT '订单标题',
  `description` text DEFAULT NULL COMMENT '订单描述',
  `order_type` tinyint(4) DEFAULT 1 COMMENT '类型：1印刷 2广告 3设计',
  `total_amount` decimal(15,2) NOT NULL DEFAULT 0.00 COMMENT '订单总额',
  `paid_amount` decimal(15,2) DEFAULT 0.00 COMMENT '已付金额',
  `discount_amount` decimal(15,2) DEFAULT 0.00 COMMENT '优惠金额',
  `status` tinyint(4) DEFAULT 1 COMMENT '状态：1待确认 2进行中 3已完成 4已取消',
  `payment_status` tinyint(4) DEFAULT 1 COMMENT '支付：1未付 2部分付 3已付清',
  `contact_person` varchar(100) DEFAULT NULL COMMENT '联系人',
  `contact_phone` varchar(20) DEFAULT NULL COMMENT '联系电话',
  `delivery_address` varchar(500) DEFAULT NULL COMMENT '交付地址',
  `remark` text DEFAULT NULL COMMENT '备注',
  `creator_id` bigint(20) DEFAULT NULL,
  `create_time` datetime DEFAULT current_timestamp(),
  `update_time` datetime DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  `deleted` tinyint(4) NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='订单主表';

--
-- 转存表中的数据 `ord_order`
--

INSERT INTO `ord_order` (`id`, `order_no`, `customer_id`, `customer_name`, `member_id`, `title`, `description`, `order_type`, `total_amount`, `paid_amount`, `discount_amount`, `status`, `payment_status`, `contact_person`, `contact_phone`, `delivery_address`, `remark`, `creator_id`, `create_time`, `update_time`, `deleted`) VALUES
(1, 'ORD20260418001', 1, '杭州苏润阀门厂', NULL, '名片印刷500张 + 宣传册1000本', NULL, 1, 8500.00, 5000.00, 0.00, 2, 2, '苏高于', '19818226202', NULL, NULL, NULL, '2026-04-20 00:48:53', '2026-04-20 00:48:53', 0),
(2, 'ORD20260418002', 2, '浙江华新传媒', NULL, '户外广告牌设计制作', NULL, 2, 35000.00, 20000.00, 0.00, 2, 2, '王总', '13900139001', NULL, NULL, NULL, '2026-04-20 00:48:53', '2026-04-20 00:48:53', 0),
(3, 'ORD20260418003', 3, '上海蓝图设计', NULL, '企业VI全套设计', NULL, 3, 15000.00, 15000.00, 0.00, 3, 3, '李总监', '13800138010', NULL, NULL, NULL, '2026-04-20 00:48:53', '2026-04-20 00:48:53', 0);

-- --------------------------------------------------------

--
-- 表的结构 `ord_order_material`
--

CREATE TABLE `ord_order_material` (
  `id` bigint(20) NOT NULL,
  `order_id` bigint(20) NOT NULL,
  `material_name` varchar(200) NOT NULL COMMENT '物料名称',
  `spec` varchar(200) DEFAULT NULL COMMENT '规格',
  `unit` varchar(20) DEFAULT NULL COMMENT '单位',
  `quantity` decimal(10,2) DEFAULT 1.00 COMMENT '数量',
  `unit_price` decimal(15,2) DEFAULT 0.00 COMMENT '单价',
  `amount` decimal(15,2) DEFAULT 0.00 COMMENT '小计金额',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_time` datetime DEFAULT current_timestamp(),
  `deleted` tinyint(4) NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='订单物料明细表';

-- --------------------------------------------------------

--
-- 表的结构 `sys_permission`
--

CREATE TABLE `sys_permission` (
  `id` bigint(20) NOT NULL COMMENT '权限ID',
  `parent_id` bigint(20) NOT NULL DEFAULT 0 COMMENT '父ID（0为根）',
  `name` varchar(100) NOT NULL COMMENT '权限名称',
  `type` varchar(20) NOT NULL DEFAULT 'menu' COMMENT '类型：menu/button/interface',
  `path` varchar(255) DEFAULT NULL COMMENT '路由路径',
  `component` varchar(255) DEFAULT NULL COMMENT '前端组件路径',
  `icon` varchar(100) DEFAULT NULL COMMENT '图标',
  `permission_code` varchar(100) DEFAULT NULL COMMENT '权限码（如 order:create）',
  `sort` int(11) DEFAULT 0 COMMENT '排序',
  `visible` tinyint(4) DEFAULT 1 COMMENT '是否显示：0隐藏 1显示',
  `status` tinyint(4) NOT NULL DEFAULT 1 COMMENT '状态：0禁用 1正常',
  `create_time` datetime DEFAULT current_timestamp(),
  `update_time` datetime DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  `deleted` tinyint(4) NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='系统权限表';

--
-- 转存表中的数据 `sys_permission`
--

INSERT INTO `sys_permission` (`id`, `parent_id`, `name`, `type`, `path`, `component`, `icon`, `permission_code`, `sort`, `visible`, `status`, `create_time`, `update_time`, `deleted`) VALUES
(1, 0, '仪表盘', 'menu', '/dashboard', 'dashboard/index', '📊', NULL, 1, 1, 1, '2026-04-20 00:48:52', '2026-04-20 00:48:52', 0),
(2, 0, '订单管理', 'menu', '/orders', 'order/index', '📋', NULL, 2, 1, 1, '2026-04-20 00:48:52', '2026-04-20 00:48:52', 0),
(3, 0, '客户管理', 'menu', '/customers', 'customer/index', '👥', NULL, 3, 1, 1, '2026-04-20 00:48:52', '2026-04-20 00:48:52', 0),
(4, 0, '会员管理', 'menu', '/members', 'member/index', '🎫', NULL, 4, 1, 1, '2026-04-20 00:48:52', '2026-04-20 00:48:52', 0),
(5, 0, '工厂账单', 'menu', '/factory', 'factory/index', '🏭', NULL, 5, 1, 1, '2026-04-20 00:48:52', '2026-04-20 00:48:52', 0),
(6, 0, '财务管理', 'menu', '/finance', 'finance/index', '💰', NULL, 6, 1, 1, '2026-04-20 00:48:52', '2026-04-20 00:48:52', 0),
(7, 0, '系统管理', 'menu', '/system', 'system/index', '⚙️', NULL, 7, 1, 1, '2026-04-20 00:48:52', '2026-04-20 00:48:52', 0),
(8, 1, '查看仪表盘', 'button', NULL, NULL, NULL, 'dashboard:view', 1, 1, 1, '2026-04-20 00:48:52', '2026-04-20 00:48:52', 0),
(9, 2, '查看订单', 'button', NULL, NULL, NULL, 'order:list', 1, 1, 1, '2026-04-20 00:48:52', '2026-04-20 00:48:52', 0),
(10, 2, '新建订单', 'button', NULL, NULL, NULL, 'order:create', 2, 1, 1, '2026-04-20 00:48:52', '2026-04-20 00:48:52', 0),
(11, 2, '编辑订单', 'button', NULL, NULL, NULL, 'order:edit', 3, 1, 1, '2026-04-20 00:48:52', '2026-04-20 00:48:52', 0),
(12, 2, '删除订单', 'button', NULL, NULL, NULL, 'order:delete', 4, 1, 1, '2026-04-20 00:48:52', '2026-04-20 00:48:52', 0),
(13, 3, '查看客户', 'button', NULL, NULL, NULL, 'customer:list', 1, 1, 1, '2026-04-20 00:48:52', '2026-04-20 00:48:52', 0),
(14, 3, '新建客户', 'button', NULL, NULL, NULL, 'customer:create', 2, 1, 1, '2026-04-20 00:48:52', '2026-04-20 00:48:52', 0),
(15, 3, '编辑客户', 'button', NULL, NULL, NULL, 'customer:edit', 3, 1, 1, '2026-04-20 00:48:52', '2026-04-20 00:48:52', 0),
(16, 3, '删除客户', 'button', NULL, NULL, NULL, 'customer:delete', 4, 1, 1, '2026-04-20 00:48:52', '2026-04-20 00:48:52', 0),
(17, 4, '查看会员', 'button', NULL, NULL, NULL, 'member:list', 1, 1, 1, '2026-04-20 00:48:52', '2026-04-20 00:48:52', 0),
(18, 4, '新建会员', 'button', NULL, NULL, NULL, 'member:create', 2, 1, 1, '2026-04-20 00:48:52', '2026-04-20 00:48:52', 0),
(19, 4, '编辑会员', 'button', NULL, NULL, NULL, 'member:edit', 3, 1, 1, '2026-04-20 00:48:52', '2026-04-20 00:48:52', 0),
(20, 4, '删除会员', 'button', NULL, NULL, NULL, 'member:delete', 4, 1, 1, '2026-04-20 00:48:52', '2026-04-20 00:48:52', 0),
(21, 4, '会员充值', 'button', NULL, NULL, NULL, 'member:recharge', 5, 1, 1, '2026-04-20 00:48:52', '2026-04-20 00:48:52', 0),
(22, 5, '查看账单', 'button', NULL, NULL, NULL, 'factory:list', 1, 1, 1, '2026-04-20 00:48:52', '2026-04-20 00:48:52', 0),
(23, 5, '新建账单', 'button', NULL, NULL, NULL, 'factory:create', 2, 1, 1, '2026-04-20 00:48:52', '2026-04-20 00:48:52', 0),
(24, 5, '编辑账单', 'button', NULL, NULL, NULL, 'factory:edit', 3, 1, 1, '2026-04-20 00:48:52', '2026-04-20 00:48:52', 0),
(25, 5, '删除账单', 'button', NULL, NULL, NULL, 'factory:delete', 4, 1, 1, '2026-04-20 00:48:52', '2026-04-20 00:48:52', 0),
(26, 6, '查看财务', 'button', NULL, NULL, NULL, 'finance:view', 1, 1, 1, '2026-04-20 00:48:52', '2026-04-20 00:48:52', 0),
(27, 6, '编辑财务', 'button', NULL, NULL, NULL, 'finance:edit', 2, 1, 1, '2026-04-20 00:48:52', '2026-04-20 00:48:52', 0),
(28, 7, '用户管理', 'button', NULL, NULL, NULL, 'system:user', 1, 1, 1, '2026-04-20 00:48:52', '2026-04-20 00:48:52', 0);

-- --------------------------------------------------------

--
-- 表的结构 `sys_role`
--

CREATE TABLE `sys_role` (
  `id` bigint(20) NOT NULL COMMENT '角色ID',
  `role_name` varchar(50) NOT NULL COMMENT '角色名称',
  `role_code` varchar(50) NOT NULL COMMENT '角色编码',
  `description` varchar(255) DEFAULT NULL COMMENT '描述',
  `sort` int(11) DEFAULT 0 COMMENT '排序',
  `status` tinyint(4) NOT NULL DEFAULT 1 COMMENT '状态：0禁用 1正常',
  `create_time` datetime DEFAULT current_timestamp(),
  `update_time` datetime DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  `deleted` tinyint(4) NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='系统角色表';

--
-- 转存表中的数据 `sys_role`
--

INSERT INTO `sys_role` (`id`, `role_name`, `role_code`, `description`, `sort`, `status`, `create_time`, `update_time`, `deleted`) VALUES
(1, '超级管理员', 'SUPER_ADMIN', '拥有所有权限', 1, 1, '2026-04-20 00:48:52', '2026-04-20 00:48:52', 0),
(2, '管理员', 'ADMIN', '除系统配置外全部权限', 2, 1, '2026-04-20 00:48:52', '2026-04-20 00:48:52', 0),
(3, '财务', 'FINANCE', '财务和订单查看', 3, 1, '2026-04-20 00:48:52', '2026-04-20 00:48:52', 0),
(4, '操作员', 'OPERATOR', '订单增删改', 4, 1, '2026-04-20 00:48:52', '2026-04-20 00:48:52', 0),
(5, '访客', 'VIEWER', '只读权限', 5, 1, '2026-04-20 00:48:52', '2026-04-20 00:48:52', 0);

-- --------------------------------------------------------

--
-- 表的结构 `sys_role_permission`
--

CREATE TABLE `sys_role_permission` (
  `id` bigint(20) NOT NULL,
  `role_id` bigint(20) NOT NULL,
  `permission_id` bigint(20) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='角色权限关联表';

--
-- 转存表中的数据 `sys_role_permission`
--

INSERT INTO `sys_role_permission` (`id`, `role_id`, `permission_id`) VALUES
(1, 1, 1),
(2, 1, 2),
(3, 1, 3),
(4, 1, 4),
(5, 1, 5),
(6, 1, 6),
(7, 1, 7),
(8, 1, 8),
(9, 1, 9),
(10, 1, 10),
(11, 1, 11),
(12, 1, 12),
(13, 1, 13),
(14, 1, 14),
(15, 1, 15),
(16, 1, 16),
(17, 1, 17),
(18, 1, 18),
(19, 1, 19),
(20, 1, 20),
(21, 1, 21),
(22, 1, 22),
(23, 1, 23),
(24, 1, 24),
(25, 1, 25),
(26, 1, 26),
(27, 1, 27),
(28, 1, 28),
(32, 2, 1),
(33, 2, 2),
(34, 2, 3),
(35, 2, 4),
(36, 2, 5),
(37, 2, 6),
(38, 2, 7),
(39, 2, 8),
(40, 2, 9),
(41, 2, 10),
(42, 2, 11),
(43, 2, 12),
(44, 2, 13),
(45, 2, 14),
(46, 2, 15),
(47, 2, 16),
(48, 2, 17),
(49, 2, 18),
(50, 2, 19),
(51, 2, 20),
(52, 2, 21),
(53, 2, 22),
(54, 2, 23),
(55, 2, 24),
(56, 2, 25),
(57, 2, 26),
(58, 2, 27),
(59, 2, 28),
(63, 3, 8),
(64, 3, 9),
(65, 3, 13),
(66, 3, 26),
(67, 3, 27),
(70, 4, 8),
(71, 4, 9),
(72, 4, 10),
(73, 4, 11),
(74, 4, 12),
(75, 4, 13),
(76, 4, 14),
(77, 4, 15),
(78, 4, 16);

-- --------------------------------------------------------

--
-- 表的结构 `sys_user`
--

CREATE TABLE `sys_user` (
  `id` bigint(20) NOT NULL COMMENT '用户ID',
  `username` varchar(50) NOT NULL COMMENT '用户名',
  `password` varchar(255) NOT NULL COMMENT '密码（BCrypt加密）',
  `real_name` varchar(100) DEFAULT NULL COMMENT '真实姓名',
  `phone` varchar(20) DEFAULT NULL COMMENT '手机号',
  `email` varchar(100) DEFAULT NULL COMMENT '邮箱',
  `avatar` varchar(500) DEFAULT NULL COMMENT '头像URL',
  `status` tinyint(4) NOT NULL DEFAULT 1 COMMENT '状态：0禁用 1正常',
  `create_by` bigint(20) DEFAULT NULL COMMENT '创建人ID',
  `create_time` datetime DEFAULT current_timestamp() COMMENT '创建时间',
  `update_by` bigint(20) DEFAULT NULL COMMENT '更新人ID',
  `update_time` datetime DEFAULT current_timestamp() ON UPDATE current_timestamp() COMMENT '更新时间',
  `deleted` tinyint(4) NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删 1已删'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='系统用户表';

--
-- 转存表中的数据 `sys_user`
--

INSERT INTO `sys_user` (`id`, `username`, `password`, `real_name`, `phone`, `email`, `avatar`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES
(1, 'admin', '$2a$10$6L6HHQZ9gA8o3zOEDRkm7OToMOM0/z0.LfYUSZLzG84GT7eyzMKJa', '系统管理员', '13800138000', NULL, NULL, 1, NULL, '2026-04-20 00:48:51', NULL, '2026-04-20 01:29:26', 0),
(2, 'finance', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '财务小王', '13800138001', NULL, NULL, 1, NULL, '2026-04-20 00:48:51', NULL, '2026-04-20 00:48:51', 0),
(3, 'operator', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '操作员小李', '13800138002', NULL, NULL, 1, NULL, '2026-04-20 00:48:51', NULL, '2026-04-20 00:48:51', 0);

-- --------------------------------------------------------

--
-- 表的结构 `sys_user_role`
--

CREATE TABLE `sys_user_role` (
  `id` bigint(20) NOT NULL,
  `user_id` bigint(20) NOT NULL,
  `role_id` bigint(20) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户角色关联表';

--
-- 转存表中的数据 `sys_user_role`
--

INSERT INTO `sys_user_role` (`id`, `user_id`, `role_id`) VALUES
(4, 1, 1),
(2, 2, 3),
(3, 3, 4);

--
-- 转储表的索引
--

--
-- 表的索引 `crm_customer`
--
ALTER TABLE `crm_customer`
  ADD PRIMARY KEY (`id`),
  ADD KEY `idx_customer_name` (`customer_name`),
  ADD KEY `idx_phone` (`phone`);

--
-- 表的索引 `fac_factory`
--
ALTER TABLE `fac_factory`
  ADD PRIMARY KEY (`id`),
  ADD KEY `idx_factory_name` (`factory_name`);

--
-- 表的索引 `fac_factory_bill`
--
ALTER TABLE `fac_factory_bill`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `bill_no` (`bill_no`),
  ADD KEY `idx_factory_id` (`factory_id`),
  ADD KEY `idx_month` (`month`),
  ADD KEY `idx_status` (`status`);

--
-- 表的索引 `fin_record`
--
ALTER TABLE `fin_record`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `record_no` (`record_no`),
  ADD KEY `idx_type` (`type`),
  ADD KEY `idx_create_time` (`create_time`);

--
-- 表的索引 `mem_member`
--
ALTER TABLE `mem_member`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `idx_phone` (`phone`),
  ADD KEY `idx_member_name` (`member_name`);

--
-- 表的索引 `mem_member_transaction`
--
ALTER TABLE `mem_member_transaction`
  ADD PRIMARY KEY (`id`),
  ADD KEY `idx_member_id` (`member_id`),
  ADD KEY `idx_create_time` (`create_time`);

--
-- 表的索引 `ord_order`
--
ALTER TABLE `ord_order`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `order_no` (`order_no`),
  ADD KEY `idx_order_no` (`order_no`),
  ADD KEY `idx_customer_id` (`customer_id`),
  ADD KEY `idx_status` (`status`),
  ADD KEY `idx_create_time` (`create_time`);

--
-- 表的索引 `ord_order_material`
--
ALTER TABLE `ord_order_material`
  ADD PRIMARY KEY (`id`),
  ADD KEY `idx_order_id` (`order_id`);

--
-- 表的索引 `sys_permission`
--
ALTER TABLE `sys_permission`
  ADD PRIMARY KEY (`id`),
  ADD KEY `idx_parent_id` (`parent_id`),
  ADD KEY `idx_permission_code` (`permission_code`);

--
-- 表的索引 `sys_role`
--
ALTER TABLE `sys_role`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `role_code` (`role_code`),
  ADD UNIQUE KEY `idx_role_code` (`role_code`);

--
-- 表的索引 `sys_role_permission`
--
ALTER TABLE `sys_role_permission`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `idx_role_permission` (`role_id`,`permission_id`),
  ADD KEY `idx_role_id` (`role_id`),
  ADD KEY `idx_permission_id` (`permission_id`);

--
-- 表的索引 `sys_user`
--
ALTER TABLE `sys_user`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `username` (`username`),
  ADD KEY `idx_username` (`username`),
  ADD KEY `idx_status` (`status`);

--
-- 表的索引 `sys_user_role`
--
ALTER TABLE `sys_user_role`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `idx_user_role` (`user_id`,`role_id`),
  ADD KEY `idx_user_id` (`user_id`),
  ADD KEY `idx_role_id` (`role_id`);

--
-- 在导出的表使用AUTO_INCREMENT
--

--
-- 使用表AUTO_INCREMENT `crm_customer`
--
ALTER TABLE `crm_customer`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=5;

--
-- 使用表AUTO_INCREMENT `fac_factory`
--
ALTER TABLE `fac_factory`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=6;

--
-- 使用表AUTO_INCREMENT `fac_factory_bill`
--
ALTER TABLE `fac_factory_bill`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=8;

--
-- 使用表AUTO_INCREMENT `fin_record`
--
ALTER TABLE `fin_record`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=5;

--
-- 使用表AUTO_INCREMENT `mem_member`
--
ALTER TABLE `mem_member`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=4;

--
-- 使用表AUTO_INCREMENT `mem_member_transaction`
--
ALTER TABLE `mem_member_transaction`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=4;

--
-- 使用表AUTO_INCREMENT `ord_order`
--
ALTER TABLE `ord_order`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=4;

--
-- 使用表AUTO_INCREMENT `ord_order_material`
--
ALTER TABLE `ord_order_material`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT;

--
-- 使用表AUTO_INCREMENT `sys_permission`
--
ALTER TABLE `sys_permission`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '权限ID', AUTO_INCREMENT=29;

--
-- 使用表AUTO_INCREMENT `sys_role`
--
ALTER TABLE `sys_role`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '角色ID', AUTO_INCREMENT=6;

--
-- 使用表AUTO_INCREMENT `sys_role_permission`
--
ALTER TABLE `sys_role_permission`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=85;

--
-- 使用表AUTO_INCREMENT `sys_user`
--
ALTER TABLE `sys_user`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '用户ID', AUTO_INCREMENT=4;

--
-- 使用表AUTO_INCREMENT `sys_user_role`
--
ALTER TABLE `sys_user_role`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=5;
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
