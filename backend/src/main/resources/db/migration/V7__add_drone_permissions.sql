INSERT INTO sys_permission (permission_code, permission_name, permission_type, client_type, parent_id, path, component, icon, sort_order, status, remark, created_at, updated_at)
SELECT 'catalog:drone', '无人机管理', 'CATALOG', 'WEB', NULL, '/drone', NULL, 'Promotion', 910, 'ACTIVE', 'Drone management catalog', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'catalog:drone');

INSERT INTO sys_permission (permission_code, permission_name, permission_type, client_type, parent_id, path, component, icon, sort_order, status, remark, created_at, updated_at)
SELECT 'menu:drone:device', '设备管理', 'MENU', 'WEB', p.id, '/drone/devices', 'drone/devices/index', 'VideoCamera', 10, 'ACTIVE', 'Drone device management menu', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM sys_permission p
WHERE p.permission_code = 'catalog:drone'
  AND NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'menu:drone:device');

INSERT INTO sys_permission (permission_code, permission_name, permission_type, client_type, parent_id, path, component, icon, sort_order, status, remark, created_at, updated_at)
SELECT 'menu:drone:job', '飞行任务', 'MENU', 'WEB', p.id, '/drone/jobs', 'drone/jobs/index', 'Operation', 20, 'ACTIVE', 'Drone job management menu', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM sys_permission p
WHERE p.permission_code = 'catalog:drone'
  AND NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'menu:drone:job');

INSERT INTO sys_permission (permission_code, permission_name, permission_type, client_type, parent_id, path, component, icon, sort_order, status, remark, created_at, updated_at)
SELECT 'menu:drone:ai-model', 'AI模型管理', 'MENU', 'WEB', p.id, '/drone/ai-models', 'drone/ai-models/index', 'Cpu', 30, 'ACTIVE', 'Drone AI model management menu', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM sys_permission p
WHERE p.permission_code = 'catalog:drone'
  AND NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'menu:drone:ai-model');

INSERT INTO sys_permission (permission_code, permission_name, permission_type, client_type, parent_id, path, component, icon, sort_order, status, remark, created_at, updated_at)
SELECT 'menu:drone:monitor', '实时监控', 'MENU', 'WEB', p.id, '/drone/monitor', 'drone/monitor/index', 'Monitor', 40, 'ACTIVE', 'Drone monitor menu', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM sys_permission p
WHERE p.permission_code = 'catalog:drone'
  AND NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'menu:drone:monitor');

