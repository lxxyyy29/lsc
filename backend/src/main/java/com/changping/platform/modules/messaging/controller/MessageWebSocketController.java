package com.changping.platform.modules.messaging.controller;

import com.changping.platform.modules.auth.model.AuthenticatedUser;
import com.changping.platform.modules.messaging.dto.SendMessagePayload;
import com.changping.platform.modules.messaging.handler.WebSocketAuthHandshakeHandler.WebSocketUserPrincipal;
import com.changping.platform.modules.messaging.service.MessageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.Map;

/**
 * 消息互通 WebSocket 控制器
 * - /app/send：发送消息，持久化后推送给接收方（/queue/messages）和发送方回执
 * - 用户通过 /user/{userId}/queue/messages 接收
 */
@Controller
public class MessageWebSocketController {

    private static final Logger log = LoggerFactory.getLogger(MessageWebSocketController.class);

    private final MessageService service;
    private final SimpMessagingTemplate messagingTemplate;

    public MessageWebSocketController(MessageService service, SimpMessagingTemplate messagingTemplate) {
        this.service = service;
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * 发送消息
     * 客户端发送目的地：/app/send
     * 载荷：{ receiverId, content, contentType }
     */
    @MessageMapping("/send")
    public void send(@Payload SendMessagePayload payload, Principal principal) {
        if (principal == null) {
            log.warn("WS send rejected: no principal");
            return;
        }
        // Principal 可能是 AuthenticatedUser（Spring Security 注入）或 WebSocketUserPrincipal
        Long senderId = resolveSenderId(principal);
        if (senderId == null) {
            log.warn("WS send rejected: cannot resolve sender from principal {}", principal.getClass().getName());
            return;
        }
        Long receiverId = payload.receiverId();
        String content = payload.content();
        if (receiverId == null || content == null || content.isBlank()) {
            return;
        }
        log.debug("WS send: sender={}, receiver={}, content={}", senderId, receiverId, content);

        // 持久化
        Long msgId = service.saveMessage(senderId, receiverId, content, payload.contentType());

        // 构造推送消息
        Map<String, Object> message = new java.util.LinkedHashMap<>();
        message.put("id", msgId);
        message.put("senderId", senderId);
        message.put("receiverId", receiverId);
        message.put("content", content);
        message.put("contentType", payload.contentType());
        message.put("createdAt", java.time.LocalDateTime.now().toString());

        // 推送给接收方
        messagingTemplate.convertAndSendToUser(
            String.valueOf(receiverId), "/queue/messages", message);
        // 回执给发送方（用于多端同步/消息确认）
        messagingTemplate.convertAndSendToUser(
            String.valueOf(senderId), "/queue/messages", message);

        log.debug("WS message #{}: {} -> {}", msgId, senderId, receiverId);
    }

    /** 从 Principal 解析发送者 ID（通过反射读取 id，避免类型强转） */
    private Long resolveSenderId(Principal principal) {
        // 先尝试直接解析 getName() 为数字（WebSocketUserPrincipal 返回 String.valueOf(id)）
        try {
            return Long.valueOf(principal.getName());
        } catch (NumberFormatException ignored) {
            // getName() 不是数字，可能是 AuthenticatedUser.toString()，用反射取 id
        }
        try {
            // AuthenticatedUser 是 record，有 id() 方法
            java.lang.reflect.Method idMethod = principal.getClass().getMethod("id");
            Object id = idMethod.invoke(principal);
            if (id instanceof Long l) return l;
        } catch (Exception e) {
            log.warn("Cannot resolve id from principal {}: {}", principal.getClass().getName(), e.getMessage());
        }
        return null;
    }
}
