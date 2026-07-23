package com.changping.platform.common.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;

/**
 * Redis 配置 - 仅在 redis.enabled=true 时启用。
 * 避免 Redis 不可用时阻塞应用启动。
 */
@Configuration
@ConditionalOnProperty(name = "spring.data.redis.enabled", havingValue = "true", matchIfMissing = false)
public class RedisConfig {

    @Bean
    public RedisConnectionFactory redisConnectionFactory(
            org.springframework.boot.autoconfigure.data.redis.RedisProperties properties) {
        RedisStandaloneConfiguration standaloneConfig = new RedisStandaloneConfiguration(
                properties.getHost(), properties.getPort());
        standaloneConfig.setPassword(org.springframework.data.redis.connection.RedisPassword.of(
                properties.getPassword() == null ? "" : properties.getPassword()));
        standaloneConfig.setDatabase(properties.getDatabase());

        LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
                .commandTimeout(properties.getTimeout())
                .build();

        return new LettuceConnectionFactory(standaloneConfig, clientConfig);
    }

    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        return new StringRedisTemplate(connectionFactory);
    }
}