INSERT INTO sys_permission (permission_code, permission_name, permission_type, client_type, parent_id, path, component, icon, sort_order, status, remark, created_at, updated_at)
SELECT permission_code, permission_name, 'API', 'WEB', parent_id, path, NULL, NULL, sort_order, 'ACTIVE', remark, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM (
    SELECT 'api:drone:workspace:list' AS permission_code, '查询工作空间列表' AS permission_name,
           (SELECT id FROM sys_permission WHERE permission_code = 'menu:drone:device') AS parent_id,
           '/api/drone/workspaces' AS path, 1001 AS sort_order, 'Drone workspace list API' AS remark
    UNION ALL SELECT 'api:drone:device:list', '查询设备列表', (SELECT id FROM sys_permission WHERE permission_code = 'menu:drone:device'), '/api/drone/devices', 1002, 'Drone device list API'
    UNION ALL SELECT 'api:drone:wayline:list', '查询航线列表', (SELECT id FROM sys_permission WHERE permission_code = 'menu:drone:job'), '/api/drone/waylines', 1101, 'Drone wayline list API'
    UNION ALL SELECT 'api:drone:wayline:points', '查询航点列表', (SELECT id FROM sys_permission WHERE permission_code = 'menu:drone:job'), '/api/drone/waylines/{id}/points', 1102, 'Drone wayline points API'
    UNION ALL SELECT 'api:drone:job:list', '查询飞行任务列表', (SELECT id FROM sys_permission WHERE permission_code = 'menu:drone:job'), '/api/drone/jobs', 1103, 'Drone job list API'
    UNION ALL SELECT 'api:drone:job:create', '创建飞行任务', (SELECT id FROM sys_permission WHERE permission_code = 'menu:drone:job'), '/api/drone/jobs', 1104, 'Drone job create API'
    UNION ALL SELECT 'api:drone:job:pause-resume', '挂起恢复飞行任务', (SELECT id FROM sys_permission WHERE permission_code = 'menu:drone:job'), '/api/drone/jobs/{jobId}/pause-resume', 1105, 'Drone job pause resume API'
    UNION ALL SELECT 'api:drone:job:return-home', '立即返航', (SELECT id FROM sys_permission WHERE permission_code = 'menu:drone:job'), '/api/drone/jobs/return-home', 1106, 'Drone return home API'
    UNION ALL SELECT 'api:drone:ai-model:list', '查询AI模型列表', (SELECT id FROM sys_permission WHERE permission_code = 'menu:drone:ai-model'), '/api/drone/ai-models', 1201, 'Drone AI model list API'
    UNION ALL SELECT 'api:drone:speaker:file:list', '查询喊话文件列表', (SELECT id FROM sys_permission WHERE permission_code = 'menu:drone:monitor'), '/api/drone/speaker/files', 1301, 'Drone speaker file list API'
    UNION ALL SELECT 'api:drone:speaker:file:upload', '上传喊话文件', (SELECT id FROM sys_permission WHERE permission_code = 'menu:drone:monitor'), '/api/drone/speaker/files', 1302, 'Drone speaker file upload API'
    UNION ALL SELECT 'api:drone:speaker:file:delete', '删除喊话文件', (SELECT id FROM sys_permission WHERE permission_code = 'menu:drone:monitor'), '/api/drone/speaker/files/{id}', 1303, 'Drone speaker file delete API'
    UNION ALL SELECT 'api:drone:speaker:play', '播放喊话文件', (SELECT id FROM sys_permission WHERE permission_code = 'menu:drone:monitor'), '/api/drone/speaker/{deviceSn}/play', 1304, 'Drone speaker play API'
    UNION ALL SELECT 'api:drone:speaker:stop', '停止喊话', (SELECT id FROM sys_permission WHERE permission_code = 'menu:drone:monitor'), '/api/drone/speaker/{deviceSn}/stop', 1305, 'Drone speaker stop API'
    UNION ALL SELECT 'api:drone:speaker:volume', '设置喊话音量', (SELECT id FROM sys_permission WHERE permission_code = 'menu:drone:monitor'), '/api/drone/speaker/{deviceSn}/volume', 1306, 'Drone speaker volume API'
    UNION ALL SELECT 'api:drone:payload:camera-mode', '切换相机模式', (SELECT id FROM sys_permission WHERE permission_code = 'menu:drone:monitor'), '/api/drone/devices/{deviceSn}/camera/mode', 1307, 'Drone camera mode API'
    UNION ALL SELECT 'api:drone:payload:record-start', '开始录像', (SELECT id FROM sys_permission WHERE permission_code = 'menu:drone:monitor'), '/api/drone/devices/{deviceSn}/camera/record-start', 1308, 'Drone record start API'
    UNION ALL SELECT 'api:drone:payload:record-stop', '停止录像', (SELECT id FROM sys_permission WHERE permission_code = 'menu:drone:monitor'), '/api/drone/devices/{deviceSn}/camera/record-stop', 1309, 'Drone record stop API'
    UNION ALL SELECT 'api:drone:ws:connect', '连接无人机实时通道', (SELECT id FROM sys_permission WHERE permission_code = 'menu:drone:monitor'), '/api/ws/drone', 1310, 'Drone websocket connect API'
) seeded_apis
WHERE parent_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = seeded_apis.permission_code);

INSERT INTO sys_role_permission (role_id, permission_id, created_at, updated_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM sys_role r
JOIN sys_permission p ON p.permission_code IN (
    'catalog:drone',
    'menu:drone:device',
    'menu:drone:job',
    'menu:drone:ai-model',
    'menu:drone:monitor',
    'api:drone:workspace:list',
    'api:drone:device:list',
    'api:drone:wayline:list',
    'api:drone:wayline:points',
    'api:drone:job:list',
    'api:drone:job:create',
    'api:drone:job:pause-resume',
    'api:drone:job:return-home',
    'api:drone:ai-model:list',
    'api:drone:speaker:file:list',
    'api:drone:speaker:file:upload',
    'api:drone:speaker:file:delete',
    'api:drone:speaker:play',
    'api:drone:speaker:stop',
    'api:drone:speaker:volume',
    'api:drone:payload:camera-mode',
    'api:drone:payload:record-start',
    'api:drone:payload:record-stop',
    'api:drone:ws:connect'
)
WHERE r.role_code = 'SUPER_ADMIN'
  AND NOT EXISTS (
      SELECT 1
      FROM sys_role_permission srp
      WHERE srp.role_id = r.id
        AND srp.permission_id = p.id
  );
