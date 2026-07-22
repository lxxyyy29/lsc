-- =============================================================================
-- V38: 新增巡查记录管理和居民上报管理权限
-- =============================================================================

-- 巡查记录管理
INSERT INTO sys_permission (permission_code, permission_name, permission_type, client_type, parent_id, path, component, sort_order, status, remark, created_at, updated_at)
SELECT 'menu:community:patrol-record', '巡查记录管理', 'MENU', 'WEB', 216, '/community/patrol-records', 'community/PatrolRecordListView', 50, 'ACTIVE', '网格治理-巡查记录管理', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'menu:community:patrol-record');

-- 居民上报管理
INSERT INTO sys_permission (permission_code, permission_name, permission_type, client_type, parent_id, path, component, sort_order, status, remark, created_at, updated_at)
SELECT 'menu:community:resident-report', '居民上报管理', 'MENU', 'WEB', 216, '/community/resident-reports', 'community/ResidentReportListView', 60, 'ACTIVE', '网格治理-居民上报管理', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'menu:community:resident-report');

-- 分配给 SUPER_ADMIN
INSERT INTO sys_role_permission (role_id, permission_id, created_at, updated_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM sys_role r
CROSS JOIN sys_permission p
WHERE r.role_code = 'SUPER_ADMIN'
  AND p.permission_code IN ('menu:community:patrol-record', 'menu:community:resident-report')
  AND NOT EXISTS (
      SELECT 1 FROM sys_role_permission srp
      WHERE srp.role_id = r.id AND srp.permission_id = p.id
  );
