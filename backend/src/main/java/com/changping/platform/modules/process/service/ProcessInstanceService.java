package com.changping.platform.modules.process.service;

import com.changping.platform.common.exception.BusinessException;
import com.changping.platform.common.security.FoundationActorResolver;
import com.changping.platform.modules.audit.entity.AuditRecordEntity;
import com.changping.platform.modules.auth.security.PermissionCodes;
import com.changping.platform.modules.auth.security.PermissionGuard;
import com.changping.platform.modules.event.entity.EventEntity;
import com.changping.platform.modules.event.service.AlarmWorkflowStatusSyncService;
import com.changping.platform.modules.process.entity.ProcessActionRecordEntity;
import com.changping.platform.modules.process.entity.ProcessInstanceEntity;
import com.changping.platform.modules.process.entity.ProcessInstanceNodeEntity;
import com.changping.platform.modules.process.entity.ProcessTemplateEntity;
import com.changping.platform.modules.process.entity.ProcessTemplateNodeEntity;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

/**
 * @Author tangxinglin
 * @Description //流程实例服务，负责审核流程实例的创建、节点推进、审批/驳回及事件状态联动，当前审核流程已废弃并统一抛出业务异常
 * @Date 2026/04/18 10:00
 */
@Service
public class ProcessInstanceService {

    private static final Logger log = LoggerFactory.getLogger(ProcessInstanceService.class);

    private static final String BUSINESS_TYPE_EVENT_AUDIT = "EVENT_AUDIT";

    private final JdbcTemplate jdbcTemplate;
    private final ProcessTemplateService processTemplateService;
    private final FoundationActorResolver foundationActorResolver;
    private final PermissionGuard permissionGuard;
    private final AlarmWorkflowStatusSyncService alarmWorkflowStatusSyncService;

    /**
     * @Author tangxinglin
     * @Description //构造器，注入JDBC模板、流程模板服务、操作者解析器、权限守卫和告警状态同步服务
     * @Date 2026/04/18 10:00
     * @Param [jdbcTemplate JDBC模板, processTemplateService 流程模板服务, foundationActorResolver 操作者解析器, permissionGuard 权限守卫, alarmWorkflowStatusSyncService 告警工作流状态同步服务]
     * @return void
     */
    public ProcessInstanceService(
            JdbcTemplate jdbcTemplate,
            ProcessTemplateService processTemplateService,
            FoundationActorResolver foundationActorResolver,
            PermissionGuard permissionGuard,
            AlarmWorkflowStatusSyncService alarmWorkflowStatusSyncService) {
        this.jdbcTemplate = jdbcTemplate;
        this.processTemplateService = processTemplateService;
        this.foundationActorResolver = foundationActorResolver;
        this.permissionGuard = permissionGuard;
        this.alarmWorkflowStatusSyncService = alarmWorkflowStatusSyncService;
    }

    /**
     * @Author tangxinglin
     * @Description //启动事件审核流程（已废弃），当前统一抛出业务异常
     * @Date 2026/04/18 10:00
     * @Param [eventId 事件主键ID, request 启动审核请求对象]
     * @return ProcessInstanceEntity 流程实例
     */
    @Transactional
    public ProcessInstanceEntity startAudit(Long eventId, StartAuditRequest request) {
        throw new BusinessException("AUDIT_FLOW_DEPRECATED", "审核流程已不在当前事件工作流中");
    }

    /**
     * @Author tangxinglin
     * @Description //获取事件审核流程详情（已废弃），当前统一抛出业务异常
     * @Date 2026/04/18 10:00
     * @Param [eventId 事件主键ID]
     * @return ProcessInstanceEntity 流程实例详情
     */
    public ProcessInstanceEntity getAuditDetail(Long eventId) {
        throw new BusinessException("AUDIT_FLOW_DEPRECATED", "审核流程已不在当前事件工作流中");
    }

    /**
     * @Author tangxinglin
     * @Description //审批通过指定流程实例节点（已废弃），当前统一抛出业务异常
     * @Date 2026/04/18 10:00
     * @Param [processInstanceId 流程实例主键ID, request 审批决策请求对象]
     * @return ProcessInstanceEntity 更新后的流程实例
     */
    @Transactional
    public ProcessInstanceEntity approve(Long processInstanceId, AuditDecisionRequest request) {
        throw new BusinessException("AUDIT_FLOW_DEPRECATED", "审核流程已不在当前事件工作流中");
    }

    /**
     * @Author tangxinglin
     * @Description //驳回指定流程实例节点（已废弃），当前统一抛出业务异常
     * @Date 2026/04/18 10:00
     * @Param [processInstanceId 流程实例主键ID, request 审批决策请求对象]
     * @return ProcessInstanceEntity 更新后的流程实例
     */
    @Transactional
    public ProcessInstanceEntity reject(Long processInstanceId, AuditDecisionRequest request) {
        throw new BusinessException("AUDIT_FLOW_DEPRECATED", "审核流程已不在当前事件工作流中");
    }

