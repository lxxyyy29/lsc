package com.changping.platform.modules.messaging.config;

import com.changping.platform.modules.messaging.handler.WebSocketAuthHandshakeHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket 消息代理配置
 * - 启用 STOMP 协议，端点 /ws（SockJS 降级支持）
 * - 消息代理：/queue（点对点）、/topic（广播）
 * - 应用前缀：/app（@MessageMapping 路由）
 * - 握手时通过 token query param 认证用户
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final WebSocketAuthHandshakeHandler handshakeHandler;

    public WebSocketConfig(WebSocketAuthHandshakeHandler handshakeHandler) {
        this.handshakeHandler = handshakeHandler;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/queue", "/topic");
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // SockJS 端点（Web 端 + 旧浏览器回退）
        registry.addEndpoint("/ws")
                .setHandshakeHandler(handshakeHandler)
                .setAllowedOriginPatterns("*")
                .withSockJS();
        // 原生 WebSocket 端点（H5 移动端，无需 SockJS）
        registry.addEndpoint("/ws-native")
                .setHandshakeHandler(handshakeHandler)
                .setAllowedOriginPatterns("*");
    }
}
