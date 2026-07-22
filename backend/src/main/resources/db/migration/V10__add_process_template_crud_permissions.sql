INSERT INTO sys_permission (permission_code, permission_name, permission_type, client_type, path, sort_order, status, remark, created_at, updated_at)
SELECT 'api:process-template:detail', '查看流程模板详情', 'API', 'WEB', '/api/processes/templates/{id}', 129, 'ACTIVE', 'Process template CRUD permission', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'api:process-template:detail');

INSERT INTO sys_permission (permission_code, permission_name, permission_type, client_type, path, sort_order, status, remark, created_at, updated_at)
SELECT 'api:process-template:update', '修改流程模板', 'API', 'WEB', '/api/processes/templates/{id}', 130, 'ACTIVE', 'Process template CRUD permission', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'api:process-template:update');

INSERT INTO sys_permission (permission_code, permission_name, permission_type, client_type, path, sort_order, status, remark, created_at, updated_at)
SELECT 'api:process-template:delete', '删除流程模板', 'API', 'WEB', '/api/processes/templates/{id}', 131, 'ACTIVE', 'Process template CRUD permission', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'api:process-template:delete');

INSERT INTO sys_role_permission (role_id, permission_id, created_at, updated_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM sys_role r
JOIN sys_permission p ON p.permission_code IN ('api:process-template:detail', 'api:process-template:update', 'api:process-template:delete')
WHERE r.role_code = 'SUPER_ADMIN'
  AND NOT EXISTS (
      SELECT 1
      FROM sys_role_permission srp
      WHERE srp.role_id = r.id
        AND srp.permission_id = p.id
  );
