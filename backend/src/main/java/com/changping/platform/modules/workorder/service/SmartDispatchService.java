package com.changping.platform.modules.workorder.service;

import com.changping.platform.common.exception.BusinessException;
import com.changping.platform.modules.workorder.entity.WorkOrderEntity;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * 智能分级派单服务
 *
 * <p>按「事件类型 → 受理角色」规则表智能路由派单：
 * <ul>
 *   <li>规则命中（如消防安全→两委干部 EVENT_OPERATOR）：派给该角色下待办工单最少的活跃人员</li>
 *   <li>规则未命中：默认派给网格员（H5_WORKER）</li>
 * </ul>
 * 规则可在 Web 端「派单规则」页维护（biz_dispatch_rule 表），未配置的类型回退到默认角色。
 */
@Service
public class SmartDispatchService {

    /** 未配置规则时的默认受理角色：网格员 */
    private static final String DEFAULT_ROLE_CODE = "H5_WORKER";

    private final JdbcTemplate jdbcTemplate;
    private final WorkOrderService workOrderService;

    public SmartDispatchService(JdbcTemplate jdbcTemplate, WorkOrderService workOrderService) {
        this.jdbcTemplate = jdbcTemplate;
        this.workOrderService = workOrderService;
    }

    /** 待办工单状态（排队中/处理中均计入负载） */
    private static final List<String> LOAD_STATUSES = List.of("WAITING_ACCEPT", "PROCESSING");

    /**
     * 候选人信息
     *
     * @param id          用户ID
     * @param name        姓名
     * @param pendingCount 当前待办工单数（用于负载均衡排序）
     */
    public record Candidate(Long id, String name, long pendingCount) {
    }

    /**
     * 派单建议结果
     *
     * @param recommendedUserId   推荐受派人ID（null 表示无可用人员）
     * @param recommendedUserName 推荐受派人姓名
     * @param roleCode            目标角色编码
     * @param roleLabel           目标角色名称
     * @param reason              推荐理由（展示给操作员）
     * @param candidates          按待办数升序的候选人员列表
     */
    public record Suggestion(
            Long recommendedUserId,
            String recommendedUserName,
            String roleCode,
            String roleLabel,
            String reason,
            List<Candidate> candidates) {
    }

    /**
     * 为事件生成派单建议：规则匹配角色 → 角色内按待办数最少推荐
     */
    public Suggestion suggest(Long eventId) {
        Map<String, Object> event = queryEventMeta(eventId);
        String eventType = (String) event.get("eventType");
        String gridName = (String) event.get("gridName");

        String roleCode = resolveRoleCode(eventType);
        String roleLabel = resolveRoleLabel(roleCode);
        List<Candidate> candidates = listCandidates(roleCode);

        String reason = buildReason(eventType, roleCode, roleLabel, gridName, candidates);
        Candidate recommended = candidates.isEmpty() ? null : candidates.get(0);
        return new Suggestion(
                recommended == null ? null : recommended.id(),
                recommended == null ? null : recommended.name(),
                roleCode,
                roleLabel,
                reason,
                candidates);
    }

    /**
     * 一键智能派单：按建议自动选择推荐人派发工单
     */
    public WorkOrderEntity smartDispatch(Long eventId, String remark) {
        Suggestion suggestion = suggest(eventId);
        if (suggestion.recommendedUserId() == null) {
            throw new BusinessException("SMART_DISPATCH_NO_CANDIDATE",
                    "当前没有可用的" + suggestion.roleLabel() + "（" + suggestion.roleCode() + "），请先在系统管理中配置工作人员");
        }
        String safeRemark = (remark == null || remark.isBlank()) ? "智能派单（自动分配）" : remark;
        return workOrderService.dispatch(eventId,
                new WorkOrderService.DispatchRequest(suggestion.recommendedUserId(), safeRemark));
    }

    /**
     * 规则表查询：事件类型 → 目标角色（启用状态优先，未配置返回默认角色）
     */
    private String resolveRoleCode(String eventType) {
        if (eventType == null || eventType.isBlank()) {
            return DEFAULT_ROLE_CODE;
        }
        List<String> roles = jdbcTemplate.query(
                "SELECT target_role_code FROM biz_dispatch_rule "
                        + "WHERE event_type = ? AND enabled = 1 ORDER BY priority ASC, id ASC LIMIT 1",
                (rs, rowNum) -> rs.getString("target_role_code"),
                eventType.trim());
        return roles.isEmpty() ? DEFAULT_ROLE_CODE : roles.get(0);
    }

