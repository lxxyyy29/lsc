package com.changping.platform.modules.party.controller;

import com.changping.platform.common.response.ApiResponse;
import com.changping.platform.modules.auth.security.AuthenticatedUserContextHolder;
import com.changping.platform.modules.auth.security.PermissionCodes;
import com.changping.platform.modules.auth.security.PermissionGuard;
import com.changping.platform.modules.auth.service.AuthService;
import com.changping.platform.modules.auth.service.CurrentUserService;
import org.springframework.jdbc.core.JdbcTemplate;
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
            "  (SELECT COUNT(DISTINCT s.user_id) FROM sys_volunteer_signup s WHERE s.activity_id = a.id AND s.status = 'SIGNED_UP') as signedUpCount, " +
            "  EXISTS(SELECT 1 FROM sys_volunteer_signup s WHERE s.activity_id = a.id AND s.user_id = ? AND s.status = 'SIGNED_UP') as signedUp " +
            "FROM sys_volunteer_activity a " +
            "LEFT JOIN sys_user u ON u.id = a.created_by " +
            "ORDER BY a.activity_date DESC",
            userId));
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
