package com.changping.platform.modules.community.controller;

import com.changping.platform.common.response.ApiResponse;
import com.changping.platform.modules.auth.model.AuthenticatedUser;
import com.changping.platform.modules.auth.service.AuthService;
import com.changping.platform.modules.auth.service.CurrentUserService;
import com.changping.platform.modules.community.entity.PatrolRecordEntity;
import com.changping.platform.modules.community.mapper.PatrolRecordMapper;
import com.changping.platform.modules.community.service.PatrolRecordService;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/community/patrol-records")
public class PatrolRecordController {

    private final PatrolRecordService service;
    private final CurrentUserService currentUserService;

    public PatrolRecordController(PatrolRecordService service, CurrentUserService currentUserService) {
        this.service = service;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public ApiResponse<List<PatrolRecordEntity>> list() {
        AuthenticatedUser user = currentUserService.requireClientType(AuthService.ClientType.H5);
        return ApiResponse.ok(service.listByUser(user.id()));
    }

    @PostMapping
    public ApiResponse<Boolean> create(@RequestBody PatrolRecordEntity entity) {
        AuthenticatedUser user = currentUserService.requireClientType(AuthService.ClientType.H5);
        entity.setUserId(user.id());
        if (entity.getPatrolType() == null) entity.setPatrolType("NORMAL");
        if (entity.getStatus() == null) entity.setStatus("NORMAL");
        return ApiResponse.ok(service.create(entity));
    }
}
