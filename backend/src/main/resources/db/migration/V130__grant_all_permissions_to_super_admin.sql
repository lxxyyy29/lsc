-- V130: 超级管理员权限兜底补齐
--
-- 背景：SUPER_ADMIN 并不会自动放行所有接口 —— 后端 PermissionGuard 按 sys_role_permission
-- 的实际授权判定。若库是由旧 dump / 旧服务器数据建立（权限行缺失或漏授权），超管会出现
-- 「新增角色提示无权限/不支持」这类问题：api:system:role:create 未授权 → 403。
--
-- 本迁移做两件幂等的事：
--   1) 补齐系统设置相关的 API 权限行（历史 dump 可能整行缺失）
--   2) 把当前存在的全部权限授予 SUPER_ADMIN，保证超管在任何环境都是全量权限

-- ---------- 1. 补齐可能缺失的系统管理 API 权限行 ----------
INSERT IGNORE INTO sys_permission (permission_code, permission_name, permission_type, client_type, parent_id, path, component, icon, sort_order, status, remark, created_at, updated_at) VALUES
('api:system:role:list',               '查询角色列表', 'API', 'WEB', NULL, '/api/system/roles',                  NULL, NULL, 201, 'ACTIVE', 'System role list API',       CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('api:system:role:detail',             '查询角色详情', 'API', 'WEB', NULL, '/api/system/roles/{id}',             NULL, NULL, 202, 'ACTIVE', 'System role detail API',     CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('api:system:role:create',             '创建角色',     'API', 'WEB', NULL, '/api/system/roles',                  NULL, NULL, 203, 'ACTIVE', 'System role create API',     CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('api:system:role:update',             '更新角色',     'API', 'WEB', NULL, '/api/system/roles/{id}',             NULL, NULL, 204, 'ACTIVE', 'System role update API',     CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('api:system:role:assign-permissions', '分配角色权限', 'API', 'WEB', NULL, '/api/system/roles/{id}/permissions', NULL, NULL, 205, 'ACTIVE', 'System role perm API',       CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('api:system:user:list',               '查询用户列表', 'API', 'WEB', NULL, '/api/system/users',                  NULL, NULL, 101, 'ACTIVE', 'System user list API',       CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('api:system:user:detail',             '查询用户详情', 'API', 'WEB', NULL, '/api/system/users/{id}',             NULL, NULL, 102, 'ACTIVE', 'System user detail API',     CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('api:system:user:create',             '创建用户',     'API', 'WEB', NULL, '/api/system/users',                  NULL, NULL, 103, 'ACTIVE', 'System user create API',     CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('api:system:user:update',             '更新用户',     'API', 'WEB', NULL, '/api/system/users/{id}',             NULL, NULL, 104, 'ACTIVE', 'System user update API',     CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('api:system:user:status',             '切换用户状态', 'API', 'WEB', NULL, '/api/system/users/{id}/status',      NULL, NULL, 105, 'ACTIVE', 'System user status API',     CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('api:system:user:assign-roles',       '分配用户角色', 'API', 'WEB', NULL, '/api/system/users/{id}/roles',       NULL, NULL, 106, 'ACTIVE', 'System user role API',       CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('api:system:user:delete',             '删除用户',     'API', 'WEB', NULL, '/api/system/users/{id}',             NULL, NULL, 108, 'ACTIVE', 'System user delete API',     CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('api:system:role:delete',             '删除角色',     'API', 'WEB', NULL, '/api/system/roles/{id}',             NULL, NULL, 133, 'ACTIVE', 'Role delete permission',     CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- ---------- 2. 全部权限授予 SUPER_ADMIN（幂等） ----------
INSERT INTO sys_role_permission (role_id, permission_id, created_at, updated_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM sys_role r
JOIN sys_permission p
LEFT JOIN sys_role_permission rp ON rp.role_id = r.id AND rp.permission_id = p.id
WHERE r.role_code = 'SUPER_ADMIN' AND rp.role_id IS NULL;
