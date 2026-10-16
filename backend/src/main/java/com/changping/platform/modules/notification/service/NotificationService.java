package com.changping.platform.modules.notification.service;

import com.changping.platform.modules.notification.entity.NotificationEntity;
import com.changping.platform.modules.notification.mapper.NotificationMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationMapper mapper;
    private final NotificationBatchWriter batchWriter;
    private final JdbcTemplate jdbcTemplate;

    public NotificationService(NotificationMapper mapper, NotificationBatchWriter batchWriter, JdbcTemplate jdbcTemplate) {
        this.mapper = mapper;
        this.batchWriter = batchWriter;
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 创建通知（异步攒批落库，避免高频单条 INSERT 阻塞业务线程）
     *
     * 时段抑制（夜间静默）：命中 NO_NOTIFY 的事件在抑制期内不产生任何通知，
     * 这里作为唯一通知入口统一拦截；到点（suppress_until 过期）或人工强制放出后自然恢复。
     */
    public void createNotification(Long userId, String title, String content,
                                   String type, String level, String relatedType, Long relatedId) {
        if (isSuppressedEvent(relatedType, relatedId)) {
            log.info("[suppress] 事件 {} 处于静默时段，跳过通知: title={}", relatedId, title);
            return;
        }
        NotificationEntity entity = new NotificationEntity();
        entity.setUserId(userId);
        entity.setTitle(title);
        entity.setContent(content);
        entity.setType(type);
        entity.setLevel(level);
        entity.setRelatedType(relatedType);
        entity.setRelatedId(relatedId);
        batchWriter.enqueue(entity);
    }

    /**
     * 判断关联对象是否为「处于静默时段且规则勾选了 NO_NOTIFY 的事件」。
     * 仅对 relatedType=EVENT 生效；查询失败时放行（通知优先，不因抑制判定异常而丢通知）。
     */
    private boolean isSuppressedEvent(String relatedType, Long relatedId) {
        if (relatedId == null || relatedType == null || !"EVENT".equalsIgnoreCase(relatedType.trim())) {
            return false;
        }
        try {
            Integer muted = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM biz_event WHERE id = ? AND COALESCE(suppress_manual, 0) = 0 "
                            + "AND suppress_until IS NOT NULL AND suppress_until > NOW() "
                            + "AND suppress_actions LIKE '%NO_NOTIFY%'",
                    Integer.class, relatedId);
            return muted != null && muted > 0;
        } catch (Exception e) {
            log.warn("[suppress] 通知抑制判定失败，按正常发送处理: eventId={}, err={}", relatedId, e.getMessage());
            return false;
        }
    }

    /**
     * 查询用户通知（分页）
     */
    public Map<String, Object> findByUserId(Long userId, int page, int size) {
        List<NotificationEntity> items = mapper.findByUserId(userId, page, size);
        long total = mapper.countByUserId(userId);
        Map<String, Object> result = new HashMap<>();
        result.put("items", items);
        result.put("total", total);
        result.put("page", page);
        result.put("size", size);
        return result;
    }

    /**
     * 未读数量
     */
    public long countUnread(Long userId) {
        return mapper.countUnreadByUserId(userId);
    }

    /**
     * 标记已读
     */
    public boolean markAsRead(Long id, Long userId) {
        return mapper.markAsRead(id, userId) > 0;
    }

    /**
     * 全部已读
     */
    public int markAllAsRead(Long userId) {
        return mapper.markAllAsRead(userId);
    }

    /**
     * 删除通知
     */
    public boolean delete(Long id, Long userId) {
        return mapper.deleteById(id, userId) > 0;
    }
}
