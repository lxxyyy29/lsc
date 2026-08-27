package com.changping.platform.modules.drone;

import com.changping.platform.common.response.ApiResponse;
import com.changping.platform.modules.auth.security.PermissionCodes;
import com.changping.platform.modules.auth.security.PermissionGuard;
import com.changping.platform.modules.auth.service.AuthService;
import com.changping.platform.modules.auth.service.CurrentUserService;
import com.changping.platform.modules.drone.config.DroneApiProperties;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Map;
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
 * @Description //无人机任务控制器，提供任务列表查询、创建任务、暂停恢复任务及一键返航接口
 * @Date 2026/04/18 10:00
 */
@RestController
@RequestMapping("/drone/jobs")
public class DroneJobController {

    private final DroneProxyService droneProxyService;
    private final CurrentUserService currentUserService;
    private final PermissionGuard permissionGuard;
    private final DroneApiProperties droneApiProperties;

    /**
     * @Author lxy
     * @Description //构造函数，注入无人机代理服务、当前用户服务、权限守卫及API配置属性
     * @Date 2026/04/18 10:00
     * @Param [droneProxyService 无人机代理服务, currentUserService 当前用户服务, permissionGuard 权限守卫, droneApiProperties 无人机API配置]
     * @return void
     */
    public DroneJobController(
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
     * @Author lxy
     * @Description //分页查询无人机执行任务列表，支持按状态筛选
     * @Date 2026/04/18 10:00
     * @Param [page 页码，默认为1, pageSize 每页条数，默认为10, status 任务状态过滤（可选）]
     * @return ApiResponse<DroneProxyService.PageResult<Map<String, Object>>> 分页任务列表
     */
    @GetMapping
    public ApiResponse<DroneProxyService.PageResult<Map<String, Object>>> listJobs(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) Integer status) {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.API_DRONE_JOB_LIST);
        return ApiResponse.ok(droneProxyService.listJobs(droneApiProperties.getFixedWorkspaceId(), page, pageSize, status));
    }

    /**
     * @Author lxy
     * @Description //创建无人机立即执行任务
     * @Date 2026/04/18 10:00
     * @Param [request 创建任务请求，包含机巢序列号和航线文件ID]
     * @return ApiResponse<Map<String, Object>> 创建任务的结果
     */
    @PostMapping
    public ApiResponse<Map<String, Object>> createJob(@Valid @RequestBody CreateJobRequest request) {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.API_DRONE_JOB_CREATE);
        return ApiResponse.ok(droneProxyService.createJob(droneApiProperties.getFixedWorkspaceId(), request.dockSn(), request.fileId()));
    }

    /**
     * @Author lxy
     * @Description //暂停或恢复指定无人机任务
     * @Date 2026/04/18 10:00
     * @Param [jobId 任务ID, request 包含状态值的请求体（0暂停，1恢复）]
     * @return ApiResponse<Map<String, Object>> 操作结果
     */
    @PutMapping("/{jobId}/pause-resume")
    public ApiResponse<Map<String, Object>> pauseResumeJob(
            @PathVariable String jobId,
            @Valid @RequestBody PauseResumeJobRequest request) {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.API_DRONE_JOB_PAUSE_RESUME);
        return ApiResponse.ok(droneProxyService.pauseResumeJob(jobId, droneApiProperties.getFixedWorkspaceId(), request.status()));
    }

    /**
     * @Author lxy
     * @Description //发送无人机一键返航指令
     * @Date 2026/04/18 10:00
     * @Param [request 包含机巢序列号的返航请求]
     * @return ApiResponse<Map<String, Object>> 返航指令执行结果
     */
    @PostMapping("/return-home")
    public ApiResponse<Map<String, Object>> returnHome(@Valid @RequestBody ReturnHomeRequest request) {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.API_DRONE_JOB_RETURN_HOME);
        return ApiResponse.ok(droneProxyService.returnHome(request.dockSn()));
    }

    public record CreateJobRequest(@NotBlank String dockSn, @NotBlank String fileId) {
    }

    public record PauseResumeJobRequest(@NotNull @Min(0) @Max(1) Integer status) {
    }

    public record ReturnHomeRequest(@NotBlank String dockSn) {
    }
}
