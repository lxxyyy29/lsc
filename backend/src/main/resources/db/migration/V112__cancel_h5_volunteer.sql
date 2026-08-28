-- ============================================================
-- V112: 取消 H5 志愿服务入口（用户需求 12.2「志愿服务取消」）
-- 说明：
--   1. 停用 H5 志愿服务相关菜单/接口权限（status -> INACTIVE）
--   2. 解除超级管理员/管理员/网格员对这些权限的绑定
--   保留权限记录便于追溯，不物理删除
-- ============================================================

-- 1. 停用 H5 志愿服务权限
UPDATE sys_permission
SET status = 'INACTIVE', updated_at = CURRENT_TIMESTAMP
WHERE permission_code IN (
    'menu:h5:volunteer:view',
    'api:h5:volunteer:list',
    'api:h5:volunteer:signup',
    'api:h5:volunteer:points'
);

-- 2. 解除角色绑定
DELETE FROM sys_role_permission
WHERE permission_id IN (
    SELECT id FROM sys_permission
    WHERE permission_code IN (
        'menu:h5:volunteer:view',
        'api:h5:volunteer:list',
        'api:h5:volunteer:signup',
        'api:h5:volunteer:points'
    )
);
