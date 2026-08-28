-- Keep existing business roles functional after api:* permissions are enforced strictly.
-- High-risk capabilities such as audit rollback and user management remain SUPER_ADMIN-only.

INSERT INTO sys_role_permission (role_id, permission_id, created_at, updated_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM sys_role r
JOIN sys_permission p ON p.permission_code IN (
    'menu:community:dashboard',
    'api:assessment:view',
    'api:parking:view'
)
WHERE r.role_code IN ('EVENT_OPERATOR', 'AUDITOR', 'DISPATCHER')
  AND NOT EXISTS (
      SELECT 1
      FROM sys_role_permission srp
      WHERE srp.role_id = r.id
        AND srp.permission_id = p.id
  );

INSERT INTO sys_role_permission (role_id, permission_id, created_at, updated_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM sys_role r
JOIN sys_permission p ON p.permission_code IN (
    'menu:community:grid',
    'menu:community:population',
    'menu:community:building',
    'menu:community:place',
    'menu:community:org-member',
    'menu:community:patrol-record',
    'menu:community:resident-report',
    'api:safety:view',
    'api:party:view'
)
WHERE r.role_code = 'EVENT_OPERATOR'
  AND NOT EXISTS (
      SELECT 1
      FROM sys_role_permission srp
      WHERE srp.role_id = r.id
        AND srp.permission_id = p.id
  );

INSERT INTO sys_role_permission (role_id, permission_id, created_at, updated_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM sys_role r
JOIN sys_permission p ON p.permission_code IN (
    'api:drone:device:list',
    'api:drone:job:list',
    'api:drone:wayline:list',
    'api:drone:ws:connect',
    'api:drone:media:list',
    'api:drone:media:files'
)
WHERE r.role_code IN ('EVENT_OPERATOR', 'DISPATCHER')
  AND NOT EXISTS (
      SELECT 1
      FROM sys_role_permission srp
      WHERE srp.role_id = r.id
        AND srp.permission_id = p.id
  );

INSERT INTO sys_role_permission (role_id, permission_id, created_at, updated_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM sys_role r
JOIN sys_permission p ON p.permission_code IN (
    'api:workorder:handle'
)
WHERE r.role_code = 'DISPATCHER'
  AND NOT EXISTS (
      SELECT 1
      FROM sys_role_permission srp
      WHERE srp.role_id = r.id
        AND srp.permission_id = p.id
  );
