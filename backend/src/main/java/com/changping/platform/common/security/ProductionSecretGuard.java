package com.changping.platform.common.security;

import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 生产环境敏感默认配置防护，避免误用开发密钥启动。
 */
@Component
public class ProductionSecretGuard implements InitializingBean {

    private static final List<String> PRODUCTION_PROFILES = List.of("prod", "production");

    private final Environment environment;
    private final String jwtSecret;
    private final String dbPassword;
    private final String redisPassword;
    private final String dronePassword;
    private final String callbackToken;
    private final String callbackSignatureSecret;
    private final String ossAccessKey;
    private final String ossSecretKey;

    public ProductionSecretGuard(
            Environment environment,
            @Value("${security.auth.jwt-secret:}") String jwtSecret,
            @Value("${spring.datasource.password:}") String dbPassword,
            @Value("${spring.data.redis.password:}") String redisPassword,
            @Value("${drone.api.password:}") String dronePassword,
            @Value("${drone.alarm-integration.callback.token:}") String callbackToken,
            @Value("${drone.alarm-integration.callback.signature-secret:}") String callbackSignatureSecret,
            @Value("${oss.accessKey:}") String ossAccessKey,
            @Value("${oss.secretKey:}") String ossSecretKey) {
        this.environment = environment;
        this.jwtSecret = jwtSecret;
        this.dbPassword = dbPassword;
        this.redisPassword = redisPassword;
        this.dronePassword = dronePassword;
        this.callbackToken = callbackToken;
        this.callbackSignatureSecret = callbackSignatureSecret;
        this.ossAccessKey = ossAccessKey;
        this.ossSecretKey = ossSecretKey;
    }

    @Override
    public void afterPropertiesSet() {
        if (!isProductionProfile()) {
            return;
        }
        rejectDefault("security.auth.jwt-secret", jwtSecret, "changping-platform-dev-jwt-secret-2026");
        rejectDefault("spring.datasource.password", dbPassword, "123456");
        rejectDefault("spring.data.redis.password", redisPassword, "123456");
        rejectDefault("drone.api.password", dronePassword, "kfk@dksl0515", "dksl@admin999");
        rejectDefault("drone.alarm-integration.callback.token", callbackToken, "changping-default-callback-token");
        rejectDefault("drone.alarm-integration.callback.signature-secret", callbackSignatureSecret, "changping-default-signature-secret");
        rejectDefault("oss.accessKey", ossAccessKey, "admin");
        rejectDefault("oss.secretKey", ossSecretKey, "kfkmi666999");
    }

    private boolean isProductionProfile() {
        return Arrays.stream(environment.getActiveProfiles())
                .map(String::toLowerCase)
                .anyMatch(PRODUCTION_PROFILES::contains);
    }

    private void rejectDefault(String propertyName, String actualValue, String... forbiddenValues) {
        if (!StringUtils.hasText(actualValue)) {
            throw new IllegalStateException(propertyName + " must be configured in production");
        }
        for (String forbidden : forbiddenValues) {
            if (forbidden.equals(actualValue)) {
                throw new IllegalStateException(propertyName + " uses a development default and must be changed in production");
            }
        }
    }
}
