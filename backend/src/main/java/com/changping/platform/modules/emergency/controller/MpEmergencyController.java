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
 * 应急会商/一键多方联合调度（B1）— 居民小程序只读接口（应急公告）
 *
 * 居民可查看社区发布的应急指令公告（标题/类型/级别/内容/响应进度），不可反馈。
 */
@RestController
@RequestMapping("/mp/emergency/dispatches")
public class MpEmergencyController {

    private final EmergencyDispatchService service;
    private final CurrentUserService currentUserService;
    private final PermissionGuard permissionGuard;

    public MpEmergencyController(EmergencyDispatchService service, CurrentUserService currentUserService,
                                 PermissionGuard permissionGuard) {
        this.service = service;
        this.currentUserService = currentUserService;
        this.permissionGuard = permissionGuard;
    }

    /** 应急公告列表（只读，含接收/响应人数统计） */
    @GetMapping
    public ApiResponse<Map<String, Object>> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String level,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.API_MP_EMERGENCY_VIEW);
        return ApiResponse.ok(service.list(status, level, page, size));
    }

    /** 应急公告详情（只读，不含回执个人明细） */
    @GetMapping("/{id}")
    public ApiResponse<Map<String, Object>> detail(@PathVariable Long id) {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.API_MP_EMERGENCY_VIEW);
        return ApiResponse.ok(service.detailForPublic(id));
    }
}
