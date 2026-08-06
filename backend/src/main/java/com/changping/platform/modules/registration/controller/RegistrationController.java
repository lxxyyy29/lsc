package com.changping.platform.modules.registration.controller;

import com.changping.platform.common.response.ApiResponse;
import com.changping.platform.modules.auth.model.AuthenticatedUser;
import com.changping.platform.modules.auth.security.PermissionCodes;
import com.changping.platform.modules.auth.security.PermissionGuard;
import com.changping.platform.modules.auth.service.AuthService;
import com.changping.platform.modules.auth.service.CurrentUserService;
import com.changping.platform.modules.registration.service.RegistrationService;
import com.fasterxml.jackson.annotation.JsonProperty;
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
    public ApiResponse<Void> submit(@RequestBody SubmitRequest body) {
        registrationService.submit(
            body.account(),
            passwordEncoder.encode(body.password()),
            body.realName(),
            body.phone()
        );
        return ApiResponse.ok(null);
    }

    public record SubmitRequest(
        @JsonProperty("account") String account,
        @JsonProperty("password") String password,
        @JsonProperty("realName") String realName,
        @JsonProperty("phone") String phone
    ) {}

    @GetMapping("/pending")
    public ApiResponse<List<Map<String, Object>>> listPending() {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.API_SYSTEM_USER_LIST);
        return ApiResponse.ok(registrationService.listPending());
    }

    @PostMapping("/{id}/approve")
    public ApiResponse<Void> approve(@PathVariable Long id, @RequestBody ApproveRejectRequest body) {
        AuthenticatedUser currentUser = currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.API_SYSTEM_USER_CREATE);
        // 审批时分配身份：memberType = GRID_WORKER(网格员，默认) / STAFF(社区工作人员)，
        // 审批通过后自动分配对应角色（可登录 H5 端巡查）并同步到组织人员管理
        registrationService.approve(id, currentUser.id(), body.remark() != null ? body.remark() : "审批通过", body.memberType());
        return ApiResponse.ok(null);
    }

    @PostMapping("/{id}/reject")
    public ApiResponse<Void> reject(@PathVariable Long id, @RequestBody ApproveRejectRequest body) {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.API_SYSTEM_USER_CREATE);
        registrationService.reject(id, body.remark() != null ? body.remark() : "拒绝");
        return ApiResponse.ok(null);
    }

    public record ApproveRejectRequest(String remark, String memberType) {}
}
