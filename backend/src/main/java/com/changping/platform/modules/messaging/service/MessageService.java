package com.changping.platform.modules.messaging.service;

import com.changping.platform.modules.messaging.entity.MessageEntity;
import com.changping.platform.modules.messaging.mapper.MessageMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 消息业务逻辑
 */
@Service
public class MessageService {

    private final MessageMapper mapper;

    public MessageService(MessageMapper mapper) {
        this.mapper = mapper;
    }

    /** 保存消息 */
    public Long saveMessage(Long senderId, Long receiverId, String content, String contentType) {
        MessageEntity e = new MessageEntity();
        e.setSenderId(senderId);
        e.setReceiverId(receiverId);
        e.setContent(content);
        e.setContentType(contentType != null ? contentType : "TEXT");
        return mapper.insert(e);
    }

    /** 历史消息（按时间正序返回） */
    public List<MessageEntity> history(Long userA, Long userB, int limit) {
        List<MessageEntity> list = mapper.findHistory(userA, userB, limit);
        // 反转：DB 按 DESC 取，前端要正序
        java.util.Collections.reverse(list);
        return list;
    }

    /** 标记已读 */
    public int markRead(Long receiverId, Long senderId) {
        return mapper.markRead(receiverId, senderId);
    }

    /** 会话列表（含对方信息） */
    public List<Map<String, Object>> conversations(Long userId) {
        List<Map<String, Object>> rows = mapper.findConversations(userId);
        for (Map<String, Object> row : rows) {
            Long partnerId = ((Number) row.get("partner_id")).longValue();
            Map<String, Object> user = mapper.findUserById(partnerId);
            row.put("partner", user);
        }
        return rows;
    }

    /** 所有网格员 */
    public List<Map<String, Object>> gridWorkers() {
        return mapper.findGridWorkers();
    }
}
