-- V132: 监管设备（视频监控点位）接口权限补齐与授权
--
-- 背景：VideoSurveillanceController 的 /api/video/cameras* 接口要求
--   api:video:camera:list   查看（列表/统计/流地址/回放）
--   api:video:camera:manage 管理（新增/修改/删除点位）
-- 这两个权限码在 PermissionCodes 中已定义、V116 也明确"保留"，
-- 但库中该权限行可能整行缺失或状态为 INACTIVE（旧 dump / 旧环境迁移而来），
-- 且 V130 只对 SUPER_ADMIN 做了全量兜底授权，于是普通管理员账号打开
-- 「监管设备 / 监控点位」页会直接 403「当前用户没有所需权限: api:video:camera:list」。
--
-- 本迁移幂等做三件事：
--   1) 补齐 2 个 API 权限行（已存在则忽略）
--   2) 若被停用则恢复 ACTIVE（V116 之后权限行可能被置为 INACTIVE）
--   3) 授予 SUPER_ADMIN 与管理员（EVENT_OPERATOR）角色（已授权则跳过）

-- ---------- 1. 补齐权限行 ----------
INSERT IGNORE INTO sys_permission (permission_code, permission_name, permission_type, client_type, parent_id, path, component, icon, sort_order, status, remark, created_at, updated_at) VALUES
('api:video:camera:list',   '查看监控点位', 'API', 'WEB', NULL, '/api/video/cameras',      NULL, NULL, 301, 'ACTIVE', 'Video camera list API',   CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('api:video:camera:manage', '管理监控点位', 'API', 'WEB', NULL, '/api/video/cameras/{id}', NULL, NULL, 302, 'ACTIVE', 'Video camera manage API', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- ---------- 2. 若被停用则恢复 ----------
UPDATE sys_permission
SET status = 'ACTIVE', updated_at = CURRENT_TIMESTAMP
WHERE permission_code IN ('api:video:camera:list', 'api:video:camera:manage')
  AND status <> 'ACTIVE';

-- ---------- 3. 授权给超管与管理员角色 ----------
INSERT INTO sys_role_permission (role_id, permission_id, created_at, updated_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM sys_role r
JOIN sys_permission p ON p.permission_code IN ('api:video:camera:list', 'api:video:camera:manage')
LEFT JOIN sys_role_permission rp ON rp.role_id = r.id AND rp.permission_id = p.id
WHERE r.role_code IN ('SUPER_ADMIN', 'EVENT_OPERATOR') AND rp.role_id IS NULL;
