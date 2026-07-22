package com.changping.platform.modules.community.controller;

import com.changping.platform.common.response.ApiResponse;
import com.changping.platform.modules.auth.model.AuthenticatedUser;
import com.changping.platform.modules.auth.service.AuthService;
import com.changping.platform.modules.auth.service.CurrentUserService;
import com.changping.platform.modules.community.entity.ResidentReportEntity;
import com.changping.platform.modules.community.service.ResidentReportService;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/community/resident-reports")
public class ResidentReportController {

    private final ResidentReportService service;
    private final CurrentUserService currentUserService;

    public ResidentReportController(ResidentReportService service, CurrentUserService currentUserService) {
        this.service = service;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public ApiResponse<List<ResidentReportEntity>> list() {
        return ApiResponse.ok(service.listAll());
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

    @PutMapping("/{id}/handle")
    public ApiResponse<Boolean> handle(@PathVariable Long id, @RequestBody Map<String, String> body) {
        AuthenticatedUser user = currentUserService.requireClientType(AuthService.ClientType.WEB);
        String handleResult = body.getOrDefault("handleResult", "已处理");
        return ApiResponse.ok(service.handleReport(id, user.id(), handleResult));
    }
}
