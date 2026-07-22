package com.changping.platform.modules.community.controller;

import com.changping.platform.modules.community.service.ExportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
