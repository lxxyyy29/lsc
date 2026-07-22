package com.changping.platform.modules.process.controller;

import com.changping.platform.common.response.ApiResponse;
import com.changping.platform.modules.auth.security.PermissionCodes;
import com.changping.platform.modules.auth.security.PermissionGuard;
import com.changping.platform.modules.auth.service.AuthService;
import com.changping.platform.modules.auth.service.CurrentUserService;
import com.changping.platform.modules.process.entity.ProcessInstanceEntity;
import com.changping.platform.modules.process.service.ProcessInstanceService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author tangxinglin
 * @Description //流程实例控制器，提供审核流程实例的审批和驳回接口，仅限Web端用户操作
 * @Date 2026/04/18 10:00
 */
@RestController
@RequestMapping("/processes/instances")
public class ProcessInstanceController {

    private final ProcessInstanceService processInstanceService;
    private final PermissionGuard permissionGuard;
    private final CurrentUserService currentUserService;

    /**
     * @Author tangxinglin
     * @Description //构造器，注入流程实例服务、权限守卫和当前用户服务
     * @Date 2026/04/18 10:00
     * @Param [processInstanceService 流程实例服务, permissionGuard 权限守卫, currentUserService 当前用户服务]
     * @return void
     */
    public ProcessInstanceController(
            ProcessInstanceService processInstanceService,
            PermissionGuard permissionGuard,
            CurrentUserService currentUserService) {
        this.processInstanceService = processInstanceService;
        this.permissionGuard = permissionGuard;
        this.currentUserService = currentUserService;
    }

    /**
     * @Author tangxinglin
     * @Description //审批通过指定流程实例节点接口，仅限Web端用户操作
     * @Date 2026/04/18 10:00
     * @Param [id 流程实例主键ID, request 审批决策请求对象，包含节点ID和审批意见]
     * @return ApiResponse<ProcessInstanceEntity> 更新后的流程实例
     */
    @PostMapping("/{id}/approve")
    public ApiResponse<ProcessInstanceEntity> approve(
            @PathVariable Long id,
            @RequestBody ProcessInstanceService.AuditDecisionRequest request) {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.API_PROCESS_INSTANCE_APPROVE);
        return ApiResponse.ok(processInstanceService.approve(id, request));
    }

    /**
     * @Author tangxinglin
     * @Description //驳回指定流程实例节点接口，仅限Web端用户操作
     * @Date 2026/04/18 10:00
     * @Param [id 流程实例主键ID, request 审批决策请求对象，包含节点ID和驳回意见]
     * @return ApiResponse<ProcessInstanceEntity> 更新后的流程实例
     */
    @PostMapping("/{id}/reject")
    public ApiResponse<ProcessInstanceEntity> reject(
            @PathVariable Long id,
            @RequestBody ProcessInstanceService.AuditDecisionRequest request) {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.API_PROCESS_INSTANCE_REJECT);
        return ApiResponse.ok(processInstanceService.reject(id, request));
    }
}
