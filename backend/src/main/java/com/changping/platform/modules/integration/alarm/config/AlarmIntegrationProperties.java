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

    /** 图片上传配置（面向板端，使用回调令牌鉴权，无需平台登录态） */
    private final Upload upload = new Upload();

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

    public Upload getUpload() {
        return upload;
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

    /**
     * 板端图片上传配置。
     * 该接口不依赖平台登录态，仅使用回调令牌鉴权，供板端上传违停抓拍图后取得可访问 URL。
     */
    public static class Upload {

        /**
         * 返回给板端的公网访问前缀（以 / 结尾）。
         * 为空时回退到 oss.access —— 但 oss.access 在部分环境为内网地址，板端无法访问，
         * 生产环境应显式配置本项（如 https://drone.kfktec.cn:8443/minio/）。
         */
        private String publicBaseUrl;

        /** 单次最多可上传的图片数量 */
        private int maxFiles = 5;

        /** 单张图片大小上限（MB） */
        private int maxFileSizeMb = 10;

        /** 允许的扩展名，逗号分隔 */
        private String allowedExtensions = "jpg,jpeg,png";

        public String getPublicBaseUrl() {
            return publicBaseUrl;
        }

        public void setPublicBaseUrl(String publicBaseUrl) {
            this.publicBaseUrl = publicBaseUrl;
        }

        public int getMaxFiles() {
            return maxFiles;
        }

        public void setMaxFiles(int maxFiles) {
            this.maxFiles = maxFiles;
        }

        public int getMaxFileSizeMb() {
            return maxFileSizeMb;
        }

        public void setMaxFileSizeMb(int maxFileSizeMb) {
            this.maxFileSizeMb = maxFileSizeMb;
        }

        public String getAllowedExtensions() {
            return allowedExtensions;
        }

        public void setAllowedExtensions(String allowedExtensions) {
            this.allowedExtensions = allowedExtensions;
        }
    }
}
