package com.changping.platform.modules.messaging.handler;

import com.changping.platform.modules.auth.model.AuthenticatedUser;
import com.changping.platform.modules.auth.security.JwtTokenService;
import com.changping.platform.modules.auth.service.AuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import java.security.Principal;
import java.util.Map;

/**
 * WebSocket 握手认证：从 query param "token" 解析 JWT，构造 Principal 绑定到 WS 会话
 * 浏览器 WebSocket 无法发送 Authorization 头，故通过 query param 传递
 */
@Component
public class WebSocketAuthHandshakeHandler extends DefaultHandshakeHandler {

    private static final Logger log = LoggerFactory.getLogger(WebSocketAuthHandshakeHandler.class);

    private final JwtTokenService jwtTokenService;
    private final AuthService authService;

    public WebSocketAuthHandshakeHandler(JwtTokenService jwtTokenService, AuthService authService) {
        this.jwtTokenService = jwtTokenService;
        this.authService = authService;
    }

    @Override
    protected Principal determineUser(ServerHttpRequest request, WebSocketHandler wsHandler,
                                      Map<String, Object> attributes) {
        String token = null;
        if (request instanceof ServletServerHttpRequest servletRequest) {
            token = servletRequest.getServletRequest().getParameter("token");
        }
        if (token == null || token.isBlank()) {
            log.debug("WS handshake without token, anonymous");
            return null;
        }
        try {
            AuthenticatedUser tokenUser = jwtTokenService.parseAuthenticatedUser(token);
            AuthenticatedUser user = authService.loadAuthenticatedUser(tokenUser.id(), tokenUser.clientType());
            if (tokenUser.passwordVersion() != user.passwordVersion()) {
                log.warn("WS handshake rejected: password changed for user {}", user.id());
                return null;
            }
            log.debug("WS handshake OK: user {} ({})", user.id(), user.userName());
            return new WebSocketUserPrincipal(user);
        } catch (Exception e) {
            log.warn("WS handshake token invalid: {}", e.getMessage());
            return null;
        }
    }

    /** WebSocket 用户 Principal，getName() 返回用户 ID 字符串，供 convertAndSendToUser 路由 */
    public record WebSocketUserPrincipal(AuthenticatedUser user) implements Principal {
        @Override
        public String getName() {
            return String.valueOf(user.id());
        }
    }
}
