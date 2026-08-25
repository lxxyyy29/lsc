package com.changping.platform.modules.party.controller;

import com.changping.platform.common.exception.BusinessException;
import com.changping.platform.common.response.ApiResponse;
import com.changping.platform.modules.auth.security.PermissionCodes;
import com.changping.platform.modules.auth.security.PermissionGuard;
import com.changping.platform.modules.auth.service.AuthService;
import com.changping.platform.modules.auth.service.CurrentUserService;
import com.changping.platform.modules.auth.security.AuthenticatedUserContextHolder;
import com.changping.platform.modules.community.entity.PolicyResourceEntity;
import com.changping.platform.modules.community.service.PolicyResourceService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/party")
public class PartyController {

    private final JdbcTemplate jdbcTemplate;
    private final CurrentUserService currentUserService;
    private final PermissionGuard permissionGuard;
    private final PolicyResourceService policyResourceService;

    public PartyController(JdbcTemplate jdbcTemplate,
                           CurrentUserService currentUserService,
                           PermissionGuard permissionGuard,
                           PolicyResourceService policyResourceService) {
        this.jdbcTemplate = jdbcTemplate;
        this.currentUserService = currentUserService;
        this.permissionGuard = permissionGuard;
        this.policyResourceService = policyResourceService;
    }

    /**
     * 党建总览统计
     */
    @GetMapping("/overview")
    public ApiResponse<Map<String, Object>> overview() {
        requirePartyViewPermission();
        Map<String, Object> result = new HashMap<>();

        Long memberCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM sys_party_member WHERE status = 'ACTIVE'", Long.class);
        Long householdCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM sys_party_household", Long.class);
        Long activityCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM sys_volunteer_activity", Long.class);
        Long meetingCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM sys_party_meeting", Long.class);

        result.put("memberCount", memberCount != null ? memberCount : 0);
        result.put("householdCount", householdCount != null ? householdCount : 0);
        result.put("activityCount", activityCount != null ? activityCount : 0);
        result.put("meetingCount", meetingCount != null ? meetingCount : 0);

        // 志愿服务活动统计
        Long plannedActivities = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM sys_volunteer_activity WHERE status = 'PLANNED'", Long.class);
        Long completedActivities = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM sys_volunteer_activity WHERE status = 'COMPLETED'", Long.class);
        result.put("plannedActivities", plannedActivities != null ? plannedActivities : 0);
        result.put("completedActivities", completedActivities != null ? completedActivities : 0);

        return ApiResponse.ok(result);
    }

    /**
     * 党员联户列表
     */
    @GetMapping("/households")
    public ApiResponse<List<Map<String, Object>>> households(@RequestParam(required = false) Long memberId) {
        requirePartyViewPermission();
        StringBuilder sql = new StringBuilder(
            "SELECT ph.*, u.real_name as memberName FROM sys_party_household ph " +
            "LEFT JOIN sys_user u ON u.id = (SELECT user_id FROM sys_party_member WHERE id = ph.party_member_id) " +
            "WHERE 1=1");
        List<Object> params = new ArrayList<>();
        if (memberId != null) {
            sql.append(" AND ph.party_member_id = ?");
            params.add(memberId);
        }
        sql.append(" ORDER BY ph.visit_count DESC");
        return ApiResponse.ok(jdbcTemplate.queryForList(sql.toString(), params.toArray()));
    }

    /**
     * 添加联户关系
     */
    @PostMapping("/households")
    public ApiResponse<Boolean> addHousehold(@RequestBody Map<String, Object> body) {
        requirePartyManagePermission();
        Long partyMemberId = toLong(body.get("partyMemberId"));
        if (partyMemberId == null) {
            throw new BusinessException("VALIDATION_ERROR", "请选择关联党员");
        }
        jdbcTemplate.update(
            "INSERT INTO sys_party_household (party_member_id, household_name, household_address, grid_id) VALUES (?, ?, ?, ?)",
            partyMemberId, body.get("householdName"), body.get("householdAddress"), body.get("gridId"));
        return ApiResponse.ok(true);
    }

