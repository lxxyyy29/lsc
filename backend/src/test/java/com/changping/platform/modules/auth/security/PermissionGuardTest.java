package com.changping.platform.modules.auth.security;

import com.changping.platform.common.exception.BusinessException;
import com.changping.platform.modules.auth.model.AuthenticatedUser;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PermissionGuardTest {

    private final PermissionGuard permissionGuard = new PermissionGuard();

    @AfterEach
    void clearContext() {
        AuthenticatedUserContextHolder.clear();
    }

    @Test
    void hasRequiresExplicitApiPermission() {
        AuthenticatedUserContextHolder.set(user("WEB", List.of(PermissionCodes.MENU_DASHBOARD_VIEW)));

        assertFalse(permissionGuard.has(PermissionCodes.API_SYSTEM_USER_LIST));
        assertThrows(BusinessException.class, () -> permissionGuard.require(PermissionCodes.API_SYSTEM_USER_LIST));
    }

    @Test
    void hasAllowsExplicitApiPermission() {
        AuthenticatedUserContextHolder.set(user("WEB", List.of(PermissionCodes.API_SYSTEM_USER_LIST)));

        assertTrue(permissionGuard.has(PermissionCodes.API_SYSTEM_USER_LIST));
        assertDoesNotThrow(() -> permissionGuard.require(PermissionCodes.API_SYSTEM_USER_LIST));
    }

    @Test
    void hasAnyIgnoresBlankPermissionsAndRequiresAtLeastOneMatch() {
        AuthenticatedUserContextHolder.set(user("WEB", List.of(PermissionCodes.API_PARKING_VIEW)));

        assertTrue(permissionGuard.hasAny(null, "", PermissionCodes.API_PARKING_VIEW));
        assertFalse(permissionGuard.hasAny(null, "", PermissionCodes.API_PARKING_MANAGE));
    }

    @Test
    void noBoundUserNeverHasPermission() {
        assertFalse(permissionGuard.has(PermissionCodes.API_SYSTEM_USER_LIST));
        assertFalse(permissionGuard.hasAny(PermissionCodes.API_SYSTEM_USER_LIST));
    }

    private AuthenticatedUser user(String clientType, List<String> permissions) {
        return new AuthenticatedUser(1L, "tester", "测试用户", clientType, List.of("TEST_ROLE"), permissions, 1);
    }
}
