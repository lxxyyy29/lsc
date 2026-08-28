-- =============================================================================
-- V34: 新增房屋、场所、组织力量管理权限
-- =============================================================================

-- 房屋管理菜单
INSERT INTO sys_permission (permission_code, permission_name, permission_type, client_type, parent_id, path, component, sort_order, status, remark, created_at, updated_at)
SELECT 'menu:community:building', '房屋/出租屋管理', 'MENU', 'WEB', p.id, '/community/buildings', 'community/BuildingListView', 25, 'ACTIVE', '网格治理-房屋管理', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM sys_permission p
WHERE p.permission_code = 'catalog:community'
  AND NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'menu:community:building');

-- 场所管理菜单
INSERT INTO sys_permission (permission_code, permission_name, permission_type, client_type, parent_id, path, component, sort_order, status, remark, created_at, updated_at)
SELECT 'menu:community:place', '场所资源管理', 'MENU', 'WEB', p.id, '/community/places', 'community/PlaceListView', 30, 'ACTIVE', '网格治理-场所管理', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM sys_permission p
WHERE p.permission_code = 'catalog:community'
  AND NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'menu:community:place');

-- 组织力量管理菜单
INSERT INTO sys_permission (permission_code, permission_name, permission_type, client_type, parent_id, path, component, sort_order, status, remark, created_at, updated_at)
SELECT 'menu:community:org-member', '组织力量管理', 'MENU', 'WEB', p.id, '/community/org-members', 'community/OrgMemberListView', 35, 'ACTIVE', '网格治理-组织力量管理', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM sys_permission p
WHERE p.permission_code = 'catalog:community'
  AND NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'menu:community:org-member');

-- 给 SUPER_ADMIN 分配所有新权限
INSERT INTO sys_role_permission (role_id, permission_id, created_at, updated_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM sys_role r
CROSS JOIN sys_permission p
WHERE r.role_code = 'SUPER_ADMIN'
  AND p.permission_code IN ('menu:community:building', 'menu:community:place', 'menu:community:org-member')
  AND NOT EXISTS (
      SELECT 1 FROM sys_role_permission srp
      WHERE srp.role_id = r.id AND srp.permission_id = p.id
  );
