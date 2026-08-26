package com.changping.platform.modules.workorder.controller;

import com.changping.platform.common.response.ApiResponse;
import com.changping.platform.modules.auth.security.PermissionCodes;
import com.changping.platform.modules.auth.security.PermissionGuard;
import com.changping.platform.modules.auth.service.AuthService;
import com.changping.platform.modules.auth.service.CurrentUserService;
import com.changping.platform.modules.workorder.entity.WorkOrderEntity;
import com.changping.platform.modules.workorder.service.SmartDispatchService;
import com.changping.platform.modules.workorder.service.WorkOrderService;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author lxy
 * @Description //Web端工单控制器，提供工单查询、详情、派发、处理及批量删除接口
 * @Date 2026/04/18 09:05
 */
@RestController
@RequestMapping("/work-orders")
public class WorkOrderController {

    private final WorkOrderService workOrderService;
    private final PermissionGuard permissionGuard;
    private final CurrentUserService currentUserService;
    private final SmartDispatchService smartDispatchService;

    /**
     * @Author lxy
     * @Description //构造函数，注入工单服务、权限校验及当前用户服务
     * @Date 2026/04/18 09:05
     * @Param [workOrderService 工单服务, permissionGuard 权限校验, currentUserService 当前用户服务]
     * @return
     */
    public WorkOrderController(
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
     * @Description //分页查询Web端工单列表，支持按状态、经办人、区域过滤
     * @Date 2026/04/18 09:05
     * @Param [page 页码，默认1, pageSize 每页条数，默认10, status 工单状态筛选, assignee 经办人姓名模糊筛选, areaId 区域ID筛选]
     * @return ApiResponse<WorkOrderService.PagedWorkOrders> 分页工单列表
     */
    @GetMapping
    public ApiResponse<WorkOrderService.PagedWorkOrders> queryWorkOrders(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String assignee,
            @RequestParam(required = false) Long areaId) {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.API_WORKORDER_DISPATCH);
        return ApiResponse.ok(workOrderService.queryWebWorkOrdersPaged(page, pageSize, status, assignee, areaId));
    }

