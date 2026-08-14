package com.changping.platform.modules.vehicle.controller;

import com.changping.platform.common.response.ApiResponse;
import com.changping.platform.modules.auth.security.PermissionCodes;
import com.changping.platform.modules.auth.security.PermissionGuard;
import com.changping.platform.modules.auth.service.AuthService;
import com.changping.platform.modules.auth.service.CurrentUserService;
import com.changping.platform.modules.vehicle.service.VehicleTrackService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 车辆/人员轨迹追踪（C2）— Web 指挥端接口
 * 车辆进出记录 + 轨迹查询回放（7 天回溯），结合视频监控 AI 抓拍
 */
@RestController
@RequestMapping("/vehicle-tracks")
public class VehicleTrackController {

    private final VehicleTrackService service;
    private final CurrentUserService currentUserService;
    private final PermissionGuard permissionGuard;

    public VehicleTrackController(VehicleTrackService service, CurrentUserService currentUserService,
                                  PermissionGuard permissionGuard) {
        this.service = service;
        this.currentUserService = currentUserService;
        this.permissionGuard = permissionGuard;
    }

    /** 统计概览 */
    @GetMapping("/stats")
    public ApiResponse<Map<String, Object>> stats() {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.API_VEHICLE_TRACK_VIEW);
        return ApiResponse.ok(service.stats());
    }

    /** 车辆列表(最后位置/是否在社区内) */
    @GetMapping("/vehicles")
    public ApiResponse<List<Map<String, Object>>> vehicles(@RequestParam(required = false) String keyword) {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.API_VEHICLE_TRACK_VIEW);
        return ApiResponse.ok(service.vehicles(keyword));
    }

    /** 单车轨迹(时间升序) */
    @GetMapping("/trajectory")
    public ApiResponse<List<Map<String, Object>>> trajectory(
            @RequestParam String plate,
            @RequestParam(required = false) String start,
            @RequestParam(required = false) String end) {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.API_VEHICLE_TRACK_VIEW);
        return ApiResponse.ok(service.trajectory(plate, start, end));
    }

    /** 进出记录分页 */
    @GetMapping("/records")
    public ApiResponse<Map<String, Object>> records(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String start,
            @RequestParam(required = false) String end,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.API_VEHICLE_TRACK_VIEW);
        return ApiResponse.ok(service.records(type, start, end, page, size));
    }

    /** 生成演示数据(10 辆车最近 7 天轨迹) */
    @PostMapping("/generate-demo")
    public ApiResponse<Map<String, Object>> generateDemo() {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.API_VEHICLE_TRACK_VIEW);
        return ApiResponse.ok(service.generateDemo());
    }
}
