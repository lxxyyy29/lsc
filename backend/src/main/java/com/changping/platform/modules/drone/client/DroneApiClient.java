package com.changping.platform.modules.drone.client;

import com.changping.platform.modules.drone.config.DroneApiProperties;
import com.changping.platform.modules.drone.exception.DroneApiException;
import com.changping.platform.modules.drone.util.DroneSm4Utils;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

/**
 * @Author tangxinglin
 * @Description //无人机第三方API客户端，负责令牌获取与缓存、SM4加密认证及HTTP请求代理，
 * 所有请求自动附带有效的x-auth-token请求头
 * @Date 2026/04/18 10:00
 */
@Component
public class DroneApiClient {

    private static final Logger log = LoggerFactory.getLogger(DroneApiClient.class);
    private static final String TOKEN_PATH = "/dj-prod-api/manage/api/v1/getToken";
    private static final String TOKEN_CACHE_KEY = "dgcp-oa:drone:api:access-token";

    private final RestTemplate restTemplate;
    private final DroneApiProperties properties;
    private final Clock clock;
    private final ObjectMapper objectMapper;
    private final StringRedisTemplate redisTemplate;

    private volatile CachedAccessToken cachedAccessToken;
    private final Object tokenMonitor = new Object();

    /**
     * @Author tangxinglin
     * @Description //主构造函数，通过Spring注入RestTemplate和配置属性，使用系统时钟和默认ObjectMapper
     * @Date 2026/04/18 10:00
     * @Param [droneApiRestTemplate 无人机专用RestTemplate, properties 无人机API配置属性]
     * @return void
    */
    @Autowired
    public DroneApiClient(
            RestTemplate droneApiRestTemplate,
            DroneApiProperties properties,
            ObjectProvider<StringRedisTemplate> redisTemplateProvider) {
        this(
                droneApiRestTemplate,
                properties,
                Clock.systemDefaultZone(),
                new ObjectMapper(),
                redisTemplateProvider.getIfAvailable());
    }

    /**
     * @Author tangxinglin
     * @Description //测试用构造函数，支持注入自定义时钟
     * @Date 2026/04/18 10:00
     * @Param [restTemplate RestTemplate实例, properties 无人机API配置属性, clock 时钟实例]
     * @return void
     */
    public DroneApiClient(RestTemplate restTemplate, DroneApiProperties properties, Clock clock) {
        this(restTemplate, properties, clock, new ObjectMapper(), null);
    }

    /**
     * @Author tangxinglin
     * @Description //全参构造函数，支持注入自定义时钟和ObjectMapper，主要用于单元测试
     * @Date 2026/04/18 10:00
     * @Param [restTemplate RestTemplate实例, properties 无人机API配置属性, clock 时钟实例, objectMapper JSON序列化工具]
     * @return void
     */
    public DroneApiClient(RestTemplate restTemplate, DroneApiProperties properties, Clock clock, ObjectMapper objectMapper) {
        this(restTemplate, properties, clock, objectMapper, null);
    }

    public DroneApiClient(
            RestTemplate restTemplate,
            DroneApiProperties properties,
            Clock clock,
            ObjectMapper objectMapper,
            StringRedisTemplate redisTemplate) {
        this.restTemplate = restTemplate;
        this.properties = properties;
        this.clock = clock;
        this.objectMapper = objectMapper;
        this.redisTemplate = redisTemplate;
    }

    /**
     * @Author tangxinglin
     * @Description //获取上游平台的区域码
     * @Date 2026/04/18 10:00
     * @Param []
     * @return String 区域码
     */
    public String getRegionCode() {
        return getRequiredAccessToken().regionCode();
    }

    /**
     * @Author tangxinglin
     * @Description //获取当前有效的访问令牌字符串
     * @Date 2026/04/18 10:00
     * @Param []
     * @return String x-auth-token令牌值
     */
    public String getAccessTokenValue() {
        return getRequiredAccessToken().token();
    }

