package com.changping.platform.modules.oss.config;

import io.minio.MinioClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @Author tangxinglin
 * @Description //MinIO 客户端配置类，在 oss.enable=true 时向容器注册 MinioClient Bean
 * @Date 2026/04/18 10:15
 */
@Configuration
@ConditionalOnProperty(prefix = "oss", name = "enable", havingValue = "true")
public class MinioConfig {

    private final OssProperties ossProperties;

    /**
     * @Author tangxinglin
     * @Description //构造函数，注入 OSS 配置属性
     * @Date 2026/04/18 10:15
     * @Param [ossProperties OSS 配置属性]
     * @return
     */
    public MinioConfig(OssProperties ossProperties) {
        this.ossProperties = ossProperties;
    }

    /**
     * @Author tangxinglin
     * @Description //创建并配置 MinioClient Bean，使用 endpoint、accessKey、secretKey 和 region
     * @Date 2026/04/18 10:15
     * @Param []
     * @return MinioClient 配置好的 MinIO 客户端实例
     */
    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(ossProperties.getEndpoint())
                .credentials(ossProperties.getAccessKey(), ossProperties.getSecretKey())
                .region(ossProperties.getRegion())
                .build();
    }
}
