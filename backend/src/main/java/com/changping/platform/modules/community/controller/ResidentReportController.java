package com.changping.platform.modules.community.controller;

import com.changping.platform.common.response.ApiResponse;
import com.changping.platform.modules.community.entity.ResidentReportEntity;
import com.changping.platform.modules.community.service.ResidentReportService;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/community/resident-reports")
public class ResidentReportController {

    private final ResidentReportService service;
    public ResidentReportController(ResidentReportService service) { this.service = service; }

    @GetMapping
    public ApiResponse<List<ResidentReportEntity>> list() {
        return ApiResponse.ok(service.listAll());
    }

    @GetMapping("/code/{queryCode}")
    public ApiResponse<ResidentReportEntity> findByCode(@PathVariable String queryCode) {
        return ApiResponse.ok(service.findByCode(queryCode));
    }

    @PostMapping
    public ApiResponse<Boolean> create(@RequestBody ResidentReportEntity entity) {
        return ApiResponse.ok(service.create(entity));
    }
}
