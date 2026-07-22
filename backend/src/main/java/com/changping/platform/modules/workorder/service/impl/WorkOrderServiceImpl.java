package com.changping.platform.modules.workorder.service.impl;

import com.changping.platform.common.exception.BusinessException;
import com.changping.platform.common.security.FoundationActorResolver;
import com.changping.platform.modules.event.entity.EventEntity;
import com.changping.platform.modules.event.service.AlarmWorkflowStatusSyncService;
import com.changping.platform.modules.process.entity.ProcessTemplateEntity;
import com.changping.platform.modules.process.entity.ProcessTemplateNodeEntity;
import com.changping.platform.modules.process.service.ProcessTemplateService;
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
import org.springframework.util.StringUtils;

/**
 * @Author tangxinglin
 * @Description //工单服务实现类，基于 JdbcTemplate 实现工单派发、处理、查询及删除的完整业务逻辑
 * @Date 2026/04/18 09:30
 */
@Service
public class WorkOrderServiceImpl implements WorkOrderService {

    private static final String WORK_ORDER_BUSINESS_TYPE = "EVENT_WORK_ORDER";
    private static final String WORK_ORDER_MEDIA_TYPE = "WORK_ORDER";
    private static final String PROCESS_STATUS_RUNNING = "RUNNING";
    private static final String PROCESS_STATUS_APPROVED = "APPROVED";
    private static final String NODE_STATUS_PENDING = "PENDING";
    private static final String NODE_STATUS_WAITING = "WAITING";
    private static final String NODE_STATUS_APPROVED = "APPROVED";
    private static final String NODE_STATUS_REJECTED = "REJECTED";

    private final JdbcTemplate jdbcTemplate;
    private final FoundationActorResolver foundationActorResolver;
    private final AlarmWorkflowStatusSyncService alarmWorkflowStatusSyncService;
    private final ProcessTemplateService processTemplateService;

    /**
     * @Author tangxinglin
     * @Description //构造函数，注入数据库模板、操作者解析器、告警同步服务及流程模板服务
     * @Date 2026/04/18 09:30
     * @Param [jdbcTemplate 数据库操作模板, foundationActorResolver 操作者解析器, alarmWorkflowStatusSyncService 告警工作流状态同步服务, processTemplateService 流程模板服务]
     * @return
     */
    public WorkOrderServiceImpl(
            JdbcTemplate jdbcTemplate,
            FoundationActorResolver foundationActorResolver,
            AlarmWorkflowStatusSyncService alarmWorkflowStatusSyncService,
            ProcessTemplateService processTemplateService) {
        this.jdbcTemplate = jdbcTemplate;
        this.foundationActorResolver = foundationActorResolver;
        this.alarmWorkflowStatusSyncService = alarmWorkflowStatusSyncService;
        this.processTemplateService = processTemplateService;
    }