    /**
     * @Author lxy
     * @Description //导出Web端全量工单列表（不分页）
     * @Date 2026/04/18 09:05
     * @Param []
     * @return ApiResponse<List<WorkOrderService.WebWorkOrderSummary>> 工单汇总列表
     */
    @GetMapping("/export")
    public ApiResponse<List<WorkOrderService.WebWorkOrderSummary>> exportWorkOrders() {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.API_WORKORDER_DISPATCH);
        return ApiResponse.ok(workOrderService.queryWebWorkOrders());
    }

    /**
     * @Author lxy
     * @Description //获取指定工单的Web端详情，包含流程流转记录
     * @Date 2026/04/18 09:05
     * @Param [id 工单ID]
     * @return ApiResponse<WorkOrderService.WebWorkOrderDetail> 工单详情
     */
    @GetMapping("/{id}")
    public ApiResponse<WorkOrderService.WebWorkOrderDetail> getWorkOrderDetail(@PathVariable Long id) {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.API_WORKORDER_DISPATCH);
        return ApiResponse.ok(workOrderService.getWebWorkOrderDetail(id));
    }

    /**
     * @Author lxy
     * @Description //Web端处理工单节点，提交处理结果
     * @Date 2026/04/18 09:05
     * @Param [id 工单ID, request 处理请求对象]
     * @return ApiResponse<WorkOrderEntity> 更新后的工单实体
     */
    @PostMapping("/{id}/handle")
    public ApiResponse<WorkOrderEntity> handle(
            @PathVariable Long id,
            @RequestBody WorkOrderService.HandleRequest request) {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.API_WORKORDER_HANDLE);
        return ApiResponse.ok(workOrderService.handle(id, request));
    }

    /**
     * @Author lxy
     * @Description //派发工单，将指定事件按流程模板生成工单并分配处理人
     * @Date 2026/04/18 09:05
     * @Param [eventId 事件ID, request 派发请求，包含流程模板ID和备注]
     * @return ApiResponse<WorkOrderEntity> 新创建的工单实体
     */
    @PostMapping("/{eventId}/dispatch")
    public ApiResponse<WorkOrderEntity> dispatch(
            @PathVariable Long eventId,
            @RequestBody WorkOrderService.DispatchRequest request) {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.API_WORKORDER_DISPATCH);
        return ApiResponse.ok(workOrderService.dispatch(eventId, request));
    }

    /**
     * 智能派单建议：按派单规则推荐受理角色与人员（供派单弹窗展示）
     */
    @GetMapping("/dispatch-suggestion")
    public ApiResponse<SmartDispatchService.Suggestion> dispatchSuggestion(@RequestParam Long eventId) {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.API_WORKORDER_DISPATCH);
        return ApiResponse.ok(smartDispatchService.suggest(eventId));
    }

    /**
     * 一键智能派单：按规则自动选择推荐人派发工单
     */
    @PostMapping("/{eventId}/smart-dispatch")
    public ApiResponse<WorkOrderEntity> smartDispatch(
            @PathVariable Long eventId,
            @RequestBody(required = false) Map<String, String> body) {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.API_WORKORDER_DISPATCH);
        String remark = body != null ? body.getOrDefault("remark", "") : "";
        return ApiResponse.ok(smartDispatchService.smartDispatch(eventId, remark));
    }

    /**
     * 获取工单附件列表
     */
    @GetMapping("/{id}/attachments")
    public ApiResponse<List<Map<String, Object>>> getAttachments(@PathVariable Long id) {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.API_WORKORDER_DISPATCH);
        List<Map<String, Object>> attachments = workOrderService.getAttachments(id);
        return ApiResponse.ok(attachments);
    }

    /**
     * 确认关闭工单（Web端）— 工单 COMPLETED，事件 CLOSED
     */
    @PostMapping("/{id}/confirm-close")
    public ApiResponse<WorkOrderEntity> confirmClose(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.API_WORKORDER_CONFIRM_CLOSE);
        String remark = body != null ? body.getOrDefault("remark", "") : "";
        return ApiResponse.ok(workOrderService.confirmClose(id, remark));
    }

    /**
     * 驳回关闭（Web端）— 工单退回 PROCESSING
     */
    @PostMapping("/{id}/reject-close")
    public ApiResponse<WorkOrderEntity> rejectClose(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.API_WORKORDER_CONFIRM_CLOSE);
        String remark = body != null ? body.getOrDefault("remark", "") : "";
        return ApiResponse.ok(workOrderService.rejectClose(id, remark));
    }

    /**
     * 设置工单展示隐藏状态：隐藏后仅工单中心可见，大屏等面板不再统计展示
     */
    @PutMapping("/{id}/hidden")
    public ApiResponse<Boolean> setHidden(@PathVariable Long id, @RequestBody Map<String, Boolean> body) {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.API_WORKORDER_DISPATCH);
        boolean hidden = Boolean.TRUE.equals(body.get("hidden"));
        return ApiResponse.ok(workOrderService.setWorkOrderHidden(id, hidden));
    }

    /**
     * @Author lxy
     * @Description //批量删除工单，按ID列表逐个删除工单及其关联的流程数据
     * @Date 2026/04/18 09:05
     * @Param [ids 要删除的工单ID列表]
     * @return ApiResponse<Void> 无返回数据
     */
    @PostMapping("/batch-delete")
    public ApiResponse<Void> deleteWorkOrders(@RequestBody List<Long> ids) {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.API_WORKORDER_DISPATCH);
        for (Long id : ids) {
            workOrderService.deleteWorkOrder(id);
        }
        return ApiResponse.ok(null);
    }

    /**
     * Web端组长待办事件列表（用于事件列表中「组长审核」状态的派发操作）
     */
    @GetMapping("/leader/pending-events")
    public ApiResponse<List<Map<String, Object>>> getLeaderPendingEvents() {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.API_LEADER_PENDING);
        Long userId = currentUserService.requireClientType(AuthService.ClientType.WEB).id();
        return ApiResponse.ok(smartDispatchService.findLeaderPendingEvents(userId));
    }

    /**
     * Web端组长派单：查询事件派单信息（Web端管理员可查看任意网格的派单信息）
     */
    @GetMapping("/leader/events/{eventId}/dispatch-info")
    public ApiResponse<Map<String, Object>> getLeaderDispatchInfo(@PathVariable Long eventId) {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.API_LEADER_DISPATCH);
        Map<String, Object> info = smartDispatchService.getLeaderDispatchInfo(eventId);
        return ApiResponse.ok(info);
    }

    /**
     * Web端组长派单：将事件派发给下属网格员
     */
    @PostMapping("/leader/events/{eventId}/dispatch")
    public ApiResponse<WorkOrderEntity> leaderDispatch(
            @PathVariable Long eventId,
            @RequestBody Map<String, Object> body) {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.API_LEADER_DISPATCH);

        Map<String, Object> info = smartDispatchService.getLeaderDispatchInfo(eventId);
        if (!Boolean.TRUE.equals(info.get("leaderFound"))) {
            throw new com.changping.platform.common.exception.BusinessException(
                    "NO_LEADER", "该事件未配置组长，无法执行组长派单");
        }

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
