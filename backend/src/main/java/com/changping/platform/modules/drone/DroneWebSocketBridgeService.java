package com.changping.platform.modules.drone;

import com.changping.platform.modules.drone.client.DroneApiClient;
import com.changping.platform.modules.drone.config.DroneApiProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/**
 * @Author lxy
 * @Description //无人机WebSocket桥接服务，维护本平台前端与上游无人机平台WebSocket的双向代理连接，
 * 过滤指定业务码消息并广播给注册的前端客户端，支持断线自动重连
 * @Date 2026/04/18 10:00
 */
@Service
public class DroneWebSocketBridgeService {

    private static final Logger log = LoggerFactory.getLogger(DroneWebSocketBridgeService.class);
    private static final Set<String> ALLOWED_BIZ_CODES = Set.of("dock_osd", "device_osd", "device_offline", "Flyline_Alg_Task_Staus");

    private final DroneApiClient droneApiClient;
    private final DroneApiProperties droneApiProperties;
    private final ObjectMapper objectMapper;
    private final StandardWebSocketClient webSocketClient = new StandardWebSocketClient();
    private final ScheduledExecutorService reconnectExecutor = Executors.newSingleThreadScheduledExecutor();
    private final Map<String, UpstreamConnection> upstreamConnections = new ConcurrentHashMap<>();

    /**
     * @Author lxy
     * @Description //构造函数，注入无人机API客户端、API配置属性及JSON序列化工具
     * @Date 2026/04/18 10:00
     * @Param [droneApiClient 无人机API客户端, droneApiProperties 无人机API配置, objectMapper JSON序列化工具]
     * @return void
     */
    public DroneWebSocketBridgeService(
            DroneApiClient droneApiClient,
            DroneApiProperties droneApiProperties,
            ObjectMapper objectMapper) {
        this.droneApiClient = droneApiClient;
        this.droneApiProperties = droneApiProperties;
        this.objectMapper = objectMapper;
    }

    /**
     * @Author lxy
     * @Description //销毁时关闭重连调度器并断开所有上游连接
     * @Date 2026/04/18 10:00
     * @Param []
     * @return void
     */
    @PreDestroy
    public void shutdown() {
        reconnectExecutor.shutdownNow();
        upstreamConnections.values().forEach(conn -> closeQuietly(conn.upstreamSession()));
        upstreamConnections.clear();
    }

    /**
     * @Author lxy
     * @Description //注册前端客户端会话到指定设备的上游连接，若上游未连接则发起连接
     * @Date 2026/04/18 10:00
     * @Param [deviceSn 设备序列号, clientSession 前端WebSocket会话]
     * @return void
     */
    public void registerClient(String deviceSn, WebSocketSession clientSession) {
        upstreamConnections.compute(deviceSn, (key, existing) -> {
            UpstreamConnection connection = existing == null ? new UpstreamConnection(deviceSn) : existing;
            connection.clients().add(clientSession);
            if (!connection.connecting() && (connection.upstreamSession() == null || !connection.upstreamSession().isOpen())) {
                connectUpstream(connection);
            }
            return connection;
        });
    }

    /**
     * @Author lxy
     * @Description //注销前端客户端会话，若该设备无客户端连接则关闭上游连接
     * @Date 2026/04/18 10:00
     * @Param [deviceSn 设备序列号, clientSession 要注销的前端WebSocket会话]
     * @return void
     */
    public void unregisterClient(String deviceSn, WebSocketSession clientSession) {
        upstreamConnections.computeIfPresent(deviceSn, (key, connection) -> {
            connection.clients().remove(clientSession);
            if (connection.clients().isEmpty()) {
                closeQuietly(connection.upstreamSession());
                return null;
            }
            return connection;
        });
    }