    /**
     * @Author tangxinglin
     * @Description //根据模板和操作者信息创建新的流程实例，并初始化节点和首条操作记录
     * @Date 2026/04/18 10:00
     * @Param [event 事件实体, template 流程模板实体, actor 当前操作者信息]
     * @return ProcessInstanceEntity 新建的流程实例
     */
    private ProcessInstanceEntity createProcessInstance(EventEntity event, ProcessTemplateEntity template, FoundationActorResolver.Actor actor) {
        ProcessInstanceEntity instance = new ProcessInstanceEntity();
        instance.setProcessNo("PI-" + event.getId() + "-" + UUID.randomUUID());
        instance.setTemplateId(template.getId());
        instance.setTemplateVersion(template.getVersion());
        instance.setBusinessType(BUSINESS_TYPE_EVENT_AUDIT);
        instance.setBusinessId(event.getId());
        instance.setStatus("RUNNING");
        instance.setCurrentNodeOrder(1);

        KeyHolder keyHolder = new GeneratedKeyHolder();
        try {
            jdbcTemplate.update(connection -> {
                PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO biz_process_instance (process_no, template_id, template_version, business_type, business_id, status, current_node_order, started_at, finished_at, created_at, updated_at) "
                                + "VALUES (?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                        Statement.RETURN_GENERATED_KEYS);
                statement.setString(1, instance.getProcessNo());
                statement.setLong(2, instance.getTemplateId());
                statement.setInt(3, instance.getTemplateVersion());
                statement.setString(4, instance.getBusinessType());
                statement.setLong(5, instance.getBusinessId());
                statement.setString(6, instance.getStatus());
                statement.setInt(7, instance.getCurrentNodeOrder());
                return statement;
            }, keyHolder);
        } catch (DataIntegrityViolationException exception) {
            if (isDuplicateBusinessInstance(exception)) {
                throw new BusinessException("AUDIT_ALREADY_STARTED", "该事件已启动审核");
            }
            throw exception;
        }
        instance.setId(extractGeneratedId(keyHolder));

        createNodeInstances(instance.getId(), template.getNodes(), 1);
        insertProcessActionRecord(instance.getId(), null, "AUDIT_START", "RUNNING", null, actor.userId(), actor.name());
        insertAuditRecord(event.getId(), instance.getId(), "START", "IN_AUDIT", null, actor.userId(), actor.name());
        return instance;
    }

    /**
     * @Author tangxinglin
     * @Description //重置已有流程实例（驳回后重新发起），归档当前节点并按新模板创建新周期节点
     * @Date 2026/04/18 10:00
     * @Param [instance 已有流程实例, template 使用的模板, actor 当前操作者]
     * @return void
     */
    private void resetProcessInstance(ProcessInstanceEntity instance, ProcessTemplateEntity template, FoundationActorResolver.Actor actor) {
        int nextCycle = determineNextCycle(instance.getId());
        archiveCurrentNodeInstances(instance.getId());
        jdbcTemplate.update(
                "UPDATE biz_process_instance SET template_id = ?, template_version = ?, status = 'RUNNING', current_node_order = 1, started_at = CURRENT_TIMESTAMP, finished_at = NULL, updated_at = CURRENT_TIMESTAMP WHERE id = ?",
                template.getId(),
                template.getVersion(),
                instance.getId());
        createNodeInstances(instance.getId(), template.getNodes(), nextCycle);
        insertProcessActionRecord(instance.getId(), null, "AUDIT_RESUBMIT", "RUNNING", null, actor.userId(), actor.name());
        insertAuditRecord(instance.getBusinessId(), instance.getId(), "START", "IN_AUDIT", null, actor.userId(), actor.name());
    }

    /**
     * @Author tangxinglin
     * @Description //根据模板节点定义为流程实例创建节点实例记录
     * @Date 2026/04/18 10:00
     * @Param [processInstanceId 流程实例ID, templateNodes 模板节点列表, cycleNo 当前审批周期编号]
     * @return void
     */
    private void createNodeInstances(Long processInstanceId, List<ProcessTemplateNodeEntity> templateNodes, int cycleNo) {
        for (ProcessTemplateNodeEntity templateNode : templateNodes) {
            String nodeStatus = templateNode.getNodeOrder() == 1 ? "PENDING" : "WAITING";
            jdbcTemplate.update(
                    "INSERT INTO biz_process_instance_node (process_instance_id, template_node_id, node_key, node_name, node_order, approve_mode, role_code, status, cycle_no, is_current, node_mode, created_at, updated_at) "
                            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 1, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                    processInstanceId,
                    templateNode.getId(),
                    templateNode.getNodeKey(),
                    templateNode.getNodeName(),
                    templateNode.getNodeOrder(),
                    templateNode.getApproveMode(),
                    templateNode.getRoleCode(),
                    nodeStatus,
                    cycleNo,
                    templateNode.getNodeMode());
        }
    }

    /**
     * @Author tangxinglin
     * @Description //将当前活跃节点标记为非当前（归档），用于驳回重新发起时归档上一周期节点
     * @Date 2026/04/18 10:00
     * @Param [processInstanceId 流程实例ID]
     * @return void
     */
    private void archiveCurrentNodeInstances(Long processInstanceId) {
        jdbcTemplate.update(
                "UPDATE biz_process_instance_node SET is_current = 0, updated_at = CURRENT_TIMESTAMP WHERE process_instance_id = ? AND is_current = 1",
                processInstanceId);
    }

    /**
     * @Author tangxinglin
     * @Description //查询当前流程实例已存在的最大审批周期编号，并返回下一周期编号
     * @Date 2026/04/18 10:00
     * @Param [processInstanceId 流程实例ID]
     * @return int 下一审批周期编号
     */
    private int determineNextCycle(Long processInstanceId) {
        Integer maxCycle = jdbcTemplate.queryForObject(
                "SELECT COALESCE(MAX(cycle_no), 0) FROM biz_process_instance_node WHERE process_instance_id = ?",
                Integer.class,
                processInstanceId);
        return maxCycle == null ? 1 : maxCycle + 1;
    }

