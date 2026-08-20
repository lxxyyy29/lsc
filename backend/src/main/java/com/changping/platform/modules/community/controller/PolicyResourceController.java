package com.changping.platform.modules.community.controller;

import com.changping.platform.common.response.ApiResponse;
import com.changping.platform.modules.auth.security.PermissionCodes;
import com.changping.platform.modules.auth.security.PermissionGuard;
import com.changping.platform.modules.community.entity.PolicyResourceEntity;
import com.changping.platform.modules.community.service.PolicyResourceService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 政策资源库控制器，提供政策 CRUD + 政策找人
 */
@RestController
@RequestMapping("/community/policy-resources")
public class PolicyResourceController {

    private final PolicyResourceService policyResourceService;
    private final PermissionGuard permissionGuard;

    public PolicyResourceController(PolicyResourceService policyResourceService, PermissionGuard permissionGuard) {
        this.policyResourceService = policyResourceService;
        this.permissionGuard = permissionGuard;
    }

    @GetMapping
    public ApiResponse<List<PolicyResourceEntity>> list(@RequestParam(required = false) String status) {
        permissionGuard.require(PermissionCodes.API_EVENT_CREATE);
        if ("ACTIVE".equals(status)) {
            return ApiResponse.ok(policyResourceService.listActive());
        }
        return ApiResponse.ok(policyResourceService.listAll());
    }

    @GetMapping("/{id}")
    public ApiResponse<PolicyResourceEntity> get(@PathVariable Long id) {
        permissionGuard.require(PermissionCodes.API_EVENT_CREATE);
        return ApiResponse.ok(policyResourceService.getById(id));
    }

    @PostMapping
    public ApiResponse<PolicyResourceEntity> create(@RequestBody PolicyResourceEntity entity) {
        permissionGuard.require(PermissionCodes.API_EVENT_CREATE);
        return ApiResponse.ok(policyResourceService.create(entity));
    }

    @PutMapping("/{id}")
    public ApiResponse<PolicyResourceEntity> update(@PathVariable Long id, @RequestBody PolicyResourceEntity entity) {
        permissionGuard.require(PermissionCodes.API_EVENT_CREATE);
        entity.setId(id);
        policyResourceService.updateById(entity);
        return ApiResponse.ok(policyResourceService.getById(id));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        permissionGuard.require(PermissionCodes.API_EVENT_CREATE);
        policyResourceService.removeById(id);
        return ApiResponse.ok(null);
    }

    /**
     * 政策找人：返回该政策匹配到的人群
     */
    @GetMapping("/{id}/matching-people")
    public ApiResponse<List<Map<String, Object>>> findMatchingPeople(@PathVariable Long id) {
        permissionGuard.require(PermissionCodes.API_EVENT_CREATE);
        return ApiResponse.ok(policyResourceService.findMatchingPeople(id));
    }

    /**
     * 定向推送：将政策以站内通知推送给匹配人群中的居民账号
     */
    @PostMapping("/{id}/push")
    public ApiResponse<Map<String, Object>> push(@PathVariable Long id) {
        permissionGuard.require(PermissionCodes.API_EVENT_CREATE);
        return ApiResponse.ok(policyResourceService.pushToResidents(id));
    }
}
