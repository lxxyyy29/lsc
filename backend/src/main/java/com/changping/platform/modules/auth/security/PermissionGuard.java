package com.changping.platform.modules.auth.security;

import com.changping.platform.common.exception.BusinessException;
import com.changping.platform.modules.auth.model.AuthenticatedUser;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * @Author tangxinglin
 * @Description //权限守卫组件，提供编程式权限检查方法，基于当前线程的已认证用户权限码集合进行校验
 * @Date 2026/04/18 10:00
 */
@Component
public class PermissionGuard {

    /**
     * @Author tangxinglin
     * @Description //检查当前用户是否拥有指定权限码
     * @Date 2026/04/18 10:00
     * @Param [permissionCode 待检查的权限码]
     * @return boolean 是否拥有该权限
     */
    public boolean has(String permissionCode) {
        return AuthenticatedUserContextHolder.getOptional()
                .map(authenticatedUser -> {
                    // api:* 只代表后端接口入口，不再作为角色可配置权限。
                    // 接口层保留登录校验和 Web/H5 客户端隔离，菜单/业务能力由菜单权限控制。
                    if (isApiPermission(permissionCode)) {
                        return isClientMatched(permissionCode, authenticatedUser);
                    }
                    return authenticatedUser.permissionCodes().stream().anyMatch(permissionCode::equals);
                })
                .orElse(false);
    }

    /**
     * @Author tangxinglin
     * @Description //检查当前用户是否拥有给定权限码列表中的任意一个权限
     * @Date 2026/04/18 10:00
     * @Param [permissionCodes 待检查的权限码列表]
     * @return boolean 是否拥有任意一个权限
     */
    public boolean hasAny(String... permissionCodes) {
        Set<String> expectedCodes = new LinkedHashSet<>(Arrays.asList(permissionCodes));
        return AuthenticatedUserContextHolder.getOptional()
                .map(authenticatedUser -> expectedCodes.stream().anyMatch(permissionCode -> {
                    if (isApiPermission(permissionCode)) {
                        return isClientMatched(permissionCode, authenticatedUser);
                    }
                    return authenticatedUser.permissionCodes().contains(permissionCode);
                }))
                .orElse(false);
    }

    /**
     * @Author tangxinglin
     * @Description //断言当前用户必须拥有指定权限码，否则抛出权限拒绝业务异常
     * @Date 2026/04/18 10:00
     * @Param [permissionCode 必须拥有的权限码]
     * @return void
     */
    public void require(String permissionCode) {
        if (!has(permissionCode)) {
            throw new BusinessException("AUTH_PERMISSION_DENIED", "当前用户没有所需权限: " + permissionCode);
        }
    }

    /**
     * @Author tangxinglin
     * @Description //断言当前用户必须拥有给定权限码列表中的任意一个权限，否则抛出权限拒绝业务异常
     * @Date 2026/04/18 10:00
     * @Param [permissionCodes 至少需要拥有其中一个的权限码列表]
     * @return void
     */
    public void requireAny(String... permissionCodes) {
        if (!hasAny(permissionCodes)) {
            throw new BusinessException(
                    "AUTH_PERMISSION_DENIED",
                    "Current user does not have any required permission: " + String.join(", ", permissionCodes));
        }
    }

    private boolean isApiPermission(String permissionCode) {
        return permissionCode != null && permissionCode.startsWith("api:");
    }

    private boolean isClientMatched(String permissionCode, AuthenticatedUser authenticatedUser) {
        if (permissionCode.startsWith("api:h5:") || PermissionCodes.API_AUTH_H5_ME.equals(permissionCode)) {
            return "H5".equals(authenticatedUser.clientType());
        }
        return "WEB".equals(authenticatedUser.clientType());
    }
}
