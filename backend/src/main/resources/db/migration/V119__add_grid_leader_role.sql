-- =============================================================
-- V119 新增内置角色：GRID_LEADER 网格组长
-- 网格组长也作为系统角色的一栏，可在"系统设置-角色管理"中编辑
-- 名称/备注/权限；该角色用户承担组长派单与组长审核职责
-- =============================================================

-- 幂等写入网格组长角色（已存在则跳过）
INSERT INTO sys_role (role_code, role_name, status, remark, created_at, updated_at)
SELECT 'GRID_LEADER', '网格组长', 'ACTIVE', '内置角色：负责所辖网格的派单与审核', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_role WHERE role_code = 'GRID_LEADER');