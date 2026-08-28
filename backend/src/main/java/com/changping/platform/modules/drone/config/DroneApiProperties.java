package com.changping.platform.modules.drone.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * @Author lxy
 * @Description //无人机API配置属性，绑定 drone.api 前缀的配置项，包含服务地址、WebSocket地址、
 * 认证账密、工作空间ID、SM4加密参数及令牌缓存时长
 * @Date 2026/04/18 10:00
 */
@Validated
@ConfigurationProperties(prefix = "drone.api")
public class DroneApiProperties {

    /** 上游无人机平台HTTP服务地址 */
    @NotBlank
    private String serverAddr;

    /** 上游无人机平台WebSocket服务地址 */
    @NotBlank
    private String wsAddr;

    /** 登录上游平台的用户名 */
    @NotBlank
    private String username;

    /** 登录上游平台的明文密码（SM4加密后发送） */
    @NotBlank
    private String password;

    /** 固定使用的工作空间ID */
    @NotBlank
    private String fixedWorkspaceId;

    /** SM4加密密钥，默认值为平台约定密钥 */
    @NotBlank
    private String sm4SecretKey = "gsis20232023gsis";

    /** SM4加密初始向量IV，默认值为平台约定IV */
    @NotBlank
    private String sm4Iv = "9na3v13cy9bt39vu";

    /** 访问令牌本地缓存有效时长，默认50分钟 */
    @NotNull
    private Duration tokenCacheTtl = Duration.ofMinutes(50);

    public String getServerAddr() {
        return serverAddr;
    }

    public void setServerAddr(String serverAddr) {
        this.serverAddr = serverAddr;
    }

    public String getWsAddr() {
        return wsAddr;
    }

    public void setWsAddr(String wsAddr) {
        this.wsAddr = wsAddr;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getFixedWorkspaceId() {
        return fixedWorkspaceId;
    }

    public void setFixedWorkspaceId(String fixedWorkspaceId) {
        this.fixedWorkspaceId = fixedWorkspaceId;
    }

    public String getSm4SecretKey() {
        return sm4SecretKey;
    }

    public void setSm4SecretKey(String sm4SecretKey) {
        this.sm4SecretKey = sm4SecretKey;
    }

    public String getSm4Iv() {
        return sm4Iv;
    }

    public void setSm4Iv(String sm4Iv) {
        this.sm4Iv = sm4Iv;
    }

    public Duration getTokenCacheTtl() {
        return tokenCacheTtl;
    }

    public void setTokenCacheTtl(Duration tokenCacheTtl) {
        this.tokenCacheTtl = tokenCacheTtl;
    }
}
