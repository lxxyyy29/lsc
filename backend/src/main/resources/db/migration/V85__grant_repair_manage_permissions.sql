-- 便民报修管理：新增菜单与接口权限，并授予 Web 端业务角色
-- 背景：居民小程序端便民报修此前仅有居民本人查看接口（/resident/repairs），
-- Web 端缺少报修列表/详情/状态流转接口与菜单入口，本迁移补齐权限体系
-- （权限码 api:repair:view / api:repair:manage 已在 PermissionCodes 预留）。

-- 1. 菜单权限
INSERT INTO sys_permission (permission_code, permission_name, permission_type, client_type, parent_id, path, component, icon, sort_order, status, remark, created_at, updated_at)
SELECT 'menu:repair:view', '便民报修管理', 'MENU', 'WEB', NULL, '/repairs', NULL, NULL, 340, 'ACTIVE', 'Repair management menu permission', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'menu:repair:view');

-- 2. 接口权限
INSERT INTO sys_permission (permission_code, permission_name, permission_type, client_type, parent_id, path, component, icon, sort_order, status, remark, created_at, updated_at)
SELECT 'api:repair:view', '报修列表/详情查询', 'API', 'WEB', NULL, '/api/repairs', NULL, NULL, 341, 'ACTIVE', 'Repair list & detail API permission', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'api:repair:view');

INSERT INTO sys_permission (permission_code, permission_name, permission_type, client_type, parent_id, path, component, icon, sort_order, status, remark, created_at, updated_at)
SELECT 'api:repair:manage', '报修状态流转', 'API', 'WEB', NULL, '/api/repairs/{id}/status', NULL, NULL, 342, 'ACTIVE', 'Repair status update API permission', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'api:repair:manage');

-- 3. 授予 Web 端业务角色（管理员/事件专员/派单员/网格员）
INSERT IGNORE INTO sys_role_permission (role_id, permission_id, created_at, updated_at)
SELECT r.id, p.id, NOW(), NOW()
FROM sys_role r
CROSS JOIN sys_permission p
WHERE r.role_code IN ('SUPER_ADMIN', 'EVENT_OPERATOR', 'DISPATCHER', 'GRID_WORKER')
  AND p.permission_code IN ('menu:repair:view', 'api:repair:view', 'api:repair:manage');
