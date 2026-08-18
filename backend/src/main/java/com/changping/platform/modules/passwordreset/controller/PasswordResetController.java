package com.changping.platform.modules.passwordreset.controller;

import com.changping.platform.common.response.ApiResponse;
import com.changping.platform.modules.auth.model.AuthenticatedUser;
import com.changping.platform.modules.auth.security.PermissionCodes;
import com.changping.platform.modules.auth.security.PermissionGuard;
import com.changping.platform.modules.auth.service.AuthService;
import com.changping.platform.modules.auth.service.CurrentUserService;
import com.changping.platform.modules.passwordreset.service.PasswordResetService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 密码重置申请：小程序端公开接口（提交/查进度）+ web 管理员处理接口。
 */
@RestController
@RequestMapping("/password-reset")
public class PasswordResetController {

    private final PasswordResetService passwordResetService;
    private final CurrentUserService currentUserService;
    private final PermissionGuard permissionGuard;

    public PasswordResetController(PasswordResetService passwordResetService,
                                   CurrentUserService currentUserService,
                                   PermissionGuard permissionGuard) {
        this.passwordResetService = passwordResetService;
        this.currentUserService = currentUserService;
        this.permissionGuard = permissionGuard;
    }

    /** 小程序端提交重置申请（公开，账号+注册手机号校验） */
    @PostMapping("/submit")
    public ApiResponse<Void> submit(@RequestBody SubmitRequest body) {
        passwordResetService.submit(body.account(), body.phone());
        return ApiResponse.ok(null);
    }

    /** 小程序端查询申请进度（公开） */
    @GetMapping("/status")
    public ApiResponse<Map<String, Object>> status(@RequestParam String account, @RequestParam String phone) {
        return ApiResponse.ok(passwordResetService.status(account, phone));
    }

    /** web 管理员查看申请列表 */
    @GetMapping("/list")
    public ApiResponse<List<Map<String, Object>>> list(@RequestParam(required = false) String status) {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.API_PASSWORD_RESET_HANDLE);
        return ApiResponse.ok(passwordResetService.list(status));
    }

    /** web 管理员批准：重置为手机号后 6 位并返回新密码（由管理员线下转达用户） */
    @PostMapping("/{id}/approve")
    public ApiResponse<Map<String, Object>> approve(@PathVariable Long id) {
        AuthenticatedUser user = currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.API_PASSWORD_RESET_HANDLE);
        String newPassword = passwordResetService.approve(id, user.id());
        return ApiResponse.ok(Map.of("newPassword", newPassword));
    }

    /** web 管理员驳回 */
    @PostMapping("/{id}/reject")
    public ApiResponse<Void> reject(@PathVariable Long id, @RequestBody(required = false) RejectRequest body) {
        AuthenticatedUser user = currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.API_PASSWORD_RESET_HANDLE);
        passwordResetService.reject(id, user.id(), body != null && body.remark() != null ? body.remark() : "驳回");
        return ApiResponse.ok(null);
    }

    public record SubmitRequest(String account, String phone) {
    }

    public record RejectRequest(String remark) {
    }
}
