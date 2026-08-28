package com.changping.platform.modules.auth.security;

import com.changping.platform.common.exception.BusinessException;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * 登录失败限流服务，按账号和客户端类型统计连续失败次数。
 */
@Service
public class LoginAttemptService {

    private final int maxFailures;
    private final Duration lockDuration;
    private final Duration failureWindow;
    private final Map<String, AttemptState> attempts = new ConcurrentHashMap<>();

    public LoginAttemptService(
            @Value("${security.auth.login-max-failures:5}") int maxFailures,
            @Value("${security.auth.login-lock-minutes:15}") long lockMinutes,
            @Value("${security.auth.login-failure-window-minutes:10}") long failureWindowMinutes) {
        this.maxFailures = Math.max(1, maxFailures);
        this.lockDuration = Duration.ofMinutes(Math.max(1, lockMinutes));
        this.failureWindow = Duration.ofMinutes(Math.max(1, failureWindowMinutes));
    }

    public void assertAllowed(String account, String clientType) {
        String key = key(account, clientType);
        AttemptState state = attempts.get(key);
        if (state == null || state.lockedUntil == null) {
            return;
        }
        if (Instant.now().isBefore(state.lockedUntil)) {
            throw new BusinessException("AUTH_LOGIN_RATE_LIMITED", "登录失败次数过多，请稍后再试");
        }
        attempts.remove(key);
    }

    public void recordFailure(String account, String clientType) {
        String key = key(account, clientType);
        Instant now = Instant.now();
        attempts.compute(key, (ignored, existing) -> {
            AttemptState state = existing;
            if (state == null || Duration.between(state.lastFailureAt, now).compareTo(failureWindow) > 0) {
                state = new AttemptState(0, null, now);
            }
            int failures = state.failures + 1;
            Instant lockedUntil = failures >= maxFailures ? now.plus(lockDuration) : state.lockedUntil;
            return new AttemptState(failures, lockedUntil, now);
        });
    }

    public void recordSuccess(String account, String clientType) {
        attempts.remove(key(account, clientType));
    }

    private String key(String account, String clientType) {
        String normalizedAccount = account == null ? "" : account.trim().toLowerCase(Locale.ROOT);
        String normalizedClient = clientType == null ? "" : clientType.trim().toUpperCase(Locale.ROOT);
        return normalizedClient + ":" + normalizedAccount;
    }

    private record AttemptState(int failures, Instant lockedUntil, Instant lastFailureAt) {
    }
}
