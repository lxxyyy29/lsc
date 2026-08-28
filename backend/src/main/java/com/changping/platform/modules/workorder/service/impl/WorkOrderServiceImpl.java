package com.changping.platform.modules.workorder.service.impl;

import com.changping.platform.common.exception.BusinessException;
import com.changping.platform.common.security.FoundationActorResolver;
import com.changping.platform.modules.auth.model.AuthenticatedUser;
import com.changping.platform.modules.auth.security.AuthenticatedUserContextHolder;
import com.changping.platform.modules.event.entity.EventEntity;
import com.changping.platform.modules.event.service.AlarmWorkflowStatusSyncService;
import com.changping.platform.modules.workorder.entity.WorkOrderEntity;
import com.changping.platform.modules.workorder.service.WorkOrderService;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

/**
 * @Author lxy
 * @Description //工单服务实现类，基于 JdbcTemplate 实现工单派发、处理、查询及删除的完整业务逻辑
 * @Date 2026/04/18 09:30
 */
@Service
public class WorkOrderServiceImpl implements WorkOrderService {

    private static final Logger log = LoggerFactory.getLogger(WorkOrderServiceImpl.class);

    private static final String WORK_ORDER_BUSINESS_TYPE = "EVENT_WORK_ORDER";
    private static final String WORK_ORDER_MEDIA_TYPE = "WORK_ORDER";
    private static final String PROCESS_STATUS_RUNNING = "RUNNING";
    private static final String PROCESS_STATUS_APPROVED = "APPROVED";

    /** 重点事件类型→路由到两委干部；其余简易事件→网格员 */
    private static final java.util.Set<String> SERIOUS_EVENT_TYPES = java.util.Set.of(
            "COMPLAINT", "FIRE", "ILLEGAL_BUILDING", "PUBLIC_SAFETY", "SAFETY", "SAFE",
            "民生诉求", "消防安全", "违建", "公共安全", "安全生产", "矛盾纠纷", "防汛防台风");

    private final JdbcTemplate jdbcTemplate;
    private final FoundationActorResolver foundationActorResolver;
    private final AlarmWorkflowStatusSyncService alarmWorkflowStatusSyncService;

    /**
     * @Author lxy
     * @Description //构造函数，注入数据库模板、操作者解析器、告警同步服务及流程模板服务
     * @Date 2026/04/18 09:30
     * @Param [jdbcTemplate 数据库操作模板, foundationActorResolver 操作者解析器, alarmWorkflowStatusSyncService 告警工作流状态同步服务, processTemplateService 流程模板服务]
     * @return
     */
    public WorkOrderServiceImpl(
            JdbcTemplate jdbcTemplate,
            FoundationActorResolver foundationActorResolver,
            AlarmWorkflowStatusSyncService alarmWorkflowStatusSyncService) {
        this.jdbcTemplate = jdbcTemplate;
        this.foundationActorResolver = foundationActorResolver;
        this.alarmWorkflowStatusSyncService = alarmWorkflowStatusSyncService;
    }

    /**
     * @Author lxy
     * @Description //派发工单：校验事件状态、创建流程实例、插入工单记录并同步事件状态
     * @Date 2026/04/18 09:30
     * @Param [eventId 事件ID, request 派发请求，包含流程模板ID和备注]
     * @return WorkOrderEntity 新创建的工单实体
     */
    @Override
    @Transactional
    public WorkOrderEntity dispatch(Long eventId, DispatchRequest request) {
        validateDispatchRequest(request);
        FoundationActorResolver.Actor actor = requireAuthenticatedActor();
        EventEntity event = getEvent(eventId);
        if (workOrderExists(eventId)) {
            throw new BusinessException("WORK_ORDER_ALREADY_EXISTS", "事件已派发为工单");
        }
        if (!"WAITING_DISPATCH".equals(event.getStatus()) && !"WAITING_LEADER_REVIEW".equals(event.getStatus())) {
            throw new BusinessException("WORK_ORDER_DISPATCH_STATUS_INVALID", "事件必须处于待派发或组长审核状态才能派发");
        }

        // 按事件类型智能路由：推荐受理角色（前端据此过滤人员）
        String recommendedRole = resolveRecommendedRole(event.getEventType());

        // 创建轻量审计容器（无模板、无节点）
        Long processInstanceId = createProcessInstance(event, actor);
        DispatchAssignee assignee = resolveDispatchAssignee(request.assigneeUserId());

        String workOrderNo = "WO-" + eventId + "-" + System.currentTimeMillis();
        String urgencyLevel = event.getUrgencyLevel() != null ? event.getUrgencyLevel() : "GREEN";
        try {
            jdbcTemplate.update(
                    "INSERT INTO biz_work_order (work_order_no, source_event_id, process_instance_id, status, urgency_level, assignee_user_id, assignee_name, dispatcher_user_id, dispatcher_name, created_at, updated_at) VALUES (?, ?, ?, 'PROCESSING', ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                    workOrderNo,
                    eventId,
                    processInstanceId,
                    urgencyLevel,
                    assignee.id(),
                    assignee.name(),
                    actor.userId(),
                    actor.name());
        } catch (DataIntegrityViolationException exception) {
            if (isDuplicateWorkOrder(exception)) {
                throw new BusinessException("WORK_ORDER_ALREADY_EXISTS", "事件已派发为工单");
            }
            throw exception;
        }

        String fromStatus = event.getStatus();
        boolean fromLeaderReview = "WAITING_LEADER_REVIEW".equals(fromStatus);
        String dispatchAction = fromLeaderReview ? "LEADER_DISPATCH" : "WORK_ORDER_DISPATCH";
        String dispatchRemark = fromLeaderReview ? "组长派单" : "派发工单";
        log.info("[DISPATCH] 派单开始: eventId={}, fromStatus={}, assignee={}({}), dispatcher={}, type={}",
                eventId, fromStatus, assignee.name(), assignee.id(), actor.name(),
                fromLeaderReview ? "LEADER_DISPATCH(组长派单)" : "WORK_ORDER_DISPATCH(普通派单)");
        updateEventStatus(eventId, "DISPATCHED_TO_WORK_ORDER", fromStatus, "WORK_ORDER_DISPATCH_STATUS_INVALID", "事件状态已变化，请刷新后重试");
        insertEventRecord(eventId, fromStatus, "DISPATCHED_TO_WORK_ORDER", dispatchAction, actor, request.remark());
        insertProcessActionRecord(processInstanceId, null, "WORK_ORDER_DISPATCH", PROCESS_STATUS_RUNNING, request.remark(), actor, null, null);
        log.info("[DISPATCH] 派单完成: eventId={}, workOrderNo={}, assignee={}, type={}", eventId, workOrderNo, assignee.name(), dispatchAction);
        return getWorkOrderByEventId(eventId);
    }