    /**
     * @Author tangxinglin
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
        if (!"WAITING_DISPATCH".equals(event.getStatus())) {
            throw new BusinessException("WORK_ORDER_DISPATCH_STATUS_INVALID", "事件必须处于待派发状态才能派发");
        }

        ProcessTemplateEntity template = processTemplateService.getTemplate(request.processTemplateId());
        validateDispatchTemplate(template, event);

        Long processInstanceId = createProcessInstance(event, template, actor);
        ProcessNodeState currentNode = getCurrentPendingNode(processInstanceId);
        String workOrderNo = "WO-" + eventId + "-" + System.currentTimeMillis();
        try {
            jdbcTemplate.update(
                    "INSERT INTO biz_work_order (work_order_no, source_event_id, process_instance_id, status, assignee_user_id, assignee_name, dispatcher_user_id, dispatcher_name, created_at, updated_at) VALUES (?, ?, ?, 'PROCESSING', ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                    workOrderNo,
                    eventId,
                    processInstanceId,
                    currentNode.assigneeUserId(),
                    currentNode.assigneeName(),
                    actor.userId(),
                    actor.name());
        } catch (DataIntegrityViolationException exception) {
            if (isDuplicateWorkOrder(exception)) {
                throw new BusinessException("WORK_ORDER_ALREADY_EXISTS", "事件已派发为工单");
            }
            throw exception;
        }

        updateEventStatus(eventId, "DISPATCHED_TO_WORK_ORDER", "WAITING_DISPATCH", "WORK_ORDER_DISPATCH_STATUS_INVALID", "事件必须处于待派发状态才能派发");
        insertEventRecord(eventId, "WAITING_DISPATCH", "DISPATCHED_TO_WORK_ORDER", "WORK_ORDER_DISPATCH", actor, request.remark());
        insertProcessActionRecord(processInstanceId, null, "WORK_ORDER_DISPATCH", PROCESS_STATUS_RUNNING, request.remark(), actor, null, null);
        return getWorkOrderByEventId(eventId);
    }

    /**
     * @Author tangxinglin
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
        requireH5Participant(workOrder, actor);

        ProcessNodeState currentNode = getCurrentPendingNode(workOrder.getProcessInstanceId());
        requireCurrentNodeAssignee(currentNode, actor);

        String result = request.result().trim();
        if ("REJECTED".equalsIgnoreCase(result) || "RETURNED".equalsIgnoreCase(result)) {
            completeProcessNode(currentNode.id(), NODE_STATUS_REJECTED);
            jdbcTemplate.update(
                    "UPDATE biz_process_instance SET status = 'REJECTED', finished_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP WHERE id = ?",
                    workOrder.getProcessInstanceId());
            updateWorkOrder(workOrderId, "CLOSED", currentNode.assigneeUserId(), currentNode.assigneeName(), null, LocalDateTime.now(), request.remark());
            updateEventStatus(workOrder.getSourceEventId(), "CLOSED", "DISPATCHED_TO_WORK_ORDER", "EVENT_CLOSE_STATUS_INVALID", "事件必须处于已派发状态才能关闭");
            insertEventRecord(workOrder.getSourceEventId(), "DISPATCHED_TO_WORK_ORDER", "CLOSED", "WORK_ORDER_NODE_REJECT", actor, request.remark());
            Long actionRecordId = insertProcessActionRecord(workOrder.getProcessInstanceId(), currentNode.id(), "WORK_ORDER_HANDLE", NODE_STATUS_REJECTED, request.remark(), actor, request.subjectType(), request.subjectId());
            saveAttachments(actionRecordId, request.attachments(), actor);
            return getWorkOrder(workOrderId);
        }

        completeProcessNode(currentNode.id(), NODE_STATUS_APPROVED);
        Long actionRecordId = insertProcessActionRecord(workOrder.getProcessInstanceId(), currentNode.id(), "WORK_ORDER_HANDLE", NODE_STATUS_APPROVED, request.remark(), actor, request.subjectType(), request.subjectId());
        saveAttachments(actionRecordId, request.attachments(), actor);

        ProcessNodeState nextNode = activateNextNode(workOrder.getProcessInstanceId(), currentNode.nodeOrder());
        if (nextNode == null) {
            jdbcTemplate.update(
                    "UPDATE biz_process_instance SET status = 'APPROVED', finished_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP WHERE id = ?",
                    workOrder.getProcessInstanceId());
            updateWorkOrder(workOrderId, "COMPLETED", null, null, LocalDateTime.now(), null, null);
            updateEventStatus(workOrder.getSourceEventId(), "CLOSED", "DISPATCHED_TO_WORK_ORDER", "EVENT_CLOSE_STATUS_INVALID", "事件必须处于已派发状态才能关闭");
            insertEventRecord(workOrder.getSourceEventId(), "DISPATCHED_TO_WORK_ORDER", "CLOSED", "WORK_ORDER_COMPLETE", actor, request.remark());
            insertProcessActionRecord(workOrder.getProcessInstanceId(), null, "WORK_ORDER_COMPLETE", PROCESS_STATUS_APPROVED, request.remark(), actor, null, null);
            return getWorkOrder(workOrderId);
        }

        updateWorkOrder(workOrderId, "PROCESSING", nextNode.assigneeUserId(), nextNode.assigneeName(), null, null, null);
        insertEventRecord(workOrder.getSourceEventId(), "DISPATCHED_TO_WORK_ORDER", "DISPATCHED_TO_WORK_ORDER", "WORK_ORDER_NODE_COMPLETE", actor, request.remark());
        return getWorkOrder(workOrderId);
    }

    @Override
    public List<WebWorkOrderSummary> queryWebWorkOrders() {
        return jdbcTemplate.query(
                "SELECT wo.id, wo.work_order_no, wo.source_event_id, wo.status, wo.assignee_name, wo.dispatcher_name, wo.created_at, wo.updated_at, "
                        + "e.event_code, e.title, e.area_id, e.area_name "
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
                        rs.getString("area_name")));
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
                "SELECT wo.id, wo.work_order_no, wo.source_event_id, wo.status, wo.assignee_name, wo.dispatcher_name, wo.created_at, wo.updated_at, "
                        + "e.event_code, e.title, e.area_id, e.area_name "
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
                        rs.getString("area_name")),
                pageParams.toArray());

        return new PagedWorkOrders(items, total != null ? total : 0, safePage, safeSize);
    }

    @Override
    public WebWorkOrderDetail getWebWorkOrderDetail(Long workOrderId) {
        List<WebWorkOrderDetail> details = jdbcTemplate.query(
                "SELECT wo.id, wo.work_order_no, wo.source_event_id, wo.process_instance_id, wo.status, wo.assignee_name, wo.dispatcher_name, wo.close_reason, wo.created_at, wo.updated_at, wo.completed_at, wo.closed_at, "
                        + "e.event_code, e.title, e.event_type, e.source_type, e.status AS event_status, e.description "
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
                        List.of()),
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
                listWebWorkOrderFlowRecords(detail.processInstanceId()));
    }

    @Override
    public List<H5WorkOrderListItem> queryH5WorkOrders() {
        FoundationActorResolver.Actor actor = requireH5Actor();
        Long actorUserId = actor.userId();
        return jdbcTemplate.query(
                "SELECT wo.id, wo.work_order_no, wo.status, wo.assignee_user_id, wo.assignee_name, "
                        + "wo.dispatcher_name, wo.created_at, wo.updated_at, "
                        + "e.title AS event_title, e.area_name, "
                        + "(SELECT node.node_name FROM biz_process_instance_node node "
                        + "  WHERE node.process_instance_id = wo.process_instance_id "
                        + "    AND node.is_current = 1 AND node.status = 'PENDING' "
                        + "  ORDER BY node.node_order ASC LIMIT 1) AS current_node_name, "
                        + "(SELECT COUNT(*) FROM biz_process_instance_node node "
                        + "  WHERE node.process_instance_id = wo.process_instance_id "
                        + "    AND node.is_current = 1 AND node.status = 'PENDING' "
                        + "    AND node.assignee_user_id = ?) AS is_handler_count "
                        + "FROM biz_work_order wo "
                        + "LEFT JOIN biz_event e ON e.id = wo.source_event_id "
                        + "WHERE EXISTS (SELECT 1 FROM biz_process_instance_node node "
                        + "  WHERE node.process_instance_id = wo.process_instance_id "
                        + "    AND node.assignee_user_id = ?) "
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
                        rs.getString("current_node_name"),
                        rs.getInt("is_handler_count") > 0,
                        rs.getString("area_name")),
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
        boolean isCurrentHandler = isCurrentPendingHandler(processInstanceId, actor.userId());

        List<H5ProcessNodeVo> processNodes = listH5ProcessNodes(processInstanceId);
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
                processNodes,
                actionRecords);
    }

    @Override
    public H5WorkbenchSummary getH5Workbench() {
        FoundationActorResolver.Actor actor = requireH5Actor();
        Long uid = actor.userId();
        // totalCount: all work orders where user is any node participant
        int totalCount = countWorkOrders(
                "SELECT COUNT(*) FROM biz_work_order wo WHERE EXISTS "
                        + "(SELECT 1 FROM biz_process_instance_node node "
                        + " WHERE node.process_instance_id = wo.process_instance_id AND node.assignee_user_id = ?)",
                uid);
        // waitingAcceptCount: PROCESSING orders where user is the current PENDING node assignee (needs action now)
        int waitingAcceptCount = countWorkOrders(
                "SELECT COUNT(*) FROM biz_work_order wo WHERE wo.status = 'PROCESSING' AND EXISTS "
                        + "(SELECT 1 FROM biz_process_instance_node node "
                        + " WHERE node.process_instance_id = wo.process_instance_id "
                        + "   AND node.is_current = 1 AND node.status = 'PENDING' AND node.assignee_user_id = ?)",
                uid);
        // pendingCloseCount: user is a participant but NOT the current pending assignee, flow still open (PROCESSING/COMPLETED)
        // meaning: user has already acted on their node but the work order is not yet fully closed
        int pendingCloseCount = countWorkOrders(
                "SELECT COUNT(*) FROM biz_work_order wo WHERE wo.status IN ('PROCESSING', 'COMPLETED') "
                        + "AND EXISTS (SELECT 1 FROM biz_process_instance_node node "
                        + " WHERE node.process_instance_id = wo.process_instance_id AND node.assignee_user_id = ?) "
                        + "AND NOT EXISTS (SELECT 1 FROM biz_process_instance_node node "
                        + " WHERE node.process_instance_id = wo.process_instance_id "
                        + "   AND node.is_current = 1 AND node.status = 'PENDING' AND node.assignee_user_id = ?)",
                uid, uid);
        // closedCount: fully closed (CLOSED or TIMEOUT) orders where user participated
        int closedCount = countWorkOrders(
                "SELECT COUNT(*) FROM biz_work_order wo WHERE wo.status IN ('CLOSED', 'TIMEOUT') AND EXISTS "
                        + "(SELECT 1 FROM biz_process_instance_node node "
                        + " WHERE node.process_instance_id = wo.process_instance_id AND node.assignee_user_id = ?)",
                uid);
        return new H5WorkbenchSummary(totalCount, waitingAcceptCount, pendingCloseCount, closedCount);
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

    private Long createProcessInstance(EventEntity event, ProcessTemplateEntity template, FoundationActorResolver.Actor actor) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO biz_process_instance (process_no, template_id, template_version, business_type, business_id, status, current_node_order, started_at, finished_at, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                    Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, "PI-WO-" + event.getId() + "-" + UUID.randomUUID());
            statement.setLong(2, template.getId());
            statement.setInt(3, template.getVersion());
            statement.setString(4, WORK_ORDER_BUSINESS_TYPE);
            statement.setLong(5, event.getId());
            statement.setString(6, PROCESS_STATUS_RUNNING);
            statement.setInt(7, 1);
            return statement;
        }, keyHolder);
        Long processInstanceId = extractGeneratedId(keyHolder);

        for (ProcessTemplateNodeEntity node : template.getNodes()) {
            jdbcTemplate.update(
                    "INSERT INTO biz_process_instance_node (process_instance_id, template_node_id, node_key, node_name, node_order, status, cycle_no, is_current, node_mode, assignee_user_id, assignee_name, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, 1, 1, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                    processInstanceId,
                    node.getId(),
                    node.getNodeKey(),
                    node.getNodeName(),
                    node.getNodeOrder(),
                    node.getNodeOrder() == 1 ? NODE_STATUS_PENDING : NODE_STATUS_WAITING,
                    "SEQUENTIAL",
                    node.getAssigneeUserId(),
                    node.getAssigneeName());
        }
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
                        + "(SELECT CONCAT('[', GROUP_CONCAT(JSON_OBJECT('fileName', mf.file_name, 'fileUrl', mf.file_url, 'fileType', mf.file_type)), ']') "
                        + " FROM biz_media_file mf WHERE mf.business_type = 'ACTION_RECORD' AND mf.business_id = record.id AND mf.status = 'ACTIVE') AS attachments "
                        + "FROM biz_process_action_record record "
                        + "LEFT JOIN biz_process_instance_node node ON node.id = record.process_instance_node_id "
                        + "LEFT JOIN biz_merchant m ON m.id = record.subject_id AND record.subject_type = 'MERCHANT' "
                        + "LEFT JOIN biz_mobile_vendor v ON v.id = record.subject_id AND record.subject_type = 'VENDOR' "
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

    private ProcessNodeState activateNextNode(Long processInstanceId, Integer currentOrder) {
        Integer nextOrder = jdbcTemplate.query(
                "SELECT MIN(node_order) FROM biz_process_instance_node WHERE process_instance_id = ? AND is_current = 1 AND node_order > ?",
                rs -> rs.next() ? (Integer) rs.getObject(1) : null,
                processInstanceId,
                currentOrder);
        if (nextOrder == null) {
            return null;
        }
        jdbcTemplate.update(
                "UPDATE biz_process_instance_node SET status = 'PENDING', updated_at = CURRENT_TIMESTAMP WHERE process_instance_id = ? AND is_current = 1 AND node_order = ? AND status = 'WAITING'",
                processInstanceId,
                nextOrder);
        jdbcTemplate.update(
                "UPDATE biz_process_instance SET current_node_order = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?",
                nextOrder,
                processInstanceId);
        return getCurrentPendingNode(processInstanceId);
    }

    /**
     * @Author tangxinglin
     * @Description //校验派发模板是否启用、事件类型是否匹配及模板节点是否存在
     * @Date 2026/04/18 09:30
     * @Param [template 流程模板实体, event 事件实体]
     * @return void
     */
    private void validateDispatchTemplate(ProcessTemplateEntity template, EventEntity event) {
        if (!Boolean.TRUE.equals(template.getEnabled())) {
            throw new BusinessException("PROCESS_TEMPLATE_NOT_ENABLED", "所选模板未启用");
        }
        String templateEventType = template.getEventType();
        String eventType = event.getEventType();
        if (StringUtils.hasText(templateEventType)
                && !"DEFAULT".equalsIgnoreCase(templateEventType)
                && StringUtils.hasText(eventType)
                && !eventType.equals(templateEventType)) {
            throw new BusinessException("PROCESS_TEMPLATE_EVENT_TYPE_MISMATCH", "所选模板与事件类型不匹配");
        }
        if (template.getNodes() == null || template.getNodes().isEmpty()) {
            throw new BusinessException("PROCESS_TEMPLATE_INVALID", "所选模板没有节点");
        }
    }

