ALTER TABLE sys_permission
    ADD COLUMN component VARCHAR(255) NULL AFTER path,
    ADD COLUMN icon VARCHAR(100) NULL AFTER component;

CREATE INDEX idx_sys_permission_parent_sort ON sys_permission (parent_id, sort_order);

INSERT INTO sys_permission (permission_code, permission_name, permission_type, client_type, parent_id, path, component, icon, sort_order, status, remark, created_at, updated_at)
SELECT 'catalog:system', '系统管理', 'CATALOG', 'WEB', NULL, '/system', NULL, 'Setting', 900, 'ACTIVE', 'System management catalog', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'catalog:system');

INSERT INTO sys_permission (permission_code, permission_name, permission_type, client_type, parent_id, path, component, icon, sort_order, status, remark, created_at, updated_at)
SELECT 'menu:system:user', '用户管理', 'MENU', 'WEB', p.id, '/system/users', 'system/users/index', 'User', 10, 'ACTIVE', 'System user management menu', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM sys_permission p
WHERE p.permission_code = 'catalog:system'
  AND NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'menu:system:user');

INSERT INTO sys_permission (permission_code, permission_name, permission_type, client_type, parent_id, path, component, icon, sort_order, status, remark, created_at, updated_at)
SELECT 'menu:system:role', '角色管理', 'MENU', 'WEB', p.id, '/system/roles', 'system/roles/index', 'UserFilled', 20, 'ACTIVE', 'System role management menu', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM sys_permission p
WHERE p.permission_code = 'catalog:system'
  AND NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'menu:system:role');

INSERT INTO sys_permission (permission_code, permission_name, permission_type, client_type, parent_id, path, component, icon, sort_order, status, remark, created_at, updated_at)
SELECT 'menu:system:menu', '菜单管理', 'MENU', 'WEB', p.id, '/system/menus', 'system/menus/index', 'Menu', 30, 'ACTIVE', 'System menu management menu', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM sys_permission p
WHERE p.permission_code = 'catalog:system'
  AND NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'menu:system:menu');

INSERT INTO sys_permission (permission_code, permission_name, permission_type, client_type, parent_id, path, component, icon, sort_order, status, remark, created_at, updated_at)
SELECT 'menu:system:permission', '权限管理', 'MENU', 'WEB', p.id, '/system/permissions', 'system/permissions/index', 'Lock', 40, 'ACTIVE', 'System permission management menu', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM sys_permission p
WHERE p.permission_code = 'catalog:system'
  AND NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'menu:system:permission');

INSERT INTO sys_permission (permission_code, permission_name, permission_type, client_type, parent_id, path, component, icon, sort_order, status, remark, created_at, updated_at)
SELECT permission_code, permission_name, permission_type, 'WEB', parent_id, path, NULL, NULL, sort_order, 'ACTIVE', remark, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM (
    SELECT 'button:system:user:create' AS permission_code, '新建用户' AS permission_name, 'BUTTON' AS permission_type,
           (SELECT id FROM sys_permission WHERE permission_code = 'menu:system:user') AS parent_id,
           NULL AS path, 11 AS sort_order, 'System user create button' AS remark
    UNION ALL SELECT 'button:system:user:update', '编辑用户', 'BUTTON', (SELECT id FROM sys_permission WHERE permission_code = 'menu:system:user'), NULL, 12, 'System user update button'
    UNION ALL SELECT 'button:system:user:status', '启停用户', 'BUTTON', (SELECT id FROM sys_permission WHERE permission_code = 'menu:system:user'), NULL, 13, 'System user status button'
    UNION ALL SELECT 'button:system:user:assign-roles', '分配角色', 'BUTTON', (SELECT id FROM sys_permission WHERE permission_code = 'menu:system:user'), NULL, 14, 'System user assign roles button'
    UNION ALL SELECT 'button:system:role:create', '新建角色', 'BUTTON', (SELECT id FROM sys_permission WHERE permission_code = 'menu:system:role'), NULL, 21, 'System role create button'
    UNION ALL SELECT 'button:system:role:update', '编辑角色', 'BUTTON', (SELECT id FROM sys_permission WHERE permission_code = 'menu:system:role'), NULL, 22, 'System role update button'
    UNION ALL SELECT 'button:system:role:assign-permissions', '分配权限', 'BUTTON', (SELECT id FROM sys_permission WHERE permission_code = 'menu:system:role'), NULL, 23, 'System role assign permissions button'
    UNION ALL SELECT 'button:system:menu:create', '新建菜单', 'BUTTON', (SELECT id FROM sys_permission WHERE permission_code = 'menu:system:menu'), NULL, 31, 'System menu create button'
    UNION ALL SELECT 'button:system:menu:update', '编辑菜单', 'BUTTON', (SELECT id FROM sys_permission WHERE permission_code = 'menu:system:menu'), NULL, 32, 'System menu update button'
    UNION ALL SELECT 'button:system:menu:delete', '删除菜单', 'BUTTON', (SELECT id FROM sys_permission WHERE permission_code = 'menu:system:menu'), NULL, 33, 'System menu delete button'
) seeded_buttons
WHERE parent_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = seeded_buttons.permission_code);

