package com.changping.platform.modules.system.controller;

import com.changping.platform.common.response.ApiResponse;
import com.changping.platform.modules.auth.security.PermissionCodes;
import com.changping.platform.modules.auth.security.PermissionGuard;
import com.changping.platform.modules.auth.service.AuthService;
import com.changping.platform.modules.auth.service.CurrentUserService;
import com.changping.platform.modules.system.service.SystemPermissionService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author tangxinglin
 * @Description //系统权限控制器，提供权限树查询接口，支持按权限类型过滤
 * @Date 2026/04/18 09:40
 */
@RestController
@RequestMapping("/system/permissions")
public class SystemPermissionController {

    private final SystemPermissionService systemPermissionService;
    private final CurrentUserService currentUserService;
    private final PermissionGuard permissionGuard;

    /**
     * @Author tangxinglin
     * @Description //构造函数，注入权限服务、当前用户服务及权限校验
     * @Date 2026/04/18 09:40
     * @Param [systemPermissionService 权限服务, currentUserService 当前用户服务, permissionGuard 权限校验]
     * @return
     */
    public SystemPermissionController(
            SystemPermissionService systemPermissionService,
            CurrentUserService currentUserService,
            PermissionGuard permissionGuard) {
        this.systemPermissionService = systemPermissionService;
        this.currentUserService = currentUserService;
        this.permissionGuard = permissionGuard;
    }

    /**
     * @Author tangxinglin
     * @Description //查询权限树，支持按权限类型过滤（如 MENU、API 等）
     * @Date 2026/04/18 09:40
     * @Param [permissionType 权限类型筛选，可选]
     * @return ApiResponse<List<SystemPermissionService.PermissionTreeNode>> 权限树
     */
    @GetMapping("/tree")
    public ApiResponse<List<SystemPermissionService.PermissionTreeNode>> listPermissions(
            @RequestParam(required = false) String permissionType) {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.API_SYSTEM_PERMISSION_LIST);
        return ApiResponse.ok(systemPermissionService.listPermissionTree(permissionType));
    }
}
