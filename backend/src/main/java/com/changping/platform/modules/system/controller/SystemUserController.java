package com.changping.platform.modules.system.controller;

import com.changping.platform.common.response.ApiResponse;
import com.changping.platform.common.response.PagedResult;
import com.changping.platform.modules.auth.model.AuthenticatedUser;
import com.changping.platform.modules.auth.security.PermissionCodes;
import com.changping.platform.modules.auth.security.PermissionGuard;
import com.changping.platform.modules.auth.service.AuthService;
import com.changping.platform.modules.auth.service.CurrentUserService;
import com.changping.platform.modules.system.service.SystemUserService;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author lxy
 * @Description //系统用户控制器，提供用户列表、详情、创建、更新、状态切换、角色分配及密码修改接口
 * @Date 2026/04/18 09:50
 */
@RestController
@RequestMapping("/system/users")
public class SystemUserController {

    private final SystemUserService systemUserService;
    private final CurrentUserService currentUserService;
    private final PermissionGuard permissionGuard;

    /**
     * @Author lxy
     * @Description //构造函数，注入用户服务、当前用户服务及权限校验
     * @Date 2026/04/18 09:50
     * @Param [systemUserService 用户服务, currentUserService 当前用户服务, permissionGuard 权限校验]
     * @return
     */
    public SystemUserController(
            SystemUserService systemUserService,
            CurrentUserService currentUserService,
            PermissionGuard permissionGuard) {
        this.systemUserService = systemUserService;
        this.currentUserService = currentUserService;
        this.permissionGuard = permissionGuard;
    }

    /**
     * @Author lxy
     * @Description //查询所有用户列表（不分页），包含角色信息
     * @Date 2026/04/18 09:50
     * @Param []
     * @return ApiResponse<List<SystemUserService.UserListItem>> 用户列表
     */
    @GetMapping
    public ApiResponse<List<SystemUserService.UserListItem>> listUsers() {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.API_SYSTEM_USER_LIST);
        return ApiResponse.ok(systemUserService.listUsers());
    }

    /**
     * @Author lxy
     * @Description //分页查询用户列表，支持按关键字（用户名/姓名/手机号）和状态过滤
     * @Date 2026/04/18 09:50
     * @Param [page 页码，默认1, pageSize 每页条数，默认10, keyword 关键字筛选，可选, status 用户状态筛选，可选]
     * @return ApiResponse<PagedResult<SystemUserService.UserListItem>> 分页用户列表
     */
    @GetMapping("/paged")
    public ApiResponse<PagedResult<SystemUserService.UserListItem>> listUsersPaged(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status) {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.API_SYSTEM_USER_LIST);
        return ApiResponse.ok(systemUserService.listUsersPaged(page, pageSize, keyword, status));
    }

    /**
     * @Author lxy
     * @Description //获取指定用户详情，包含角色及权限编码列表
     * @Date 2026/04/18 09:50
     * @Param [id 用户ID]
     * @return ApiResponse<SystemUserService.UserDetail> 用户详情
     */
    @GetMapping("/{id}")
    public ApiResponse<SystemUserService.UserDetail> getUserDetail(@PathVariable Long id) {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.API_SYSTEM_USER_DETAIL);
        return ApiResponse.ok(systemUserService.getUserDetail(id));
    }

    /**
     * @Author lxy
     * @Description //创建新用户，并关联角色
     * @Date 2026/04/18 09:50
     * @Param [request 创建用户请求，包含用户名、密码、姓名、手机号、状态和角色ID列表]
     * @return ApiResponse<SystemUserService.UserDetail> 新建的用户详情
     */
    @PostMapping
    public ApiResponse<SystemUserService.UserDetail> createUser(@RequestBody SystemUserService.CreateUserRequest request) {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.API_SYSTEM_USER_CREATE);
        return ApiResponse.ok(systemUserService.createUser(request));
    }

    /**
     * @Author lxy
     * @Description //更新指定用户的基本信息（用户名、姓名、手机号、状态）
     * @Date 2026/04/18 09:50
     * @Param [id 用户ID, request 更新用户请求]
     * @return ApiResponse<SystemUserService.UserDetail> 更新后的用户详情
     */
    @PutMapping("/{id}")
    public ApiResponse<SystemUserService.UserDetail> updateUser(
            @PathVariable Long id,
            @RequestBody SystemUserService.UpdateUserRequest request) {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.API_SYSTEM_USER_UPDATE);
        return ApiResponse.ok(systemUserService.updateUser(id, request));
    }

    /**
     * @Author lxy
     * @Description //切换指定用户的启用/禁用状态
     * @Date 2026/04/18 09:50
     * @Param [id 用户ID, request 状态更新请求，包含目标状态]
     * @return ApiResponse<SystemUserService.UserDetail> 更新后的用户详情
     */
    @PutMapping("/{id}/status")
    public ApiResponse<SystemUserService.UserDetail> toggleStatus(
            @PathVariable Long id,
            @RequestBody SystemUserService.UpdateUserStatusRequest request) {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.API_SYSTEM_USER_STATUS);
        return ApiResponse.ok(systemUserService.toggleStatus(id, request));
    }

    /**
     * @Author lxy
     * @Description //为指定用户分配角色，覆盖原有角色列表
     * @Date 2026/04/18 09:50
     * @Param [id 用户ID, request 角色分配请求，包含角色ID列表]
     * @return ApiResponse<SystemUserService.UserDetail> 更新后的用户详情
     */
    @PutMapping("/{id}/roles")
    public ApiResponse<SystemUserService.UserDetail> assignRoles(
            @PathVariable Long id,
            @RequestBody SystemUserService.AssignUserRolesRequest request) {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.API_SYSTEM_USER_ASSIGN_ROLES);
        return ApiResponse.ok(systemUserService.assignRoles(id, request));
    }

    /**
     * @Author lxy
     * @Description //修改指定用户的登录密码
     * @Date 2026/04/18 09:50
     * @Param [id 用户ID, request 密码修改请求，包含新密码]
     * @return ApiResponse<Void> 无返回数据
     */
    @PutMapping("/{id}/password")
    public ApiResponse<Void> changePassword(
            @PathVariable Long id,
            @RequestBody ChangePasswordRequest request) {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.API_SYSTEM_USER_CHANGE_PASSWORD);
        systemUserService.changePassword(id, request.newPassword());
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteUser(@PathVariable Long id) {
        AuthenticatedUser currentUser = currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.API_SYSTEM_USER_DELETE);
        systemUserService.deleteUser(id, currentUser.id());
        return ApiResponse.ok(null);
    }

    public record ChangePasswordRequest(String newPassword) {
    }
}
