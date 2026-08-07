-- H5 志愿服务：新增菜单与接口权限，并授予所有可登录 H5 端的角色
-- 背景：志愿服务报名此前仅 Web 端可用（party:manage 权限 + 硬编码 userId），
-- 现放开 H5 端供工作人员自助报名/取消/查看积分。

-- 1. 菜单权限
INSERT INTO sys_permission (permission_code, permission_name, permission_type, client_type, parent_id, path, component, icon, sort_order, status, remark, created_at, updated_at)
SELECT 'menu:h5:volunteer:view', 'H5志愿服务入口', 'MENU', 'H5', NULL, '/volunteer', NULL, NULL, 325, 'ACTIVE', 'H5 volunteer service menu permission', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'menu:h5:volunteer:view');

-- 2. API 权限
INSERT INTO sys_permission (permission_code, permission_name, permission_type, client_type, parent_id, path, component, icon, sort_order, status, remark, created_at, updated_at)
SELECT 'api:h5:volunteer:list', 'H5查询志愿活动列表', 'API', 'H5', NULL, '/api/h5/activities', NULL, NULL, 326, 'ACTIVE', 'H5 volunteer activity list API permission', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'api:h5:volunteer:list');

INSERT INTO sys_permission (permission_code, permission_name, permission_type, client_type, parent_id, path, component, icon, sort_order, status, remark, created_at, updated_at)
SELECT 'api:h5:volunteer:signup', 'H5志愿活动报名', 'API', 'H5', NULL, '/api/h5/activities/{id}/signup', NULL, NULL, 327, 'ACTIVE', 'H5 volunteer signup API permission', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'api:h5:volunteer:signup');

INSERT INTO sys_permission (permission_code, permission_name, permission_type, client_type, parent_id, path, component, icon, sort_order, status, remark, created_at, updated_at)
SELECT 'api:h5:volunteer:points', 'H5查询志愿积分', 'API', 'H5', NULL, '/api/h5/points', NULL, NULL, 328, 'ACTIVE', 'H5 volunteer points API permission', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'api:h5:volunteer:points');

-- 3. 授予所有拥有 H5 工作台入口权限（menu:h5:workbench:view）的角色
INSERT INTO sys_role_permission (role_id, permission_id, created_at, updated_at)
SELECT DISTINCT srp.role_id, p.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM sys_role_permission srp
JOIN sys_permission entry_p
  ON entry_p.id = srp.permission_id
 AND entry_p.permission_code = 'menu:h5:workbench:view'
CROSS JOIN sys_permission p
WHERE p.permission_code IN (
      'menu:h5:volunteer:view',
      'api:h5:volunteer:list',
      'api:h5:volunteer:signup',
      'api:h5:volunteer:points'
  )
  AND NOT EXISTS (
      SELECT 1
      FROM sys_role_permission x
      WHERE x.role_id = srp.role_id
        AND x.permission_id = p.id
  );
