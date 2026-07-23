package com.changping.platform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration;

/**
 * @Author tangxinglin
 * @Description //Spring Boot 应用程序入口，负责启动整个事件治理平台服务
 * @Date 2026/04/18 09:00
 */
@SpringBootApplication(exclude = {
        RedisAutoConfiguration.class,
        RedisRepositoriesAutoConfiguration.class
})
public class Application {

    /**
     * @Author tangxinglin
     * @Description //应用程序主方法，启动 Spring Boot 容器
     * @Date 2026/04/18 09:00
     * @Param [args 命令行启动参数]
     * @return void
     */
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}

