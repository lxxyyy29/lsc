package com.changping.platform.modules.common.security;

import com.changping.platform.common.exception.BusinessException;
import com.changping.platform.modules.auth.security.AuthenticatedUserContextHolder;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.List;

/**
 * 接口限流切面 - 基于 Redis 令牌桶
 * 对标记了 @RateLimit 的方法进行限流
 * 仅在 Redis 可用时启用
 */
@Aspect
@Component
@ConditionalOnBean(StringRedisTemplate.class)
public class RateLimitAspect {

    private final StringRedisTemplate redisTemplate;

    /**
     * Lua 脚本实现令牌桶限流
     * KEYS[1] = 限流 key
     * ARGV[1] = 桶容量 (limit)
     * ARGV[2] = 时间窗口 (window)
     * ARGV[3] = 当前时间戳
     *
     * 返回 1 = 允许，0 = 拒绝
     */
    private static final String LIMIT_SCRIPT = """
            local key = KEYS[1]
            local limit = tonumber(ARGV[1])
            local window = tonumber(ARGV[2])
            local now = tonumber(ARGV[3])
            local bucket = redis.call('hmget', key, 'tokens', 'last_time')
            local tokens = tonumber(bucket[1])
            local last_time = tonumber(bucket[2])
            if tokens == nil then
                tokens = limit - 1
                last_time = now
                redis.call('hmset', key, 'tokens', tokens, 'last_time', now)
                redis.call('expire', key, window)
                return 1
            end
            local elapsed = now - last_time
            local refill = math.floor(elapsed * limit / window)
            if refill > 0 then
                tokens = math.min(tokens + refill, limit)
                last_time = now
            end
            if tokens > 0 then
                tokens = tokens - 1
                redis.call('hmset', key, 'tokens', tokens, 'last_time', last_time)
                redis.call('expire', key, window)
                return 1
            end
            return 0
            """;

    public RateLimitAspect(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Around("@annotation(rateLimit)")
    public Object around(ProceedingJoinPoint point, RateLimit rateLimit) throws Throwable {
        String key = generateKey(rateLimit);

        DefaultRedisScript<Long> script = new DefaultRedisScript<>(LIMIT_SCRIPT, Long.class);
        List<String> keys = Collections.singletonList(key);

        Long result = redisTemplate.execute(script, keys,
                String.valueOf(rateLimit.limit()),
                String.valueOf(rateLimit.window()),
                String.valueOf(System.currentTimeMillis() / 1000));

        if (result == null || result == 0) {
            throw new BusinessException("RATE_LIMIT_EXCEEDED", rateLimit.message());
        }

        return point.proceed();
    }

    private String generateKey(RateLimit rateLimit) {
        String prefix = "ratelimit:";
        if (rateLimit.type() == RateLimit.RateLimitType.IP) {
            return prefix + "ip:" + getClientIp();
        }
        // 按用户
        Long userId = AuthenticatedUserContextHolder.getOptional().map(u -> u.id()).orElse(null);
        if (userId != null) {
            return prefix + "user:" + userId;
        }
        return prefix + "ip:" + getClientIp();
    }

    private String getClientIp() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) return "unknown";
            HttpServletRequest request = attrs.getRequest();
            String ip = request.getHeader("X-Forwarded-For");
            if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getHeader("X-Real-IP");
            }
            if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getRemoteAddr();
            }
            return ip != null ? ip.split(",")[0].trim() : "unknown";
        } catch (Exception e) {
            return "unknown";
        }
    }
}
