-- H5 端上报改为创建事件：为可登录 H5 端的工作人员角色授予创建事件权限（api:event:create）
-- 背景：H5 端只有工作人员登录，上报应归纳为事件（biz_event）进入闭环处置流程，
-- 而非居民上报（cmn_resident_report）。EVENT_OPERATOR 已在 V5 获得该权限，SUPER_ADMIN 为全量权限。
-- 本次为 H5 端其他工作人员角色（移动端处置/核查、派单员、审核员、网格员）补充创建事件权限。

INSERT IGNORE INTO sys_role_permission (role_id, permission_id, created_at, updated_at)
SELECT r.id, p.id, NOW(), NOW()
FROM sys_role r
JOIN sys_permission p ON p.permission_code = 'api:event:create'
WHERE r.role_code IN ('H5_WORKER', 'H5_VERIFIER', 'DISPATCHER', 'AUDITOR', 'GRID_WORKER');
