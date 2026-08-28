package com.changping.platform.modules.community.controller;

import com.changping.platform.common.response.ApiResponse;
import com.changping.platform.modules.auth.model.AuthenticatedUser;
import com.changping.platform.modules.auth.security.PermissionCodes;
import com.changping.platform.modules.auth.security.PermissionGuard;
import com.changping.platform.modules.auth.service.AuthService;
import com.changping.platform.modules.auth.service.CurrentUserService;
import com.changping.platform.modules.community.mapper.DashboardMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/community/dashboard")
public class DashboardController {

    private final DashboardMapper dashboardMapper;
    private final JdbcTemplate jdbcTemplate;
    private final CurrentUserService currentUserService;
    private final PermissionGuard permissionGuard;

    public DashboardController(
            DashboardMapper dashboardMapper,
            JdbcTemplate jdbcTemplate,
            CurrentUserService currentUserService,
            PermissionGuard permissionGuard) {
        this.dashboardMapper = dashboardMapper;
        this.jdbcTemplate = jdbcTemplate;
        this.currentUserService = currentUserService;
        this.permissionGuard = permissionGuard;
    }

    @GetMapping("/overview")
    public ApiResponse<Map<String, Object>> overview() {
        requireDashboardPermission();
        return ApiResponse.ok(dashboardMapper.getOverview());
    }

    /**
     * 综合监管大屏 — 一次性返回所有指标
     */
    @GetMapping("/big-screen")
    public ApiResponse<Map<String, Object>> bigScreen() {
        requireDashboardPermission();
        return ApiResponse.ok(dashboardMapper.getBigScreenData());
    }

    /**
     * 月度汇总报表
     */
    @GetMapping("/monthly-summary")
    public ApiResponse<Map<String, Object>> monthlySummary(
            @RequestParam(required = false) String year,
            @RequestParam(required = false) String month) {
        requireDashboardPermission();
        if (year == null) year = String.valueOf(java.time.LocalDate.now().getYear());
        if (month == null) month = String.valueOf(java.time.LocalDate.now().getMonthValue());
        return ApiResponse.ok(dashboardMapper.getMonthlySummary(year, month));
    }

    /**
     * 网格处置排名
     */
    @GetMapping("/grid-ranking")
    public ApiResponse<Map<String, Object>> gridRanking(
            @RequestParam(defaultValue = "month") String period) {
        requireDashboardPermission();
        return ApiResponse.ok(dashboardMapper.getGridRanking(period));
    }

    @GetMapping("/grid-stats")
    public ApiResponse<Map<String, Object>> gridStats() {
        requireDashboardPermission();
        return ApiResponse.ok(dashboardMapper.getGridStats());
    }

    @GetMapping("/ledger-stats")
    public ApiResponse<Map<String, Object>> ledgerStats() {
        requireDashboardPermission();
        try {
            Long total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM cmn_place_ledger", Long.class);
            Long rental = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM cmn_place_ledger WHERE place_category = 'RENTAL_HOUSE'", Long.class);
            Long shop = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM cmn_place_ledger WHERE place_category = 'SMALL_SHOP'", Long.class);
            Long other = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM cmn_place_ledger WHERE place_category NOT IN ('RENTAL_HOUSE','SMALL_SHOP')", Long.class);
            return ApiResponse.ok(Map.of("total", total, "rentalHouse", rental, "smallShop", shop, "other", other));
        } catch (Exception e) {
            return ApiResponse.ok(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 菜单角标 — 各模块待处理数量（Web 侧边栏微信式红点）
     * 微信式语义：点击进入页面即已读（记录已读时间），仅统计已读时间之后新增的待处理记录；
     * 从未已读过的模块显示全部待处理数量。
     */
    @GetMapping("/menu-badges")
    public ApiResponse<Map<String, Object>> menuBadges() {
        AuthenticatedUser user = currentUserService.requireClientType(AuthService.ClientType.WEB);
        Map<String, Object> badges = new LinkedHashMap<>();
        // 事件闭环处置：待审核 + 审核中 + 待派单 + 组长审核（排除归档与已删除事件）
        badges.put("eventsPending", countUnread(user.id(), "eventsPending", "biz_event",
                "status IN ('PENDING_AUDIT','IN_AUDIT','WAITING_DISPATCH','WAITING_LEADER_REVIEW') AND COALESCE(archived, 0) = 0 AND COALESCE(deleted, 0) = 0"));
        // 已完成工单：只读归档页，无待处理角标
        badges.put("workOrdersPending", countUnread(user.id(), "workOrdersPending", "biz_work_order",
                "1 = 0"));
        // 事件审核：网格员手机端已处理、待 PC 审核（工单待核实/待关闭确认），排除归档与已删除事件
        badges.put("auditsPending", countUnread(user.id(), "auditsPending", "biz_event",
                "COALESCE(archived, 0) = 0 AND COALESCE(deleted, 0) = 0 AND EXISTS (SELECT 1 FROM biz_work_order wo WHERE wo.source_event_id = biz_event.id AND wo.status IN ('WAITING_VERIFY','WAITING_CLOSE_CONFIRM'))"));
        // 居民上报：待审核（已读后新增）
        badges.put("residentReportsPending", countUnread(user.id(), "residentReportsPending", "cmn_resident_report",
                "status = 'PENDING'"));
        // 趋势预判：待处理预警（已读后新增）
        badges.put("trendAlerts", countUnread(user.id(), "trendAlerts", "biz_trend_alert",
                "status = 'OPEN'"));
        // 组织人员：待处理密码重置申请（已读后新增）
        badges.put("pwdResetsPending", countUnread(user.id(), "pwdResetsPending", "pwd_reset_request",
                "status = 'PENDING'"));
        return ApiResponse.ok(badges);
    }

    /**
     * 标记角标已读（点击进入对应页面时前端调用）
     */
    @PostMapping("/badges/{badgeKey}/read")
    public ApiResponse<Boolean> markBadgeRead(@PathVariable String badgeKey) {
        AuthenticatedUser user = currentUserService.requireClientType(AuthService.ClientType.WEB);
        jdbcTemplate.update(
            "INSERT INTO biz_badge_read (user_id, badge_key, read_at, created_at, updated_at) " +
            "VALUES (?, ?, NOW(), NOW(), NOW()) " +
            "ON DUPLICATE KEY UPDATE read_at = NOW(), updated_at = NOW()",
            user.id(), badgeKey);
        return ApiResponse.ok(true);
    }

    /** 统计某模块在用户已读时间之后新增的待处理记录数 */
    private long countUnread(Long userId, String badgeKey, String table, String whereSql) {
        String readAtSubquery = "IFNULL((SELECT read_at FROM biz_badge_read WHERE user_id = ? AND badge_key = ?), '1970-01-01 00:00:00')";
        Long c = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM " + table + " WHERE " + whereSql + " AND created_at > " + readAtSubquery,
            Long.class, userId, badgeKey);
        return c != null ? c : 0L;
    }

    private void requireDashboardPermission() {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.requireAny(PermissionCodes.MENU_COMMUNITY_DASHBOARD, PermissionCodes.MENU_BIG_SCREEN_VIEW);
    }
}
