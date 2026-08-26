package com.changping.platform.modules.integration.alarm.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @Author lxy
 * @Description //告警集成配置类，启用AlarmIntegrationProperties配置属性绑定
 * @Date 2026/04/18 10:00
 */
@Configuration
@EnableConfigurationProperties(AlarmIntegrationProperties.class)
public class AlarmIntegrationConfig {
}
