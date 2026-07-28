package com.changping.platform.modules.drone;

import com.changping.platform.common.exception.BusinessException;
import com.changping.platform.modules.auth.model.AuthenticatedUser;
import com.changping.platform.modules.auth.security.JwtTokenService;
import com.changping.platform.modules.auth.service.AuthService;
import com.changping.platform.modules.drone.config.DroneApiProperties;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

/**
 * 无人机 WebSocket 配置
 * 1. /ws/drone - 遥测数据（通过 DroneWebSocketBridgeService）
 * 2. /ws/video/{deviceSn} - 视频流代理
 */
@Configuration
@EnableWebSocket
public class DroneWebSocketConfig implements WebSocketConfigurer {

    private static final Logger log = LoggerFactory.getLogger(DroneWebSocketConfig.class);

    private final DroneWebSocketBridgeService bridgeService;
    private final JwtTokenService jwtTokenService;
    private final AuthService authService;
    private final DroneProxyService droneProxyService;
    private final DroneApiProperties droneApiProperties;

    public DroneWebSocketConfig(DroneWebSocketBridgeService bridgeService,
                                 JwtTokenService jwtTokenService,
                                 AuthService authService,
                                 DroneProxyService droneProxyService,
                                 DroneApiProperties droneApiProperties) {
        this.bridgeService = bridgeService;
        this.jwtTokenService = jwtTokenService;
        this.authService = authService;
        this.droneProxyService = droneProxyService;
        this.droneApiProperties = droneApiProperties;
        log.info("DroneWebSocketConfig 已加载 - 注册 WebSocket 端点: /ws/drone, /ws/video");
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        log.info("=== 正在注册 WebSocket 处理器 ===");
        // 遥测数据 WebSocket
        registry.addHandler(new DroneWebSocketHandler(bridgeService), "/ws/drone")
                .addInterceptors(new DroneHandshakeInterceptor(jwtTokenService, authService))
                .setAllowedOriginPatterns("*");
        log.info("=== 已注册 /ws/drone 端点 ===");
        // 视频流 WebSocket 代理
        registry.addHandler(new VideoStreamHandler(droneProxyService, droneApiProperties), "/ws/video")
                .addInterceptors(new VideoHandshakeInterceptor())
                .setAllowedOriginPatterns("*");
        log.info("=== 已注册 /ws/video 端点 ===");
    }

    // ========== 视频流拦截器 ==========

    private static final class VideoHandshakeInterceptor implements HandshakeInterceptor {
        @Override
        public boolean beforeHandshake(ServerHttpRequest request, org.springframework.http.server.ServerHttpResponse response,
                                       WebSocketHandler wsHandler, Map<String, Object> attributes) {
            if (request instanceof ServletServerHttpRequest servletRequest) {
                HttpServletRequest httpRequest = servletRequest.getServletRequest();
                String deviceSn = httpRequest.getParameter("deviceSn");
                if (deviceSn != null && !deviceSn.isBlank()) {
                    attributes.put("deviceSn", deviceSn);
                }
            }
            return true;
        }

        @Override
        public void afterHandshake(ServerHttpRequest request, org.springframework.http.server.ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Exception exception) {
        }
    }

    // ========== 视频流代理 ==========

    private static final class VideoStreamHandler extends AbstractWebSocketHandler {
        private final DroneProxyService droneProxyService;
        private final DroneApiProperties droneApiProperties;
        private final StandardWebSocketClient wsClient = new StandardWebSocketClient();

        VideoStreamHandler(DroneProxyService droneProxyService, DroneApiProperties droneApiProperties) {
            this.droneProxyService = droneProxyService;
            this.droneApiProperties = droneApiProperties;
        }

        @Override
        public void afterConnectionEstablished(WebSocketSession session) throws Exception {
            String deviceSn = (String) session.getAttributes().get("deviceSn");
            if (deviceSn == null) {
                session.close(CloseStatus.BAD_DATA);
                return;
            }
            log.info("视频流 WebSocket 连接: deviceSn={}", deviceSn);
            connectUpstream(session, deviceSn);
        }

        @Override
        public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
            closeUpstream(session);
        }

