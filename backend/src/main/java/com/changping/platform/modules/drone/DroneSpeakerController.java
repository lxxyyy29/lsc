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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * @Author tangxinglin
 * @Description //无人机喊话器控制器，提供喊话器音频文件的上传、删除、列表查询及播放、停止、音量设置接口
 * @Date 2026/04/18 10:00
 */
@RestController
@RequestMapping("/drone/speaker")
public class DroneSpeakerController {

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
    public DroneSpeakerController(
            DroneProxyService droneProxyService,
            CurrentUserService currentUserService,
            PermissionGuard permissionGuard) {
        this.droneProxyService = droneProxyService;
        this.currentUserService = currentUserService;
        this.permissionGuard = permissionGuard;
    }

    /**
     * @Author tangxinglin
     * @Description //分页查询喊话器音频文件列表
     * @Date 2026/04/18 10:00
     * @Param [page 页码，默认为1, pageSize 每页条数，默认为10]
     * @return ApiResponse<DroneProxyService.PageResult<Map<String, Object>>> 分页音频文件列表
     */
    @GetMapping("/files")
    public ApiResponse<DroneProxyService.PageResult<Map<String, Object>>> listFiles(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.API_DRONE_SPEAKER_FILE_LIST);
        return ApiResponse.ok(droneProxyService.listSpeakerFiles(page, pageSize));
    }

    /**
     * @Author tangxinglin
     * @Description //上传喊话器音频文件（PCM格式）
     * @Date 2026/04/18 10:00
     * @Param [file 上传的音频文件]
     * @return ApiResponse<Map<String, Object>> 上传结果
     */
    @PostMapping("/files")
    public ApiResponse<Map<String, Object>> uploadFile(@RequestPart("file") MultipartFile file) {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.API_DRONE_SPEAKER_FILE_UPLOAD);
        return ApiResponse.ok(droneProxyService.uploadSpeakerFile(file));
    }

    /**
     * @Author tangxinglin
     * @Description //删除指定ID的喊话器音频文件
     * @Date 2026/04/18 10:00
     * @Param [id 音频文件ID]
     * @return ApiResponse<Map<String, Object>> 删除结果
     */
    @DeleteMapping("/files/{id}")
    public ApiResponse<Map<String, Object>> deleteFile(@PathVariable String id) {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.API_DRONE_SPEAKER_FILE_DELETE);
        return ApiResponse.ok(droneProxyService.deleteSpeakerFile(id));
    }

    /**
     * @Author tangxinglin
     * @Description //控制指定设备的喊话器播放指定音频文件
     * @Date 2026/04/18 10:00
     * @Param [deviceSn 设备序列号, request 包含音频文件ID的播放请求]
     * @return ApiResponse<Map<String, Object>> 播放指令结果
     */
    @PostMapping("/{deviceSn}/play")
    public ApiResponse<Map<String, Object>> play(@PathVariable String deviceSn, @Valid @RequestBody PlayRequest request) {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.API_DRONE_SPEAKER_PLAY);
        return ApiResponse.ok(droneProxyService.playSpeaker(deviceSn, request.fileId()));
    }

    /**
     * @Author tangxinglin
     * @Description //停止指定设备的喊话器播放
     * @Date 2026/04/18 10:00
     * @Param [deviceSn 设备序列号]
     * @return ApiResponse<Map<String, Object>> 停止播放结果
     */
    @PostMapping("/{deviceSn}/stop")
    public ApiResponse<Map<String, Object>> stop(@PathVariable String deviceSn) {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.API_DRONE_SPEAKER_STOP);
        return ApiResponse.ok(droneProxyService.stopSpeaker(deviceSn));
    }

    /**
     * @Author tangxinglin
     * @Description //设置指定设备喊话器的音量
     * @Date 2026/04/18 10:00
     * @Param [deviceSn 设备序列号, request 包含音量值的请求体（0-100）]
     * @return ApiResponse<Map<String, Object>> 音量设置结果
     */
    @PostMapping("/{deviceSn}/volume")
    public ApiResponse<Map<String, Object>> volume(@PathVariable String deviceSn, @Valid @RequestBody VolumeRequest request) {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.API_DRONE_SPEAKER_VOLUME);
        return ApiResponse.ok(droneProxyService.setSpeakerVolume(deviceSn, request.volume()));
    }

    public record PlayRequest(@NotNull Long fileId) {
    }

    public record VolumeRequest(@NotNull @Min(0) @Max(100) Integer volume) {
    }
}
