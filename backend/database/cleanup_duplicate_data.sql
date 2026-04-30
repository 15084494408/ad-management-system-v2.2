-- =========================================================
-- 企业广告管理系统 v2.2 init.sql 重复数据清理脚本
-- 执行日期: 2026-04-30
-- 说明: 删除 init.sql 中与基础数据重复的增量同步数据
-- =========================================================
--
-- 注意：以下清理脚本用于识别和删除重复数据
-- 执行前请确保已备份数据库
--
-- 重复数据位置: init.sql 第1067-1098行（sys_permission增量INSERT）
-- 与第1067-1098行数据重复的 INSERT 语句
--
-- 建议：
-- 1. 在 init.sql 中删除第1065-1098行（增量同步部分）
-- 2. 或使用以下脚本在数据库中清理重复记录（已存在的生产环境）
--
-- 以下是数据库清理脚本（仅在生产环境执行）：
/*

-- 检查重复的权限记录
SELECT permission_code, COUNT(*) as cnt
FROM sys_permission
GROUP BY permission_code
HAVING cnt > 1;

-- 删除重复的权限记录（保留ID最小的）
DELETE FROM sys_permission
WHERE id NOT IN (
    SELECT MIN(id) FROM sys_permission GROUP BY permission_code
);

-- 检查重复的按钮记录
SELECT permission, COUNT(*) as cnt
FROM sys_button
GROUP BY permission
HAVING cnt > 1;

-- 删除重复的按钮记录
DELETE FROM sys_button
WHERE id NOT IN (
    SELECT MIN(id) FROM sys_button GROUP BY permission
);

*/
