package com.changping.platform.modules.safety.controller;

import com.changping.platform.common.response.ApiResponse;
import com.changping.platform.modules.auth.model.AuthenticatedUser;
import com.changping.platform.modules.auth.security.PermissionCodes;
import com.changping.platform.modules.auth.security.PermissionGuard;
import com.changping.platform.modules.auth.service.AuthService;
import com.changping.platform.modules.auth.service.CurrentUserService;
import com.changping.platform.modules.safety.entity.SafetyInspectionEntity;
import com.changping.platform.modules.safety.service.SafetyInspectionService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/safety/inspections")
public class SafetyInspectionController {

    private final SafetyInspectionService service;
    private final CurrentUserService currentUserService;
    private final PermissionGuard permissionGuard;

    public SafetyInspectionController(
            SafetyInspectionService service,
            CurrentUserService currentUserService,
            PermissionGuard permissionGuard) {
        this.service = service;
        this.currentUserService = currentUserService;
        this.permissionGuard = permissionGuard;
    }

    @GetMapping
    public ApiResponse<List<SafetyInspectionEntity>> list(@RequestParam(required = false) Long merchantId) {
        requireSafetyViewPermission();
        if (merchantId != null) {
            return ApiResponse.ok(service.getByMerchant(merchantId));
        }
        return ApiResponse.ok(service.getAll());
    }

    @GetMapping("/statistics")
    public ApiResponse<Object> statistics() {
        requireSafetyViewPermission();
        return ApiResponse.ok(service.getStatistics());
    }

    @PostMapping
    public ApiResponse<Boolean> create(@RequestBody SafetyInspectionEntity entity) {
        requireSafetyManagePermission();
        AuthenticatedUser user = currentUserService.requireClientType(AuthService.ClientType.WEB);
        entity.setInspectorId(user.id());
        entity.setInspectorName(user.userName());
        if (entity.getInspectionDate() == null) {
            entity.setInspectionDate(java.time.LocalDate.now());
        }
        if (entity.getRectificationStatus() == null) {
            entity.setRectificationStatus("PENDING");
        }
        service.createInspection(entity);
        return ApiResponse.ok(true);
    }

    @PutMapping("/{id}/status")
    public ApiResponse<Boolean> updateStatus(@PathVariable Long id, @RequestBody java.util.Map<String, String> body) {
        requireSafetyManagePermission();
        String status = body.get("status");
        return ApiResponse.ok(service.updateStatus(id, status));
    }

    @PostMapping("/mark-overdue")
    public ApiResponse<Integer> markOverdue() {
        requireSafetyManagePermission();
        return ApiResponse.ok(service.markOverdue());
    }

    private void requireSafetyViewPermission() {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.API_SAFETY_VIEW);
    }

    private void requireSafetyManagePermission() {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.API_SAFETY_MANAGE);
    }
}
