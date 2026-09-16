-- =============================================================
-- V125 修复：Web 端组长派单权限缺失
--
-- 问题：api:leader:pending / api:leader:dispatch 只存在于 Java 常量
--       （PermissionCodes.API_LEADER_*，对应 WorkOrderController 的
--        /work-orders/leader/* 三个 Web 端点），从未插入 sys_permission，
--       也未授予任何角色。而 PermissionGuard.has() 无超管豁免，只是纯
--       权限码集合包含判断，因此连 admin(SUPER_ADMIN) 打开「事件闭环处置
--       -组长派单」也会 403「当前用户没有所需权限: api:leader:dispatch」。
--       （V120 修过同类的 H5 版权限，Web 版这两条被漏掉了）
--
-- 修复：1) 幂等插入这两个 Web 权限
--       2) 授予 SUPER_ADMIN（超级管理员）与 EVENT_OPERATOR（管理员）
--       注: 网格员/网格组长的组长派单走 H5 端，用 api:h5:leader:dispatch，
--           已在 V120 授予 GRID_WORKER / GRID_LEADER，此处不重复授予。
-- 全部语句幂等，可重复执行
-- =============================================================

-- ── 1. 插入缺失的 Web 端组长权限 ──
INSERT INTO sys_permission (permission_code, permission_name, permission_type, client_type, path, sort_order, status, remark, created_at, updated_at)
SELECT 'api:leader:pending', 'Web组长待办事件', 'API', 'WEB', '/api/work-orders/leader/pending-events', 240, 'ACTIVE', '事件闭环处置-组长待办事件', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'api:leader:pending');

INSERT INTO sys_permission (permission_code, permission_name, permission_type, client_type, path, sort_order, status, remark, created_at, updated_at)
SELECT 'api:leader:dispatch', 'Web组长派单', 'API', 'WEB', '/api/work-orders/leader/events/{eventId}/dispatch', 241, 'ACTIVE', '事件闭环处置-组长派单', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'api:leader:dispatch');

-- ── 2. 授予超管与事件运营管理员 ──
INSERT INTO sys_role_permission (role_id, permission_id, created_at, updated_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM sys_role r
JOIN sys_permission p ON p.permission_code IN (
    'api:leader:pending',
    'api:leader:dispatch'
)
WHERE r.role_code IN ('SUPER_ADMIN', 'EVENT_OPERATOR')
  AND NOT EXISTS (
      SELECT 1 FROM sys_role_permission srp
      WHERE srp.role_id = r.id AND srp.permission_id = p.id
  );
