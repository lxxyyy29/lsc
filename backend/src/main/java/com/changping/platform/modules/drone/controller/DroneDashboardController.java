package com.changping.platform.modules.drone.controller;

import com.changping.platform.common.response.ApiResponse;
import com.changping.platform.modules.auth.security.PermissionCodes;
import com.changping.platform.modules.auth.security.PermissionGuard;
import com.changping.platform.modules.auth.service.AuthService;
import com.changping.platform.modules.auth.service.CurrentUserService;
import com.changping.platform.modules.drone.DroneProxyService;
import com.changping.platform.modules.drone.config.DroneApiProperties;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/drone/dashboard")
public class DroneDashboardController {

    private final DroneProxyService droneProxyService;
    private final DroneApiProperties droneApiProperties;
    private final JdbcTemplate jdbcTemplate;
    private final CurrentUserService currentUserService;
    private final PermissionGuard permissionGuard;

    public DroneDashboardController(
            DroneProxyService droneProxyService,
            DroneApiProperties droneApiProperties,
            JdbcTemplate jdbcTemplate,
            CurrentUserService currentUserService,
            PermissionGuard permissionGuard) {
        this.droneProxyService = droneProxyService;
        this.droneApiProperties = droneApiProperties;
        this.jdbcTemplate = jdbcTemplate;
        this.currentUserService = currentUserService;
        this.permissionGuard = permissionGuard;
    }

    /**
     * 无人机看板总览
     */
    @GetMapping("/overview")
    public ApiResponse<Map<String, Object>> overview() {
        requireDroneDashboardPermission();
        Map<String, Object> result = new HashMap<>();

        try {
            // 设备统计
            DroneProxyService.PageResult<Map<String, Object>> devices = droneProxyService.listDevices(droneApiProperties.getFixedWorkspaceId(), 1, 100);
            long onlineCount = devices.items().stream().filter(d -> "ONLINE".equals(d.get("deviceStatus")) || "online".equals(String.valueOf(d.get("status")))).count();
            result.put("totalDevices", devices.total());
            result.put("onlineDevices", onlineCount);
        } catch (Exception e) {
            result.put("totalDevices", 0);
            result.put("onlineDevices", 0);
        }

        try {
            // 任务统计
            DroneProxyService.PageResult<Map<String, Object>> jobs = droneProxyService.listJobs(droneApiProperties.getFixedWorkspaceId(), 1, 100, null);
            long runningJobs = jobs.items().stream().filter(j -> Integer.valueOf(1).equals(j.get("status")) || "RUNNING".equals(j.get("jobStatus"))).count();
            result.put("totalJobs", jobs.total());
            result.put("runningJobs", runningJobs);
        } catch (Exception e) {
            result.put("totalJobs", 0);
            result.put("runningJobs", 0);
        }

        try {
            // 航线数量
            DroneProxyService.PageResult<Map<String, Object>> waylines = droneProxyService.listWaylines(droneApiProperties.getFixedWorkspaceId(), 1, 100);
            result.put("totalWaylines", waylines.total());
        } catch (Exception e) {
            result.put("totalWaylines", 0);
        }

        try {
            // AI告警（从告警事件表中统计）
            Long aiAlerts = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM biz_event WHERE source_type = 'AI_CAMERA' AND status NOT IN ('CLOSED', 'IGNORED')", Long.class);
            result.put("aiAlerts", aiAlerts != null ? aiAlerts : 0);
        } catch (Exception e) {
            result.put("aiAlerts", 0);
        }

        try {
            // 今日巡检次数
            Long todayInspections = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM biz_patrol_task WHERE DATE(completed_at) = CURDATE() AND status = 'COMPLETED'", Long.class);
            result.put("todayInspections", todayInspections != null ? todayInspections : 0);
        } catch (Exception e) {
            result.put("todayInspections", 0);
        }

        return ApiResponse.ok(result);
    }

    /**
     * 获取设备列表（带状态）
     */
    @GetMapping("/devices")
    public ApiResponse<List<Map<String, Object>>> devices() {
        requireDroneDashboardPermission();
        try {
            DroneProxyService.PageResult<Map<String, Object>> result = droneProxyService.listDevices(droneApiProperties.getFixedWorkspaceId(), 1, 100);
            return ApiResponse.ok(result.items());
        } catch (Exception e) {
            return ApiResponse.ok(List.of());
        }
    }

    /**
     * 获取任务列表
     */
    @GetMapping("/jobs")
    public ApiResponse<List<Map<String, Object>>> jobs(@RequestParam(required = false) Integer status) {
        requireDroneDashboardPermission();
        try {
            DroneProxyService.PageResult<Map<String, Object>> result = droneProxyService.listJobs(droneApiProperties.getFixedWorkspaceId(), 1, 50, status);
            return ApiResponse.ok(result.items());
        } catch (Exception e) {
            return ApiResponse.ok(List.of());
        }
    }

    /**
     * 获取航线列表
     */
    @GetMapping("/waylines")
    public ApiResponse<List<Map<String, Object>>> waylines() {
        requireDroneDashboardPermission();
        try {
            DroneProxyService.PageResult<Map<String, Object>> result = droneProxyService.listWaylines(droneApiProperties.getFixedWorkspaceId(), 1, 50);
            return ApiResponse.ok(result.items());
        } catch (Exception e) {
            return ApiResponse.ok(List.of());
        }
    }

    /**
     * 获取AI告警事件
     */
    @GetMapping("/ai-alerts")
    public ApiResponse<List<Map<String, Object>>> aiAlerts() {
        requireDroneDashboardPermission();
        try {
            List<Map<String, Object>> alerts = jdbcTemplate.queryForList(
                "SELECT e.event_code, e.title, e.event_type, e.urgency_level, e.status, e.occurred_at, e.incident_address, " +
                "g.grid_name FROM biz_event e LEFT JOIN cmn_grid g ON g.id = e.grid_id " +
                "WHERE e.source_type = 'AI_CAMERA' ORDER BY e.created_at DESC LIMIT 50");
            return ApiResponse.ok(alerts);
        } catch (Exception e) {
            return ApiResponse.ok(List.of());
        }
    }

    private void requireDroneDashboardPermission() {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.requireAny(
                PermissionCodes.API_DRONE_DEVICE_LIST,
                PermissionCodes.API_DRONE_JOB_LIST,
                PermissionCodes.API_DRONE_WAYLINE_LIST,
                PermissionCodes.API_DRONE_WS_CONNECT);
    }
}
