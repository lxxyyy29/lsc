-- Ensure roles that can enter event pages also have the supporting event APIs.
-- Some existing databases have menu:event:list assigned without api:event:list,
-- causing /api/events to fail with AUTH_PERMISSION_DENIED.

-- Keep SUPER_ADMIN complete for environments where earlier grants were missed.
INSERT INTO sys_role_permission (role_id, permission_id, created_at, updated_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM sys_role r
CROSS JOIN sys_permission p
WHERE r.role_code = 'SUPER_ADMIN'
  AND p.status = 'ACTIVE'
  AND NOT EXISTS (
      SELECT 1
      FROM sys_role_permission srp
      WHERE srp.role_id = r.id
        AND srp.permission_id = p.id
  );

-- Event list page needs list/detail permissions whenever the event menu is visible.
INSERT INTO sys_role_permission (role_id, permission_id, created_at, updated_at)
SELECT DISTINCT menu_grants.role_id, api_permissions.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM sys_role_permission menu_grants
JOIN sys_permission event_menu
  ON event_menu.id = menu_grants.permission_id
 AND event_menu.permission_code = 'menu:event:list'
JOIN sys_permission api_permissions
  ON api_permissions.permission_code IN ('api:event:list', 'api:event:detail')
WHERE NOT EXISTS (
    SELECT 1
    FROM sys_role_permission existing
    WHERE existing.role_id = menu_grants.role_id
      AND existing.permission_id = api_permissions.id
);

-- Dashboard shows recent events, so dashboard-only roles also need the event list API.
INSERT INTO sys_role_permission (role_id, permission_id, created_at, updated_at)
SELECT DISTINCT menu_grants.role_id, api_permissions.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM sys_role_permission menu_grants
JOIN sys_permission dashboard_menu
  ON dashboard_menu.id = menu_grants.permission_id
 AND dashboard_menu.permission_code = 'menu:dashboard:view'
JOIN sys_permission api_permissions
  ON api_permissions.permission_code = 'api:event:list'
WHERE NOT EXISTS (
    SELECT 1
    FROM sys_role_permission existing
    WHERE existing.role_id = menu_grants.role_id
      AND existing.permission_id = api_permissions.id
);
