-- H5 商户管理菜单权限
INSERT INTO sys_permission (permission_code, permission_name, permission_type, client_type, parent_id, path, component, icon, sort_order, status, remark, created_at, updated_at)
SELECT 'menu:h5:merchant:view', 'H5商户管理入口', 'MENU', 'H5', NULL, '/merchants', NULL, NULL, 310, 'ACTIVE', 'H5 merchant management menu permission', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'menu:h5:merchant:view');

-- H5 摊贩管理菜单权限
INSERT INTO sys_permission (permission_code, permission_name, permission_type, client_type, parent_id, path, component, icon, sort_order, status, remark, created_at, updated_at)
SELECT 'menu:h5:vendor:view', 'H5摊贩管理入口', 'MENU', 'H5', NULL, '/mobile-vendors', NULL, NULL, 320, 'ACTIVE', 'H5 vendor management menu permission', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'menu:h5:vendor:view');

-- H5 商户 API 权限
INSERT INTO sys_permission (permission_code, permission_name, permission_type, client_type, parent_id, path, component, icon, sort_order, status, remark, created_at, updated_at)
SELECT 'api:h5:merchant:list', 'H5查询商户列表', 'API', 'H5', NULL, '/api/h5/merchants', NULL, NULL, 330, 'ACTIVE', 'H5 merchant list API permission', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'api:h5:merchant:list');

INSERT INTO sys_permission (permission_code, permission_name, permission_type, client_type, parent_id, path, component, icon, sort_order, status, remark, created_at, updated_at)
SELECT 'api:h5:merchant:detail', 'H5查看商户详情', 'API', 'H5', NULL, '/api/h5/merchants/{id}', NULL, NULL, 331, 'ACTIVE', 'H5 merchant detail API permission', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'api:h5:merchant:detail');

INSERT INTO sys_permission (permission_code, permission_name, permission_type, client_type, parent_id, path, component, icon, sort_order, status, remark, created_at, updated_at)
SELECT 'api:h5:merchant:create', 'H5创建商户', 'API', 'H5', NULL, '/api/h5/merchants', NULL, NULL, 332, 'ACTIVE', 'H5 merchant create API permission', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'api:h5:merchant:create');

INSERT INTO sys_permission (permission_code, permission_name, permission_type, client_type, parent_id, path, component, icon, sort_order, status, remark, created_at, updated_at)
SELECT 'api:h5:merchant:update', 'H5更新商户', 'API', 'H5', NULL, '/api/h5/merchants/{id}', NULL, NULL, 333, 'ACTIVE', 'H5 merchant update API permission', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'api:h5:merchant:update');

INSERT INTO sys_permission (permission_code, permission_name, permission_type, client_type, parent_id, path, component, icon, sort_order, status, remark, created_at, updated_at)
SELECT 'api:h5:merchant:delete', 'H5删除商户', 'API', 'H5', NULL, '/api/h5/merchants/{id}', NULL, NULL, 334, 'ACTIVE', 'H5 merchant delete API permission', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'api:h5:merchant:delete');

-- H5 摊贩 API 权限
INSERT INTO sys_permission (permission_code, permission_name, permission_type, client_type, parent_id, path, component, icon, sort_order, status, remark, created_at, updated_at)
SELECT 'api:h5:vendor:list', 'H5查询摊贩列表', 'API', 'H5', NULL, '/api/h5/mobile-vendors', NULL, NULL, 340, 'ACTIVE', 'H5 vendor list API permission', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'api:h5:vendor:list');

INSERT INTO sys_permission (permission_code, permission_name, permission_type, client_type, parent_id, path, component, icon, sort_order, status, remark, created_at, updated_at)
SELECT 'api:h5:vendor:detail', 'H5查看摊贩详情', 'API', 'H5', NULL, '/api/h5/mobile-vendors/{id}', NULL, NULL, 341, 'ACTIVE', 'H5 vendor detail API permission', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'api:h5:vendor:detail');

INSERT INTO sys_permission (permission_code, permission_name, permission_type, client_type, parent_id, path, component, icon, sort_order, status, remark, created_at, updated_at)
SELECT 'api:h5:vendor:create', 'H5创建摊贩', 'API', 'H5', NULL, '/api/h5/mobile-vendors', NULL, NULL, 342, 'ACTIVE', 'H5 vendor create API permission', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'api:h5:vendor:create');

