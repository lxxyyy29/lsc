-- 信息互通：添加 H5 端消息菜单权限，并授予 GRID_WORKER 角色
INSERT IGNORE INTO sys_permission (permission_code, permission_name, permission_type, client_type, sort_order, status)
VALUES ('menu:h5:message:view', '信息互通', 'MENU', 'H5', 60, 'ACTIVE');

-- 授予 GRID_WORKER 角色
INSERT IGNORE INTO sys_role_permission (role_id, permission_id, created_at, updated_at)
SELECT r.id, p.id, NOW(), NOW()
FROM sys_role r, sys_permission p
WHERE r.role_code = 'GRID_WORKER'
  AND p.permission_code = 'menu:h5:message:view';
