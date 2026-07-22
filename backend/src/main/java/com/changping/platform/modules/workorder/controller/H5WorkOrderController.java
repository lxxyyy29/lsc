package com.changping.platform.modules.workorder.controller;

import com.changping.platform.common.response.ApiResponse;
import com.changping.platform.modules.auth.security.PermissionCodes;
import com.changping.platform.modules.auth.security.PermissionGuard;
import com.changping.platform.modules.auth.service.AuthService;
import com.changping.platform.modules.auth.service.CurrentUserService;
import com.changping.platform.modules.workorder.dto.HandleWorkOrderRequest;
import com.changping.platform.modules.workorder.entity.WorkOrderEntity;
import com.changping.platform.modules.workorder.service.WorkOrderService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author tangxinglin
 * @Description //H5移动端工单控制器，提供工作台概览、工单列表、工单详情及工单处理接口
 * @Date 2026/04/18 09:00
 */
@RestController
@RequestMapping("/h5")
public class H5WorkOrderController {

    private final WorkOrderService workOrderService;
    private final PermissionGuard permissionGuard;
    private final CurrentUserService currentUserService;

    /**
     * @Author tangxinglin
     * @Description //构造函数，注入工单服务、权限校验及当前用户服务
     * @Date 2026/04/18 09:00
     * @Param [workOrderService 工单服务, permissionGuard 权限校验, currentUserService 当前用户服务]
     * @return
     */
    public H5WorkOrderController(
            WorkOrderService workOrderService,
            PermissionGuard permissionGuard,
            CurrentUserService currentUserService) {
        this.workOrderService = workOrderService;
        this.permissionGuard = permissionGuard;
        this.currentUserService = currentUserService;
    }

    /**
     * @Author tangxinglin
     * @Description //获取H5工作台概览，包含各状态工单计数汇总
     * @Date 2026/04/18 09:00
     * @Param []
     * @return ApiResponse<WorkOrderService.H5WorkbenchSummary> 工作台汇总信息
     */
    @GetMapping("/workbench")
    public ApiResponse<WorkOrderService.H5WorkbenchSummary> getWorkbench() {
        currentUserService.requireClientType(AuthService.ClientType.H5);
        permissionGuard.require(PermissionCodes.API_H5_WORKBENCH_VIEW);
        return ApiResponse.ok(workOrderService.getH5Workbench());
    }

    /**
     * @Author tangxinglin
     * @Description //查询当前H5用户参与的工单列表
     * @Date 2026/04/18 09:00
     * @Param []
     * @return ApiResponse<List<WorkOrderService.H5WorkOrderListItem>> 工单列表
     */
    @GetMapping("/work-orders")
    public ApiResponse<List<WorkOrderService.H5WorkOrderListItem>> queryWorkOrders() {
        currentUserService.requireClientType(AuthService.ClientType.H5);
        permissionGuard.require(PermissionCodes.API_H5_WORKORDER_LIST);
        return ApiResponse.ok(workOrderService.queryH5WorkOrders());
    }

    /**
     * @Author tangxinglin
     * @Description //获取指定工单的H5详情，包含流程节点和操作记录
     * @Date 2026/04/18 09:00
     * @Param [id 工单ID]
     * @return ApiResponse<WorkOrderService.H5WorkOrderDetail> 工单详情
     */
    @GetMapping("/work-orders/{id}")
    public ApiResponse<WorkOrderService.H5WorkOrderDetail> getWorkOrderDetail(@PathVariable Long id) {
        currentUserService.requireClientType(AuthService.ClientType.H5);
        permissionGuard.require(PermissionCodes.API_H5_WORKORDER_DETAIL);
        return ApiResponse.ok(workOrderService.getH5WorkOrderDetail(id));
    }

    /**
     * @Author tangxinglin
     * @Description //处理工单节点，提交处理结果、备注及附件，支持通过或拒绝
     * @Date 2026/04/18 09:00
     * @Param [id 工单ID, request 处理请求对象，包含处理结果、备注、附件等]
     * @return ApiResponse<WorkOrderEntity> 更新后的工单实体
     */
    @PostMapping("/work-orders/{id}/handle")
    public ApiResponse<WorkOrderEntity> handle(
            @PathVariable Long id,
            @Valid @RequestBody HandleWorkOrderRequest request) {
        currentUserService.requireClientType(AuthService.ClientType.H5);
        permissionGuard.require(PermissionCodes.API_H5_WORKORDER_HANDLE);
        return ApiResponse.ok(workOrderService.handle(
                id,
                new WorkOrderService.HandleRequest(
                        request.result(),
                        request.remark(),
                        request.attachments() == null
                                ? List.of()
                                : request.attachments().stream()
                                        .map(item -> new WorkOrderService.HandleAttachment(item.fileName(), item.fileUrl(), item.fileType(), item.mimeType()))
                                        .toList(),
                        request.subjectType(),
                        request.subjectId())));
    }

}
