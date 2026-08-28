-- =============================================
-- V58: 信息互通 - 外部系统对接框架
-- =============================================

-- 外部系统配置表
CREATE TABLE biz_external_system (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    system_code VARCHAR(64) NOT NULL COMMENT '系统编码: EMERGENCY/HEALTH/CIVIL_AFFAIRS/PROPERTY/12345',
    system_name VARCHAR(100) NOT NULL COMMENT '系统名称',
    system_type VARCHAR(32) NOT NULL COMMENT '系统类型: API/WEBHOOK/FTP/DB',
    api_base_url VARCHAR(500) COMMENT 'API 基础地址',
    api_key VARCHAR(255) COMMENT 'API 密钥',
    api_secret VARCHAR(255) COMMENT 'API 密钥密码',
    sync_enabled TINYINT(1) DEFAULT 1 COMMENT '是否启用同步: 1=启用 0=禁用',
    sync_cron VARCHAR(64) DEFAULT '0 0/30 * * * ?' COMMENT '同步 Cron 表达式',
    last_sync_at TIMESTAMP NULL COMMENT '最后同步时间',
    last_sync_status VARCHAR(16) DEFAULT 'PENDING' COMMENT '最后同步状态: SUCCESS/FAILURE/PENDING',
    last_sync_message VARCHAR(500) COMMENT '最后同步消息',
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态: ACTIVE/DISABLED',
    remark VARCHAR(500) COMMENT '备注',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_biz_external_system_code (system_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='外部系统配置';

-- 数据映射配置表
CREATE TABLE biz_data_mapping (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    system_code VARCHAR(64) NOT NULL COMMENT '外部系统编码',
    source_table VARCHAR(64) NOT NULL COMMENT '源数据表/接口',
    target_table VARCHAR(64) NOT NULL COMMENT '目标数据表',
    field_mapping JSON NOT NULL COMMENT '字段映射规则: {"source_field": "target_field"}',
    filter_rule VARCHAR(500) COMMENT '过滤条件',
    transform_rule VARCHAR(500) COMMENT '转换规则',
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_biz_data_mapping_system (system_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据映射配置';

-- 同步日志表
CREATE TABLE biz_sync_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    system_code VARCHAR(64) NOT NULL COMMENT '外部系统编码',
    sync_type VARCHAR(32) NOT NULL COMMENT '同步类型: PUSH/PULL',
    sync_action VARCHAR(64) NOT NULL COMMENT '同步动作',
    records_total INT DEFAULT 0 COMMENT '总记录数',
    records_success INT DEFAULT 0 COMMENT '成功数',
    records_failed INT DEFAULT 0 COMMENT '失败数',
    status VARCHAR(16) NOT NULL COMMENT '状态: SUCCESS/FAILURE/PARTIAL',
    error_message TEXT COMMENT '错误信息',
    started_at TIMESTAMP NOT NULL COMMENT '开始时间',
    finished_at TIMESTAMP NULL COMMENT '结束时间',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_biz_sync_log_system (system_code),
    INDEX idx_biz_sync_log_time (started_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='同步日志';

-- 外部数据缓存表（存储从外部系统拉取的原始数据）
CREATE TABLE biz_external_data (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    system_code VARCHAR(64) NOT NULL COMMENT '外部系统编码',
    external_id VARCHAR(128) NOT NULL COMMENT '外部系统数据ID',
    data_type VARCHAR(64) NOT NULL COMMENT '数据类型: EVENT/POPULATION/MERCHANT',
    raw_data JSON COMMENT '原始数据',
    normalized_data JSON COMMENT '标准化后数据',
    sync_status VARCHAR(16) DEFAULT 'PENDING' COMMENT '同步状态: PENDING/SYNCED/FAILED',
    linked_business_id BIGINT COMMENT '关联的业务ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_biz_external_data (system_code, external_id, data_type),
    INDEX idx_biz_external_data_status (sync_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='外部数据缓存';

-- 插入默认外部系统配置
INSERT INTO biz_external_system (system_code, system_name, system_type, api_base_url, sync_enabled, remark) VALUES
('EMERGENCY', '应急管理平台', 'API', 'https://emergency.example.com/api', 0, '应急事件数据对接'),
('HEALTH', '卫生健康平台', 'API', 'https://health.example.com/api', 0, '卫生事件/健康数据对接'),
('CIVIL_AFFAIRS', '民政平台', 'API', 'https://civil.example.com/api', 0, '低保/特困/救助数据对接'),
('PROPERTY', '物业管理平台', 'API', 'https://property.example.com/api', 0, '物业报修/投诉数据对接'),
('12345', '12345政务热线', 'API', 'https://12345.example.com/api', 0, '市民热线工单对接')
ON DUPLICATE KEY UPDATE system_name = VALUES(system_name);
