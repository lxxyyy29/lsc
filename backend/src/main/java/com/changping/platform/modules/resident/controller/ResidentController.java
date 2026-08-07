package com.changping.platform.modules.resident.controller;

import com.changping.platform.common.exception.BusinessException;
import com.changping.platform.common.response.ApiResponse;
import com.changping.platform.modules.auth.security.AuthenticatedUserContextHolder;
import com.changping.platform.modules.auth.service.AuthService;
import com.changping.platform.modules.auth.service.CurrentUserService;
import com.changping.platform.modules.community.entity.PolicyResourceEntity;
import com.changping.platform.modules.community.service.PolicyResourceService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Author tangxinglin
 * @Description //居民小程序端互动控制器：志愿活动报名/取消、政策查询、志愿积分、便民报修
 * @Date 2026/08/07 15:00
 */
@RestController
@RequestMapping("/resident")
public class ResidentController {

    private final JdbcTemplate jdbcTemplate;
    private final CurrentUserService currentUserService;
    private final PolicyResourceService policyResourceService;

    /**
     * @Author tangxinglin
     * @Description //构造函数，注入 JDBC 模板、当前用户服务与政策资源服务
     * @Date 2026/08/07 15:00
     */
    public ResidentController(JdbcTemplate jdbcTemplate,
                              CurrentUserService currentUserService,
                              PolicyResourceService policyResourceService) {
        this.jdbcTemplate = jdbcTemplate;
        this.currentUserService = currentUserService;
        this.policyResourceService = policyResourceService;
    }

    /**
     * @Author tangxinglin
     * @Description //查询志愿活动列表，含当前用户报名状态与报名人数（返回驼峰字段供小程序端使用）
     * @Date 2026/08/07 15:00
     * @Param []
     * @return ApiResponse<List<Map<String, Object>>> 活动列表
     */
    @GetMapping("/activities")
    public ApiResponse<List<Map<String, Object>>> activities() {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        Long userId = AuthenticatedUserContextHolder.getRequired().id();
        return ApiResponse.ok(jdbcTemplate.queryForList(
            "SELECT a.id, a.title, a.description, " +
            "  DATE_FORMAT(a.activity_date, '%Y-%m-%d') as activityDate, " +
            "  a.max_participants as maxParticipants, a.status, " +
            "  (SELECT COUNT(DISTINCT s.user_id) FROM sys_volunteer_signup s WHERE s.activity_id = a.id AND s.status = 'SIGNED_UP') as attendedCount, " +
            "  EXISTS(SELECT 1 FROM sys_volunteer_signup s WHERE s.activity_id = a.id AND s.user_id = ? AND s.status = 'SIGNED_UP') as signedUp, " +
            "  u.real_name as creatorName " +
            "FROM sys_volunteer_activity a " +
            "LEFT JOIN sys_user u ON u.id = a.created_by " +
            "ORDER BY a.activity_date DESC",
            userId));
    }

    /**
     * @Author tangxinglin
     * @Description //当前居民报名志愿活动，重复报名时幂等返回成功
     * @Date 2026/08/07 15:00
     * @Param [id 活动ID]
     * @return ApiResponse<Boolean> 报名结果
     */
    @PostMapping("/activities/{id}/signup")
    public ApiResponse<Boolean> signupActivity(@PathVariable Long id) {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
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
     * @Description //当前居民取消志愿活动报名
     * @Date 2026/08/07 15:00
     * @Param [id 活动ID]
     * @return ApiResponse<Boolean> 取消结果
     */
    @DeleteMapping("/activities/{id}/signup")
    public ApiResponse<Boolean> cancelSignup(@PathVariable Long id) {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        Long userId = AuthenticatedUserContextHolder.getRequired().id();
        jdbcTemplate.update(
            "DELETE FROM sys_volunteer_signup WHERE activity_id = ? AND user_id = ? AND status = 'SIGNED_UP'",
            id, userId);
        return ApiResponse.ok(true);
    }

    /**
     * @Author tangxinglin
     * @Description //政策资源列表（仅启用状态），供居民查询惠民政策
     * @Date 2026/08/07 15:00
     * @Param []
     * @return ApiResponse<List<PolicyResourceEntity>> 政策列表
     */
    @GetMapping("/policy-resources")
    public ApiResponse<List<PolicyResourceEntity>> policyResources() {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        return ApiResponse.ok(policyResourceService.listActive());
    }

    /**
     * @Author tangxinglin
     * @Description //查询当前居民的志愿积分账户与最近积分流水
     * @Date 2026/08/07 15:00
     * @Param []
     * @return ApiResponse<Map<String, Object>> 积分账户与流水
     */
    @GetMapping("/points")
    public ApiResponse<Map<String, Object>> points() {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
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
            "SELECT id, points, reason, source_type as sourceType, DATE_FORMAT(created_at, '%Y-%m-%d %H:%i') as createdAt " +
            "FROM sys_volunteer_points_log WHERE user_id = ? ORDER BY created_at DESC LIMIT 50", userId);

        Map<String, Object> result = new HashMap<>();
        result.put("account", account);
        result.put("logs", logs);
        return ApiResponse.ok(result);
    }