    /**
     * 角色编码 → 展示名称
     */
    private String resolveRoleLabel(String roleCode) {
        List<String> names = jdbcTemplate.query(
                "SELECT role_name FROM sys_role WHERE role_code = ? LIMIT 1",
                (rs, rowNum) -> rs.getString("role_name"),
                roleCode);
        return names.isEmpty() ? roleCode : names.get(0);
    }

    /**
     * 列出指定角色下的活跃人员，按待办工单数升序（负载均衡）、再按用户ID升序
     */
    private List<Candidate> listCandidates(String roleCode) {
        List<Long> userIds = jdbcTemplate.query(
                "SELECT DISTINCT u.id FROM sys_user u "
                        + "JOIN sys_user_role ur ON ur.user_id = u.id "
                        + "JOIN sys_role r ON r.id = ur.role_id "
                        + "WHERE r.role_code = ? AND u.status = 'ACTIVE' AND u.deleted = 0 "
                        + "ORDER BY u.id ASC",
                (rs, rowNum) -> rs.getLong("id"),
                roleCode);
        List<Candidate> candidates = new ArrayList<>();
        for (Long userId : userIds) {
            String name = jdbcTemplate.query(
                    "SELECT real_name FROM sys_user WHERE id = ?",
                    (rs, rowNum) -> rs.getString("real_name"),
                    userId).stream().findFirst().orElse("用户" + userId);
            long pending = countPending(userId);
            candidates.add(new Candidate(userId, name, pending));
        }
        candidates.sort((a, b) -> {
            int byLoad = Long.compare(a.pendingCount(), b.pendingCount());
            return byLoad != 0 ? byLoad : Long.compare(a.id(), b.id());
        });
        return candidates;
    }

    /**
     * 统计用户当前待办工单数（排队中+处理中）
     */
    private long countPending(Long userId) {
        StringBuilder sql = new StringBuilder(
                "SELECT COUNT(*) FROM biz_work_order WHERE assignee_user_id = ? AND status IN (");
        for (int i = 0; i < LOAD_STATUSES.size(); i++) {
            sql.append(i == 0 ? "?" : ", ?");
        }
        sql.append(")");
        Object[] args = new Object[LOAD_STATUSES.size() + 1];
        args[0] = userId;
        for (int i = 0; i < LOAD_STATUSES.size(); i++) {
            args[i + 1] = LOAD_STATUSES.get(i);
        }
        Long count = jdbcTemplate.queryForObject(sql.toString(), Long.class, args);
        return count != null ? count : 0L;
    }

