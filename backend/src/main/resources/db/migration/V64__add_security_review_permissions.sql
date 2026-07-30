-- Permissions added after tightening api:* checks to real RBAC checks.

INSERT INTO sys_permission (permission_code, permission_name, permission_type, client_type, parent_id, path, component, icon, sort_order, status, remark, created_at, updated_at)
SELECT permission_code, permission_name, permission_type, 'WEB', parent_id, path, component, icon, sort_order, 'ACTIVE', remark, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM (
    SELECT 'menu:system:audit-log' AS permission_code, '审计日志' AS permission_name, 'MENU' AS permission_type,
           (SELECT id FROM sys_permission WHERE permission_code = 'catalog:system') AS parent_id,
           '/audit-logs' AS path, 'system/AuditLogView' AS component, 'Document' AS icon, 50 AS sort_order,
           'Audit log menu' AS remark
    UNION ALL SELECT 'menu:parking', '停车管理', 'MENU', NULL, '/parking', 'parking/ParkingView', 'Car', 650, 'Parking management menu'
    UNION ALL SELECT 'menu:assessment', '考核研判', 'MENU', NULL, '/assessment', 'assessment/AssessmentView', 'TrendCharts', 660, 'Assessment menu'
    UNION ALL SELECT 'menu:safety:inspection', '安全防控', 'MENU', NULL, '/safety', 'safety/SafetyView', 'Warning', 670, 'Safety inspection menu'
    UNION ALL SELECT 'menu:party:view', '智慧党建', 'MENU', NULL, '/party', 'party/PartyView', 'Flag', 680, 'Party governance menu'
) seeded_menus
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = seeded_menus.permission_code);

INSERT INTO sys_permission (permission_code, permission_name, permission_type, client_type, parent_id, path, component, icon, sort_order, status, remark, created_at, updated_at)
SELECT permission_code, permission_name, 'API', 'WEB', parent_id, path, NULL, NULL, sort_order, 'ACTIVE', remark, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM (
    SELECT 'api:system:user:change-password' AS permission_code, '修改用户密码' AS permission_name,
           (SELECT id FROM sys_permission WHERE permission_code = 'menu:system:user') AS parent_id,
           '/api/system/users/{id}/password' AS path, 107 AS sort_order, 'System user password API' AS remark
    UNION ALL SELECT 'api:system:user:delete', '删除用户', (SELECT id FROM sys_permission WHERE permission_code = 'menu:system:user'), '/api/system/users/{id}', 108, 'System user delete API'
    UNION ALL SELECT 'api:audit-log:view', '查看审计日志', (SELECT id FROM sys_permission WHERE permission_code = 'menu:system:audit-log'), '/api/audit-logs', 501, 'Audit log view API'
    UNION ALL SELECT 'api:audit-log:rollback', '回滚审计版本', (SELECT id FROM sys_permission WHERE permission_code = 'menu:system:audit-log'), '/api/audit-logs/rollback/{id}', 502, 'Audit log rollback API'
    UNION ALL SELECT 'api:parking:view', '查看停车管理', (SELECT id FROM sys_permission WHERE permission_code = 'menu:parking'), '/api/parking/**', 651, 'Parking view API'
    UNION ALL SELECT 'api:parking:manage', '操作停车管理', (SELECT id FROM sys_permission WHERE permission_code = 'menu:parking'), '/api/parking/**', 652, 'Parking manage API'
    UNION ALL SELECT 'api:assessment:view', '查看考核研判', (SELECT id FROM sys_permission WHERE permission_code = 'menu:assessment'), '/api/assessment/**', 661, 'Assessment view API'
    UNION ALL SELECT 'api:safety:view', '查看安全防控', (SELECT id FROM sys_permission WHERE permission_code = 'menu:safety:inspection'), '/api/safety/inspections/**', 671, 'Safety view API'
    UNION ALL SELECT 'api:safety:manage', '操作安全防控', (SELECT id FROM sys_permission WHERE permission_code = 'menu:safety:inspection'), '/api/safety/inspections/**', 672, 'Safety manage API'
    UNION ALL SELECT 'api:party:view', '查看智慧党建', (SELECT id FROM sys_permission WHERE permission_code = 'menu:party:view'), '/api/party/**', 681, 'Party view API'
    UNION ALL SELECT 'api:party:manage', '操作智慧党建', (SELECT id FROM sys_permission WHERE permission_code = 'menu:party:view'), '/api/party/**', 682, 'Party manage API'
) seeded_apis
WHERE parent_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = seeded_apis.permission_code);

INSERT INTO sys_role_permission (role_id, permission_id, created_at, updated_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM sys_role r
JOIN sys_permission p ON p.permission_code IN (
    'menu:system:audit-log',
    'menu:parking',
    'menu:assessment',
    'menu:safety:inspection',
    'menu:party:view',
    'api:system:user:change-password',
    'api:system:user:delete',
    'api:audit-log:view',
    'api:audit-log:rollback',
    'api:parking:view',
    'api:parking:manage',
    'api:assessment:view',
    'api:safety:view',
    'api:safety:manage',
    'api:party:view',
    'api:party:manage'
)
WHERE r.role_code = 'SUPER_ADMIN'
  AND NOT EXISTS (
      SELECT 1
      FROM sys_role_permission srp
      WHERE srp.role_id = r.id
        AND srp.permission_id = p.id
  );
