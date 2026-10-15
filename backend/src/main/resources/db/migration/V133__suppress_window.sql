-- V133: 时段抑制（夜间静默）规则 —— 后台可自助调整时段
--
-- 目标：落在可配置时段内上传的事件，不进入正常展示/派单/通知流程；
--       时段结束后（suppress_until 到点）自动恢复可见，数据不删除、可追溯、可回滚。
--
-- 幂等：建表 IF NOT EXISTS；列/配置/权限用 IGNORE 或条件判断。

-- ---------- 1. 规则表（支持多条、跨天、星期、动作组合、白名单范围）----------
CREATE TABLE IF NOT EXISTS sys_suppress_window (
  id              BIGINT       NOT NULL AUTO_INCREMENT,
  name            VARCHAR(64)  NOT NULL COMMENT '规则名称，如：夜间静默',
  start_time      TIME         NOT NULL COMMENT '开始时间',
  end_time        TIME         NOT NULL COMMENT '结束时间；小于开始时间表示跨天',
  weekdays        VARCHAR(20)  NOT NULL DEFAULT '1,2,3,4,5,6,7' COMMENT '生效星期(1=周一..7=周日)',
  action          VARCHAR(64)  NOT NULL DEFAULT 'HIDE' COMMENT 'HIDE,NO_DISPATCH,NO_NOTIFY 逗号分隔',
  scope_source    VARCHAR(255) NULL COMMENT '限定来源系统，逗号分隔，空=全部',
  scope_type      VARCHAR(255) NULL COMMENT '限定事件类型，逗号分隔，空=全部',
  exempt_urgency  VARCHAR(64)  NOT NULL DEFAULT 'RED' COMMENT '豁免的紧急等级，逗号分隔',
  enabled         TINYINT      NOT NULL DEFAULT 1 COMMENT '1启用 0停用',
  remark          VARCHAR(255) NULL,
  created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='时段抑制规则（夜间静默等）';

-- ---------- 2. biz_event 增列：记录抑制事实（不删数据，到点自动放行）----------
SET @ddl := (SELECT IF(COUNT(*) = 0,
  'ALTER TABLE biz_event ADD COLUMN suppress_until DATETIME NULL COMMENT ''抑制截止时间：该时间前不展示/不派单；NULL=未抑制''',
  'SELECT 1')
  FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'biz_event' AND COLUMN_NAME = 'suppress_until');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

SET @ddl := (SELECT IF(COUNT(*) = 0,
  'ALTER TABLE biz_event ADD COLUMN suppress_rule_id BIGINT NULL COMMENT ''命中的抑制规则ID''',
  'SELECT 1')
  FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'biz_event' AND COLUMN_NAME = 'suppress_rule_id');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

SET @ddl := (SELECT IF(COUNT(*) = 0,
  'ALTER TABLE biz_event ADD COLUMN suppress_manual TINYINT NOT NULL DEFAULT 0 COMMENT ''人工强制放出：1=不受抑制''',
  'SELECT 1')
  FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'biz_event' AND COLUMN_NAME = 'suppress_manual');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

-- 过滤索引：列表/大屏统一按 suppress_until 过滤
SET @ddl := (SELECT IF(COUNT(*) = 0,
  'CREATE INDEX idx_biz_event_suppress_until ON biz_event (suppress_until)',
  'SELECT 1')
  FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'biz_event' AND INDEX_NAME = 'idx_biz_event_suppress_until');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

-- ---------- 3. 全局配置项（应急一键关闭）----------
INSERT IGNORE INTO sys_config (config_key, config_value, remark, updated_at) VALUES
('suppress.enabled',  '1',                '时段抑制总开关：0=全局失效（应急用）', CURRENT_TIMESTAMP),
('suppress.timezone', 'Asia/Shanghai',    '时段判定时区',                        CURRENT_TIMESTAMP);

-- ---------- 4. 预置一条默认规则（22:00-07:00 跨天，仅 HIDE，豁免 RED 与 12345）----------
INSERT INTO sys_suppress_window (name, start_time, end_time, weekdays, action, scope_source, scope_type, exempt_urgency, enabled, remark)
SELECT '夜间静默', '22:00:00', '07:00:00', '1,2,3,4,5,6,7', 'HIDE', '12345', NULL, 'RED', 1, '默认预置：夜间仅不展示，仍正常派单/通知；紧急(RED)与12345来源豁免'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_suppress_window);

-- ---------- 5. 权限行（菜单 + API）----------
INSERT IGNORE INTO sys_permission (permission_code, permission_name, permission_type, client_type, parent_id, path, component, icon, sort_order, status, remark, created_at, updated_at) VALUES
('web:menu:system-suppress',       '时段抑制菜单',       'MENU', 'WEB', NULL, '/system-suppress', NULL, NULL, 190, 'ACTIVE', 'Suppress window menu',   CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('api:system:suppress:list',       '查询时段抑制规则',   'API',  'WEB', NULL, '/api/system/suppress-windows',         NULL, NULL, 401, 'ACTIVE', 'Suppress list API',   CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('api:system:suppress:create',     '新增时段抑制规则',   'API',  'WEB', NULL, '/api/system/suppress-windows',         NULL, NULL, 402, 'ACTIVE', 'Suppress create API', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('api:system:suppress:update',     '修改时段抑制规则',   'API',  'WEB', NULL, '/api/system/suppress-windows/{id}',    NULL, NULL, 403, 'ACTIVE', 'Suppress update API', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('api:system:suppress:delete',     '删除时段抑制规则',   'API',  'WEB', NULL, '/api/system/suppress-windows/{id}',    NULL, NULL, 404, 'ACTIVE', 'Suppress delete API', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('api:events:suppress-manual',     '事件强制放出',       'API',  'WEB', NULL, '/api/events/{id}/suppress-manual',     NULL, NULL, 405, 'ACTIVE', 'Event suppress manual', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- 授予超管与管理员（幂等）
INSERT INTO sys_role_permission (role_id, permission_id, created_at, updated_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM sys_role r
JOIN sys_permission p ON p.permission_code IN (
  'web:menu:system-suppress',
  'api:system:suppress:list', 'api:system:suppress:create',
  'api:system:suppress:update', 'api:system:suppress:delete',
  'api:events:suppress-manual')
LEFT JOIN sys_role_permission rp ON rp.role_id = r.id AND rp.permission_id = p.id
WHERE r.role_code IN ('SUPER_ADMIN', 'EVENT_OPERATOR') AND rp.role_id IS NULL;
