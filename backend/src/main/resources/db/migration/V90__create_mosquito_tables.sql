-- V90：卫生防疫/爱卫 — 蚊媒孳生地红黄绿分级 + 消杀记录 + 重点场所卫生监测（C4）

-- 1. 蚊媒孳生地（三色分级：RED=紧急险情需立即消杀 / YELLOW=重点 / GREEN=一般）
CREATE TABLE IF NOT EXISTS biz_mosquito_site (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    site_no VARCHAR(64) NOT NULL COMMENT '孳生地编号',
    site_name VARCHAR(128) NOT NULL COMMENT '孳生地名称',
    site_type VARCHAR(32) NOT NULL DEFAULT 'OTHER' COMMENT '类型: CATCH_BASIN=积水容器 DITCH=沟渠 SEWER=下水道 GREEN=绿化带 GARBAGE=垃圾点 WATER=水塘 OTHER=其他',
    address VARCHAR(255) COMMENT '详细地址',
    grid_id BIGINT COMMENT '所属网格ID',
    grid_name VARCHAR(100) COMMENT '所属网格名称',
    risk_level VARCHAR(16) NOT NULL DEFAULT 'GREEN' COMMENT '三色分级: RED=红(紧急) YELLOW=黄(重点) GREEN=绿(一般)',
    longitude DECIMAL(10, 6) COMMENT '经度',
    latitude DECIMAL(10, 6) COMMENT '纬度',
    owner_name VARCHAR(100) COMMENT '责任人',
    owner_phone VARCHAR(32) COMMENT '责任人电话',
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态: ACTIVE=在管 ELIMINATED=已消除',
    last_check_at TIMESTAMP NULL COMMENT '最近检查时间',
    remark VARCHAR(500) COMMENT '备注',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_mosquito_site_no (site_no),
    INDEX idx_mosquito_grid (grid_id),
    INDEX idx_mosquito_level_status (risk_level, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='蚊媒孳生地（三色分级）';

-- 2. 消杀记录
CREATE TABLE IF NOT EXISTS biz_disinfection_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    record_no VARCHAR(64) NOT NULL COMMENT '消杀记录编号',
    site_id BIGINT COMMENT '关联孳生地ID（可空，直接消杀）',
    site_name VARCHAR(128) COMMENT '消杀对象名称',
    disinfection_type VARCHAR(32) NOT NULL DEFAULT 'CHEMICAL' COMMENT '方式: CHEMICAL=药物消杀 CLEANING=清理积水 BIOLOGICAL=生物防治 OTHER=其他',
    disinfectant VARCHAR(100) COMMENT '使用药物/手段',
    operator_name VARCHAR(100) COMMENT '作业人员',
    operator_date DATE NOT NULL COMMENT '作业日期',
    area_sqm DECIMAL(10, 2) COMMENT '消杀面积(㎡)',
    result VARCHAR(16) NOT NULL DEFAULT 'GOOD' COMMENT '效果: GOOD=良好 FAIR=一般 POOR=差(需复查)',
    remark VARCHAR(500) COMMENT '备注',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_disinfection_record_no (record_no),
    INDEX idx_disinfection_site (site_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='蚊媒消杀记录';

-- 3. 重点场所卫生监测
CREATE TABLE IF NOT EXISTS biz_health_monitor (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    place_name VARCHAR(128) NOT NULL COMMENT '监测场所名称',
    place_type VARCHAR(32) NOT NULL DEFAULT 'OTHER' COMMENT '类型: SCHOOL=学校 MARKET=农贸市场 RESTAURANT=餐饮 CLINIC=诊所 COMMUNITY=小区 OTHER=其他',
    address VARCHAR(255) COMMENT '详细地址',
    grid_id BIGINT COMMENT '所属网格ID',
    grid_name VARCHAR(100) COMMENT '所属网格名称',
    monitor_item VARCHAR(128) COMMENT '监测项目（如：蚊媒密度、环境卫生、病媒生物）',
    score INT NOT NULL DEFAULT 100 COMMENT '评分（0-100）',
    risk_level VARCHAR(16) NOT NULL DEFAULT 'GREEN' COMMENT '三色分级: RED/YELLOW/GREEN',
    monitor_date DATE NOT NULL COMMENT '监测日期',
    monitor_org VARCHAR(128) COMMENT '监测机构/单位',
    remark VARCHAR(500) COMMENT '备注',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_health_grid (grid_id),
    INDEX idx_health_date (monitor_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='重点场所卫生监测';
