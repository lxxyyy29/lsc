package com.changping.platform.modules.notification.service;

import com.changping.platform.common.async.BatchInsertWorker;
import com.changping.platform.modules.notification.entity.NotificationEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 通知异步批量写入器：通知创建改为内存攒批后批量落库，避免高频单条 INSERT 阻塞业务线程。
 * SQL 与 NotificationMapper.insert 保持一致。
 */
@Component
public class NotificationBatchWriter extends BatchInsertWorker<NotificationEntity> {

    private static final String INSERT_SQL =
            "INSERT INTO sys_notification (user_id, title, content, type, level, related_type, related_id, is_read, created_at) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, 0, NOW())";

    public NotificationBatchWriter(JdbcTemplate jdbcTemplate) {
        super(jdbcTemplate, INSERT_SQL, 500, 500, "notification-batch-writer");
    }

    @Override
    protected Object[] toArgs(NotificationEntity entity) {
        return new Object[]{
                entity.getUserId(),
                entity.getTitle(),
                entity.getContent(),
                entity.getType(),
                entity.getLevel(),
                entity.getRelatedType(),
                entity.getRelatedId()
        };
    }
}
