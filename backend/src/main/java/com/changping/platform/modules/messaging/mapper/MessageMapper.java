package com.changping.platform.modules.messaging.mapper;

import com.changping.platform.modules.messaging.entity.MessageEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

/**
 * 消息数据访问：持久化、历史查询、会话列表、未读数
 */
@Component
public class MessageMapper {

    private final JdbcTemplate jdbcTemplate;

    private static final RowMapper<MessageEntity> ROW_MAPPER = (rs, rowNum) -> {
        MessageEntity e = new MessageEntity();
        e.setId(rs.getLong("id"));
        e.setSenderId(rs.getLong("sender_id"));
        e.setReceiverId(rs.getLong("receiver_id"));
        e.setContent(rs.getString("content"));
        e.setContentType(rs.getString("content_type"));
        Timestamp readAt = rs.getTimestamp("read_at");
        e.setReadAt(readAt != null ? readAt.toLocalDateTime() : null);
        e.setCreatedAt(rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toLocalDateTime() : null);
        return e;
    };

    public MessageMapper(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /** 保存消息 */
    public Long insert(MessageEntity e) {
        jdbcTemplate.update(
            "INSERT INTO biz_message (sender_id, receiver_id, content, content_type, created_at) VALUES (?, ?, ?, ?, NOW())",
            e.getSenderId(), e.getReceiverId(), e.getContent(),
            e.getContentType() != null ? e.getContentType() : "TEXT");
        return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    /** 查询两个用户之间的历史消息（按时间正序） */
    public List<MessageEntity> findHistory(Long userA, Long userB, int limit) {
        return jdbcTemplate.query(
            "SELECT * FROM biz_message " +
            "WHERE (sender_id = ? AND receiver_id = ?) OR (sender_id = ? AND receiver_id = ?) " +
            "ORDER BY created_at DESC LIMIT ?",
            ROW_MAPPER, userA, userB, userB, userA, limit);
    }

    /** 将会话中对方发来的未读消息标记为已读 */
    public int markRead(Long receiverId, Long senderId) {
        return jdbcTemplate.update(
            "UPDATE biz_message SET read_at = NOW() " +
            "WHERE receiver_id = ? AND sender_id = ? AND read_at IS NULL",
            receiverId, senderId);
    }

    /**
     * 会话列表：按对方用户分组，返回每个对话的最后一条消息、对方信息、未读数
     * 当前用户为 userId，列出所有有过消息往来的对方
     */
    public List<Map<String, Object>> findConversations(Long userId) {
        // 取每个对话方的最后一条消息
        String sql =
            "SELECT " +
            "  partner_id, " +
            "  last_content, " +
            "  last_created, " +
            "  unread_count " +
            "FROM ( " +
            "  SELECT " +
            "    CASE WHEN sender_id = ? THEN receiver_id ELSE sender_id END AS partner_id, " +
            "    content AS last_content, " +
            "    created_at AS last_created, " +
            "    SUM(CASE WHEN receiver_id = ? AND read_at IS NULL THEN 1 ELSE 0 END) " +
            "      OVER (PARTITION BY CASE WHEN sender_id = ? THEN receiver_id ELSE sender_id END) AS unread_count, " +
            "    ROW_NUMBER() " +
            "      OVER (PARTITION BY CASE WHEN sender_id = ? THEN receiver_id ELSE sender_id END ORDER BY created_at DESC) AS rn " +
            "  FROM biz_message " +
            "  WHERE sender_id = ? OR receiver_id = ? " +
            ") t " +
            "WHERE rn = 1 " +
            "ORDER BY last_created DESC";
        return jdbcTemplate.queryForList(sql, userId, userId, userId, userId, userId, userId);
    }

    /** 查询某用户信息（用于会话列表展示） */
    public Map<String, Object> findUserById(Long userId) {
        List<Map<String, Object>> list = jdbcTemplate.queryForList(
            "SELECT id, username, real_name FROM sys_user WHERE id = ?", userId);
        return list.isEmpty() ? null : list.get(0);
    }

    /** 查询所有网格员（GRID_WORKER 角色），用于发起新会话 */
    public List<Map<String, Object>> findGridWorkers() {
        return jdbcTemplate.queryForList(
            "SELECT u.id, u.username, u.real_name " +
            "FROM sys_user u " +
            "JOIN sys_user_role ur ON ur.user_id = u.id " +
            "JOIN sys_role r ON r.id = ur.role_id " +
            "WHERE r.role_code = 'GRID_WORKER' " +
            "ORDER BY u.id");
    }
}
