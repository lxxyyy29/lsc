package com.changping.platform.modules.drone;

import com.changping.platform.common.exception.BusinessException;
import com.changping.platform.modules.auth.model.AuthenticatedUser;
import com.changping.platform.modules.auth.security.JwtTokenService;
import com.changping.platform.modules.auth.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

/**
 * @Author tangxinglin
 * @Description //无人机WebSocket配置类，注册WebSocket处理器及握手拦截器，
 * 实现客户端连接时的JWT认证、权限校验和设备序列号绑定
 * @Date 2026/04/18 10:00
 */
@Configuration
@EnableWebSocket
public class DroneWebSocketConfig implements WebSocketConfigurer {

    private final DroneWebSocketBridgeService bridgeService;
    private final JwtTokenService jwtTokenService;
    private final AuthService authService;

    /**
     * @Author tangxinglin
     * @Description //构造函数，注入WebSocket桥接服务、JWT令牌服务及认证服务
     * @Date 2026/04/18 10:00
     * @Param [bridgeService WebSocket桥接服务, jwtTokenService JWT令牌服务, authService 认证服务]
     * @return void
     */
    public DroneWebSocketConfig(
            DroneWebSocketBridgeService bridgeService,
            JwtTokenService jwtTokenService,
            AuthService authService) {
        this.bridgeService = bridgeService;
        this.jwtTokenService = jwtTokenService;
        this.authService = authService;
    }

    /**
     * @Author tangxinglin
     * @Description //注册无人机WebSocket处理器到路径 /ws/drone，添加握手拦截器并允许所有来源
     * @Date 2026/04/18 10:00
     * @Param [registry WebSocket处理器注册表]
     * @return void
     */
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(new DroneWebSocketHandler(bridgeService), "/ws/drone")
                .addInterceptors(new DroneHandshakeInterceptor(jwtTokenService, authService))
                .setAllowedOriginPatterns("*");
    }

    private static final class DroneWebSocketHandler extends TextWebSocketHandler {

        private final DroneWebSocketBridgeService bridgeService;

        private DroneWebSocketHandler(DroneWebSocketBridgeService bridgeService) {
            this.bridgeService = bridgeService;
        }

        @Override
        public void afterConnectionEstablished(WebSocketSession session) throws Exception {
            String deviceSn = (String) session.getAttributes().get("deviceSn");
            if (deviceSn == null || deviceSn.isBlank()) {
                session.close(CloseStatus.BAD_DATA);
                return;
            }
            bridgeService.registerClient(deviceSn, session);
            session.sendMessage(new TextMessage("{\"type\":\"connected\",\"deviceSn\":\"" + deviceSn + "\"}"));
        }

        @Override
        public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
            String deviceSn = (String) session.getAttributes().get("deviceSn");
            if (deviceSn != null) {
                bridgeService.unregisterClient(deviceSn, session);
            }
        }
    }

    private static final class DroneHandshakeInterceptor implements HandshakeInterceptor {

        private final JwtTokenService jwtTokenService;
        private final AuthService authService;

        private DroneHandshakeInterceptor(JwtTokenService jwtTokenService, AuthService authService) {
            this.jwtTokenService = jwtTokenService;
            this.authService = authService;
        }

        @Override
        public boolean beforeHandshake(
                ServerHttpRequest request,
                org.springframework.http.server.ServerHttpResponse response,
                WebSocketHandler wsHandler,
                Map<String, Object> attributes) {
            if (!(request instanceof ServletServerHttpRequest servletRequest)) {
                throw new BusinessException("AUTH_TOKEN_INVALID", "认证令牌无效");
            }
            HttpServletRequest httpRequest = servletRequest.getServletRequest();
            String token = resolveToken(httpRequest);
            AuthenticatedUser tokenUser = jwtTokenService.parseAuthenticatedUser(token);
            AuthenticatedUser currentUser = authService.loadAuthenticatedUser(tokenUser.id(), tokenUser.clientType());
            if (!AuthService.ClientType.WEB.name().equals(currentUser.clientType())) {
                throw new BusinessException("AUTH_CLIENT_TYPE_FORBIDDEN", "认证令牌不适用于该客户端类型");
            }
            String deviceSn = httpRequest.getParameter("deviceSn");
            if (deviceSn == null || deviceSn.isBlank()) {
                throw new BusinessException("VALIDATION_ERROR", "设备序列号不能为空");
            }
            attributes.put("deviceSn", deviceSn);
            attributes.put("user", currentUser);
            return true;
        }

        @Override
        public void afterHandshake(
                ServerHttpRequest request,
                org.springframework.http.server.ServerHttpResponse response,
                WebSocketHandler wsHandler,
                Exception exception) {
        }

        private String resolveToken(HttpServletRequest request) {
            // Try Authorization header first
            String authorization = request.getHeader(org.springframework.http.HttpHeaders.AUTHORIZATION);
            if (authorization != null && authorization.startsWith("Bearer ")) {
                String token = authorization.substring(7).trim();
                if (!token.isEmpty()) {
                    return token;
                }
            }
            // Fallback: query parameter (browser WebSocket cannot set headers)
            String queryToken = request.getParameter("token");
            if (queryToken != null && !queryToken.isBlank()) {
                return queryToken.trim();
            }
            throw new BusinessException("AUTH_TOKEN_REQUIRED", "请提供认证令牌");
        }
    }
}
