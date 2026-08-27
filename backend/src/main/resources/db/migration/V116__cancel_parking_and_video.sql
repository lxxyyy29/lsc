-- ============================================================
-- V116: 取消 停车管理 / 视频轮巡 模块（需求11）
-- 说明：
--   1. 停用 停车管理 菜单与 API 权限（status -> INACTIVE）
--   2. 停用 视频轮巡 菜单权限 + 遗留 api:video:upload/list/delete 权限
--   3. 解除相关角色绑定
--   保留：api:video:camera:list / api:video:camera:manage
--        （无人机管理 DronesView 外部视频源依赖，不在此次取消范围）
-- ============================================================

-- 1. 停用停车管理相关权限
UPDATE sys_permission
SET status = 'INACTIVE', updated_at = CURRENT_TIMESTAMP
WHERE permission_code IN (
    'menu:parking',
    'web:menu:parking',
    'api:parking:view',
    'api:parking:manage'
);

-- 2. 停用视频轮巡相关权限（菜单 + 遗留视频 API；保留 camera 权限供无人机）
UPDATE sys_permission
SET status = 'INACTIVE', updated_at = CURRENT_TIMESTAMP
WHERE permission_code IN (
    'web:menu:video',
    'api:video:upload',
    'api:video:list',
    'api:video:delete'
);

-- 3. 解除上述权限的角色绑定
DELETE FROM sys_role_permission
WHERE permission_id IN (
    SELECT id FROM sys_permission
    WHERE permission_code IN (
        'menu:parking',
        'web:menu:parking',
        'api:parking:view',
        'api:parking:manage',
        'web:menu:video',
        'api:video:upload',
        'api:video:list',
        'api:video:delete'
    )
);
