package com.changping.platform.modules.community.controller;

import com.changping.platform.common.response.ApiResponse;
import com.changping.platform.modules.auth.security.PermissionCodes;
import com.changping.platform.modules.auth.security.PermissionGuard;
import com.changping.platform.modules.auth.service.AuthService;
import com.changping.platform.modules.auth.service.CurrentUserService;
import com.changping.platform.modules.community.mapper.DashboardMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
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
     * 登录用户即可访问（菜单可见即可看角标），无需看板权限
     */
    @GetMapping("/menu-badges")
    public ApiResponse<Map<String, Object>> menuBadges() {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        Map<String, Object> badges = new LinkedHashMap<>();
        // 事件闭环处置：待审核 + 审核中 + 待派单
        badges.put("eventsPending", countBy("SELECT COUNT(*) FROM biz_event WHERE status IN ('PENDING_AUDIT','IN_AUDIT','WAITING_DISPATCH')"));
        // 工单中心：待接单 + 处理中
        badges.put("workOrdersPending", countBy("SELECT COUNT(*) FROM biz_work_order WHERE status IN ('WAITING_ACCEPT','PROCESSING')"));
        // 审核中心：待审核 + 审核中（与 AuditController 默认查询一致）
        badges.put("auditsPending", countBy("SELECT COUNT(*) FROM biz_event WHERE status IN ('PENDING_AUDIT','IN_AUDIT')"));
        // 居民上报：待审核
        badges.put("residentReportsPending", countBy("SELECT COUNT(*) FROM cmn_resident_report WHERE status = 'PENDING'"));
        return ApiResponse.ok(badges);
    }

    private long countBy(String sql) {
        Long c = jdbcTemplate.queryForObject(sql, Long.class);
        return c != null ? c : 0L;
    }

    private void requireDashboardPermission() {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.requireAny(PermissionCodes.MENU_COMMUNITY_DASHBOARD, PermissionCodes.MENU_BIG_SCREEN_VIEW);
    }
}