    /**
     * @Author tangxinglin
     * @Description //将事件状态流转为"审核中"，并写入事件流转记录和同步MongoDB状态
     * @Date 2026/04/18 10:00
     * @Param [eventId 事件ID, fromStatus 原状态, actionType 操作类型, actor 当前操作者]
     * @return void
     */
    private void transitionEventToInAudit(Long eventId, String fromStatus, String actionType, FoundationActorResolver.Actor actor) {
        int updatedRows = jdbcTemplate.update(
                "UPDATE biz_event SET status = 'IN_AUDIT', updated_at = CURRENT_TIMESTAMP WHERE id = ? AND status = ?",
                eventId,
                fromStatus);
        if (updatedRows == 0) {
            throw new BusinessException("AUDIT_ALREADY_STARTED", "该事件已启动审核");
        }
        jdbcTemplate.update(
                "INSERT INTO biz_event_record (event_id, from_status, to_status, action_type, operator_user_id, operator_name, remark, created_at, updated_at) VALUES (?, ?, 'IN_AUDIT', ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                eventId,
                fromStatus,
                actionType,
                actor.userId(),
                actor.name(),
                actionType);
        alarmWorkflowStatusSyncService.syncWorkflowStatus(eventId, "IN_AUDIT");
    }

    /**
     * @Author tangxinglin
     * @Description //解析初次提交审核时使用的流程模板，支持显式指定或按事件类型自动匹配
     * @Date 2026/04/18 10:00
     * @Param [event 事件实体, request 启动审核请求（可含指定模板ID）]
     * @return ProcessTemplateEntity 确定使用的流程模板
     */
    private ProcessTemplateEntity resolveTemplateForInitialSubmission(EventEntity event, StartAuditRequest request) {
        if (request != null && request.templateId() != null) {
            ProcessTemplateEntity selected = processTemplateService.getTemplate(request.templateId());
            validateEnabledTemplateForEvent(selected, event.getEventType());
            return selected;
        }
        List<ProcessTemplateEntity> matched = processTemplateService.findEnabledByEventType(event.getEventType());
        if (matched.size() == 1) {
            return matched.get(0);
        }
        if (matched.size() > 1) {
            throw new BusinessException("AUDIT_TEMPLATE_SELECTION_REQUIRED", "多个启用的模板匹配该事件类型，需要显式选择模板");
        }
        throw new BusinessException("AUDIT_TEMPLATE_NOT_FOUND", "没有启用的流程模板匹配该事件类型");
    }

    /**
     * @Author tangxinglin
     * @Description //解析驳回后重新提交审核时使用的流程模板，默认复用冻结快照，支持授权覆盖
     * @Date 2026/04/18 10:00
     * @Param [event 事件实体, existing 已有流程实例, request 启动审核请求（可含指定模板ID）]
     * @return ProcessTemplateEntity 确定使用的流程模板
     */
    private ProcessTemplateEntity resolveTemplateForRejectedResubmission(
            EventEntity event,
            ProcessInstanceEntity existing,
            StartAuditRequest request) {
        if (request != null && request.templateId() != null) {
            requireTemplateOverrideAuthorization();
            ProcessTemplateEntity selected = processTemplateService.getTemplate(request.templateId());
            validateEnabledTemplateForEvent(selected, event.getEventType());
            return selected;
        }

        return buildFrozenTemplateDefinition(existing, event.getEventType());
    }

    /**
     * @Author tangxinglin
     * @Description //校验当前用户是否具备模板覆盖权限
     * @Date 2026/04/18 10:00
     * @Param []
     * @return void
     */
    private void requireTemplateOverrideAuthorization() {
        if (!permissionGuard.has(PermissionCodes.API_PROCESS_TEMPLATE_CREATE)) {
            throw new BusinessException("AUDIT_TEMPLATE_OVERRIDE_NOT_AUTHORIZED", "模板覆盖需要服务端模板覆盖权限");
        }
    }

    /**
     * @Author tangxinglin
     * @Description //从已有流程实例的最新周期节点快照构建冻结模板定义，用于驳回重提时复用原模板
     * @Date 2026/04/18 10:00
     * @Param [existing 已有流程实例, eventType 事件类型]
     * @return ProcessTemplateEntity 冻结模板定义
     */
    private ProcessTemplateEntity buildFrozenTemplateDefinition(ProcessInstanceEntity existing, String eventType) {
        List<ProcessInstanceNodeEntity> latestCycleNodes = loadLatestCycleNodes(existing.getId());
        if (latestCycleNodes.isEmpty()) {
            throw new BusinessException("AUDIT_FROZEN_TEMPLATE_MISSING", "冻结的审核模板快照缺失");
        }

        ProcessTemplateEntity template = new ProcessTemplateEntity();
        template.setId(existing.getTemplateId());
        template.setEnabled(true);
        template.setEventType(eventType);
        template.setBusinessType(eventType);
        template.setVersion(existing.getTemplateVersion());
        template.setVersionNo(existing.getTemplateVersion());
        template.setTemplateCode("FROZEN-" + existing.getTemplateId() + "-V" + existing.getTemplateVersion());
        template.setTemplateName("Frozen template " + existing.getTemplateId() + " v" + existing.getTemplateVersion());

        List<ProcessTemplateNodeEntity> frozenDefinition = new ArrayList<>();
        for (ProcessInstanceNodeEntity instanceNode : latestCycleNodes) {
            ProcessTemplateNodeEntity node = new ProcessTemplateNodeEntity();
            node.setId(null);
            node.setTemplateId(existing.getTemplateId());
            node.setNodeKey(instanceNode.getNodeKey());
            node.setNodeName(instanceNode.getNodeName());
            node.setNodeOrder(instanceNode.getNodeOrder());
            node.setApproveMode(instanceNode.getNodeMode());
            node.setNodeMode(instanceNode.getNodeMode());
            node.setStatus("ACTIVE");
            frozenDefinition.add(node);
        }
        template.setNodes(frozenDefinition);
        return template;
    }

