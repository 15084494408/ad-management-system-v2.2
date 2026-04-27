-- =========================================================
-- 增量升级脚本：设计师提成模块
-- 执行时间：2026-04-28
-- =========================================================

-- 1. 订单表增加设计师ID字段
ALTER TABLE ord_order ADD COLUMN designer_id BIGINT COMMENT '设计师用户ID（关联sys_user）' AFTER designer_name;

-- 2. 新建设计师提成表
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

-- 3. 新增 DESIGNER 角色（如果不存在）
INSERT INTO sys_role (role_name, role_code, description, sort, deleted)
SELECT '设计师', 'DESIGNER', '设计师角色，可查看和管理设计相关订单', 6, 0
FROM DUAL WHERE NOT EXISTS (
    SELECT 1 FROM sys_role WHERE role_code = 'DESIGNER' AND deleted = 0
);

-- 4. 给 DESIGNER 角色分配基础权限（设计相关）
-- 注意：需要根据实际 sys_permission 表中的数据调整 permission_id
INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM sys_role r, sys_permission p
WHERE r.role_code = 'DESIGNER' AND r.deleted = 0
  AND p.status = 1 AND p.deleted = 0
  AND p.permission_code IN (
    'dashboard:view',
    'order:list', 'order:view', 'order:create', 'order:edit',
    'design:file',
    'square:manage',
    'customer:list'
  );
