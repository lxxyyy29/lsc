package com.changping.platform.modules.community.controller;

import com.changping.platform.common.response.ApiResponse;
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

    public DashboardController(DashboardMapper dashboardMapper, JdbcTemplate jdbcTemplate) {
        this.dashboardMapper = dashboardMapper;
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/overview")
    public ApiResponse<Map<String, Object>> overview() {
        return ApiResponse.ok(dashboardMapper.getOverview());
    }

    @GetMapping("/grid-stats")
    public ApiResponse<Map<String, Object>> gridStats() {
        return ApiResponse.ok(dashboardMapper.getGridStats());
    }

    @GetMapping("/ledger-stats")
    public ApiResponse<Map<String, Object>> ledgerStats() {
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
}
