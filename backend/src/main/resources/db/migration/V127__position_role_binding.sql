-- =============================================================
-- V127：岗位即角色 —— 按组织人员「岗位」对齐账号「角色」，并收敛组长派单权限
--
-- 背景（两个真实缺陷）：
-- 1) 岗位与角色此前没有完整的绑定通路：
--    - 「新增组织人员」不调用角色同步，只有「编辑」才调用；
--    - 新建系统账号后不回写 cmn_org_member.sys_user_id，
--      导致之后编辑岗位时因 sys_user_id 为空而整段跳过角色同步。
--    表现：岗位写着「网格组长」，账号角色却是「网格员」，甚至根本没有账号。
-- 2) 组长派单权限 api:h5:leader:* 此前同时授予了 GRID_WORKER；
--    而实际「组长」多以 member_type='GRID_WORKER' + position 含「组长/网格长」记录，
--    一旦回收 GRID_WORKER 的授权，组长就看不到「组长工作台」入口 —— 口径脆弱。
--
-- 本迁移只做数据对齐（幂等，可重复执行；不新增表、不改结构）：
-- 1) 岗位含 组长/网格长 或 member_type='LEADER' → 账号角色 = GRID_LEADER
-- 2) 普通网格员（且未在任何网格担任组长） → 账号角色 = GRID_WORKER
-- 3) 「组长工作台」相关权限收敛为只授予 GRID_LEADER
-- =============================================================

-- ---------- 1) 组长 / 网格长 → GRID_LEADER ----------
DELETE ur FROM sys_user_role ur
JOIN cmn_org_member m ON m.sys_user_id = ur.user_id
WHERE m.status = 'ACTIVE'
  AND (m.member_type = 'LEADER' OR m.position LIKE '%组长%' OR m.position LIKE '%网格长%');

INSERT INTO sys_user_role (user_id, role_id, created_at, updated_at)
SELECT DISTINCT m.sys_user_id, r.id, NOW(), NOW()
FROM cmn_org_member m
JOIN sys_role r ON r.role_code = 'GRID_LEADER'
WHERE m.status = 'ACTIVE'
  AND m.sys_user_id IS NOT NULL
  AND (m.member_type = 'LEADER' OR m.position LIKE '%组长%' OR m.position LIKE '%网格长%');

-- ---------- 2) 普通网格员 → GRID_WORKER（排除在任一网格担任组长的人，避免把组长降级） ----------
DELETE ur FROM sys_user_role ur
JOIN cmn_org_member m ON m.sys_user_id = ur.user_id
WHERE m.status = 'ACTIVE'
  AND m.member_type = 'GRID_WORKER'
  AND NOT (m.position LIKE '%组长%' OR m.position LIKE '%网格长%')
  AND NOT EXISTS (
      SELECT 1 FROM cmn_org_member l
      WHERE l.status = 'ACTIVE' AND l.sys_user_id = m.sys_user_id
        AND (l.member_type = 'LEADER' OR l.position LIKE '%组长%' OR l.position LIKE '%网格长%')
  );

INSERT INTO sys_user_role (user_id, role_id, created_at, updated_at)
SELECT DISTINCT m.sys_user_id, r.id, NOW(), NOW()
FROM cmn_org_member m
JOIN sys_role r ON r.role_code = 'GRID_WORKER'
WHERE m.status = 'ACTIVE'
  AND m.sys_user_id IS NOT NULL
  AND m.member_type = 'GRID_WORKER'
  AND NOT (m.position LIKE '%组长%' OR m.position LIKE '%网格长%')
  AND NOT EXISTS (
      SELECT 1 FROM cmn_org_member l
      WHERE l.status = 'ACTIVE' AND l.sys_user_id = m.sys_user_id
        AND (l.member_type = 'LEADER' OR l.position LIKE '%组长%' OR l.position LIKE '%网格长%')
  );

-- ---------- 3) 主角色同步：sys_user.role_id 保存 sys_user_role 中的角色 ----------
UPDATE sys_user u
JOIN sys_user_role ur ON ur.user_id = u.id
JOIN sys_role r ON r.id = ur.role_id
SET u.role_id = r.id, u.updated_at = NOW()
WHERE r.role_code IN ('GRID_LEADER', 'GRID_WORKER');

-- ---------- 4) 「组长工作台」权限收敛为只授予 GRID_LEADER ----------
-- 岗位即角色后，组长已具备 GRID_LEADER 角色，无需再借道 GRID_WORKER 授权
DELETE rp FROM sys_role_permission rp
JOIN sys_role r ON r.id = rp.role_id
JOIN sys_permission p ON p.id = rp.permission_id
WHERE r.role_code = 'GRID_WORKER'
  AND p.permission_code IN ('api:h5:leader:pending', 'api:h5:leader:dispatch');