    /**
     * @Author tangxinglin
     * @Description //加载流程实例最新审批周期的节点列表
     * @Date 2026/04/18 10:00
     * @Param [processInstanceId 流程实例ID]
     * @return List<ProcessInstanceNodeEntity> 最新周期的节点列表
     */
    private List<ProcessInstanceNodeEntity> loadLatestCycleNodes(Long processInstanceId) {
        Integer maxCycle = jdbcTemplate.queryForObject(
                "SELECT COALESCE(MAX(cycle_no), 0) FROM biz_process_instance_node WHERE process_instance_id = ?",
                Integer.class,
                processInstanceId);
        if (maxCycle == null || maxCycle == 0) {
            return List.of();
        }
        return jdbcTemplate.query(
                "SELECT id, process_instance_id, template_node_id, node_key, node_name, node_order, status, cycle_no, is_current, node_mode, assignee_user_id, assignee_name, handled_at "
                        + "FROM biz_process_instance_node WHERE process_instance_id = ? AND cycle_no = ? ORDER BY node_order ASC, id ASC",
                (rs, rowNum) -> mapProcessNode(rs),
                processInstanceId,
                maxCycle);
    }

    /**
     * @Author tangxinglin
     * @Description //校验所选模板是否启用且与事件类型匹配
     * @Date 2026/04/18 10:00
     * @Param [template 流程模板, eventType 事件类型]
     * @return void
     */
    private void validateEnabledTemplateForEvent(ProcessTemplateEntity template, String eventType) {
        if (!Boolean.TRUE.equals(template.getEnabled())) {
            throw new BusinessException("AUDIT_TEMPLATE_NOT_ENABLED", "所选模板未启用");
        }
        if (!eventType.equals(template.getEventType())) {
            throw new BusinessException("AUDIT_TEMPLATE_EVENT_TYPE_MISMATCH", "所选模板与事件类型不匹配");
        }
    }

    /**
     * @Author tangxinglin
     * @Description //加载流程实例详情，包含节点列表和关联事件状态
     * @Date 2026/04/18 10:00
     * @Param [eventId 事件ID, processInstanceId 流程实例ID]
     * @return ProcessInstanceEntity 流程实例详情
     */
    private ProcessInstanceEntity loadAuditDetail(Long eventId, Long processInstanceId) {
        ProcessInstanceEntity instance = jdbcTemplate.queryForObject(
                "SELECT id, process_no, template_id, template_version, business_type, business_id, status, current_node_order, started_at, finished_at, created_at, updated_at FROM biz_process_instance WHERE id = ?",
                (rs, rowNum) -> {
                    ProcessInstanceEntity entity = new ProcessInstanceEntity();
                    entity.setId(rs.getLong("id"));
                    entity.setProcessNo(rs.getString("process_no"));
                    entity.setTemplateId(rs.getLong("template_id"));
                    entity.setTemplateVersion(rs.getInt("template_version"));
                    entity.setBusinessType(rs.getString("business_type"));
                    entity.setBusinessId(rs.getLong("business_id"));
                    entity.setStatus(rs.getString("status"));
                    entity.setCurrentNodeOrder(rs.getInt("current_node_order"));
                    Timestamp startedAt = rs.getTimestamp("started_at");
                    if (startedAt != null) {
                        entity.setStartedAt(startedAt.toLocalDateTime());
                    }
                    Timestamp finishedAt = rs.getTimestamp("finished_at");
                    if (finishedAt != null) {
                        entity.setFinishedAt(finishedAt.toLocalDateTime());
                    }
                    return entity;
                },
                processInstanceId);
        instance.setProcessInstanceId(instance.getId());
        instance.setEventId(eventId);
        instance.setProcessStatus(instance.getStatus());
        instance.setNodes(new ArrayList<>(loadCurrentNodes(processInstanceId)));
        instance.setEventStatus(jdbcTemplate.queryForObject(
                "SELECT status FROM biz_event WHERE id = ?",
                String.class,
                eventId));
        return instance;
    }

    /**
     * @Author tangxinglin
     * @Description //校验审批决策请求不为空且包含节点ID
     * @Date 2026/04/18 10:00
     * @Param [request 审批决策请求]
     * @return void
     */
    private void validateDecisionRequest(AuditDecisionRequest request) {
        if (request == null || request.nodeId() == null) {
            throw new BusinessException("VALIDATION_ERROR", "节点 ID 不能为空");
        }
    }

    /**
     * @Author tangxinglin
     * @Description //根据主键ID加载流程实例基本信息
     * @Date 2026/04/18 10:00
     * @Param [processInstanceId 流程实例主键ID]
     * @return ProcessInstanceEntity 流程实例
     */
    private ProcessInstanceEntity getProcessInstance(Long processInstanceId) {
        List<ProcessInstanceEntity> instances = jdbcTemplate.query(
                "SELECT id, process_no, template_id, template_version, business_type, business_id, status, current_node_order, started_at, finished_at, created_at, updated_at "
                        + "FROM biz_process_instance WHERE id = ?",
                (rs, rowNum) -> {
                    ProcessInstanceEntity entity = new ProcessInstanceEntity();
                    entity.setId(rs.getLong("id"));
                    entity.setProcessNo(rs.getString("process_no"));
                    entity.setTemplateId(rs.getLong("template_id"));
                    entity.setTemplateVersion(rs.getInt("template_version"));
                    entity.setBusinessType(rs.getString("business_type"));
                    entity.setBusinessId(rs.getLong("business_id"));
                    entity.setStatus(rs.getString("status"));
                    entity.setCurrentNodeOrder(rs.getInt("current_node_order"));
                    return entity;
                },
                processInstanceId);
        if (instances.isEmpty()) {
            throw new BusinessException("AUDIT_NOT_FOUND", "未找到该事件的审核流程");
        }
        return instances.get(0);
    }

