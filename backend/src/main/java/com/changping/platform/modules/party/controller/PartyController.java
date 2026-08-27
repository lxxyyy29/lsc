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
        Long branchCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM sys_party_branch WHERE status = 'ACTIVE'", Long.class);

        result.put("memberCount", memberCount != null ? memberCount : 0);
        result.put("householdCount", householdCount != null ? householdCount : 0);
        result.put("activityCount", activityCount != null ? activityCount : 0);
        result.put("meetingCount", meetingCount != null ? meetingCount : 0);
        result.put("branchCount", branchCount != null ? branchCount : 0);

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

    // ==================== 党支部管理 V101 ====================

    /**
     * 党支部列表（支持按名称模糊搜索）
     */
    @GetMapping("/branches")
    public ApiResponse<List<Map<String, Object>>> listBranches(@RequestParam(required = false) String keyword) {
        requirePartyViewPermission();
        StringBuilder sql = new StringBuilder(
            "SELECT b.*, g.grid_name as gridName, " +
            "  u.real_name as secretaryName, " +
            "  (SELECT COUNT(*) FROM sys_party_branch_member m WHERE m.branch_id = b.id) AS memberCount, " +
            "  (SELECT COUNT(*) FROM sys_party_branch_member m WHERE m.branch_id = b.id AND m.role = 'SECRETARY') AS hasSecretary " +
            "FROM sys_party_branch b " +
            "LEFT JOIN cmn_grid g ON g.id = b.grid_id " +
            "LEFT JOIN sys_party_branch_member sm ON sm.branch_id = b.id AND sm.role = 'SECRETARY' " +
            "LEFT JOIN sys_party_member pm ON pm.id = sm.party_member_id " +
            "LEFT JOIN sys_user u ON u.id = pm.user_id " +
            "WHERE 1=1");
        List<Object> params = new ArrayList<>();
        if (keyword != null && !keyword.trim().isEmpty()) {
            sql.append(" AND b.branch_name LIKE ?");
            params.add("%" + keyword.trim() + "%");
        }
        sql.append(" ORDER BY b.id ASC");
        return ApiResponse.ok(jdbcTemplate.queryForList(sql.toString(), params.toArray()));
    }

    /**
     * 党支部详情（含书记、成员列表）
     */
    @GetMapping("/branches/{id}")
    public ApiResponse<Map<String, Object>> getBranchDetail(@PathVariable Long id) {
        requirePartyViewPermission();
        Map<String, Object> branch = jdbcTemplate.queryForMap(
            "SELECT b.*, g.grid_name as gridName, " +
            "  u.real_name as secretaryName " +
            "FROM sys_party_branch b " +
            "LEFT JOIN cmn_grid g ON g.id = b.grid_id " +
            "LEFT JOIN sys_party_branch_member sm ON sm.branch_id = b.id AND sm.role = 'SECRETARY' " +
            "LEFT JOIN sys_party_member pm ON pm.id = sm.party_member_id " +
            "LEFT JOIN sys_user u ON u.id = pm.user_id " +
            "WHERE b.id = ?", id);

        List<Map<String, Object>> members = jdbcTemplate.queryForList(
            "SELECT m.id as relId, m.role, m.joined_date AS joinedDate, " +
            "  pm.id AS memberId, u.real_name AS memberName, u.username AS memberAccount, " +
            "  pm.join_date AS partyJoinDate " +
            "FROM sys_party_branch_member m " +
            "JOIN sys_party_member pm ON pm.id = m.party_member_id " +
            "JOIN sys_user u ON u.id = pm.user_id " +
            "WHERE m.branch_id = ? " +
            "ORDER BY (m.role='SECRETARY') DESC, m.id ASC", id);

        List<Map<String, Object>> availableMembers = jdbcTemplate.queryForList(
            "SELECT pm.id AS memberId, u.real_name AS memberName, u.username AS memberAccount, " +
            "  pm.party_branch AS currentBranch, pm.status " +
            "FROM sys_party_member pm " +
            "JOIN sys_user u ON u.id = pm.user_id " +
            "WHERE pm.status = 'ACTIVE' " +
            "  AND pm.id NOT IN (SELECT party_member_id FROM sys_party_branch_member WHERE branch_id = ?) " +
            "ORDER BY u.real_name", id);

        Map<String, Object> result = new HashMap<>(branch);
        result.put("members", members);
        result.put("availableMembers", availableMembers);
        return ApiResponse.ok(result);
    }

    /**
     * 创建党支部（以党支部名称为必填）
     */
    @PostMapping("/branches")
    public ApiResponse<Long> createBranch(@RequestBody Map<String, Object> body) {
        requirePartyManagePermission();
        String name = (String) body.get("branchName");
        if (name == null || name.trim().isEmpty()) {
            throw new BusinessException("VALIDATION_ERROR", "党支部名称必填");
        }
        Long exists = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM sys_party_branch WHERE branch_name = ?", Long.class, name.trim());
        if (exists != null && exists > 0) {
            throw new BusinessException("VALIDATION_ERROR", "党支部名称已存在：" + name);
        }
        jdbcTemplate.update(
            "INSERT INTO sys_party_branch (branch_name, secretary_member_id, grid_id, address, phone, establish_date, remark, status) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, COALESCE(?, 'ACTIVE'))",
            name.trim(),
            toLong(body.get("secretaryMemberId")),
            toLong(body.get("gridId")),
            body.get("address"),
            body.get("phone"),
            body.get("establishDate"),
            body.get("remark"),
            body.get("status"));
        Long id = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);

        // 如果指定了书记，直接写入支部-党员关联
        Long secretaryMemberId = toLong(body.get("secretaryMemberId"));
        if (secretaryMemberId != null && id != null) {
            jdbcTemplate.update(
                "INSERT INTO sys_party_branch_member (branch_id, party_member_id, role, joined_date) VALUES (?, ?, 'SECRETARY', COALESCE(?, CURDATE())) " +
                "ON DUPLICATE KEY UPDATE role = 'SECRETARY', joined_date = VALUES(joined_date)",
                id, secretaryMemberId, body.get("establishDate"));
            jdbcTemplate.update("UPDATE sys_party_branch SET secretary_member_id = ? WHERE id = ?", secretaryMemberId, id);
        }
        return ApiResponse.ok(id);
    }

    /**
     * 更新党支部
     */
    @PutMapping("/branches/{id}")
    public ApiResponse<Boolean> updateBranch(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        requirePartyManagePermission();
        String name = (String) body.get("branchName");
        if (name == null || name.trim().isEmpty()) {
            throw new BusinessException("VALIDATION_ERROR", "党支部名称必填");
        }
        // 名称唯一性校验（排除自身）
        Long exists = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM sys_party_branch WHERE branch_name = ? AND id <> ?",
            Long.class, name.trim(), id);
        if (exists != null && exists > 0) {
            throw new BusinessException("VALIDATION_ERROR", "党支部名称已存在：" + name);
        }
        jdbcTemplate.update(
            "UPDATE sys_party_branch SET branch_name = ?, grid_id = ?, address = ?, phone = ?, establish_date = ?, remark = ?, status = ?, updated_at = NOW() " +
            "WHERE id = ?",
            name.trim(),
            toLong(body.get("gridId")),
            body.get("address"),
            body.get("phone"),
            body.get("establishDate"),
            body.get("remark"),
            body.get("status"),
            id);

        // 同步书记变动
        Long newSecretaryId = toLong(body.get("secretaryMemberId"));
        // 1) 清掉旧书记标记
        jdbcTemplate.update("UPDATE sys_party_branch_member SET role = 'MEMBER' WHERE branch_id = ? AND role = 'SECRETARY'", id);
        // 2) 新任书记
        if (newSecretaryId != null) {
            jdbcTemplate.update(
                "INSERT INTO sys_party_branch_member (branch_id, party_member_id, role, joined_date) VALUES (?, ?, 'SECRETARY', CURDATE()) " +
                "ON DUPLICATE KEY UPDATE role = 'SECRETARY'",
                id, newSecretaryId);
            jdbcTemplate.update("UPDATE sys_party_branch SET secretary_member_id = ?, updated_at = NOW() WHERE id = ?", newSecretaryId, id);
        } else {
            jdbcTemplate.update("UPDATE sys_party_branch SET secretary_member_id = NULL, updated_at = NOW() WHERE id = ?", id);
        }
        return ApiResponse.ok(true);
    }

    /**
     * 删除党支部（同时移除支部-党员关联）
     */
    @DeleteMapping("/branches/{id}")
    public ApiResponse<Boolean> deleteBranch(@PathVariable Long id) {
        requirePartyManagePermission();
        jdbcTemplate.update("DELETE FROM sys_party_branch_member WHERE branch_id = ?", id);
        jdbcTemplate.update("DELETE FROM sys_party_branch WHERE id = ?", id);
        return ApiResponse.ok(true);
    }

    /**
     * 导入党支部（批量以名称创建，已存在的跳过）
     * body.branches: [{ branchName, gridId?, secretaryMemberId?, address?, phone?, establishDate?, remark? }]
     * 返回 { created: N, skipped: N, total: N, errors: [...] }
     */
    @PostMapping("/branches/import")
    public ApiResponse<Map<String, Object>> importBranches(@RequestBody Map<String, Object> body) {
        requirePartyManagePermission();
        Object raw = body.get("branches");
        if (!(raw instanceof List)) {
            throw new BusinessException("VALIDATION_ERROR", "缺少导入数据 branches");
        }
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> list = (List<Map<String, Object>>) raw;
        int created = 0, skipped = 0;
        List<String> errors = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            Map<String, Object> row = list.get(i);
            String name = row.get("branchName") != null ? row.get("branchName").toString().trim() : "";
            if (name.isEmpty()) {
                errors.add("第" + (i + 1) + "行：党支部名称为空，已跳过");
                skipped++;
                continue;
            }
            try {
                Long exists = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM sys_party_branch WHERE branch_name = ?", Long.class, name);
                if (exists != null && exists > 0) {
                    skipped++;
                    continue;
                }
                jdbcTemplate.update(
                    "INSERT INTO sys_party_branch (branch_name, grid_id, address, phone, establish_date, remark, status) VALUES (?, ?, ?, ?, ?, ?, 'ACTIVE')",
                    name, toLong(row.get("gridId")), row.get("address"), row.get("phone"),
                    row.get("establishDate"), row.get("remark"));
                Long newId = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
                Long secId = toLong(row.get("secretaryMemberId"));
                if (secId != null && newId != null) {
                    jdbcTemplate.update(
                        "INSERT INTO sys_party_branch_member (branch_id, party_member_id, role, joined_date) VALUES (?, ?, 'SECRETARY', CURDATE()) " +
                        "ON DUPLICATE KEY UPDATE role = 'SECRETARY'",
                        newId, secId);
                    jdbcTemplate.update("UPDATE sys_party_branch SET secretary_member_id = ? WHERE id = ?", secId, newId);
                }
                created++;
            } catch (Exception e) {
                errors.add("第" + (i + 1) + "行（" + name + "）：" + e.getMessage());
                skipped++;
            }
        }
        Map<String, Object> res = new HashMap<>();
        res.put("total", list.size());
        res.put("created", created);
        res.put("skipped", skipped);
        res.put("errors", errors);
        return ApiResponse.ok(res);
    }

    /**
     * 向党支部添加党员（指定角色：SECRETARY / MEMBER）
     */
    @PostMapping("/branches/{id}/members")
    public ApiResponse<Boolean> addBranchMember(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        requirePartyManagePermission();
        Long memberId = toLong(body.get("memberId"));
        if (memberId == null) throw new BusinessException("VALIDATION_ERROR", "请选择党员");
        String role = body.get("role") != null ? body.get("role").toString().toUpperCase() : "MEMBER";
        if (!"SECRETARY".equals(role) && !"MEMBER".equals(role)) {
            throw new BusinessException("VALIDATION_ERROR", "角色仅支持 SECRETARY 或 MEMBER");
        }
        // 如果要设为书记，先把现有书记降为成员（避免双书记）
        if ("SECRETARY".equals(role)) {
            jdbcTemplate.update("UPDATE sys_party_branch_member SET role = 'MEMBER' WHERE branch_id = ? AND role = 'SECRETARY'", id);
        }
        jdbcTemplate.update(
            "INSERT INTO sys_party_branch_member (branch_id, party_member_id, role, joined_date) VALUES (?, ?, ?, CURDATE()) " +
            "ON DUPLICATE KEY UPDATE role = VALUES(role)",
            id, memberId, role);
        // 同步支部表的 secretary_member_id 字段
        if ("SECRETARY".equals(role)) {
            jdbcTemplate.update("UPDATE sys_party_branch SET secretary_member_id = ?, updated_at = NOW() WHERE id = ?", memberId, id);
        } else {
            // 如果被降级的那位正好是主记录书记，清除
            Long curSec = jdbcTemplate.queryForObject(
                "SELECT secretary_member_id FROM sys_party_branch WHERE id = ?", Long.class, id);
            if (curSec != null) {
                Long stillSecretary = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM sys_party_branch_member WHERE branch_id = ? AND party_member_id = ? AND role = 'SECRETARY'",
                    Long.class, id, curSec);
                if (stillSecretary == null || stillSecretary == 0) {
                    jdbcTemplate.update("UPDATE sys_party_branch SET secretary_member_id = NULL, updated_at = NOW() WHERE id = ?", id);
                }
            }
        }
        return ApiResponse.ok(true);
    }

    /**
     * 从党支部移除党员
     */
    @DeleteMapping("/branches/{id}/members/{memberId}")
    public ApiResponse<Boolean> removeBranchMember(@PathVariable Long id, @PathVariable Long memberId) {
        requirePartyManagePermission();
        jdbcTemplate.update("DELETE FROM sys_party_branch_member WHERE branch_id = ? AND party_member_id = ?", id, memberId);
        // 如果删除的是书记，清理支部主记录引用
        Long curSec = jdbcTemplate.queryForObject(
            "SELECT secretary_member_id FROM sys_party_branch WHERE id = ?", Long.class, id);
        if (curSec != null && curSec.equals(memberId)) {
            jdbcTemplate.update("UPDATE sys_party_branch SET secretary_member_id = NULL, updated_at = NOW() WHERE id = ?", id);
        }
        return ApiResponse.ok(true);
    }

    /**
     * 未归属支部的在册党员列表（添加人员到支部时下拉选择）
     */
    @GetMapping("/members/available")
    public ApiResponse<List<Map<String, Object>>> availableMembers(@RequestParam(required = false) Long excludeBranchId) {
        requirePartyViewPermission();
        StringBuilder sql = new StringBuilder(
            "SELECT pm.id AS memberId, u.real_name AS memberName, u.username AS memberAccount, " +
            "  pm.party_branch AS currentBranch, pm.status, pm.join_date AS joinDate " +
            "FROM sys_party_member pm " +
            "JOIN sys_user u ON u.id = pm.user_id " +
            "WHERE pm.status = 'ACTIVE'");
        List<Object> params = new ArrayList<>();
        if (excludeBranchId != null) {
            sql.append(" AND pm.id NOT IN (SELECT party_member_id FROM sys_party_branch_member WHERE branch_id = ?)");
            params.add(excludeBranchId);
        }
        sql.append(" ORDER BY u.real_name");
        return ApiResponse.ok(jdbcTemplate.queryForList(sql.toString(), params.toArray()));
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
