package com.changping.platform.modules.auth.security;

import com.changping.platform.common.exception.BusinessException;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * @Author lxy
 * @Description //权限守卫组件，提供编程式权限检查方法，基于当前线程的已认证用户权限码集合进行校验
 * @Date 2026/04/18 10:00
 */
@Component
public class PermissionGuard {

    /**
     * @Author lxy
     * @Description //检查当前用户是否拥有指定权限码
     * @Date 2026/04/18 10:00
     * @Param [permissionCode 待检查的权限码]
     * @return boolean 是否拥有该权限
     */
    public boolean has(String permissionCode) {
        if (permissionCode == null || permissionCode.isBlank()) {
            return false;
        }
        return AuthenticatedUserContextHolder.getOptional()
                .map(authenticatedUser -> authenticatedUser.permissionCodes().contains(permissionCode))
                .orElse(false);
    }

    /**
     * @Author lxy
     * @Description //检查当前用户是否拥有给定权限码列表中的任意一个权限
     * @Date 2026/04/18 10:00
     * @Param [permissionCodes 待检查的权限码列表]
     * @return boolean 是否拥有任意一个权限
     */
    public boolean hasAny(String... permissionCodes) {
        Set<String> expectedCodes = new LinkedHashSet<>(Arrays.asList(permissionCodes));
        expectedCodes.removeIf(permissionCode -> permissionCode == null || permissionCode.isBlank());
        if (expectedCodes.isEmpty()) {
            return false;
        }
        return AuthenticatedUserContextHolder.getOptional()
                .map(authenticatedUser -> expectedCodes.stream().anyMatch(authenticatedUser.permissionCodes()::contains))
                .orElse(false);
    }

    /**
     * @Author lxy
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
     * @Author lxy
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

}