    /**
     * @Author tangxinglin
     * @Description //当前居民提交便民报修，报修人姓名取账号真实姓名
     * @Date 2026/08/07 15:00
     * @Param [body 报修表单：repairType/title/description/address/reporterPhone]
     * @return ApiResponse<Long> 报修单ID
     */
    @PostMapping("/repairs")
    public ApiResponse<Long> submitRepair(@RequestBody Map<String, Object> body) {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        Long userId = AuthenticatedUserContextHolder.getRequired().id();
        String repairType = String.valueOf(body.get("repairType"));
        String title = String.valueOf(body.get("title"));
        String description = body.get("description") == null ? null : String.valueOf(body.get("description"));
        String address = body.get("address") == null ? null : String.valueOf(body.get("address"));
        String reporterPhone = body.get("reporterPhone") == null ? null : String.valueOf(body.get("reporterPhone"));
        if (title == null || title.isEmpty() || repairType == null || repairType.isEmpty()) {
            throw new BusinessException("VALIDATION_ERROR", "请填写报修类型和标题");
        }
        String reporterName = jdbcTemplate.query(
            "SELECT real_name FROM sys_user WHERE id = ? AND deleted = 0",
            rs -> rs.next() ? rs.getString(1) : null, userId);
        jdbcTemplate.update(
            "INSERT INTO biz_repair_request (reporter_name, reporter_phone, reporter_user_id, repair_type, title, description, address, status) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, 'PENDING')",
            reporterName, reporterPhone, userId, repairType, title, description, address);
        Long id = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        return ApiResponse.ok(id);
    }

    /**
     * @Author tangxinglin
     * @Description //查询当前居民提交的报修列表（返回驼峰字段供小程序端使用）
     * @Date 2026/08/07 15:00
     * @Param []
     * @return ApiResponse<List<Map<String, Object>>> 报修列表
     */
    @GetMapping("/repairs")
    public ApiResponse<List<Map<String, Object>>> myRepairs() {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        Long userId = AuthenticatedUserContextHolder.getRequired().id();
        return ApiResponse.ok(jdbcTemplate.queryForList(
            "SELECT r.id, r.title, r.description, r.status, " +
            "  r.repair_type as repairType, r.address, r.reporter_phone as reporterPhone, " +
            "  DATE_FORMAT(r.created_at, '%Y-%m-%d %H:%i') as createdAt, r.handle_result as handleResult, " +
            "  DATE_FORMAT(r.handled_at, '%Y-%m-%d %H:%i') as handledAt " +
            "FROM biz_repair_request r " +
            "WHERE r.reporter_user_id = ? ORDER BY r.id DESC",
            userId));
    }

    /**
     * @Author tangxinglin
     * @Description //查询当前居民报修详情（仅本人可见）
     * @Date 2026/08/07 15:00
     * @Param [id 报修单ID]
     * @return ApiResponse<Map<String, Object>> 报修详情
     */
    @GetMapping("/repairs/{id}")
    public ApiResponse<Map<String, Object>> repairDetail(@PathVariable Long id) {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        Long userId = AuthenticatedUserContextHolder.getRequired().id();
        List<Map<String, Object>> items = jdbcTemplate.queryForList(
            "SELECT r.id, r.title, r.description, r.status, " +
            "  r.repair_type as repairType, r.address, r.reporter_name as reporterName, r.reporter_phone as reporterPhone, " +
            "  DATE_FORMAT(r.created_at, '%Y-%m-%d %H:%i') as createdAt, r.handle_result as handleResult, " +
            "  DATE_FORMAT(r.handled_at, '%Y-%m-%d %H:%i') as handledAt, hu.real_name as handlerName " +
            "FROM biz_repair_request r " +
            "LEFT JOIN sys_user hu ON hu.id = r.handler_user_id " +
            "WHERE r.id = ? AND r.reporter_user_id = ?",
            id, userId);
        if (items.isEmpty()) {
            throw new BusinessException("NOT_FOUND", "报修单不存在");
        }
        return ApiResponse.ok(items.get(0));
    }
}
