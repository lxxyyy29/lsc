package com.changping.platform.modules.party.controller;

import com.changping.platform.common.response.ApiResponse;
import com.changping.platform.modules.auth.security.AuthenticatedUserContextHolder;
import com.changping.platform.modules.auth.security.PermissionCodes;
import com.changping.platform.modules.auth.security.PermissionGuard;
import com.changping.platform.modules.auth.service.AuthService;
import com.changping.platform.modules.auth.service.CurrentUserService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Author tangxinglin
 * @Description //H5移动端志愿服务控制器，提供志愿活动列表、报名/取消报名及志愿积分查询接口
 * @Date 2026/08/07 10:00
 */
@RestController
@RequestMapping("/h5")
public class H5VolunteerController {

    private final JdbcTemplate jdbcTemplate;
    private final CurrentUserService currentUserService;
    private final PermissionGuard permissionGuard;

    /**
     * @Author tangxinglin
     * @Description //构造函数，注入 JDBC 模板、当前用户服务及权限校验
     * @Date 2026/08/07 10:00
     */
    public H5VolunteerController(JdbcTemplate jdbcTemplate,
                                 CurrentUserService currentUserService,
                                 PermissionGuard permissionGuard) {
        this.jdbcTemplate = jdbcTemplate;
        this.currentUserService = currentUserService;
        this.permissionGuard = permissionGuard;
    }

    /**
     * @Author tangxinglin
     * @Description //查询志愿活动列表，包含当前用户报名状态与报名人数
     * @Date 2026/08/07 10:00
     * @Param []
     * @return ApiResponse<List<Map<String, Object>>> 活动列表
     */
    @GetMapping("/activities")
    public ApiResponse<List<Map<String, Object>>> activities() {
        currentUserService.requireClientType(AuthService.ClientType.H5);
        permissionGuard.require(PermissionCodes.API_H5_VOLUNTEER_LIST);
        Long userId = AuthenticatedUserContextHolder.getRequired().id();
        return ApiResponse.ok(jdbcTemplate.queryForList(
            "SELECT a.*, u.real_name as creatorName, " +
            "  (SELECT COUNT(DISTINCT s.user_id) FROM sys_volunteer_signup s WHERE s.activity_id = a.id AND s.status IN ('SIGNED_UP','CHECKED_IN')) as signedUpCount, " +
            "  EXISTS(SELECT 1 FROM sys_volunteer_signup s WHERE s.activity_id = a.id AND s.user_id = ? AND s.status IN ('SIGNED_UP','CHECKED_IN')) as signedUp, " +
            "  EXISTS(SELECT 1 FROM sys_volunteer_signup s WHERE s.activity_id = a.id AND s.user_id = ? AND s.status = 'CHECKED_IN') as checkedIn " +
            "FROM sys_volunteer_activity a " +
            "LEFT JOIN sys_user u ON u.id = a.created_by " +
            "ORDER BY a.activity_date DESC",
            userId, userId));
    }

