-- 创建普通群众角色（PUBLIC）并迁移居民测试账号
-- 背景：原注册接口在 PUBLIC 角色不存在时兜底绑定 EVENT_OPERATOR，
-- 导致普通用户（如 yonghu）获得 H5 端入口权限（menu:h5:*），可登录 H5 端。
-- 本次创建独立 PUBLIC 角色：仅授予 Web/MP 端所需权限，不授予任何 menu:h5:* 权限，
-- 使普通用户只能登录小程序端（MP 走 Web 登录接口），无法登录 H5 端。

-- 1. 创建 PUBLIC 角色（若不存在）
INSERT INTO sys_role (role_code, role_name, status, remark, created_at, updated_at)
SELECT 'PUBLIC', '普通群众', 'ACTIVE', '居民/普通用户角色，仅限小程序端使用', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_role WHERE role_code = 'PUBLIC');

-- 2. 为 PUBLIC 角色授予 Web/MP 端所需权限（无任何 menu:h5:* 权限）
INSERT IGNORE INTO sys_role_permission (role_id, permission_id, created_at, updated_at)
SELECT r.id, p.id, NOW(), NOW()
FROM sys_role r
JOIN sys_permission p
  ON p.permission_code IN (
      'menu:community:resident-report',
      'api:auth:web:me',
      'api:event:list'
  )
WHERE r.role_code = 'PUBLIC';

-- 3. 迁移已有普通群众账号：将 yonghu 从 EVENT_OPERATOR 迁移到 PUBLIC
INSERT IGNORE INTO sys_user_role (user_id, role_id)
SELECT u.id, r.id
FROM sys_user u
JOIN sys_role r ON r.role_code = 'PUBLIC'
WHERE u.username = 'yonghu' AND u.deleted = 0;

DELETE ur
FROM sys_user_role ur
JOIN sys_user u ON u.id = ur.user_id
JOIN sys_role r ON r.id = ur.role_id
WHERE u.username = 'yonghu' AND u.deleted = 0 AND r.role_code = 'EVENT_OPERATOR';
