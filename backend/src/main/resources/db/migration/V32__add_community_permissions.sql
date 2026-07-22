-- =============================================================================
-- V32: 新增网格治理模块权限
-- =============================================================================

-- 网格治理目录
INSERT INTO sys_permission (permission_code, permission_name, permission_type, client_type, parent_id, path, sort_order, status, remark, created_at, updated_at)
SELECT 'catalog:community', '网格治理', 'CATALOG', 'WEB', NULL, '/community/grid', 40, 'ACTIVE', '网格治理目录', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'catalog:community');

-- GIS网格可视化菜单
INSERT INTO sys_permission (permission_code, permission_name, permission_type, client_type, parent_id, path, component, sort_order, status, remark, created_at, updated_at)
SELECT 'menu:community:grid', 'GIS网格可视化', 'MENU', 'WEB', p.id, '/community/grid', 'community/GridView', 15, 'ACTIVE', '网格治理-GIS网格可视化', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM sys_permission p
WHERE p.permission_code = 'catalog:community'
  AND NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'menu:community:grid');

-- 给 SUPER_ADMIN 分配所有社区治理权限
INSERT INTO sys_role_permission (role_id, permission_id, created_at, updated_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM sys_role r
CROSS JOIN sys_permission p
WHERE r.role_code = 'SUPER_ADMIN'
  AND p.permission_code IN ('catalog:community', 'menu:community:grid')
  AND NOT EXISTS (
      SELECT 1 FROM sys_role_permission srp
      WHERE srp.role_id = r.id AND srp.permission_id = p.id
  );
