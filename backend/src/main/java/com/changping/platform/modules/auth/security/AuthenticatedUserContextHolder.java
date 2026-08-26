package com.changping.platform.modules.auth.security;

import com.changping.platform.modules.auth.model.AuthenticatedUser;
import java.util.Optional;

/**
 * @Author lxy
 * @Description //已认证用户上下文持有者，基于 ThreadLocal 在当前请求线程内存储和获取已认证用户信息，请求结束后须调用 clear() 释放
 * @Date 2026/04/18 09:40
 */
public final class AuthenticatedUserContextHolder {

    private static final ThreadLocal<AuthenticatedUser> CONTEXT = new ThreadLocal<>();

    /**
     * @Author lxy
     * @Description //私有构造器，防止工具类被实例化
     * @Date 2026/04/18 09:40
     * @Param []
     * @return void
     */
    private AuthenticatedUserContextHolder() {
    }

    /**
     * @Author lxy
     * @Description //将已认证用户绑定到当前请求线程的上下文中
     * @Date 2026/04/18 09:40
     * @Param [authenticatedUser 已认证用户对象]
     * @return void
     */
    public static void set(AuthenticatedUser authenticatedUser) {
        CONTEXT.set(authenticatedUser);
    }

    /**
     * @Author lxy
     * @Description //可选方式获取当前线程绑定的已认证用户，未登录时返回空 Optional
     * @Date 2026/04/18 09:40
     * @Param []
     * @return Optional<AuthenticatedUser> 当前已认证用户的 Optional 包装
     */
    public static Optional<AuthenticatedUser> getOptional() {
        return Optional.ofNullable(CONTEXT.get());
    }

    /**
     * @Author lxy
     * @Description //必须方式获取当前线程绑定的已认证用户，未绑定时抛出 IllegalStateException
     * @Date 2026/04/18 09:40
     * @Param []
     * @return AuthenticatedUser 当前已认证用户对象
     */
    public static AuthenticatedUser getRequired() {
        AuthenticatedUser authenticatedUser = CONTEXT.get();
        if (authenticatedUser == null) {
            throw new IllegalStateException("No authenticated user is bound to the current thread");
        }
        return authenticatedUser;
    }

    /**
     * @Author lxy
     * @Description //清除当前线程绑定的已认证用户上下文，防止内存泄漏，应在请求结束时调用
     * @Date 2026/04/18 09:40
     * @Param []
     * @return void
     */
    public static void clear() {
        CONTEXT.remove();
    }
}