    /**
     * 记录走访
     */
    @PostMapping("/households/{id}/visit")
    public ApiResponse<Boolean> recordVisit(@PathVariable Long id) {
        requirePartyManagePermission();
        jdbcTemplate.update(
            "UPDATE sys_party_household SET visit_count = visit_count + 1, last_visit_date = CURDATE(), updated_at = NOW() WHERE id = ?", id);
        return ApiResponse.ok(true);
    }

    /**
     * 志愿服务活动列表
     */
    @GetMapping("/activities")
    public ApiResponse<List<Map<String, Object>>> activities(@RequestParam(required = false) String status) {
        requirePartyViewPermission();
        StringBuilder sql = new StringBuilder(
            "SELECT a.*, u.real_name as creatorName, " +
            "(SELECT COUNT(*) FROM sys_volunteer_signup s WHERE s.activity_id = a.id AND s.status = 'ATTENDED') as attendedCount " +
            "FROM sys_volunteer_activity a LEFT JOIN sys_user u ON u.id = a.created_by WHERE 1=1");
        List<Object> params = new ArrayList<>();
        if (status != null && !status.isEmpty()) {
            sql.append(" AND a.status = ?");
            params.add(status);
        }
        sql.append(" ORDER BY a.activity_date DESC");
        return ApiResponse.ok(jdbcTemplate.queryForList(sql.toString(), params.toArray()));
    }

    /**
     * 添加志愿活动
     */
    @PostMapping("/activities")
    public ApiResponse<Boolean> addActivity(@RequestBody Map<String, Object> body) {
        requirePartyManagePermission();
        jdbcTemplate.update(
            "INSERT INTO sys_volunteer_activity (title, description, activity_date, grid_id, max_participants, status, created_by) VALUES (?, ?, ?, ?, ?, 'PLANNED', ?)",
            body.get("title"), body.get("description"), body.get("activityDate"), body.get("gridId"), body.get("maxParticipants"), body.get("createdBy"));
        return ApiResponse.ok(true);
    }

