package com.changping.platform.modules.drone.controller;

import com.changping.platform.common.exception.BusinessException;
import com.changping.platform.common.response.ApiResponse;
import com.changping.platform.modules.auth.security.PermissionCodes;
import com.changping.platform.modules.auth.security.PermissionGuard;
import com.changping.platform.modules.auth.service.AuthService;
import com.changping.platform.modules.auth.service.CurrentUserService;
import com.changping.platform.modules.drone.DroneProxyService;
import com.changping.platform.modules.drone.config.DroneApiProperties;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 无人机实时视频流代理控制器
 * 浏览器 → 后端代理 → 无人机平台（解决SSL/CORS/签名过期问题）
 */
@RestController
@RequestMapping("/drone/stream")
public class DroneStreamController {

    private static final Logger log = LoggerFactory.getLogger(DroneStreamController.class);
    private static final Pattern TS_PATTERN = Pattern.compile("(.+)\\.ts");

    private final DroneProxyService droneProxyService;
    private final DroneApiProperties droneApiProperties;
    private final CurrentUserService currentUserService;
    private final PermissionGuard permissionGuard;

    // 缓存每个设备的最新签名URL
    private final ConcurrentHashMap<String, StreamCache> streamCache = new ConcurrentHashMap<>();

    public DroneStreamController(
            DroneProxyService droneProxyService,
            DroneApiProperties droneApiProperties,
            CurrentUserService currentUserService,
            PermissionGuard permissionGuard) {
        this.droneProxyService = droneProxyService;
        this.droneApiProperties = droneApiProperties;
        this.currentUserService = currentUserService;
        this.permissionGuard = permissionGuard;
    }

    /**
     * HLS 播放列表代理 - 前端连接此地址
     * 每次请求都获取最新签名的 playlist，并将 .ts 替换为代理地址
     */
    @GetMapping("/proxy/{deviceSn}/hls.m3u8")
    public void proxyHlsPlaylist(
            @PathVariable String deviceSn,
            HttpServletResponse response) throws IOException {
        requireDroneStreamPermission();
        response.setContentType("application/vnd.apple.mpegurl");
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");

        try {
            String latestUrl = getLatestHlsUrl(deviceSn);
            if (latestUrl == null) {
                response.sendError(404, "Stream not available");
                return;
            }
            validateProxyUrl(latestUrl);

            // 从平台获取 playlist
            String playlist = httpGetString(latestUrl);
            if (playlist == null) {
                response.sendError(502, "Failed to fetch playlist");
                return;
            }

            response.getWriter().write(rewritePlaylist(playlist, latestUrl, deviceSn));
        } catch (Exception e) {
            log.warn("代理playlist失败: {}", e.getMessage());
            response.sendError(502, "Proxy error");
        }
    }

    /**
     * TS 分片代理 - 转发视频片段
     */
    @GetMapping("/proxy/{deviceSn}/seg")
    public void proxySegment(@PathVariable String deviceSn, @RequestParam("u") String tsUrl,
                              HttpServletResponse response) throws IOException {
        requireDroneStreamPermission();
        response.setContentType("video/mp2t");
        response.setHeader("Cache-Control", "public, max-age=3600");

        try {
            // URL 可能是编码过的
            String decodedUrl = tsUrl;
            if (decodedUrl.startsWith("/")) {
                // 相对路径，拼接基础URL
                String baseUrl = getLatestHlsUrl(deviceSn);
                if (baseUrl != null) {
                    decodedUrl = getBaseUrl(baseUrl) + decodedUrl;
                }
            }

            // 如果签名过期，尝试用新签名替换
            if (decodedUrl.contains("sign=")) {
                String freshUrl = refreshSignature(decodedUrl, deviceSn);
                if (freshUrl != null) decodedUrl = freshUrl;
            }
            validateProxyUrl(decodedUrl);

            // 转发
            HttpURLConnection conn = (HttpURLConnection) new URL(decodedUrl).openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");

            int code = conn.getResponseCode();
            if (code != 200) {
                response.sendError(code);
                return;
            }

            try (InputStream in = new BufferedInputStream(conn.getInputStream());
                 OutputStream out = response.getOutputStream()) {
                byte[] buf = new byte[8192];
                int len;
                while ((len = in.read(buf)) != -1) {
                    out.write(buf, 0, len);
                }
                out.flush();
            }
        } catch (Exception e) {
            log.warn("代理TS失败: {}", e.getMessage());
            if (!response.isCommitted()) {
                response.sendError(502, "Segment proxy error");
            }
        }
    }

