package com.changping.platform.modules.drone;

import com.changping.platform.common.response.ApiResponse;
import com.changping.platform.modules.auth.security.PermissionCodes;
import com.changping.platform.modules.auth.security.PermissionGuard;
import com.changping.platform.modules.auth.service.AuthService;
import com.changping.platform.modules.auth.service.CurrentUserService;
import com.changping.platform.modules.drone.config.DroneApiProperties;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author tangxinglin
 * @Description //无人机航线控制器，提供航线列表分页查询及航线航点坐标获取接口
 * @Date 2026/04/18 10:00
 */
@RestController
@RequestMapping("/drone/waylines")
public class DroneWaylineController {

    private final DroneProxyService droneProxyService;
    private final CurrentUserService currentUserService;
    private final PermissionGuard permissionGuard;
    private final DroneApiProperties droneApiProperties;

    /**
     * @Author tangxinglin
     * @Description //构造函数，注入无人机代理服务、当前用户服务、权限守卫及API配置属性
     * @Date 2026/04/18 10:00
     * @Param [droneProxyService 无人机代理服务, currentUserService 当前用户服务, permissionGuard 权限守卫, droneApiProperties 无人机API配置]
     * @return void
     */
    public DroneWaylineController(
            DroneProxyService droneProxyService,
            CurrentUserService currentUserService,
            PermissionGuard permissionGuard,
            DroneApiProperties droneApiProperties) {
        this.droneProxyService = droneProxyService;
        this.currentUserService = currentUserService;
        this.permissionGuard = permissionGuard;
        this.droneApiProperties = droneApiProperties;
    }

    /**
     * @Author tangxinglin
     * @Description //分页查询无人机航线列表
     * @Date 2026/04/18 10:00
     * @Param [page 页码，默认为1, pageSize 每页条数，默认为10]
     * @return ApiResponse<DroneProxyService.PageResult<Map<String, Object>>> 分页航线列表
     */
    @GetMapping
    public ApiResponse<DroneProxyService.PageResult<Map<String, Object>>> listWaylines(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.API_DRONE_WAYLINE_LIST);
        return ApiResponse.ok(droneProxyService.listWaylines(droneApiProperties.getFixedWorkspaceId(), page, pageSize));
    }

    /**
     * @Author tangxinglin
     * @Description //获取指定航线的航点坐标列表
     * @Date 2026/04/18 10:00
     * @Param [id 航线ID]
     * @return ApiResponse<Map<String, Object>> 包含航点坐标数组的结果
     */
    @GetMapping("/{id}/points")
    public ApiResponse<Map<String, Object>> getWaylinePoints(@PathVariable String id) {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.API_DRONE_WAYLINE_POINTS);
        return ApiResponse.ok(droneProxyService.getWaylinePoints(droneApiProperties.getFixedWorkspaceId(), id));
    }
}