    /**
     * 按事件类型智能路由：重点事件→两委干部(EVENT_OPERATOR)，简易事件→网格员(H5_WORKER)
     */
    private String resolveRecommendedRole(String eventType) {
        if (eventType != null && SERIOUS_EVENT_TYPES.contains(eventType.trim())) {
            return "EVENT_OPERATOR";
        }
        return "H5_WORKER";
    }

    /**
     * @Author lxy
     * @Description //处理工单节点：根据结果（通过/拒绝）推进或关闭流程，更新工单和事件状态，保存附件
     * @Date 2026/04/18 09:30
     * @Param [workOrderId 工单ID, request 处理请求，包含处理结果、备注及附件]
     * @return WorkOrderEntity 更新后的工单实体
     */
    @Override
    @Transactional
    public WorkOrderEntity handle(Long workOrderId, HandleRequest request) {
        validateHandleRequest(request);
        FoundationActorResolver.Actor actor = requireAuthenticatedActor();
        WorkOrderEntity workOrder = getWorkOrder(workOrderId);

        // 检查工单状态，已关闭/已完成的工单不允许操作
        if ("CLOSED".equals(workOrder.getStatus()) || "COMPLETED".equals(workOrder.getStatus())) {
            throw new BusinessException("WORK_ORDER_STATUS_INVALID", "工单已关闭或已完成，无法操作");
        }

        requireH5Participant(workOrder, actor);

        // 校验当前处理人 = 工单受派人（不再依赖流程节点）
        if (workOrder.getAssigneeUserId() == null || !workOrder.getAssigneeUserId().equals(actor.userId())) {
            throw new BusinessException("WORK_ORDER_NOT_FOUND", "工单未找到");
        }

        // 兼容中文结果（H5核查页传中文标签）
        String result = normalizeResult(request.result());

        // 不属实 → 工单和事件同时关闭
        if ("NOT_TRUE".equals(result)) {
            jdbcTemplate.update(
                    "UPDATE biz_process_instance SET status = 'REJECTED', finished_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP WHERE id = ?",
                    workOrder.getProcessInstanceId());
            updateWorkOrder(workOrderId, "CLOSED", actor.userId(), actor.name(), null, LocalDateTime.now(), request.remark());
            updateEventStatus(workOrder.getSourceEventId(), "CLOSED", "DISPATCHED_TO_WORK_ORDER", "EVENT_CLOSE_STATUS_INVALID", "事件必须处于已派发状态才能关闭");
            insertEventRecord(workOrder.getSourceEventId(), "DISPATCHED_TO_WORK_ORDER", "CLOSED", "WORK_ORDER_NOT_TRUE", actor, request.remark());
            Long actionRecordId = insertProcessActionRecord(workOrder.getProcessInstanceId(), null, "WORK_ORDER_HANDLE", "NOT_TRUE", request.remark(), actor, request.subjectType(), request.subjectId());
            saveAttachments(actionRecordId, request.attachments(), actor);
            return getWorkOrder(workOrderId);
        }

        // 需补充证据 → 工单进入待核实
        if ("NEEDS_MORE_EVIDENCE".equals(result)) {
            Long actionRecordId = insertProcessActionRecord(workOrder.getProcessInstanceId(), null, "WORK_ORDER_HANDLE", "NEEDS_MORE_EVIDENCE", request.remark(), actor, request.subjectType(), request.subjectId());
            saveAttachments(actionRecordId, request.attachments(), actor);
            updateWorkOrder(workOrderId, "WAITING_VERIFY", actor.userId(), actor.name(), null, null, request.remark());
            insertEventRecord(workOrder.getSourceEventId(), "DISPATCHED_TO_WORK_ORDER", "DISPATCHED_TO_WORK_ORDER", "WORK_ORDER_NEEDS_EVIDENCE", actor, request.remark());
            return getWorkOrder(workOrderId);
        }

        // RESOLVED / APPROVED（属实并已处理）→ 直接进入待关闭确认（无多节点，一次处理即完成）
        Long actionRecordId = insertProcessActionRecord(workOrder.getProcessInstanceId(), null, "WORK_ORDER_HANDLE", "APPROVED", request.remark(), actor, request.subjectType(), request.subjectId());
        saveAttachments(actionRecordId, request.attachments(), actor);

        jdbcTemplate.update(
                "UPDATE biz_process_instance SET status = 'APPROVED', finished_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP WHERE id = ?",
                workOrder.getProcessInstanceId());
        updateWorkOrder(workOrderId, "WAITING_CLOSE_CONFIRM", null, null, null, null, request.remark());
        log.info("工单{}处理完成，进入待关闭确认状态", workOrderId);
        insertEventRecord(workOrder.getSourceEventId(), "DISPATCHED_TO_WORK_ORDER", "DISPATCHED_TO_WORK_ORDER", "WORK_ORDER_WAITING_CLOSE", actor, request.remark());
        insertProcessActionRecord(workOrder.getProcessInstanceId(), null, "WORK_ORDER_RESOLVED", PROCESS_STATUS_APPROVED, request.remark(), actor, null, null);
        return getWorkOrder(workOrderId);
    }

    /**
     * 统一处理结果：兼容英文代码与中文标签（H5核查页传中文）
     */
    private String normalizeResult(String rawResult) {
        if (rawResult == null) {
            return "";
        }
        String r = rawResult.trim();
        switch (r) {
            case "属实并已处理": return "RESOLVED";
            case "不属实": return "NOT_TRUE";
            case "需补充证据": return "NEEDS_MORE_EVIDENCE";
            default: return r.toUpperCase();
        }
    }

