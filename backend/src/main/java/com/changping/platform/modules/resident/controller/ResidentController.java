package com.changping.platform.modules.resident.controller;

import com.changping.platform.common.exception.BusinessException;
import com.changping.platform.common.response.ApiResponse;
import com.changping.platform.modules.auth.security.AuthenticatedUserContextHolder;
import com.changping.platform.modules.auth.service.AuthService;
import com.changping.platform.modules.auth.service.CurrentUserService;
import com.changping.platform.modules.community.entity.PolicyResourceEntity;
import com.changping.platform.modules.community.service.PolicyResourceService;
import com.changping.platform.modules.event.service.EventService;
import com.changping.platform.modules.event.vo.EventDetailVo;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Author lxy
 * @Description //居民小程序端互动控制器：志愿活动报名/取消、政策查询、志愿积分、便民报修
 * @Date 2026/08/07 15:00
 */
@RestController
@RequestMapping("/resident")
public class ResidentController {

    private final JdbcTemplate jdbcTemplate;
    private final CurrentUserService currentUserService;
    private final PolicyResourceService policyResourceService;
    private final EventService eventService;

    /**
     * @Author lxy
     * @Description //构造函数，注入 JDBC 模板、当前用户服务、政策资源服务与事件服务
     * @Date 2026/08/07 15:00
     */
    public ResidentController(JdbcTemplate jdbcTemplate,
                              CurrentUserService currentUserService,
                              PolicyResourceService policyResourceService,
                              EventService eventService) {
        this.jdbcTemplate = jdbcTemplate;
        this.currentUserService = currentUserService;
        this.policyResourceService = policyResourceService;
        this.eventService = eventService;
    }

    /**
     * @Author lxy
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
            "  (SELECT COUNT(DISTINCT s.user_id) FROM sys_volunteer_signup s WHERE s.activity_id = a.id AND s.status IN ('SIGNED_UP','CHECKED_IN')) as attendedCount, " +
            "  EXISTS(SELECT 1 FROM sys_volunteer_signup s WHERE s.activity_id = a.id AND s.user_id = ? AND s.status IN ('SIGNED_UP','CHECKED_IN')) as signedUp, " +
            "  EXISTS(SELECT 1 FROM sys_volunteer_signup s WHERE s.activity_id = a.id AND s.user_id = ? AND s.status = 'CHECKED_IN') as checkedIn, " +
            "  u.real_name as creatorName " +
            "FROM sys_volunteer_activity a " +
            "LEFT JOIN sys_user u ON u.id = a.created_by " +
            "ORDER BY a.activity_date DESC",
            userId, userId));
    }

    /**
     * @Author lxy
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
     * @Author lxy
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
     * @Author lxy
     * @Description //当前居民对已报名志愿活动签到，限活动期间（活动当天至结束后2天）且仅一次，成功后发放20积分
     * @Date 2026/08/17 16:00
     * @Param [id 活动ID]
     * @return ApiResponse<Boolean> 签到结果
     */
    @PostMapping("/activities/{id}/checkin")
    @Transactional
    public ApiResponse<Boolean> checkinActivity(@PathVariable Long id) {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        Long userId = AuthenticatedUserContextHolder.getRequired().id();
        return ApiResponse.ok(checkinAndGrantPoints(id, userId));
    }

    /**
     * @Author lxy
     * @Description //签到校验与积分发放公共逻辑：校验报名状态与活动窗口，幂等返回已签到，成功后更新签到状态、累计积分并写入流水。供居民端与H5端复用
     * @Date 2026/08/17 16:00
     * @Param [activityId 活动ID, userId 签到用户ID]
     * @return boolean 是否签到成功（已签到幂等返回 true）
     */
    public boolean checkinAndGrantPoints(Long activityId, Long userId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            "SELECT a.activity_date, a.title, a.status as activity_status, s.status FROM sys_volunteer_activity a " +
            "JOIN sys_volunteer_signup s ON s.activity_id = a.id AND s.user_id = ? " +
            "WHERE a.id = ?", userId, activityId);
        if (rows.isEmpty()) {
            throw new BusinessException("VALIDATION_ERROR", "请先报名该活动");
        }
        Map<String, Object> row = rows.get(0);
        if ("CHECKED_IN".equals(row.get("status"))) {
            return true;
        }
        if ("CANCELLED".equals(row.get("activity_status"))) {
            throw new BusinessException("VALIDATION_ERROR", "活动已取消，无法签到");
        }
        java.sql.Date activityDate = (java.sql.Date) row.get("activity_date");
        if (activityDate == null) {
            throw new BusinessException("VALIDATION_ERROR", "活动未设置日期，无法签到");
        }
        long diffDays = java.time.temporal.ChronoUnit.DAYS.between(
            activityDate.toLocalDate(), java.time.LocalDate.now());
        if (diffDays < 0 || diffDays > 2) {
            throw new BusinessException("VALIDATION_ERROR", "仅可在活动当天至活动结束后2天内签到");
        }
        // 以 UPDATE 作为并发闸门：仅 SIGNED_UP→CHECKED_IN 成功者才发放积分，杜绝并发/双击重复加分
        int updated = jdbcTemplate.update(
            "UPDATE sys_volunteer_signup SET status = 'CHECKED_IN', check_in_time = CURRENT_TIMESTAMP " +
            "WHERE activity_id = ? AND user_id = ? AND status = 'SIGNED_UP'", activityId, userId);
        if (updated == 0) {
            Integer checked = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sys_volunteer_signup WHERE activity_id = ? AND user_id = ? AND status = 'CHECKED_IN'",
                Integer.class, activityId, userId);
            if (checked != null && checked > 0) {
                return true;
            }
            throw new BusinessException("VALIDATION_ERROR", "报名状态已变化，无法签到");
        }
        jdbcTemplate.update(
            "INSERT INTO sys_volunteer_points (user_id, total_points, available_points) VALUES (?, 20, 20) " +
            "ON DUPLICATE KEY UPDATE total_points = total_points + 20, available_points = available_points + 20",
            userId);
        jdbcTemplate.update(
            "INSERT INTO sys_volunteer_points_log (user_id, points, reason, source_type, source_id) " +
            "VALUES (?, 20, ?, 'VOLUNTEER_ACTIVITY', ?)",
            userId, "志愿活动签到：" + row.get("title"), activityId);
        return true;
    }

    /**
     * @Author lxy
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
     * @Author lxy
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
     * @Author lxy
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
        // 报修统一归口至事件闭环处理中心：自动生成事件，派单处置一律走事件中心，报修单仅做记录凭证
        try {
            String eventDescription = "报修类型：" + repairType + (description != null && !description.isBlank() ? "\n" + description : "");
            EventDetailVo event = eventService.reportFromResident(title, eventDescription, "REPAIR", address,
                    reporterName, reporterPhone, userId, null, null);
            jdbcTemplate.update("UPDATE biz_repair_request SET event_id = ? WHERE id = ?", event.id(), id);
        } catch (Exception e) {
            // 建事件失败不回滚报修记录，避免阻断居民提交；由管理员在事件中心补录
        }
        return ApiResponse.ok(id);
    }

    /**
     * @Author lxy
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
     * @Author lxy
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
