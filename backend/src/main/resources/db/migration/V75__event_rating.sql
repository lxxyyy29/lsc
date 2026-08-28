-- 事件评价表
CREATE TABLE IF NOT EXISTS biz_event_rating (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_id BIGINT NOT NULL COMMENT '关联事件ID',
    user_id BIGINT COMMENT '评价人ID（居民）',
    user_name VARCHAR(64) COMMENT '评价人姓名',
    score TINYINT NOT NULL COMMENT '评分 1-5',
    content TEXT COMMENT '评价内容',
    tags VARCHAR(255) COMMENT '评价标签（逗号分隔）',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_event_id (event_id),
    INDEX idx_user_id (user_id),
    INDEX idx_score (score)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='事件处置评价';

-- 评价统计视图（按事件）
-- 评分权限
INSERT INTO sys_permission (permission_code, permission_name, permission_type, client_type, parent_id, path, component, icon, sort_order, status, remark, created_at, updated_at)
SELECT 'api:event:rating', '事件评价', 'API', 'WEB', id, NULL, NULL, NULL, 10, 'ACTIVE', '事件评价权限', NOW(), NOW()
FROM sys_permission WHERE permission_code = 'catalog:community'
ON DUPLICATE KEY UPDATE updated_at = NOW();