INSERT INTO sys_permission (permission_code, permission_name, permission_type, client_type, parent_id, path, component, icon, sort_order, status, remark, created_at, updated_at)
SELECT permission_code, permission_name, 'API', 'WEB', parent_id, path, NULL, NULL, sort_order, 'ACTIVE', remark, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM (
    SELECT 'api:system:user:list' AS permission_code, '查询用户列表' AS permission_name,
           (SELECT id FROM sys_permission WHERE permission_code = 'menu:system:user') AS parent_id,
           '/api/system/users' AS path, 101 AS sort_order, 'System user list API' AS remark
    UNION ALL SELECT 'api:system:user:detail', '查询用户详情', (SELECT id FROM sys_permission WHERE permission_code = 'menu:system:user'), '/api/system/users/{id}', 102, 'System user detail API'
    UNION ALL SELECT 'api:system:user:create', '创建用户', (SELECT id FROM sys_permission WHERE permission_code = 'menu:system:user'), '/api/system/users', 103, 'System user create API'
    UNION ALL SELECT 'api:system:user:update', '更新用户', (SELECT id FROM sys_permission WHERE permission_code = 'menu:system:user'), '/api/system/users/{id}', 104, 'System user update API'
    UNION ALL SELECT 'api:system:user:status', '切换用户状态', (SELECT id FROM sys_permission WHERE permission_code = 'menu:system:user'), '/api/system/users/{id}/status', 105, 'System user status API'
    UNION ALL SELECT 'api:system:user:assign-roles', '分配用户角色', (SELECT id FROM sys_permission WHERE permission_code = 'menu:system:user'), '/api/system/users/{id}/roles', 106, 'System user role assignment API'
    UNION ALL SELECT 'api:system:role:list', '查询角色列表', (SELECT id FROM sys_permission WHERE permission_code = 'menu:system:role'), '/api/system/roles', 201, 'System role list API'
    UNION ALL SELECT 'api:system:role:detail', '查询角色详情', (SELECT id FROM sys_permission WHERE permission_code = 'menu:system:role'), '/api/system/roles/{id}', 202, 'System role detail API'
    UNION ALL SELECT 'api:system:role:create', '创建角色', (SELECT id FROM sys_permission WHERE permission_code = 'menu:system:role'), '/api/system/roles', 203, 'System role create API'
    UNION ALL SELECT 'api:system:role:update', '更新角色', (SELECT id FROM sys_permission WHERE permission_code = 'menu:system:role'), '/api/system/roles/{id}', 204, 'System role update API'
    UNION ALL SELECT 'api:system:role:assign-permissions', '分配角色权限', (SELECT id FROM sys_permission WHERE permission_code = 'menu:system:role'), '/api/system/roles/{id}/permissions', 205, 'System role permission assignment API'
    UNION ALL SELECT 'api:system:menu:list', '查询菜单树', (SELECT id FROM sys_permission WHERE permission_code = 'menu:system:menu'), '/api/system/menus/tree', 301, 'System menu tree API'
    UNION ALL SELECT 'api:system:menu:create', '创建菜单', (SELECT id FROM sys_permission WHERE permission_code = 'menu:system:menu'), '/api/system/menus', 302, 'System menu create API'
    UNION ALL SELECT 'api:system:menu:update', '更新菜单', (SELECT id FROM sys_permission WHERE permission_code = 'menu:system:menu'), '/api/system/menus/{id}', 303, 'System menu update API'
    UNION ALL SELECT 'api:system:menu:delete', '删除菜单', (SELECT id FROM sys_permission WHERE permission_code = 'menu:system:menu'), '/api/system/menus/{id}', 304, 'System menu delete API'
    UNION ALL SELECT 'api:system:permission:list', '查询权限树', (SELECT id FROM sys_permission WHERE permission_code = 'menu:system:permission'), '/api/system/permissions/tree', 401, 'System permission tree API'
) seeded_apis
WHERE parent_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = seeded_apis.permission_code);

INSERT INTO sys_role_permission (role_id, permission_id, created_at, updated_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM sys_role r
JOIN sys_permission p ON p.permission_code IN (
    'catalog:system',
    'menu:system:user',
    'menu:system:role',
    'menu:system:menu',
    'menu:system:permission',
    'button:system:user:create',
    'button:system:user:update',
    'button:system:user:status',
    'button:system:user:assign-roles',
    'button:system:role:create',
    'button:system:role:update',
    'button:system:role:assign-permissions',
    'button:system:menu:create',
    'button:system:menu:update',
    'button:system:menu:delete',
    'api:system:user:list',
    'api:system:user:detail',
    'api:system:user:create',
    'api:system:user:update',
    'api:system:user:status',
    'api:system:user:assign-roles',
    'api:system:role:list',
    'api:system:role:detail',
    'api:system:role:create',
    'api:system:role:update',
    'api:system:role:assign-permissions',
    'api:system:menu:list',
    'api:system:menu:create',
    'api:system:menu:update',
    'api:system:menu:delete',
    'api:system:permission:list'
)
WHERE r.role_code = 'SUPER_ADMIN'
  AND NOT EXISTS (
      SELECT 1
      FROM sys_role_permission srp
      WHERE srp.role_id = r.id
        AND srp.permission_id = p.id
  );
