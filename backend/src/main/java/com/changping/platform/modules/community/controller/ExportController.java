package com.changping.platform.modules.community.controller;

import com.changping.platform.modules.auth.security.PermissionCodes;
import com.changping.platform.modules.auth.security.PermissionGuard;
import com.changping.platform.modules.auth.service.AuthService;
import com.changping.platform.modules.auth.service.CurrentUserService;
import com.changping.platform.modules.community.mapper.ExportMapper;
import com.changping.platform.modules.community.service.ExportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/community/export")
public class ExportController {

    private final ExportService exportService;
    private final ExportMapper exportMapper;
    private final CurrentUserService currentUserService;
    private final PermissionGuard permissionGuard;

    public ExportController(
            ExportService exportService,
            ExportMapper exportMapper,
            CurrentUserService currentUserService,
            PermissionGuard permissionGuard) {
        this.exportService = exportService;
        this.exportMapper = exportMapper;
        this.currentUserService = currentUserService;
        this.permissionGuard = permissionGuard;
    }

    @GetMapping("/events")
    public ResponseEntity<byte[]> exportEvents() throws Exception {
        requireExportPermission(PermissionCodes.API_EVENT_LIST);
        byte[] data = exportService.exportEventLedger();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=event_ledger.xlsx")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(data);
    }

    @GetMapping("/merchants")
    public ResponseEntity<byte[]> exportMerchants() throws Exception {
        requireExportPermission(PermissionCodes.MENU_BIZ_LEDGER);
        List<Map<String, Object>> data = exportMapper.getMerchantLedger();
        List<String> columns = List.of("merchant_name", "legal_person_name", "legal_person_phone", "remark", "created_at");
        byte[] excel = exportService.exportLedger("场所台账", columns, data);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=merchant_ledger.xlsx")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(excel);
    }

    @GetMapping("/population")
    public ResponseEntity<byte[]> exportPopulation() throws Exception {
        requireExportPermission(PermissionCodes.MENU_COMMUNITY_POPULATION);
        List<Map<String, Object>> data = exportMapper.getPopulationLedger();
        List<String> columns = List.of("name", "id_card", "phone", "household_type", "address", "grid_name", "tags", "created_at");
        byte[] excel = exportService.exportLedger("实有人口台账", columns, data);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=population_ledger.xlsx")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(excel);
    }

    @GetMapping("/buildings")
    public ResponseEntity<byte[]> exportBuildings() throws Exception {
        requireExportPermission(PermissionCodes.MENU_COMMUNITY_BUILDING);
        List<Map<String, Object>> data = exportMapper.getBuildingLedger();
        List<String> columns = List.of("building_no", "address", "landlord_name", "landlord_phone", "fire_risk_level", "is_group_rental", "grid_name");
        byte[] excel = exportService.exportLedger("房屋台账", columns, data);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=building_ledger.xlsx")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(excel);
    }

    @GetMapping("/patrols")
    public ResponseEntity<byte[]> exportPatrols() throws Exception {
        requireExportPermission(PermissionCodes.MENU_COMMUNITY_PATROL_RECORD);
        List<Map<String, Object>> data = exportMapper.getPatrolLedger();
        List<String> columns = List.of("grid_name", "patrol_type", "content", "status", "created_at", "longitude", "latitude");
        byte[] excel = exportService.exportLedger("巡查台账", columns, data);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=patrol_ledger.xlsx")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(excel);
    }

    @GetMapping(value = "/test", produces = MediaType.TEXT_PLAIN_VALUE)
    public String testExport() {
        requireExportPermission(PermissionCodes.API_EVENT_LIST);
        try {
            byte[] data = exportService.exportEventLedger();
            return "Export successful! Size: " + data.length + " bytes";
        } catch (Exception e) {
            return "Export failed: " + e.getMessage();
        }
    }

    private void requireExportPermission(String permissionCode) {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(permissionCode);
    }
}
