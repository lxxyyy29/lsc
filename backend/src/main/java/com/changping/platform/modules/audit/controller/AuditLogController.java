package com.changping.platform.modules.audit.controller;

import com.changping.platform.common.response.ApiResponse;
import com.changping.platform.modules.audit.entity.AuditLogEntity;
import com.changping.platform.modules.audit.service.AuditLogService;
import com.changping.platform.modules.auth.security.PermissionCodes;
import com.changping.platform.modules.auth.security.PermissionGuard;
import com.changping.platform.modules.auth.service.AuthService;
import com.changping.platform.modules.auth.service.CurrentUserService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/audit-logs")
public class AuditLogController {

    private final AuditLogService service;
    private final CurrentUserService currentUserService;
    private final PermissionGuard permissionGuard;

    public AuditLogController(AuditLogService service, CurrentUserService currentUserService, PermissionGuard permissionGuard) {
        this.service = service;
        this.currentUserService = currentUserService;
        this.permissionGuard = permissionGuard;
    }

    @GetMapping
    public ApiResponse<Map<String, Object>> query(
            @RequestParam(required = false) String tableName,
            @RequestParam(required = false) String recordId,
            @RequestParam(required = false) String operationType,
            @RequestParam(required = false) Long operatorId,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        requireViewPermission();
        return ApiResponse.ok(service.queryPaged(tableName, recordId, operationType, operatorId, startTime, endTime, page, size));
    }

    @GetMapping("/history")
    public ApiResponse<List<AuditLogEntity>> history(
            @RequestParam String tableName,
            @RequestParam String recordId) {
        requireViewPermission();
        return ApiResponse.ok(service.getHistory(tableName, recordId));
    }

    @GetMapping("/tables")
    public ApiResponse<List<String>> tables() {
        requireViewPermission();
        return ApiResponse.ok(service.getTables());
    }

    @PostMapping("/rollback/{id}")
    public ApiResponse<Boolean> rollback(@PathVariable Long id) {
        requireRollbackPermission();
        try {
            boolean result = service.rollbackToVersion(id);
            return ApiResponse.ok(result);
        } catch (Exception e) {
            return ApiResponse.fail("ROLLBACK_FAILED", "回滚失败: " + e.getMessage());
        }
    }

    /**
     * 获取字段级变更详情（Diff 展示）
     */
    @GetMapping("/{id}/diff")
    public ApiResponse<Map<String, Object>> diff(@PathVariable Long id) {
        requireViewPermission();
        return ApiResponse.ok(service.getDiffDetail(id));
    }

    /**
     * 预览回滚结果（返回将被修改的字段，不实际执行）
     */
    @GetMapping("/{id}/preview-rollback")
    public ApiResponse<Map<String, Object>> previewRollback(@PathVariable Long id) {
        requireRollbackPermission();
        try {
            return ApiResponse.ok(service.previewRollback(id));
        } catch (Exception e) {
            return ApiResponse.fail("PREVIEW_FAILED", "预览失败: " + e.getMessage());
        }
    }

    private void requireViewPermission() {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.API_AUDIT_LOG_VIEW);
    }

    private void requireRollbackPermission() {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.API_AUDIT_LOG_ROLLBACK);
    }
}
