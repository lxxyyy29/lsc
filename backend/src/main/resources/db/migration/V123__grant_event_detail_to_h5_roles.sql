-- V123: 网格员 / 网格组长补授「事件详情」读取权限
-- 背景：H5 网格员处置工单时，工单详情页需读取关联事件详情（现场照片 / 坐标 / 证据），
--   用于「告警现场」展示与导航。此前 api:event:detail 仅授 WEB 角色（V5/V67），
--   H5 处置链路 403（AUTH_PERMISSION_DENIED: api:event:detail），导致该区块永远隐藏。
INSERT INTO sys_role_permission (role_id, permission_id, created_at, updated_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM sys_role r
JOIN sys_permission p ON p.permission_code = 'api:event:detail'
WHERE r.role_code IN ('GRID_WORKER', 'GRID_LEADER')
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_permission x
    WHERE x.role_id = r.id AND x.permission_id = p.id
  );
