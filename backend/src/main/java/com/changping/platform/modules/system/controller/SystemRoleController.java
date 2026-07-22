package com.changping.platform.modules.system.controller;

import com.changping.platform.common.response.ApiResponse;
import com.changping.platform.common.response.PagedResult;
import com.changping.platform.modules.auth.security.PermissionCodes;
import com.changping.platform.modules.auth.security.PermissionGuard;
import com.changping.platform.modules.auth.service.AuthService;
import com.changping.platform.modules.auth.service.CurrentUserService;
import com.changping.platform.modules.system.service.SystemRoleService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author tangxinglin
 * @Description //系统角色控制器，提供角色列表、详情、创建、更新及权限分配接口
 * @Date 2026/04/18 09:45
 */
@Validated
@RestController
@RequestMapping("/system/roles")
public class SystemRoleController {

    private final SystemRoleService systemRoleService;
    private final CurrentUserService currentUserService;
    private final PermissionGuard permissionGuard;

    /**
     * @Author tangxinglin
     * @Description //构造函数，注入角色服务、当前用户服务及权限校验
     * @Date 2026/04/18 09:45
     * @Param [systemRoleService 角色服务, currentUserService 当前用户服务, permissionGuard 权限校验]
     * @return
     */
    public SystemRoleController(
            SystemRoleService systemRoleService,
            CurrentUserService currentUserService,
            PermissionGuard permissionGuard) {
        this.systemRoleService = systemRoleService;
        this.currentUserService = currentUserService;
        this.permissionGuard = permissionGuard;
    }

    /**
     * @Author tangxinglin
     * @Description //查询所有角色列表（不分页），包含用户数和权限数统计
     * @Date 2026/04/18 09:45
     * @Param []
     * @return ApiResponse<List<SystemRoleService.RoleListItem>> 角色列表
     */
    @GetMapping
    public ApiResponse<List<SystemRoleService.RoleListItem>> listRoles() {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.API_SYSTEM_ROLE_LIST);
        return ApiResponse.ok(systemRoleService.listRoles());
    }

    /**
     * @Author tangxinglin
     * @Description //分页查询角色列表
     * @Date 2026/04/18 09:45
     * @Param [page 页码，默认1, pageSize 每页条数，默认10]
     * @return ApiResponse<PagedResult<SystemRoleService.RoleListItem>> 分页角色列表
     */
    @GetMapping("/paged")
    public ApiResponse<PagedResult<SystemRoleService.RoleListItem>> listRolesPaged(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.API_SYSTEM_ROLE_LIST);
        return ApiResponse.ok(systemRoleService.listRolesPaged(page, pageSize));
    }

    /**
     * @Author tangxinglin
     * @Description //获取指定角色的详情，包含关联的权限ID和权限编码列表
     * @Date 2026/04/18 09:45
     * @Param [id 角色ID]
     * @return ApiResponse<SystemRoleService.RoleDetail> 角色详情
     */
    @GetMapping("/{id}")
    public ApiResponse<SystemRoleService.RoleDetail> getRoleDetail(@PathVariable Long id) {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.API_SYSTEM_ROLE_DETAIL);
        return ApiResponse.ok(systemRoleService.getRoleDetail(id));
    }

    /**
     * @Author tangxinglin
     * @Description //创建新角色
     * @Date 2026/04/18 09:45
     * @Param [request 创建角色请求，包含角色编码、名称、状态和备注]
     * @return ApiResponse<SystemRoleService.RoleDetail> 新建的角色详情
     */
    @PostMapping
    public ApiResponse<SystemRoleService.RoleDetail> createRole(@Valid @RequestBody SystemRoleService.CreateRoleRequest request) {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.API_SYSTEM_ROLE_CREATE);
        return ApiResponse.ok(systemRoleService.createRole(request));
    }

    /**
     * @Author tangxinglin
     * @Description //更新指定角色的基本信息
     * @Date 2026/04/18 09:45
     * @Param [id 角色ID, request 更新角色请求]
     * @return ApiResponse<SystemRoleService.RoleDetail> 更新后的角色详情
     */
    @PutMapping("/{id}")
    public ApiResponse<SystemRoleService.RoleDetail> updateRole(
            @PathVariable Long id,
            @Valid @RequestBody SystemRoleService.UpdateRoleRequest request) {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.API_SYSTEM_ROLE_UPDATE);
        return ApiResponse.ok(systemRoleService.updateRole(id, request));
    }

    /**
     * @Author tangxinglin
     * @Description //为指定角色分配权限，覆盖原有权限列表
     * @Date 2026/04/18 09:45
     * @Param [id 角色ID, request 权限分配请求，包含权限ID列表]
     * @return ApiResponse<SystemRoleService.RoleDetail> 更新后的角色详情
     */
    @PutMapping("/{id}/permissions")
    public ApiResponse<SystemRoleService.RoleDetail> assignPermissions(
            @PathVariable Long id,
            @RequestBody SystemRoleService.AssignPermissionsRequest request) {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.API_SYSTEM_ROLE_ASSIGN_PERMISSIONS);
        return ApiResponse.ok(systemRoleService.assignPermissions(id, request));
    }
}
