package com.changping.platform.modules.community.controller;

import com.changping.platform.modules.community.service.ExportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/community/export")
public class ExportController {

    private final ExportService exportService;

    public ExportController(ExportService exportService) {
        this.exportService = exportService;
    }

    @GetMapping("/events")
    public ResponseEntity<byte[]> exportEvents() throws Exception {
        byte[] data = exportService.exportEventLedger();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=event_ledger.xlsx")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(data);
    }

    @GetMapping("/population")
    public ResponseEntity<byte[]> exportPopulation() throws Exception {
        List<Map<String, Object>> data = exportMapper.getPopulationLedger();
        List<String> columns = List.of("name", "id_card", "phone", "household_type", "address", "grid_name", "tags", "created_at");
        String[] headers = {"姓名", "身份证", "电话", "户籍类型", "地址", "网格", "标签", "创建时间"};
        byte[] excel = exportService.exportLedger("实有人口台账", columns, data);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=population_ledger.xlsx")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(excel);
    }

    @GetMapping("/buildings")
    public ResponseEntity<byte[]> exportBuildings() throws Exception {
        List<Map<String, Object>> data = exportMapper.getBuildingLedger();
        List<String> columns = List.of("building_no", "address", "landlord_name", "landlord_phone", "fire_risk_level", "is_group_rental", "grid_name");
        String[] headers = {"楼栋编号", "地址", "房东", "房东电话", "消防风险", "群租房", "网格"};
        byte[] excel = exportService.exportLedger("房屋台账", columns, data);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=building_ledger.xlsx")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(excel);
    }

    @GetMapping("/patrols")
    public ResponseEntity<byte[]> exportPatrols() throws Exception {
        List<Map<String, Object>> data = exportMapper.getPatrolLedger();
        List<String> columns = List.of("grid_name", "patrol_type", "content", "status", "created_at", "longitude", "latitude");
        String[] headers = {"网格", "巡查类型", "内容", "状态", "时间", "经度", "纬度"};
        byte[] excel = exportService.exportLedger("巡查台账", columns, data);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=patrol_ledger.xlsx")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(excel);
    }

    @GetMapping(value = "/test", produces = MediaType.TEXT_PLAIN_VALUE)
    public String testExport() {
        try {
            byte[] data = exportService.exportEventLedger();
            return "Export successful! Size: " + data.length + " bytes";
        } catch (Exception e) {
            return "Export failed: " + e.getMessage();
        }
    }
}
