package com.changping.platform.modules.system.controller;

import com.changping.platform.common.response.ApiResponse;
import com.changping.platform.modules.auth.security.PermissionCodes;
import com.changping.platform.modules.auth.security.PermissionGuard;
import com.changping.platform.modules.auth.service.AuthService;
import com.changping.platform.modules.auth.service.CurrentUserService;
import com.changping.platform.modules.system.service.SystemMenuService;
import com.changping.platform.modules.system.service.SystemPermissionService;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author lxy
 * @Description //系统菜单控制器，提供菜单树查询、创建、更新及删除接口
 * @Date 2026/04/18 09:35
 */
@RestController
@RequestMapping("/system/menus")
public class SystemMenuController {

    private final SystemMenuService systemMenuService;
    private final CurrentUserService currentUserService;
    private final PermissionGuard permissionGuard;

    /**
     * @Author lxy
     * @Description //构造函数，注入菜单服务、当前用户服务及权限校验
     * @Date 2026/04/18 09:35
     * @Param [systemMenuService 菜单服务, currentUserService 当前用户服务, permissionGuard 权限校验]
     * @return
     */
    public SystemMenuController(
            SystemMenuService systemMenuService,
            CurrentUserService currentUserService,
            PermissionGuard permissionGuard) {
        this.systemMenuService = systemMenuService;
        this.currentUserService = currentUserService;
        this.permissionGuard = permissionGuard;
    }

    /**
     * @Author lxy
     * @Description //查询菜单树，返回目录、菜单、按钮的树形结构
     * @Date 2026/04/18 09:35
     * @Param []
     * @return ApiResponse<List<SystemPermissionService.PermissionTreeNode>> 菜单权限树
     */
    @GetMapping("/tree")
    public ApiResponse<List<SystemPermissionService.PermissionTreeNode>> listMenus() {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.API_SYSTEM_MENU_LIST);
        return ApiResponse.ok(systemMenuService.listMenuTree());
    }

    /**
     * @Author lxy
     * @Description //创建菜单权限项
     * @Date 2026/04/18 09:35
     * @Param [request 创建菜单请求对象，包含权限编码、名称、类型、路径等]
     * @return ApiResponse<SystemPermissionService.PermissionRecord> 新建的权限记录
     */
    @PostMapping
    public ApiResponse<SystemPermissionService.PermissionRecord> createMenu(@RequestBody SystemMenuService.CreateMenuRequest request) {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.API_SYSTEM_MENU_CREATE);
        return ApiResponse.ok(systemMenuService.createMenu(request));
    }

    /**
     * @Author lxy
     * @Description //更新指定菜单权限项的信息
     * @Date 2026/04/18 09:35
     * @Param [id 菜单权限ID, request 更新请求对象]
     * @return ApiResponse<SystemPermissionService.PermissionRecord> 更新后的权限记录
     */
    @PutMapping("/{id}")
    public ApiResponse<SystemPermissionService.PermissionRecord> updateMenu(
            @PathVariable Long id,
            @RequestBody SystemMenuService.UpdateMenuRequest request) {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.API_SYSTEM_MENU_UPDATE);
        return ApiResponse.ok(systemMenuService.updateMenu(id, request));
    }

    /**
     * @Author lxy
     * @Description //删除指定菜单权限项，仅允许删除无子项且未被角色引用的菜单
     * @Date 2026/04/18 09:35
     * @Param [id 菜单权限ID]
     * @return ApiResponse<Void> 无返回数据
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteMenu(@PathVariable Long id) {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.API_SYSTEM_MENU_DELETE);
        systemMenuService.deleteMenu(id);
        return ApiResponse.ok(null);
    }
}
