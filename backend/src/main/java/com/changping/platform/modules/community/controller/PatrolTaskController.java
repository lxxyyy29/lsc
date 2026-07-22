package com.changping.platform.modules.community.controller;

import com.changping.platform.common.response.ApiResponse;
import com.changping.platform.modules.auth.model.AuthenticatedUser;
import com.changping.platform.modules.auth.service.AuthService;
import com.changping.platform.modules.auth.service.CurrentUserService;
import com.changping.platform.modules.community.entity.PatrolTaskEntity;
import com.changping.platform.modules.community.service.PatrolTaskService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/community/patrol-tasks")
public class PatrolTaskController {

    private final PatrolTaskService service;
    private final CurrentUserService currentUserService;

    public PatrolTaskController(PatrolTaskService service, CurrentUserService currentUserService) {
        this.service = service;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public ApiResponse<List<PatrolTaskEntity>> list() {
        AuthenticatedUser user = currentUserService.requireClientType(AuthService.ClientType.H5);
        return ApiResponse.ok(service.listByUser(user.id()));
    }

    @PostMapping("/generate")
    public ApiResponse<Integer> generateWeekly() {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        return ApiResponse.ok(service.generateWeeklyTasks());
    }

    @PostMapping("/mark-overdue")
    public ApiResponse<Integer> markOverdue() {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        return ApiResponse.ok(service.markOverdueTasks());
    }

    @PostMapping("/{id}/complete")
    public ApiResponse<Boolean> complete(@PathVariable Long id) {
        currentUserService.requireClientType(AuthService.ClientType.H5);
        return ApiResponse.ok(service.completeTask(id));
    }
}