    /**
     * @Author tangxinglin
     * @Description //校验派发请求，流程模板ID不能为空
     * @Date 2026/04/18 09:30
     * @Param [request 派发请求]
     * @return void
     */
    private void validateDispatchRequest(DispatchRequest request) {
        if (request == null || request.processTemplateId() == null) {
            throw new BusinessException("VALIDATION_ERROR", "流程模板 ID 不能为空");
        }
    }

    /**
     * @Author tangxinglin
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
     * @Author tangxinglin
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
     * @Author tangxinglin
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
     * @Author tangxinglin
     * @Description //根据事件ID查询对应的工单实体
     * @Date 2026/04/18 09:30
     * @Param [eventId 事件ID]
     * @return WorkOrderEntity 工单实体
     */
    private WorkOrderEntity getWorkOrderByEventId(Long eventId) {
        return jdbcTemplate.queryForObject(workOrderSelectSql() + " WHERE source_event_id = ?", (rs, rowNum) -> mapWorkOrder(rs), eventId);
    }

    /**
     * @Author tangxinglin
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
     * @Author tangxinglin
     * @Description //返回工单查询的 SELECT 基础语句（不含 WHERE 条件）
     * @Date 2026/04/18 09:30
     * @Param []
     * @return String 工单查询 SQL 片段
     */
    private String workOrderSelectSql() {
        return "SELECT id, work_order_no, source_event_id, process_instance_id, status, assignee_user_id, assignee_name, dispatcher_user_id, dispatcher_name, completed_at, closed_at, close_reason, created_at, updated_at FROM biz_work_order";
    }

