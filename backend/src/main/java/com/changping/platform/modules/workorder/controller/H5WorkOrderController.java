package com.changping.platform.modules.workorder.controller;

import com.changping.platform.common.response.ApiResponse;
import com.changping.platform.modules.auth.security.PermissionCodes;
import com.changping.platform.modules.auth.security.PermissionGuard;
import com.changping.platform.modules.auth.service.AuthService;
import com.changping.platform.modules.auth.service.CurrentUserService;
import com.changping.platform.modules.workorder.dto.HandleWorkOrderRequest;
import com.changping.platform.modules.workorder.entity.WorkOrderEntity;
import com.changping.platform.modules.workorder.service.SmartDispatchService;
import com.changping.platform.modules.workorder.service.WorkOrderService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author lxy
 * @Description //H5移动端工单控制器，提供工作台概览、工单列表、工单详情及工单处理接口
 * @Date 2026/04/18 09:00
 */
@RestController
@RequestMapping("/h5")
public class H5WorkOrderController {

    private final WorkOrderService workOrderService;
    private final PermissionGuard permissionGuard;
    private final CurrentUserService currentUserService;
    private final SmartDispatchService smartDispatchService;

    /**
     * @Author lxy
     * @Description //构造函数，注入工单服务、权限校验及当前用户服务
     * @Date 2026/04/18 09:00
     * @Param [workOrderService 工单服务, permissionGuard 权限校验, currentUserService 当前用户服务]
     * @return
     */
    public H5WorkOrderController(
            WorkOrderService workOrderService,
            PermissionGuard permissionGuard,
            CurrentUserService currentUserService,
            SmartDispatchService smartDispatchService) {
        this.workOrderService = workOrderService;
        this.permissionGuard = permissionGuard;
        this.currentUserService = currentUserService;
        this.smartDispatchService = smartDispatchService;
    }

    /**
     * @Author lxy
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
     * @Author lxy
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
     * @Author lxy
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
     * @Author lxy
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

    /**
     * H5 组长工作台：查询当前用户名下的组长待办事件
     */
    @GetMapping("/leader/pending-events")
    public ApiResponse<List<Map<String, Object>>> getLeaderPendingEvents() {
        currentUserService.requireClientType(AuthService.ClientType.H5);
        permissionGuard.require(PermissionCodes.API_H5_LEADER_PENDING);
        Long userId = currentUserService.requireClientType(AuthService.ClientType.H5).id();
        return ApiResponse.ok(smartDispatchService.findLeaderPendingEvents(userId));
    }

    /**
     * H5 组长派单：查询事件派单信息（组长+下属网格员）
     */
    @GetMapping("/leader/events/{eventId}/dispatch-info")
    public ApiResponse<Map<String, Object>> getLeaderDispatchInfo(@PathVariable Long eventId) {
        currentUserService.requireClientType(AuthService.ClientType.H5);
        permissionGuard.require(PermissionCodes.API_H5_LEADER_DISPATCH);
        Long userId = currentUserService.requireClientType(AuthService.ClientType.H5).id();
        Map<String, Object> info = smartDispatchService.getLeaderDispatchInfo(eventId);
        // 验证当前用户是否为该网格的组长
        Object leaderObj = info.get("leader");
        if (leaderObj == null || !Boolean.TRUE.equals(info.get("leaderFound"))) {
            return ApiResponse.ok(info);
        }
        Map<String, Object> leader = (Map<String, Object>) leaderObj;
        Long leaderUserId = (Long) leader.get("userId");
        if (leaderUserId == null || !leaderUserId.equals(userId)) {
            throw new com.changping.platform.common.exception.BusinessException(
                    "NOT_LEADER_OF_THIS_GRID", "您不是该事件所属网格的组长，无权派单");
        }
        return ApiResponse.ok(info);
    }

    /**
     * H5 组长派单：将事件派发给下属网格员
     */
    @PostMapping("/leader/events/{eventId}/dispatch")
    public ApiResponse<WorkOrderEntity> leaderDispatch(
            @PathVariable Long eventId,
            @RequestBody Map<String, Object> body) {
        currentUserService.requireClientType(AuthService.ClientType.H5);
        permissionGuard.require(PermissionCodes.API_H5_LEADER_DISPATCH);
        Long userId = currentUserService.requireClientType(AuthService.ClientType.H5).id();

        // 验证组长身份
        Map<String, Object> info = smartDispatchService.getLeaderDispatchInfo(eventId);
        if (!Boolean.TRUE.equals(info.get("leaderFound"))) {
            throw new com.changping.platform.common.exception.BusinessException(
                    "NO_LEADER", "该事件未配置组长，无法执行组长派单");
        }
        Map<String, Object> leader = (Map<String, Object>) info.get("leader");
        Long leaderUserId = (Long) leader.get("userId");
        if (leaderUserId == null || !leaderUserId.equals(userId)) {
            throw new com.changping.platform.common.exception.BusinessException(
                    "NOT_LEADER_OF_THIS_GRID", "您不是该事件所属网格的组长，无权派单");
        }

        // 派发
        Long assigneeUserId = body.get("assigneeUserId") != null
                ? Long.valueOf(body.get("assigneeUserId").toString())
                : null;
        String remark = body.getOrDefault("remark", "") != null
                ? body.getOrDefault("remark", "").toString()
                : "";

        WorkOrderService.DispatchRequest req = new WorkOrderService.DispatchRequest(assigneeUserId, remark);
        return ApiResponse.ok(workOrderService.dispatch(eventId, req));
    }

}
