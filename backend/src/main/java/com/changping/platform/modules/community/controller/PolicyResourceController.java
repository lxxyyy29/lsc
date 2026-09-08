package com.changping.platform.modules.community.controller;

import com.changping.platform.common.response.ApiResponse;
import com.changping.platform.modules.auth.security.PermissionCodes;
import com.changping.platform.modules.auth.security.PermissionGuard;
import com.changping.platform.modules.auth.service.AuthService;
import com.changping.platform.modules.auth.service.CurrentUserService;
import com.changping.platform.modules.community.entity.PolicyResourceEntity;
import com.changping.platform.modules.community.service.PolicyResourceService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 政策资源库控制器（管理端）：提供政策 CRUD + 政策找人/定向推送。
 * 居民端政策浏览走独立入口 /resident/policy-resources，本控制器仅限 WEB 客户端。
 */
@RestController
@RequestMapping("/community/policy-resources")
public class PolicyResourceController {

    private final PolicyResourceService policyResourceService;
    private final PermissionGuard permissionGuard;
    private final CurrentUserService currentUserService;

    public PolicyResourceController(PolicyResourceService policyResourceService,
            PermissionGuard permissionGuard,
            CurrentUserService currentUserService) {
        this.policyResourceService = policyResourceService;
        this.permissionGuard = permissionGuard;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public ApiResponse<List<PolicyResourceEntity>> list(@RequestParam(required = false) String status) {
        requireWebPolicyManage();
        if ("ACTIVE".equals(status)) {
            return ApiResponse.ok(policyResourceService.listActive());
        }
        return ApiResponse.ok(policyResourceService.listAll());
    }

    @GetMapping("/{id}")
    public ApiResponse<PolicyResourceEntity> get(@PathVariable Long id) {
        requireWebPolicyManage();
        return ApiResponse.ok(policyResourceService.getById(id));
    }

    @PostMapping
    public ApiResponse<PolicyResourceEntity> create(@RequestBody PolicyResourceEntity entity) {
        requireWebPolicyManage();
        return ApiResponse.ok(policyResourceService.create(entity));
    }

    @PutMapping("/{id}")
    public ApiResponse<PolicyResourceEntity> update(@PathVariable Long id, @RequestBody PolicyResourceEntity entity) {
        requireWebPolicyManage();
        entity.setId(id);
        policyResourceService.updateById(entity);
        return ApiResponse.ok(policyResourceService.getById(id));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        requireWebPolicyManage();
        policyResourceService.removeById(id);
        return ApiResponse.ok(null);
    }

    /**
     * 政策找人：返回该政策匹配到的人群
     */
    @GetMapping("/{id}/matching-people")
    public ApiResponse<List<Map<String, Object>>> findMatchingPeople(@PathVariable Long id) {
        requireWebPolicyManage();
        return ApiResponse.ok(policyResourceService.findMatchingPeople(id));
    }

    /**
     * 定向推送：将政策以站内通知推送给匹配人群中的居民账号（影响面大，仅限管理端）
     */
    @PostMapping("/{id}/push")
    public ApiResponse<Map<String, Object>> push(@PathVariable Long id) {
        requireWebPolicyManage();
        return ApiResponse.ok(policyResourceService.pushToResidents(id));
    }

    /** 政策资源库为管理端功能：仅 WEB 客户端 + 事件创建权限（沿用原权限码） */
    private void requireWebPolicyManage() {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.API_EVENT_CREATE);
    }
}