    /**
     * @Author tangxinglin
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
     * @Author tangxinglin
     * @Description //查询流程实例中当前处于 PENDING 状态的节点（按节点顺序取最小的）
     * @Date 2026/04/18 09:30
     * @Param [processInstanceId 流程实例ID]
     * @return ProcessNodeState 当前待处理节点状态
     */
    private ProcessNodeState getCurrentPendingNode(Long processInstanceId) {
        List<ProcessNodeState> nodes = jdbcTemplate.query(
                "SELECT id, node_order, assignee_user_id, assignee_name FROM biz_process_instance_node WHERE process_instance_id = ? AND is_current = 1 AND status = 'PENDING' ORDER BY node_order ASC LIMIT 1",
                (rs, rowNum) -> new ProcessNodeState(rs.getLong("id"), rs.getInt("node_order"), getNullableLong(rs, "assignee_user_id"), rs.getString("assignee_name")),
                processInstanceId);
        if (nodes.isEmpty()) {
            throw new BusinessException("PROCESS_NODE_NOT_FOUND", "当前流程节点未找到");
        }
        return nodes.get(0);
    }

    /**
     * @Author tangxinglin
     * @Description //将指定流程节点标记为已完成，设置处理状态（APPROVED/REJECTED）
     * @Date 2026/04/18 09:30
     * @Param [nodeId 节点ID, status 目标状态]
     * @return void
     */
    private void completeProcessNode(Long nodeId, String status) {
        int updatedRows = jdbcTemplate.update(
                "UPDATE biz_process_instance_node SET status = ?, handled_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP WHERE id = ? AND status = 'PENDING'",
                status,
                nodeId);
        if (updatedRows == 0) {
            throw new BusinessException("PROCESS_NODE_STATUS_INVALID", "当前流程节点非待处理状态");
        }
    }

