-- 事件专员（EVENT_OPERATOR）角色补发 H5 端接口权限
-- 背景：群众/事件专员账号（如 yonghu）注册时绑定 EVENT_OPERATOR 角色，
-- 该角色仅有 H5 菜单权限（menu:h5:*），缺少对应接口权限（api:h5:*），
-- 导致 H5 端工作台、工单列表、会话恢复（/auth/me）全部 403。
-- 本次只补查看类权限（工作台/工单列表/工单详情/当前用户），
-- 不授予处理类权限（accept/arrive/handle/verify），保持居民最小权限。

INSERT IGNORE INTO sys_role_permission (role_id, permission_id, created_at, updated_at)
SELECT r.id, p.id, NOW(), NOW()
FROM sys_role r
JOIN sys_permission p
  ON p.permission_code IN (
      'api:auth:h5:me',
      'api:h5:workbench:view',
      'api:h5:workorder:list',
      'api:h5:workorder:detail'
  )
WHERE r.role_code = 'EVENT_OPERATOR';