    /**
     * @Author tangxinglin
     * @Description //当前 H5 用户报名志愿活动，重复报名时幂等返回成功
     * @Date 2026/08/07 10:00
     * @Param [id 活动ID]
     * @return ApiResponse<Boolean> 报名结果
     */
    @PostMapping("/activities/{id}/signup")
    public ApiResponse<Boolean> signupActivity(@PathVariable Long id) {
        currentUserService.requireClientType(AuthService.ClientType.H5);
        permissionGuard.require(PermissionCodes.API_H5_VOLUNTEER_SIGNUP);
        Long userId = AuthenticatedUserContextHolder.getRequired().id();
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
     * @Author tangxinglin
     * @Description //当前 H5 用户取消志愿活动报名
     * @Date 2026/08/07 10:00
     * @Param [id 活动ID]
     * @return ApiResponse<Boolean> 取消结果
     */
    @DeleteMapping("/activities/{id}/signup")
    public ApiResponse<Boolean> cancelSignup(@PathVariable Long id) {
        currentUserService.requireClientType(AuthService.ClientType.H5);
        permissionGuard.require(PermissionCodes.API_H5_VOLUNTEER_SIGNUP);
        Long userId = AuthenticatedUserContextHolder.getRequired().id();
        jdbcTemplate.update(
            "DELETE FROM sys_volunteer_signup WHERE activity_id = ? AND user_id = ? AND status = 'SIGNED_UP'",
            id, userId);
        return ApiResponse.ok(true);
    }

    /**
     * @Author tangxinglin
     * @Description //当前 H5 用户对已报名志愿活动签到，限活动期间（活动当天至结束后2天）且仅一次，成功后发放20积分
     * @Date 2026/08/17 16:00
     * @Param [id 活动ID]
     * @return ApiResponse<Boolean> 签到结果
     */
    @PostMapping("/activities/{id}/checkin")
    @Transactional
    public ApiResponse<Boolean> checkinActivity(@PathVariable Long id) {
        currentUserService.requireClientType(AuthService.ClientType.H5);
        permissionGuard.require(PermissionCodes.API_H5_VOLUNTEER_SIGNUP);
        Long userId = AuthenticatedUserContextHolder.getRequired().id();

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            "SELECT a.activity_date, a.title, s.status FROM sys_volunteer_activity a " +
            "JOIN sys_volunteer_signup s ON s.activity_id = a.id AND s.user_id = ? " +
            "WHERE a.id = ?", userId, id);
        if (rows.isEmpty()) {
            throw new com.changping.platform.common.exception.BusinessException("VALIDATION_ERROR", "请先报名该活动");
        }
        Map<String, Object> row = rows.get(0);
        if ("CHECKED_IN".equals(row.get("status"))) {
            return ApiResponse.ok(true);
        }
        java.sql.Date activityDate = (java.sql.Date) row.get("activity_date");
        if (activityDate == null) {
            throw new com.changping.platform.common.exception.BusinessException("VALIDATION_ERROR", "活动未设置日期，无法签到");
        }
        long diffDays = java.time.temporal.ChronoUnit.DAYS.between(
            activityDate.toLocalDate(), java.time.LocalDate.now());
        if (diffDays < 0 || diffDays > 2) {
            throw new com.changping.platform.common.exception.BusinessException("VALIDATION_ERROR", "仅可在活动当天至活动结束后2天内签到");
        }
        jdbcTemplate.update(
            "UPDATE sys_volunteer_signup SET status = 'CHECKED_IN', check_in_time = CURRENT_TIMESTAMP " +
            "WHERE activity_id = ? AND user_id = ? AND status = 'SIGNED_UP'", id, userId);
        jdbcTemplate.update(
            "INSERT INTO sys_volunteer_points (user_id, total_points, available_points) VALUES (?, 20, 20) " +
            "ON DUPLICATE KEY UPDATE total_points = total_points + 20, available_points = available_points + 20",
            userId);
        jdbcTemplate.update(
            "INSERT INTO sys_volunteer_points_log (user_id, points, reason, source_type, source_id) " +
            "VALUES (?, 20, ?, 'VOLUNTEER_ACTIVITY', ?)",
            userId, "志愿活动签到：" + row.get("title"), id);
        return ApiResponse.ok(true);
    }

    /**
     * @Author tangxinglin
     * @Description //查询当前 H5 用户的志愿积分账户与最近积分流水
     * @Date 2026/08/07 10:00
     * @Param []
     * @return ApiResponse<Map<String, Object>> 积分账户与流水
     */
    @GetMapping("/points")
    public ApiResponse<Map<String, Object>> points() {
        currentUserService.requireClientType(AuthService.ClientType.H5);
        permissionGuard.require(PermissionCodes.API_H5_VOLUNTEER_POINTS);
        Long userId = AuthenticatedUserContextHolder.getRequired().id();

        List<Map<String, Object>> accounts = jdbcTemplate.queryForList(
            "SELECT total_points, available_points FROM sys_volunteer_points WHERE user_id = ?", userId);
        Map<String, Object> account = new HashMap<>();
        account.put("totalPoints", 0);
        account.put("availablePoints", 0);
        if (!accounts.isEmpty()) {
            Map<String, Object> row = accounts.get(0);
            Object total = row.get("total_points");
            Object available = row.get("available_points");
            account.put("totalPoints", total != null ? total : 0);
            account.put("availablePoints", available != null ? available : 0);
        }

        List<Map<String, Object>> logs = jdbcTemplate.queryForList(
            "SELECT points, reason, source_type as sourceType, DATE_FORMAT(created_at, '%Y-%m-%d %H:%i') as createdAt " +
            "FROM sys_volunteer_points_log WHERE user_id = ? ORDER BY created_at DESC LIMIT 50", userId);

        Map<String, Object> result = new HashMap<>();
        result.put("account", account);
        result.put("logs", logs);
        return ApiResponse.ok(result);
    }
}
