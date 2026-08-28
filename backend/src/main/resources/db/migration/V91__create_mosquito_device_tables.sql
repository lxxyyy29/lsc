-- V91：爱卫蚊媒 - 检测设备接入（设备台账 + 设备监测数据流）
-- 设备通过 /integrations/mosquito/device-data 推送上报，平台落库并做超标判定

-- 1. 蚊媒监测设备台账
CREATE TABLE IF NOT EXISTS biz_mosquito_device (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    device_no VARCHAR(64) NOT NULL COMMENT '设备编号（厂商唯一标识）',
    device_name VARCHAR(128) COMMENT '设备名称',
    device_type VARCHAR(32) NOT NULL DEFAULT 'MOSQUITO_TRAP' COMMENT '设备类型: MOSQUITO_TRAP=智能捕蚊器 DENSITY_MONITOR=密度监测仪 SENSOR=传感器',
    site_id BIGINT COMMENT '关联孳生地ID',
    site_name VARCHAR(128) COMMENT '关联孳生地名称',
    status VARCHAR(16) NOT NULL DEFAULT 'OFFLINE' COMMENT '在线状态: ONLINE=在线 OFFLINE=离线',
    last_online_at TIMESTAMP NULL COMMENT '最后上线时间（最近一次上报时间）',
    remark VARCHAR(500) COMMENT '备注',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_mosquito_device_no (device_no),
    INDEX idx_mosquito_device_site (site_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='蚊媒监测设备台账';

-- 2. 设备监测数据流（设备上报的原始监测数据）
CREATE TABLE IF NOT EXISTS biz_mosquito_device_data (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    device_no VARCHAR(64) NOT NULL COMMENT '设备编号',
    site_id BIGINT COMMENT '关联孳生地ID',
    site_name VARCHAR(128) COMMENT '孳生地名称',
    metric_type VARCHAR(32) NOT NULL DEFAULT 'DENSITY' COMMENT '指标类型: DENSITY=成蚊密度(只/灯·夜) CAPTURE=捕获数(只) BITE=叮咬率(次/人·夜) BREEDING=孳生指数',
    metric_value DECIMAL(10, 2) NOT NULL COMMENT '指标数值',
    threshold DECIMAL(10, 2) COMMENT '预警阈值',
    alarm_level VARCHAR(16) NOT NULL DEFAULT 'NORMAL' COMMENT '预警级别: NORMAL=正常 OVER=超标',
    collected_at DATETIME NOT NULL COMMENT '采集时间（设备上报时间）',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_devicedata_unique (device_no, metric_type, collected_at),
    INDEX idx_devicedata_site_metric (site_id, metric_type, collected_at),
    INDEX idx_devicedata_alarm (alarm_level)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='蚊媒设备监测数据流';
