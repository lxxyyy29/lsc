package com.changping.platform.modules.messaging.dto;

/**
 * 发送消息载荷（WebSocket /app/send 端点接收）
 */
public record SendMessagePayload(Long receiverId, String content, String contentType) {
}