    /**
     * @Author tangxinglin
     * @Description //校验当前节点的处理人必须是当前认证用户，否则抛出业务异常
     * @Date 2026/04/18 09:30
     * @Param [currentNode 当前节点状态, actor 当前认证用户]
     * @return void
     */
    private void requireCurrentNodeAssignee(ProcessNodeState currentNode, FoundationActorResolver.Actor actor) {
        if (currentNode.assigneeUserId() == null || !currentNode.assigneeUserId().equals(actor.userId())) {
            throw new BusinessException("WORK_ORDER_NOT_FOUND", "工单未找到");
        }
    }

    /**
     * @Author tangxinglin
     * @Description //校验当前 H5 用户是工单流程的参与者之一，否则抛出业务异常
     * @Date 2026/04/18 09:30
     * @Param [workOrder 工单实体, actor 当前 H5 用户]
     * @return void
     */
    private void requireH5Participant(WorkOrderEntity workOrder, FoundationActorResolver.Actor actor) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM biz_process_instance_node WHERE process_instance_id = ? AND assignee_user_id = ?",
                Integer.class,
                workOrder.getProcessInstanceId(),
                actor.userId());
        if (count == null || count == 0) {
            throw new BusinessException("WORK_ORDER_NOT_FOUND", "工单未找到");
        }
    }

    /**
     * @Author tangxinglin
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
     * @Author tangxinglin
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
     * @Author tangxinglin
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
     * @Author tangxinglin
     * @Description //更新事件状态，要求当前状态匹配预期状态，更新后同步告警工作流状态
     * @Date 2026/04/18 09:30
     * @Param [eventId 事件ID, targetStatus 目标状态, expectedFromStatus 期望的原状态, code 异常编码, message 异常消息]
     * @return void
     */
    private void updateEventStatus(Long eventId, String targetStatus, String expectedFromStatus, String code, String message) {
        int updatedRows = jdbcTemplate.update(
                "UPDATE biz_event SET status = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ? AND status = ?",
                targetStatus,
                eventId,
                expectedFromStatus);
        if (updatedRows == 0) {
            throw new BusinessException(code, message);
        }
        if ("WAITING_DISPATCH".equals(targetStatus) || "DISPATCHED_TO_WORK_ORDER".equals(targetStatus) || "CLOSED".equals(targetStatus)) {
            alarmWorkflowStatusSyncService.syncWorkflowStatus(eventId, targetStatus);
        }
    }

    /**
     * @Author tangxinglin
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
     * @Author tangxinglin
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

    private boolean isCurrentPendingHandler(Long processInstanceId, Long actorUserId) {
        if (processInstanceId == null || actorUserId == null) {
            return false;
        }
        List<Long> assignees = jdbcTemplate.query(
                "SELECT assignee_user_id FROM biz_process_instance_node "
                        + "WHERE process_instance_id = ? AND is_current = 1 AND status = 'PENDING' "
                        + "ORDER BY node_order ASC LIMIT 1",
                (rs, rowNum) -> getNullableLong(rs, "assignee_user_id"),
                processInstanceId);
        return !assignees.isEmpty() && actorUserId.equals(assignees.get(0));
    }

    private List<H5ProcessNodeVo> listH5ProcessNodes(Long processInstanceId) {
        if (processInstanceId == null) {
            return List.of();
        }
        return jdbcTemplate.query(
                "SELECT node_order, node_name, assignee_user_id, assignee_name, status "
                        + "FROM biz_process_instance_node "
                        + "WHERE process_instance_id = ? "
                        + "ORDER BY node_order ASC",
                (rs, rowNum) -> new H5ProcessNodeVo(
                        rs.getInt("node_order"),
                        rs.getString("node_name"),
                        getNullableLong(rs, "assignee_user_id"),
                        rs.getString("assignee_name"),
                        rs.getString("status")),
                processInstanceId);
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
                        + "(SELECT CONCAT('[', GROUP_CONCAT(JSON_OBJECT('fileName', mf.file_name, 'fileUrl', mf.file_url, 'fileType', mf.file_type)), ']') "
                        + " FROM biz_media_file mf WHERE mf.business_type = 'ACTION_RECORD' AND mf.business_id = r.id AND mf.status = 'ACTIVE') AS attachments "
                        + "FROM biz_process_action_record r "
                        + "LEFT JOIN biz_process_instance_node n ON n.id = r.process_instance_node_id "
                        + "LEFT JOIN biz_merchant m ON m.id = r.subject_id AND r.subject_type = 'MERCHANT' "
                        + "LEFT JOIN biz_mobile_vendor v ON v.id = r.subject_id AND r.subject_type = 'VENDOR' "
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

    private record ProcessNodeState(Long id, Integer nodeOrder, Long assigneeUserId, String assigneeName) {
    }
}
