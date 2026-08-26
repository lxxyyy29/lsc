package com.changping.platform.modules.oss.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @Author lxy
 * @Description //OSS 配置属性类，绑定 application.yml 中 oss.* 前缀的配置项
 * @Date 2026/04/18 10:16
 */
@Component
@ConfigurationProperties(prefix = "oss")
public class OssProperties {

    /** 是否启用OSS */
    private boolean enable = false;
    /** OSS 提供商，默认 minio */
    private String provider = "minio";
    /** OSS 公开访问前缀 URL */
    private String access;
    /** MinIO 服务端点地址 */
    private String endpoint;
    /** 访问密钥 ID */
    private String accessKey;
    /** 访问密钥 Secret */
    private String secretKey;
    /** 存储桶名称 */
    private String bucket;
    /** 预签名 URL 过期时间（秒），默认 3600 */
    private int expire = 3600;
    /** 存储区域，默认 us-east-1 */
    private String region = "us-east-1";

    public boolean isEnable() {
        return enable;
    }

    public void setEnable(boolean enable) {
        this.enable = enable;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getAccess() {
        return access;
    }

    public void setAccess(String access) {
        this.access = access;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public String getBucket() {
        return bucket;
    }

    public void setBucket(String bucket) {
        this.bucket = bucket;
    }

    public int getExpire() {
        return expire;
    }

    public void setExpire(int expire) {
        this.expire = expire;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }
}
