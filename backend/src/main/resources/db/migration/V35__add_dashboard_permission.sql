-- =============================================================================
-- V35: 新增BI态势看板权限
-- =============================================================================

INSERT INTO sys_permission (permission_code, permission_name, permission_type, client_type, parent_id, path, component, sort_order, status, remark, created_at, updated_at)
SELECT 'menu:community:dashboard', 'BI态势看板', 'MENU', 'WEB', p.id, '/community/dashboard', 'community/DashboardView', 40, 'ACTIVE', '网格治理-BI态势看板', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM sys_permission p
WHERE p.permission_code = 'catalog:community'
  AND NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'menu:community:dashboard');

-- 给 SUPER_ADMIN 分配看板权限
INSERT INTO sys_role_permission (role_id, permission_id, created_at, updated_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM sys_role r
CROSS JOIN sys_permission p
WHERE r.role_code = 'SUPER_ADMIN'
  AND p.permission_code = 'menu:community:dashboard'
  AND NOT EXISTS (
      SELECT 1 FROM sys_role_permission srp
      WHERE srp.role_id = r.id AND srp.permission_id = p.id
  );
