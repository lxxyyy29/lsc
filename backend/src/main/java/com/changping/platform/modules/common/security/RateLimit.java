package com.changping.platform.modules.common.security;

import java.lang.annotation.*;

/**
 * 接口限流注解
 * 用于标记需要限流的控制器方法
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {

    /**
     * 允许的最大请求数（默认 60）
     */
    int limit() default 60;

    /**
     * 时间窗口（秒，默认 60）
     */
    int window() default 60;

    /**
     * 限流维度（默认按用户，可选 IP）
     */
    RateLimitType type() default RateLimitType.USER;

    /**
     * 限流提示消息
     */
    String message() default "请求过于频繁，请稍后重试";

    /**
     * 限流维度类型
     */
    enum RateLimitType {
        USER,   // 按用户 ID
        IP      // 按 IP 地址
    }
}
