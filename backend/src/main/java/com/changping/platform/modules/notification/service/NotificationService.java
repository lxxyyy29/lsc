package com.changping.platform.modules.notification.service;

import com.changping.platform.modules.notification.entity.NotificationEntity;
import com.changping.platform.modules.notification.mapper.NotificationMapper;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class NotificationService {

    private final NotificationMapper mapper;

    public NotificationService(NotificationMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * 创建通知
     */
    public void createNotification(Long userId, String title, String content,
                                   String type, String level, String relatedType, Long relatedId) {
        NotificationEntity entity = new NotificationEntity();
        entity.setUserId(userId);
        entity.setTitle(title);
        entity.setContent(content);
        entity.setType(type);
        entity.setLevel(level);
        entity.setRelatedType(relatedType);
        entity.setRelatedId(relatedId);
        mapper.insert(entity);
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
