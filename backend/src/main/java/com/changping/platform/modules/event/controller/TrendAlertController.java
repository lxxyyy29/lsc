package com.changping.platform.modules.event.controller;

import com.changping.platform.common.response.ApiResponse;
import com.changping.platform.modules.auth.security.PermissionCodes;
import com.changping.platform.modules.auth.security.PermissionGuard;
import com.changping.platform.modules.auth.service.AuthService;
import com.changping.platform.modules.auth.service.CurrentUserService;
import com.changping.platform.modules.event.service.TrendAlertService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 趋势预判/反复投诉自动预警（考核研判 A3）
 */
@RestController
@RequestMapping("/trend-alerts")
public class TrendAlertController {

    private final TrendAlertService service;
    private final CurrentUserService currentUserService;
    private final PermissionGuard permissionGuard;

    public TrendAlertController(TrendAlertService service, CurrentUserService currentUserService,
                                PermissionGuard permissionGuard) {
        this.service = service;
        this.currentUserService = currentUserService;
        this.permissionGuard = permissionGuard;
    }

    /** 分页查询预警列表（Web） */
    @GetMapping
    public ApiResponse<Map<String, Object>> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String dimension,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.API_ASSESSMENT_VIEW);
        return ApiResponse.ok(service.list(status, dimension, page, size));
    }

    /** 预警统计（Web） */
    @GetMapping("/statistics")
    public ApiResponse<Map<String, Object>> statistics() {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.API_ASSESSMENT_VIEW);
        return ApiResponse.ok(service.statistics());
    }

    /** 处理预警（Web） */
    @PostMapping("/{id}/handle")
    public ApiResponse<Boolean> handle(@PathVariable Long id, @RequestBody Map<String, String> body) {
        var user = currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.API_EVENT_CREATE);
        String remark = body.getOrDefault("remark", "");
        return ApiResponse.ok(service.handle(id, remark, user.id(), user.userName()));
    }

    /** 手动触发扫描（Web，用于验证与补扫） */
    @PostMapping("/scan")
    public ApiResponse<Integer> scan() {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.API_EVENT_CREATE);
        return ApiResponse.ok(service.scan());
    }
}
