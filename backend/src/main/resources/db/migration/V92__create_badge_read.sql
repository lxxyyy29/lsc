-- V92：菜单角标已读时间（Web 侧边栏微信式红点：点击进入页面即已读，有新记录才再亮）

CREATE TABLE IF NOT EXISTS biz_badge_read (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    badge_key VARCHAR(32) NOT NULL COMMENT '角标KEY: eventsPending/workOrdersPending/auditsPending/residentReportsPending/trendAlerts',
    read_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '已读时间（该时间之前的新增不再亮红点）',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_badge_read (user_id, badge_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='菜单角标已读时间';