    /**
     * @Author tangxinglin
     * @Description //对流程实例加行级锁（SELECT FOR UPDATE），防止并发审批冲突
     * @Date 2026/04/18 10:00
     * @Param [processInstanceId 流程实例ID]
     * @return void
     */
    private void lockProcessInstance(Long processInstanceId) {
        List<Long> instanceIds = jdbcTemplate.query(
                "SELECT id FROM biz_process_instance WHERE id = ? FOR UPDATE",
                (rs, rowNum) -> rs.getLong("id"),
                processInstanceId);
        if (instanceIds.isEmpty()) {
            throw new BusinessException("AUDIT_NOT_FOUND", "未找到该事件的审核流程");
        }
    }

    /**
     * @Author tangxinglin
     * @Description //校验流程实例和关联事件均处于可审批状态
     * @Date 2026/04/18 10:00
     * @Param [instance 流程实例, event 关联事件]
     * @return void
     */
    private void ensureRunnableAudit(ProcessInstanceEntity instance, EventEntity event) {
        if (!BUSINESS_TYPE_EVENT_AUDIT.equals(instance.getBusinessType())) {
            throw new BusinessException("AUDIT_PROCESS_INVALID", "该流程不是事件审核实例");
        }
        if (!"RUNNING".equals(instance.getStatus()) || !"IN_AUDIT".equals(event.getStatus())) {
            throw new BusinessException("AUDIT_PROCESS_STATUS_INVALID", "审核流程不在可审批状态");
        }
    }

    /**
     * @Author tangxinglin
     * @Description //校验目标节点尚未被处理（状态不为 APPROVED 或 REJECTED）
     * @Date 2026/04/18 10:00
     * @Param [targetNode 目标审批节点]
     * @return void
     */
    private void ensureNodeNotAlreadyProcessed(ProcessInstanceNodeEntity targetNode) {
        if ("APPROVED".equals(targetNode.getStatus()) || "REJECTED".equals(targetNode.getStatus())) {
            throw new BusinessException("AUDIT_NODE_ALREADY_PROCESSED", "审核节点已处理");
        }
    }

    /**
     * @Author tangxinglin
     * @Description //解析当前已认证的操作者，未认证时抛出业务异常
     * @Date 2026/04/18 10:00
     * @Param []
     * @return FoundationActorResolver.Actor 当前操作者信息
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
     * @Description //校验目标节点是否指派给当前操作者，若指派但不匹配则拒绝
     * @Date 2026/04/18 10:00
     * @Param [targetNode 目标审批节点, actor 当前操作者]
     * @return void
     */
    private void ensureNodeAssignedToActor(ProcessInstanceNodeEntity targetNode, FoundationActorResolver.Actor actor) {
        if (targetNode.getAssigneeUserId() == null) {
            return;
        }
        if (!actor.userId().equals(targetNode.getAssigneeUserId())) {
            throw new BusinessException("AUDIT_NODE_NOT_FOUND", "审核节点未找到");
        }
    }

    /**
     * @Author tangxinglin
     * @Description //从节点列表中找到指定ID的目标节点，未找到则抛出业务异常
     * @Date 2026/04/18 10:00
     * @Param [nodes 节点列表, nodeId 目标节点ID]
     * @return ProcessInstanceNodeEntity 目标节点
     */
    private ProcessInstanceNodeEntity requireTargetNode(List<ProcessInstanceNodeEntity> nodes, Long nodeId) {
        return nodes.stream()
                .filter(node -> node.getId().equals(nodeId))
                .findFirst()
                .orElseThrow(() -> new BusinessException("AUDIT_NODE_NOT_FOUND", "审核节点未找到"));
    }

    /**
     * @Author tangxinglin
     * @Description //校验目标节点是否允许执行审批通过操作
     * @Date 2026/04/18 10:00
     * @Param [instance 流程实例, targetNode 目标节点, nodes 当前所有节点]
     * @return void
     */
    private void validateApproveAllowed(
            ProcessInstanceEntity instance,
            ProcessInstanceNodeEntity targetNode,
            List<ProcessInstanceNodeEntity> nodes) {
        if ("WAITING".equals(targetNode.getStatus())) {
            throw new BusinessException("AUDIT_NODE_NOT_ACTIVE", "节点尚未激活");
        }
        if (!"PENDING".equals(targetNode.getStatus())) {
            throw new BusinessException("AUDIT_NODE_STATUS_INVALID", "只能审批待处理的活动节点");
        }
        if ("SEQUENTIAL".equals(targetNode.getNodeMode()) && !targetNode.getNodeOrder().equals(instance.getCurrentNodeOrder())) {
            throw new BusinessException("AUDIT_NODE_NOT_ACTIVE", "只能审批当前顺序节点");
        }
        if ("PARALLEL".equals(targetNode.getNodeMode())) {
            long activeCount = nodes.stream()
                    .filter(node -> node.getNodeOrder().equals(targetNode.getNodeOrder()))
                    .filter(node -> "PENDING".equals(node.getStatus()) || "APPROVED".equals(node.getStatus()))
                    .count();
            if (activeCount == 0) {
                throw new BusinessException("AUDIT_NODE_NOT_ACTIVE", "并行节点尚未激活");
            }
        }
    }