    @Override
    public List<WebWorkOrderSummary> queryWebWorkOrders() {
        return jdbcTemplate.query(
                "SELECT wo.id, wo.work_order_no, wo.source_event_id, wo.status, wo.assignee_name, wo.dispatcher_name, wo.created_at, wo.updated_at, wo.hidden, "
                        + "e.event_code, e.title, e.area_id, e.area_name, e.urgency_level "
                        + "FROM biz_work_order wo "
                        + "LEFT JOIN biz_event e ON e.id = wo.source_event_id "
                        + "ORDER BY wo.id DESC",
                (rs, rowNum) -> new WebWorkOrderSummary(
                        rs.getLong("id"),
                        rs.getString("work_order_no"),
                        rs.getLong("source_event_id"),
                        rs.getString("event_code"),
                        rs.getString("title"),
                        rs.getString("status"),
                        rs.getString("assignee_name"),
                        rs.getString("dispatcher_name"),
                        getNullableTime(rs.getTimestamp("created_at")),
                        getNullableTime(rs.getTimestamp("updated_at")),
                        getNullableLong(rs, "area_id"),
                        rs.getString("area_name"),
                        rs.getString("urgency_level"),
                        rs.getInt("hidden") == 1));
    }

    @Override
    public PagedWorkOrders queryWebWorkOrdersPaged(int page, int pageSize, String status, String assignee, Long areaId) {
        int safePage = Math.max(1, page);
        int safeSize = Math.max(1, Math.min(pageSize, 500));
        int offset = (safePage - 1) * safeSize;

        List<Object> params = new ArrayList<>();
        StringBuilder where = new StringBuilder();

        if (status != null && !status.isBlank()) {
            where.append(" AND wo.status = ?");
            params.add(status.trim());
        }
        if (assignee != null && !assignee.isBlank()) {
            where.append(" AND wo.assignee_name LIKE ?");
            params.add("%" + assignee.trim() + "%");
        }
        if (areaId != null) {
            where.append(" AND e.area_id = ?");
            params.add(areaId);
        }

        String whereClause = where.length() > 0 ? " WHERE " + where.substring(5) : "";

        Long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM biz_work_order wo LEFT JOIN biz_event e ON e.id = wo.source_event_id" + whereClause,
                Long.class,
                params.toArray());

        List<Object> pageParams = new ArrayList<>(params);
        pageParams.add(safeSize);
        pageParams.add(offset);

        List<WebWorkOrderSummary> items = jdbcTemplate.query(
                "SELECT wo.id, wo.work_order_no, wo.source_event_id, wo.status, wo.assignee_name, wo.dispatcher_name, wo.created_at, wo.updated_at, wo.hidden, "
                        + "e.event_code, e.title, e.area_id, e.area_name, e.urgency_level "
                        + "FROM biz_work_order wo "
                        + "LEFT JOIN biz_event e ON e.id = wo.source_event_id"
                        + whereClause
                        + " ORDER BY wo.id DESC "
                        + "LIMIT ? OFFSET ?",
                (rs, rowNum) -> new WebWorkOrderSummary(
                        rs.getLong("id"),
                        rs.getString("work_order_no"),
                        rs.getLong("source_event_id"),
                        rs.getString("event_code"),
                        rs.getString("title"),
                        rs.getString("status"),
                        rs.getString("assignee_name"),
                        rs.getString("dispatcher_name"),
                        getNullableTime(rs.getTimestamp("created_at")),
                        getNullableTime(rs.getTimestamp("updated_at")),
                        getNullableLong(rs, "area_id"),
                        rs.getString("area_name"),
                        rs.getString("urgency_level"),
                        rs.getInt("hidden") == 1),
                pageParams.toArray());

