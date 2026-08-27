package com.changping.platform.modules.drone.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * @Author lxy
 * @Description //无人机API配置类，注册DroneApiRestTemplate Bean并设置上游服务根URI
 * @Date 2026/04/18 10:00
 */
@Configuration
@EnableConfigurationProperties(DroneApiProperties.class)
public class DroneApiConfiguration {

    /**
     * @Author lxy
     * @Description //创建无人机专用RestTemplate Bean，根配置为上游服务地址
     * @Date 2026/04/18 10:00
     * @Param [restTemplateBuilder RestTemplate构建器, properties 无人机API配置属性]
     * @return RestTemplate 配置好根URI的RestTemplate实例
     */
    @Bean
    public RestTemplate droneApiRestTemplate(RestTemplateBuilder restTemplateBuilder, DroneApiProperties properties) {
        return restTemplateBuilder
                .rootUri(properties.getServerAddr())
                .build();
    }
}
