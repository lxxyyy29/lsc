package com.changping.platform.modules.event.controller;

import com.changping.platform.common.exception.BusinessException;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @Author tangxinglin
 * @Description //事件控制器，提供事件的创建、查询、派单、忽略（误报）及批量删除接口
 * @Date 2026/04/18 10:00
 */
@Validated
@RestController
@RequestMapping("/events")
public class EventController {

    private static final Logger log = LoggerFactory.getLogger(EventController.class);

    private final EventService eventService;
    private final EventIgnoreService eventIgnoreService;
    private final WorkOrderService workOrderService;
    private final PermissionGuard permissionGuard;
    private final CurrentUserService currentUserService;
    private final org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

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
            CurrentUserService currentUserService,
            org.springframework.jdbc.core.JdbcTemplate jdbcTemplate) {
        this.eventService = eventService;
        this.eventIgnoreService = eventIgnoreService;
        this.workOrderService = workOrderService;
        this.permissionGuard = permissionGuard;
        this.currentUserService = currentUserService;
        this.jdbcTemplate = jdbcTemplate;
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
     * 通过外部事件ID获取事件详情（兼容MongoDB来源的事件）
     */
    @GetMapping("/by-external/{externalEventId}")
    public ApiResponse<EventDetailVo> getEventDetailByExternal(@PathVariable String externalEventId) {
        permissionGuard.require(PermissionCodes.API_EVENT_DETAIL);
        return ApiResponse.ok(eventService.getEventDetailByExternalEventId(externalEventId));
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

    /**
     * 更新事件紧急程度（三色分级）
     */
    @PutMapping("/{id}/urgency")
    public ApiResponse<Boolean> updateUrgency(@PathVariable Long id, @RequestBody Map<String, String> body) {
        permissionGuard.require(PermissionCodes.API_EVENT_CREATE);
        String level = body.get("urgencyLevel");
        if (level == null || !List.of("GREEN", "YELLOW", "RED").contains(level)) {
            throw new BusinessException("VALIDATION_ERROR", "紧急程度必须是 GREEN/YELLOW/RED");
        }
        return ApiResponse.ok(eventService.updateUrgencyLevel(id, level));
    }

    /**
     * 手动触发三色分级自动升级
     */
    @PostMapping("/auto-escalate")
    public ApiResponse<Void> autoEscalate() {
        permissionGuard.require(PermissionCodes.API_EVENT_CREATE);
        eventService.autoEscalateUrgency();
        return ApiResponse.ok(null);
    }

    /**
     * 关闭事件
     */
    @PutMapping("/{id}/close")
    public ApiResponse<Void> closeEvent(@PathVariable Long id, @RequestBody Map<String, String> body) {
        permissionGuard.require(PermissionCodes.API_EVENT_CREATE);
        String reason = body.get("reason");
        eventService.closeEvent(id, reason);
        return ApiResponse.ok(null);
    }

    /**
     * 重新打开已关闭事件
     */
    @PutMapping("/{id}/reopen")
    public ApiResponse<Void> reopenEvent(@PathVariable Long id) {
        permissionGuard.require(PermissionCodes.API_EVENT_CREATE);
        eventService.reopenEvent(id);
        return ApiResponse.ok(null);
    }

    /**
     * 获取事件/工单的附件列表
     */
    @GetMapping("/{id}/attachments")
    public ApiResponse<List<Map<String, Object>>> getAttachments(@PathVariable Long id) {
        // 查找关联的工单
        List<Map<String, Object>> workOrders = jdbcTemplate.queryForList(
            "SELECT process_instance_id FROM biz_work_order WHERE source_event_id = ?", id);

        if (workOrders.isEmpty()) {
            // 没有工单，返回事件自身的证据附件
            List<Map<String, Object>> eventMedia = jdbcTemplate.queryForList(
                "SELECT id, file_name, file_url, file_type, mime_type, uploader_name, created_at " +
                "FROM biz_media_file WHERE business_type = 'EVENT' AND business_id = ? AND status = 'ACTIVE' ORDER BY id DESC", id);
            return ApiResponse.ok(eventMedia);
        }

        // 有工单，返回所有工单操作记录的附件
        Long processInstanceId = ((Number) workOrders.get(0).get("process_instance_id")).longValue();
        List<Map<String, Object>> attachments = jdbcTemplate.query(
            "SELECT mf.id, mf.file_name, mf.file_url, mf.file_type, mf.mime_type, mf.uploader_name, mf.created_at " +
            "FROM biz_media_file mf " +
            "JOIN biz_process_action_record ar ON ar.id = mf.business_id AND mf.business_type = 'ACTION_RECORD' " +
            "WHERE ar.process_instance_id = ? AND mf.status = 'ACTIVE' " +
            "ORDER BY mf.id DESC",
            (rs, rowNum) -> {
                Map<String, Object> map = new java.util.LinkedHashMap<>();
                map.put("id", rs.getLong("id"));
                map.put("fileName", rs.getString("file_name"));
                map.put("fileUrl", rs.getString("file_url"));
                map.put("fileType", rs.getString("file_type"));
                map.put("mimeType", rs.getString("mime_type"));
                map.put("uploaderName", rs.getString("uploader_name"));
                map.put("createdAt", rs.getTimestamp("created_at"));
                return map;
            },
            processInstanceId);
        return ApiResponse.ok(attachments);
    }

    /**
     * 获取事件生命周期时间轴
     */
    @GetMapping("/{id}/timeline")
    public ApiResponse<List<EventDetailVo.LifecycleRecordVo>> getTimeline(@PathVariable Long id) {
        permissionGuard.require(PermissionCodes.API_EVENT_DETAIL);
        return ApiResponse.ok(eventService.getTimeline(id));
    }

    /**
     * 事件统计数据
     */
    @GetMapping("/statistics")
    public ApiResponse<EventService.EventStatistics> getStatistics() {
        permissionGuard.require(PermissionCodes.API_EVENT_LIST);
        return ApiResponse.ok(eventService.getStatistics());
    }

    /**
     * 审核事件（通过/驳回）
     */
    @PostMapping("/{id}/audit")
    public ApiResponse<Boolean> auditEvent(@PathVariable Long id, @RequestBody Map<String, String> body) {
        permissionGuard.require(PermissionCodes.API_EVENT_CREATE);
        String action = body.get("action");
        String remark = body.get("remark");
        if (action == null || !List.of("pass", "reject").contains(action)) {
            throw new BusinessException("VALIDATION_ERROR", "审核操作必须是 pass 或 reject");
        }
        return ApiResponse.ok(eventService.auditEvent(id, "pass".equals(action), remark));
    }

    /**
     * 群众随手拍上报（简化接口，自动填充技术字段）
     */
    @PostMapping("/public-report")
    public ResponseEntity<?> publicReport(@RequestBody Map<String, Object> body) {
        try {
            String title = (String) body.get("title");
            String description = (String) body.get("description");
            String type = (String) body.get("type");

            if (title == null || title.isBlank()) {
                return ResponseEntity.badRequest().body(ApiResponse.fail("VALIDATION_ERROR", "请填写问题标题"));
            }

            // 直接插入数据库，避免复杂的服务层逻辑
            String eventCode = "EVT-" + System.currentTimeMillis();
            String externalId = "PUBLIC-" + System.currentTimeMillis();

            jdbcTemplate.update(
                "INSERT INTO biz_event (event_code, external_event_id, source_type, source_system, event_type, title, description, incident_address, status, occurred_at, created_at, updated_at) " +
                "VALUES (?, ?, 'PUBLIC', 'PUBLIC_REPORT', ?, ?, ?, '拔蛟窝社区', 'WAITING_DISPATCH', NOW(), NOW(), NOW())",
                eventCode, externalId, type != null ? type : "OTHER", title, description);

            Long eventId = jdbcTemplate.queryForObject("SELECT id FROM biz_event WHERE event_code = ?", Long.class, eventCode);

            Map<String, Object> result = new HashMap<>();
            result.put("id", eventId);
            result.put("eventCode", eventCode);
            result.put("title", title);
            result.put("status", "WAITING_DISPATCH");

            return ResponseEntity.ok(ApiResponse.ok(result));
        } catch (Exception e) {
            log.error("群众上报失败", e);
            String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            return ResponseEntity.badRequest().body(ApiResponse.fail("PUBLIC_REPORT_ERROR", msg));
        }
    }

    /**
     * 群众对事件处置结果进行评价（打分+评论）
     */
    @PostMapping("/{id}/rate")
    public ApiResponse<Boolean> rateEvent(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        // 不需要特殊权限，任何登录用户都可以评价
        Object ratingObj = body.get("rating");
        String comment = body.get("comment") != null ? String.valueOf(body.get("comment")) : "";
        int rating = ratingObj instanceof Number ? ((Number) ratingObj).intValue() : 0;
        if (rating < 1 || rating > 5) {
            throw new BusinessException("VALIDATION_ERROR", "评分必须在1-5之间");
        }
        return ApiResponse.ok(eventService.rateEvent(id, rating, comment));
    }
}
