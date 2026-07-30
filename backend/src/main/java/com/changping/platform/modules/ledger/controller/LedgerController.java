package com.changping.platform.modules.ledger.controller;

import com.changping.platform.common.response.ApiResponse;
import com.changping.platform.modules.auth.security.PermissionCodes;
import com.changping.platform.modules.auth.security.PermissionGuard;
import com.changping.platform.modules.auth.service.AuthService;
import com.changping.platform.modules.auth.service.CurrentUserService;
import com.changping.platform.modules.ledger.entity.LedgerTemplateEntity;
import com.changping.platform.modules.ledger.service.LedgerService;
import com.changping.platform.modules.community.service.ExportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

@RestController
@RequestMapping("/ledger")
public class LedgerController {

    private final LedgerService ledgerService;
    private final ExportService exportService;
    private final CurrentUserService currentUserService;
    private final PermissionGuard permissionGuard;

    public LedgerController(
            LedgerService ledgerService,
            ExportService exportService,
            CurrentUserService currentUserService,
            PermissionGuard permissionGuard) {
        this.ledgerService = ledgerService;
        this.exportService = exportService;
        this.currentUserService = currentUserService;
        this.permissionGuard = permissionGuard;
    }

    @GetMapping("/templates")
    public ApiResponse<List<LedgerTemplateEntity>> getTemplates() {
        requireLedgerPermission();
        return ApiResponse.ok(ledgerService.getAllTemplates());
    }

    @GetMapping("/templates/{type}")
    public ApiResponse<List<LedgerTemplateEntity>> getTemplatesByType(@PathVariable String type) {
        requireLedgerPermission();
        return ApiResponse.ok(ledgerService.getTemplatesByType(type));
    }

    @PostMapping("/templates")
    public ApiResponse<Boolean> saveTemplate(@RequestBody LedgerTemplateEntity entity) {
        requireLedgerPermission();
        ledgerService.saveTemplate(entity);
        return ApiResponse.ok(true);
    }

    @DeleteMapping("/templates/{id}")
    public ApiResponse<Boolean> deleteTemplate(@PathVariable Long id) {
        requireLedgerPermission();
        ledgerService.deleteTemplate(id);
        return ApiResponse.ok(true);
    }

    @GetMapping("/data/{type}")
    public ApiResponse<List<Map<String, Object>>> getData(@PathVariable String type,
            @RequestParam(required = false) String gridId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        requireLedgerPermission();
        Map<String, String> filters = new HashMap<>();
        if (gridId != null) filters.put("gridId", gridId);
        if (status != null) filters.put("status", status);
        if (startDate != null) filters.put("startDate", startDate);
        if (endDate != null) filters.put("endDate", endDate);
        return ApiResponse.ok(ledgerService.getLedgerData(type, filters));
    }

    @GetMapping("/export/{type}")
    public ResponseEntity<byte[]> exportLedger(@PathVariable String type,
            @RequestParam(required = false) String templateId) throws Exception {
        requireLedgerPermission();
        List<Map<String, Object>> data = ledgerService.getLedgerData(type, new HashMap<>());
        List<String> columns = Arrays.asList("name", "phone", "address", "status", "created_at");
        String sheetName = type + "台账";

        byte[] excel = exportService.exportLedger(sheetName, columns, data);

        String fileName = URLEncoder.encode(sheetName + ".xlsx", StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + fileName)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(excel);
    }

    private void requireLedgerPermission() {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.MENU_BIZ_LEDGER);
    }
}
