-- V100: Web 管理端"系统设置"模块——菜单权限记录 + 角色授权
-- 1) 新增 web 侧菜单权限（web:menu:* 前缀，与历史 menu:* 遗留记录区分），用于左侧导航按权限过滤
-- 2) 新增角色删除 API 权限
-- 3) SUPER_ADMIN 授全部；EVENT_OPERATOR 授业务菜单（不含审计日志/系统设置）

-- ---------- 1. 菜单权限记录 ----------
INSERT IGNORE INTO sys_permission (permission_code, permission_name, permission_type, client_type, path, sort_order, status, remark, created_at, updated_at) VALUES
('web:menu:dashboard',       '全域态势看板',   'MENU', 'WEB', '/',                201, 'ACTIVE', 'Web菜单-首页概览', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('web:menu:big-screen',      '综合监管大屏',   'MENU', 'WEB', '/big-screen',      202, 'ACTIVE', 'Web菜单-首页概览', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('web:menu:events',          '事件闭环处置',   'MENU', 'WEB', '/events',          203, 'ACTIVE', 'Web菜单-事件工单', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('web:menu:work-orders',     '工单中心',       'MENU', 'WEB', '/work-orders',     204, 'ACTIVE', 'Web菜单-事件工单', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('web:menu:audits',          '审核中心',       'MENU', 'WEB', '/audits',          205, 'ACTIVE', 'Web菜单-事件工单', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('web:menu:dispatch-rules',  '智能派单规则',   'MENU', 'WEB', '/dispatch-rules',  206, 'ACTIVE', 'Web菜单-事件工单', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('web:menu:gis',             'GIS网格可视化',  'MENU', 'WEB', '/gis',             207, 'ACTIVE', 'Web菜单-网格治理', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('web:menu:grid-manage',     '网格管理',       'MENU', 'WEB', '/grid-manage',     208, 'ACTIVE', 'Web菜单-网格治理', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('web:menu:biz-areas',       '辖区管理',       'MENU', 'WEB', '/biz-areas',       209, 'ACTIVE', 'Web菜单-网格治理', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('web:menu:org-members',     '组织人员',       'MENU', 'WEB', '/org-members',     210, 'ACTIVE', 'Web菜单-网格治理', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('web:menu:population',      '实有人口库',     'MENU', 'WEB', '/population',      211, 'ACTIVE', 'Web菜单-基础台账', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('web:menu:buildings',       '房屋/出租屋库',  'MENU', 'WEB', '/buildings',       212, 'ACTIVE', 'Web菜单-基础台账', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('web:menu:places',          '场所资源库',     'MENU', 'WEB', '/places',          213, 'ACTIVE', 'Web菜单-基础台账', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('web:menu:ledger',          '场所台账',       'MENU', 'WEB', '/ledger',          214, 'ACTIVE', 'Web菜单-基础台账', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('web:menu:resident-reports','居民上报',       'MENU', 'WEB', '/resident-reports',215, 'ACTIVE', 'Web菜单-居民服务', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('web:menu:repairs',         '报修管理',       'MENU', 'WEB', '/repairs',         216, 'ACTIVE', 'Web菜单-居民服务', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('web:menu:policy-resources','政策资源',       'MENU', 'WEB', '/policy-resources',217, 'ACTIVE', 'Web菜单-居民服务', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('web:menu:patrol',          '网格巡查',       'MENU', 'WEB', '/patrol',          218, 'ACTIVE', 'Web菜单-巡查防控', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('web:menu:emergency',       '应急调度',       'MENU', 'WEB', '/emergency',       219, 'ACTIVE', 'Web菜单-巡查防控', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('web:menu:mosquito',        '爱卫蚊媒',       'MENU', 'WEB', '/mosquito',        220, 'ACTIVE', 'Web菜单-巡查防控', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('web:menu:safety',          '安全防控',       'MENU', 'WEB', '/safety',          221, 'ACTIVE', 'Web菜单-巡查防控', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('web:menu:parking',         '停车管理',       'MENU', 'WEB', '/parking',         222, 'ACTIVE', 'Web菜单-巡查防控', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('web:menu:vehicle-track',   '车辆轨迹',       'MENU', 'WEB', '/vehicle-track',   223, 'ACTIVE', 'Web菜单-巡查防控', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('web:menu:party',           '智慧党建',       'MENU', 'WEB', '/party',           224, 'ACTIVE', 'Web菜单-智慧应用', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('web:menu:drones',          '无人机管理',     'MENU', 'WEB', '/drones',          225, 'ACTIVE', 'Web菜单-智慧应用', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('web:menu:video',           '视频轮巡',       'MENU', 'WEB', '/video',           226, 'ACTIVE', 'Web菜单-智慧应用', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('web:menu:reports',         '数据报表',       'MENU', 'WEB', '/reports',         227, 'ACTIVE', 'Web菜单-数据决策', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('web:menu:assessment',      '考核研判',       'MENU', 'WEB', '/assessment',      228, 'ACTIVE', 'Web菜单-数据决策', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('web:menu:trend-alerts',    '趋势预判预警',   'MENU', 'WEB', '/trend-alerts',    229, 'ACTIVE', 'Web菜单-数据决策', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('web:menu:audit-logs',      '审计日志',       'MENU', 'WEB', '/audit-logs',      230, 'ACTIVE', 'Web菜单-数据决策（仅超管）', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('web:menu:system-roles',    '角色管理',       'MENU', 'WEB', '/system-roles',    231, 'ACTIVE', 'Web菜单-系统设置（仅超管）', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('web:menu:system-users',    '账号管理',       'MENU', 'WEB', '/system-users',    232, 'ACTIVE', 'Web菜单-系统设置（仅超管）', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- ---------- 2. 角色删除 API 权限 ----------
INSERT IGNORE INTO sys_permission (permission_code, permission_name, permission_type, client_type, path, sort_order, status, remark, created_at, updated_at)
VALUES ('api:system:role:delete', '删除角色', 'API', 'WEB', '/api/system/roles/{id}', 133, 'ACTIVE', 'Role delete permission', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- ---------- 3. SUPER_ADMIN 授全部新权限 ----------
INSERT IGNORE INTO sys_role_permission (role_id, permission_id, created_at, updated_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM sys_role r
JOIN sys_permission p ON p.permission_code LIKE 'web:menu:%' OR p.permission_code = 'api:system:role:delete'
WHERE r.role_code = 'SUPER_ADMIN';

-- ---------- 4. EVENT_OPERATOR 授业务菜单（不含审计日志与系统设置） ----------
INSERT IGNORE INTO sys_role_permission (role_id, permission_id, created_at, updated_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM sys_role r
JOIN sys_permission p ON p.permission_code LIKE 'web:menu:%'
WHERE r.role_code = 'EVENT_OPERATOR'
  AND p.permission_code NOT IN ('web:menu:audit-logs', 'web:menu:system-roles', 'web:menu:system-users');
