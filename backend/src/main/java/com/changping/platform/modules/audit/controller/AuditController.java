package com.changping.platform.modules.audit.controller;

import com.changping.platform.common.response.ApiResponse;
import com.changping.platform.modules.auth.security.PermissionCodes;
import com.changping.platform.modules.auth.security.PermissionGuard;
import com.changping.platform.modules.process.entity.ProcessInstanceEntity;
import com.changping.platform.modules.process.service.ProcessInstanceService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author tangxinglin
 * @Description //审核控制器，提供针对事件的审核发起和审核详情查询接口，需要对应 API 权限码
 * @Date 2026/04/18 10:10
 */
@RestController
@RequestMapping("/audits")
public class AuditController {

    private final ProcessInstanceService processInstanceService;
    private final PermissionGuard permissionGuard;

    /**
     * @Author tangxinglin
     * @Description //构造函数注入流程实例服务和权限守卫
     * @Date 2026/04/18 10:10
     * @Param [processInstanceService 流程实例服务, permissionGuard 权限守卫]
     * @return void
     */
    public AuditController(ProcessInstanceService processInstanceService, PermissionGuard permissionGuard) {
        this.processInstanceService = processInstanceService;
        this.permissionGuard = permissionGuard;
    }

    /**
     * @Author tangxinglin
     * @Description //针对指定事件发起审核流程，可选传入流程模板ID，需要 API_AUDIT_START 权限
     * @Date 2026/04/18 10:10
     * @Param [eventId 事件ID, request 发起审核请求（可选，包含流程模板ID和是否强制重新发起标志）]
     * @return ApiResponse<ProcessInstanceEntity> 创建的流程实例信息
     */
    @PostMapping("/{eventId}/start")
    public ApiResponse<ProcessInstanceEntity> startAudit(
            @PathVariable Long eventId,
            @RequestBody(required = false) ProcessInstanceService.StartAuditRequest request) {
        permissionGuard.require(PermissionCodes.API_AUDIT_START);
        ProcessInstanceService.StartAuditRequest safeRequest = request == null
                ? new ProcessInstanceService.StartAuditRequest(null, false)
                : request;
        return ApiResponse.ok(processInstanceService.startAudit(eventId, safeRequest));
    }

    /**
     * @Author tangxinglin
     * @Description //查询指定事件的审核流程详情，需要 API_AUDIT_DETAIL 权限
     * @Date 2026/04/18 10:10
     * @Param [eventId 事件ID]
     * @return ApiResponse<ProcessInstanceEntity> 流程实例详情
     */
    @GetMapping("/{eventId}")
    public ApiResponse<ProcessInstanceEntity> getAuditDetail(@PathVariable Long eventId) {
        permissionGuard.require(PermissionCodes.API_AUDIT_DETAIL);
        return ApiResponse.ok(processInstanceService.getAuditDetail(eventId));
    }

}
