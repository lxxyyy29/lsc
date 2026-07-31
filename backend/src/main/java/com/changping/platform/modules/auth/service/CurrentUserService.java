package com.changping.platform.modules.auth.service;

import com.changping.platform.common.exception.BusinessException;
import com.changping.platform.modules.auth.model.AuthenticatedUser;
import com.changping.platform.modules.auth.security.AuthenticatedUserContextHolder;
import com.changping.platform.modules.auth.security.PermissionGuard;
import com.changping.platform.modules.auth.vo.CurrentUserVo;
import org.springframework.stereotype.Service;

/**
 * @Author tangxinglin
 * @Description //当前用户服务，提供获取当前登录用户信息的能力，并对客户端类型和权限进行双重校验
 * @Date 2026/04/18 10:05
 */
@Service
public class CurrentUserService {

    private final AuthService authService;
    private final PermissionGuard permissionGuard;

    /**
     * @Author tangxinglin
     * @Description //构造函数注入认证服务和权限守卫
     * @Date 2026/04/18 10:05
     * @Param [authService 认证服务, permissionGuard 权限守卫]
     * @return void
     */
    public CurrentUserService(AuthService authService, PermissionGuard permissionGuard) {
        this.authService = authService;
        this.permissionGuard = permissionGuard;
    }

    /**
     * @Author tangxinglin
     * @Description //获取当前登录用户信息，校验客户端类型与必需权限后从数据库重新加载最新用户数据
     * @Date 2026/04/18 10:05
     * @Param [expectedClientType 期望的客户端类型, requiredPermission 必须拥有的权限码]
     * @return CurrentUserVo 当前用户视图对象
     */
    public CurrentUserVo getCurrentUser(AuthService.ClientType expectedClientType, String requiredPermission) {
        AuthenticatedUser authenticatedUser = requireClientType(expectedClientType);
        permissionGuard.require(requiredPermission);
        return authService.toCurrentUserVo(authService.loadAuthenticatedUser(authenticatedUser.id(), authenticatedUser.clientType()));
    }

    /**
     * @Author tangxinglin
     * @Description //从线程上下文获取已认证用户并校验其客户端类型，未登录或类型不匹配时抛出业务异常
     * @Date 2026/04/18 10:05
     * @Param [expectedClientType 期望的客户端类型]
     * @return AuthenticatedUser 当前已认证用户对象
     */
    public AuthenticatedUser requireClientType(AuthService.ClientType expectedClientType) {
        AuthenticatedUser authenticatedUser = AuthenticatedUserContextHolder.getOptional()
                .orElseThrow(() -> new BusinessException("AUTH_TOKEN_REQUIRED", "请提供认证令牌"));
        if (!expectedClientType.name().equals(authenticatedUser.clientType())) {
            throw new BusinessException("AUTH_CLIENT_TYPE_FORBIDDEN", "认证令牌不适用于该客户端类型");
        }
        return authenticatedUser;
    }

    /**
     * 获取当前登录用户ID（Web端），未登录时抛出业务异常
     */
    public Long requireUserId() {
        AuthenticatedUser authenticatedUser = requireClientType(AuthService.ClientType.WEB);
        return authenticatedUser.id();
    }
}
