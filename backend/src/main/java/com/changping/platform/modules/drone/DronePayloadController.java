package com.changping.platform.modules.drone;

import com.changping.platform.common.response.ApiResponse;
import com.changping.platform.modules.auth.security.PermissionCodes;
import com.changping.platform.modules.auth.security.PermissionGuard;
import com.changping.platform.modules.auth.service.AuthService;
import com.changping.platform.modules.auth.service.CurrentUserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.Map;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author tangxinglin
 * @Description //无人机挂载载荷控制器，提供摄像头模式切换、开始录像及停止录像接口
 * @Date 2026/04/18 10:00
 */
@RestController
@RequestMapping("/drone/devices")
public class DronePayloadController {

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
    public DronePayloadController(
            DroneProxyService droneProxyService,
            CurrentUserService currentUserService,
            PermissionGuard permissionGuard) {
        this.droneProxyService = droneProxyService;
        this.currentUserService = currentUserService;
        this.permissionGuard = permissionGuard;
    }

    /**
     * @Author tangxinglin
     * @Description //切换指定无人机设备的摄像头模式
     * @Date 2026/04/18 10:00
     * @Param [deviceSn 设备序列号, request 包含摄像头模式值的请求体（0-3）]
     * @return ApiResponse<Map<String, Object>> 模式切换结果
     */
    @PostMapping("/{deviceSn}/camera/mode")
    public ApiResponse<Map<String, Object>> switchMode(@PathVariable String deviceSn, @Valid @RequestBody CameraModeRequest request) {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.API_DRONE_PAYLOAD_CAMERA_MODE);
        return ApiResponse.ok(droneProxyService.switchCameraMode(deviceSn, request.cameraMode()));
    }

    /**
     * @Author tangxinglin
     * @Description //启动指定无人机设备的摄像头录像
     * @Date 2026/04/18 10:00
     * @Param [deviceSn 设备序列号]
     * @return ApiResponse<Map<String, Object>> 开始录像结果
     */
    @PostMapping("/{deviceSn}/camera/record-start")
    public ApiResponse<Map<String, Object>> startRecording(@PathVariable String deviceSn) {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.API_DRONE_PAYLOAD_RECORD_START);
        return ApiResponse.ok(droneProxyService.startRecording(deviceSn));
    }

    /**
     * @Author tangxinglin
     * @Description //停止指定无人机设备的摄像头录像
     * @Date 2026/04/18 10:00
     * @Param [deviceSn 设备序列号]
     * @return ApiResponse<Map<String, Object>> 停止录像结果
     */
    @PostMapping("/{deviceSn}/camera/record-stop")
    public ApiResponse<Map<String, Object>> stopRecording(@PathVariable String deviceSn) {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.API_DRONE_PAYLOAD_RECORD_STOP);
        return ApiResponse.ok(droneProxyService.stopRecording(deviceSn));
    }

    public record CameraModeRequest(@NotNull @Min(0) @Max(3) Integer cameraMode) {
    }
}