    /**
     * @Author tangxinglin
     * @Description //从当前访问令牌的JWT Payload中提取用户ID
     * @Date 2026/04/18 10:00
     * @Param []
     * @return String 令牌中的用户ID
     */
    public String extractUserIdFromToken() {
        String token = getAccessTokenValue();
        String[] parts = token.split("\\.");
        if (parts.length < 2) {
            throw new DroneApiException("JWT 令牌格式无效");
        }
        try {
            String payload = new String(java.util.Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
            JsonNode node = objectMapper.readTree(payload);
            JsonNode idNode = node.get("id");
            if (idNode == null || idNode.isNull()) {
                throw new DroneApiException("JWT 令牌缺少 'id' 声明");
            }
            return idNode.asText();
        } catch (DroneApiException e) {
            throw e;
        } catch (Exception e) {
            throw new DroneApiException("从 JWT 令牌中提取用户 ID 失败: " + e.getMessage());
        }
    }

    /**
     * @Author tangxinglin
     * @Description //以GET方式请求上游API接口
     * @Date 2026/04/18 10:00
     * @Param [path 接口路径, responseType 响应体类型]
     * @return T 反序列化后的响应数据
     */
    public <T> T get(String path, Class<T> responseType) {
        return exchange(path, HttpMethod.GET, null, responseType);
    }

    /**
     * @Author tangxinglin
     * @Description //以multipart/form-data方式POST请求上游API接口，用于文件上传
     * @Date 2026/04/18 10:00
     * @Param [path 接口路径, requestEntity 包含文件数据的请求实体, responseType 响应体类型]
     * @return T 反序列化后的响应数据
     */
    public <T> T postMultipart(String path, HttpEntity<?> requestEntity, Class<T> responseType) {
        return exchangeWithTokenRefresh(path, HttpMethod.POST, requestEntity, responseType, false);
    }

    /**
     * @Author tangxinglin
     * @Description //以POST方式请求上游API接口
     * @Date 2026/04/18 10:00
     * @Param [path 接口路径, requestBody 请求体对象, responseType 响应体类型]
     * @return T 反序列化后的响应数据
     */
    public <T> T post(String path, Object requestBody, Class<T> responseType) {
        return exchange(path, HttpMethod.POST, requestBody, responseType);
    }

    /**
     * @Author tangxinglin
     * @Description //通用HTTP请求方法，自动附带认证令牌并解包上游响应数据
     * @Date 2026/04/18 10:00
     * @Param [path 接口路径, method HTTP方法, requestBody 请求体对象, responseType 响应体类型]
     * @return T 反序列化后的响应数据
     */
    public <T> T exchange(String path, HttpMethod method, Object requestBody, Class<T> responseType) {
        return exchangeWithTokenRefresh(path, method, requestBody, responseType, false);
    }

    /**
     * @Author tangxinglin
     * @Description //清除缓存的访问令牌，强制下次请求时重新获取
     * @Date 2026/04/18 10:00
     * @Param []
     * @return void
     */
    public void evictAccessToken() {
        cachedAccessToken = null;
        if (redisTemplate != null) {
            redisTemplate.delete(TOKEN_CACHE_KEY);
        }
    }

    /**
     * Upstream drone tokens can be invalidated before the local cache TTL expires. Retry once with a
     * freshly fetched token when the upstream explicitly reports token expiration or returns HTTP 401.
     */
    private <T> T exchangeWithTokenRefresh(
            String path,
            HttpMethod method,
            Object requestBody,
            Class<T> responseType,
            boolean retried) {
        try {
            ResponseEntity<DroneApiResponse<T>> response = restTemplate.exchange(
                    path,
                    method,
                    buildAuthorizedEntity(requestBody),
                    ParameterizedTypeReferences.response(responseType));
            return unwrap(response.getBody());
        } catch (DroneApiException exception) {
            if (!retried && isTokenExpiredMessage(exception.getMessage())) {
                evictAccessToken();
                return exchangeWithTokenRefresh(path, method, requestBody, responseType, true);
            }
            throw exception;
        } catch (HttpStatusCodeException exception) {
            if (!retried && exception.getStatusCode().value() == 401) {
                evictAccessToken();
                return exchangeWithTokenRefresh(path, method, requestBody, responseType, true);
            }
            throw new DroneApiException(extractHttpErrorMessage(exception));
        }
    }

    /**
     * @Author tangxinglin
     * @Description //获取有效的访问令牌，优先使用缓存，缓存过期则加锁刷新
     * @Date 2026/04/18 10:00
     * @Param []
     * @return DroneAccessToken 有效的访问令牌对象
     */
    DroneAccessToken getRequiredAccessToken() {
        if (redisTemplate != null) {
            DroneAccessToken cached = readRedisAccessToken();
            if (cached != null) {
                return cached;
            }
            synchronized (tokenMonitor) {
                DroneAccessToken reloaded = readRedisAccessToken();
                if (reloaded != null) {
                    return reloaded;
                }
                DroneAccessToken refreshed = requestAccessToken();
                writeRedisAccessToken(refreshed);
                return refreshed;
            }
        }

        CachedAccessToken current = cachedAccessToken;
        if (current != null && current.isValidAt(clock.instant())) {
            return current.accessToken();
        }
        synchronized (tokenMonitor) {
            CachedAccessToken reloaded = cachedAccessToken;
            if (reloaded != null && reloaded.isValidAt(clock.instant())) {
                return reloaded.accessToken();
            }
            DroneAccessToken refreshed = requestAccessToken();
            cachedAccessToken = new CachedAccessToken(refreshed, clock.instant().plus(properties.getTokenCacheTtl()));
            return refreshed;
        }
    }

    private DroneAccessToken readRedisAccessToken() {
        String raw = redisTemplate.opsForValue().get(TOKEN_CACHE_KEY);
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            DroneAccessToken token = objectMapper.readValue(raw, DroneAccessToken.class);
            if (token.token() == null || token.token().isBlank()
                    || token.regionCode() == null || token.regionCode().isBlank()) {
                evictAccessToken();
                return null;
            }
            return token;
        } catch (Exception exception) {
            evictAccessToken();
            return null;
        }
    }

