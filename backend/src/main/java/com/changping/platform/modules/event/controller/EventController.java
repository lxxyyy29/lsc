package com.changping.platform.modules.event.controller;

import com.changping.platform.common.exception.BusinessException;
import com.changping.platform.common.response.ApiResponse;
import com.changping.platform.modules.common.security.RateLimit;
import com.changping.platform.common.response.PagedResult;
import com.changping.platform.modules.auth.model.AuthenticatedUser;
import com.changping.platform.modules.auth.security.AuthenticatedUserContextHolder;
import com.changping.platform.modules.auth.security.PermissionCodes;
import com.changping.platform.modules.auth.security.PermissionGuard;
import com.changping.platform.modules.auth.service.AuthService;
import com.changping.platform.modules.auth.service.CurrentUserService;
import com.changping.platform.modules.event.dto.CreateEventRequest;
import com.changping.platform.modules.event.dto.IgnoreEventRequest;
import com.changping.platform.modules.event.service.EventIgnoreService;
import com.changping.platform.modules.event.service.EventService;
import com.changping.platform.modules.integration.alarm.service.AlarmEventMongoService;
import com.changping.platform.modules.event.vo.EventDetailVo;
import com.changping.platform.modules.event.vo.EventIgnoreRecordVo;
import com.changping.platform.modules.workorder.entity.WorkOrderEntity;
import com.changping.platform.modules.workorder.service.WorkOrderService;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @Author lxy
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
    private final AlarmEventMongoService alarmEventMongoService;
    private final com.changping.platform.modules.audit.service.AuditLogService auditLogService;

    /**
     * @Author lxy
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
            org.springframework.jdbc.core.JdbcTemplate jdbcTemplate,
            AlarmEventMongoService alarmEventMongoService,
            com.changping.platform.modules.audit.service.AuditLogService auditLogService) {
        this.eventService = eventService;
        this.eventIgnoreService = eventIgnoreService;
        this.workOrderService = workOrderService;
        this.permissionGuard = permissionGuard;
        this.currentUserService = currentUserService;
        this.jdbcTemplate = jdbcTemplate;
        this.alarmEventMongoService = alarmEventMongoService;
        this.auditLogService = auditLogService;
    }

    /**
     * @Author lxy
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
     * 查询当前登录用户自己的上报记录（居民小程序 WEB 端与 H5 工作人员共用）。
     */
    @GetMapping("/my-reports")
    public ApiResponse<PagedResult<Map<String, Object>>> getMyReports(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        AuthenticatedUser user = AuthenticatedUserContextHolder.getOptional()
                .orElseThrow(() -> new BusinessException("AUTH_TOKEN_REQUIRED", "请提供认证令牌"));
        int safePage = Math.max(page, 1);
        int safePageSize = Math.min(Math.max(pageSize, 1), 100);
        int offset = (safePage - 1) * safePageSize;

        // 与事件列表页口径一致：仅展示未归档的活跃事件
        Long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM biz_event WHERE report_user_id = ? AND COALESCE(archived, 0) = 0",
                Long.class,
                user.id());
        List<Map<String, Object>> items = jdbcTemplate.query(
                "SELECT id, event_code, title, description, status, created_at " +
                        "FROM biz_event WHERE report_user_id = ? AND COALESCE(archived, 0) = 0 ORDER BY id DESC LIMIT ? OFFSET ?",
                (rs, rowNum) -> {
                    Map<String, Object> item = new java.util.LinkedHashMap<>();
                    item.put("id", rs.getLong("id"));
                    item.put("eventCode", rs.getString("event_code"));
                    item.put("title", rs.getString("title"));
                    item.put("description", rs.getString("description"));
                    item.put("status", rs.getString("status"));
                    item.put("createdAt", rs.getTimestamp("created_at"));
                    return item;
                },
                user.id(),
                safePageSize,
                offset);

        return ApiResponse.ok(PagedResult.of(items, total == null ? 0 : total, safePage, safePageSize));
    }

    /**
     * @Author lxy
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
     * @Author lxy
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
            @RequestParam(required = false) String urgencyLevel,
            @RequestParam(required = false) String sourceSystem,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) Long areaId,
            @RequestParam(defaultValue = "false") boolean excludeHidden,
            @RequestParam(defaultValue = "false") boolean includeArchived,
            @RequestParam(defaultValue = "false") boolean onlyArchived) {
        permissionGuard.require(PermissionCodes.API_EVENT_LIST);
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 100);
        return ApiResponse.ok(eventService.queryEvents(externalEventId, safePage, safeSize, status, urgencyLevel, startDate, endDate, areaId, excludeHidden, includeArchived, sourceSystem, onlyArchived));
    }

    /**
     * 四类工单工作台列表（Web 端事件工单整理）：
     * closed-loop 事件闭环处置 / audit 事件审核 / completed 已完成工单 / abnormal 异常工单
     */
    @GetMapping("/sections/{section}")
    public ApiResponse<PagedResult<Map<String, Object>>> querySectionEvents(
            @PathVariable String section,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String workOrderStatus,
            @RequestParam(required = false) String urgencyLevel,
            @RequestParam(required = false) String sourceSystem,
            @RequestParam(required = false) String searchKey,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        permissionGuard.require(PermissionCodes.API_EVENT_LIST);
        return ApiResponse.ok(eventService.querySectionEvents(section, page, size, status, workOrderStatus, urgencyLevel, sourceSystem, searchKey, startDate, endDate));
    }

    /**
     * @Description //设置事件展示隐藏：隐藏后仅事件闭环/工单中心可见，大屏/GIS 等面板不再展示
     * @Param [id 事件主键ID, body hidden=true 隐藏 / false 恢复显示]
     * @return ApiResponse<Boolean> 操作结果
     */
    @PutMapping("/{id}/hidden")
    public ApiResponse<Boolean> setEventHidden(@PathVariable Long id, @RequestBody Map<String, Boolean> body) {
        permissionGuard.require(PermissionCodes.API_EVENT_CREATE);
        boolean hidden = Boolean.TRUE.equals(body.get("hidden"));
        return ApiResponse.ok(eventService.setEventHidden(id, hidden));
    }

    /**
     * @Author lxy
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
     * @Author lxy
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
     * @Author lxy
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
     * @Author lxy
     * @Description //批量删除事件接口，仅限Web端用户操作，需填写删除原因并记录审计日志
     * @Date 2026/04/18 10:00
     * @Param [body 包含 ids(事件主键ID列表) 和 reason(删除原因)]
     * @return ApiResponse<Void> void
     */
    @PostMapping("/batch-delete")
    @SuppressWarnings("unchecked")
    public ApiResponse<Void> deleteEvents(@RequestBody Map<String, Object> body) {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.API_EVENT_CREATE);
        Object idsObj = body.get("ids");
        if (!(idsObj instanceof List)) {
            throw new BusinessException("INVALID_PARAM", "缺少 ids 列表");
        }
        List<Long> ids = ((List<Object>) idsObj).stream()
                .map(o -> Long.valueOf(o.toString())).toList();
        String reason = body.get("reason") == null ? "" : body.get("reason").toString().trim();
        if (reason.isEmpty()) {
            throw new BusinessException("REASON_REQUIRED", "请填写删除原因");
        }
        com.changping.platform.modules.auth.vo.CurrentUserVo user =
                currentUserService.getCurrentUser(AuthService.ClientType.WEB, PermissionCodes.API_EVENT_CREATE);
        for (Long id : ids) {
            eventService.deleteEvent(id, reason);
            auditLogService.logQuickChange(
                    "biz_event", String.valueOf(id), "DELETE",
                    null, null, user.id(), user.realName(), reason);
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
     * 归档事件：关闭/忽略后的案件标记为已归档留存
     */
    @PostMapping("/{id}/archive")
    public ApiResponse<Void> archiveEvent(@PathVariable Long id) {
        permissionGuard.require(PermissionCodes.API_EVENT_CREATE);
        eventService.archiveEvent(id);
        return ApiResponse.ok(null);
    }

    /**
     * 12345 政务热线转办导入
     */
    @PostMapping("/12345-import")
    public ApiResponse<EventDetailVo> importFrom12345(@RequestBody Map<String, String> body) {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.API_EVENT_CREATE);
        String title = body.get("title");
        String description = body.get("description");
        String eventType = body.get("eventType");
        String location = body.get("location");
        String reporterName = body.get("reporterName");
        String reporterPhone = body.get("reporterPhone");
        String externalNo = body.get("externalNo");
        if (title == null || title.isBlank()) {
            throw new BusinessException("VALIDATION_ERROR", "请填写标题");
        }
        return ApiResponse.ok(eventService.importFrom12345(title, description, eventType, location, reporterName, reporterPhone, externalNo));
    }

    /**
     * 物业上报导入
     */
    @PostMapping("/property-report")
    public ApiResponse<EventDetailVo> reportFromProperty(@RequestBody Map<String, String> body) {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.API_EVENT_CREATE);
        String title = body.get("title");
        String description = body.get("description");
        String eventType = body.get("eventType");
        String location = body.get("location");
        String reporterName = body.get("reporterName");
        String propertyName = body.get("propertyName");
        if (title == null || title.isBlank()) {
            throw new BusinessException("VALIDATION_ERROR", "请填写标题");
        }
        return ApiResponse.ok(eventService.reportFromProperty(title, description, eventType, location, reporterName, propertyName));
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
    @RateLimit(limit = 30, window = 60, type = RateLimit.RateLimitType.IP, message = "上报过于频繁，请稍后再试")
    @PostMapping("/public-report")
    public ResponseEntity<?> publicReport(@RequestBody Map<String, Object> body) {
        try {
            String title = (String) body.get("title");
            String description = (String) body.get("description");
            String type = (String) body.get("type");
            String contactName = body.get("contactName") == null ? null : String.valueOf(body.get("contactName"));
            String contactPhone = body.get("contactPhone") == null ? null : String.valueOf(body.get("contactPhone"));
            // 现场坐标与照片（前端定位/拍照上传后随上报提交）
            Double latitude = toDoubleOrNull(body.get("latitude"));
            Double longitude = toDoubleOrNull(body.get("longitude"));
            String imagesJson = null;
            Object photosObj = body.get("photos");
            if (photosObj instanceof List<?> photoList && !photoList.isEmpty()) {
                List<String> urls = photoList.stream()
                        .filter(item -> item instanceof String s && !s.isBlank())
                        .map(item -> (String) item)
                        .toList();
                if (!urls.isEmpty()) {
                    imagesJson = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(urls);
                }
            }
            AuthenticatedUser reporter = AuthenticatedUserContextHolder.getOptional().orElse(null);
            Long reporterUserId = reporter == null ? null : reporter.id();
            String reporterName = contactName != null && !contactName.isBlank()
                    ? contactName.trim()
                    : reporter == null ? null : reporter.userName();
            String reporterPhone = contactPhone != null && !contactPhone.isBlank() ? contactPhone.trim() : null;

            if (title == null || title.isBlank()) {
                return ResponseEntity.badRequest().body(ApiResponse.fail("VALIDATION_ERROR", "请填写问题标题"));
            }

            // 直接插入数据库，避免复杂的服务层逻辑
            String eventCode = "EVT-" + System.currentTimeMillis();
            String externalId = "PUBLIC-" + System.currentTimeMillis();

            jdbcTemplate.update(
                "INSERT INTO biz_event (event_code, external_event_id, source_type, source_system, event_type, title, description, report_user_id, report_user_name, report_phone, incident_address, latitude, longitude, images, status, occurred_at, created_at, updated_at) " +
                "VALUES (?, ?, 'PUBLIC', 'PUBLIC_REPORT', ?, ?, ?, ?, ?, ?, '拔蛟窝社区', ?, ?, ?, 'WAITING_DISPATCH', NOW(), NOW(), NOW())",
                eventCode, externalId, type != null ? type : "OTHER", title, description, reporterUserId, reporterName, reporterPhone, latitude, longitude, imagesJson);

            Long eventId = jdbcTemplate.queryForObject("SELECT id FROM biz_event WHERE event_code = ?", Long.class, eventCode);

            // 同时写入 MongoDB，使事件能在列表中显示
            try {
                com.changping.platform.modules.event.dto.CreateEventRequest mongoRequest = new com.changping.platform.modules.event.dto.CreateEventRequest(
                    externalId, "PUBLIC", "PUBLIC_REPORT",
                    type != null ? type : "OTHER", title, description,
                    java.time.LocalDateTime.now(), "拔蛟窝社区", null, null,
                    new java.util.ArrayList<>(), null, null);
                alarmEventMongoService.upsertManualEvent(mongoRequest, eventId, eventCode, "WAITING_DISPATCH");
            } catch (Exception mongoEx) {
                log.warn("公众上报事件写入MongoDB失败（不影响主流程）: {}", mongoEx.getMessage());
            }

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
     * 将上报请求体中的坐标值安全转为 Double（支持数字或字符串，非法值返回 null）
     */
    private Double toDoubleOrNull(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value instanceof String str && !str.isBlank()) {
            try {
                return Double.parseDouble(str.trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
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

    /**
     * 事件热力图数据：返回有坐标的事件列表，用于地图热力图层
     */
    @GetMapping("/heatmap")
    public ApiResponse<List<Map<String, Object>>> heatmap(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String eventType) {
        permissionGuard.require(PermissionCodes.API_EVENT_LIST);
        return ApiResponse.ok(eventService.getHeatmapData(startDate, endDate, eventType));
    }

    /**
     * 事件坐标点列表（带权重：urgencyLevel RED=3, YELLOW=2, GREEN=1）
     */
    @GetMapping("/map-points")
    public ApiResponse<List<Map<String, Object>>> mapPoints(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String status) {
        permissionGuard.require(PermissionCodes.API_EVENT_LIST);
        StringBuilder sql = new StringBuilder(
            "SELECT id, title, event_type, urgency_level, status, " +
            "CAST(longitude AS DECIMAL(10,6)) as lng, CAST(latitude AS DECIMAL(10,6)) as lat, " +
            "created_at FROM biz_event " +
            "WHERE longitude IS NOT NULL AND latitude IS NOT NULL AND archived = 0 AND COALESCE(hidden, 0) = 0");
        List<Object> params = new ArrayList<>();
        if (startDate != null && !startDate.isEmpty()) { sql.append(" AND created_at >= ?"); params.add(startDate); }
        if (endDate != null && !endDate.isEmpty()) { sql.append(" AND created_at <= ?"); params.add(endDate); }
        if (status != null && !status.isEmpty()) { sql.append(" AND status = ?"); params.add(status); }
        sql.append(" ORDER BY created_at DESC LIMIT 500");
        return ApiResponse.ok(jdbcTemplate.queryForList(sql.toString(), params.toArray()));
    }

    /**
     * H5 移动端 GIS 事件坐标点（仅需 H5 登录，无需管理端权限）
     */
    @GetMapping("/h5/map-points")
    public ApiResponse<List<Map<String, Object>>> h5MapPoints() {
        currentUserService.requireClientType(AuthService.ClientType.H5);
        String sql =
            "SELECT id, title, event_type, status, " +
            "CAST(longitude AS DECIMAL(10,6)) as lng, CAST(latitude AS DECIMAL(10,6)) as lat, " +
            "created_at FROM biz_event " +
            "WHERE longitude IS NOT NULL AND latitude IS NOT NULL AND archived = 0 AND COALESCE(hidden, 0) = 0 " +
            "ORDER BY created_at DESC LIMIT 200";
        return ApiResponse.ok(jdbcTemplate.queryForList(sql));
    }

    /**
     * 批量审核事件（通过/忽略）
     */
    @PostMapping("/batch-audit")
    public ApiResponse<Map<String, Object>> batchAudit(@RequestBody Map<String, Object> request) {
        @SuppressWarnings("unchecked")
        List<Number> eventIds = (List<Number>) request.get("eventIds");
        String action = (String) request.get("action");
        if (eventIds == null || eventIds.isEmpty()) {
            return ApiResponse.fail("INVALID_PARAMS", "请选择要操作的事件");
        }
        AuthenticatedUser operator = currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.API_EVENT_IGNORE);

        int success = 0;
        for (Number id : eventIds) {
            try {
                if ("ignore".equals(action)) {
                    eventIgnoreService.ignoreEvent(id.longValue(), operator.id(), operator.userName(), "批量忽略");
                }
                success++;
            } catch (Exception e) {
                // 跳过失败
            }
        }
        return ApiResponse.ok(Map.of("success", success, "total", eventIds.size()));
    }

    /**
     * 批量派单
     */
    @PostMapping("/batch-dispatch")
    public ApiResponse<Map<String, Object>> batchDispatch(@RequestBody Map<String, Object> request) {
        @SuppressWarnings("unchecked")
        List<Number> eventIds = (List<Number>) request.get("eventIds");
        Long assigneeUserId = request.get("assigneeUserId") != null ? Long.parseLong(request.get("assigneeUserId").toString()) : null;
        String remark = (String) request.get("remark");

        if (eventIds == null || eventIds.isEmpty() || assigneeUserId == null) {
            return ApiResponse.fail("INVALID_PARAMS", "请选择事件和网格员");
        }
        permissionGuard.require(PermissionCodes.API_WORKORDER_DISPATCH);

        int success = 0;
        for (Number id : eventIds) {
            try {
                workOrderService.dispatch(id.longValue(),
                    new WorkOrderService.DispatchRequest(assigneeUserId, remark != null ? remark : "批量派单"));
                success++;
            } catch (Exception e) {
                // 跳过失败
            }
        }
        return ApiResponse.ok(Map.of("success", success, "total", eventIds.size()));
    }
}
