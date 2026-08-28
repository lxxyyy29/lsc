-- Add media center menu and API permissions under the drone catalog

-- Step 1: Add media center menu
INSERT INTO sys_permission (permission_code, permission_name, permission_type, client_type, parent_id, path, component, icon, sort_order, status, remark, created_at, updated_at)
SELECT 'menu:drone:media', '媒体中心', 'MENU', 'WEB', p.id, '/media/results', 'media/index', 'Picture', 50, 'ACTIVE', 'Drone media center menu', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM sys_permission p
WHERE p.permission_code = 'catalog:drone'
  AND NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'menu:drone:media');

-- Step 2: Add media API permissions
INSERT INTO sys_permission (permission_code, permission_name, permission_type, client_type, parent_id, path, component, icon, sort_order, status, remark, created_at, updated_at)
SELECT permission_code, permission_name, 'API', 'WEB', parent_id, path, NULL, NULL, sort_order, 'ACTIVE', remark, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM (
    SELECT 'api:drone:media:list' AS permission_code, '查询媒体文件夹列表' AS permission_name,
           (SELECT id FROM sys_permission WHERE permission_code = 'menu:drone:media') AS parent_id,
           '/api/drone/media/files' AS path, 1401 AS sort_order, 'Drone media folder list API' AS remark
    UNION ALL SELECT 'api:drone:media:files', '查询媒体文件列表',
           (SELECT id FROM sys_permission WHERE permission_code = 'menu:drone:media'),
           '/api/drone/media/files/{jobId}', 1402, 'Drone media files by job API'
) seeded_apis
WHERE parent_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = seeded_apis.permission_code);

-- Step 3: Grant all media permissions to SUPER_ADMIN
INSERT INTO sys_role_permission (role_id, permission_id, created_at, updated_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM sys_role r
JOIN sys_permission p ON p.permission_code IN (
    'menu:drone:media',
    'api:drone:media:list',
    'api:drone:media:files'
)
WHERE r.role_code = 'SUPER_ADMIN'
  AND NOT EXISTS (
      SELECT 1
      FROM sys_role_permission srp
      WHERE srp.role_id = r.id
        AND srp.permission_id = p.id
  );
