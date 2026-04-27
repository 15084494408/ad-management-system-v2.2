-- =========================================================
-- v2.4 升级脚本：逻辑删除表唯一索引改造
-- 执行环境：MySQL 8.0+（需要函数索引支持）
--
-- 原因：@TableLogic 逻辑删除后，唯一索引仍约束已删除记录，
--       导致删除后再创建同名记录报 Duplicate entry 错误。
-- 方案：改为条件唯一索引，deleted=1 时字段值为 NULL，不参与唯一约束。
-- =========================================================

USE enterprise_ad;

-- 1. sys_user: username
ALTER TABLE sys_user DROP INDEX username;
ALTER TABLE sys_user DROP INDEX idx_username;
ALTER TABLE sys_user ADD UNIQUE INDEX uk_user_username ((CASE WHEN deleted = 0 THEN username ELSE NULL END));

-- 2. sys_role: role_code（列定义内联 UNIQUE + 独立 INDEX，两个都要删）
ALTER TABLE sys_role DROP INDEX role_code;
ALTER TABLE sys_role DROP INDEX idx_role_code;
ALTER TABLE sys_role ADD UNIQUE INDEX uk_role_code ((CASE WHEN deleted = 0 THEN role_code ELSE NULL END));

-- 3. sys_dict: code
ALTER TABLE sys_dict DROP INDEX code;
ALTER TABLE sys_dict ADD UNIQUE INDEX uk_dict_code ((CASE WHEN deleted = 0 THEN code ELSE NULL END));

-- 4. mem_member: phone
ALTER TABLE mem_member DROP INDEX idx_phone;
ALTER TABLE mem_member ADD UNIQUE INDEX uk_member_phone ((CASE WHEN deleted = 0 THEN phone ELSE NULL END));

-- 5. ord_order: order_no
ALTER TABLE ord_order DROP INDEX order_no;
ALTER TABLE ord_order DROP INDEX idx_order_no;
ALTER TABLE ord_order ADD UNIQUE INDEX uk_order_no ((CASE WHEN deleted = 0 THEN order_no ELSE NULL END));

-- 6. fac_factory_bill: bill_no
ALTER TABLE fac_factory_bill DROP INDEX bill_no;
ALTER TABLE fac_factory_bill ADD UNIQUE INDEX uk_bill_no ((CASE WHEN deleted = 0 THEN bill_no ELSE NULL END));

-- 7. fin_record: record_no
ALTER TABLE fin_record DROP INDEX record_no;
ALTER TABLE fin_record ADD UNIQUE INDEX uk_record_no ((CASE WHEN deleted = 0 THEN record_no ELSE NULL END));

-- 8. fin_quote: quote_no
ALTER TABLE fin_quote DROP INDEX idx_quote_no;
ALTER TABLE fin_quote ADD UNIQUE INDEX uk_quote_no ((CASE WHEN deleted = 0 THEN quote_no ELSE NULL END));

-- 9. fin_invoice: invoice_no
ALTER TABLE fin_invoice DROP INDEX idx_invoice_no;
ALTER TABLE fin_invoice ADD UNIQUE INDEX uk_invoice_no ((CASE WHEN deleted = 0 THEN invoice_no ELSE NULL END));

-- 10. mat_material: code
ALTER TABLE mat_material DROP INDEX code;
ALTER TABLE mat_material ADD UNIQUE INDEX uk_material_code ((CASE WHEN deleted = 0 THEN code ELSE NULL END));

-- 11. sq_requirement: req_no
ALTER TABLE sq_requirement DROP INDEX req_no;
ALTER TABLE sq_requirement ADD UNIQUE INDEX uk_req_no ((CASE WHEN deleted = 0 THEN req_no ELSE NULL END));

-- 12. sys_notice_setting: user_id
ALTER TABLE sys_notice_setting DROP INDEX user_id;
ALTER TABLE sys_notice_setting ADD UNIQUE INDEX uk_notice_setting_user ((CASE WHEN deleted = 0 THEN user_id ELSE NULL END));

-- =========================================================
-- 执行完毕！现在删除后的记录不再阻塞新建同名记录。
-- =========================================================
