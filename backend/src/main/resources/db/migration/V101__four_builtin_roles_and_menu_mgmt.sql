-- =============================================================
-- V101 角色归一为 4 个内置角色 + 菜单管理权限
-- 内置角色：SUPER_ADMIN 超级管理员 / EVENT_OPERATOR 管理员 /
--           GRID_WORKER 网格员 / PUBLIC 居民
-- 每个账号只保留一个角色
-- =============================================================

-- 1. 内置角色名称归一（role_code 不动，代码中多处引用）
UPDATE sys_role SET role_name = '超级管理员', remark = '内置角色：拥有系统全部权限' WHERE role_code = 'SUPER_ADMIN';
UPDATE sys_role SET role_name = '管理员', remark = '内置角色：负责日常业务处置（原事件专员等管理岗合并）' WHERE role_code = 'EVENT_OPERATOR';
UPDATE sys_role SET role_name = '网格员', remark = '内置角色：移动端网格巡查与工单处置' WHERE role_code = 'GRID_WORKER';
UPDATE sys_role SET role_name = '居民', remark = '内置角色：居民端上报与查询' WHERE role_code = 'PUBLIC';

-- 2. 仅持有待裁撤角色（审核员/派单员/H5处置/核查/自定义角色）的用户归入"管理员"
INSERT INTO sys_user_role (user_id, role_id, created_at, updated_at)
SELECT DISTINCT sur.user_id,
       (SELECT id FROM sys_role WHERE role_code = 'EVENT_OPERATOR'),
       NOW(), NOW()
FROM sys_user_role sur
JOIN sys_role r ON r.id = sur.role_id
WHERE r.role_code NOT IN ('SUPER_ADMIN', 'EVENT_OPERATOR', 'GRID_WORKER', 'PUBLIC')
  AND NOT EXISTS (
      SELECT 1 FROM sys_user_role x
      JOIN sys_role rx ON rx.id = x.role_id
      WHERE x.user_id = sur.user_id
        AND rx.role_code IN ('SUPER_ADMIN', 'EVENT_OPERATOR', 'GRID_WORKER', 'PUBLIC'));

-- 3. 每个账号只保留一个角色（优先级：超级管理员 > 管理员 > 网格员 > 居民）
DELETE FROM sys_user_role
WHERE user_id IN (
        SELECT user_id FROM (
            SELECT sur.user_id FROM sys_user_role sur
            JOIN sys_role r ON r.id = sur.role_id
            WHERE r.role_code = 'SUPER_ADMIN'
        ) t)
  AND role_id <> (SELECT id FROM sys_role WHERE role_code = 'SUPER_ADMIN');

DELETE FROM sys_user_role
WHERE user_id IN (
        SELECT user_id FROM (
            SELECT sur.user_id FROM sys_user_role sur
            JOIN sys_role r ON r.id = sur.role_id
            WHERE r.role_code = 'EVENT_OPERATOR'
        ) t)
  AND role_id <> (SELECT id FROM sys_role WHERE role_code = 'EVENT_OPERATOR');

DELETE FROM sys_user_role
WHERE user_id IN (
        SELECT user_id FROM (
            SELECT sur.user_id FROM sys_user_role sur
            JOIN sys_role r ON r.id = sur.role_id
            WHERE r.role_code = 'GRID_WORKER'
        ) t)
  AND role_id <> (SELECT id FROM sys_role WHERE role_code = 'GRID_WORKER');

DELETE FROM sys_user_role
WHERE user_id IN (
        SELECT user_id FROM (
            SELECT sur.user_id FROM sys_user_role sur
            JOIN sys_role r ON r.id = sur.role_id
            WHERE r.role_code = 'PUBLIC'
        ) t)
  AND role_id <> (SELECT id FROM sys_role WHERE role_code = 'PUBLIC');

-- 4. sys_user.role_id（旧主角色字段）同步为用户当前唯一角色（先于删角色，避免外键 fk_sys_user_role 阻断）
UPDATE sys_user u
JOIN sys_user_role sur ON sur.user_id = u.id
SET u.role_id = sur.role_id
WHERE u.deleted = 0;

UPDATE sys_user
SET role_id = NULL
WHERE role_id IS NOT NULL
  AND role_id NOT IN (SELECT id FROM (SELECT id FROM sys_role WHERE role_code IN ('SUPER_ADMIN', 'EVENT_OPERATOR', 'GRID_WORKER', 'PUBLIC')) t);

-- 5. 清理待裁撤角色的残余绑定、授权与角色记录
DELETE FROM sys_user_role
WHERE role_id IN (
    SELECT id FROM (
        SELECT id FROM sys_role
        WHERE role_code NOT IN ('SUPER_ADMIN', 'EVENT_OPERATOR', 'GRID_WORKER', 'PUBLIC')
    ) t);

DELETE FROM sys_role_permission
WHERE role_id IN (
    SELECT id FROM (
        SELECT id FROM sys_role
        WHERE role_code NOT IN ('SUPER_ADMIN', 'EVENT_OPERATOR', 'GRID_WORKER', 'PUBLIC')
    ) t);

DELETE FROM sys_role
WHERE role_code NOT IN ('SUPER_ADMIN', 'EVENT_OPERATOR', 'GRID_WORKER', 'PUBLIC');

-- 6. 新增“菜单管理”菜单权限
INSERT INTO sys_permission (permission_code, permission_name, permission_type, client_type, parent_id, path, sort_order, status, remark, created_at, updated_at)
SELECT 'web:menu:system-menus', '菜单管理', 'MENU', 'WEB', NULL, '/system-menus', 233, 'ACTIVE', 'Web菜单-系统设置', NOW(), NOW()
FROM (SELECT 1) seed
WHERE NOT EXISTS (SELECT 1 FROM (SELECT id FROM sys_permission WHERE permission_code = 'web:menu:system-menus') t);

-- 7. 超级管理员补齐全部 Web 菜单权限（含菜单管理；侧边栏以数据库菜单树为准，缺授权会丢菜单）
INSERT INTO sys_role_permission (role_id, permission_id, created_at, updated_at)
SELECT r.id, p.id, NOW(), NOW()
FROM sys_role r
JOIN sys_permission p
  ON p.client_type = 'WEB'
 AND p.permission_type = 'MENU'
 AND p.permission_code LIKE 'web:menu:%'
LEFT JOIN sys_role_permission rp ON rp.role_id = r.id AND rp.permission_id = p.id
WHERE r.role_code = 'SUPER_ADMIN'
  AND rp.id IS NULL;