    /**
     * @Author tangxinglin
     * @Description //校验目标节点是否允许执行驳回操作（复用审批通过校验逻辑）
     * @Date 2026/04/18 10:00
     * @Param [instance 流程实例, targetNode 目标节点, nodes 当前所有节点]
     * @return void
     */
    private void validateRejectAllowed(
            ProcessInstanceEntity instance,
            ProcessInstanceNodeEntity targetNode,
            List<ProcessInstanceNodeEntity> nodes) {
        validateApproveAllowed(instance, targetNode, nodes);
    }

    /**
     * @Author tangxinglin
     * @Description //将目标节点状态从 PENDING 更新为指定状态，若节点已被其他请求处理则抛出异常
     * @Date 2026/04/18 10:00
     * @Param [nodeId 节点ID, status 目标状态]
     * @return void
     */
    private void completePendingNodeOrThrow(Long nodeId, String status) {
        int updatedRows = jdbcTemplate.update(
                "UPDATE biz_process_instance_node SET status = ?, handled_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP WHERE id = ? AND status = 'PENDING'",
                status,
                nodeId);
        if (updatedRows == 0) {
            throw new BusinessException("AUDIT_NODE_ALREADY_PROCESSED", "审核节点已被其他请求处理");
        }
    }

    /**
     * @Author tangxinglin
     * @Description //审批通过后推进流程：若当前节点组全部通过则激活下一节点组，若全部完成则关闭流程并流转事件状态
     * @Date 2026/04/18 10:00
     * @Param [instance 流程实例, event 关联事件, nodes 当前所有节点, actor 操作者]
     * @return void
     */
    private void advanceProcessAfterApproval(
            ProcessInstanceEntity instance,
            EventEntity event,
            List<ProcessInstanceNodeEntity> nodes,
            FoundationActorResolver.Actor actor) {
        int currentOrder = instance.getCurrentNodeOrder() == null ? 1 : instance.getCurrentNodeOrder();
        List<ProcessInstanceNodeEntity> currentGroup = nodes.stream()
                .filter(node -> node.getNodeOrder().equals(currentOrder))
                .sorted(Comparator.comparing(ProcessInstanceNodeEntity::getId))
                .toList();
        if (currentGroup.isEmpty()) {
            throw new BusinessException("AUDIT_NODE_STATE_INVALID", "当前审核节点组缺失");
        }

        boolean currentGroupApproved = currentGroup.stream().allMatch(node -> "APPROVED".equals(node.getStatus()));
        if (!currentGroupApproved) {
            jdbcTemplate.update(
                    "UPDATE biz_process_instance SET status = 'RUNNING', updated_at = CURRENT_TIMESTAMP WHERE id = ?",
                    instance.getId());
            return;
        }

        Integer nextOrder = nodes.stream()
                .map(ProcessInstanceNodeEntity::getNodeOrder)
                .filter(order -> order > currentOrder)
                .min(Integer::compareTo)
                .orElse(null);

        if (nextOrder == null) {
            jdbcTemplate.update(
                    "UPDATE biz_process_instance SET status = 'APPROVED', finished_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP WHERE id = ?",
                    instance.getId());
            insertProcessActionRecord(instance.getId(), null, "AUDIT_COMPLETE", "APPROVED", "流程审批通过", actor.userId(), actor.name());
            transitionEventStatus(event.getId(), "IN_AUDIT", "AUDIT_APPROVED", "AUDIT_APPROVE", "审核通过", actor.userId(), actor.name());

            // 地理路由：检查事件所属网格是否有组长
            String dispatchTargetStatus = "WAITING_DISPATCH";
            String dispatchAction = "WAIT_DISPATCH";
            String dispatchRemark = "审核通过，进入待派单";
            Long gridId = event.getGridId();
            log.info("[BPM-GEO-ROUTE] BPM审批完成触发地理路由, eventId={}, processInstanceId={}, gridId={}, operator={}", event.getId(), instance.getId(), gridId, actor.name());
            if (gridId != null) {
                Integer leaderCount = jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM cmn_org_member WHERE grid_id = ? AND status = 'ACTIVE' "
                                + "AND (position LIKE '%组长%' OR position LIKE '%网格长%' OR member_type = 'LEADER')",
                        Integer.class, gridId);
                log.info("[BPM-GEO-ROUTE] 网格组长查询完成, eventId={}, gridId={}, leaderCount={}", event.getId(), gridId, leaderCount);
                if (leaderCount != null && leaderCount > 0) {
                    dispatchTargetStatus = "WAITING_LEADER_REVIEW";
                    dispatchAction = "WAIT_LEADER_REVIEW";
                    dispatchRemark = "审核通过，进入组长审核";
                    log.info("[BPM-GEO-ROUTE] 路由决策: eventId={}, gridId={} → WAITING_LEADER_REVIEW (组长审核), leaderCount={}", event.getId(), gridId, leaderCount);
                } else {
                    log.info("[BPM-GEO-ROUTE] 路由决策: eventId={}, gridId={} → WAITING_DISPATCH (无组长)", event.getId(), gridId);
                }
            } else {
                log.info("[BPM-GEO-ROUTE] 事件未关联网格, eventId={}, gridId=null, 直接走待派单流程", event.getId());
            }
            transitionEventStatus(event.getId(), "AUDIT_APPROVED", dispatchTargetStatus, dispatchAction, dispatchRemark, actor.userId(), actor.name());
            log.info("[BPM-GEO-ROUTE] 路由落地完成: eventId={} 已更新为 {}", event.getId(), dispatchTargetStatus);
            insertAuditRecord(event.getId(), instance.getId(), "APPROVE", dispatchTargetStatus, "流程审批通过", actor.userId(), actor.name());
            return;
        }

