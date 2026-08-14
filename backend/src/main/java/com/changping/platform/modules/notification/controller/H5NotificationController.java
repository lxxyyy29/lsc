package com.changping.platform.modules.notification.controller;

import com.changping.platform.common.response.ApiResponse;
import com.changping.platform.modules.auth.security.PermissionCodes;
import com.changping.platform.modules.auth.security.PermissionGuard;
import com.changping.platform.modules.auth.service.AuthService;
import com.changping.platform.modules.auth.service.CurrentUserService;
import com.changping.platform.modules.notification.service.NotificationService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * H5 移动端站内通知接口（消息页：应急指令/预警等通知）
 */
@RestController
@RequestMapping("/h5/notifications")
public class H5NotificationController {

    private final NotificationService service;
    private final CurrentUserService currentUserService;
    private final PermissionGuard permissionGuard;

    public H5NotificationController(NotificationService service, CurrentUserService currentUserService,
                                    PermissionGuard permissionGuard) {
        this.service = service;
        this.currentUserService = currentUserService;
        this.permissionGuard = permissionGuard;
    }

    /** 我的通知分页列表 */
    @GetMapping
    public ApiResponse<Map<String, Object>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        var user = currentUserService.requireClientType(AuthService.ClientType.H5);
        permissionGuard.require(PermissionCodes.API_H5_NOTIFICATION_VIEW);
        return ApiResponse.ok(service.findByUserId(user.id(), page, size));
    }

    /** 未读数量 */
    @GetMapping("/unread-count")
    public ApiResponse<Long> unreadCount() {
        var user = currentUserService.requireClientType(AuthService.ClientType.H5);
        permissionGuard.require(PermissionCodes.API_H5_NOTIFICATION_VIEW);
        return ApiResponse.ok(service.countUnread(user.id()));
    }

    /** 标记已读 */
    @PostMapping("/{id}/read")
    public ApiResponse<Boolean> markAsRead(@PathVariable Long id) {
        var user = currentUserService.requireClientType(AuthService.ClientType.H5);
        permissionGuard.require(PermissionCodes.API_H5_NOTIFICATION_VIEW);
        return ApiResponse.ok(service.markAsRead(id, user.id()));
    }

    /** 全部已读 */
    @PostMapping("/read-all")
    public ApiResponse<Integer> markAllAsRead() {
        var user = currentUserService.requireClientType(AuthService.ClientType.H5);
        permissionGuard.require(PermissionCodes.API_H5_NOTIFICATION_VIEW);
        return ApiResponse.ok(service.markAllAsRead(user.id()));
    }
}
