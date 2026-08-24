package com.changping.platform.modules.auth.service;

import com.changping.platform.common.exception.BusinessException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * 微信小程序能力服务：
 * 1. access_token 获取与 Redis 缓存（有效期 7200s，提前 200s 过期）
 * 2. 手机号快速验证：getPhoneNumber 按钮返回的 code → 真实手机号（需企业/组织主体认证）
 * 配置来自环境变量 WECHAT_APPID / WECHAT_APPSECRET，未配置时接口返回明确提示
 */
@Service
public class WechatService {

    private static final String TOKEN_URL = "https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential&appid=%s&secret=%s";
    private static final String PHONE_URL = "https://api.weixin.qq.com/wxa/business/getuserphonenumber?access_token=%s";
    private static final String TOKEN_CACHE_KEY = "wechat:access_token";

    @Value("${wechat.appid:}")
    private String appId;

    @Value("${wechat.secret:}")
    private String appSecret;

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    public WechatService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    private boolean configured() {
        return appId != null && !appId.isBlank() && appSecret != null && !appSecret.isBlank();
    }

    /** 获取微信 access_token（Redis 缓存 7000 秒，避免频繁请求） */
    public String getAccessToken() {
        String cached = redisTemplate.opsForValue().get(TOKEN_CACHE_KEY);
        if (cached != null && !cached.isBlank()) {
            return cached;
        }
        if (!configured()) {
            throw new BusinessException("WECHAT_NOT_CONFIGURED", "微信登录未配置（缺少 WECHAT_APPID / WECHAT_APPSECRET），请联系管理员");
        }
        try {
            String url = String.format(TOKEN_URL, appId, appSecret);
            HttpResponse<String> resp = httpClient.send(
                    HttpRequest.newBuilder(URI.create(url)).GET().timeout(Duration.ofSeconds(10)).build(),
                    HttpResponse.BodyHandlers.ofString());
            JsonNode node = objectMapper.readTree(resp.body());
            String token = node.path("access_token").asText(null);
            if (token == null || token.isBlank()) {
                throw new BusinessException("WECHAT_TOKEN_FAILED", "微信接口鉴权失败：" + node.path("errmsg").asText("未知错误"));
            }
            redisTemplate.opsForValue().set(TOKEN_CACHE_KEY, token, Duration.ofSeconds(7000));
            return token;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("WECHAT_TOKEN_FAILED", "获取微信 access_token 失败：" + e.getMessage());
        }
    }

    /**
     * 手机号快速验证：getPhoneNumber 按钮返回的 code 换取真实手机号
     * code 仅 5 分钟有效且只能使用一次
     */
    public String getPhoneNumber(String code) {
        if (code == null || code.isBlank()) {
            throw new BusinessException("WECHAT_CODE_REQUIRED", "缺少微信授权 code");
        }
        String token = getAccessToken();
        try {
            String body = "{\"code\":\"" + code + "\"}";
            HttpResponse<String> resp = httpClient.send(
                    HttpRequest.newBuilder(URI.create(String.format(PHONE_URL, token)))
                            .header("Content-Type", "application/json")
                            .POST(HttpRequest.BodyPublishers.ofString(body))
                            .timeout(Duration.ofSeconds(10))
                            .build(),
                    HttpResponse.BodyHandlers.ofString());
            JsonNode node = objectMapper.readTree(resp.body());
            if (node.path("errcode").asInt(0) != 0) {
                throw new BusinessException("WECHAT_PHONE_FAILED", "微信手机号授权失败：" + node.path("errmsg").asText("未知错误"));
            }
            String phone = node.path("phone_info").path("phoneNumber").asText(null);
            if (phone == null || phone.isBlank()) {
                throw new BusinessException("WECHAT_PHONE_FAILED", "微信未返回手机号，请重试");
            }
            return phone;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("WECHAT_PHONE_FAILED", "微信手机号授权失败：" + e.getMessage());
        }
    }
}