        return new PagedWorkOrders(items, total != null ? total : 0, safePage, safeSize);
    }

    @Override
    public WebWorkOrderDetail getWebWorkOrderDetail(Long workOrderId) {
        List<WebWorkOrderDetail> details = jdbcTemplate.query(
                "SELECT wo.id, wo.work_order_no, wo.source_event_id, wo.process_instance_id, wo.status, wo.assignee_name, wo.dispatcher_name, wo.close_reason, wo.created_at, wo.updated_at, wo.completed_at, wo.closed_at, "
                        + "e.event_code, e.title, e.event_type, e.source_type, e.status AS event_status, e.description, e.urgency_level "
                        + "FROM biz_work_order wo "
                        + "LEFT JOIN biz_event e ON e.id = wo.source_event_id "
                        + "WHERE wo.id = ?",
                (rs, rowNum) -> new WebWorkOrderDetail(
                        rs.getLong("id"),
                        rs.getString("work_order_no"),
                        rs.getLong("source_event_id"),
                        getNullableLong(rs, "process_instance_id"),
                        rs.getString("status"),
                        rs.getString("assignee_name"),
                        rs.getString("dispatcher_name"),
                        rs.getString("event_code"),
                        rs.getString("title"),
                        rs.getString("event_type"),
                        rs.getString("source_type"),
                        rs.getString("event_status"),
                        rs.getString("description"),
                        rs.getString("close_reason"),
                        getNullableTime(rs.getTimestamp("created_at")),
                        getNullableTime(rs.getTimestamp("updated_at")),
                        getNullableTime(rs.getTimestamp("completed_at")),
                        getNullableTime(rs.getTimestamp("closed_at")),
                        List.of(),
                        rs.getString("urgency_level")),
                workOrderId);
        if (details.isEmpty()) {
            throw new BusinessException("WORK_ORDER_NOT_FOUND", "工单未找到");
        }
        WebWorkOrderDetail detail = details.get(0);
        return new WebWorkOrderDetail(
                detail.id(),
                detail.workOrderNo(),
                detail.sourceEventId(),
                detail.processInstanceId(),
                detail.status(),
                detail.assigneeName(),
                detail.dispatcherName(),
                detail.eventCode(),
                detail.eventTitle(),
                detail.eventType(),
                detail.sourceType(),
                detail.eventStatus(),
                detail.description(),
                detail.closeReason(),
                detail.createdAt(),
                detail.updatedAt(),
                detail.completedAt(),
                detail.closedAt(),
                listWebWorkOrderFlowRecords(detail.processInstanceId()),
                detail.urgencyLevel());
    }

    @Override
    public List<H5WorkOrderListItem> queryH5WorkOrders() {
        FoundationActorResolver.Actor actor = requireH5Actor();
        Long actorUserId = actor.userId();
        return jdbcTemplate.query(
                "SELECT wo.id, wo.work_order_no, wo.status, wo.assignee_user_id, wo.assignee_name, "
                        + "wo.dispatcher_name, wo.created_at, wo.updated_at, "
                        + "e.title AS event_title, e.area_name, e.urgency_level, "
                        + "wo.status = 'PROCESSING' AND wo.assignee_user_id = ? AS is_current_handler "
                        + "FROM biz_work_order wo "
                        + "LEFT JOIN biz_event e ON e.id = wo.source_event_id "
                        + "WHERE wo.assignee_user_id = ? "
                        + "   OR EXISTS (SELECT 1 FROM biz_process_action_record record "
                        + "     WHERE record.process_instance_id = wo.process_instance_id "
                        + "       AND record.operator_user_id = ?) "
                        + "ORDER BY wo.id DESC",
                (rs, rowNum) -> new H5WorkOrderListItem(
                        rs.getLong("id"),
                        rs.getString("work_order_no"),
                        rs.getString("status"),
                        getNullableLong(rs, "assignee_user_id"),
                        rs.getString("assignee_name"),
                        rs.getString("dispatcher_name"),
                        getNullableTime(rs.getTimestamp("created_at")),
                        getNullableTime(rs.getTimestamp("updated_at")),
                        rs.getString("event_title"),
                        null,
                        rs.getBoolean("is_current_handler"),
                        rs.getString("area_name"),
                        rs.getString("urgency_level")),
                actorUserId,
                actorUserId,
                actorUserId);
    }

    @Override
    public WorkOrderEntity getWorkOrderDetail(Long workOrderId) {
        FoundationActorResolver.Actor actor = requireH5Actor();
        WorkOrderEntity workOrder = getWorkOrder(workOrderId);
        requireH5Participant(workOrder, actor);
        return workOrder;
    }

    @Override
    public H5WorkOrderDetail getH5WorkOrderDetail(Long workOrderId) {
        FoundationActorResolver.Actor actor = requireH5Actor();

        List<H5WorkOrderDetail> details = jdbcTemplate.query(
                "SELECT wo.id, wo.work_order_no, wo.status, wo.assignee_user_id, wo.assignee_name, "
                        + "wo.dispatcher_name, wo.created_at, wo.updated_at, wo.source_event_id, "
                        + "wo.process_instance_id, "
                        + "e.title, e.incident_address, e.description, e.event_type "
                        + "FROM biz_work_order wo "
                        + "LEFT JOIN biz_event e ON e.id = wo.source_event_id "
                        + "WHERE wo.id = ?",
                (rs, rowNum) -> new H5WorkOrderDetail(
                        rs.getLong("id"),
                        rs.getString("work_order_no"),
                        rs.getString("status"),
                        getNullableLong(rs, "assignee_user_id"),
                        rs.getString("assignee_name"),
                        rs.getString("dispatcher_name"),
                        getNullableTime(rs.getTimestamp("created_at")),
                        getNullableTime(rs.getTimestamp("updated_at")),
                        rs.getLong("source_event_id"),
                        rs.getString("title"),
                        rs.getString("incident_address"),
                        rs.getString("description"),
                        null,
                        rs.getString("event_type"),
                        false,
                        List.of(),
                        List.of()),
                workOrderId);

        if (details.isEmpty()) {
            throw new BusinessException("WORK_ORDER_NOT_FOUND", "工单未找到");
        }

        H5WorkOrderDetail base = details.get(0);

        WorkOrderEntity workOrder = getWorkOrder(workOrderId);
        requireH5Participant(workOrder, actor);

        Long processInstanceId = workOrder.getProcessInstanceId();
        // 当前处理人 = 工单受派人 且 状态为处理中
        boolean isCurrentHandler = "PROCESSING".equals(base.status())
                && base.assigneeUserId() != null
                && base.assigneeUserId().equals(actor.userId());

        List<H5ActionRecordVo> actionRecords = listH5ActionRecords(processInstanceId);

        return new H5WorkOrderDetail(
                base.id(),
                base.workOrderNo(),
                base.status(),
                base.assigneeUserId(),
                base.assigneeName(),
                base.dispatcherName(),
                base.createdAt(),
                base.updatedAt(),
                base.sourceEventId(),
                base.eventTitle(),
                base.eventLocation(),
                base.eventDescription(),
                base.merchantName(),
                base.eventType(),
                isCurrentHandler,
                List.of(),
                actionRecords);
    }

    @Override
    public H5WorkbenchSummary getH5Workbench() {
        FoundationActorResolver.Actor actor = requireH5Actor();
        Long uid = actor.userId();
        // 合并为单次查询，使用条件聚合减少数据库往返（参与条件：受派人 或 操作记录操作人）
        String sql = "SELECT "
                + "COUNT(DISTINCT CASE WHEN (wo.assignee_user_id = ? OR record_part.process_instance_id IS NOT NULL) THEN wo.id END) AS total_count, "
                + "COUNT(DISTINCT CASE WHEN wo.status = 'PROCESSING' AND wo.assignee_user_id = ? THEN wo.id END) AS waiting_accept_count, "
                + "COUNT(DISTINCT CASE WHEN wo.status IN ('PROCESSING', 'WAITING_CLOSE_CONFIRM', 'WAITING_VERIFY') "
                + "  AND (wo.assignee_user_id = ? OR record_part.process_instance_id IS NOT NULL) "
                + "  AND NOT (wo.status = 'PROCESSING' AND wo.assignee_user_id = ?) "
                + "  THEN wo.id END) AS pending_close_count, "
                + "COUNT(DISTINCT CASE WHEN wo.status IN ('COMPLETED', 'CLOSED', 'TIMEOUT') "
                + "  AND (wo.assignee_user_id = ? OR record_part.process_instance_id IS NOT NULL) "
                + "  THEN wo.id END) AS closed_count "
                + "FROM biz_work_order wo "
                + "LEFT JOIN (SELECT DISTINCT process_instance_id FROM biz_process_action_record WHERE operator_user_id = ?) record_part ON record_part.process_instance_id = wo.process_instance_id ";
        Map<String, Object> result = jdbcTemplate.queryForMap(sql, uid, uid, uid, uid, uid, uid);
        int totalCount = ((Number) result.get("total_count")).intValue();
        int waitingAcceptCount = ((Number) result.get("waiting_accept_count")).intValue();
        int pendingCloseCount = ((Number) result.get("pending_close_count")).intValue();
        int closedCount = ((Number) result.get("closed_count")).intValue();
        return new H5WorkbenchSummary(totalCount, waitingAcceptCount, pendingCloseCount, closedCount);
    }

    @Override
    public boolean setWorkOrderHidden(Long workOrderId, boolean hidden) {
        int updated = jdbcTemplate.update(
                "UPDATE biz_work_order SET hidden = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?",
                hidden ? 1 : 0, workOrderId);
        if (updated == 0) {
            throw new BusinessException("WORK_ORDER_NOT_FOUND", "工单不存在");
        }
        return true;
    }

    @Override
    @Transactional
    public void deleteWorkOrder(Long workOrderId) {
        WorkOrderEntity workOrder = getWorkOrder(workOrderId);
        Long piId = workOrder.getProcessInstanceId();

        if (piId != null) {
            jdbcTemplate.update(
                "DELETE FROM biz_media_file WHERE business_type = 'ACTION_RECORD' AND business_id IN (SELECT id FROM biz_process_action_record WHERE process_instance_id = ?)",
                piId);
            jdbcTemplate.update("DELETE FROM biz_process_action_record WHERE process_instance_id = ?", piId);
            jdbcTemplate.update("DELETE FROM biz_process_instance_node WHERE process_instance_id = ?", piId);
        }

        jdbcTemplate.update("DELETE FROM biz_work_order WHERE id = ?", workOrderId);

        if (piId != null) {
            jdbcTemplate.update("DELETE FROM biz_process_instance WHERE id = ?", piId);
        }
    }

    /**
     * 创建轻量审计容器（无模板、无节点），仅作为工单操作记录的归属壳
     */
    private Long createProcessInstance(EventEntity event, FoundationActorResolver.Actor actor) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO biz_process_instance (process_no, template_id, business_type, business_id, status, started_at, finished_at, created_at, updated_at) VALUES (?, NULL, ?, ?, ?, CURRENT_TIMESTAMP, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                    Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, "PI-WO-" + event.getId() + "-" + UUID.randomUUID());
            statement.setString(2, WORK_ORDER_BUSINESS_TYPE);
            statement.setLong(3, event.getId());
            statement.setString(4, PROCESS_STATUS_RUNNING);
            return statement;
        }, keyHolder);
        Long processInstanceId = extractGeneratedId(keyHolder);

        insertProcessActionRecord(processInstanceId, null, "WORK_ORDER_START", PROCESS_STATUS_RUNNING, null, actor, null, null);
        return processInstanceId;
    }

    private List<WebWorkOrderFlowRecord> listWebWorkOrderFlowRecords(Long processInstanceId) {
        if (processInstanceId == null) {
            return List.of();
        }
        return jdbcTemplate.query(
                "SELECT record.id, record.action_type, record.action_result, record.remark, record.operator_name, record.created_at, record.subject_type, record.subject_id, node.node_name, "
                        + "CASE "
                        + "  WHEN record.subject_type = 'MERCHANT' THEN m.merchant_name "
                        + "  WHEN record.subject_type = 'VENDOR' THEN v.vendor_name "
                        + "END AS subject_name, "
                        + "mf.attachments "
                        + "FROM biz_process_action_record record "
                        + "LEFT JOIN biz_process_instance_node node ON node.id = record.process_instance_node_id "
                        + "LEFT JOIN biz_merchant m ON m.id = record.subject_id AND record.subject_type = 'MERCHANT' "
                        + "LEFT JOIN biz_mobile_vendor v ON v.id = record.subject_id AND record.subject_type = 'VENDOR' "
                        + "LEFT JOIN ("
                        + "  SELECT business_id, "
                        + "    CONCAT('[', GROUP_CONCAT(JSON_OBJECT('fileName', file_name, 'fileUrl', file_url, 'fileType', file_type)), ']') AS attachments "
                        + "  FROM biz_media_file WHERE business_type = 'ACTION_RECORD' AND status = 'ACTIVE' GROUP BY business_id"
                        + ") mf ON mf.business_id = record.id "
                        + "WHERE record.process_instance_id = ? "
                        + "ORDER BY record.created_at ASC, record.id ASC",
                (rs, rowNum) -> new WebWorkOrderFlowRecord(
                        rs.getLong("id"),
                        rs.getString("action_type"),
                        rs.getString("action_result"),
                        rs.getString("remark"),
                        rs.getString("operator_name"),
                        rs.getString("node_name"),
                        getNullableTime(rs.getTimestamp("created_at")),
                        rs.getString("subject_type"),
                        getNullableLong(rs, "subject_id"),
                        rs.getString("subject_name"),
                        rs.getString("attachments")),
                processInstanceId);
    }

    /**
     * 校验派发请求，受派人不能为空
     */
    private void validateDispatchRequest(DispatchRequest request) {
        if (request == null || request.assigneeUserId() == null) {
            throw new BusinessException("VALIDATION_ERROR", "请选择受派人员");
        }
    }

    private DispatchAssignee resolveDispatchAssignee(Long assigneeUserId) {
        List<DispatchAssignee> users = jdbcTemplate.query(
                "SELECT u.id, u.real_name FROM sys_user u " +
                        "WHERE u.id = ? AND u.deleted = 0 AND u.status = 'ACTIVE' LIMIT 1",
                (rs, rowNum) -> new DispatchAssignee(rs.getLong("id"), rs.getString("real_name")),
                assigneeUserId);
        if (users.isEmpty()) {
            throw new BusinessException("WORK_ORDER_ASSIGNEE_INVALID", "请选择有效的受派人员");
        }
        return users.get(0);
    }

    /**
     * @Author lxy
     * @Description //校验处理请求，处理结果不能为空，附件字段不能含空值
     * @Date 2026/04/18 09:30
     * @Param [request 处理请求]
     * @return void
     */
    private void validateHandleRequest(HandleRequest request) {
        if (request == null || !StringUtils.hasText(request.result())) {
            throw new BusinessException("VALIDATION_ERROR", "处理结果不能为空");
        }
        if (request.attachments() == null) {
            return;
        }
        for (HandleAttachment attachment : request.attachments()) {
            if (attachment == null || !StringUtils.hasText(attachment.fileName()) || !StringUtils.hasText(attachment.fileUrl())) {
                throw new BusinessException("VALIDATION_ERROR", "附件文件名和文件地址不能为空");
            }
        }
    }

    /**
     * @Author lxy
     * @Description //根据事件ID查询事件实体，不存在时抛出业务异常
     * @Date 2026/04/18 09:30
     * @Param [eventId 事件ID]
     * @return EventEntity 事件实体
     */
    private EventEntity getEvent(Long eventId) {
        List<EventEntity> events = jdbcTemplate.query(
                "SELECT id, event_code, external_event_id, title, description, source_type, source_system, event_type, status FROM biz_event WHERE id = ?",
                (rs, rowNum) -> {
                    EventEntity event = new EventEntity();
                    event.setId(rs.getLong("id"));
                    event.setEventCode(rs.getString("event_code"));
                    event.setExternalEventId(rs.getString("external_event_id"));
                    event.setTitle(rs.getString("title"));
                    event.setDescription(rs.getString("description"));
                    event.setSourceType(rs.getString("source_type"));
                    event.setSourceSystem(rs.getString("source_system"));
                    event.setEventType(rs.getString("event_type"));
                    event.setStatus(rs.getString("status"));
                    return event;
                },
                eventId);
        if (events.isEmpty()) {
            throw new BusinessException("EVENT_NOT_FOUND", "事件未找到");
        }
        return events.get(0);
    }

    /**
     * @Author lxy
     * @Description //判断指定事件是否已存在工单记录
     * @Date 2026/04/18 09:30
     * @Param [eventId 事件ID]
     * @return boolean 是否已存在工单
     */
    private boolean workOrderExists(Long eventId) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM biz_work_order WHERE source_event_id = ?", Integer.class, eventId);
        return count != null && count > 0;
    }

    /**
     * @Author lxy
     * @Description //根据事件ID查询对应的工单实体
     * @Date 2026/04/18 09:30
     * @Param [eventId 事件ID]
     * @return WorkOrderEntity 工单实体
     */
    private WorkOrderEntity getWorkOrderByEventId(Long eventId) {
        return jdbcTemplate.queryForObject(workOrderSelectSql() + " WHERE source_event_id = ?", (rs, rowNum) -> mapWorkOrder(rs), eventId);
    }

    /**
     * @Author lxy
     * @Description //根据工单ID查询工单实体，不存在时抛出业务异常
     * @Date 2026/04/18 09:30
     * @Param [workOrderId 工单ID]
     * @return WorkOrderEntity 工单实体
     */
    private WorkOrderEntity getWorkOrder(Long workOrderId) {
        List<WorkOrderEntity> workOrders = jdbcTemplate.query(workOrderSelectSql() + " WHERE id = ?", (rs, rowNum) -> mapWorkOrder(rs), workOrderId);
        if (workOrders.isEmpty()) {
            throw new BusinessException("WORK_ORDER_NOT_FOUND", "工单未找到");
        }
        return workOrders.get(0);
    }

    /**
     * @Author lxy
     * @Description //返回工单查询的 SELECT 基础语句（不含 WHERE 条件）
     * @Date 2026/04/18 09:30
     * @Param []
     * @return String 工单查询 SQL 片段
     */
    private String workOrderSelectSql() {
        return "SELECT id, work_order_no, source_event_id, process_instance_id, status, assignee_user_id, assignee_name, dispatcher_user_id, dispatcher_name, completed_at, closed_at, close_reason, created_at, updated_at FROM biz_work_order";
    }

    /**
     * @Author lxy
     * @Description //将 ResultSet 行映射为 WorkOrderEntity 对象
     * @Date 2026/04/18 09:30
     * @Param [rs 数据库结果集]
     * @return WorkOrderEntity 工单实体
     */
    private WorkOrderEntity mapWorkOrder(ResultSet rs) throws SQLException {
        WorkOrderEntity entity = new WorkOrderEntity();
        entity.setId(rs.getLong("id"));
        entity.setWorkOrderNo(rs.getString("work_order_no"));
        entity.setSourceEventId(rs.getLong("source_event_id"));
        entity.setProcessInstanceId(getNullableLong(rs, "process_instance_id"));
        entity.setStatus(rs.getString("status"));
        entity.setAssigneeUserId(getNullableLong(rs, "assignee_user_id"));
        entity.setAssigneeName(rs.getString("assignee_name"));
        entity.setDispatcherUserId(getNullableLong(rs, "dispatcher_user_id"));
        entity.setDispatcherName(rs.getString("dispatcher_name"));
        entity.setCompletedAt(getNullableTime(rs.getTimestamp("completed_at")));
        entity.setClosedAt(getNullableTime(rs.getTimestamp("closed_at")));
        entity.setCloseReason(rs.getString("close_reason"));
        entity.setCreatedAt(getNullableTime(rs.getTimestamp("created_at")));
        entity.setUpdatedAt(getNullableTime(rs.getTimestamp("updated_at")));
        return entity;
    }

    /**
     * @Author lxy
     * @Description //校验当前 H5 用户是工单流程的参与者之一，否则抛出业务异常
     * @Date 2026/04/18 09:30
     * @Param [workOrder 工单实体, actor 当前 H5 用户]
     * @return void
     */
    private void requireH5Participant(WorkOrderEntity workOrder, FoundationActorResolver.Actor actor) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM biz_work_order wo "
                        + "WHERE wo.id = ? "
                        + "  AND (wo.assignee_user_id = ? "
                        + "    OR EXISTS (SELECT 1 FROM biz_process_action_record record "
                        + "      WHERE record.process_instance_id = wo.process_instance_id "
                        + "        AND record.operator_user_id = ?))",
                Integer.class,
                workOrder.getId(),
                actor.userId(),
                actor.userId());
        if (count == null || count == 0) {
            throw new BusinessException("WORK_ORDER_NOT_FOUND", "工单未找到");
        }
    }

    /**
     * @Author lxy
     * @Description //获取当前认证的 H5 用户操作者，userId 为空时抛出业务异常
     * @Date 2026/04/18 09:30
     * @Param []
     * @return FoundationActorResolver.Actor H5 用户操作者
     */
    private FoundationActorResolver.Actor requireH5Actor() {
        FoundationActorResolver.Actor actor = foundationActorResolver.resolveActor();
        if (actor.userId() == null) {
            throw new BusinessException("H5_ACTOR_CONTEXT_REQUIRED", "需要 H5 用户上下文");
        }
        return actor;
    }

    /**
     * @Author lxy
     * @Description //获取当前已认证的操作者，userId 为空时抛出业务异常
     * @Date 2026/04/18 09:30
     * @Param []
     * @return FoundationActorResolver.Actor 已认证的操作者
     */
    private FoundationActorResolver.Actor requireAuthenticatedActor() {
        FoundationActorResolver.Actor actor = foundationActorResolver.resolveActor();
        if (actor.userId() == null) {
            throw new BusinessException("AUTH_CONTEXT_REQUIRED", "需要已认证的操作者上下文");
        }
        return actor;
    }

    /**
     * 要求 Web 端认证用户
     */
    private FoundationActorResolver.Actor requireWebActor() {
        FoundationActorResolver.Actor actor = requireAuthenticatedActor();
        String clientType = AuthenticatedUserContextHolder.getOptional()
                .map(AuthenticatedUser::clientType)
                .orElse(null);
        if (!"WEB".equals(clientType)) {
            throw new BusinessException("AUTH_CLIENT_TYPE_FORBIDDEN", "仅 Web 端可执行此操作");
        }
        return actor;
    }

    /**
     * @Author lxy
     * @Description //更新工单的状态、处理人及完成/关闭时间等字段
     * @Date 2026/04/18 09:30
     * @Param [workOrderId 工单ID, status 目标状态, assigneeUserId 新处理人ID, assigneeName 新处理人姓名, completedAt 完成时间, closedAt 关闭时间, closeReason 关闭原因]
     * @return void
     */
    private void updateWorkOrder(Long workOrderId, String status, Long assigneeUserId, String assigneeName, LocalDateTime completedAt, LocalDateTime closedAt, String closeReason) {
        jdbcTemplate.update(
                "UPDATE biz_work_order SET status = ?, assignee_user_id = ?, assignee_name = ?, completed_at = ?, closed_at = ?, close_reason = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?",
                status,
                assigneeUserId,
                assigneeName,
                toTimestamp(completedAt),
                toTimestamp(closedAt),
                closeReason,
                workOrderId);
    }

    /**
     * @Author lxy
     * @Description //更新事件状态，要求当前状态匹配预期状态，更新后同步告警工作流状态
     * @Date 2026/04/18 09:30
     * @Param [eventId 事件ID, targetStatus 目标状态, expectedFromStatus 期望的原状态, code 异常编码, message 异常消息]
     * @return void
     */
    private void updateEventStatus(Long eventId, String targetStatus, String expectedFromStatus, String code, String message) {
        // 关闭时自动归档
        String archiveSet = "CLOSED".equals(targetStatus) ? ", archived = 1, archived_at = CURRENT_TIMESTAMP" : "";
        int updatedRows = jdbcTemplate.update(
                "UPDATE biz_event SET status = ?, updated_at = CURRENT_TIMESTAMP" + archiveSet + " WHERE id = ? AND status = ?",
                targetStatus,
                eventId,
                expectedFromStatus);
        if (updatedRows == 0) {
            throw new BusinessException(code, message);
        }
        if ("WAITING_DISPATCH".equals(targetStatus) || "WAITING_LEADER_REVIEW".equals(targetStatus) || "DISPATCHED_TO_WORK_ORDER".equals(targetStatus) || "CLOSED".equals(targetStatus)) {
            alarmWorkflowStatusSyncService.syncWorkflowStatus(eventId, targetStatus);
        }
    }

    /**
     * @Author lxy
     * @Description //插入事件操作记录，记录事件状态流转和操作人信息
     * @Date 2026/04/18 09:30
     * @Param [eventId 事件ID, fromStatus 原状态, toStatus 目标状态, actionType 操作类型, actor 操作者, remark 备注]
     * @return void
     */
    private void insertEventRecord(Long eventId, String fromStatus, String toStatus, String actionType, FoundationActorResolver.Actor actor, String remark) {
        jdbcTemplate.update(
                "INSERT INTO biz_event_record (event_id, from_status, to_status, action_type, operator_user_id, operator_name, remark, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                eventId,
                fromStatus,
                toStatus,
                actionType,
                actor.userId(),
                actor.name(),
                remark);
    }

    /**
     * @Author lxy
     * @Description //插入流程操作记录，记录节点处理结果和操作人信息，返回新记录ID
     * @Date 2026/04/18 09:30
     * @Param [processInstanceId 流程实例ID, nodeId 节点ID（可为null）, actionType 操作类型, actionResult 操作结果, remark 备注, actor 操作者, subjectType 关联主体类型, subjectId 关联主体ID]
     * @return Long 新插入记录的ID
     */
    private Long insertProcessActionRecord(Long processInstanceId, Long nodeId, String actionType, String actionResult, String remark, FoundationActorResolver.Actor actor, String subjectType, Long subjectId) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO biz_process_action_record (process_instance_id, process_instance_node_id, action_type, action_result, operator_user_id, operator_name, remark, subject_type, subject_id, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setObject(1, processInstanceId);
            ps.setObject(2, nodeId);
            ps.setString(3, actionType);
            ps.setString(4, actionResult);
            ps.setObject(5, actor.userId());
            ps.setString(6, actor.name());
            ps.setString(7, remark);
            ps.setString(8, subjectType);
            ps.setObject(9, subjectId);
            return ps;
        }, keyHolder);
        return extractGeneratedId(keyHolder);
    }

    private void saveAttachments(Long actionRecordId, List<HandleAttachment> attachments, FoundationActorResolver.Actor actor) {
        if (attachments == null || attachments.isEmpty()) {
            return;
        }
        for (HandleAttachment attachment : attachments) {
            jdbcTemplate.update(
                    "INSERT INTO biz_media_file (business_type, business_id, file_name, file_url, file_type, mime_type, status, uploader_user_id, uploader_name, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, 'ACTIVE', ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                    "ACTION_RECORD",
                    actionRecordId,
                    attachment.fileName(),
                    attachment.fileUrl(),
                    StringUtils.hasText(attachment.fileType()) ? attachment.fileType() : "REFERENCE",
                    StringUtils.hasText(attachment.mimeType()) ? attachment.mimeType() : null,
                    actor.userId(),
                    actor.name());
        }
    }

    private int countWorkOrders(String sql, Object... args) {
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, args);
        return count == null ? 0 : count;
    }

    private Long extractGeneratedId(KeyHolder keyHolder) {
        Map<String, Object> keys = keyHolder.getKeys();
        if (keys != null && keys.containsKey("ID")) {
            return ((Number) keys.get("ID")).longValue();
        }
        Number key = keyHolder.getKey();
        if (key != null) {
            return key.longValue();
        }
        throw new BusinessException("PROCESS_INSTANCE_CREATE_FAILED", "创建流程实例失败");
    }

    private LocalDateTime getNullableTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    private Timestamp toTimestamp(LocalDateTime value) {
        return value == null ? null : Timestamp.valueOf(value);
    }

    private Long getNullableLong(ResultSet rs, String column) throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }

    private boolean isDuplicateWorkOrder(DataIntegrityViolationException exception) {
        Throwable cause = exception;
        while (cause != null) {
            if (cause instanceof SQLException sqlException) {
                String message = sqlException.getMessage();
                if (containsWorkOrderConstraint(message)) {
                    return true;
                }
            }
            cause = cause.getCause();
        }
        return containsWorkOrderConstraint(exception.getMessage());
    }

    private boolean containsWorkOrderConstraint(String message) {
        if (message == null) {
            return false;
        }
        String normalized = message.toLowerCase();
        return normalized.contains("source_event_id") || normalized.contains("biz_work_order") || normalized.contains("unique");
    }

    private List<H5ActionRecordVo> listH5ActionRecords(Long processInstanceId) {
        if (processInstanceId == null) {
            return List.of();
        }
        return jdbcTemplate.query(
                "SELECT r.id AS record_id, r.action_type, r.action_result, r.remark, r.created_at, r.operator_name, "
                        + "r.subject_type, r.subject_id, "
                        + "n.node_order, "
                        + "CASE "
                        + "  WHEN r.subject_type = 'MERCHANT' THEN m.merchant_name "
                        + "  WHEN r.subject_type = 'VENDOR' THEN v.vendor_name "
                        + "END AS subject_name, "
                        + "mf.attachments "
                        + "FROM biz_process_action_record r "
                        + "LEFT JOIN biz_process_instance_node n ON n.id = r.process_instance_node_id "
                        + "LEFT JOIN biz_merchant m ON m.id = r.subject_id AND r.subject_type = 'MERCHANT' "
                        + "LEFT JOIN biz_mobile_vendor v ON v.id = r.subject_id AND r.subject_type = 'VENDOR' "
                        + "LEFT JOIN ("
                        + "  SELECT business_id, "
                        + "    CONCAT('[', GROUP_CONCAT(JSON_OBJECT('fileName', file_name, 'fileUrl', file_url, 'fileType', file_type)), ']') AS attachments "
                        + "  FROM biz_media_file WHERE business_type = 'ACTION_RECORD' AND status = 'ACTIVE' GROUP BY business_id"
                        + ") mf ON mf.business_id = r.id "
                        + "WHERE r.process_instance_id = ? "
                        + "ORDER BY r.created_at ASC, r.id ASC",
                (rs, rowNum) -> new H5ActionRecordVo(
                        rs.getString("action_type"),
                        rs.getString("action_result"),
                        rs.getString("remark"),
                        getNullableTime(rs.getTimestamp("created_at")),
                        rs.getString("operator_name"),
                        (Integer) rs.getObject("node_order"),
                        rs.getString("subject_type"),
                        getNullableLong(rs, "subject_id"),
                        rs.getString("subject_name"),
                        rs.getString("attachments")),
                processInstanceId);
    }

    @Override
    public List<Map<String, Object>> getAttachments(Long workOrderId) {
        return jdbcTemplate.query(
            "SELECT mf.id, mf.file_name, mf.file_url, mf.file_type, mf.mime_type, mf.uploader_name, mf.created_at " +
            "FROM biz_media_file mf " +
            "JOIN biz_process_action_record ar ON ar.id = mf.business_id AND mf.business_type = 'ACTION_RECORD' " +
            "JOIN biz_process_instance pi ON pi.id = ar.process_instance_id " +
            "JOIN biz_work_order wo ON wo.process_instance_id = pi.id " +
            "WHERE wo.id = ? AND mf.status = 'ACTIVE' " +
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
            workOrderId);
    }

    /**
     * 确认关闭工单（Web端）— 工单已完成，事件关闭
     */
    @Override
    @Transactional
    public WorkOrderEntity confirmClose(Long workOrderId, String remark) {
        FoundationActorResolver.Actor actor = requireWebActor();
        WorkOrderEntity workOrder = getWorkOrder(workOrderId);

        if (!"WAITING_CLOSE_CONFIRM".equalsIgnoreCase(workOrder.getStatus())) {
            throw new BusinessException("VALIDATION_ERROR", "工单不处于待关闭确认状态");
        }

        LocalDateTime now = LocalDateTime.now();
        jdbcTemplate.update(
                "UPDATE biz_work_order SET status = 'COMPLETED', completed_at = ?, closed_at = ?, close_reason = ?, updated_at = ? WHERE id = ?",
                Timestamp.valueOf(now), Timestamp.valueOf(now), remark, Timestamp.valueOf(now), workOrderId);

        // 更新事件状态（带状态检查，确保不会覆盖已关闭的事件），关闭即自动归档
        int updated = jdbcTemplate.update(
                "UPDATE biz_event SET status = 'CLOSED', archived = 1, archived_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP WHERE id = ? AND status != 'CLOSED'",
                workOrder.getSourceEventId());
        if (updated == 0) {
            log.warn("事件{}已处于CLOSED状态，跳过更新", workOrder.getSourceEventId());
        }
        alarmWorkflowStatusSyncService.syncWorkflowStatus(workOrder.getSourceEventId(), "CLOSED");
        insertEventRecord(workOrder.getSourceEventId(), null, "CLOSED", "CONFIRM_CLOSE", actor, remark);
        insertProcessActionRecord(workOrder.getProcessInstanceId(), null, "CONFIRM_CLOSE", "APPROVED", remark, actor, null, null);

        return getWorkOrder(workOrderId);
    }

    /**
     * 驳回关闭（Web端）— 工单退回处理中状态，保留原受派人
     */
    @Override
    @Transactional
    public WorkOrderEntity rejectClose(Long workOrderId, String remark) {
        FoundationActorResolver.Actor actor = requireWebActor();
        WorkOrderEntity workOrder = getWorkOrder(workOrderId);

        if (!"WAITING_CLOSE_CONFIRM".equalsIgnoreCase(workOrder.getStatus())) {
            throw new BusinessException("VALIDATION_ERROR", "工单不处于待关闭确认状态");
        }

        // 直接退回处理中，保留原受派人
        jdbcTemplate.update(
                "UPDATE biz_work_order SET status = 'PROCESSING', updated_at = CURRENT_TIMESTAMP WHERE id = ?",
                workOrderId);

        // 重置流程实例状态为运行中
        jdbcTemplate.update(
                "UPDATE biz_process_instance SET status = 'RUNNING', finished_at = NULL, updated_at = CURRENT_TIMESTAMP WHERE id = ?",
                workOrder.getProcessInstanceId());

        insertEventRecord(workOrder.getSourceEventId(), null, "DISPATCHED_TO_WORK_ORDER", "REJECT_CLOSE", actor, remark);
        insertProcessActionRecord(workOrder.getProcessInstanceId(), null, "REJECT_CLOSE", "REJECTED", remark, actor, null, null);

        return getWorkOrder(workOrderId);
    }

    private record DispatchAssignee(Long id, String name) {
    }
}