        jdbcTemplate.update(
                "UPDATE biz_process_instance_node SET status = 'PENDING', updated_at = CURRENT_TIMESTAMP WHERE process_instance_id = ? AND is_current = 1 AND node_order = ? AND status = 'WAITING'",
                instance.getId(),
                nextOrder);
        jdbcTemplate.update(
                "UPDATE biz_process_instance SET status = 'RUNNING', current_node_order = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?",
                nextOrder,
                instance.getId());
    }

    /**
     * @Author tangxinglin
     * @Description //流转事件状态并写入流转记录，同步特定目标状态到MongoDB
     * @Date 2026/04/18 10:00
     * @Param [eventId 事件ID, fromStatus 原状态, toStatus 目标状态, actionType 操作类型, remark 备注, operatorUserId 操作人ID, operatorName 操作人名称]
     * @return void
     */
    private void transitionEventStatus(
            Long eventId,
            String fromStatus,
            String toStatus,
            String actionType,
            String remark,
            Long operatorUserId,
            String operatorName) {
        int updatedRows = jdbcTemplate.update(
                "UPDATE biz_event SET status = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ? AND status = ?",
                toStatus,
                eventId,
                fromStatus);
        if (updatedRows == 0) {
            throw new BusinessException("EVENT_STATUS_TRANSITION_INVALID", "事件状态不符合流转要求");
        }
        jdbcTemplate.update(
                "INSERT INTO biz_event_record (event_id, from_status, to_status, action_type, operator_user_id, operator_name, remark, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                eventId,
                fromStatus,
                toStatus,
                actionType,
                operatorUserId,
                operatorName,
                remark);
        if ("PENDING_AUDIT".equals(toStatus)
                || "IN_AUDIT".equals(toStatus)
                || "AUDIT_REJECTED".equals(toStatus)
                || "WAITING_DISPATCH".equals(toStatus)
                || "WAITING_LEADER_REVIEW".equals(toStatus)
                || "DISPATCHED_TO_WORK_ORDER".equals(toStatus)
                || "CLOSED".equals(toStatus)) {
            alarmWorkflowStatusSyncService.syncWorkflowStatus(eventId, toStatus);
        }
    }

    /**
     * @Author tangxinglin
     * @Description //加载流程实例当前周期的活跃节点列表，按节点序号和ID升序排列
     * @Date 2026/04/18 10:00
     * @Param [processInstanceId 流程实例ID]
     * @return List<ProcessInstanceNodeEntity> 当前活跃节点列表
     */
    private List<ProcessInstanceNodeEntity> loadCurrentNodes(Long processInstanceId) {
        return jdbcTemplate.query(
                "SELECT id, process_instance_id, template_node_id, node_key, node_name, node_order, status, cycle_no, is_current, node_mode, assignee_user_id, assignee_name, handled_at "
                        + "FROM biz_process_instance_node WHERE process_instance_id = ? AND is_current = 1 ORDER BY node_order ASC, id ASC",
                (rs, rowNum) -> mapProcessNode(rs),
                processInstanceId);
    }

    /**
     * @Author tangxinglin
     * @Description //将ResultSet一行数据映射为流程实例节点实体
     * @Date 2026/04/18 10:00
     * @Param [rs 数据库结果集]
     * @return ProcessInstanceNodeEntity 流程实例节点实体
     */
    private ProcessInstanceNodeEntity mapProcessNode(java.sql.ResultSet rs) throws SQLException {
        ProcessInstanceNodeEntity node = new ProcessInstanceNodeEntity();
        node.setId(rs.getLong("id"));
        node.setProcessInstanceId(rs.getLong("process_instance_id"));
        node.setTemplateNodeId(getNullableLong(rs, "template_node_id"));
        node.setNodeKey(rs.getString("node_key"));
        node.setNodeName(rs.getString("node_name"));
        node.setNodeOrder(rs.getInt("node_order"));
        node.setStatus(rs.getString("status"));
        node.setCycleNo(rs.getInt("cycle_no"));
        node.setIsCurrent(rs.getInt("is_current") == 1);
        node.setNodeMode(rs.getString("node_mode"));
        node.setAssigneeUserId(getNullableLong(rs, "assignee_user_id"));
        node.setAssigneeName(rs.getString("assignee_name"));
        Timestamp handledAt = rs.getTimestamp("handled_at");
        if (handledAt != null) {
            node.setHandledAt(handledAt.toLocalDateTime());
        }
        return node;
    }

    /**
     * @Author tangxinglin
     * @Description //从 KeyHolder 中提取自动生成的主键ID
     * @Date 2026/04/18 10:00
     * @Param [keyHolder 包含生成键的持有者]
     * @return Long 自动生成的主键ID
     */
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

    /**
     * @Author tangxinglin
     * @Description //根据事件ID查询该事件关联的审核流程实例（取第一条）
     * @Date 2026/04/18 10:00
     * @Param [eventId 事件主键ID]
     * @return ProcessInstanceEntity 关联的流程实例，不存在则返回null
     */
    private ProcessInstanceEntity findByEventId(Long eventId) {
        List<ProcessInstanceEntity> instances = jdbcTemplate.query(
                "SELECT id, process_no, template_id, template_version, business_type, business_id, status, current_node_order, started_at, finished_at, created_at, updated_at "
                        + "FROM biz_process_instance WHERE business_type = 'EVENT_AUDIT' AND business_id = ? LIMIT 1",
                (rs, rowNum) -> {
                    ProcessInstanceEntity entity = new ProcessInstanceEntity();
                    entity.setId(rs.getLong("id"));
                    entity.setProcessNo(rs.getString("process_no"));
                    entity.setTemplateId(rs.getLong("template_id"));
                    entity.setTemplateVersion(rs.getInt("template_version"));
                    entity.setBusinessType(rs.getString("business_type"));
                    entity.setBusinessId(rs.getLong("business_id"));
                    entity.setStatus(rs.getString("status"));
                    entity.setCurrentNodeOrder(rs.getInt("current_node_order"));
                    return entity;
                },
                eventId);
        return instances.isEmpty() ? null : instances.get(0);
    }

    /**
     * @Author tangxinglin
     * @Description //根据事件ID查询事件实体，未找到则抛出业务异常
     * @Date 2026/04/18 10:00
     * @Param [eventId 事件主键ID]
     * @return EventEntity 事件实体
     */
    private EventEntity getEvent(Long eventId) {
        List<EventEntity> events = jdbcTemplate.query(
                "SELECT id, event_code, external_event_id, title, description, source_type, source_system, event_type, status, incident_address, longitude, latitude, occurred_at, created_at, updated_at "
                        + "FROM biz_event WHERE id = ?",
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
     * @Description //插入一条流程操作记录到 biz_process_action_record
     * @Date 2026/04/18 10:00
     * @Param [processInstanceId 流程实例ID, processInstanceNodeId 节点ID（可为null）, actionType 操作类型, actionResult 操作结果, remark 备注, operatorUserId 操作人ID, operatorName 操作人名称]
     * @return void
     */
    private void insertProcessActionRecord(
            Long processInstanceId,
            Long processInstanceNodeId,
            String actionType,
            String actionResult,
            String remark,
            Long operatorUserId,
            String operatorName) {
        ProcessActionRecordEntity record = new ProcessActionRecordEntity();
        record.setProcessInstanceId(processInstanceId);
        record.setProcessInstanceNodeId(processInstanceNodeId);
        record.setActionType(actionType);
        record.setActionResult(actionResult);
        record.setOperatorUserId(operatorUserId);
        record.setOperatorName(operatorName);
        record.setRemark(remark);
        jdbcTemplate.update(
                "INSERT INTO biz_process_action_record (process_instance_id, process_instance_node_id, action_type, action_result, operator_user_id, operator_name, remark, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                record.getProcessInstanceId(),
                record.getProcessInstanceNodeId(),
                record.getActionType(),
                record.getActionResult(),
                record.getOperatorUserId(),
                record.getOperatorName(),
                record.getRemark());
    }

    /**
     * @Author tangxinglin
     * @Description //插入一条审核记录到 biz_audit_record
     * @Date 2026/04/18 10:00
     * @Param [eventId 事件ID, processInstanceId 流程实例ID, decision 审批决定, status 审核后状态, opinion 审批意见, auditorUserId 审批人ID, auditorName 审批人名称]
     * @return void
     */
    private void insertAuditRecord(
            Long eventId,
            Long processInstanceId,
            String decision,
            String status,
            String opinion,
            Long auditorUserId,
            String auditorName) {
        AuditRecordEntity record = new AuditRecordEntity();
        record.setEventId(eventId);
        record.setProcessInstanceId(processInstanceId);
        record.setDecision(decision);
        record.setStatus(status);
        record.setOpinion(opinion);
        record.setAuditedAt(LocalDateTime.now());
        record.setAuditorUserId(auditorUserId);
        record.setAuditorName(auditorName);
        jdbcTemplate.update(
                "INSERT INTO biz_audit_record (event_id, process_instance_id, decision, status, auditor_user_id, auditor_name, opinion, audited_at, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                record.getEventId(),
                record.getProcessInstanceId(),
                record.getDecision(),
                record.getStatus(),
                record.getAuditorUserId(),
                record.getAuditorName(),
                record.getOpinion(),
                Timestamp.valueOf(record.getAuditedAt()));
    }

    /**
     * @Author tangxinglin
     * @Description //判断数据完整性异常是否由业务唯一约束（同一事件不能重复创建审核实例）触发
     * @Date 2026/04/18 10:00
     * @Param [exception 数据完整性异常]
     * @return boolean true表示是重复业务实例约束冲突
     */
    private boolean isDuplicateBusinessInstance(DataIntegrityViolationException exception) {
        Throwable cause = exception;
        while (cause != null) {
            if (cause instanceof SQLException sqlException) {
                String message = sqlException.getMessage();
                if (containsBusinessInstanceConstraint(message)) {
                    return true;
                }
            }
            cause = cause.getCause();
        }
        return containsBusinessInstanceConstraint(exception.getMessage());
    }

    /**
     * @Author tangxinglin
     * @Description //检查异常消息中是否包含业务实例唯一约束关键字
     * @Date 2026/04/18 10:00
     * @Param [message 异常消息字符串]
     * @return boolean true表示消息包含约束关键字
     */
    private boolean containsBusinessInstanceConstraint(String message) {
        if (message == null) {
            return false;
        }
        String normalized = message.toLowerCase();
        return normalized.contains("uk_biz_process_instance_business")
                || normalized.contains("constraint_af5")
                || normalized.contains("biz_process_instance(business_type nulls first, business_id nulls first)");
    }

    /**
     * @Author tangxinglin
     * @Description //安全读取ResultSet中可为NULL的Long型列值
     * @Date 2026/04/18 10:00
     * @Param [rs 数据库结果集, column 列名]
     * @return Long 列值，若数据库值为NULL则返回null
     */
    private Long getNullableLong(java.sql.ResultSet rs, String column) throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }

    public record StartAuditRequest(Long templateId, Boolean overrideAuthorized) {
    }

    public record AuditDecisionRequest(Long nodeId, String remark) {
    }
}
