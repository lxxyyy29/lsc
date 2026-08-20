-- V102: 产品架构精简与上报归口
-- 1) 报修/居民上报归口事件闭环：回写关联事件 ID
-- 2) 组织人员组长划分：新增 leader_id
-- 3) 下线模块权限清理 + 台账 SAFETY 模板清理
-- 4) 删除已下线模块数据表（应急/安全/车辆/蚊媒/停车/健康监测）
-- 全部语句幂等，可重复执行

-- ── 1. biz_repair_request.event_id（幂等） ──
SET @has_col := (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'biz_repair_request' AND column_name = 'event_id');
SET @ddl := IF(@has_col = 0,
    'ALTER TABLE biz_repair_request ADD COLUMN event_id BIGINT NULL COMMENT ''关联事件ID''',
    'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ── 2. cmn_resident_report.event_id（幂等） ──
SET @has_col := (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'cmn_resident_report' AND column_name = 'event_id');
SET @ddl := IF(@has_col = 0,
    'ALTER TABLE cmn_resident_report ADD COLUMN event_id BIGINT NULL COMMENT ''关联事件ID''',
    'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ── 3. cmn_org_member.leader_id（幂等） ──
SET @has_col := (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'cmn_org_member' AND column_name = 'leader_id');
SET @ddl := IF(@has_col = 0,
    'ALTER TABLE cmn_org_member ADD COLUMN leader_id BIGINT NULL COMMENT ''所属组长 cmn_org_member.id''',
    'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ── 4. 清理已下线模块权限（保留 268 api:h5:notification:view） ──
-- 注意 fk_sys_permission_parent 自引用外键 RESTRICT：必须先删子行再删父行
DELETE FROM sys_role_permission WHERE permission_id IN
    (233, 242, 243, 258, 259, 260, 263, 264, 265, 266, 267, 269, 270, 287, 290, 291, 292, 294)
    OR permission_id IN (SELECT id FROM (SELECT id FROM sys_permission WHERE parent_id IN
        (233, 242, 243, 258, 259, 260, 263, 264, 265, 266, 267, 269, 270, 287, 290, 291, 292, 294)) AS sub);
-- 4.1 先删挂在待删权限下的子权限（如 242/243 挂在 233 下）
DELETE FROM sys_permission WHERE parent_id IN
    (233, 242, 243, 258, 259, 260, 263, 264, 265, 266, 267, 269, 270, 287, 290, 291, 292, 294);
-- 4.2 再删目标权限本身
DELETE FROM sys_permission WHERE id IN
    (233, 242, 243, 258, 259, 260, 263, 264, 265, 266, 267, 269, 270, 287, 290, 291, 292, 294);

-- ── 5. 台账模板清理 SAFETY ──
DELETE FROM sys_ledger_template WHERE template_type = 'SAFETY';

-- ── 6. 删除已下线模块数据表（无外键依赖，IF EXISTS 幂等） ──
DROP TABLE IF EXISTS biz_disinfection_record;
DROP TABLE IF EXISTS biz_emergency_receipt;
DROP TABLE IF EXISTS biz_emergency_dispatch;
DROP TABLE IF EXISTS biz_mosquito_device_data;
DROP TABLE IF EXISTS biz_mosquito_device;
DROP TABLE IF EXISTS biz_mosquito_site;
DROP TABLE IF EXISTS biz_safety_inspection;
DROP TABLE IF EXISTS biz_vehicle_track_record;
DROP TABLE IF EXISTS biz_parking_violation;
DROP TABLE IF EXISTS biz_parking_space;
DROP TABLE IF EXISTS biz_health_monitor;
