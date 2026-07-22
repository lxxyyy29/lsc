package com.changping.platform.modules.integration.alarm.security;

import com.changping.platform.common.exception.BusinessException;
import com.changping.platform.modules.integration.alarm.config.AlarmIntegrationProperties;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * @Author tangxinglin
 * @Description //第三方回调验证器，根据配置支持令牌验证和HMAC-SHA256签名验证两种方式，
 * 未启用验证时直接放行
 * @Date 2026/04/18 10:00
 */
@Component
public class ThirdPartyCallbackVerifier {

    private final AlarmIntegrationProperties properties;

    /**
     * @Author tangxinglin
     * @Description //构造函数，注入告警集成配置属性
     * @Date 2026/04/18 10:00
     * @Param [properties 告警集成配置属性]
     * @return void
     */
    public ThirdPartyCallbackVerifier(AlarmIntegrationProperties properties) {
        this.properties = properties;
    }

    /**
     * @Author tangxinglin
     * @Description //验证第三方回调请求的合法性，支持令牌验证和HMAC-SHA256签名验证，未启用验证时直接返回true
     * @Date 2026/04/18 10:00
     * @Param [request HTTP请求对象, rawBody 请求原始体字符串]
     * @return boolean 验证通过返回true，失败时抛出BusinessException
     */
    public boolean verify(HttpServletRequest request, String rawBody) {
        AlarmIntegrationProperties.Callback callback = properties.getCallback();
        if (!callback.isRequireVerification()) {
            return true;
        }
        String headerToken = request.getHeader(callback.getTokenHeader());
        if (StringUtils.hasText(callback.getToken())) {
            if (!callback.getToken().equals(headerToken)) {
                throw new BusinessException("CALLBACK_TOKEN_INVALID", "第三方回调令牌无效");
            }
            return true;
        }
        if (StringUtils.hasText(callback.getSignatureSecret())) {
            String signature = request.getHeader(callback.getSignatureHeader());
            if (!StringUtils.hasText(signature)) {
                throw new BusinessException("CALLBACK_SIGNATURE_MISSING", "第三方回调签名缺失");
            }
            String expected = hmacSha256Hex(rawBody == null ? "" : rawBody, callback.getSignatureSecret());
            if (!expected.equalsIgnoreCase(signature)) {
                throw new BusinessException("CALLBACK_SIGNATURE_INVALID", "第三方回调签名无效");
            }
            return true;
        }
        throw new BusinessException("CALLBACK_VERIFICATION_NOT_CONFIGURED", "第三方回调验证已启用但未配置验证器");
    }

    /**
     * @Author tangxinglin
     * @Description //计算内容的HMAC-SHA256签名并返回十六进制字符串
     * @Date 2026/04/18 10:00
     * @Param [content 待签名内容, secret 签名密钥]
     * @return String 十六进制格式的HMAC-SHA256签名
     */
    private String hmacSha256Hex(String content, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal(content.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(digest.length * 2);
            for (byte value : digest) {
                builder.append(String.format("%02x", value));
            }
            return builder.toString();
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to verify third-party callback signature", exception);
        }
    }
}
