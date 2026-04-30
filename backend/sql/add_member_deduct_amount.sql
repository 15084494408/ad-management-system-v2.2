-- 订单表新增会员抵扣金额字段
-- 用于记录收款时从会员余额扣除了多少，取消订单时据此退回

ALTER TABLE ord_order
    ADD COLUMN member_deduct_amount DECIMAL(15,2) DEFAULT 0 COMMENT '会员余额抵扣金额（取消订单时据此退回）'
    AFTER rounding_amount;
