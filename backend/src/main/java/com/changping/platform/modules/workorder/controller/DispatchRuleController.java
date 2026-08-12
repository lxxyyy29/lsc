package com.changping.platform.modules.workorder.controller;

import com.changping.platform.common.response.ApiResponse;
import com.changping.platform.modules.auth.security.PermissionCodes;
import com.changping.platform.modules.auth.security.PermissionGuard;
import com.changping.platform.modules.auth.service.AuthService;
import com.changping.platform.modules.auth.service.CurrentUserService;
import com.changping.platform.modules.workorder.service.SmartDispatchService;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 智能派单规则管理（Web 端）：维护「事件类型 → 受理角色」映射，
 * 供派单时自动路由。接口权限复用工单派发权限。
 */
@RestController
@RequestMapping("/dispatch-rules")
public class DispatchRuleController {

    private final SmartDispatchService smartDispatchService;
    private final PermissionGuard permissionGuard;
    private final CurrentUserService currentUserService;

    public DispatchRuleController(
            SmartDispatchService smartDispatchService,
            PermissionGuard permissionGuard,
            CurrentUserService currentUserService) {
        this.smartDispatchService = smartDispatchService;
        this.permissionGuard = permissionGuard;
        this.currentUserService = currentUserService;
    }

    /** 规则列表 */
    @GetMapping
    public ApiResponse<List<Map<String, Object>>> listRules() {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.API_WORKORDER_DISPATCH);
        return ApiResponse.ok(smartDispatchService.listRules());
    }

    /** 新增规则 */
    @PostMapping
    public ApiResponse<Void> createRule(@RequestBody Map<String, Object> body) {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.API_WORKORDER_DISPATCH);
        smartDispatchService.createRule(
                (String) body.get("eventType"),
                (String) body.get("targetRoleCode"),
                body.get("priority") == null ? null : Integer.parseInt(body.get("priority").toString()),
                body.get("enabled") == null ? null : Integer.parseInt(body.get("enabled").toString()),
                (String) body.get("remark"));
        return ApiResponse.ok(null);
    }

    /** 更新规则 */
    @PutMapping("/{id}")
    public ApiResponse<Void> updateRule(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.API_WORKORDER_DISPATCH);
        smartDispatchService.updateRule(
                id,
                (String) body.get("eventType"),
                (String) body.get("targetRoleCode"),
                body.get("priority") == null ? null : Integer.parseInt(body.get("priority").toString()),
                body.get("enabled") == null ? null : Integer.parseInt(body.get("enabled").toString()),
                (String) body.get("remark"));
        return ApiResponse.ok(null);
    }

    /** 删除规则 */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteRule(@PathVariable Long id) {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.API_WORKORDER_DISPATCH);
        smartDispatchService.deleteRule(id);
        return ApiResponse.ok(null);
    }
}
