/*
 Navicat Premium Dump SQL

 Source Server         : wywsql
 Source Server Type    : MySQL
 Source Server Version : 80046 (8.0.46)
 Source Host           : localhost:3306
 Source Schema         : enterprise_ad

 Target Server Type    : MySQL
 Target Server Version : 80046 (8.0.46)
 File Encoding         : 65001

 Date: 30/04/2026 15:06:37
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for crm_customer
-- ----------------------------
DROP TABLE IF EXISTS `crm_customer`;
CREATE TABLE `crm_customer`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `customer_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '瀹㈡埛鍚嶇О',
  `contact_person` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '鑱旂郴浜',
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '鎵嬫満鍙',
  `telephone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '搴ф満',
  `email` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '閭??',
  `address` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '鍦板潃',
  `industry` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '琛屼笟',
  `customer_type` tinyint NOT NULL DEFAULT 1 COMMENT '瀹㈡埛绫诲瀷锛?=鏅??瀹㈡埛 2=宸ュ巶瀹㈡埛',
  `factory_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '宸ュ巶绫诲瀷锛堝嵃鍒?鍖呰?/骞垮憡鍒朵綔锛屼粎宸ュ巶瀹㈡埛锛',
  `total_amount` decimal(15, 2) NULL DEFAULT 0.00 COMMENT '绱??娑堣垂閲戦?',
  `order_count` int NULL DEFAULT 0 COMMENT '璁㈠崟鏁伴噺',
  `level` tinyint NULL DEFAULT 1 COMMENT '绛夌骇锛?鏅?? 2VIP 3鎴樼暐',
  `status` tinyint NULL DEFAULT 1 COMMENT '鐘舵?锛?姝ｅ父 0绂佺敤',
  `creator_id` bigint NULL DEFAULT NULL,
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_customer_name`(`customer_name` ASC) USING BTREE,
  INDEX `idx_phone`(`phone` ASC) USING BTREE,
  INDEX `idx_customer_type`(`customer_type` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 6 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '瀹㈡埛琛?紙鍚?櫘閫氬?鎴?宸ュ巶瀹㈡埛锛' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of crm_customer
-- ----------------------------
INSERT INTO `crm_customer` VALUES (1, '零售客户', NULL, NULL, NULL, NULL, NULL, '零售', 1, NULL, 0.00, 0, 1, 1, NULL, '2026-04-28 15:30:01', '2026-04-28 15:30:01', 0);
INSERT INTO `crm_customer` VALUES (2, '浙江万源鸿科技有限公司', NULL, '', NULL, NULL, '', NULL, 2, '包装', 0.00, 0, 3, 1, NULL, '2026-04-30 10:22:39', '2026-04-30 10:22:39', 0);
INSERT INTO `crm_customer` VALUES (3, '水泵', NULL, NULL, NULL, NULL, NULL, NULL, 1, NULL, 0.00, 0, 1, 1, NULL, '2026-04-30 12:25:30', '2026-04-30 12:25:30', 0);
INSERT INTO `crm_customer` VALUES (4, '李四（服装店）', NULL, '13900002222', NULL, NULL, NULL, NULL, 1, NULL, 0.00, 0, 1, 1, NULL, '2026-04-30 13:17:21', '2026-04-30 13:17:21', 0);
INSERT INTO `crm_customer` VALUES (5, '吴洋威', NULL, NULL, NULL, NULL, NULL, NULL, 1, NULL, 0.00, 0, 1, 1, NULL, '2026-04-30 14:12:13', '2026-04-30 14:12:13', 0);

-- ----------------------------
-- Table structure for customer_level
-- ----------------------------
DROP TABLE IF EXISTS `customer_level`;
CREATE TABLE `customer_level`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '绛夌骇鍚嶇О',
  `level` int NOT NULL COMMENT '绛夌骇鏉冮噸锛堟暟鍊艰秺澶х瓑绾ц秺楂橈級',
  `min_amount` decimal(12, 2) NULL DEFAULT 0.00 COMMENT '娑堣垂闂ㄦ?',
  `discount` int NULL DEFAULT 100 COMMENT '鎶樻墸姣斾緥锛堝?90=9鎶橈紝100=鍘熶环锛',
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '绛夌骇璇存槑',
  `sort` int NULL DEFAULT 0 COMMENT '鎺掑簭',
  `status` int NULL DEFAULT 1 COMMENT '鐘舵? 1姝ｅ父 0绂佺敤',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` int NULL DEFAULT 0 COMMENT '閫昏緫鍒犻櫎',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_level`(`level` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '瀹㈡埛绛夌骇琛' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of customer_level
-- ----------------------------

-- ----------------------------
-- Table structure for customer_tag
-- ----------------------------
DROP TABLE IF EXISTS `customer_tag`;
CREATE TABLE `customer_tag`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '鏍囩?鍚嶇О',
  `color` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '鏍囩?棰滆壊',
  `icon` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '鏍囩?鍥炬爣',
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '鏍囩?璇存槑',
  `sort` int NULL DEFAULT 0 COMMENT '鎺掑簭',
  `status` int NULL DEFAULT 1 COMMENT '鐘舵? 1姝ｅ父 0绂佺敤',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` int NULL DEFAULT 0 COMMENT '閫昏緫鍒犻櫎',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_sort`(`sort` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '瀹㈡埛鏍囩?琛' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of customer_tag
-- ----------------------------

-- ----------------------------
-- Table structure for des_file
-- ----------------------------
DROP TABLE IF EXISTS `des_file`;
CREATE TABLE `des_file`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '鏂囦欢鍚',
  `original_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '鍘熷?鏂囦欢鍚',
  `path` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '瀛樺偍璺?緞',
  `url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '璁块棶URL',
  `size` bigint NULL DEFAULT NULL COMMENT '鏂囦欢澶у皬锛堝瓧鑺傦級',
  `extension` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '鏂囦欢鎵╁睍鍚',
  `mime_type` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'MIME绫诲瀷',
  `order_id` bigint NULL DEFAULT NULL COMMENT '鍏宠仈璁㈠崟ID',
  `uploader_id` bigint NULL DEFAULT NULL COMMENT '涓婁紶浜篒D',
  `uploader_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '涓婁紶浜',
  `version` int NULL DEFAULT 1 COMMENT '鐗堟湰鍙',
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '鏂囦欢鎻忚堪',
  `status` tinyint NULL DEFAULT 1 COMMENT '鐘舵?锛?寰呭?鏍?2閫氳繃 3椹冲洖',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '瀹℃牳澶囨敞',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NULL DEFAULT NULL,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_order`(`order_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '璁捐?鏂囦欢琛' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of des_file
-- ----------------------------

-- ----------------------------
-- Table structure for des_file_version
-- ----------------------------
DROP TABLE IF EXISTS `des_file_version`;
CREATE TABLE `des_file_version`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `file_id` bigint NOT NULL COMMENT '鏂囦欢ID',
  `version` int NULL DEFAULT NULL COMMENT '鐗堟湰鍙',
  `file_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `file_size` bigint NULL DEFAULT NULL,
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `creator_id` bigint NULL DEFAULT NULL,
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_file`(`file_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '璁捐?鏂囦欢鐗堟湰璁板綍' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of des_file_version
-- ----------------------------

-- ----------------------------
-- Table structure for designer_commission_config
-- ----------------------------
DROP TABLE IF EXISTS `designer_commission_config`;
CREATE TABLE `designer_commission_config`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `designer_id` bigint NOT NULL COMMENT '设计师用户ID',
  `designer_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '设计师姓名（冗余）',
  `commission_rate` decimal(5, 2) NULL DEFAULT 0.00 COMMENT '提成比例（%）',
  `enabled` tinyint NULL DEFAULT 1 COMMENT '是否启用：0否 1是',
  `updated_by` bigint NULL DEFAULT NULL COMMENT '最后修改人',
  `updated_time` datetime NULL DEFAULT NULL COMMENT '最后修改时间',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `deleted` tinyint NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_designer`(`designer_id` ASC) USING BTREE,
  INDEX `idx_designer`(`designer_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '设计师提成比例配置' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of designer_commission_config
-- ----------------------------
INSERT INTO `designer_commission_config` VALUES (1, 3, '李明', 0.00, 1, NULL, NULL, '2026-04-28 19:57:47', 0);
INSERT INTO `designer_commission_config` VALUES (2, 4, '王设计', 0.00, 1, NULL, NULL, '2026-04-28 19:57:47', 0);

-- ----------------------------
-- Table structure for fac_factory_bill
-- ----------------------------
DROP TABLE IF EXISTS `fac_factory_bill`;
CREATE TABLE `fac_factory_bill`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `bill_no` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '璐﹀崟缂栧彿',
  `factory_id` bigint NULL DEFAULT NULL COMMENT '鍘熷伐鍘侷D锛堜繚鐣欏吋瀹癸級',
  `factory_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '宸ュ巶鍚嶇О锛堝啑浣欙級',
  `customer_id` bigint NULL DEFAULT NULL COMMENT '瀹㈡埛ID锛堝叧鑱攃rm_customer锛宑ustomer_type=2涓哄伐鍘傚?鎴凤級',
  `salesman_id` bigint NULL DEFAULT NULL COMMENT '涓氬姟鍛業D锛堝叧鑱攆ac_factory_salesman锛',
  `salesman_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '涓氬姟鍛樺?鍚嶏紙鍐椾綑锛',
  `bill_type` tinyint NOT NULL DEFAULT 1 COMMENT '账单类型：1=工厂账单 2=客户账单',
  `month` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '璐﹀崟鏈堜唤锛堝? 2026骞?4鏈堬級',
  `total_amount` decimal(15, 2) NOT NULL DEFAULT 0.00 COMMENT '璐﹀崟鎬婚?',
  `paid_amount` decimal(15, 2) NULL DEFAULT 0.00 COMMENT '宸蹭粯閲戦?',
  `status` tinyint NULL DEFAULT 1 COMMENT '鐘舵?锛?鏈??璐?2宸插?璐?3閮ㄥ垎浠樻? 4宸茬粨娓',
  `reconcile_file` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '瀵硅处鏂囦欢URL',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '澶囨敞',
  `creator_id` bigint NULL DEFAULT NULL,
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_bill_no`(((case when (`deleted` = 0) then `bill_no` else NULL end)) ASC) USING BTREE,
  INDEX `idx_factory_id`(`factory_id` ASC) USING BTREE,
  INDEX `idx_customer_id`(`customer_id` ASC) USING BTREE,
  INDEX `idx_month`(`month` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE,
  INDEX `idx_bill_type_customer`(`bill_type` ASC, `customer_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '宸ュ巶璐﹀崟琛' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of fac_factory_bill
-- ----------------------------
INSERT INTO `fac_factory_bill` VALUES (1, 'FB1777515783173', 2, NULL, NULL, NULL, NULL, 1, '2026年04月', 0.00, 0.00, 1, NULL, '', NULL, '2026-04-30 10:23:03', '2026-04-30 10:23:03', 0);

-- ----------------------------
-- Table structure for fac_factory_bill_detail
-- ----------------------------
DROP TABLE IF EXISTS `fac_factory_bill_detail`;
CREATE TABLE `fac_factory_bill_detail`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `bill_id` bigint NOT NULL COMMENT '鍏宠仈璐﹀崟ID锛坒ac_factory_bill.id锛',
  `bill_no` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '璐﹀崟缂栧彿锛堝啑浣欙紝鏂逛究鏌ヨ?锛',
  `record_date` date NOT NULL COMMENT '鐧昏?鏃ユ湡锛堝? 2026-04-01锛',
  `item_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '椤圭洰鍚嶇О锛堝?锛氬悕鐗囧嵃鍒?瀹ｄ紶鍐岋級',
  `spec` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '瑙勬牸璇存槑',
  `quantity` decimal(10, 2) NULL DEFAULT 1.00 COMMENT '鏁伴噺锛堟寜鏁伴噺璁′环鏃朵娇鐢?級',
  `unit` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '鍗曚綅锛堝紶/鏈?椤?骞虫柟绫筹級',
  `unit_price` decimal(15, 2) NULL DEFAULT 0.00 COMMENT '鍗曚环锛堟寜鏁伴噺=姣忎欢鍗曚环锛屾寜闈㈢Н=姣忋帯鍗曚环锛',
  `calc_mode` tinyint NOT NULL DEFAULT 1 COMMENT '璁′环鏂瑰紡: 1=鎸夋暟閲?2=鎸夐潰绉?骞虫柟) 3=鍥哄畾浠锋牸',
  `length_val` decimal(10, 3) NULL DEFAULT NULL COMMENT '闀垮害(m)锛屾寜闈㈢Н璁′环鏃朵娇鐢',
  `width_val` decimal(10, 3) NULL DEFAULT NULL COMMENT '瀹藉害(m)锛屾寜闈㈢Н璁′环鏃朵娇鐢',
  `area_sq` decimal(10, 3) NULL DEFAULT NULL COMMENT '璁＄畻闈㈢Н(銕?锛岀郴缁熻嚜鍔ㄧ畻',
  `amount` decimal(15, 2) NULL DEFAULT 0.00 COMMENT '灏忚?閲戦?锛堟牴鎹?calcMode 鑷?姩璁＄畻锛',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '澶囨敞',
  `creator_id` bigint NULL DEFAULT NULL,
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_bill_id`(`bill_id` ASC) USING BTREE,
  INDEX `idx_bill_no`(`bill_no` ASC) USING BTREE,
  INDEX `idx_record_date`(`record_date` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '宸ュ巶璐﹀崟鏄庣粏锛堟瘡鏃ョ櫥璁拌?褰曪紝鏀?寔澶氳?浠锋ā寮忥級' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of fac_factory_bill_detail
-- ----------------------------

-- ----------------------------
-- Table structure for fac_factory_salesman
-- ----------------------------
DROP TABLE IF EXISTS `fac_factory_salesman`;
CREATE TABLE `fac_factory_salesman`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '涓氬姟鍛樺?鍚',
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '鑱旂郴鐢佃瘽',
  `email` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '閭??',
  `factory_id` bigint NULL DEFAULT NULL COMMENT '鎵?睘宸ュ巶ID锛堝叧鑱攃rm_customer.id where customer_type=2锛',
  `status` tinyint NULL DEFAULT 1 COMMENT '鐘舵?锛?鍚?敤 0绂佺敤',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '澶囨敞',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_factory_id`(`factory_id` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '宸ュ巶涓氬姟鍛樿〃' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of fac_factory_salesman
-- ----------------------------
INSERT INTO `fac_factory_salesman` VALUES (1, 'Ann-陶', '', '', 2, 1, '', '2026-04-30 14:37:36', '2026-04-30 14:37:36', 0);

-- ----------------------------
-- Table structure for fin_designer_commission
-- ----------------------------
DROP TABLE IF EXISTS `fin_designer_commission`;
CREATE TABLE `fin_designer_commission`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `order_id` bigint NULL DEFAULT NULL COMMENT '鍏宠仈璁㈠崟ID',
  `order_no` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '璁㈠崟缂栧彿锛堝啑浣欙級',
  `designer_id` bigint NOT NULL COMMENT '璁捐?甯堢敤鎴稩D锛堝叧鑱攕ys_user锛',
  `designer_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '璁捐?甯堝?鍚',
  `base_amount` decimal(15, 2) NOT NULL DEFAULT 0.00 COMMENT '璁＄畻鍩烘暟锛堣?鍗曢噾棰濓級',
  `commission_rate` decimal(5, 2) NOT NULL DEFAULT 0.00 COMMENT '鎻愭垚姣斾緥锛堝? 5.00 浠ｈ〃 5%锛',
  `commission_amount` decimal(15, 2) NOT NULL DEFAULT 0.00 COMMENT '鎻愭垚閲戦?',
  `status` tinyint NULL DEFAULT 1 COMMENT '鐘舵?锛?寰呯粨绠?2宸茬粨绠?3宸叉墦娆',
  `settle_time` datetime NULL DEFAULT NULL COMMENT '缁撶畻鏃堕棿',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '澶囨敞',
  `creator_id` bigint NULL DEFAULT NULL,
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_order_id`(`order_id` ASC) USING BTREE,
  INDEX `idx_designer_id`(`designer_id` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '璁捐?甯堟彁鎴愯〃' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of fin_designer_commission
-- ----------------------------

-- ----------------------------
-- Table structure for fin_invoice
-- ----------------------------
DROP TABLE IF EXISTS `fin_invoice`;
CREATE TABLE `fin_invoice`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `invoice_no` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '鍙戠エ缂栧彿',
  `customer_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '瀹㈡埛鍚嶇О',
  `type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'normal' COMMENT '绫诲瀷锛歴pecial涓撶エ/normal鏅?エ/receipt鏀舵嵁',
  `amount` decimal(12, 2) NULL DEFAULT 0.00 COMMENT '鍙戠エ閲戦?',
  `tax_rate` decimal(5, 2) NULL DEFAULT 0.00 COMMENT '绋庣巼',
  `tax_amount` decimal(12, 2) NULL DEFAULT 0.00 COMMENT '绋庨?',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'pending' COMMENT '鐘舵?锛歝ompleted宸插紑鍏?pending寰呭紑鍏?cancelled宸蹭綔搴',
  `issue_date` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '寮?叿鏃ユ湡',
  `remark` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '澶囨敞',
  `creator_id` bigint NULL DEFAULT NULL COMMENT '鍒涘缓浜篒D',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_invoice_no`(((case when (`deleted` = 0) then `invoice_no` else NULL end)) ASC) USING BTREE,
  INDEX `idx_customer`(`customer_name` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '鍙戠エ璁板綍琛' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of fin_invoice
-- ----------------------------

-- ----------------------------
-- Table structure for fin_quote
-- ----------------------------
DROP TABLE IF EXISTS `fin_quote`;
CREATE TABLE `fin_quote`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `quote_no` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '鎶ヤ环缂栧彿',
  `customer_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '瀹㈡埛鍚嶇О',
  `project_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '椤圭洰鍚嶇О',
  `total_amount` decimal(12, 2) NULL DEFAULT 0.00 COMMENT '鎶ヤ环閲戦?',
  `discount` decimal(5, 2) NULL DEFAULT 100.00 COMMENT '鎶樻墸鐧惧垎姣',
  `final_amount` decimal(12, 2) NULL DEFAULT 0.00 COMMENT '鏈?粓閲戦?',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'pending' COMMENT '鐘舵?锛歱ending/accepted/rejected/expired',
  `valid_until` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '鏈夋晥鏈熻嚦',
  `remark` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '澶囨敞',
  `customer_id` bigint NULL DEFAULT NULL COMMENT '瀹㈡埛ID',
  `company_id` bigint NULL DEFAULT NULL COMMENT '鎶ヤ环鍏?徃ID',
  `tax_rate` decimal(5, 2) NULL DEFAULT 0.00 COMMENT '绋庣巼%',
  `tax_amount` decimal(12, 2) NULL DEFAULT 0.00 COMMENT '绋庨?',
  `quote_date` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '鎶ヤ环鏃ユ湡',
  `creator_id` bigint NULL DEFAULT NULL COMMENT '鍒涘缓浜篒D',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_quote_no`(((case when (`deleted` = 0) then `quote_no` else NULL end)) ASC) USING BTREE,
  INDEX `idx_customer`(`customer_name` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '鎶ヤ环璁板綍琛' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of fin_quote
-- ----------------------------
INSERT INTO `fin_quote` VALUES (1, 'QT20260430256', '零售客户', '草编', 705.00, 100.00, 712.05, 'accepted', '2026-05-30', '', 1, 1, 1.00, 7.05, NULL, NULL, '2026-04-30 13:59:01', '2026-04-30 14:04:48', 0);

-- ----------------------------
-- Table structure for fin_quote_detail
-- ----------------------------
DROP TABLE IF EXISTS `fin_quote_detail`;
CREATE TABLE `fin_quote_detail`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `quote_id` bigint NOT NULL COMMENT '鍏宠仈鎶ヤ环ID',
  `material_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '鐗╂枡鍚嶇О',
  `spec` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '瑙勬牸',
  `unit` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '鍗曚綅',
  `quantity` decimal(10, 2) NULL DEFAULT 1.00 COMMENT '鏁伴噺',
  `unit_price` decimal(15, 2) NULL DEFAULT 0.00 COMMENT '鍗曚环',
  `amount` decimal(15, 2) NULL DEFAULT 0.00 COMMENT '灏忚?閲戦?',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '澶囨敞',
  `is_custom` tinyint NULL DEFAULT 0 COMMENT '鏄?惁鎵嬪姩鐗╂枡(0=鍚?1=鏄?',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_quote_id`(`quote_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '鎶ヤ环鐗╂枡鏄庣粏琛' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of fin_quote_detail
-- ----------------------------
INSERT INTO `fin_quote_detail` VALUES (1, 1, 'KT板 60x90cm', '60x90cm 5mm', '张', 7.00, 15.00, 105.00, '', 0, '2026-04-30 13:59:01', 0);
INSERT INTO `fin_quote_detail` VALUES (2, 1, '写真布 1m宽', '1m宽 灯箱布', '米', 50.00, 12.00, 600.00, '', 0, '2026-04-30 13:59:01', 0);

-- ----------------------------
-- Table structure for fin_record
-- ----------------------------
DROP TABLE IF EXISTS `fin_record`;
CREATE TABLE `fin_record`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `record_no` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '娴佹按鍙',
  `type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '绫诲瀷锛歩ncome鏀跺叆 expense鏀?嚭',
  `category` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '绫诲埆锛氳?鍗曟敹鍏?鍏呭?鏀跺叆/閫??/閲囪喘鏀?嚭/宸ヨ祫/鎴跨?',
  `amount` decimal(15, 2) NOT NULL COMMENT '閲戦?',
  `related_id` bigint NULL DEFAULT NULL COMMENT '鍏宠仈ID锛堣?鍗旾D/浼氬憳ID锛',
  `related_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '鍏宠仈鍚嶇О',
  `payment_method` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '鏀?粯鏂瑰紡锛氱幇閲?杞?处/寰?俊/鏀?粯瀹',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '澶囨敞',
  `creator_id` bigint NULL DEFAULT NULL,
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_record_no`(((case when (`deleted` = 0) then `record_no` else NULL end)) ASC) USING BTREE,
  INDEX `idx_type`(`type` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '璐㈠姟娴佹按琛' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of fin_record
-- ----------------------------

-- ----------------------------
-- Table structure for fin_record_item
-- ----------------------------
DROP TABLE IF EXISTS `fin_record_item`;
CREATE TABLE `fin_record_item`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `record_id` bigint NOT NULL COMMENT '关联流水ID',
  `material_id` bigint NOT NULL COMMENT '物料ID',
  `material_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '物料名称',
  `pricing_type` tinyint NULL DEFAULT 0 COMMENT '计价方式：0按数量 1按面积',
  `quantity` int NULL DEFAULT 0 COMMENT '数量（按数量计价时使用）',
  `width` decimal(12, 2) NULL DEFAULT NULL COMMENT '宽度（按面积计价时使用）',
  `height` decimal(12, 2) NULL DEFAULT NULL COMMENT '高度（按面积计价时使用）',
  `area` decimal(12, 2) NULL DEFAULT NULL COMMENT '面积（宽×高）',
  `unit_price` decimal(15, 2) NOT NULL COMMENT '单价',
  `total_price` decimal(15, 2) NOT NULL COMMENT '小计金额（向上取整）',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_record_id`(`record_id` ASC) USING BTREE,
  INDEX `idx_material_id`(`material_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '财务流水物料明细表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of fin_record_item
-- ----------------------------

-- ----------------------------
-- Table structure for mat_category
-- ----------------------------
DROP TABLE IF EXISTS `mat_category`;
CREATE TABLE `mat_category`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '鍒嗙被鍚嶇О',
  `code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '鍒嗙被缂栫爜',
  `icon` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '馃摝' COMMENT '鍒嗙被鍥炬爣',
  `parent_id` bigint NULL DEFAULT 0 COMMENT '鐖跺垎绫籌D',
  `sort_order` int NULL DEFAULT 0,
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '鍒嗙被璇存槑',
  `status` int NULL DEFAULT 1 COMMENT '鐘舵?锛?姝ｅ父 0绂佺敤',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '鐗╂枡鍒嗙被琛' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of mat_category
-- ----------------------------
INSERT INTO `mat_category` VALUES (1, '印刷材料', 'PRINT', '📄', 0, 1, NULL, 1, '2026-04-28 15:30:01', 0);
INSERT INTO `mat_category` VALUES (2, '广告材料', 'AD', '🖼️', 0, 2, NULL, 1, '2026-04-28 15:30:01', 0);
INSERT INTO `mat_category` VALUES (3, '耗材', 'CONSUMABLE', '📦', 0, 3, NULL, 1, '2026-04-28 15:30:01', 0);
INSERT INTO `mat_category` VALUES (4, '包装材料', 'PACKAGE', '📦', 0, 4, NULL, 1, '2026-04-28 15:30:01', 0);

-- ----------------------------
-- Table structure for mat_material
-- ----------------------------
DROP TABLE IF EXISTS `mat_material`;
CREATE TABLE `mat_material`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '鐗╂枡鍚嶇О',
  `code` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '鐗╂枡缂栫爜',
  `category_id` bigint NULL DEFAULT NULL COMMENT '鍒嗙被ID',
  `category_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '鍒嗙被鍚嶇О锛堝啑浣欙級',
  `paper_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '绾稿紶绫诲瀷锛欰4/A3/SRA3 绛',
  `paper_spec` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '绾稿紶鏉愯川锛?0g鍙岃兌/128g閾滅増/157g閾滅増 绛',
  `colour_type` tinyint NULL DEFAULT 0 COMMENT '鑹插僵绫诲瀷锛?榛戠櫧 1褰╄壊',
  `paper_group` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '绾稿紶鍒嗙粍鏍囪瘑锛堝悓灏哄?+鏉愯川鍏变韩搴撳瓨锛夛紝濡?A4-128G-TB',
  `spec` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '瑙勬牸鍨嬪彿',
  `unit` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '璁￠噺鍗曚綅',
  `price` decimal(15, 2) NULL DEFAULT NULL COMMENT '闆跺敭浠',
  `cost_price` decimal(15, 2) NULL DEFAULT NULL COMMENT '鎴愭湰浠',
  `factory_price` decimal(15, 2) NULL DEFAULT NULL COMMENT '宸ュ巶浠',
  `pricing_type` tinyint NULL DEFAULT 0 COMMENT '计价方式：0按数量 1按面积',
  `stock_quantity` int NULL DEFAULT 0 COMMENT '搴撳瓨鏁伴噺',
  `warning_quantity` int NULL DEFAULT 10 COMMENT '棰勮?搴撳瓨',
  `min_quantity` int NULL DEFAULT 5 COMMENT '鏈?皬搴撳瓨',
  `status` tinyint NULL DEFAULT 1 COMMENT '鐘舵?锛?姝ｅ父 0绂佺敤',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NULL DEFAULT NULL,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_material_code`(((case when (`deleted` = 0) then `code` else NULL end)) ASC) USING BTREE,
  INDEX `idx_category`(`category_id` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 8 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '鐗╂枡搴撳瓨琛' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of mat_material
-- ----------------------------
INSERT INTO `mat_material` VALUES (1, '铜版纸 A4', 'MAT001', 1, '印刷材料', NULL, NULL, 0, NULL, 'A4 105g', '张', 0.50, 0.30, NULL, 0, 5000, 500, 200, 1, NULL, '2026-04-28 15:30:01', NULL, 0);
INSERT INTO `mat_material` VALUES (2, '铜版纸 A3', 'MAT002', 1, '印刷材料', NULL, NULL, 0, NULL, 'A3 105g', '张', 0.90, 0.55, NULL, 0, 3000, 300, 100, 1, NULL, '2026-04-28 15:30:01', NULL, 0);
INSERT INTO `mat_material` VALUES (3, '哑粉纸 A4', 'MAT003', 1, '印刷材料', NULL, NULL, 0, NULL, 'A4 128g', '张', 0.60, 0.38, NULL, 0, 2000, 200, 100, 1, NULL, '2026-04-28 15:30:01', NULL, 0);
INSERT INTO `mat_material` VALUES (4, 'KT板 60x90cm', 'MAT004', 2, '广告材料', NULL, NULL, 0, NULL, '60x90cm 5mm', '张', 15.00, 8.00, NULL, 0, 200, 30, 10, 1, NULL, '2026-04-28 15:30:01', NULL, 0);
INSERT INTO `mat_material` VALUES (5, '写真布 1m宽', 'MAT005', 2, '广告材料', NULL, NULL, 0, NULL, '1m宽 灯箱布', '米', 12.00, 7.00, NULL, 0, 500, 50, 20, 1, NULL, '2026-04-28 15:30:01', NULL, 0);
INSERT INTO `mat_material` VALUES (6, '墨盒（黑色）', 'MAT006', 3, '耗材', NULL, NULL, 0, NULL, '通用型', '个', 85.00, 55.00, NULL, 0, 20, 5, 2, 1, NULL, '2026-04-28 15:30:01', NULL, 0);
INSERT INTO `mat_material` VALUES (7, '装订胶', 'MAT007', 3, '耗材', NULL, NULL, 0, NULL, '500ml', '瓶', 35.00, 20.00, NULL, 0, 15, 5, 3, 1, NULL, '2026-04-28 15:30:01', NULL, 0);

-- ----------------------------
-- Table structure for mat_stock_log
-- ----------------------------
DROP TABLE IF EXISTS `mat_stock_log`;
CREATE TABLE `mat_stock_log`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `material_id` bigint NOT NULL,
  `material_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `change_type` tinyint NULL DEFAULT NULL COMMENT '鍙樺姩绫诲瀷锛?鍏ュ簱 2鍑哄簱 3璋冩暣',
  `quantity` int NULL DEFAULT NULL COMMENT '鍙樺姩鏁伴噺锛堣礋鏁颁负鍑哄簱锛',
  `before_stock` int NULL DEFAULT NULL COMMENT '鍙樺姩鍓嶅簱瀛',
  `after_stock` int NULL DEFAULT NULL COMMENT '鍙樺姩鍚庡簱瀛',
  `unit_price` decimal(15, 2) NULL DEFAULT NULL,
  `total_price` decimal(15, 2) NULL DEFAULT NULL,
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `operator_id` bigint NULL DEFAULT NULL COMMENT '鎿嶄綔浜篒D',
  `operator_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '鎿嶄綔浜哄?鍚',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_material`(`material_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '搴撳瓨鍙樺姩鏃ュ織' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of mat_stock_log
-- ----------------------------

-- ----------------------------
-- Table structure for mem_member
-- ----------------------------
DROP TABLE IF EXISTS `mem_member`;
CREATE TABLE `mem_member`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `member_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '浼氬憳鍚嶇О',
  `contact_person` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '鑱旂郴浜',
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '鎵嬫満鍙',
  `level` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '閾滅墝' COMMENT '绛夌骇锛氶摐鐗?閾剁墝/閲戠墝/閽荤煶',
  `balance` decimal(15, 2) NULL DEFAULT 0.00 COMMENT '璐︽埛浣欓?锛堥?瀛橀噾棰濓級',
  `total_recharge` decimal(15, 2) NULL DEFAULT 0.00 COMMENT '绱??鍏呭?',
  `total_consume` decimal(15, 2) NULL DEFAULT 0.00 COMMENT '绱??娑堣垂',
  `status` tinyint NULL DEFAULT 1 COMMENT '鐘舵?锛?姝ｅ父 0绂佺敤',
  `creator_id` bigint NULL DEFAULT NULL,
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_member_phone`(((case when (`deleted` = 0) then `phone` else NULL end)) ASC) USING BTREE,
  INDEX `idx_member_name`(`member_name` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '浼氬憳琛' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of mem_member
-- ----------------------------
INSERT INTO `mem_member` VALUES (1, '吴洋威', NULL, '12345655566', 'diamond', 5000.00, 0.00, 0.00, 1, NULL, '2026-04-30 14:09:30', '2026-04-30 14:09:30', 0);

-- ----------------------------
-- Table structure for mem_member_transaction
-- ----------------------------
DROP TABLE IF EXISTS `mem_member_transaction`;
CREATE TABLE `mem_member_transaction`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `member_id` bigint NOT NULL,
  `type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '绫诲瀷锛歳echarge鍏呭? consume娑堣垂',
  `amount` decimal(15, 2) NOT NULL COMMENT '閲戦?锛堟?鏁帮級',
  `balance_before` decimal(15, 2) NULL DEFAULT NULL COMMENT '鍙樺姩鍓嶄綑棰',
  `balance_after` decimal(15, 2) NULL DEFAULT NULL COMMENT '鍙樺姩鍚庝綑棰',
  `order_id` bigint NULL DEFAULT NULL COMMENT '鍏宠仈璁㈠崟ID锛堟秷璐规椂锛',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '澶囨敞',
  `creator_id` bigint NULL DEFAULT NULL,
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_member_id`(`member_id` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '浼氬憳娴佹按琛' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of mem_member_transaction
-- ----------------------------
INSERT INTO `mem_member_transaction` VALUES (1, 1, 'recharge', 5000.00, 0.00, 5000.00, NULL, '初始充值', NULL, '2026-04-30 14:09:30', 0);

-- ----------------------------
-- Table structure for ord_order
-- ----------------------------
DROP TABLE IF EXISTS `ord_order`;
CREATE TABLE `ord_order`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `order_no` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '璁㈠崟缂栧彿',
  `customer_id` bigint NULL DEFAULT NULL COMMENT '瀹㈡埛ID',
  `customer_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '瀹㈡埛鍚嶇О锛堝啑浣欙級',
  `member_id` bigint NULL DEFAULT NULL COMMENT '浼氬憳ID',
  `title` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '璁㈠崟鏍囬?',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '璁㈠崟鎻忚堪',
  `order_type` tinyint NULL DEFAULT 1 COMMENT '绫诲瀷锛?鍗板埛 2骞垮憡 3璁捐?',
  `total_amount` decimal(15, 2) NOT NULL DEFAULT 0.00 COMMENT '璁㈠崟鎬婚?',
  `total_cost` decimal(15, 2) NULL DEFAULT 0.00 COMMENT '总成本',
  `designer_commission` decimal(15, 2) NULL DEFAULT 0.00 COMMENT '设计师提成（暂不启用）',
  `paid_amount` decimal(15, 2) NULL DEFAULT 0.00 COMMENT '宸蹭粯閲戦?',
  `discount_amount` decimal(15, 2) NULL DEFAULT 0.00 COMMENT '浼樻儬閲戦?',
  `rounding_amount` decimal(15, 2) NULL DEFAULT 0.00 COMMENT '鎶归浂閲戦?',
  `status` tinyint NULL DEFAULT 1 COMMENT '鐘舵?锛?寰呯‘璁?2杩涜?涓?3宸插畬鎴?4宸插彇娑',
  `payment_status` tinyint NULL DEFAULT 1 COMMENT '鏀?粯锛?鏈?粯 2閮ㄥ垎浠?3宸蹭粯娓?4宸叉姽闆剁粨娓',
  `contact_person` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '鑱旂郴浜',
  `contact_phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '鑱旂郴鐢佃瘽',
  `delivery_address` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '浜や粯鍦板潃',
  `delivery_date` date NULL DEFAULT NULL COMMENT '浜や粯鏃ユ湡',
  `designer_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '璁捐?甯',
  `designer_id` bigint NULL DEFAULT NULL COMMENT '璁捐?甯堢敤鎴稩D锛堝叧鑱攕ys_user锛',
  `priority` tinyint NULL DEFAULT 1 COMMENT '浼樺厛绾э細1鏅?? 2绱ф? 3鍔犳?',
  `source` tinyint NULL DEFAULT 1 COMMENT '鏉ユ簮锛?闂ㄥ簵鍒涘缓 2璁捐?骞垮満',
  `quote_amount` decimal(15, 2) NULL DEFAULT 0.00 COMMENT '鎶ヤ环閲戦?',
  `deposit_amount` decimal(15, 2) NULL DEFAULT 0.00 COMMENT '瀹氶噾閲戦?',
  `pay_method` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '浠樻?鏂瑰紡锛氬叏娆?棰勪粯50%/棰勪粯30%/璐у埌浠樻?/浼氬憳浣欓?鎶垫墸',
  `invoice_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '寮?エ绫诲瀷锛氭棤闇?紑绁?澧炲?绋庢櫘閫氬彂绁?澧炲?绋庝笓鐢ㄥ彂绁',
  `remark` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '澶囨敞',
  `creator_id` bigint NULL DEFAULT NULL,
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_order_no`(((case when (`deleted` = 0) then `order_no` else NULL end)) ASC) USING BTREE,
  INDEX `idx_customer_id`(`customer_id` ASC) USING BTREE,
  INDEX `idx_customer_name`(`customer_name` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '璁㈠崟涓昏〃' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of ord_order
-- ----------------------------
INSERT INTO `ord_order` VALUES (1, 'DD20260430001', 3, '水泵', NULL, '跑马灯灯箱', NULL, 2, 220.00, 120.00, 0.00, 220.00, 0.00, 0.00, 2, 3, '', '', '', '2026-05-02', '操作员小李', 3, 1, 1, 0.00, 0.00, NULL, NULL, '量尺尺寸：跑马灯灯箱，60*40cm', 1, '2026-04-30 12:25:31', '2026-04-30 12:39:21', 0);
INSERT INTO `ord_order` VALUES (2, 'DD20260430002', 4, '李四（服装店）', NULL, '服装高清车贴，过膜', NULL, 2, 74.95, 0.00, 0.00, 0.00, 0.00, 0.00, 4, 1, '', '13900002222', '', NULL, '系统管理员', 1, 1, 1, 0.00, 0.00, NULL, NULL, '量尺尺寸：橱窗贴：宽60cm×高90cm×3张\n客户需求：高清车贴，过膜，3天内制作完成', 1, '2026-04-30 13:17:23', '2026-04-30 13:33:19', 0);
INSERT INTO `ord_order` VALUES (3, 'DD20260430003', 1, '零售客户', NULL, '草编', NULL, 2, 705.00, 0.00, 0.00, 0.00, 0.00, 0.00, 4, 1, NULL, NULL, NULL, NULL, '操作员小李', 3, 1, 2, 0.00, 0.00, NULL, NULL, '[报价转单 QT20260430256]', NULL, '2026-04-30 14:04:45', '2026-04-30 14:05:24', 0);
INSERT INTO `ord_order` VALUES (4, 'DD20260430004', 5, '吴洋威', NULL, '广告制作', NULL, 2, 90.00, 0.00, 0.00, 0.00, 0.00, 0.00, 3, 3, '', '', '', '2026-04-30', '操作员小李', 3, 1, 1, 0.00, 0.00, NULL, NULL, '', 1, '2026-04-30 14:12:14', '2026-04-30 14:12:29', 0);

-- ----------------------------
-- Table structure for ord_order_material
-- ----------------------------
DROP TABLE IF EXISTS `ord_order_material`;
CREATE TABLE `ord_order_material`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `order_id` bigint NOT NULL,
  `material_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '鐗╂枡鍚嶇О',
  `spec` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '瑙勬牸',
  `unit` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '鍗曚綅',
  `quantity` decimal(10, 2) NULL DEFAULT 1.00 COMMENT '鏁伴噺',
  `unit_price` decimal(15, 2) NULL DEFAULT 0.00 COMMENT '鍗曚环',
  `amount` decimal(15, 2) NULL DEFAULT 0.00 COMMENT '灏忚?閲戦?',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '澶囨敞',
  `unit_cost` decimal(15, 2) NULL DEFAULT NULL COMMENT '单位成本（管理员填写）',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_order_id`(`order_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 6 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '璁㈠崟鐗╂枡鏄庣粏琛' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of ord_order_material
-- ----------------------------
INSERT INTO `ord_order_material` VALUES (1, 1, '跑马灯灯箱', '60*40cm', '个', 1.00, 220.00, 220.00, NULL, 120.00, '2026-04-30 12:25:31', 0);
INSERT INTO `ord_order_material` VALUES (2, 2, 'KT板 60x90cm', '60x90cm 5mm', '张', 5.00, 14.99, 74.95, NULL, NULL, '2026-04-30 13:17:23', 0);
INSERT INTO `ord_order_material` VALUES (3, 3, 'KT板 60x90cm', '60x90cm 5mm', '张', 7.00, 15.00, 105.00, NULL, NULL, '2026-04-30 14:04:45', 0);
INSERT INTO `ord_order_material` VALUES (4, 3, '写真布 1m宽', '1m宽 灯箱布', '米', 50.00, 12.00, 600.00, NULL, NULL, '2026-04-30 14:04:45', 0);
INSERT INTO `ord_order_material` VALUES (5, 4, 'KT板 60x90cm', '60x90cm 5mm', '张', 6.00, 15.00, 90.00, NULL, NULL, '2026-04-30 14:12:14', 0);

-- ----------------------------
-- Table structure for sq_application
-- ----------------------------
DROP TABLE IF EXISTS `sq_application`;
CREATE TABLE `sq_application`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `requirement_id` bigint NOT NULL COMMENT '闇?眰ID',
  `designer_id` bigint NULL DEFAULT NULL COMMENT '璁捐?甯堢敤鎴稩D',
  `designer_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '璁捐?甯堝?鍚',
  `proposal` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '鐢宠?璇存槑/鎻愭?',
  `quoted_price` decimal(15, 2) NULL DEFAULT NULL COMMENT '鎶ヤ环',
  `status` tinyint NULL DEFAULT 1 COMMENT '鐘舵?锛?寰呭?鏍?2宸叉帴鍙?3宸叉嫆缁',
  `reject_reason` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NULL DEFAULT NULL,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_requirement`(`requirement_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '璁捐?甯堢敵璇疯〃' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sq_application
-- ----------------------------

-- ----------------------------
-- Table structure for sq_income
-- ----------------------------
DROP TABLE IF EXISTS `sq_income`;
CREATE TABLE `sq_income`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `requirement_id` bigint NULL DEFAULT NULL COMMENT '闇?眰鍗旾D',
  `title` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '需求标题',
  `quoted_price` decimal(15, 2) NULL DEFAULT NULL COMMENT '承接价格',
  `platform_fee` decimal(15, 2) NULL DEFAULT NULL COMMENT '平台手续费',
  `actual_income` decimal(15, 2) NULL DEFAULT NULL COMMENT '实际收入',
  `designer_id` bigint NULL DEFAULT NULL,
  `designer_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `amount` decimal(15, 2) NULL DEFAULT NULL,
  `status` tinyint NULL DEFAULT 1 COMMENT '鐘舵?锛?寰呮墦娆?2宸叉墦娆',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '璁捐?甯堟敹鍏ヨ?褰' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sq_income
-- ----------------------------

-- ----------------------------
-- Table structure for sq_requirement
-- ----------------------------
DROP TABLE IF EXISTS `sq_requirement`;
CREATE TABLE `sq_requirement`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `req_no` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '闇?眰鍗曞彿',
  `title` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '闇?眰鏍囬?',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '闇?眰鎻忚堪',
  `category` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '绫诲埆锛歭ogo/banner/poster/card/vi',
  `budget` decimal(15, 2) NULL DEFAULT NULL COMMENT '棰勭畻',
  `budget_desc` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '预算说明',
  `deadline` date NULL DEFAULT NULL COMMENT '鎴??鏃ユ湡',
  `attachment` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '附件JSON',
  `customer_id` bigint NULL DEFAULT NULL COMMENT '关联客户ID',
  `customer_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '关联客户名称',
  `view_count` int NULL DEFAULT 0 COMMENT '浏览次数',
  `apply_count` int NULL DEFAULT 0 COMMENT '申请次数',
  `designer_id` bigint NULL DEFAULT NULL COMMENT '承接设计师ID',
  `designer_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '承接设计师名称',
  `status` tinyint NULL DEFAULT 1 COMMENT '鐘舵?锛?鎷涘嫙涓?2宸查?瀹?3杩涜?涓?4宸插畬鎴?5宸插彇娑',
  `publisher_id` bigint NULL DEFAULT NULL COMMENT '鍙戝竷浜篒D',
  `publisher_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `selected_designer_id` bigint NULL DEFAULT NULL,
  `selected_designer_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `final_price` decimal(15, 2) NULL DEFAULT NULL,
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NULL DEFAULT NULL,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_req_no`(((case when (`deleted` = 0) then `req_no` else NULL end)) ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '璁捐?骞垮満闇?眰鍗' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sq_requirement
-- ----------------------------

-- ----------------------------
-- Table structure for sys_backup
-- ----------------------------
DROP TABLE IF EXISTS `sys_backup`;
CREATE TABLE `sys_backup`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `file_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '澶囦唤鏂囦欢鍚',
  `file_path` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '鏂囦欢瀛樺偍璺?緞',
  `file_size` bigint NULL DEFAULT 0 COMMENT '鏂囦欢澶у皬锛堝瓧鑺傦級',
  `type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'manual' COMMENT '绫诲瀷锛歛uto/manual',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'success' COMMENT '鐘舵?锛歴uccess/failed',
  `remark` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '澶囨敞',
  `operator_id` bigint NULL DEFAULT NULL COMMENT '鎿嶄綔浜篒D',
  `operator_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '鎿嶄綔浜哄悕绉',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_time`(`create_time` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '鏁版嵁澶囦唤璁板綍琛' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_backup
-- ----------------------------

-- ----------------------------
-- Table structure for sys_button
-- ----------------------------
DROP TABLE IF EXISTS `sys_button`;
CREATE TABLE `sys_button`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '鎸夐挳鍚嶇О',
  `permission` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '鏉冮檺鏍囪瘑锛屽? system:user:add',
  `type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'button' COMMENT '绫诲瀷锛歜utton/menu/api',
  `parent_id` bigint NULL DEFAULT 0 COMMENT '涓婄骇鑿滃崟ID',
  `sort` int NULL DEFAULT 0 COMMENT '鎺掑簭',
  `status` tinyint NULL DEFAULT 1 COMMENT '1鍚?敤 0鍋滅敤',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_parent`(`parent_id` ASC) USING BTREE,
  INDEX `idx_permission`(`permission` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 32 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '鎸夐挳鏉冮檺璧勬簮琛' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_button
-- ----------------------------
INSERT INTO `sys_button` VALUES (1, '查看仪表盘', 'dashboard:view', 'button', 0, 1, 1, '2026-04-28 15:30:01', '2026-04-28 15:30:01', 0);
INSERT INTO `sys_button` VALUES (2, '查看订单', 'order:list', 'button', 0, 2, 1, '2026-04-28 15:30:01', '2026-04-28 15:30:01', 0);
INSERT INTO `sys_button` VALUES (3, '新建订单', 'order:create', 'button', 0, 3, 1, '2026-04-28 15:30:01', '2026-04-28 15:30:01', 0);
INSERT INTO `sys_button` VALUES (4, '编辑订单', 'order:edit', 'button', 0, 4, 1, '2026-04-28 15:30:01', '2026-04-28 15:30:01', 0);
INSERT INTO `sys_button` VALUES (5, '删除订单', 'order:delete', 'button', 0, 5, 1, '2026-04-28 15:30:01', '2026-04-28 15:30:01', 0);
INSERT INTO `sys_button` VALUES (6, '查看客户', 'customer:list', 'button', 0, 6, 1, '2026-04-28 15:30:01', '2026-04-28 15:30:01', 0);
INSERT INTO `sys_button` VALUES (7, '新建客户', 'customer:create', 'button', 0, 7, 1, '2026-04-28 15:30:01', '2026-04-28 15:30:01', 0);
INSERT INTO `sys_button` VALUES (8, '编辑客户', 'customer:edit', 'button', 0, 8, 1, '2026-04-28 15:30:01', '2026-04-28 15:30:01', 0);
INSERT INTO `sys_button` VALUES (9, '删除客户', 'customer:delete', 'button', 0, 9, 1, '2026-04-28 15:30:01', '2026-04-28 15:30:01', 0);
INSERT INTO `sys_button` VALUES (10, '查看会员', 'member:list', 'button', 0, 10, 1, '2026-04-28 15:30:01', '2026-04-28 15:30:01', 0);
INSERT INTO `sys_button` VALUES (11, '新建会员', 'member:create', 'button', 0, 11, 1, '2026-04-28 15:30:01', '2026-04-28 15:30:01', 0);
INSERT INTO `sys_button` VALUES (12, '编辑会员', 'member:edit', 'button', 0, 12, 1, '2026-04-28 15:30:01', '2026-04-28 15:30:01', 0);
INSERT INTO `sys_button` VALUES (13, '删除会员', 'member:delete', 'button', 0, 13, 1, '2026-04-28 15:30:01', '2026-04-28 15:30:01', 0);
INSERT INTO `sys_button` VALUES (14, '会员充值', 'member:recharge', 'button', 0, 14, 1, '2026-04-28 15:30:01', '2026-04-28 15:30:01', 0);
INSERT INTO `sys_button` VALUES (15, '查看账单', 'factory:list', 'button', 0, 15, 1, '2026-04-28 15:30:01', '2026-04-28 15:30:01', 0);
INSERT INTO `sys_button` VALUES (16, '新建账单', 'factory:create', 'button', 0, 16, 1, '2026-04-28 15:30:01', '2026-04-28 15:30:01', 0);
INSERT INTO `sys_button` VALUES (17, '编辑账单', 'factory:edit', 'button', 0, 17, 1, '2026-04-28 15:30:01', '2026-04-28 15:30:01', 0);
INSERT INTO `sys_button` VALUES (18, '删除账单', 'factory:delete', 'button', 0, 18, 1, '2026-04-28 15:30:01', '2026-04-28 15:30:01', 0);
INSERT INTO `sys_button` VALUES (19, '查看财务', 'finance:view', 'button', 0, 19, 1, '2026-04-28 15:30:01', '2026-04-28 15:30:01', 0);
INSERT INTO `sys_button` VALUES (20, '编辑财务', 'finance:edit', 'button', 0, 20, 1, '2026-04-28 15:30:01', '2026-04-28 15:30:01', 0);
INSERT INTO `sys_button` VALUES (21, '用户管理', 'system:user', 'button', 0, 21, 1, '2026-04-28 15:30:01', '2026-04-28 15:30:01', 0);
INSERT INTO `sys_button` VALUES (22, '角色权限', 'system:role', 'button', 0, 22, 1, '2026-04-28 15:30:01', '2026-04-28 15:30:01', 0);
INSERT INTO `sys_button` VALUES (23, '操作日志', 'system:log', 'button', 0, 23, 1, '2026-04-28 15:30:01', '2026-04-28 15:30:01', 0);
INSERT INTO `sys_button` VALUES (24, '数据字典', 'system:dict', 'button', 0, 24, 1, '2026-04-28 15:30:01', '2026-04-28 15:30:01', 0);
INSERT INTO `sys_button` VALUES (25, '数据备份', 'system:backup', 'button', 0, 25, 1, '2026-04-28 15:30:01', '2026-04-28 15:30:01', 0);
INSERT INTO `sys_button` VALUES (26, '公告管理', 'system:notice', 'button', 0, 26, 1, '2026-04-28 15:30:01', '2026-04-28 15:30:01', 0);
INSERT INTO `sys_button` VALUES (27, '按钮管理', 'system:menu', 'button', 0, 27, 1, '2026-04-28 15:30:01', '2026-04-28 15:30:01', 0);
INSERT INTO `sys_button` VALUES (28, '查看物料', 'material:view', 'button', 0, 28, 1, '2026-04-28 15:30:01', '2026-04-28 15:30:01', 0);
INSERT INTO `sys_button` VALUES (29, '查看文件', 'design:file', 'button', 0, 29, 1, '2026-04-28 15:30:01', '2026-04-28 15:30:01', 0);
INSERT INTO `sys_button` VALUES (30, '查看报表', 'statistics:view', 'button', 0, 30, 1, '2026-04-28 15:30:01', '2026-04-28 15:30:01', 0);
INSERT INTO `sys_button` VALUES (31, '查看广场', 'square:manage', 'button', 0, 31, 1, '2026-04-28 15:30:01', '2026-04-28 15:30:01', 0);

-- ----------------------------
-- Table structure for sys_company
-- ----------------------------
DROP TABLE IF EXISTS `sys_company`;
CREATE TABLE `sys_company`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `company_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '鍏?徃鍚嶇О',
  `address` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '鍦板潃',
  `phone` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '鐢佃瘽',
  `fax` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '浼犵湡',
  `email` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '閭??',
  `contact_person` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '联系人',
  `company_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '公司类型：headquarters/branch',
  `bank_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '寮?埛閾惰?',
  `bank_account` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '閾惰?璐﹀彿',
  `tax_no` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '绋庡彿',
  `logo_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'Logo URL',
  `is_default` tinyint NULL DEFAULT 0 COMMENT '鏄?惁榛樿?鍏?徃(1=鏄?',
  `status` tinyint NULL DEFAULT 1 COMMENT '鐘舵?:0=绂佺敤,1=鍚?敤',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '鍏?徃淇℃伅琛' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_company
-- ----------------------------
INSERT INTO `sys_company` VALUES (1, '台州新原力广告传媒有限公司', '浙江省台州市椒江区台州湾新区', '15084494408', '021-88888889', '2991404309@qq.com', NULL, NULL, '中国建设银行股份有限公司台州湾新区支行', '33050111438300000786', '91331001MAK6XBJU5Q', '', 1, 1, '2026-04-28 15:32:04', '2026-04-28 15:41:21', 0);

-- ----------------------------
-- Table structure for sys_dict
-- ----------------------------
DROP TABLE IF EXISTS `sys_dict`;
CREATE TABLE `sys_dict`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '瀛楀吀鍚嶇О',
  `code` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '瀛楀吀缂栫爜',
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '鎻忚堪',
  `sort_order` int NULL DEFAULT 0,
  `status` tinyint NULL DEFAULT 1,
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NULL DEFAULT NULL,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_dict_code`(((case when (`deleted` = 0) then `code` else NULL end)) ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '鏁版嵁瀛楀吀琛' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_dict
-- ----------------------------
INSERT INTO `sys_dict` VALUES (1, '订单类型', 'order_type', '订单类型枚举', 1, 1, '2026-04-28 15:30:01', NULL, 0);
INSERT INTO `sys_dict` VALUES (2, '订单状态', 'order_status', '订单状态枚举', 2, 1, '2026-04-28 15:30:01', NULL, 0);
INSERT INTO `sys_dict` VALUES (3, '支付方式', 'payment_method', '支付方式枚举', 3, 1, '2026-04-28 15:30:01', NULL, 0);
INSERT INTO `sys_dict` VALUES (4, '客户等级', 'customer_level', '客户等级枚举', 4, 1, '2026-04-28 15:30:01', NULL, 0);

-- ----------------------------
-- Table structure for sys_dict_item
-- ----------------------------
DROP TABLE IF EXISTS `sys_dict_item`;
CREATE TABLE `sys_dict_item`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `dict_id` bigint NOT NULL COMMENT '瀛楀吀ID',
  `label` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '鏍囩?',
  `value` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '鍊',
  `sort_order` int NULL DEFAULT 0,
  `status` tinyint NULL DEFAULT 1,
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_dict`(`dict_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 16 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '鏁版嵁瀛楀吀椤' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_dict_item
-- ----------------------------
INSERT INTO `sys_dict_item` VALUES (1, 1, '印刷', '1', 1, 1, NULL, '2026-04-28 15:30:01', 0);
INSERT INTO `sys_dict_item` VALUES (2, 1, '广告', '2', 2, 1, NULL, '2026-04-28 15:30:01', 0);
INSERT INTO `sys_dict_item` VALUES (3, 1, '设计', '3', 3, 1, NULL, '2026-04-28 15:30:01', 0);
INSERT INTO `sys_dict_item` VALUES (4, 2, '待确认', '1', 1, 1, NULL, '2026-04-28 15:30:01', 0);
INSERT INTO `sys_dict_item` VALUES (5, 2, '进行中', '2', 2, 1, NULL, '2026-04-28 15:30:01', 0);
INSERT INTO `sys_dict_item` VALUES (6, 2, '已完成', '3', 3, 1, NULL, '2026-04-28 15:30:01', 0);
INSERT INTO `sys_dict_item` VALUES (7, 2, '已取消', '4', 4, 1, NULL, '2026-04-28 15:30:01', 0);
INSERT INTO `sys_dict_item` VALUES (8, 3, '现金', 'cash', 1, 1, NULL, '2026-04-28 15:30:01', 0);
INSERT INTO `sys_dict_item` VALUES (9, 3, '转账', 'transfer', 2, 1, NULL, '2026-04-28 15:30:01', 0);
INSERT INTO `sys_dict_item` VALUES (10, 3, '微信', 'wechat', 3, 1, NULL, '2026-04-28 15:30:01', 0);
INSERT INTO `sys_dict_item` VALUES (11, 3, '支付宝', 'alipay', 4, 1, NULL, '2026-04-28 15:30:01', 0);
INSERT INTO `sys_dict_item` VALUES (12, 4, '普通', '1', 1, 1, NULL, '2026-04-28 15:30:01', 0);
INSERT INTO `sys_dict_item` VALUES (13, 4, '银牌', '2', 2, 1, NULL, '2026-04-28 15:30:01', 0);
INSERT INTO `sys_dict_item` VALUES (14, 4, '金牌', '3', 3, 1, NULL, '2026-04-28 15:30:01', 0);
INSERT INTO `sys_dict_item` VALUES (15, 4, '钻石', '4', 4, 1, NULL, '2026-04-28 15:30:01', 0);

-- ----------------------------
-- Table structure for sys_notice
-- ----------------------------
DROP TABLE IF EXISTS `sys_notice`;
CREATE TABLE `sys_notice`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `title` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '閫氱煡鏍囬?',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '閫氱煡鍐呭?',
  `type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '绫诲瀷锛歴ystem/order/finance/warning',
  `level` tinyint NULL DEFAULT 1 COMMENT '绾у埆锛?鏅?? 2閲嶈? 3绱ф?',
  `user_id` bigint NULL DEFAULT NULL COMMENT '鎺ユ敹鐢ㄦ埛ID锛坣ull琛ㄧず鍏ㄩ儴锛',
  `user_ids` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '鎸囧畾澶氫釜鐢ㄦ埛ID閫楀彿鍒嗛殧',
  `is_read` tinyint NOT NULL DEFAULT 0 COMMENT '鏄?惁宸茶?锛?鏈?? 1宸茶?',
  `read_user_id` bigint NULL DEFAULT NULL COMMENT '璇诲彇鐢ㄦ埛ID',
  `read_time` datetime NULL DEFAULT NULL COMMENT '璇诲彇鏃堕棿',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_is_read`(`is_read` ASC) USING BTREE,
  INDEX `idx_type`(`type` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '绯荤粺閫氱煡琛' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_notice
-- ----------------------------

-- ----------------------------
-- Table structure for sys_notice_setting
-- ----------------------------
DROP TABLE IF EXISTS `sys_notice_setting`;
CREATE TABLE `sys_notice_setting`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT '鐢ㄦ埛ID',
  `order_notify` tinyint NULL DEFAULT 1 COMMENT '璁㈠崟閫氱煡锛?寮?惎 0鍏抽棴',
  `finance_notify` tinyint NULL DEFAULT 1 COMMENT '璐㈠姟閫氱煡',
  `system_notify` tinyint NULL DEFAULT 1 COMMENT '绯荤粺閫氱煡',
  `warning_notify` tinyint NULL DEFAULT 1 COMMENT '棰勮?閫氱煡',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NULL DEFAULT NULL,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_notice_setting_user`(((case when (`deleted` = 0) then `user_id` else NULL end)) ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '鐢ㄦ埛閫氱煡璁剧疆琛' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_notice_setting
-- ----------------------------

-- ----------------------------
-- Table structure for sys_operation_log
-- ----------------------------
DROP TABLE IF EXISTS `sys_operation_log`;
CREATE TABLE `sys_operation_log`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NULL DEFAULT NULL COMMENT '鎿嶄綔鐢ㄦ埛ID',
  `username` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '鎿嶄綔鐢ㄦ埛鍚',
  `module` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '鎿嶄綔妯″潡',
  `action` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '鎿嶄綔绫诲瀷',
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '鎿嶄綔鎻忚堪',
  `method` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '璇锋眰鏂规硶',
  `url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '璇锋眰URL',
  `ip` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '鎿嶄綔IP',
  `user_agent` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'UserAgent',
  `param` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '璇锋眰鍙傛暟',
  `result` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '鍝嶅簲缁撴灉锛堟憳瑕侊級',
  `status` tinyint NULL DEFAULT 1 COMMENT '1鎴愬姛 0澶辫触',
  `duration` bigint NULL DEFAULT NULL COMMENT '鑰楁椂锛堟?绉掞級',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user`(`user_id` ASC) USING BTREE,
  INDEX `idx_time`(`create_time` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '鎿嶄綔鏃ュ織琛' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_operation_log
-- ----------------------------

-- ----------------------------
-- Table structure for sys_permission
-- ----------------------------
DROP TABLE IF EXISTS `sys_permission`;
CREATE TABLE `sys_permission`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '鏉冮檺ID',
  `parent_id` bigint NOT NULL DEFAULT 0 COMMENT '鐖禝D锛?涓烘牴锛',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '鏉冮檺鍚嶇О',
  `type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'menu' COMMENT '绫诲瀷锛歮enu/button/interface',
  `path` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '璺?敱璺?緞',
  `component` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '鍓嶇?缁勪欢璺?緞',
  `icon` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '鍥炬爣',
  `permission_code` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '鏉冮檺鐮侊紙濡?order:create锛',
  `sort` int NULL DEFAULT 0 COMMENT '鎺掑簭',
  `visible` tinyint NULL DEFAULT 1 COMMENT '鏄?惁鏄剧ず锛?闅愯棌 1鏄剧ず',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '鐘舵?锛?绂佺敤 1姝ｅ父',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_parent_id`(`parent_id` ASC) USING BTREE,
  INDEX `idx_permission_code`(`permission_code` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 44 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '绯荤粺鏉冮檺琛' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_permission
-- ----------------------------
INSERT INTO `sys_permission` VALUES (1, 0, '仪表盘', 'menu', '/dashboard', 'dashboard/index', '📊', NULL, 1, 1, 1, '2026-04-28 15:30:01', '2026-04-28 15:30:01', 0);
INSERT INTO `sys_permission` VALUES (2, 0, '订单管理', 'menu', '/orders', 'order/index', '📋', NULL, 2, 1, 1, '2026-04-28 15:30:01', '2026-04-28 15:30:01', 0);
INSERT INTO `sys_permission` VALUES (3, 0, '客户管理', 'menu', '/customers', 'customer/index', '👥', NULL, 3, 1, 1, '2026-04-28 15:30:01', '2026-04-28 15:30:01', 0);
INSERT INTO `sys_permission` VALUES (4, 0, '会员管理', 'menu', '/members', 'member/index', '🎫', NULL, 4, 1, 1, '2026-04-28 15:30:01', '2026-04-28 15:30:01', 0);
INSERT INTO `sys_permission` VALUES (5, 0, '工厂账单', 'menu', '/factory', 'factory/index', '🏭', NULL, 5, 1, 1, '2026-04-28 15:30:01', '2026-04-28 15:30:01', 0);
INSERT INTO `sys_permission` VALUES (6, 0, '财务管理', 'menu', '/finance', 'finance/index', '💰', NULL, 6, 1, 1, '2026-04-28 15:30:01', '2026-04-28 15:30:01', 0);
INSERT INTO `sys_permission` VALUES (7, 0, '系统管理', 'menu', '/system', 'system/index', '⚙️', NULL, 7, 1, 1, '2026-04-28 15:30:01', '2026-04-28 15:30:01', 0);
INSERT INTO `sys_permission` VALUES (8, 0, '物料管理', 'menu', '/material', 'material/index', '📦', NULL, 8, 1, 1, '2026-04-28 15:30:01', '2026-04-28 15:30:01', 0);
INSERT INTO `sys_permission` VALUES (9, 0, '设计文件', 'menu', '/design', 'design/file', '🎨', NULL, 9, 1, 1, '2026-04-28 15:30:01', '2026-04-28 15:30:01', 0);
INSERT INTO `sys_permission` VALUES (10, 0, '统计报表', 'menu', '/statistics', 'statistics/index', '📈', NULL, 10, 1, 1, '2026-04-28 15:30:01', '2026-04-28 15:30:01', 0);
INSERT INTO `sys_permission` VALUES (11, 0, '广场管理', 'menu', '/square', 'square/index', '✏️', NULL, 11, 1, 1, '2026-04-28 15:30:01', '2026-04-28 15:30:01', 0);
INSERT INTO `sys_permission` VALUES (12, 1, '查看仪表盘', 'button', NULL, NULL, NULL, 'dashboard:view', 1, 1, 1, '2026-04-28 15:30:01', '2026-04-28 15:30:01', 0);
INSERT INTO `sys_permission` VALUES (13, 2, '查看订单', 'button', NULL, NULL, NULL, 'order:list', 1, 1, 1, '2026-04-28 15:30:01', '2026-04-28 15:30:01', 0);
INSERT INTO `sys_permission` VALUES (14, 2, '新建订单', 'button', NULL, NULL, NULL, 'order:create', 2, 1, 1, '2026-04-28 15:30:01', '2026-04-28 15:30:01', 0);
INSERT INTO `sys_permission` VALUES (15, 2, '编辑订单', 'button', NULL, NULL, NULL, 'order:edit', 3, 1, 1, '2026-04-28 15:30:01', '2026-04-28 15:30:01', 0);
INSERT INTO `sys_permission` VALUES (16, 2, '删除订单', 'button', NULL, NULL, NULL, 'order:delete', 4, 1, 1, '2026-04-28 15:30:01', '2026-04-28 15:30:01', 0);
INSERT INTO `sys_permission` VALUES (17, 3, '查看客户', 'button', NULL, NULL, NULL, 'customer:list', 1, 1, 1, '2026-04-28 15:30:01', '2026-04-28 15:30:01', 0);
INSERT INTO `sys_permission` VALUES (18, 3, '新建客户', 'button', NULL, NULL, NULL, 'customer:create', 2, 1, 1, '2026-04-28 15:30:01', '2026-04-28 15:30:01', 0);
INSERT INTO `sys_permission` VALUES (19, 3, '编辑客户', 'button', NULL, NULL, NULL, 'customer:edit', 3, 1, 1, '2026-04-28 15:30:01', '2026-04-28 15:30:01', 0);
INSERT INTO `sys_permission` VALUES (20, 3, '删除客户', 'button', NULL, NULL, NULL, 'customer:delete', 4, 1, 1, '2026-04-28 15:30:01', '2026-04-28 15:30:01', 0);
INSERT INTO `sys_permission` VALUES (21, 4, '查看会员', 'button', NULL, NULL, NULL, 'member:list', 1, 1, 1, '2026-04-28 15:30:01', '2026-04-28 15:30:01', 0);
INSERT INTO `sys_permission` VALUES (22, 4, '新建会员', 'button', NULL, NULL, NULL, 'member:create', 2, 1, 1, '2026-04-28 15:30:01', '2026-04-28 15:30:01', 0);
INSERT INTO `sys_permission` VALUES (23, 4, '编辑会员', 'button', NULL, NULL, NULL, 'member:edit', 3, 1, 1, '2026-04-28 15:30:01', '2026-04-28 15:30:01', 0);
INSERT INTO `sys_permission` VALUES (24, 4, '删除会员', 'button', NULL, NULL, NULL, 'member:delete', 4, 1, 1, '2026-04-28 15:30:01', '2026-04-28 15:30:01', 0);
INSERT INTO `sys_permission` VALUES (25, 4, '会员充值', 'button', NULL, NULL, NULL, 'member:recharge', 5, 1, 1, '2026-04-28 15:30:01', '2026-04-28 15:30:01', 0);
INSERT INTO `sys_permission` VALUES (26, 5, '查看账单', 'button', NULL, NULL, NULL, 'factory:list', 1, 1, 1, '2026-04-28 15:30:01', '2026-04-28 15:30:01', 0);
INSERT INTO `sys_permission` VALUES (27, 5, '新建账单', 'button', NULL, NULL, NULL, 'factory:create', 2, 1, 1, '2026-04-28 15:30:01', '2026-04-28 15:30:01', 0);
INSERT INTO `sys_permission` VALUES (28, 5, '编辑账单', 'button', NULL, NULL, NULL, 'factory:edit', 3, 1, 1, '2026-04-28 15:30:01', '2026-04-28 15:30:01', 0);
INSERT INTO `sys_permission` VALUES (29, 5, '删除账单', 'button', NULL, NULL, NULL, 'factory:delete', 4, 1, 1, '2026-04-28 15:30:01', '2026-04-28 15:30:01', 0);
INSERT INTO `sys_permission` VALUES (30, 6, '查看财务', 'button', NULL, NULL, NULL, 'finance:view', 1, 1, 1, '2026-04-28 15:30:01', '2026-04-28 15:30:01', 0);
INSERT INTO `sys_permission` VALUES (31, 6, '编辑财务', 'button', NULL, NULL, NULL, 'finance:edit', 2, 1, 1, '2026-04-28 15:30:01', '2026-04-28 15:30:01', 0);
INSERT INTO `sys_permission` VALUES (32, 7, '用户管理', 'button', NULL, NULL, NULL, 'system:user', 1, 1, 1, '2026-04-28 15:30:01', '2026-04-28 15:30:01', 0);
INSERT INTO `sys_permission` VALUES (33, 7, '角色权限', 'button', NULL, NULL, NULL, 'system:role', 2, 1, 1, '2026-04-28 15:30:01', '2026-04-28 15:30:01', 0);
INSERT INTO `sys_permission` VALUES (34, 7, '操作日志', 'button', NULL, NULL, NULL, 'system:log', 3, 1, 1, '2026-04-28 15:30:01', '2026-04-28 15:30:01', 0);
INSERT INTO `sys_permission` VALUES (35, 7, '数据字典', 'button', NULL, NULL, NULL, 'system:dict', 4, 1, 1, '2026-04-28 15:30:01', '2026-04-28 15:30:01', 0);
INSERT INTO `sys_permission` VALUES (36, 7, '数据备份', 'button', NULL, NULL, NULL, 'system:backup', 5, 1, 1, '2026-04-28 15:30:01', '2026-04-28 15:30:01', 0);
INSERT INTO `sys_permission` VALUES (37, 7, '公告管理', 'button', NULL, NULL, NULL, 'system:notice', 6, 1, 1, '2026-04-28 15:30:01', '2026-04-28 15:30:01', 0);
INSERT INTO `sys_permission` VALUES (38, 7, '按钮管理', 'button', NULL, NULL, NULL, 'system:menu', 7, 1, 1, '2026-04-28 15:30:01', '2026-04-28 15:30:01', 0);
INSERT INTO `sys_permission` VALUES (39, 8, '查看物料', 'button', NULL, NULL, NULL, 'material:view', 1, 1, 1, '2026-04-28 15:30:01', '2026-04-28 15:30:01', 0);
INSERT INTO `sys_permission` VALUES (40, 9, '查看文件', 'button', NULL, NULL, NULL, 'design:file', 1, 1, 1, '2026-04-28 15:30:01', '2026-04-28 15:30:01', 0);
INSERT INTO `sys_permission` VALUES (41, 10, '查看报表', 'button', NULL, NULL, NULL, 'statistics:view', 1, 1, 1, '2026-04-28 15:30:01', '2026-04-28 15:30:01', 0);
INSERT INTO `sys_permission` VALUES (42, 11, '查看广场', 'button', NULL, NULL, NULL, 'square:manage', 1, 1, 1, '2026-04-28 15:30:01', '2026-04-28 15:30:01', 0);
INSERT INTO `sys_permission` VALUES (43, 7, '公司管理', 'button', NULL, NULL, NULL, 'system:config', 28, 1, 1, '2026-04-28 15:32:37', '2026-04-28 15:32:37', 0);

-- ----------------------------
-- Table structure for sys_role
-- ----------------------------
DROP TABLE IF EXISTS `sys_role`;
CREATE TABLE `sys_role`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '瑙掕壊ID',
  `role_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '瑙掕壊鍚嶇О',
  `role_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '瑙掕壊缂栫爜',
  `description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '鎻忚堪',
  `sort` int NULL DEFAULT 0 COMMENT '鎺掑簭',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '鐘舵?锛?绂佺敤 1姝ｅ父',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_role_code`(((case when (`deleted` = 0) then `role_code` else NULL end)) ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 7 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '绯荤粺瑙掕壊琛' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_role
-- ----------------------------
INSERT INTO `sys_role` VALUES (1, '超级管理员', 'SUPER_ADMIN', '拥有所有权限', 1, 1, '2026-04-28 15:30:01', '2026-04-28 15:30:01', 0);
INSERT INTO `sys_role` VALUES (2, '管理员', 'ADMIN', '除系统配置外全部权限', 2, 1, '2026-04-28 15:30:01', '2026-04-28 15:30:01', 0);
INSERT INTO `sys_role` VALUES (3, '财务', 'FINANCE', '财务和订单查看', 3, 1, '2026-04-28 15:30:01', '2026-04-28 15:30:01', 0);
INSERT INTO `sys_role` VALUES (4, '操作员', 'OPERATOR', '订单增删改', 4, 1, '2026-04-28 15:30:01', '2026-04-28 15:30:01', 0);
INSERT INTO `sys_role` VALUES (5, '访客', 'VIEWER', '只读权限', 5, 1, '2026-04-28 15:30:01', '2026-04-28 15:30:01', 0);
INSERT INTO `sys_role` VALUES (6, '设计师', 'DESIGNER', '设计师，可接单和处理订单', 5, 1, '2026-04-28 19:57:29', '2026-04-28 21:17:06', 0);

-- ----------------------------
-- Table structure for sys_role_permission
-- ----------------------------
DROP TABLE IF EXISTS `sys_role_permission`;
CREATE TABLE `sys_role_permission`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `role_id` bigint NOT NULL,
  `permission_id` bigint NOT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `idx_role_permission`(`role_id` ASC, `permission_id` ASC) USING BTREE,
  INDEX `idx_role_id`(`role_id` ASC) USING BTREE,
  INDEX `idx_permission_id`(`permission_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 134 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '瑙掕壊鏉冮檺鍏宠仈琛' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_role_permission
-- ----------------------------
INSERT INTO `sys_role_permission` VALUES (1, 1, 1);
INSERT INTO `sys_role_permission` VALUES (2, 1, 2);
INSERT INTO `sys_role_permission` VALUES (3, 1, 3);
INSERT INTO `sys_role_permission` VALUES (4, 1, 4);
INSERT INTO `sys_role_permission` VALUES (5, 1, 5);
INSERT INTO `sys_role_permission` VALUES (6, 1, 6);
INSERT INTO `sys_role_permission` VALUES (7, 1, 7);
INSERT INTO `sys_role_permission` VALUES (8, 1, 8);
INSERT INTO `sys_role_permission` VALUES (9, 1, 9);
INSERT INTO `sys_role_permission` VALUES (10, 1, 10);
INSERT INTO `sys_role_permission` VALUES (11, 1, 11);
INSERT INTO `sys_role_permission` VALUES (12, 1, 12);
INSERT INTO `sys_role_permission` VALUES (13, 1, 13);
INSERT INTO `sys_role_permission` VALUES (14, 1, 14);
INSERT INTO `sys_role_permission` VALUES (15, 1, 15);
INSERT INTO `sys_role_permission` VALUES (16, 1, 16);
INSERT INTO `sys_role_permission` VALUES (17, 1, 17);
INSERT INTO `sys_role_permission` VALUES (18, 1, 18);
INSERT INTO `sys_role_permission` VALUES (19, 1, 19);
INSERT INTO `sys_role_permission` VALUES (20, 1, 20);
INSERT INTO `sys_role_permission` VALUES (21, 1, 21);
INSERT INTO `sys_role_permission` VALUES (22, 1, 22);
INSERT INTO `sys_role_permission` VALUES (23, 1, 23);
INSERT INTO `sys_role_permission` VALUES (24, 1, 24);
INSERT INTO `sys_role_permission` VALUES (25, 1, 25);
INSERT INTO `sys_role_permission` VALUES (26, 1, 26);
INSERT INTO `sys_role_permission` VALUES (27, 1, 27);
INSERT INTO `sys_role_permission` VALUES (28, 1, 28);
INSERT INTO `sys_role_permission` VALUES (29, 1, 29);
INSERT INTO `sys_role_permission` VALUES (30, 1, 30);
INSERT INTO `sys_role_permission` VALUES (31, 1, 31);
INSERT INTO `sys_role_permission` VALUES (32, 1, 32);
INSERT INTO `sys_role_permission` VALUES (33, 1, 33);
INSERT INTO `sys_role_permission` VALUES (34, 1, 34);
INSERT INTO `sys_role_permission` VALUES (35, 1, 35);
INSERT INTO `sys_role_permission` VALUES (36, 1, 36);
INSERT INTO `sys_role_permission` VALUES (37, 1, 37);
INSERT INTO `sys_role_permission` VALUES (38, 1, 38);
INSERT INTO `sys_role_permission` VALUES (39, 1, 39);
INSERT INTO `sys_role_permission` VALUES (40, 1, 40);
INSERT INTO `sys_role_permission` VALUES (41, 1, 41);
INSERT INTO `sys_role_permission` VALUES (42, 1, 42);
INSERT INTO `sys_role_permission` VALUES (117, 1, 43);
INSERT INTO `sys_role_permission` VALUES (64, 2, 12);
INSERT INTO `sys_role_permission` VALUES (65, 2, 13);
INSERT INTO `sys_role_permission` VALUES (66, 2, 14);
INSERT INTO `sys_role_permission` VALUES (67, 2, 15);
INSERT INTO `sys_role_permission` VALUES (68, 2, 16);
INSERT INTO `sys_role_permission` VALUES (69, 2, 17);
INSERT INTO `sys_role_permission` VALUES (70, 2, 18);
INSERT INTO `sys_role_permission` VALUES (71, 2, 19);
INSERT INTO `sys_role_permission` VALUES (72, 2, 20);
INSERT INTO `sys_role_permission` VALUES (73, 2, 21);
INSERT INTO `sys_role_permission` VALUES (74, 2, 22);
INSERT INTO `sys_role_permission` VALUES (75, 2, 23);
INSERT INTO `sys_role_permission` VALUES (76, 2, 24);
INSERT INTO `sys_role_permission` VALUES (77, 2, 25);
INSERT INTO `sys_role_permission` VALUES (78, 2, 26);
INSERT INTO `sys_role_permission` VALUES (79, 2, 27);
INSERT INTO `sys_role_permission` VALUES (80, 2, 28);
INSERT INTO `sys_role_permission` VALUES (81, 2, 29);
INSERT INTO `sys_role_permission` VALUES (82, 2, 30);
INSERT INTO `sys_role_permission` VALUES (83, 2, 31);
INSERT INTO `sys_role_permission` VALUES (84, 2, 32);
INSERT INTO `sys_role_permission` VALUES (85, 2, 33);
INSERT INTO `sys_role_permission` VALUES (86, 2, 34);
INSERT INTO `sys_role_permission` VALUES (87, 2, 35);
INSERT INTO `sys_role_permission` VALUES (88, 2, 37);
INSERT INTO `sys_role_permission` VALUES (89, 2, 38);
INSERT INTO `sys_role_permission` VALUES (90, 2, 39);
INSERT INTO `sys_role_permission` VALUES (91, 2, 40);
INSERT INTO `sys_role_permission` VALUES (92, 2, 41);
INSERT INTO `sys_role_permission` VALUES (93, 2, 42);
INSERT INTO `sys_role_permission` VALUES (118, 2, 43);
INSERT INTO `sys_role_permission` VALUES (96, 3, 12);
INSERT INTO `sys_role_permission` VALUES (99, 3, 13);
INSERT INTO `sys_role_permission` VALUES (95, 3, 17);
INSERT INTO `sys_role_permission` VALUES (98, 3, 30);
INSERT INTO `sys_role_permission` VALUES (97, 3, 31);
INSERT INTO `sys_role_permission` VALUES (106, 4, 12);
INSERT INTO `sys_role_permission` VALUES (110, 4, 13);
INSERT INTO `sys_role_permission` VALUES (107, 4, 14);
INSERT INTO `sys_role_permission` VALUES (109, 4, 15);
INSERT INTO `sys_role_permission` VALUES (108, 4, 16);
INSERT INTO `sys_role_permission` VALUES (105, 4, 17);
INSERT INTO `sys_role_permission` VALUES (102, 4, 18);
INSERT INTO `sys_role_permission` VALUES (104, 4, 19);
INSERT INTO `sys_role_permission` VALUES (103, 4, 20);
INSERT INTO `sys_role_permission` VALUES (119, 6, 12);
INSERT INTO `sys_role_permission` VALUES (120, 6, 13);
INSERT INTO `sys_role_permission` VALUES (121, 6, 14);
INSERT INTO `sys_role_permission` VALUES (122, 6, 15);
INSERT INTO `sys_role_permission` VALUES (123, 6, 17);
INSERT INTO `sys_role_permission` VALUES (124, 6, 18);
INSERT INTO `sys_role_permission` VALUES (125, 6, 19);
INSERT INTO `sys_role_permission` VALUES (126, 6, 21);
INSERT INTO `sys_role_permission` VALUES (127, 6, 22);
INSERT INTO `sys_role_permission` VALUES (128, 6, 26);
INSERT INTO `sys_role_permission` VALUES (129, 6, 27);
INSERT INTO `sys_role_permission` VALUES (130, 6, 28);
INSERT INTO `sys_role_permission` VALUES (131, 6, 39);
INSERT INTO `sys_role_permission` VALUES (132, 6, 40);
INSERT INTO `sys_role_permission` VALUES (133, 6, 42);

-- ----------------------------
-- Table structure for sys_user
-- ----------------------------
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '鐢ㄦ埛ID',
  `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '鐢ㄦ埛鍚',
  `password` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '瀵嗙爜锛圔Crypt鍔犲瘑锛',
  `real_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '鐪熷疄濮撳悕',
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '鎵嬫満鍙',
  `email` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '閭??',
  `avatar` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '澶村儚URL',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '鐘舵?锛?绂佺敤 1姝ｅ父',
  `company_id` bigint NULL DEFAULT NULL COMMENT '所属公司ID',
  `department` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '部门',
  `create_by` bigint NULL DEFAULT NULL COMMENT '鍒涘缓浜篒D',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '鍒涘缓鏃堕棿',
  `update_by` bigint NULL DEFAULT NULL COMMENT '鏇存柊浜篒D',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '鏇存柊鏃堕棿',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '閫昏緫鍒犻櫎锛?鏈?垹 1宸插垹',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_user_username`(((case when (`deleted` = 0) then `username` else NULL end)) ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 6 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '绯荤粺鐢ㄦ埛琛' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_user
-- ----------------------------
INSERT INTO `sys_user` VALUES (1, 'admin', '$2b$12$MPNCwxVVTbgUfTtoplfF.e4rCXbD7zb6529bQGIrzVRlQe6MoYYIS', '系统管理员', '13800138000', NULL, NULL, 1, 1, NULL, NULL, '2026-04-28 15:30:01', NULL, '2026-04-28 20:06:45', 0);
INSERT INTO `sys_user` VALUES (2, 'finance', '$2b$12$MPNCwxVVTbgUfTtoplfF.e4rCXbD7zb6529bQGIrzVRlQe6MoYYIS', '财务小王', '13800138001', NULL, NULL, 1, 1, NULL, NULL, '2026-04-28 15:30:01', NULL, '2026-04-30 11:53:38', 0);
INSERT INTO `sys_user` VALUES (3, 'operator', '$2b$12$MPNCwxVVTbgUfTtoplfF.e4rCXbD7zb6529bQGIrzVRlQe6MoYYIS', '操作员小李', '13800138002', NULL, NULL, 1, 1, NULL, NULL, '2026-04-28 15:30:01', NULL, '2026-04-28 21:17:31', 0);
INSERT INTO `sys_user` VALUES (4, 'designer_li', 'e10adc3949ba59abbe56e057f20f883e', '李明', '13900001001', NULL, NULL, 1, 1, NULL, NULL, '2026-04-28 19:57:38', NULL, '2026-04-30 11:53:21', 0);
INSERT INTO `sys_user` VALUES (5, 'designer_wang', 'e10adc3949ba59abbe56e057f20f883e', '王设计', '13900001002', NULL, NULL, 1, 1, NULL, NULL, '2026-04-28 19:57:38', NULL, '2026-04-30 12:08:00', 0);

-- ----------------------------
-- Table structure for sys_user_role
-- ----------------------------
DROP TABLE IF EXISTS `sys_user_role`;
CREATE TABLE `sys_user_role`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `role_id` bigint NOT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `idx_user_role`(`user_id` ASC, `role_id` ASC) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_role_id`(`role_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 9 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '鐢ㄦ埛瑙掕壊鍏宠仈琛' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_user_role
-- ----------------------------
INSERT INTO `sys_user_role` VALUES (1, 1, 1);
INSERT INTO `sys_user_role` VALUES (2, 2, 3);
INSERT INTO `sys_user_role` VALUES (3, 3, 4);
INSERT INTO `sys_user_role` VALUES (6, 3, 5);
INSERT INTO `sys_user_role` VALUES (4, 3, 6);
INSERT INTO `sys_user_role` VALUES (5, 4, 6);
INSERT INTO `sys_user_role` VALUES (7, 5, 6);

-- ----------------------------
-- Table structure for todo_item
-- ----------------------------
DROP TABLE IF EXISTS `todo_item`;
CREATE TABLE `todo_item`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `customer_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '客户名称',
  `contact_phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '联系电话',
  `contact_person` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '联系人',
  `dimensions` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '量好的尺寸（如：宽60cm×高90cm×数量2块）',
  `requirements` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '客户需求描述（材质、工艺、交期等）',
  `status` tinyint NULL DEFAULT 1 COMMENT '状态：1新收集 2分析中 3待确认 4已转订单',
  `quote_amount` decimal(15, 2) NULL DEFAULT NULL COMMENT 'AI生成报价金额（草稿）',
  `quote_detail` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT 'AI生成的报价明细（JSON）',
  `order_id` bigint NULL DEFAULT NULL COMMENT '关联的正式订单ID',
  `order_no` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '关联的正式订单编号',
  `priority` tinyint NULL DEFAULT 1 COMMENT '优先级：1普通 2紧急 3加急',
  `source` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '来源：门店/电话/微信/上门',
  `remark` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '内部备注',
  `creator_id` bigint NULL DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE,
  INDEX `idx_customer`(`customer_name` ASC) USING BTREE,
  INDEX `idx_order`(`order_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '待办事项/需求收集' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of todo_item
-- ----------------------------
INSERT INTO `todo_item` VALUES (1, '张三（奶茶店）', '13900001111', NULL, '门头招牌：宽80cm×高120cm×1块', '亚克力发光字，白天晚上两用，红色为主色调，要求3天内完成', 3, NULL, NULL, NULL, NULL, 2, '上门量尺', NULL, 1, '2026-04-28 20:11:41', '2026-04-28 21:08:07', 1);
INSERT INTO `todo_item` VALUES (2, '李四（服装店）', '13900002222', NULL, '橱窗贴：宽60cm×高90cm×3张', '高清车贴，过膜，3天内制作完成', 4, NULL, NULL, 2, NULL, 1, '门店', NULL, 1, '2026-04-28 20:11:41', '2026-04-30 13:17:23', 0);
INSERT INTO `todo_item` VALUES (3, '王五（餐厅）', '13900003333', NULL, '桌卡：宽10cm×高14cm×50个', 'pvc透明桌卡，双面印刷，含设计费', 3, NULL, NULL, NULL, NULL, 1, '电话', NULL, 1, '2026-04-28 20:11:41', '2026-04-28 21:08:40', 0);
INSERT INTO `todo_item` VALUES (4, '水泵', '', NULL, '跑马灯灯箱，60*40cm', '', 4, NULL, NULL, 1, NULL, 1, '门店', '', NULL, '2026-04-28 21:10:35', '2026-04-30 12:43:58', 0);

SET FOREIGN_KEY_CHECKS = 1;
