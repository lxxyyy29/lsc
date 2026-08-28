INSERT INTO sys_permission (permission_code, permission_name, permission_type, client_type, path, sort_order, status, remark, created_at, updated_at)
SELECT 'api:workorder:handle', 'Web处置工单', 'API', 'WEB', '/api/work-orders/{id}/handle', 132, 'ACTIVE', 'Web workorder handle permission', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'api:workorder:handle');
