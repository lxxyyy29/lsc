package com.changping.platform.modules.safety.controller;

import com.changping.platform.common.response.ApiResponse;
import com.changping.platform.modules.auth.security.PermissionCodes;
import com.changping.platform.modules.auth.security.PermissionGuard;
import com.changping.platform.modules.auth.service.AuthService;
import com.changping.platform.modules.auth.service.CurrentUserService;
import com.changping.platform.modules.safety.service.MosquitoService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 卫生防疫/爱卫 — 蚊媒孳生地三色分级 + 消杀记录 + 重点场所卫生监测（C4）
 */
@RestController
@RequestMapping("/community/mosquito")
public class MosquitoController {

    private final MosquitoService service;
    private final CurrentUserService currentUserService;
    private final PermissionGuard permissionGuard;

    public MosquitoController(MosquitoService service, CurrentUserService currentUserService,
                              PermissionGuard permissionGuard) {
        this.service = service;
        this.currentUserService = currentUserService;
        this.permissionGuard = permissionGuard;
    }

    // ==================== 蚊媒孳生地 ====================

    @GetMapping("/sites")
    public ApiResponse<Map<String, Object>> listSites(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String level,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.API_SAFETY_VIEW);
        return ApiResponse.ok(service.listSites(status, level, page, size));
    }

    @GetMapping("/sites/statistics")
    public ApiResponse<Map<String, Object>> siteStatistics() {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.API_SAFETY_VIEW);
        return ApiResponse.ok(service.siteStatistics());
    }

    @PostMapping("/sites")
    public ApiResponse<Long> createSite(@RequestBody Map<String, Object> body) {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.API_SAFETY_MANAGE);
        return ApiResponse.ok(service.createSite(body));
    }

    @PutMapping("/sites/{id}")
    public ApiResponse<Boolean> updateSite(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.API_SAFETY_MANAGE);
        return ApiResponse.ok(service.updateSite(id, body));
    }

    /** 标记消除 */
    @PostMapping("/sites/{id}/eliminate")
    public ApiResponse<Boolean> eliminateSite(@PathVariable Long id) {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.API_SAFETY_MANAGE);
        return ApiResponse.ok(service.eliminateSite(id));
    }

    @DeleteMapping("/sites/{id}")
    public ApiResponse<Boolean> deleteSite(@PathVariable Long id) {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.API_SAFETY_MANAGE);
        return ApiResponse.ok(service.deleteSite(id));
    }

    // ==================== 消杀记录 ====================

    @GetMapping("/disinfections")
    public ApiResponse<Map<String, Object>> listDisinfections(
            @RequestParam(required = false) Long siteId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.API_SAFETY_VIEW);
        return ApiResponse.ok(service.listDisinfections(siteId, page, size));
    }

    @PostMapping("/disinfections")
    public ApiResponse<Long> createDisinfection(@RequestBody Map<String, Object> body) {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.API_SAFETY_MANAGE);
        return ApiResponse.ok(service.createDisinfection(body));
    }

    @DeleteMapping("/disinfections/{id}")
    public ApiResponse<Boolean> deleteDisinfection(@PathVariable Long id) {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.API_SAFETY_MANAGE);
        return ApiResponse.ok(service.deleteDisinfection(id));
    }

    // ==================== 重点场所卫生监测 ====================

    @GetMapping("/monitors")
    public ApiResponse<Map<String, Object>> listMonitors(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.API_SAFETY_VIEW);
        return ApiResponse.ok(service.listMonitors(page, size));
    }

    @GetMapping("/monitors/statistics")
    public ApiResponse<Map<String, Object>> monitorStatistics() {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.API_SAFETY_VIEW);
        return ApiResponse.ok(service.monitorStatistics());
    }

    @PostMapping("/monitors")
    public ApiResponse<Long> createMonitor(@RequestBody Map<String, Object> body) {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.API_SAFETY_MANAGE);
        return ApiResponse.ok(service.createMonitor(body));
    }

    @PutMapping("/monitors/{id}")
    public ApiResponse<Boolean> updateMonitor(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.API_SAFETY_MANAGE);
        return ApiResponse.ok(service.updateMonitor(id, body));
    }

    @DeleteMapping("/monitors/{id}")
    public ApiResponse<Boolean> deleteMonitor(@PathVariable Long id) {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.API_SAFETY_MANAGE);
        return ApiResponse.ok(service.deleteMonitor(id));
    }

    // ==================== 检测设备接入（设备台账 + 监测数据流） ====================

    @GetMapping("/devices")
    public ApiResponse<Map<String, Object>> listDevices(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.API_SAFETY_VIEW);
        return ApiResponse.ok(service.listDevices(page, size));
    }

    @GetMapping("/device-data")
    public ApiResponse<Map<String, Object>> listDeviceData(
            @RequestParam(required = false) Long siteId,
            @RequestParam(required = false) String deviceNo,
            @RequestParam(required = false) String metricType,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.API_SAFETY_VIEW);
        return ApiResponse.ok(service.listDeviceData(siteId, deviceNo, metricType, page, size));
    }

    @GetMapping("/device-data/trend")
    public ApiResponse<java.util.List<Map<String, Object>>> deviceTrend(
            @RequestParam(required = false) Long siteId,
            @RequestParam(required = false) String deviceNo,
            @RequestParam(required = false) String metricType,
            @RequestParam(required = false) Integer hours) {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.API_SAFETY_VIEW);
        return ApiResponse.ok(service.deviceTrend(siteId, deviceNo, metricType, hours));
    }

    @GetMapping("/device-data/statistics")
    public ApiResponse<Map<String, Object>> deviceStatistics() {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.API_SAFETY_VIEW);
        return ApiResponse.ok(service.deviceStatistics());
    }

    /** 一键生成演示数据（真实设备接入前用于效果演示，接入后仅保留推送通道） */
    @PostMapping("/device-data/simulate")
    public ApiResponse<Map<String, Object>> simulateDeviceData(@RequestBody(required = false) Map<String, Object> body) {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.API_SAFETY_MANAGE);
        int days = 3;
        if (body != null && body.get("days") instanceof Number) {
            days = ((Number) body.get("days")).intValue();
        }
        return ApiResponse.ok(service.simulateDeviceData(days));
    }
}
