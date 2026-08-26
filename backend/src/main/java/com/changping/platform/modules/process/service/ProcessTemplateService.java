package com.changping.platform.modules.process.service;

import com.changping.platform.common.exception.BusinessException;
import com.changping.platform.common.response.PagedResult;
import com.changping.platform.modules.process.entity.ProcessTemplateEntity;
import com.changping.platform.modules.process.entity.ProcessTemplateNodeEntity;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * @Author lxy
 * @Description //流程模板服务，负责流程模板及其节点的增删改查，包含校验逻辑和按事件类型查询启用模板的功能
 * @Date 2026/04/18 10:00
 */
@Service
public class ProcessTemplateService {

    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_DISABLED = "DISABLED";
    private static final String STATUS_ARCHIVED = "ARCHIVED";
    private static final String NODE_MODE_SEQUENTIAL = "SEQUENTIAL";
    private static final String DEFAULT_EVENT_TYPE = "DEFAULT";

    private final JdbcTemplate jdbcTemplate;

    /**
     * @Author lxy
     * @Description //构造器，注入 JdbcTemplate
     * @Date 2026/04/18 10:00
     * @Param [jdbcTemplate JDBC模板]
     * @return void
     */
    public ProcessTemplateService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * @Author lxy
     * @Description //创建流程模板及其节点配置，校验请求合法性后持久化并返回完整模板信息
     * @Date 2026/04/18 10:00
     * @Param [request 创建模板请求对象，包含名称、事件类型、版本和节点配置]
     * @return ProcessTemplateEntity 新建的流程模板实体（含节点）
     */
    @Transactional
    public ProcessTemplateEntity createTemplate(CreateTemplateRequest request) {
        validateCreateRequest(request);

        ProcessTemplateEntity entity = new ProcessTemplateEntity();
        entity.setTemplateCode("TPL-" + System.currentTimeMillis() + "-" + Math.abs(request.templateName().hashCode()));
        entity.setTemplateName(request.templateName().trim());
        entity.setBusinessType(normalizeEventType(request.eventType()));
        entity.setVersionNo(request.version());
        entity.setStatus(Boolean.TRUE.equals(request.enabled()) ? STATUS_ACTIVE : STATUS_DISABLED);

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO biz_process_template (template_code, template_name, business_type, version_no, status, created_at, updated_at) "
                            + "VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                    Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, entity.getTemplateCode());
            statement.setString(2, entity.getTemplateName());
            statement.setString(3, entity.getBusinessType());
            statement.setInt(4, entity.getVersionNo());
            statement.setString(5, entity.getStatus());
            return statement;
        }, keyHolder);
        entity.setId(extractGeneratedId(keyHolder));

        entity.setNodes(insertNodes(entity.getId(), request.nodes()));
        return enrich(entity);
    }

    /**
     * @Author lxy
     * @Description //查询所有流程模板列表（含节点），按ID升序排列
     * @Date 2026/04/18 10:00
     * @Param []
     * @return List<ProcessTemplateEntity> 流程模板列表
     */
    public List<ProcessTemplateEntity> listTemplates() {
        List<ProcessTemplateEntity> templates = jdbcTemplate.query(
                "SELECT id, template_code, template_name, business_type, version_no, status, remark, created_at, updated_at "
                        + "FROM biz_process_template ORDER BY id ASC",
                (rs, rowNum) -> {
                    ProcessTemplateEntity entity = new ProcessTemplateEntity();
                    entity.setId(rs.getLong("id"));
                    entity.setTemplateCode(rs.getString("template_code"));
                    entity.setTemplateName(rs.getString("template_name"));
                    entity.setBusinessType(rs.getString("business_type"));
                    entity.setVersionNo(rs.getInt("version_no"));
                    entity.setStatus(rs.getString("status"));
                    entity.setRemark(rs.getString("remark"));
                    Timestamp createdAt = rs.getTimestamp("created_at");
                    if (createdAt != null) {
                        entity.setCreatedAt(createdAt.toLocalDateTime());
                    }
                    Timestamp updatedAt = rs.getTimestamp("updated_at");
                    if (updatedAt != null) {
                        entity.setUpdatedAt(updatedAt.toLocalDateTime());
                    }
                    return entity;
                });
        attachNodes(templates);
        return templates.stream().map(this::enrich).toList();
    }

    /**
     * @Author lxy
     * @Description //分页查询流程模板列表，支持按模板名称关键词模糊过滤
     * @Date 2026/04/18 10:00
     * @Param [page 页码, pageSize 每页条数, keyword 关键词（可选）]
     * @return PagedResult<ProcessTemplateEntity> 分页流程模板列表
     */
    public PagedResult<ProcessTemplateEntity> listTemplatesPaged(int page, int pageSize, String keyword) {
        int offset = (page - 1) * pageSize;
        String where = "";
        Object[] countParams = new Object[0];
        Object[] dataParams;
        if (org.springframework.util.StringUtils.hasText(keyword)) {
            where = " WHERE template_name LIKE ?";
            String like = "%" + keyword.trim() + "%";
            countParams = new Object[]{like};
            dataParams = new Object[]{like, pageSize, offset};
        } else {
            dataParams = new Object[]{pageSize, offset};
        }
        Integer total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM biz_process_template" + where, Integer.class, countParams);
        int totalCount = total != null ? total : 0;
        List<ProcessTemplateEntity> templates = jdbcTemplate.query(
                "SELECT id, template_code, template_name, business_type, version_no, status, remark, created_at, updated_at "
                        + "FROM biz_process_template" + where + " ORDER BY id ASC LIMIT ? OFFSET ?",
                (rs, rowNum) -> {
                    ProcessTemplateEntity entity = new ProcessTemplateEntity();
                    entity.setId(rs.getLong("id"));
                    entity.setTemplateCode(rs.getString("template_code"));
                    entity.setTemplateName(rs.getString("template_name"));
                    entity.setBusinessType(rs.getString("business_type"));
                    entity.setVersionNo(rs.getInt("version_no"));
                    entity.setStatus(rs.getString("status"));
                    entity.setRemark(rs.getString("remark"));
                    Timestamp createdAt = rs.getTimestamp("created_at");
                    if (createdAt != null) {
                        entity.setCreatedAt(createdAt.toLocalDateTime());
                    }
                    Timestamp updatedAt = rs.getTimestamp("updated_at");
                    if (updatedAt != null) {
                        entity.setUpdatedAt(updatedAt.toLocalDateTime());
                    }
                    return entity;
                },
                dataParams);
        attachNodes(templates);
        List<ProcessTemplateEntity> enriched = templates.stream().map(this::enrich).toList();
        return new PagedResult<>(enriched, totalCount, page, pageSize);
    }

    /**
     * @Author lxy
     * @Description //查询与指定事件类型匹配的所有已启用（ACTIVE）流程模板
     * @Date 2026/04/18 10:00
     * @Param [eventType 事件类型]
     * @return List<ProcessTemplateEntity> 启用的流程模板列表
     */
    public List<ProcessTemplateEntity> findEnabledByEventType(String eventType) {
        List<ProcessTemplateEntity> templates = jdbcTemplate.query(
                "SELECT id, template_code, template_name, business_type, version_no, status, remark, created_at, updated_at "
                        + "FROM biz_process_template WHERE business_type = ? AND status = 'ACTIVE' ORDER BY id ASC",
                (rs, rowNum) -> {
                    ProcessTemplateEntity entity = new ProcessTemplateEntity();
                    entity.setId(rs.getLong("id"));
                    entity.setTemplateCode(rs.getString("template_code"));
                    entity.setTemplateName(rs.getString("template_name"));
                    entity.setBusinessType(rs.getString("business_type"));
                    entity.setVersionNo(rs.getInt("version_no"));
                    entity.setStatus(rs.getString("status"));
                    return entity;
                },
                eventType);
        attachNodes(templates);
        return templates.stream().map(this::enrich).toList();
    }

    /**
     * @Author lxy
     * @Description //更新流程模板信息，删除原有节点并重建新节点配置
     * @Date 2026/04/18 10:00
     * @Param [templateId 模板主键ID, request 更新请求对象]
     * @return ProcessTemplateEntity 更新后的流程模板实体
     */
    @Transactional
    public ProcessTemplateEntity updateTemplate(Long templateId, UpdateTemplateRequest request) {
        ProcessTemplateEntity existing = getTemplate(templateId);
        validateUpdateRequest(templateId, request, existing.getTemplateName());

        jdbcTemplate.update(
                "UPDATE biz_process_template SET template_name = ?, business_type = ?, version_no = ?, status = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?",
                request.templateName().trim(),
                normalizeEventType(request.eventType()),
                request.version(),
                Boolean.TRUE.equals(request.enabled()) ? STATUS_ACTIVE : STATUS_DISABLED,
                templateId);

        upsertNodes(templateId, request.nodes());

        return getTemplate(templateId);
    }

    /**
     * @Author lxy
     * @Description //全量替换模板节点：先删除旧节点,再按请求插入新节点。实例节点的 template_node_id 是弱引用(V22 移除了外键),此处删除不会影响已存在的流程实例及其节点快照。
     * @Date 2026/04/22 00:00
     * @Param [templateId 模板ID, nodeRequests 节点请求]
     * @return void
     */
    private void upsertNodes(Long templateId, List<TemplateNodeRequest> nodeRequests) {
        jdbcTemplate.update("DELETE FROM biz_process_template_node WHERE template_id = ?", templateId);
        insertNodes(templateId, nodeRequests);
    }

    /**
     * @Author lxy
     * @Description //删除流程模板及其所有节点配置
     * @Date 2026/04/18 10:00
     * @Param [templateId 模板主键ID]
     * @return void
     */
    @Transactional
    public void deleteTemplate(Long templateId) {
        getTemplate(templateId);
        if (isTemplateReferencedByInstance(templateId)) {
            jdbcTemplate.update(
                    "UPDATE biz_process_template SET status = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?",
                    STATUS_ARCHIVED,
                    templateId);
            jdbcTemplate.update(
                    "UPDATE biz_process_template_node SET status = ?, updated_at = CURRENT_TIMESTAMP WHERE template_id = ? AND status <> ?",
                    STATUS_ARCHIVED,
                    templateId,
                    STATUS_ARCHIVED);
            return;
        }
        jdbcTemplate.update("DELETE FROM biz_process_template_node WHERE template_id = ?", templateId);
        jdbcTemplate.update("DELETE FROM biz_process_template WHERE id = ?", templateId);
    }

    private boolean isTemplateReferencedByInstance(Long templateId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM biz_process_instance WHERE template_id = ?",
                Integer.class,
                templateId);
        return count != null && count > 0;
    }

    /**
     * @Author lxy
     * @Description //根据主键ID查询流程模板详情（含节点），未找到则抛出业务异常
     * @Date 2026/04/18 10:00
     * @Param [templateId 模板主键ID]
     * @return ProcessTemplateEntity 流程模板详情
     */
    public ProcessTemplateEntity getTemplate(Long templateId) {
        List<ProcessTemplateEntity> templates = jdbcTemplate.query(
                "SELECT id, template_code, template_name, business_type, version_no, status, remark, created_at, updated_at "
                        + "FROM biz_process_template WHERE id = ?",
                (rs, rowNum) -> {
                    ProcessTemplateEntity entity = new ProcessTemplateEntity();
                    entity.setId(rs.getLong("id"));
                    entity.setTemplateCode(rs.getString("template_code"));
                    entity.setTemplateName(rs.getString("template_name"));
                    entity.setBusinessType(rs.getString("business_type"));
                    entity.setVersionNo(rs.getInt("version_no"));
                    entity.setStatus(rs.getString("status"));
                    entity.setRemark(rs.getString("remark"));
                    Timestamp createdAt = rs.getTimestamp("created_at");
                    if (createdAt != null) {
                        entity.setCreatedAt(createdAt.toLocalDateTime());
                    }
                    Timestamp updatedAt = rs.getTimestamp("updated_at");
                    if (updatedAt != null) {
                        entity.setUpdatedAt(updatedAt.toLocalDateTime());
                    }
                    return entity;
                },
                templateId);
        if (templates.isEmpty()) {
            throw new BusinessException("PROCESS_TEMPLATE_NOT_FOUND", "流程模板未找到");
        }
        attachNodes(templates);
        return enrich(templates.get(0));
    }

    /**
     * @Author lxy
     * @Description //批量插入模板节点配置，并验证负责人存在且状态正常
     * @Date 2026/04/18 10:00
     * @Param [templateId 模板ID, nodeRequests 节点配置请求列表]
     * @return List<ProcessTemplateNodeEntity> 已插入的节点列表
     */
    private List<ProcessTemplateNodeEntity> insertNodes(Long templateId, List<TemplateNodeRequest> nodeRequests) {
        List<ProcessTemplateNodeEntity> nodes = new ArrayList<>();
        for (TemplateNodeRequest nodeRequest : nodeRequests) {
            ProcessTemplateNodeEntity node = new ProcessTemplateNodeEntity();
            node.setTemplateId(templateId);
            node.setNodeKey(nodeRequest.nodeKey().trim());
            node.setNodeName(nodeRequest.nodeName().trim());
            node.setNodeOrder(nodeRequest.nodeOrder());
            node.setApproveMode(NODE_MODE_SEQUENTIAL);
            node.setNodeMode(NODE_MODE_SEQUENTIAL);
            node.setStatus(STATUS_ACTIVE);
            node.setAssigneeUserId(nodeRequest.assigneeUserId());
            node.setAssigneeName(requireActiveUserName(nodeRequest.assigneeUserId()));
            jdbcTemplate.update(
                    "INSERT INTO biz_process_template_node (template_id, node_key, node_name, node_order, approve_mode, status, assignee_user_id, assignee_name, created_at, updated_at) "
                            + "VALUES (?, ?, ?, ?, ?, 'ACTIVE', ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                    node.getTemplateId(),
                    node.getNodeKey(),
                    node.getNodeName(),
                    node.getNodeOrder(),
                    node.getApproveMode(),
                    node.getAssigneeUserId(),
                    node.getAssigneeName());
            nodes.add(node);
        }
        return nodes;
    }

    /**
     * @Author lxy
     * @Description //批量加载模板列表的节点配置，并挂载到对应模板实体上
     * @Date 2026/04/18 10:00
     * @Param [templates 需要附加节点的模板列表]
     * @return void
     */
    private void attachNodes(List<ProcessTemplateEntity> templates) {
        if (templates.isEmpty()) {
            return;
        }
        Map<Long, ProcessTemplateEntity> templateMap = new LinkedHashMap<>();
        for (ProcessTemplateEntity template : templates) {
            template.setNodes(new ArrayList<>());
            templateMap.put(template.getId(), template);
        }
        String placeholders = String.join(", ", templateMap.keySet().stream().map(id -> "?").toList());
        jdbcTemplate.query(
                "SELECT id, template_id, node_key, node_name, node_order, approve_mode, status, role_code, remark, assignee_user_id, assignee_name, created_at, updated_at "
                        + "FROM biz_process_template_node WHERE template_id IN (" + placeholders + ") ORDER BY template_id ASC, node_order ASC",
                rs -> {
                    ProcessTemplateNodeEntity node = new ProcessTemplateNodeEntity();
                    node.setId(rs.getLong("id"));
                    node.setTemplateId(rs.getLong("template_id"));
                    node.setNodeKey(rs.getString("node_key"));
                    node.setNodeName(rs.getString("node_name"));
                    node.setNodeOrder(rs.getInt("node_order"));
                    node.setApproveMode(rs.getString("approve_mode"));
                    node.setStatus(rs.getString("status"));
                    node.setRoleCode(rs.getString("role_code"));
                    node.setRemark(rs.getString("remark"));
                    long assigneeUserId = rs.getLong("assignee_user_id");
                    node.setAssigneeUserId(rs.wasNull() ? null : assigneeUserId);
                    node.setAssigneeName(rs.getString("assignee_name"));
                    templateMap.get(node.getTemplateId()).getNodes().add(enrich(node));
                },
                templateMap.keySet().toArray());
    }

    /**
     * @Author lxy
     * @Description //为模板实体填充派生字段（enabled、eventType、version）及节点扩展字段
     * @Date 2026/04/18 10:00
     * @Param [entity 流程模板实体]
     * @return ProcessTemplateEntity 填充后的模板实体
     */
    private ProcessTemplateEntity enrich(ProcessTemplateEntity entity) {
        entity.setEnabled(STATUS_ACTIVE.equals(entity.getStatus()));
        entity.setEventType(entity.getBusinessType());
        entity.setVersion(entity.getVersionNo());
        if (entity.getNodes() != null) {
            entity.setNodes(entity.getNodes().stream().map(this::enrich).toList());
        }
        return entity;
    }

    /**
     * @Author lxy
     * @Description //为模板节点实体填充 nodeMode 派生字段（固定为顺序模式）
     * @Date 2026/04/18 10:00
     * @Param [node 流程模板节点实体]
     * @return ProcessTemplateNodeEntity 填充后的节点实体
     */
    private ProcessTemplateNodeEntity enrich(ProcessTemplateNodeEntity node) {
        node.setNodeMode(NODE_MODE_SEQUENTIAL);
        node.setApproveMode(NODE_MODE_SEQUENTIAL);
        return node;
    }

    /**
     * @Author lxy
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
        throw new BusinessException("PROCESS_TEMPLATE_CREATE_FAILED", "创建流程模板失败");
    }

    /**
     * @Author lxy
     * @Description //校验创建模板请求的合法性
     * @Date 2026/04/18 10:00
     * @Param [request 创建模板请求对象]
     * @return void
     */
    private void validateCreateRequest(CreateTemplateRequest request) {
        validateTemplateRequest(request.templateName(), request.version(), request.nodes());
        validateTemplateName(request.templateName(), null);
    }

    /**
     * @Author lxy
     * @Description //校验更新模板请求的合法性
     * @Date 2026/04/18 10:00
     * @Param [request 更新模板请求对象]
     * @return void
     */
    private void validateUpdateRequest(Long templateId, UpdateTemplateRequest request, String existingTemplateName) {
        if (request == null) {
            throw new BusinessException("VALIDATION_ERROR", "模板请求不能为空");
        }
        validateTemplateRequest(request.templateName(), request.version(), request.nodes());
        validateTemplateName(request.templateName(), templateId, existingTemplateName);
    }

    /**
     * @Author lxy
     * @Description //通用模板请求校验：名称非空、版本号大于0、节点非空且节点序号连续从1开始
     * @Date 2026/04/18 10:00
     * @Param [templateName 模板名称, version 版本号, nodes 节点请求列表]
     * @return void
     */
    private void validateTemplateRequest(String templateName, Integer version, List<TemplateNodeRequest> nodes) {
        if (!StringUtils.hasText(templateName)) {
            throw new BusinessException("VALIDATION_ERROR", "模板名称不能为空");
        }
        if (version == null || version < 1) {
            throw new BusinessException("VALIDATION_ERROR", "版本号必须大于零");
        }
        if (nodes == null || nodes.isEmpty()) {
            throw new BusinessException("VALIDATION_ERROR", "至少需要一个节点");
        }
        int expectedOrder = 1;
        for (TemplateNodeRequest node : nodes) {
            if (!StringUtils.hasText(node.nodeKey()) || !StringUtils.hasText(node.nodeName())) {
                throw new BusinessException("VALIDATION_ERROR", "节点标识和节点名称不能为空");
            }
            if (node.nodeOrder() == null || node.nodeOrder() != expectedOrder) {
                throw new BusinessException("VALIDATION_ERROR", "节点排序必须从 1 开始连续编号");
            }
            if (!NODE_MODE_SEQUENTIAL.equals(node.nodeMode())) {
                throw new BusinessException("VALIDATION_ERROR", "仅支持顺序节点");
            }
            if (node.assigneeUserId() == null) {
                throw new BusinessException("VALIDATION_ERROR", "节点负责人 ID 不能为空");
            }
            if (!StringUtils.hasText(node.assigneeName())) {
                throw new BusinessException("VALIDATION_ERROR", "节点负责人名称不能为空");
            }
            requireActiveUserName(node.assigneeUserId());
            expectedOrder++;
        }
    }

    private void validateTemplateName(String templateName, Long templateId) {
        validateTemplateName(templateName, templateId, null);
    }

    private void validateTemplateName(String templateName, Long templateId, String existingTemplateName) {
        String normalizedTemplateName = templateName.trim();
        if (normalizedTemplateName.equals(existingTemplateName == null ? null : existingTemplateName.trim())) {
            return;
        }
        Integer count = jdbcTemplate.queryForObject(
                templateId == null
                        ? "SELECT COUNT(*) FROM biz_process_template WHERE template_name = ?"
                        : "SELECT COUNT(*) FROM biz_process_template WHERE template_name = ? AND id <> ?",
                Integer.class,
                templateId == null ? new Object[]{normalizedTemplateName} : new Object[]{normalizedTemplateName, templateId});
        if (count != null && count > 0) {
            throw new BusinessException("PROCESS_TEMPLATE_NAME_EXISTS", "流程名称已存在");
        }
    }

    /**
     * @Author lxy
     * @Description //规范化事件类型，空值时返回默认值 DEFAULT
     * @Date 2026/04/18 10:00
     * @Param [eventType 事件类型字符串]
     * @return String 规范化后的事件类型
     */
    private String normalizeEventType(String eventType) {
        if (!StringUtils.hasText(eventType)) {
            return DEFAULT_EVENT_TYPE;
        }
        return eventType.trim();
    }

    /**
     * @Author lxy
     * @Description //校验指定用户存在且状态为 ACTIVE，返回其真实姓名
     * @Date 2026/04/18 10:00
     * @Param [userId 用户主键ID]
     * @return String 用户真实姓名
     */
    private String requireActiveUserName(Long userId) {
        List<UserIdentity> users = jdbcTemplate.query(
                "SELECT real_name, status FROM sys_user WHERE id = ?",
                (rs, rowNum) -> new UserIdentity(rs.getString("real_name"), rs.getString("status")),
                userId);
        if (users.isEmpty()) {
            throw new BusinessException("PROCESS_TEMPLATE_ASSIGNEE_NOT_FOUND", "模板节点负责人不存在");
        }
        UserIdentity user = users.get(0);
        if (!"ACTIVE".equalsIgnoreCase(user.status())) {
            throw new BusinessException("AUTH_USER_DISABLED", "模板节点负责人已被禁用");
        }
        return user.realName();
    }

    public record CreateTemplateRequest(
            String templateName,
            String eventType,
            Boolean enabled,
            Integer version,
            List<TemplateNodeRequest> nodes) {
    }

    public record UpdateTemplateRequest(
            String templateName,
            String eventType,
            Boolean enabled,
            Integer version,
            List<TemplateNodeRequest> nodes) {
    }

    public record TemplateNodeRequest(
            String nodeKey,
            String nodeName,
            Integer nodeOrder,
            String nodeMode,
            Long assigneeUserId,
            String assigneeName) {
    }

    private record UserIdentity(String realName, String status) {
    }
}
