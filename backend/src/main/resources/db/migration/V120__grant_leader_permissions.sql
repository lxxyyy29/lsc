-- =============================================================
-- V120 修复：网格组长 × 微信小程序(H5) 联动权限
-- 问题：api:h5:leader:pending / api:h5:leader:dispatch 权限从未入库，
--       导致 H5 工作台「组长工作台」入口不显示、/h5/leader/* 接口 403。
-- 修复：1) 幂等插入缺失权限  2) 授予 GRID_WORKER（老式组长所在角色）
--       与 GRID_LEADER（新建组长角色）对应的 H5 组长权限。
-- 全部语句幂等，可重复执行
-- =============================================================

-- ── 1. 插入缺失的组长权限（api:* 由 service 层校验，不入角色配置菜单树） ──
INSERT INTO sys_permission (permission_code, permission_name, permission_type, client_type, path, sort_order, status, remark, created_at, updated_at)
SELECT 'api:h5:leader:pending', 'H5组长待办事件', 'API', 'H5', '/api/h5/leader/pending-events', 238, 'ACTIVE', '组长工作台-待办事件', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'api:h5:leader:pending');

INSERT INTO sys_permission (permission_code, permission_name, permission_type, client_type, path, sort_order, status, remark, created_at, updated_at)
SELECT 'api:h5:leader:dispatch', 'H5组长派单', 'API', 'H5', '/api/h5/leader/events/{eventId}/dispatch', 239, 'ACTIVE', '组长工作台-派单', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'api:h5:leader:dispatch');

-- ── 2. 授予 GRID_WORKER：组长权限（现有网格长/组长所在角色，保持老式组长可用） ──
INSERT INTO sys_role_permission (role_id, permission_id, created_at, updated_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM sys_role r
JOIN sys_permission p ON p.permission_code IN (
    'api:h5:leader:pending',
    'api:h5:leader:dispatch'
)
WHERE r.role_code = 'GRID_WORKER'
  AND NOT EXISTS (
      SELECT 1 FROM sys_role_permission srp
      WHERE srp.role_id = r.id AND srp.permission_id = p.id
  );

-- ── 3. 授予 GRID_LEADER（新建组长）：H5 登录入口 + 工作台 + 工单 + 组长派单全套 ──
INSERT INTO sys_role_permission (role_id, permission_id, created_at, updated_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM sys_role r
JOIN sys_permission p ON p.permission_code IN (
    'menu:h5:workbench:view',
    'menu:h5:workorder:list',
    'menu:h5:message:view',
    'api:auth:h5:me',
    'api:h5:workbench:view',
    'api:h5:workorder:list',
    'api:h5:workorder:detail',
    'api:h5:workorder:handle',
    'api:h5:workorder:verify',
    'api:h5:leader:pending',
    'api:h5:leader:dispatch'
)
WHERE r.role_code = 'GRID_LEADER'
  AND NOT EXISTS (
      SELECT 1 FROM sys_role_permission srp
      WHERE srp.role_id = r.id AND srp.permission_id = p.id
  );