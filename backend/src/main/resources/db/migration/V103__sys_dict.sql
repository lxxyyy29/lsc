-- V103: 系统字典管理
-- 1) sys_dict_type 字典类型 + sys_dict_item 字典项
-- 2) 种子数据：事件上报来源 event_report_source（事件创建表单下拉数据源）
-- 3) 菜单权限 web:menu:system-dicts + API 权限 list/manage；
--    SUPER_ADMIN 授全部，EVENT_OPERATOR 授 list（创建事件表单需读取上报来源字典）

-- ---------- 1. 字典表 ----------
CREATE TABLE IF NOT EXISTS sys_dict_type (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    dict_code VARCHAR(64) NOT NULL COMMENT '字典编码，如 event_report_source',
    dict_name VARCHAR(128) NOT NULL COMMENT '字典名称',
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/DISABLED',
    remark VARCHAR(255) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_sys_dict_type_code (dict_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统字典类型';

CREATE TABLE IF NOT EXISTS sys_dict_item (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    dict_code VARCHAR(64) NOT NULL COMMENT '所属字典编码',
    item_value VARCHAR(64) NOT NULL COMMENT '字典项值（存入业务字段）',
    item_label VARCHAR(128) NOT NULL COMMENT '字典项显示名',
    sort_order INT NOT NULL DEFAULT 0,
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/DISABLED',
    remark VARCHAR(255) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_sys_dict_item (dict_code, item_value),
    KEY idx_sys_dict_item_code (dict_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统字典项';

-- ---------- 2. 种子数据：事件上报来源 ----------
INSERT IGNORE INTO sys_dict_type (dict_code, dict_name, status, remark) VALUES
('event_report_source', '事件上报来源', 'ACTIVE', '事件创建表单-上报来源下拉');

INSERT IGNORE INTO sys_dict_item (dict_code, item_value, item_label, sort_order, status) VALUES
('event_report_source', 'GRID_MEMBER', '网格员上报', 1, 'ACTIVE'),
('event_report_source', 'RESIDENT', '居民上报', 2, 'ACTIVE'),
('event_report_source', '12345', '12345转办', 3, 'ACTIVE'),
('event_report_source', 'PROPERTY', '物业上报', 4, 'ACTIVE'),
('event_report_source', 'AI_CAMERA', 'AI监控抓拍', 5, 'ACTIVE');

-- ---------- 3. 权限记录 ----------
INSERT IGNORE INTO sys_permission (permission_code, permission_name, permission_type, client_type, path, sort_order, status, remark, created_at, updated_at) VALUES
('web:menu:system-dicts', '字典管理', 'MENU', 'WEB', '/system-dicts', 233, 'ACTIVE', 'Web菜单-系统设置（仅超管）', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('api:system:dict:list', '查询字典', 'API', 'WEB', '/api/system/dicts', 134, 'ACTIVE', 'Dict read permission', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('api:system:dict:manage', '管理字典', 'API', 'WEB', '/api/system/dicts', 135, 'ACTIVE', 'Dict write permission', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- ---------- 4. 授权：SUPER_ADMIN 全部；EVENT_OPERATOR 仅字典读取 ----------
INSERT IGNORE INTO sys_role_permission (role_id, permission_id, created_at, updated_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM sys_role r
JOIN sys_permission p ON p.permission_code IN ('web:menu:system-dicts', 'api:system:dict:list', 'api:system:dict:manage')
WHERE r.role_code = 'SUPER_ADMIN';

INSERT IGNORE INTO sys_role_permission (role_id, permission_id, created_at, updated_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM sys_role r
JOIN sys_permission p ON p.permission_code = 'api:system:dict:list'
WHERE r.role_code = 'EVENT_OPERATOR';