INSERT INTO sys_permission (permission_code, permission_name, permission_type, client_type, parent_id, path, component, icon, sort_order, status, remark, created_at, updated_at)
SELECT 'api:h5:vendor:update', 'H5更新摊贩', 'API', 'H5', NULL, '/api/h5/mobile-vendors/{id}', NULL, NULL, 343, 'ACTIVE', 'H5 vendor update API permission', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'api:h5:vendor:update');

INSERT INTO sys_permission (permission_code, permission_name, permission_type, client_type, parent_id, path, component, icon, sort_order, status, remark, created_at, updated_at)
SELECT 'api:h5:vendor:delete', 'H5删除摊贩', 'API', 'H5', NULL, '/api/h5/mobile-vendors/{id}', NULL, NULL, 344, 'ACTIVE', 'H5 vendor delete API permission', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'api:h5:vendor:delete');

-- H5 商户按钮权限
INSERT INTO sys_permission (permission_code, permission_name, permission_type, client_type, parent_id, path, component, icon, sort_order, status, remark, created_at, updated_at)
SELECT 'button:h5:merchant:create', 'H5新增商户按钮', 'BUTTON', 'H5', NULL, NULL, NULL, NULL, 350, 'ACTIVE', 'H5 merchant create button permission', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'button:h5:merchant:create');

INSERT INTO sys_permission (permission_code, permission_name, permission_type, client_type, parent_id, path, component, icon, sort_order, status, remark, created_at, updated_at)
SELECT 'button:h5:merchant:update', 'H5编辑商户按钮', 'BUTTON', 'H5', NULL, NULL, NULL, NULL, 351, 'ACTIVE', 'H5 merchant update button permission', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'button:h5:merchant:update');

INSERT INTO sys_permission (permission_code, permission_name, permission_type, client_type, parent_id, path, component, icon, sort_order, status, remark, created_at, updated_at)
SELECT 'button:h5:merchant:delete', 'H5删除商户按钮', 'BUTTON', 'H5', NULL, NULL, NULL, NULL, 352, 'ACTIVE', 'H5 merchant delete button permission', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'button:h5:merchant:delete');

-- H5 摊贩按钮权限
INSERT INTO sys_permission (permission_code, permission_name, permission_type, client_type, parent_id, path, component, icon, sort_order, status, remark, created_at, updated_at)
SELECT 'button:h5:vendor:create', 'H5新增摊贩按钮', 'BUTTON', 'H5', NULL, NULL, NULL, NULL, 360, 'ACTIVE', 'H5 vendor create button permission', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'button:h5:vendor:create');

INSERT INTO sys_permission (permission_code, permission_name, permission_type, client_type, parent_id, path, component, icon, sort_order, status, remark, created_at, updated_at)
SELECT 'button:h5:vendor:update', 'H5编辑摊贩按钮', 'BUTTON', 'H5', NULL, NULL, NULL, NULL, 361, 'ACTIVE', 'H5 vendor update button permission', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'button:h5:vendor:update');

INSERT INTO sys_permission (permission_code, permission_name, permission_type, client_type, parent_id, path, component, icon, sort_order, status, remark, created_at, updated_at)
SELECT 'button:h5:vendor:delete', 'H5删除摊贩按钮', 'BUTTON', 'H5', NULL, NULL, NULL, NULL, 362, 'ACTIVE', 'H5 vendor delete button permission', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'button:h5:vendor:delete');

-- 授予 SUPER_ADMIN 所有新增权限
INSERT INTO sys_role_permission (role_id, permission_id, created_at, updated_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM sys_role r
CROSS JOIN sys_permission p
WHERE r.role_code = 'SUPER_ADMIN'
  AND p.permission_code IN (
      'menu:h5:merchant:view',
      'menu:h5:vendor:view',
      'api:h5:merchant:list',
      'api:h5:merchant:detail',
      'api:h5:merchant:create',
      'api:h5:merchant:update',
      'api:h5:merchant:delete',
      'api:h5:vendor:list',
      'api:h5:vendor:detail',
      'api:h5:vendor:create',
      'api:h5:vendor:update',
      'api:h5:vendor:delete',
      'button:h5:merchant:create',
      'button:h5:merchant:update',
      'button:h5:merchant:delete',
      'button:h5:vendor:create',
      'button:h5:vendor:update',
      'button:h5:vendor:delete'
  )
  AND NOT EXISTS (
      SELECT 1
      FROM sys_role_permission srp
      WHERE srp.role_id = r.id
        AND srp.permission_id = p.id
  );
