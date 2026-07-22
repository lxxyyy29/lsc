package com.changping.platform.common.security;

import com.changping.platform.common.exception.BusinessException;
import com.changping.platform.modules.auth.model.AuthenticatedUser;
import com.changping.platform.modules.auth.security.AuthenticatedUserContextHolder;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Temporary server-side actor strategy used during foundation-stage development.
 * This deliberately does not treat request JSON as authenticated identity.
 * Replace with real authenticated principal resolution when auth is implemented.
 */
/**
 * @Author tangxinglin
 * @Description //操作人解析器，优先从 Bearer Token 认证上下文中获取当前操作人，作为记录操作日志的身份来源；
 *              开发阶段兼容 X-Foundation-* 请求头作为回退策略
 * @Date 2026/04/18 09:20
 */
@Component
public class FoundationActorResolver {

    private static final String USER_ID_HEADER = "X-Foundation-User-Id";
    private static final String USER_NAME_HEADER = "X-Foundation-User-Name";
    private static final String TEST_SCENARIO_ATTRIBUTE = "foundationActorResolver.testScenario";

    /**
     * @Author tangxinglin
     * @Description //解析当前请求的操作人信息，优先使用 Bearer Token 认证用户，其次回退至请求头，均无则返回内部虚拟操作人
     * @Date 2026/04/18 09:20
     * @Param []
     * @return Actor 当前操作人对象，包含用户 ID 和用户名
     */
    public Actor resolveActor() {
        java.util.Optional<AuthenticatedUser> authenticatedUser = AuthenticatedUserContextHolder.getOptional();
        if (authenticatedUser.isPresent()) {
            AuthenticatedUser user = authenticatedUser.get();
            return new Actor(user.id(), user.userName());
        }

        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (requestAttributes instanceof ServletRequestAttributes servletRequestAttributes) {
            HttpServletRequest request = servletRequestAttributes.getRequest();
            if (!isHeaderFallbackAllowed(request)) {
                return new Actor(null, "FOUNDATION_INTERNAL_ACTOR");
            }
            String userIdHeader = request.getHeader(USER_ID_HEADER);
            if (userIdHeader != null) {
                String normalizedUserId = userIdHeader.trim();
                if (normalizedUserId.isEmpty()) {
                    return new Actor(null, "FOUNDATION_INTERNAL_ACTOR");
                }
                try {
                    Long userId = Long.valueOf(normalizedUserId);
                    String userName = request.getHeader(USER_NAME_HEADER);
                    return new Actor(userId, userName == null || userName.isBlank() ? "FOUNDATION_H5_ACTOR" : userName);
                } catch (NumberFormatException exception) {
                    throw new BusinessException("H5_ACTOR_CONTEXT_INVALID", "H5 用户上下文无效");
                }
            }
        }
        return new Actor(null, "FOUNDATION_INTERNAL_ACTOR");
    }

    /**
     * @Author tangxinglin
     * @Description //判断当前请求是否允许使用 X-Foundation-* 请求头作为身份回退，仅测试场景下允许
     * @Date 2026/04/18 09:20
     * @Param [request 当前 HTTP 请求对象]
     * @return boolean 是否允许使用请求头回退策略
     */
    private boolean isHeaderFallbackAllowed(HttpServletRequest request) {
        Object testScenarioAttribute = request.getAttribute(TEST_SCENARIO_ATTRIBUTE);
        return Boolean.TRUE.equals(testScenarioAttribute);
    }

    public record Actor(Long userId, String name) {
    }
}
