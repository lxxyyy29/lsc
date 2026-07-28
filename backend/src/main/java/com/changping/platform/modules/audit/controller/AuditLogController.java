package com.changping.platform.modules.audit.controller;

import com.changping.platform.common.response.ApiResponse;
import com.changping.platform.modules.audit.entity.AuditLogEntity;
import com.changping.platform.modules.audit.service.AuditLogService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/audit-logs")
public class AuditLogController {

    private final AuditLogService service;

    public AuditLogController(AuditLogService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<Map<String, Object>> query(
            @RequestParam(required = false) String tableName,
            @RequestParam(required = false) String recordId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(service.queryPaged(tableName, recordId, page, size));
    }

    @GetMapping("/history")
    public ApiResponse<List<AuditLogEntity>> history(
            @RequestParam String tableName,
            @RequestParam String recordId) {
        return ApiResponse.ok(service.getHistory(tableName, recordId));
    }

    @GetMapping("/tables")
    public ApiResponse<List<String>> tables() {
        return ApiResponse.ok(service.getTables());
    }

    @PostMapping("/rollback/{id}")
    public ApiResponse<Boolean> rollback(@PathVariable Long id) {
        try {
            boolean result = service.rollbackToVersion(id);
            return ApiResponse.ok(result);
        } catch (Exception e) {
            return ApiResponse.fail("ROLLBACK_FAILED", "回滚失败: " + e.getMessage());
        }
    }
}
