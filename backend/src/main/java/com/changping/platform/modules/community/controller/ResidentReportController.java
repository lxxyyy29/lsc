package com.changping.platform.modules.community.controller;

import com.changping.platform.common.response.ApiResponse;
import com.changping.platform.modules.community.entity.ResidentReportEntity;
import com.changping.platform.modules.community.service.ResidentReportService;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/**
 * 居民上报记录控制器：仅提供只读记录查询。
 * 居民上报已统一归口至事件闭环处理中心，提交时自动生成事件，
 * 处置与派单一律通过事件中心进行，本模块不再提供直接处置操作。
 */
@RestController
@RequestMapping("/community/resident-reports")
public class ResidentReportController {

    private final ResidentReportService service;

    public ResidentReportController(ResidentReportService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<List<ResidentReportEntity>> list(@RequestParam(required = false) String status) {
        return ApiResponse.ok(service.listByStatus(status));
    }

    @GetMapping("/{id}")
    public ApiResponse<ResidentReportEntity> getById(@PathVariable Long id) {
        return ApiResponse.ok(service.findById(id));
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