    /**
     * 获取最新 HLS URL
     */
    private String getLatestHlsUrl(String deviceSn) {
        try {
            DroneProxyService.PageResult<Map<String, Object>> devices =
                droneProxyService.listDevices(droneApiProperties.getFixedWorkspaceId(), 1, 100);

            for (Map<String, Object> device : devices.items()) {
                String sn = Optional.ofNullable(device.get("deviceSn")).map(Object::toString).orElse("");
                String childSn = Optional.ofNullable(device.get("childSn")).map(Object::toString).orElse("");

                if (sn.equals(deviceSn) || childSn.equals(deviceSn)) {
                    String hlsUrl = extractHlsUrl(device);
                    if (hlsUrl != null) {
                        return hlsUrl;
                    }
                }
            }
        } catch (Exception e) {
            log.warn("获取设备流URL失败: {}", e.getMessage());
        }
        return null;
    }

    /**
     * 刷新 URL 中的签名（从平台获取最新签名并替换）
     */
    private String refreshSignature(String oldUrl, String deviceSn) {
        String freshUrl = getLatestHlsUrl(deviceSn);
        if (freshUrl == null) return null;

        // 提取新签名（兼容 ?sign= 和 &sign=）
        Pattern p = Pattern.compile("[?&]sign=([^&]+)");
        Matcher m = p.matcher(freshUrl);
        if (m.find()) {
            String newSign = m.group(1);
            // 替换旧URL中的签名
            return oldUrl.replaceAll("[?&]sign=[^&]+", "&sign=" + newSign);
        }
        return null;
    }

    private String getBaseUrl(String url) {
        int idx = url.lastIndexOf('/');
        return idx > 0 ? url.substring(0, idx + 1) : url;
    }

