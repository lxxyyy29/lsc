-- 信息互通：消息表（支持 Web 管理员 ↔ H5 网格员 实时聊天）
CREATE TABLE IF NOT EXISTS biz_message (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sender_id BIGINT NOT NULL COMMENT '发送人ID',
    receiver_id BIGINT NOT NULL COMMENT '接收人ID',
    content TEXT NOT NULL COMMENT '消息内容',
    content_type VARCHAR(16) DEFAULT 'TEXT' COMMENT '类型: TEXT=文本 / IMAGE=图片',
    read_at TIMESTAMP NULL COMMENT '对方阅读时间',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_sender (sender_id),
    INDEX idx_receiver (receiver_id),
    INDEX idx_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='信息互通消息';