    /**
     * @Author lxy
     * @Description //发起与上游无人机平台的WebSocket连接，连接失败时安排重连
     * @Date 2026/04/18 10:00
     * @Param [connection 上游连接对象]
     * @return void
     */
    private void connectUpstream(UpstreamConnection connection) {
        connection.setConnecting(true);
        String token = droneApiClient.getAccessTokenValue();
        String uri = connection.buildUri(token);
        log.info("连接无人机平台 WebSocket: deviceSn={}, uri={}", connection.deviceSn(),
                uri.substring(0, Math.min(uri.length(), 120)) + "...");
        webSocketClient.execute(new UpstreamHandler(connection), uri)
                .whenComplete((session, throwable) -> {
                    connection.setConnecting(false);
                    if (throwable != null) {
                        log.warn("连接无人机平台 WebSocket 失败: deviceSn={}, error={}", connection.deviceSn(), throwable.getMessage());
                        scheduleReconnect(connection.deviceSn());
                    } else {
                        log.info("无人机平台 WebSocket 连接成功: deviceSn={}", connection.deviceSn());
                        connection.setUpstreamSession(session);
                    }
                });
    }

    /**
     * @Author lxy
     * @Description //延迟3秒后重新尝试建立上游WebSocket连接
     * @Date 2026/04/18 10:00
     * @Param [deviceSn 设备序列号]
     * @return void
     */
    private void scheduleReconnect(String deviceSn) {
        reconnectExecutor.schedule(() -> {
            try {
                upstreamConnections.computeIfPresent(deviceSn, (key, connection) -> {
                    if (!connection.clients().isEmpty()
                            && (connection.upstreamSession() == null || !connection.upstreamSession().isOpen())
                            && !connection.connecting()) {
                        connectUpstream(connection);
                    }
                    return connection;
                });
            } catch (Exception e) {
                log.warn("Reconnect attempt failed for device {}", deviceSn, e);
            }
        }, 3, TimeUnit.SECONDS);
    }

    /**
     * @Author lxy
     * @Description //将上游消息广播给所有已注册的前端客户端，统一将bizCode字段名归一化为biz_code
     * @Date 2026/04/18 10:00
     * @Param [connection 上游连接对象, payload 消息内容]
     * @return void
     */
    private void broadcast(UpstreamConnection connection, String payload) {
        // Normalize upstream "bizCode" (camelCase) to "biz_code" (snake_case) for frontend
        String normalizedPayload = payload;
        try {
            JsonNode node = objectMapper.readTree(payload);
            if (node.has("bizCode") && !node.has("biz_code")) {
                var objectNode = (com.fasterxml.jackson.databind.node.ObjectNode) node;
                objectNode.set("biz_code", objectNode.get("bizCode"));
                normalizedPayload = objectMapper.writeValueAsString(objectNode);
            }
        } catch (IOException ignored) {
        }
        TextMessage msg = new TextMessage(normalizedPayload);
        for (WebSocketSession client : connection.clients()) {
            if (client.isOpen()) {
                try {
                    // Spring WebSocketSession is not thread-safe for concurrent sends;
                    // synchronize per-session to prevent frame corruption
                    synchronized (client) {
                        client.sendMessage(msg);
                    }
                } catch (IOException exception) {
                    log.warn("Failed to forward drone websocket message to client {}", client.getId(), exception);
                }
            }
        }
    }

    /**
     * @Author lxy
     * @Description //判断消息是否包含允许转发的业务码（biz_code）
     * @Date 2026/04/18 10:00
     * @Param [payload 原始消息字符串]
     * @return boolean 若业务码在白名单中则返回true
     */
    private boolean shouldForward(String payload) {
        try {
            JsonNode node = objectMapper.readTree(payload);
            // Upstream sends "bizCode" (camelCase), normalize to "biz_code" for downstream
            JsonNode bizCodeNode = node.get("biz_code");
            if (bizCodeNode == null) {
                bizCodeNode = node.get("bizCode");
            }
            return bizCodeNode != null && ALLOWED_BIZ_CODES.contains(bizCodeNode.asText());
        } catch (IOException exception) {
            return false;
        }
    }

