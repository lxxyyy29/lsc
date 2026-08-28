-- V70：政策资源库（低保/养老/救助/医保/惠民），支撑政策找人
CREATE TABLE cmn_policy_resource (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    policy_code VARCHAR(64) NOT NULL,
    title VARCHAR(200) NOT NULL COMMENT '政策标题',
    policy_type VARCHAR(32) NOT NULL COMMENT 'LOW_INCOME=低保 ELDERLY=养老 RESCUE=救助 MEDICAL=医保 BENEFIT=惠民 OTHER=其他',
    description TEXT COMMENT '政策说明',
    eligibility TEXT COMMENT '资格条件说明',
    tags VARCHAR(255) COMMENT '匹配标签（逗号分隔），用于政策找人',
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE=启用 DISABLED=停用',
    publish_date DATE COMMENT '发布日期',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_policy_code (policy_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='政策资源库';
