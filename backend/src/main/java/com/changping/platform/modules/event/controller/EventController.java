package com.changping.platform.modules.event.controller;

import com.changping.platform.common.response.ApiResponse;
import com.changping.platform.common.response.PagedResult;
import com.changping.platform.modules.auth.model.AuthenticatedUser;
import com.changping.platform.modules.auth.security.PermissionCodes;
import com.changping.platform.modules.auth.security.PermissionGuard;
import com.changping.platform.modules.auth.service.AuthService;
import com.changping.platform.modules.auth.service.CurrentUserService;
import com.changping.platform.modules.event.dto.CreateEventRequest;
import com.changping.platform.modules.event.dto.IgnoreEventRequest;
import com.changping.platform.modules.event.service.EventIgnoreService;
import com.changping.platform.modules.event.service.EventService;
import com.changping.platform.modules.event.vo.EventDetailVo;
import com.changping.platform.modules.event.vo.EventIgnoreRecordVo;
import com.changping.platform.modules.workorder.entity.WorkOrderEntity;
import com.changping.platform.modules.workorder.service.WorkOrderService;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @Author tangxinglin
 * @Description //事件控制器，提供事件的创建、查询、派单、忽略（误报）及批量删除接口
 * @Date 2026/04/18 10:00
 */
@Validated
@RestController
@RequestMapping("/events")
public class EventController {

    private final EventService eventService;
    private final EventIgnoreService eventIgnoreService;
    private final WorkOrderService workOrderService;
    private final PermissionGuard permissionGuard;
    private final CurrentUserService currentUserService;

    /**
     * @Author tangxinglin
     * @Description //构造器，注入事件服务、忽略服务、工单服务、权限守卫和当前用户服务
     * @Date 2026/04/18 10:00
     * @Param [eventService 事件服务, eventIgnoreService 事件忽略服务, workOrderService 工单服务, permissionGuard 权限守卫, currentUserService 当前用户服务]
     * @return void
     */
    public EventController(
            EventService eventService,
            EventIgnoreService eventIgnoreService,
            WorkOrderService workOrderService,
            PermissionGuard permissionGuard,
            CurrentUserService currentUserService) {
        this.eventService = eventService;
        this.eventIgnoreService = eventIgnoreService;
        this.workOrderService = workOrderService;
        this.permissionGuard = permissionGuard;
        this.currentUserService = currentUserService;
    }

    /**
     * @Author tangxinglin
     * @Description //创建事件接口
     * @Date 2026/04/18 10:00
     * @Param [request 创建事件请求对象]
     * @return ApiResponse<EventDetailVo> 新建的事件详情
     */
    @PostMapping
    public ApiResponse<EventDetailVo> createEvent(@Valid @RequestBody CreateEventRequest request) {
        permissionGuard.require(PermissionCodes.API_EVENT_CREATE);
        return ApiResponse.ok(eventService.createEvent(request));
    }

    /**
     * @Author tangxinglin
     * @Description //根据ID获取事件详情接口
     * @Date 2026/04/18 10:00
     * @Param [id 事件主键ID]
     * @return ApiResponse<EventDetailVo> 事件详情
     */
    @GetMapping("/{id}")
    public ApiResponse<EventDetailVo> getEventDetail(@PathVariable Long id) {
        permissionGuard.require(PermissionCodes.API_EVENT_DETAIL);
        return ApiResponse.ok(eventService.getEventDetail(id));
    }

    /**
     * @Author tangxinglin
     * @Description //分页查询事件列表接口，支持按外部事件ID、状态、日期范围和区域过滤
     * @Date 2026/04/18 10:00
     * @Param [externalEventId 外部事件ID（可选）, page 页码（默认1）, size 每页条数（默认20）, status 事件状态（可选）, startDate 开始日期（可选）, endDate 结束日期（可选）, areaId 区域ID（可选）]
     * @return ApiResponse<PagedResult<EventDetailVo>> 分页事件详情列表
     */
    @GetMapping
    public ApiResponse<PagedResult<EventDetailVo>> queryEvents(
            @RequestParam(required = false) String externalEventId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) Long areaId) {
        permissionGuard.require(PermissionCodes.API_EVENT_LIST);
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 100);
        return ApiResponse.ok(eventService.queryEvents(externalEventId, safePage, safeSize, status, startDate, endDate, areaId));
    }

    /**
     * @Author tangxinglin
     * @Description //将事件派单给工单处理接口，仅限Web端用户操作
     * @Date 2026/04/18 10:00
     * @Param [id 事件主键ID, request 派单请求对象，包含受派人信息]
     * @return ApiResponse<WorkOrderEntity> 新建的工单实体
     */
    @PostMapping("/{id}/dispatch")
    public ApiResponse<WorkOrderEntity> dispatchEvent(
            @PathVariable Long id,
            @RequestBody WorkOrderService.DispatchRequest request) {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.API_WORKORDER_DISPATCH);
        return ApiResponse.ok(workOrderService.dispatch(id, request));
    }

    /**
     * @Author tangxinglin
     * @Description //将事件标记为误报（忽略）接口，仅限Web端用户操作
     * @Date 2026/04/18 10:00
     * @Param [id 事件主键ID, request 忽略事件请求对象，包含忽略原因]
     * @return ApiResponse<Void> void
     */
    @PostMapping("/{id}/ignore")
    public ApiResponse<Void> ignoreEvent(
            @PathVariable Long id,
            @Valid @RequestBody IgnoreEventRequest request) {
        AuthenticatedUser operator = currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.API_EVENT_IGNORE);
        eventIgnoreService.ignoreEvent(id, operator.id(), operator.userName(), request.reason());
        return ApiResponse.ok(null);
    }

    /**
     * @Author tangxinglin
     * @Description //分页查询误报（已忽略）事件记录列表，仅限Web端用户操作
     * @Date 2026/04/18 10:00
     * @Param [page 页码（默认1）, size 每页条数（默认20）]
     * @return ApiResponse<PagedResult<EventIgnoreRecordVo>> 分页误报记录列表
     */
    @GetMapping("/false-alarms")
    public ApiResponse<PagedResult<EventIgnoreRecordVo>> listFalseAlarms(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.MENU_EVENT_FALSE_ALARM);
        return ApiResponse.ok(eventIgnoreService.listIgnoreRecords(page, size));
    }

    /**
     * @Author tangxinglin
     * @Description //批量删除事件接口，仅限Web端用户操作
     * @Date 2026/04/18 10:00
     * @Param [ids 待删除的事件主键ID列表]
     * @return ApiResponse<Void> void
     */
    @PostMapping("/batch-delete")
    public ApiResponse<Void> deleteEvents(@RequestBody  List<Long> ids) {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.API_EVENT_CREATE);
        for (Long id : ids) {
            eventService.deleteEvent(id);
        }
        return ApiResponse.ok(null);
    }
}
