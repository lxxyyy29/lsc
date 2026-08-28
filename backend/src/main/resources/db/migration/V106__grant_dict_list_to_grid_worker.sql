-- V106: 字典读取权限补充授予
-- 创建事件弹窗的"上报来源"下拉由字典 event_report_source 驱动，
-- 网格员（GRID_WORKER）等角色打开创建事件时需要读取字典，补充授予 api:system:dict:list
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM sys_role r, sys_permission p
WHERE r.role_code = 'GRID_WORKER' AND p.permission_code = 'api:system:dict:list'
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_permission x
    WHERE x.role_id = r.id AND x.permission_id = p.id
  );
