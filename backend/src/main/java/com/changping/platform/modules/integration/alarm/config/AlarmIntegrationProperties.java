package com.changping.platform.modules.integration.alarm.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * @Author lxy
 * @Description //告警集成配置属性，绑定 drone.alarm-integration 前缀配置项，
 * 包含来源系统标识、默认事件类型及回调验证相关参数
 * @Date 2026/04/18 10:00
 */
@Validated
@ConfigurationProperties(prefix = "drone.alarm-integration")
public class AlarmIntegrationProperties {

    /** 来源系统标识，默认为THIRD_PARTY_DRONE */
    @NotBlank
    private String sourceSystem = "THIRD_PARTY_DRONE";

    /** 来源类型标识，默认为DRONE_ALARM */
    @NotBlank
    private String sourceType = "DRONE_ALARM";

    /** 默认事件类型，当原始载荷未提供时使用，默认为DRONE_ALARM */
    @NotBlank
    private String defaultEventType = "DRONE_ALARM";

    /** 回调验证配置，包含令牌和签名验证相关参数 */
    private final Callback callback = new Callback();

    public String getSourceSystem() {
        return sourceSystem;
    }

    public void setSourceSystem(String sourceSystem) {
        this.sourceSystem = sourceSystem;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public String getDefaultEventType() {
        return defaultEventType;
    }

    public void setDefaultEventType(String defaultEventType) {
        this.defaultEventType = defaultEventType;
    }

    public Callback getCallback() {
        return callback;
    }

    public static class Callback {
        private boolean requireVerification;

        @NotBlank
        private String tokenHeader = "X-Alarm-Token";

        private String token;

        @NotBlank
        private String signatureHeader = "X-Alarm-Signature";

        private String signatureSecret;

        public boolean isRequireVerification() {
            return requireVerification;
        }

        public void setRequireVerification(boolean requireVerification) {
            this.requireVerification = requireVerification;
        }

        public String getTokenHeader() {
            return tokenHeader;
        }

        public void setTokenHeader(String tokenHeader) {
            this.tokenHeader = tokenHeader;
        }

        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            this.token = token;
        }

        public String getSignatureHeader() {
            return signatureHeader;
        }

        public void setSignatureHeader(String signatureHeader) {
            this.signatureHeader = signatureHeader;
        }

        public String getSignatureSecret() {
            return signatureSecret;
        }

        public void setSignatureSecret(String signatureSecret) {
            this.signatureSecret = signatureSecret;
        }
    }
}
