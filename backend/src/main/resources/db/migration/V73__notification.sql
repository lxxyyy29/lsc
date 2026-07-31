-- 站内通知表
CREATE TABLE IF NOT EXISTS sys_notification (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '接收人ID',
    title VARCHAR(128) NOT NULL COMMENT '通知标题',
    content TEXT COMMENT '通知内容',
    type VARCHAR(32) NOT NULL COMMENT '类型: WORK_ORDER/AUDIT/SYSTEM',
    level VARCHAR(16) DEFAULT 'NORMAL' COMMENT '级别: URGENT/NORMAL/LOW',
    related_type VARCHAR(32) COMMENT '关联业务类型',
    related_id BIGINT COMMENT '关联业务ID',
    is_read TINYINT DEFAULT 0 COMMENT '是否已读',
    read_at TIMESTAMP NULL COMMENT '阅读时间',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_user_read (user_id, is_read),
    INDEX idx_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='站内通知';