    /**
     * 报名志愿活动
     */
    @PostMapping("/activities/{id}/signup")
    public ApiResponse<Boolean> signupActivity(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        requirePartyManagePermission();
        Object userId = body.get("userId");
        if (userId == null) {
            userId = AuthenticatedUserContextHolder.getRequired().id();
        }
        // 幂等报名：已报名则直接返回成功，避免重复插入
        Long exists = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM sys_volunteer_signup WHERE activity_id = ? AND user_id = ? AND status = 'SIGNED_UP'",
            Long.class, id, userId);
        if (exists != null && exists > 0) {
            return ApiResponse.ok(true);
        }
        jdbcTemplate.update(
            "INSERT INTO sys_volunteer_signup (activity_id, user_id, status) VALUES (?, ?, 'SIGNED_UP')",
            id, userId);
        return ApiResponse.ok(true);
    }

    /**
     * 三会一课列表
     */
    @GetMapping("/meetings")
    public ApiResponse<List<Map<String, Object>>> meetings(@RequestParam(required = false) String type) {
        requirePartyViewPermission();
        // 字段转 camelCase 别名，与前端取值（meetingDate/partyBranch/participantCount）保持一致
        StringBuilder sql = new StringBuilder(
                "SELECT id, meeting_type AS meetingType, title, meeting_date AS meetingDate, " +
                "party_branch AS partyBranch, content, participant_count AS participantCount, status " +
                "FROM sys_party_meeting WHERE 1=1");
        List<Object> params = new ArrayList<>();
        if (type != null && !type.isEmpty()) {
            sql.append(" AND meeting_type = ?");
            params.add(type);
        }
        sql.append(" ORDER BY meeting_date DESC");
        return ApiResponse.ok(jdbcTemplate.queryForList(sql.toString(), params.toArray()));
    }

    /**
     * 添加会议记录
     */
    @PostMapping("/meetings")
    public ApiResponse<Boolean> addMeeting(@RequestBody Map<String, Object> body) {
        requirePartyManagePermission();
        jdbcTemplate.update(
            "INSERT INTO sys_party_meeting (meeting_type, title, meeting_date, party_branch, content, participant_count, status) VALUES (?, ?, ?, ?, ?, ?, 'COMPLETED')",
            body.get("meetingType"), body.get("title"), body.get("meetingDate"), body.get("partyBranch"), body.get("content"), body.get("participantCount"));
        return ApiResponse.ok(true);
    }

    /**
     * 编辑会议记录
     */
    @PutMapping("/meetings/{id}")
    public ApiResponse<Boolean> updateMeeting(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        requirePartyManagePermission();
        jdbcTemplate.update(
            "UPDATE sys_party_meeting SET meeting_type = ?, title = ?, meeting_date = ?, party_branch = ?, content = ?, participant_count = ?, status = ?, updated_at = NOW() WHERE id = ?",
            body.get("meetingType"), body.get("title"), body.get("meetingDate"), body.get("partyBranch"), body.get("content"), body.get("participantCount"), body.get("status"), id);
        return ApiResponse.ok(true);
    }

    /**
     * 删除会议记录
     */
    @DeleteMapping("/meetings/{id}")
    public ApiResponse<Boolean> deleteMeeting(@PathVariable Long id) {
        requirePartyManagePermission();
        jdbcTemplate.update("DELETE FROM sys_party_meeting WHERE id = ?", id);
        return ApiResponse.ok(true);
    }

    /**
     * 党员量化考核
     */
    @GetMapping("/assessments")
    public ApiResponse<List<Map<String, Object>>> assessments(@RequestParam(required = false) String month) {
        requirePartyViewPermission();
        if (month == null || month.isEmpty()) {
            month = new java.text.SimpleDateFormat("yyyy-MM").format(new Date());
        }
        List<Map<String, Object>> result = jdbcTemplate.queryForList(
            "SELECT a.*, u.real_name as memberName, u.username, " +
            "a.patrol_count as patrolCount, a.mediation_count as mediationCount, " +
            "a.volunteer_hours as volunteerHours, a.meeting_attendance as meetingAttendance, " +
            "a.total_score as totalScore " +
            "FROM sys_party_assessment a " +
            "LEFT JOIN sys_party_member pm ON pm.id = a.party_member_id " +
            "LEFT JOIN sys_user u ON u.id = pm.user_id " +
            "WHERE a.assessment_month = ? " +
            "ORDER BY a.total_score DESC", month);
        return ApiResponse.ok(result);
    }

    /**
     * 生成月度考核
     */
    @PostMapping("/assessments/generate")
    public ApiResponse<Integer> generateAssessment(@RequestBody Map<String, String> body) {
        requirePartyManagePermission();
        String month = body.get("month");
        if (month == null || month.isEmpty()) {
            month = new java.text.SimpleDateFormat("yyyy-MM").format(new Date());
        }

        // 删除已存在的考核记录
        jdbcTemplate.update("DELETE FROM sys_party_assessment WHERE assessment_month = ?", month);

        // 生成考核数据：巡查次数取巡查打卡记录（cmn_patrol_record），志愿时长按活动日期估算 2 小时/次
        jdbcTemplate.update(
            "INSERT INTO sys_party_assessment (party_member_id, assessment_month, patrol_count, mediation_count, volunteer_hours, meeting_attendance, total_score) " +
            "SELECT pm.id, ?, " +
            "  (SELECT COUNT(*) FROM cmn_patrol_record r WHERE r.user_id = pm.user_id AND DATE_FORMAT(r.created_at, '%Y-%m') = ?), " +
            "  0, " +
            "  COALESCE((SELECT SUM(TIMESTAMPDIFF(HOUR, a.activity_date, DATE_ADD(a.activity_date, INTERVAL 2 HOUR))) FROM sys_volunteer_signup s JOIN sys_volunteer_activity a ON a.id = s.activity_id WHERE s.user_id = pm.user_id AND s.status = 'ATTENDED' AND DATE_FORMAT(a.activity_date, '%Y-%m') = ?), 0), " +
            "  (SELECT COUNT(*) FROM sys_party_meeting m WHERE m.meeting_date LIKE CONCAT(?, '%') AND m.status = 'COMPLETED'), " +
            "  0 " +
            "FROM sys_party_member pm WHERE pm.status = 'ACTIVE'",
            month, month, month, month);

        // 计算综合得分
        jdbcTemplate.update(
            "UPDATE sys_party_assessment SET total_score = patrol_count * 10 + mediation_count * 15 + volunteer_hours * 5 + meeting_attendance * 20 WHERE assessment_month = ?",
            month);

        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM sys_party_assessment WHERE assessment_month = ?", Long.class, month);
        return ApiResponse.ok(count != null ? count.intValue() : 0);
    }

    // ==================== 党建任务下沉 ====================

    /**
     * 党建任务列表（可按网格/状态/党员筛选）
     */
    @GetMapping("/tasks")
    public ApiResponse<List<Map<String, Object>>> tasks(@RequestParam(required = false) Long gridId,
                                                        @RequestParam(required = false) String status,
                                                        @RequestParam(required = false) Long memberId) {
        requirePartyViewPermission();
        StringBuilder sql = new StringBuilder(
            "SELECT t.*, u.real_name as memberName, g.grid_name as gridName " +
            "FROM sys_party_task t " +
            "LEFT JOIN sys_party_member pm ON pm.id = t.assigned_member_id " +
            "LEFT JOIN sys_user u ON u.id = pm.user_id " +
            "LEFT JOIN cmn_grid g ON g.id = t.grid_id " +
            "WHERE 1=1");
        List<Object> params = new ArrayList<>();
        if (gridId != null) { sql.append(" AND t.grid_id = ?"); params.add(gridId); }
        if (status != null && !status.isEmpty()) { sql.append(" AND t.status = ?"); params.add(status); }
        if (memberId != null) { sql.append(" AND t.assigned_member_id = ?"); params.add(memberId); }
        sql.append(" ORDER BY t.created_at DESC");
        return ApiResponse.ok(jdbcTemplate.queryForList(sql.toString(), params.toArray()));
    }

    /**
     * 创建党建任务
     */
    @PostMapping("/tasks")
    public ApiResponse<Boolean> createTask(@RequestBody Map<String, Object> body) {
        requirePartyManagePermission();
        jdbcTemplate.update(
            "INSERT INTO sys_party_task (task_title, task_type, description, grid_id, assigned_member_id, deadline, status, created_by) " +
            "VALUES (?, ?, ?, ?, ?, ?, 'PENDING', ?)",
            body.get("taskTitle"), body.get("taskType"), body.get("description"),
            body.get("gridId"), body.get("assignedMemberId"), body.get("deadline"),
            body.get("createdBy"));
        return ApiResponse.ok(true);
    }

    /**
     * 领办任务
     */
    @PostMapping("/tasks/{id}/accept")
    public ApiResponse<Boolean> acceptTask(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        requirePartyManagePermission();
        jdbcTemplate.update(
            "UPDATE sys_party_task SET status = 'ACCEPTED', assigned_member_id = ?, updated_at = NOW() WHERE id = ? AND status = 'PENDING'",
            body.get("memberId"), id);
        return ApiResponse.ok(true);
    }

    /**
     * 完成任务
     */
    @PostMapping("/tasks/{id}/complete")
    public ApiResponse<Boolean> completeTask(@PathVariable Long id) {
        requirePartyManagePermission();
        jdbcTemplate.update(
            "UPDATE sys_party_task SET status = 'COMPLETED', updated_at = NOW() WHERE id = ? AND status = 'ACCEPTED'", id);
        return ApiResponse.ok(true);
    }

    // ==================== 党群议事 ====================

    /**
     * 议事列表（含投票统计）
     */
    @GetMapping("/deliberations")
    public ApiResponse<List<Map<String, Object>>> deliberations(@RequestParam(required = false) String status) {
        requirePartyViewPermission();
        StringBuilder sql = new StringBuilder(
            "SELECT d.*, u.real_name as creatorName " +
            "FROM sys_party_deliberation d LEFT JOIN sys_user u ON u.id = d.created_by WHERE 1=1");
        List<Object> params = new ArrayList<>();
        if (status != null && !status.isEmpty()) { sql.append(" AND d.status = ?"); params.add(status); }
        sql.append(" ORDER BY d.created_at DESC");
        return ApiResponse.ok(jdbcTemplate.queryForList(sql.toString(), params.toArray()));
    }

    /**
     * 发起议事
     */
    @PostMapping("/deliberations")
    public ApiResponse<Boolean> createDeliberation(@RequestBody Map<String, Object> body) {
        requirePartyManagePermission();
        jdbcTemplate.update(
            "INSERT INTO sys_party_deliberation (title, content, grid_id, status, created_by) VALUES (?, ?, ?, 'OPEN', ?)",
            body.get("title"), body.get("content"), body.get("gridId"), body.get("createdBy"));
        return ApiResponse.ok(true);
    }

    /**
     * 议事投票（赞成/反对/弃权）+ 意见
     */
    @PostMapping("/deliberations/{id}/vote")
    public ApiResponse<Boolean> voteDeliberation(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        requirePartyViewPermission();
        Long userId = AuthenticatedUserContextHolder.getRequired().id();
        String voteType = (String) body.get("voteType");
        String comment = (String) body.get("comment");

        // 插入或更新投票（每人每议题一票）
        jdbcTemplate.update(
            "INSERT INTO sys_party_deliberation_vote (deliberation_id, user_id, vote_type, comment) VALUES (?, ?, ?, ?) " +
            "ON DUPLICATE KEY UPDATE vote_type = VALUES(vote_type), comment = VALUES(comment)",
            id, userId, voteType, comment);

        // 重新统计并更新议事表计数
        jdbcTemplate.update(
            "UPDATE sys_party_deliberation d SET " +
            "  support_count = (SELECT COUNT(*) FROM sys_party_deliberation_vote WHERE deliberation_id = ? AND vote_type = 'SUPPORT'), " +
            "  oppose_count  = (SELECT COUNT(*) FROM sys_party_deliberation_vote WHERE deliberation_id = ? AND vote_type = 'OPPOSE'), " +
            "  abstain_count = (SELECT COUNT(*) FROM sys_party_deliberation_vote WHERE deliberation_id = ? AND vote_type = 'ABSTAIN'), " +
            "  updated_at = NOW() " +
            "WHERE id = ?",
            id, id, id, id);

        return ApiResponse.ok(true);
    }

    /**
     * 获取议事的投票明细
     */
    @GetMapping("/deliberations/{id}/votes")
    public ApiResponse<List<Map<String, Object>>> getDeliberationVotes(@PathVariable Long id) {
        requirePartyViewPermission();
        return ApiResponse.ok(jdbcTemplate.queryForList(
            "SELECT v.*, u.real_name as userName FROM sys_party_deliberation_vote v " +
            "LEFT JOIN sys_user u ON u.id = v.user_id " +
            "WHERE v.deliberation_id = ? ORDER BY v.created_at DESC", id));
    }

    /**
     * 结项议事
     */
    @PostMapping("/deliberations/{id}/close")
    public ApiResponse<Boolean> closeDeliberation(@PathVariable Long id) {
        requirePartyManagePermission();
        jdbcTemplate.update("UPDATE sys_party_deliberation SET status = 'CLOSED', updated_at = NOW() WHERE id = ?", id);
        return ApiResponse.ok(true);
    }

    // ==================== 政策宣传推送 ====================

    /**
     * 一键推送政策（关联政策资源库，推送到网格/人群）
     */
    @PostMapping("/policy-push")
    public ApiResponse<Map<String, Object>> pushPolicy(@RequestBody Map<String, Object> body) {
        requirePartyManagePermission();
        Long policyId = toLong(body.get("policyId"));
        String pushTarget = (String) body.get("pushTarget");
        Long gridId = toLong(body.get("gridId"));
        Long userId = AuthenticatedUserContextHolder.getRequired().id();

        // 计算推送人次
        int pushCount = 0;
        if ("GRID".equals(pushTarget) && gridId != null) {
            Long cnt = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM cmn_population WHERE grid_id = ? AND status = 'ACTIVE'", Long.class, gridId);
            pushCount = cnt != null ? cnt.intValue() : 0;
        } else if ("POPULATION".equals(pushTarget)) {
            Long cnt = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM cmn_population WHERE status = 'ACTIVE'", Long.class);
            pushCount = cnt != null ? cnt.intValue() : 0;
        } else if ("ALL".equals(pushTarget)) {
            Long cnt = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM sys_user WHERE status = 'ACTIVE'", Long.class);
            pushCount = cnt != null ? cnt.intValue() : 0;
        }

        jdbcTemplate.update(
            "INSERT INTO sys_policy_push (policy_id, push_target, grid_id, push_count, created_by) VALUES (?, ?, ?, ?, ?)",
            policyId, pushTarget, gridId, pushCount, userId);

        Map<String, Object> result = new HashMap<>();
        result.put("pushCount", pushCount);
        return ApiResponse.ok(result);
    }

    /**
     * 推送历史
     */
    @GetMapping("/policy-push")
    public ApiResponse<List<Map<String, Object>>> policyPushHistory() {
        requirePartyViewPermission();
        return ApiResponse.ok(jdbcTemplate.queryForList(
            "SELECT p.*, pol.title as policyTitle, pol.policy_type as policyType, u.real_name as creatorName " +
            "FROM sys_policy_push p " +
            "LEFT JOIN cmn_policy_resource pol ON pol.id = p.policy_id " +
            "LEFT JOIN sys_user u ON u.id = p.created_by " +
            "ORDER BY p.created_at DESC"));
    }

    /**
     * 政策资源列表（供推送选择）
     */
    @GetMapping("/policy-resources")
    public ApiResponse<List<PolicyResourceEntity>> policyResourcesForPush() {
        requirePartyViewPermission();
        return ApiResponse.ok(policyResourceService.listActive());
    }

    // ==================== 考核自动生成增强 ====================

    /**
     * 生成月度考核（增强版：mediation_count 自动从矛盾纠纷类事件统计）
     */
    @PostMapping("/assessments/generate-v2")
    public ApiResponse<Integer> generateAssessmentV2(@RequestBody Map<String, String> body) {
        requirePartyManagePermission();
        String month = body.get("month");
        if (month == null || month.isEmpty()) {
            month = new java.text.SimpleDateFormat("yyyy-MM").format(new Date());
        }

        jdbcTemplate.update("DELETE FROM sys_party_assessment WHERE assessment_month = ?", month);

        // patrol_count 来自巡查打卡记录，mediation_count 来自矛盾纠纷类事件已办结工单
        jdbcTemplate.update(
            "INSERT INTO sys_party_assessment (party_member_id, assessment_month, patrol_count, mediation_count, volunteer_hours, meeting_attendance, total_score) " +
            "SELECT pm.id, ?, " +
            "  (SELECT COUNT(*) FROM cmn_patrol_record r WHERE r.user_id = pm.user_id AND DATE_FORMAT(r.created_at, '%Y-%m') = ?), " +
            "  (SELECT COUNT(*) FROM biz_work_order wo JOIN biz_event e ON e.id = wo.source_event_id WHERE wo.assignee_user_id = pm.user_id AND wo.status IN ('COMPLETED','CLOSED') AND e.event_type IN ('CONTRADICTION','矛盾纠纷','DISPUTE') AND DATE_FORMAT(wo.completed_at, '%Y-%m') = ?), " +
            "  COALESCE((SELECT SUM(TIMESTAMPDIFF(HOUR, a.activity_date, DATE_ADD(a.activity_date, INTERVAL 2 HOUR))) FROM sys_volunteer_signup s JOIN sys_volunteer_activity a ON a.id = s.activity_id WHERE s.user_id = pm.user_id AND s.status = 'ATTENDED' AND DATE_FORMAT(a.activity_date, '%Y-%m') = ?), 0), " +
            "  (SELECT COUNT(*) FROM sys_party_meeting m WHERE m.status = 'COMPLETED' AND DATE_FORMAT(m.meeting_date, '%Y-%m') = ?), " +
            "  0 " +
            "FROM sys_party_member pm WHERE pm.status = 'ACTIVE'",
            month, month, month, month, month);

        jdbcTemplate.update(
            "UPDATE sys_party_assessment SET total_score = patrol_count * 10 + mediation_count * 15 + volunteer_hours * 5 + meeting_attendance * 20 WHERE assessment_month = ?",
            month);

        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM sys_party_assessment WHERE assessment_month = ?", Long.class, month);
        return ApiResponse.ok(count != null ? count.intValue() : 0);
    }

    private void requirePartyViewPermission() {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.API_PARTY_VIEW);
    }

    private void requirePartyManagePermission() {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.API_PARTY_MANAGE);
    }

    private Long toLong(Object o) {
        if (o == null) return null;
        if (o instanceof Number) return ((Number) o).longValue();
        try {
            return Long.parseLong(o.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
