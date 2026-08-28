-- 安全风险防控：为场所表添加安全字段
ALTER TABLE biz_merchant
    ADD COLUMN fire_risk_level VARCHAR(16) DEFAULT 'LOW' COMMENT '消防风险等级: LOW/MEDIUM/HIGH',
    ADD COLUMN safety_status VARCHAR(16) DEFAULT 'NORMAL' COMMENT '安全状态: NORMAL/WARNING/DANGER',
    ADD COLUMN last_inspection_date DATE COMMENT '最近巡查日期',
    ADD COLUMN inspection_remark VARCHAR(500) COMMENT '巡查备注';

-- 安全检查记录表
CREATE TABLE IF NOT EXISTS biz_safety_inspection (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    merchant_id BIGINT NOT NULL COMMENT '场所ID',
    inspector_id BIGINT COMMENT '检查人ID',
    inspector_name VARCHAR(64) COMMENT '检查人姓名',
    inspection_date DATE NOT NULL COMMENT '检查日期',
    fire_risk_level VARCHAR(16) COMMENT '消防风险等级',
    safety_status VARCHAR(16) COMMENT '安全状态',
    hazards_found JSON COMMENT '发现的隐患',
    rectification_required TINYINT(1) DEFAULT 0 COMMENT '是否需要整改',
    rectification_deadline DATE COMMENT '整改截止日期',
    rectification_status VARCHAR(16) DEFAULT 'PENDING' COMMENT '整改状态: PENDING/IN_PROGRESS/COMPLETED/OVERDUE',
    remarks VARCHAR(500) COMMENT '备注',
    photo_urls JSON COMMENT '检查照片',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_merchant (merchant_id),
    INDEX idx_date (inspection_date),
    INDEX idx_status (rectification_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='安全检查记录';
