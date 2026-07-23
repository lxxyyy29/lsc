-- =============================================================================
-- V44: 新增场所台账菜单权限
-- =============================================================================

INSERT INTO sys_permission (permission_code, permission_name, permission_type, client_type, parent_id, path, component, sort_order, status, remark, created_at, updated_at)
SELECT 'menu:biz:ledger', '场所台账', 'MENU', 'WEB', 123, '/biz/ledger', 'biz/MerchantLedgerView', 50, 'ACTIVE', '业务管理-场所台账', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'menu:biz:ledger');

-- 分配给 SUPER_ADMIN
INSERT INTO sys_role_permission (role_id, permission_id, created_at, updated_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM sys_role r
CROSS JOIN sys_permission p
WHERE r.role_code = 'SUPER_ADMIN'
  AND p.permission_code = 'menu:biz:ledger'
  AND NOT EXISTS (
      SELECT 1 FROM sys_role_permission srp
      WHERE srp.role_id = r.id AND srp.permission_id = p.id
  );
