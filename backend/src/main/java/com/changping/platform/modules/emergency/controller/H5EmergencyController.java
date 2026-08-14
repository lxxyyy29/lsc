package com.changping.platform.modules.emergency.controller;

import com.changping.platform.common.response.ApiResponse;
import com.changping.platform.modules.auth.security.PermissionCodes;
import com.changping.platform.modules.auth.security.PermissionGuard;
import com.changping.platform.modules.auth.service.AuthService;
import com.changping.platform.modules.auth.service.CurrentUserService;
import com.changping.platform.modules.emergency.service.EmergencyDispatchService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 应急会商/一键多方联合调度（B1）— H5 移动端接口
 * 网格员在移动端接收指令、查看详情、反馈状态
 */
@RestController
@RequestMapping("/h5/emergency")
public class H5EmergencyController {

    private final EmergencyDispatchService service;
    private final CurrentUserService currentUserService;
    private final PermissionGuard permissionGuard;

    public H5EmergencyController(EmergencyDispatchService service, CurrentUserService currentUserService,
                                 PermissionGuard permissionGuard) {
        this.service = service;
        this.currentUserService = currentUserService;
        this.permissionGuard = permissionGuard;
    }

    /** 我收到的指令列表（含我的回执状态） */
    @GetMapping("/dispatches")
    public ApiResponse<List<Map<String, Object>>> myDispatches() {
        var user = currentUserService.requireClientType(AuthService.ClientType.H5);
        permissionGuard.require(PermissionCodes.API_H5_EMERGENCY_VIEW);
        return ApiResponse.ok(service.myDispatches(user.id()));
    }

    /** 指令详情（查看即标记已接收） */
    @GetMapping("/dispatches/{id}")
    public ApiResponse<Map<String, Object>> detail(@PathVariable Long id) {
        var user = currentUserService.requireClientType(AuthService.ClientType.H5);
        permissionGuard.require(PermissionCodes.API_H5_EMERGENCY_VIEW);
        Map<String, Object> result = service.detailForH5(id, user.id());
        return ApiResponse.ok(result);
    }

    /** 反馈状态：RESPONDING 响应中 / COMPLETED 已完成 */
    @PostMapping("/dispatches/{id}/receipt")
    public ApiResponse<Map<String, Object>> feedback(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        var user = currentUserService.requireClientType(AuthService.ClientType.H5);
        permissionGuard.require(PermissionCodes.API_H5_EMERGENCY_FEEDBACK);
        String status = body.get("status") == null ? null : String.valueOf(body.get("status"));
        String feedback = body.get("feedback") == null ? null : String.valueOf(body.get("feedback"));
        return ApiResponse.ok(service.feedback(id, user.id(), status, feedback));
    }
}
