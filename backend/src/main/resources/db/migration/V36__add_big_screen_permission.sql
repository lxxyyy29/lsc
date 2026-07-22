-- =============================================================================
-- V36: 新增综合监管大屏权限
-- =============================================================================

INSERT INTO sys_permission (permission_code, permission_name, permission_type, client_type, parent_id, path, sort_order, status, remark, created_at, updated_at)
SELECT 'menu:big-screen:view', '综合监管大屏', 'MENU', 'WEB', NULL, '/big-screen', 45, 'ACTIVE', '综合监管大屏', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'menu:big-screen:view');

-- 给 SUPER_ADMIN 分配大屏权限
INSERT INTO sys_role_permission (role_id, permission_id, created_at, updated_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM sys_role r
CROSS JOIN sys_permission p
WHERE r.role_code = 'SUPER_ADMIN'
  AND p.permission_code = 'menu:big-screen:view'
  AND NOT EXISTS (
      SELECT 1 FROM sys_role_permission srp
      WHERE srp.role_id = r.id AND srp.permission_id = p.id
  );