    /**
     * 查询事件基础信息（类型、所属网格），仅用于生成推荐理由
     */
    private Map<String, Object> queryEventMeta(Long eventId) {
        List<Map<String, Object>> rows = jdbcTemplate.query(
                "SELECT e.event_type AS eventType, e.grid_id AS gridId, g.grid_name AS gridName "
                        + "FROM biz_event e LEFT JOIN cmn_grid g ON g.id = e.grid_id WHERE e.id = ?",
                (rs, rowNum) -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("eventType", rs.getString("eventType"));
                    long gid = rs.getLong("gridId");
                    row.put("gridId", rs.wasNull() ? null : gid);
                    row.put("gridName", rs.getString("gridName"));
                    return row;
                },
                eventId);
        if (rows.isEmpty()) {
            throw new BusinessException("EVENT_NOT_FOUND", "事件不存在");
        }
        return rows.get(0);
    }

    /**
     * 生成推荐理由文案
     */
    private String buildReason(String eventType, String roleCode, String roleLabel,
            String gridName, List<Candidate> candidates) {
        String ruleText = resolveRoleCode(eventType).equals(roleCode) ? "命中派单规则" : "默认路由";
        StringBuilder sb = new StringBuilder();
        sb.append("「").append(eventType == null || eventType.isBlank() ? "未分类" : eventType).append("」");
        sb.append(ruleText).append(" → ").append(roleLabel);
        if (gridName != null && !gridName.isBlank()) {
            sb.append("；事件所属网格：").append(gridName);
        }
        if (!candidates.isEmpty()) {
            sb.append("；已按待办工单数最少排序（负载均衡）");
        } else {
            sb.append("；当前无可用人员");
        }
        return sb.toString();
    }

    /* ==================== 地理路由（两级派单：网格组长 → 网格员） ==================== */

    /**
     * 按地理路由生成派单建议：事件 → 所属网格 → 网格组长 → 组长下属网格员
     * 若网格无组长，回退到原智能派单逻辑
     */
    public Suggestion suggestByGeography(Long eventId) {
        Map<String, Object> event = queryEventMeta(eventId);
        Long gridId = (Long) event.get("gridId");
        String eventType = (String) event.get("eventType");
        String gridName = (String) event.get("gridName");

        if (gridId == null) {
            // 无网格信息 → 回退原逻辑
            return suggest(eventId);
        }

        Map<String, Object> leader = findGridLeader(gridId);
        if (leader == null) {
            // 无组长 → 回退原逻辑
            return suggest(eventId);
        }

        Long leaderOrgMemberId = (Long) leader.get("orgMemberId");
        Long leaderUserId = (Long) leader.get("userId");
        String leaderName = (String) leader.get("name");

        List<Candidate> subordinates = findLeaderSubordinates(leaderOrgMemberId);

        String reason = String.format(
                "事件所属网格「%s」的组长为%s；组长待办工单数最少的下属网格员为推荐人选",
                gridName != null ? gridName : "未知网格",
                leaderName);

        Candidate recommended = subordinates.isEmpty() ? null : subordinates.get(0);
        return new Suggestion(
                recommended == null ? null : recommended.id(),
                recommended == null ? null : recommended.name(),
                "H5_WORKER",
                "网格员",
                reason,
                subordinates);
    }

    /**
     * 查询事件所属网格的组长
     */
    public Map<String, Object> findGridLeader(Long gridId) {
        if (gridId == null) return null;
        List<Map<String, Object>> leaders = jdbcTemplate.query(
                "SELECT m.id AS orgMemberId, m.sys_user_id AS userId, m.name, m.position, g.grid_name AS gridName "
                        + "FROM cmn_org_member m "
                        + "LEFT JOIN cmn_grid g ON g.id = m.grid_id "
                        + "WHERE m.grid_id = ? AND m.status = 'ACTIVE' "
                        + "AND (m.position LIKE '%组长%' OR m.position LIKE '%网格长%' OR m.member_type = 'LEADER') "
                        + "LIMIT 1",
                (rs, rowNum) -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("orgMemberId", rs.getLong("orgMemberId"));
                    row.put("userId", rs.getLong("userId"));
                    row.put("name", rs.getString("name"));
                    row.put("position", rs.getString("position"));
                    row.put("gridName", rs.getString("gridName"));
                    return row;
                },
                gridId);
        return leaders.isEmpty() ? null : leaders.get(0);
    }

    /**
     * 查询组长下属的网格员列表（按待办工单数排序）
     */
    public List<Candidate> findLeaderSubordinates(Long leaderOrgMemberId) {
        if (leaderOrgMemberId == null) return List.of();
        List<Map<String, Object>> subordinates = jdbcTemplate.query(
                "SELECT m.sys_user_id AS userId, m.name, m.position "
                        + "FROM cmn_org_member m "
                        + "WHERE m.leader_id = ? AND m.status = 'ACTIVE' AND m.member_type = 'GRID_WORKER' "
                        + "ORDER BY m.id ASC",
                (rs, rowNum) -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("userId", rs.getLong("userId"));
                    row.put("name", rs.getString("name"));
                    return row;
                },
                leaderOrgMemberId);

        List<Candidate> candidates = new ArrayList<>();
        for (Map<String, Object> sub : subordinates) {
            Long userId = (Long) sub.get("userId");
            if (userId == null) continue;
            String name = (String) sub.get("name");
            long pending = countPending(userId);
            candidates.add(new Candidate(userId, name, pending));
        }
        candidates.sort((a, b) -> {
            int byLoad = Long.compare(a.pendingCount(), b.pendingCount());
            return byLoad != 0 ? byLoad : Long.compare(a.id(), b.id());
        });
        return candidates;
    }

    /**
     * 查询指定组长的待审核事件列表（WAITING_LEADER_REVIEW 状态）
     */
    public List<Map<String, Object>> findLeaderPendingEvents(Long leaderUserId) {
        if (leaderUserId == null) return List.of();
        // 找组长负责的网格
        List<Long> gridIds = jdbcTemplate.queryForList(
                "SELECT DISTINCT grid_id FROM cmn_org_member "
                        + "WHERE sys_user_id = ? AND status = 'ACTIVE' AND grid_id IS NOT NULL",
                Long.class,
                leaderUserId);
        if (gridIds.isEmpty()) return List.of();

        String placeholders = String.join(",", gridIds.stream().map(id -> "?").toList());
        return jdbcTemplate.query(
                "SELECT e.id, e.event_code, e.title, e.event_type, e.status, e.incident_address, "
                        + "e.urgency_level, e.created_at, g.grid_name AS gridName "
                        + "FROM biz_event e "
                        + "LEFT JOIN cmn_grid g ON g.id = e.grid_id "
                        + "WHERE e.status = 'WAITING_LEADER_REVIEW' AND e.grid_id IN (" + placeholders + ") "
                        + "AND COALESCE(e.archived, 0) = 0 "
                        + "ORDER BY e.id DESC",
                (rs, rowNum) -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("id", rs.getLong("id"));
                    row.put("eventCode", rs.getString("event_code"));
                    row.put("title", rs.getString("title"));
                    row.put("eventType", rs.getString("event_type"));
                    row.put("status", rs.getString("status"));
                    row.put("incidentAddress", rs.getString("incident_address"));
                    row.put("urgencyLevel", rs.getString("urgency_level"));
                    row.put("gridName", rs.getString("gridName"));
                    java.sql.Timestamp ts = rs.getTimestamp("created_at");
                    row.put("createdAt", ts != null ? ts.toLocalDateTime() : null);
                    return row;
                },
                gridIds.toArray());
    }

    /**
     * 查询事件对应的组长及下属网格员（用于 H5 组长派单界面）
     */
    public Map<String, Object> getLeaderDispatchInfo(Long eventId) {
        Map<String, Object> result = new LinkedHashMap<>();
        Map<String, Object> event = queryEventMeta(eventId);
        Long gridId = (Long) event.get("gridId");
        if (gridId == null) {
            result.put("leaderFound", false);
            result.put("reason", "事件未关联网格");
            return result;
        }
        Map<String, Object> leader = findGridLeader(gridId);
        if (leader == null) {
            result.put("leaderFound", false);
            result.put("reason", "网格未配置组长");
            return result;
        }
        Long leaderOrgMemberId = (Long) leader.get("orgMemberId");
        List<Candidate> subordinates = findLeaderSubordinates(leaderOrgMemberId);
        result.put("leaderFound", true);
        result.put("leader", leader);
        result.put("subordinates", subordinates);
        result.put("event", event);
        return result;
    }

    /* ==================== 规则管理（Web 端派单规则页） ==================== */

    /**
     * 规则列表（按优先级、ID 排序）
     */
    public List<Map<String, Object>> listRules() {
        return jdbcTemplate.query(
                "SELECT id, event_type AS eventType, target_role_code AS targetRoleCode, "
                        + "priority, enabled, remark, created_at AS createdAt, updated_at AS updatedAt "
                        + "FROM biz_dispatch_rule ORDER BY priority ASC, id ASC",
                (rs, rowNum) -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("id", rs.getLong("id"));
                    row.put("eventType", rs.getString("eventType"));
                    row.put("targetRoleCode", rs.getString("targetRoleCode"));
                    row.put("priority", rs.getInt("priority"));
                    row.put("enabled", rs.getInt("enabled"));
                    row.put("remark", rs.getString("remark"));
                    row.put("createdAt", rs.getTimestamp("createdAt") != null ? String.valueOf(rs.getTimestamp("createdAt").toLocalDateTime()) : null);
                    row.put("updatedAt", rs.getTimestamp("updatedAt") != null ? String.valueOf(rs.getTimestamp("updatedAt").toLocalDateTime()) : null);
                    return row;
                });
    }

    /**
     * 新增规则（同事件类型唯一，重复时抛业务异常）
     */
    public void createRule(String eventType, String targetRoleCode, Integer priority, Integer enabled, String remark) {
        if (eventType == null || eventType.isBlank() || targetRoleCode == null || targetRoleCode.isBlank()) {
            throw new BusinessException("VALIDATION_ERROR", "事件类型与目标角色不能为空");
        }
        jdbcTemplate.update(
                "INSERT INTO biz_dispatch_rule (event_type, target_role_code, priority, enabled, remark, created_at, updated_at) "
                        + "VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                eventType.trim(), targetRoleCode.trim(),
                priority == null ? 0 : priority,
                enabled == null || enabled == 0 ? 0 : 1,
                remark);
    }

    /**
     * 更新规则
     */
    public void updateRule(Long id, String eventType, String targetRoleCode, Integer priority, Integer enabled, String remark) {
        if (id == null) {
            throw new BusinessException("VALIDATION_ERROR", "规则ID不能为空");
        }
        int updated = jdbcTemplate.update(
                "UPDATE biz_dispatch_rule SET event_type = ?, target_role_code = ?, priority = ?, enabled = ?, remark = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?",
                eventType.trim(), targetRoleCode.trim(),
                priority == null ? 0 : priority,
                enabled == null || enabled == 0 ? 0 : 1,
                remark, id);
        if (updated == 0) {
            throw new BusinessException("DISPATCH_RULE_NOT_FOUND", "派单规则不存在");
        }
    }

    /**
     * 删除规则
     */
    public void deleteRule(Long id) {
        int deleted = jdbcTemplate.update("DELETE FROM biz_dispatch_rule WHERE id = ?", id);
        if (deleted == 0) {
            throw new BusinessException("DISPATCH_RULE_NOT_FOUND", "派单规则不存在");
        }
    }
}
