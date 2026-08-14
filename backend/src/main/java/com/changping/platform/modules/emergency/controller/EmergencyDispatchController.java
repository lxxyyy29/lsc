package com.changping.platform.modules.emergency.controller;

import com.changping.platform.common.response.ApiResponse;
import com.changping.platform.modules.auth.security.PermissionCodes;
import com.changping.platform.modules.auth.security.PermissionGuard;
import com.changping.platform.modules.auth.service.AuthService;
import com.changping.platform.modules.auth.service.CurrentUserService;
import com.changping.platform.modules.emergency.service.EmergencyDispatchService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 应急会商/一键多方联合调度（B1）— Web 指挥端接口
 */
@RestController
@RequestMapping("/emergency/dispatches")
public class EmergencyDispatchController {

    private final EmergencyDispatchService service;
    private final CurrentUserService currentUserService;
    private final PermissionGuard permissionGuard;

    public EmergencyDispatchController(EmergencyDispatchService service, CurrentUserService currentUserService,
                                       PermissionGuard permissionGuard) {
        this.service = service;
        this.currentUserService = currentUserService;
        this.permissionGuard = permissionGuard;
    }

    /** 类型/级别字典 */
    @GetMapping("/meta")
    public ApiResponse<Map<String, Object>> meta() {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.API_EMERGENCY_DISPATCH_VIEW);
        return ApiResponse.ok(service.meta());
    }

    /** 指令分页列表 */
    @GetMapping
    public ApiResponse<Map<String, Object>> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String level,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.API_EMERGENCY_DISPATCH_VIEW);
        return ApiResponse.ok(service.list(status, level, page, size));
    }

    /** 指令详情（含回执列表） */
    @GetMapping("/{id}")
    public ApiResponse<Map<String, Object>> detail(@PathVariable Long id) {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.API_EMERGENCY_DISPATCH_VIEW);
        return ApiResponse.ok(service.detail(id));
    }

    /** 一键发起多方联合调度 */
    @PostMapping
    public ApiResponse<Map<String, Object>> create(@RequestBody Map<String, Object> body) {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.API_EMERGENCY_DISPATCH_MANAGE);
        var user = currentUserService.requireClientType(AuthService.ClientType.WEB);
        return ApiResponse.ok(service.create(
                str(body, "title"), str(body, "type"), str(body, "level"),
                longOrNull(body, "gridId"), str(body, "content"),
                longOrNull(body, "eventId"), str(body, "eventCode"),
                str(body, "videoCameraIds"), str(body, "meetingUrl"),
                user.id(), user.userName()));
    }

    /** 当前用户反馈回执（Web 端接收人同样可反馈） */
    @PostMapping("/{id}/receipt")
    public ApiResponse<Map<String, Object>> feedback(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.API_EMERGENCY_RECEIPT_FEEDBACK);
        Long userId = currentUserService.requireUserId();
        return ApiResponse.ok(service.feedback(id, userId, str(body, "status"), str(body, "feedback")));
    }

    /** 指挥端完成指令 */
    @PostMapping("/{id}/complete")
    public ApiResponse<Void> complete(@PathVariable Long id) {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.API_EMERGENCY_DISPATCH_MANAGE);
        service.complete(id);
        return ApiResponse.ok(null);
    }

    private static String str(Map<String, Object> body, String key) {
        Object v = body.get(key);
        return v == null ? null : String.valueOf(v);
    }

    private static Long longOrNull(Map<String, Object> body, String key) {
        Object v = body.get(key);
        if (v == null || String.valueOf(v).isBlank()) return null;
        return Long.valueOf(String.valueOf(v));
    }
}
