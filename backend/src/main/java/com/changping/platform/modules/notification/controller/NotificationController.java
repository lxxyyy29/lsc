package com.changping.platform.modules.notification.controller;

import com.changping.platform.common.response.ApiResponse;
import com.changping.platform.modules.auth.security.PermissionGuard;
import com.changping.platform.modules.auth.service.CurrentUserService;
import com.changping.platform.modules.notification.service.NotificationService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationService service;
    private final CurrentUserService currentUserService;
    private final PermissionGuard permissionGuard;

    public NotificationController(NotificationService service, CurrentUserService currentUserService,
                                   PermissionGuard permissionGuard) {
        this.service = service;
        this.currentUserService = currentUserService;
        this.permissionGuard = permissionGuard;
    }

    /**
     * 分页查询当前用户通知
     */
    @GetMapping
    public ApiResponse<Map<String, Object>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long userId = currentUserService.requireUserId();
        return ApiResponse.ok(service.findByUserId(userId, page, size));
    }

    /**
     * 未读数量
     */
    @GetMapping("/unread-count")
    public ApiResponse<Long> unreadCount() {
        Long userId = currentUserService.requireUserId();
        return ApiResponse.ok(service.countUnread(userId));
    }

    /**
     * 标记已读
     */
    @PostMapping("/{id}/read")
    public ApiResponse<Boolean> markAsRead(@PathVariable Long id) {
        Long userId = currentUserService.requireUserId();
        return ApiResponse.ok(service.markAsRead(id, userId));
    }

    /**
     * 全部已读
     */
    @PostMapping("/read-all")
    public ApiResponse<Integer> markAllAsRead() {
        Long userId = currentUserService.requireUserId();
        return ApiResponse.ok(service.markAllAsRead(userId));
    }

    /**
     * 删除通知
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Boolean> delete(@PathVariable Long id) {
        Long userId = currentUserService.requireUserId();
        return ApiResponse.ok(service.delete(id, userId));
    }
}