        @Override
        protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) throws Exception {
            WebSocketSession upstream = getUpstream(session);
            if (upstream != null && upstream.isOpen()) {
                upstream.sendMessage(message);
            }
        }

        @SuppressWarnings("unchecked")
        private void connectUpstream(WebSocketSession clientSession, String deviceSn) {
            try {
                String flvUrl = getFlvStreamUrl(deviceSn);
                if (flvUrl == null) {
                    log.warn("无法获取 FLV 流地址: {}", deviceSn);
                    clientSession.close(CloseStatus.NOT_ACCEPTABLE);
                    return;
                }
                log.info("连接上游视频流: {}", flvUrl);

                wsClient.execute(new AbstractWebSocketHandler() {
                    @Override
                    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) throws Exception {
                        if (clientSession.isOpen()) {
                            clientSession.sendMessage(new BinaryMessage(message.getPayload()));
                        }
                    }

                    @Override
                    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
                        if (clientSession.isOpen()) {
                            clientSession.close(status);
                        }
                    }
                }, flvUrl).whenComplete((session, throwable) -> {
                    if (throwable != null) {
                        log.warn("上游视频流连接失败: {} - {}", deviceSn, throwable.getMessage());
                        try { clientSession.close(CloseStatus.SERVER_ERROR); } catch (IOException e) {}
                    } else {
                        clientSession.getAttributes().put("upstream", session);
                    }
                });
            } catch (Exception e) {
                log.warn("连接上游视频流异常: {} - {}", deviceSn, e.getMessage());
                try { clientSession.close(CloseStatus.SERVER_ERROR); } catch (IOException ex) {}
            }
        }

        private String getFlvStreamUrl(String deviceSn) {
            try {
                var devices = droneProxyService.listDevices(droneApiProperties.getFixedWorkspaceId(), 1, 100);
                for (var device : devices.items()) {
                    String sn = Optional.ofNullable(device.get("deviceSn")).map(Object::toString).orElse("");
                    String childSn = Optional.ofNullable(device.get("childSn")).map(Object::toString).orElse("");
                    if (sn.equals(deviceSn) || childSn.equals(deviceSn)) {
                        // 优先 videoPlayUrlInner (FLV over WebSocket)
                        String url = extractVideoUrl(device, "videoPlayUrlInner");
                        if (url == null) url = extractVideoUrl(device, "videoPlayUrl");
                        if (url == null) {
                            Object droneInfo = device.get("droneInfo");
                            if (droneInfo instanceof Map) {
                                url = extractVideoUrl((Map<String, Object>) droneInfo, "videoPlayUrlInner");
                                if (url == null) url = extractVideoUrl((Map<String, Object>) droneInfo, "videoPlayUrl");
                            }
                        }
                        return url;
                    }
                }
            } catch (Exception e) {
                log.warn("获取 FLV 流地址失败: {}", e.getMessage());
            }
            return null;
        }

        @SuppressWarnings("unchecked")
        private String extractVideoUrl(Map<String, Object> device, String key) {
            Object videoUrlObj = device.get(key);
            if (videoUrlObj instanceof java.util.List) {
                var videoList = (java.util.List<Map<String, Object>>) videoUrlObj;
                if (!videoList.isEmpty()) {
                    Object innerList = videoList.get(0).get("videoList");
                    if (innerList instanceof java.util.List) {
                        var inner = (java.util.List<Map<String, Object>>) innerList;
                        if (!inner.isEmpty()) {
                            return (String) inner.get(0).get("playUrl");
                        }
                    }
                }
            }
            return null;
        }

        private WebSocketSession getUpstream(WebSocketSession clientSession) {
            return (WebSocketSession) clientSession.getAttributes().get("upstream");
        }

        private void closeUpstream(WebSocketSession clientSession) {
            WebSocketSession upstream = getUpstream(clientSession);
            if (upstream != null) {
                try { upstream.close(); } catch (IOException e) {}
            }
        }
    }

    // ========== 遥测数据 WebSocket ==========

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
        public boolean beforeHandshake(ServerHttpRequest request, org.springframework.http.server.ServerHttpResponse response,
                                       WebSocketHandler wsHandler, Map<String, Object> attributes) {
            try {
                if (!(request instanceof ServletServerHttpRequest servletRequest)) {
                    log.error("WebSocket 握手失败: 非 HTTP 请求");
                    return false;
                }
                HttpServletRequest httpRequest = servletRequest.getServletRequest();
                String token = resolveToken(httpRequest);
                AuthenticatedUser tokenUser = jwtTokenService.parseAuthenticatedUser(token);
                AuthenticatedUser currentUser = authService.loadAuthenticatedUser(tokenUser.id(), tokenUser.clientType());
                if (!AuthService.ClientType.WEB.name().equals(currentUser.clientType())) {
                    log.error("WebSocket 握手失败: 客户端类型不匹配, {}", currentUser.clientType());
                    return false;
                }
                String deviceSn = httpRequest.getParameter("deviceSn");
                if (deviceSn == null || deviceSn.isBlank()) {
                    log.error("WebSocket 握手失败: 设备序列号为空");
                    return false;
                }
                attributes.put("deviceSn", deviceSn);
                attributes.put("user", currentUser);
                log.info("WebSocket 握手成功: deviceSn={}, user={}", deviceSn, currentUser.account());
                return true;
            } catch (Exception e) {
                log.error("WebSocket 握手异常: {}", e.getMessage(), e);
                return false;
            }
        }

        @Override
        public void afterHandshake(ServerHttpRequest request, org.springframework.http.server.ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Exception exception) {
        }

        private String resolveToken(HttpServletRequest request) {
            String authorization = request.getHeader(org.springframework.http.HttpHeaders.AUTHORIZATION);
            if (authorization != null && authorization.startsWith("Bearer ")) {
                String token = authorization.substring(7).trim();
                if (!token.isEmpty()) return token;
            }
            String queryToken = request.getParameter("token");
            if (queryToken != null && !queryToken.isBlank()) return queryToken.trim();
            throw new BusinessException("AUTH_TOKEN_REQUIRED", "请提供认证令牌");
        }
    }
}
