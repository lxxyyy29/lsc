package com.changping.platform.modules.drone;

import com.changping.platform.common.response.ApiResponse;
import com.changping.platform.modules.auth.security.PermissionCodes;
import com.changping.platform.modules.auth.security.PermissionGuard;
import com.changping.platform.modules.auth.service.AuthService;
import com.changping.platform.modules.auth.service.CurrentUserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author tangxinglin
 * @Description //无人机工作空间控制器，提供工作空间列表分页查询接口
 * @Date 2026/04/18 10:00
 */
@RestController
@RequestMapping("/drone/workspaces")
public class DroneWorkspaceController {

    private final DroneProxyService droneProxyService;
    private final CurrentUserService currentUserService;
    private final PermissionGuard permissionGuard;

    /**
     * @Author tangxinglin
     * @Description //构造函数，注入无人机代理服务、当前用户服务及权限守卫
     * @Date 2026/04/18 10:00
     * @Param [droneProxyService 无人机代理服务, currentUserService 当前用户服务, permissionGuard 权限守卫]
     * @return void
     */
    public DroneWorkspaceController(
            DroneProxyService droneProxyService,
            CurrentUserService currentUserService,
            PermissionGuard permissionGuard) {
        this.droneProxyService = droneProxyService;
        this.currentUserService = currentUserService;
        this.permissionGuard = permissionGuard;
    }

    /**
     * @Author tangxinglin
     * @Description //分页查询无人机工作空间列表
     * @Date 2026/04/18 10:00
     * @Param [page 页码，默认为1, pageSize 每页条数，默认为10]
     * @return ApiResponse<DroneProxyService.PageResult<Map<String, Object>>> 分页工作空间列表
     */
    @GetMapping
    public ApiResponse<DroneProxyService.PageResult<java.util.Map<String, Object>>> listWorkspaces(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.API_DRONE_WORKSPACE_LIST);
        return ApiResponse.ok(droneProxyService.listWorkspaces(page, pageSize));
    }
}
