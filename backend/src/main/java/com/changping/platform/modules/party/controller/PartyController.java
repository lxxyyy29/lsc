package com.changping.platform.modules.party.controller;

import com.changping.platform.common.response.ApiResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/party")
public class PartyController {

    private final JdbcTemplate jdbcTemplate;

    public PartyController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 党建总览统计
     */
    @GetMapping("/overview")
    public ApiResponse<Map<String, Object>> overview() {
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
        jdbcTemplate.update(
            "INSERT INTO sys_party_household (party_member_id, household_name, household_address, grid_id) VALUES (?, ?, ?, ?)",
            body.get("partyMemberId"), body.get("householdName"), body.get("householdAddress"), body.get("gridId"));
        return ApiResponse.ok(true);
    }

    /**
     * 记录走访
     */
    @PostMapping("/households/{id}/visit")
    public ApiResponse<Boolean> recordVisit(@PathVariable Long id) {
        jdbcTemplate.update(
            "UPDATE sys_party_household SET visit_count = visit_count + 1, last_visit_date = CURDATE(), updated_at = NOW() WHERE id = ?", id);
        return ApiResponse.ok(true);
    }

    /**
     * 志愿服务活动列表
     */
    @GetMapping("/activities")
    public ApiResponse<List<Map<String, Object>>> activities(@RequestParam(required = false) String status) {
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
        jdbcTemplate.update(
            "INSERT INTO sys_volunteer_signup (activity_id, user_id, status) VALUES (?, ?, 'SIGNED_UP')",
            id, body.get("userId"));
        return ApiResponse.ok(true);
    }

    /**
     * 三会一课列表
     */
    @GetMapping("/meetings")
    public ApiResponse<List<Map<String, Object>>> meetings(@RequestParam(required = false) String type) {
        StringBuilder sql = new StringBuilder("SELECT * FROM sys_party_meeting WHERE 1=1");
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
        jdbcTemplate.update(
            "INSERT INTO sys_party_meeting (meeting_type, title, meeting_date, party_branch, content, participant_count, status) VALUES (?, ?, ?, ?, ?, ?, 'COMPLETED')",
            body.get("meetingType"), body.get("title"), body.get("meetingDate"), body.get("partyBranch"), body.get("content"), body.get("participantCount"));
        return ApiResponse.ok(true);
    }

    /**
     * 党员量化考核
     */
    @GetMapping("/assessments")
    public ApiResponse<List<Map<String, Object>>> assessments(@RequestParam(required = false) String month) {
        if (month == null || month.isEmpty()) {
            month = new java.text.SimpleDateFormat("yyyy-MM").format(new Date());
        }
        List<Map<String, Object>> result = jdbcTemplate.queryForList(
            "SELECT a.*, u.real_name as memberName, u.username " +
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
        String month = body.get("month");
        if (month == null || month.isEmpty()) {
            month = new java.text.SimpleDateFormat("yyyy-MM").format(new Date());
        }

        // 删除已存在的考核记录
        jdbcTemplate.update("DELETE FROM sys_party_assessment WHERE assessment_month = ?", month);

        // 生成考核数据
        jdbcTemplate.update(
            "INSERT INTO sys_party_assessment (party_member_id, assessment_month, patrol_count, mediation_count, volunteer_hours, meeting_attendance, total_score) " +
            "SELECT pm.id, ?, " +
            "  (SELECT COUNT(*) FROM biz_patrol_task t WHERE t.user_id = pm.user_id AND DATE_FORMAT(t.completed_at, '%Y-%m') = ?), " +
            "  0, " +
            "  COALESCE((SELECT SUM(TIMESTAMPDIFF(HOUR, '2026-01-01 00:00:00', '2026-01-01 02:00:00')) FROM sys_volunteer_signup s JOIN sys_volunteer_activity a ON a.id = s.activity_id WHERE s.user_id = pm.user_id AND s.status = 'ATTENDED' AND DATE_FORMAT(a.activity_date, '%Y-%m') = ?), 0), " +
            "  (SELECT COUNT(*) FROM sys_party_meeting m WHERE m.meeting_date LIKE CONCAT(?, '%') AND m.status = 'COMPLETED'), " +
            "  0 " +
            "FROM sys_party_member pm WHERE pm.status = 'ACTIVE'",
            month, month, month, month);

        // 计算综合得分
        jdbcTemplate.update(
            "UPDATE sys_party_assessment SET total_score = patrol_count * 10 + mediation_count * 15 + volunteer_hours * 5 + meeting_attendance * 20 WHERE assessment_month = ?",
            month);

        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM sys_party_assessment WHERE assessment_month = ?", Long.class);
        return ApiResponse.ok(count != null ? count.intValue() : 0);
    }
}