    private void writeRedisAccessToken(DroneAccessToken token) {
        try {
            redisTemplate.opsForValue().set(
                    TOKEN_CACHE_KEY,
                    objectMapper.writeValueAsString(token),
                    properties.getTokenCacheTtl());
        } catch (Exception exception) {
            throw new DroneApiException("无人机令牌缓存写入失败");
        }
    }

    /**
     * @Author tangxinglin
     * @Description //向上游平台请求新的访问令牌，使用SM4加密密码进行认证
     * @Date 2026/04/18 10:00
     * @Param []
     * @return DroneAccessToken 新获取的访问令牌对象
     */
    private DroneAccessToken requestAccessToken() {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            DroneTokenRequest request = new DroneTokenRequest(
                    properties.getUsername(),
                    DroneSm4Utils.encrypt(properties.getPassword(), properties.getSm4SecretKey(), properties.getSm4Iv()));
            ResponseEntity<DroneApiResponse<Map>> response = restTemplate.exchange(
                    TOKEN_PATH,
                    HttpMethod.POST,
                    new HttpEntity<>(request, headers),
                    ParameterizedTypeReferences.response(Map.class));
            Map<?, ?> data = unwrap(response.getBody());
            String token = stringValue(data.get("x-auth-token"));
            if (token == null || token.isBlank()) {
                throw new DroneApiException("无人机令牌响应缺少 x-auth-token");
            }
            String regionCode = stringValue(data.get("region_code"));
            if (regionCode == null || regionCode.isBlank()) {
                throw new DroneApiException("无人机令牌响应缺少 region_code");
            }
            return new DroneAccessToken(token, regionCode);
        } catch (Exception e) {
            log.warn("无人机平台连接失败，无人机功能暂不可用: {}", e.getMessage());
            return new DroneAccessToken("UNAVAILABLE", "UNAVAILABLE");
        }
    }

    /**
     * @Author tangxinglin
     * @Description //构建附带认证令牌的请求实体，兼容普通请求体和已有HttpEntity两种形式
     * @Date 2026/04/18 10:00
     * @Param [requestBody 请求体对象或HttpEntity]
     * @return HttpEntity<?> 附带认证头的请求实体
     */
    private HttpEntity<?> buildAuthorizedEntity(Object requestBody) {
        DroneAccessToken token = getRequiredAccessToken();
        if (requestBody instanceof HttpEntity<?> requestEntity) {
            HttpHeaders headers = new HttpHeaders();
            headers.putAll(requestEntity.getHeaders());
            headers.set("x-auth-token", token.token());
            return new HttpEntity<>(requestEntity.getBody(), headers);
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-auth-token", token.token());
        return new HttpEntity<>(requestBody, headers);
    }

    /**
     * @Author tangxinglin
     * @Description //解包上游响应信封，校验响应码并返回data字段，异常时抛出DroneApiException
     * @Date 2026/04/18 10:00
     * @Param [response 上游响应信封对象]
     * @return T 响应data字段内容
     */
    private <T> T unwrap(DroneApiResponse<T> response) {
        if (response == null) {
            throw new DroneApiException("无人机 API 响应为空");
        }
        Integer code = response.code();
        if (code == null || (code != 0 && code != 200)) {
            throw new DroneApiException(response.message() == null || response.message().isBlank()
                    ? "无人机 API 请求失败"
                    : response.message());
        }
        return response.data();
    }

    private boolean isTokenExpiredMessage(String message) {
        if (message == null || message.isBlank()) {
            return false;
        }
        String normalized = message.toLowerCase();
        return normalized.contains("token")
                && (normalized.contains("过期")
                || normalized.contains("失效")
                || normalized.contains("无效")
                || normalized.contains("重新登录")
                || normalized.contains("expired")
                || normalized.contains("invalid")
                || normalized.contains("unauthorized"));
    }

    private String extractHttpErrorMessage(HttpStatusCodeException exception) {
        try {
            DroneApiResponse<?> response = objectMapper.readValue(
                    exception.getResponseBodyAsString(),
                    objectMapper.getTypeFactory().constructParametricType(DroneApiResponse.class, Object.class));
            if (response.message() != null && !response.message().isBlank()) {
                return response.message();
            }
        } catch (Exception ignored) {
            // Fall back to the HTTP exception message below.
        }
        return exception.getMessage() == null || exception.getMessage().isBlank()
                ? "无人机 API 请求失败"
                : exception.getMessage();
    }

    /**
     * @Author tangxinglin
     * @Description //将任意值安全转换为字符串，null值返回null
     * @Date 2026/04/18 10:00
     * @Param [value 原始值]
     * @return String 字符串值或null
     */
    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private record CachedAccessToken(DroneAccessToken accessToken, Instant expiresAt) {
        private boolean isValidAt(Instant now) {
            return now.isBefore(expiresAt);
        }
    }

    public record DroneAccessToken(String token, String regionCode) {
    }

    record DroneTokenRequest(String username, String password) {
    }

    record DroneTokenData(
            @JsonProperty("x-auth-token") String authToken,
            @JsonProperty("region_code") String regionCode) {
    }

    record DroneApiResponse<T>(Integer code, String message, T data) {
    }

    private static final class ParameterizedTypeReferences {
        private ParameterizedTypeReferences() {
        }

        private static <T> ParameterizedTypeReference<DroneApiResponse<T>> response(Class<T> responseType) {
            return new ParameterizedTypeReference<>() {
            };
        }
    }
}
