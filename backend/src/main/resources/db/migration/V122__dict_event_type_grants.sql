-- V122: 字典读取权限补齐 + event_type 字典状态修正
-- 背景：V121 引入事件类型字典 event_type 后，H5 端（网格员、组长、居民）的事件上报表单也要读取该字典。
--   1) V103 仅授予 SUPER_ADMIN / EVENT_OPERATOR，V106 补了 GRID_WORKER，
--      居民（PUBLIC）与网格组长（GRID_LEADER，V119 引入）仍缺 api:system:dict:list，读取会被拒。
--   2) V121 使用 INSERT IGNORE，若 event_type 已存在且为 DISABLED，不会把状态改回 ACTIVE，
--      导致业务表单读不到字典项（只能走前端兜底列表）。此处显式修正启用状态。

-- ---------- 1. 补齐字典读取权限 ----------
INSERT INTO sys_role_permission (role_id, permission_id, created_at, updated_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM sys_role r
JOIN sys_permission p ON p.permission_code = 'api:system:dict:list'
WHERE r.role_code IN ('PUBLIC', 'GRID_LEADER')
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_permission x
    WHERE x.role_id = r.id AND x.permission_id = p.id
  );

-- ---------- 2. 修正 event_type 字典及字典项状态 ----------
UPDATE sys_dict_type
SET status = 'ACTIVE', updated_at = CURRENT_TIMESTAMP
WHERE dict_code = 'event_type' AND status <> 'ACTIVE';

UPDATE sys_dict_item
SET status = 'ACTIVE', updated_at = CURRENT_TIMESTAMP
WHERE dict_code = 'event_type' AND status <> 'ACTIVE';
