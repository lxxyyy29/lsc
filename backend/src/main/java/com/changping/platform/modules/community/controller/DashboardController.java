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
import org.springframework.web.bind.annotation.RestController;

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

    private void requireDashboardPermission() {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.requireAny(PermissionCodes.MENU_COMMUNITY_DASHBOARD, PermissionCodes.MENU_BIG_SCREEN_VIEW);
    }
}
