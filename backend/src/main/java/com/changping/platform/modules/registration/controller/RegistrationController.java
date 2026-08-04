package com.changping.platform.modules.registration.controller;

import com.changping.platform.common.response.ApiResponse;
import com.changping.platform.modules.auth.model.AuthenticatedUser;
import com.changping.platform.modules.auth.security.PermissionCodes;
import com.changping.platform.modules.auth.security.PermissionGuard;
import com.changping.platform.modules.auth.service.AuthService;
import com.changping.platform.modules.auth.service.CurrentUserService;
import com.changping.platform.modules.registration.service.RegistrationService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/registration")
public class RegistrationController {

    private final RegistrationService registrationService;
    private final CurrentUserService currentUserService;
    private final PermissionGuard permissionGuard;

    private final PasswordEncoder passwordEncoder;

    public RegistrationController(RegistrationService registrationService,
                                   CurrentUserService currentUserService,
                                   PermissionGuard permissionGuard,
                                   PasswordEncoder passwordEncoder) {
        this.registrationService = registrationService;
        this.currentUserService = currentUserService;
        this.permissionGuard = permissionGuard;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/submit")
    public ApiResponse<Void> submit(@RequestBody Map<String, String> body) {
        registrationService.submit(
            body.get("account"),
            passwordEncoder.encode(body.get("password")),
            body.get("realName"),
            body.get("phone")
        );
        return ApiResponse.ok(null);
    }

    @GetMapping("/pending")
    public ApiResponse<List<Map<String, Object>>> listPending() {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.API_SYSTEM_USER_LIST);
        return ApiResponse.ok(registrationService.listPending());
    }

    @PostMapping("/{id}/approve")
    public ApiResponse<Void> approve(@PathVariable Long id, @RequestBody Map<String, String> body) {
        AuthenticatedUser currentUser = currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.API_SYSTEM_USER_CREATE);
        registrationService.approve(id, currentUser.id(), body.getOrDefault("remark", "审批通过"));
        return ApiResponse.ok(null);
    }

    @PostMapping("/{id}/reject")
    public ApiResponse<Void> reject(@PathVariable Long id, @RequestBody Map<String, String> body) {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.API_SYSTEM_USER_CREATE);
        registrationService.reject(id, body.getOrDefault("remark", "拒绝"));
        return ApiResponse.ok(null);
    }
}