    /**
     * @Author lxy
     * @Description //静默关闭WebSocket会话，忽略关闭时的IO异常
     * @Date 2026/04/18 10:00
     * @Param [session 要关闭的WebSocket会话]
     * @return void
     */
    private void closeQuietly(WebSocketSession session) {
        if (session == null) {
            return;
        }
        try {
            session.close();
        } catch (IOException ignored) {
        }
    }

    private final class UpstreamHandler extends TextWebSocketHandler {

        private final UpstreamConnection connection;

        private UpstreamHandler(UpstreamConnection connection) {
            this.connection = connection;
        }

        @Override
        public void afterConnectionEstablished(WebSocketSession session) throws Exception {
            log.info("Upstream drone websocket connected for device {}", connection.deviceSn());
            // Upstream uses STOMP over WebSocket — send CONNECT frame to complete handshake
            // Enable bidirectional heartbeat (10s) to detect stale/half-open connections
            String connectFrame = "CONNECT\naccept-version:1.1,1.2\nheart-beat:10000,10000\n\n\0";
            session.sendMessage(new TextMessage(connectFrame));
        }

        @Override
        public void handleTextMessage(WebSocketSession session, TextMessage message) {
            String payload = message.getPayload();
            // Skip STOMP heartbeat frames (single newline or empty)
            if (payload.isEmpty() || payload.equals("\n") || payload.equals("\r\n")) {
                return;
            }
            // Skip STOMP protocol frames (CONNECTED, ERROR, etc.)
            if (payload.startsWith("CONNECTED") || payload.startsWith("ERROR") || payload.startsWith("RECEIPT")) {
                log.debug("Received STOMP control frame for device {}: {}", connection.deviceSn(),
                        payload.length() > 80 ? payload.substring(0, 80) + "..." : payload);
                return;
            }
            // Handle STOMP MESSAGE frames — extract JSON body after the double newline
            if (payload.startsWith("MESSAGE")) {
                int bodyStart = payload.indexOf("\n\n");
                if (bodyStart >= 0) {
                    payload = payload.substring(bodyStart + 2);
                    // Remove STOMP null terminator if present
                    if (payload.endsWith("\0")) {
                        payload = payload.substring(0, payload.length() - 1);
                    }
                }
            }
            // Forward if it contains a recognized biz code
            if (shouldForward(payload)) {
                broadcast(connection, payload);
            }
        }

        @Override
        public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
            log.info("Upstream drone websocket closed for device {} with status {}", connection.deviceSn(), status);
            connection.setUpstreamSession(null);
            if (!connection.clients().isEmpty()) {
                scheduleReconnect(connection.deviceSn());
            }
        }

        @Override
        public void handleTransportError(WebSocketSession session, Throwable exception) {
            log.warn("Drone upstream websocket transport error for device {}", connection.deviceSn(), exception);
        }
    }

    private final class UpstreamConnection {
        private final String deviceSn;
        private final Set<WebSocketSession> clients = ConcurrentHashMap.newKeySet();
        private volatile WebSocketSession upstreamSession;
        private volatile boolean connecting;

        private UpstreamConnection(String deviceSn) {
            this.deviceSn = deviceSn;
        }

        public String deviceSn() {
            return deviceSn;
        }

        public Set<WebSocketSession> clients() {
            return clients;
        }

        public WebSocketSession upstreamSession() {
            return upstreamSession;
        }

        public void setUpstreamSession(WebSocketSession upstreamSession) {
            this.upstreamSession = upstreamSession;
        }

        public boolean connecting() {
            return connecting;
        }

        public void setConnecting(boolean connecting) {
            this.connecting = connecting;
        }

        public String buildUri(String token) {
            return droneApiProperties.getWsAddr() + "/dj-prod-api/api/v1/ws?x-auth-token=" + token + "&device_sn=" + deviceSn;
        }
    }
}
