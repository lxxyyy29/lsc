package com.changping.platform.modules.notification.mapper;

import com.changping.platform.modules.notification.entity.NotificationEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.util.List;

@Component
public class NotificationMapper {

    private final JdbcTemplate jdbcTemplate;

    private static final RowMapper<NotificationEntity> ROW_MAPPER = (rs, rowNum) -> {
        NotificationEntity e = new NotificationEntity();
        e.setId(rs.getLong("id"));
        e.setUserId(rs.getLong("user_id"));
        e.setTitle(rs.getString("title"));
        e.setContent(rs.getString("content"));
        e.setType(rs.getString("type"));
        e.setLevel(rs.getString("level"));
        e.setRelatedType(rs.getString("related_type"));
        long relatedId = rs.getLong("related_id");
        e.setRelatedId(rs.wasNull() ? null : relatedId);
        e.setIsRead(rs.getInt("is_read"));
        java.sql.Timestamp readAt = rs.getTimestamp("read_at");
        e.setReadAt(readAt != null ? readAt.toLocalDateTime() : null);
        java.sql.Timestamp createdAt = rs.getTimestamp("created_at");
        e.setCreatedAt(createdAt != null ? createdAt.toLocalDateTime() : null);
        return e;
    };

    /**
     * 查找用户通知（分页）
     */
    public List<NotificationEntity> findByUserId(Long userId, int page, int size) {
        int offset = (Math.max(1, page) - 1) * size;
        return jdbcTemplate.query(
                "SELECT * FROM sys_notification WHERE user_id = ? ORDER BY is_read ASC, created_at DESC LIMIT ? OFFSET ?",
                ROW_MAPPER, userId, size, offset);
    }

    public NotificationMapper(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void insert(NotificationEntity entity) {
        String sql = "INSERT INTO sys_notification (user_id, title, content, type, level, related_type, related_id, is_read, created_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, 0, NOW())";
        jdbcTemplate.update(sql, entity.getUserId(), entity.getTitle(), entity.getContent(),
                entity.getType(), entity.getLevel(), entity.getRelatedType(), entity.getRelatedId());
    }

    public List<NotificationEntity> findByUserId(Long userId, int limit) {
        return jdbcTemplate.query(
                "SELECT * FROM sys_notification WHERE user_id = ? ORDER BY created_at DESC LIMIT ?",
                ROW_MAPPER, userId, limit);
    }

    public List<NotificationEntity> findUnreadByUserId(Long userId, int page, int size) {
        int offset = (Math.max(1, page) - 1) * size;
        return jdbcTemplate.query(
                "SELECT * FROM sys_notification WHERE user_id = ? ORDER BY created_at DESC LIMIT ? OFFSET ?",
                ROW_MAPPER, userId, size, offset);
    }

    public long countByUserId(Long userId) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sys_notification WHERE user_id = ?", Long.class, userId);
        return count != null ? count : 0;
    }

    public long countUnreadByUserId(Long userId) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sys_notification WHERE user_id = ? AND is_read = 0", Long.class, userId);
        return count != null ? count : 0;
    }

    public int markAsRead(Long id, Long userId) {
        return jdbcTemplate.update(
                "UPDATE sys_notification SET is_read = 1, read_at = NOW() WHERE id = ? AND user_id = ?",
                id, userId);
    }

    public int markAllAsRead(Long userId) {
        return jdbcTemplate.update(
                "UPDATE sys_notification SET is_read = 1, read_at = NOW() WHERE user_id = ? AND is_read = 0",
                userId);
    }

    public int deleteById(Long id, Long userId) {
        return jdbcTemplate.update(
                "DELETE FROM sys_notification WHERE id = ? AND user_id = ?", id, userId);
    }
}