    private String httpGetString(String urlStr) {
        try {
            validateProxyUrl(urlStr);
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");

            int code = conn.getResponseCode();
            if (code != 200) return null;

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\n");
                }
                return sb.toString();
            }
        } catch (Exception e) {
            log.warn("HTTP GET失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 从设备数据中提取 HLS URL（优先 DOCK 相机，其次无人机相机）
     */
    private String extractHlsUrl(Map<String, Object> device) {
        // 优先使用 DOCK 相机的视频流（设备顶层 videoUrl）
        String dockUrl = extractVideoUrlFromList(device, "videoUrl");
        if (dockUrl != null) return dockUrl;
        // 其次使用无人机相机的视频流
        Object droneInfoObj = device.get("droneInfo");
        if (droneInfoObj instanceof Map) {
            return extractVideoUrlFromList((Map<String, Object>) droneInfoObj, "videoUrl");
        }
        return null;
    }

    /**
     * 从设备数据中提取 WebSocket FLV 流地址（实时推送，不需要无人机起飞）
     */
    private String extractFlvUrl(Map<String, Object> device) {
        // 优先 droneInfo 的 videoPlayUrlInner
        Object droneInfoObj = device.get("droneInfo");
        if (droneInfoObj instanceof Map) {
            String url = extractVideoUrlFromList((Map<String, Object>) droneInfoObj, "videoPlayUrlInner");
            if (url != null) return url;
        }
        // 其次顶层 videoPlayUrlInner
        String topUrl = extractVideoUrlFromList(device, "videoPlayUrlInner");
        if (topUrl != null) return topUrl;
        // 其次 droneInfo 的 videoPlayUrl
        if (droneInfoObj instanceof Map) {
            return extractVideoUrlFromList((Map<String, Object>) droneInfoObj, "videoPlayUrl");
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private String extractVideoUrlFromList(Map<String, Object> device, String key) {
        Object videoUrlObj = device.get(key);
        if (videoUrlObj instanceof List) {
            List<Map<String, Object>> videoList = (List<Map<String, Object>>) videoUrlObj;
            if (!videoList.isEmpty()) {
                Object innerList = videoList.get(0).get("videoList");
                if (innerList instanceof List) {
                    List<Map<String, Object>> inner = (List<Map<String, Object>>) innerList;
                    if (!inner.isEmpty()) {
                        String url = (String) inner.get(0).get("playUrl");
                        return fixUrlSign(url);
                    }
                }
            }
        }
        return null;
    }

    /**
     * 修复平台返回的URL格式问题（?sign= → &sign=）
     * 平台返回的URL格式为: http://xxx/hls.m3u8?originTypeStr=rtmp_push?sign=xxx
     * 需要改为: http://xxx/hls.m3u8?originTypeStr=rtmp_push&sign=xxx
     */
    static String fixUrlSign(String url) {
        if (url == null) return null;
        int firstQ = url.indexOf('?');
        if (firstQ < 0) return url;
        int secondQ = url.indexOf('?', firstQ + 1);
        if (secondQ > 0) {
            return url.substring(0, secondQ) + '&' + url.substring(secondQ + 1);
        }
        return url;
    }

    /**
     * 获取设备 FLV 流地址（WebSocket 实时推送，不需要无人机起飞）
     * 前端用 flv.js 播放
     */
    @GetMapping("/flv/{deviceSn}")
    public ApiResponse<Map<String, Object>> getFlvUrl(@PathVariable String deviceSn) {
        requireDroneStreamPermission();
        try {
            DroneProxyService.PageResult<Map<String, Object>> devices =
                droneProxyService.listDevices(droneApiProperties.getFixedWorkspaceId(), 1, 100);

            for (Map<String, Object> device : devices.items()) {
                String sn = Optional.ofNullable(device.get("deviceSn")).map(Object::toString).orElse("");
                String childSn = Optional.ofNullable(device.get("childSn")).map(Object::toString).orElse("");

                if (sn.equals(deviceSn) || childSn.equals(deviceSn)) {
                    String flvUrl = extractFlvUrl(device);
                    if (flvUrl != null) {
                        Map<String, Object> result = new HashMap<>();
                        result.put("flvUrl", flvUrl);
                        result.put("type", "FLV");
                        return ApiResponse.ok(result);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("获取FLV流地址失败: {}", e.getMessage());
        }
        return ApiResponse.fail("NOT_FOUND", "未找到设备流地址");
    }

    private String rewritePlaylist(String playlist, String latestUrl, String deviceSn) {
        String proxyBase = "/api/drone/stream/proxy/" + deviceSn;
        StringBuilder modified = new StringBuilder();
        for (String line : playlist.split("\\R", -1)) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty() && !trimmed.startsWith("#") && trimmed.contains(".ts")) {
                String segmentUrl = trimmed.startsWith("http://") || trimmed.startsWith("https://")
                        ? trimmed
                        : getBaseUrl(latestUrl) + trimmed;
                modified.append(proxyBase)
                        .append("/seg?u=").append(URLEncoder.encode(segmentUrl, StandardCharsets.UTF_8))
                        .append("\n");
            } else {
                modified.append(line).append("\n");
            }
        }
        return modified.toString();
    }

    private void validateProxyUrl(String url) {
        try {
            URI uri = URI.create(url);
            String scheme = uri.getScheme();
            if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
                throw new BusinessException("DRONE_STREAM_URL_FORBIDDEN", "视频流地址协议不允许");
            }
            String host = uri.getHost();
            if (host == null || !allowedDroneHost(host)) {
                throw new BusinessException("DRONE_STREAM_URL_FORBIDDEN", "视频流地址域名不允许");
            }
            InetAddress address = InetAddress.getByName(host);
            if (address.isAnyLocalAddress() || address.isLoopbackAddress() || address.isLinkLocalAddress()
                    || address.isSiteLocalAddress() || address.isMulticastAddress()) {
                throw new BusinessException("DRONE_STREAM_URL_FORBIDDEN", "视频流地址不允许访问内网地址");
            }
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BusinessException("DRONE_STREAM_URL_INVALID", "视频流地址无效");
        }
    }

    private boolean allowedDroneHost(String host) {
        return host.equalsIgnoreCase(hostOf(droneApiProperties.getServerAddr()))
                || host.equalsIgnoreCase(hostOf(droneApiProperties.getWsAddr()));
    }

    private String hostOf(String url) {
        try {
            return URI.create(url).getHost();
        } catch (Exception exception) {
            return "";
        }
    }

    private void requireDroneStreamPermission() {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.API_DRONE_WS_CONNECT);
    }

    private record StreamCache(String hlsUrl, String wsUrl, long timestamp) {}
}
