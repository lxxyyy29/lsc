package com.changping.platform.modules.community.controller;

import com.changping.platform.common.response.ApiResponse;
import com.changping.platform.modules.auth.model.AuthenticatedUser;
import com.changping.platform.modules.auth.service.AuthService;
import com.changping.platform.modules.auth.service.CurrentUserService;
import com.changping.platform.modules.community.entity.PatrolRecordEntity;
import com.changping.platform.modules.community.service.PatrolRecordService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/community/patrol-records")
public class PatrolRecordController {

    private final PatrolRecordService service;
    private final CurrentUserService currentUserService;
    private final JdbcTemplate jdbcTemplate;

    public PatrolRecordController(PatrolRecordService service, CurrentUserService currentUserService, JdbcTemplate jdbcTemplate) {
        this.service = service;
        this.currentUserService = currentUserService;
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/h5")
    public ApiResponse<List<PatrolRecordEntity>> listH5() {
        AuthenticatedUser user = currentUserService.requireClientType(AuthService.ClientType.H5);
        return ApiResponse.ok(service.listByUser(user.id()));
    }

    @GetMapping
    public ApiResponse<List<PatrolRecordEntity>> listAll() {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        return ApiResponse.ok(service.listAll());
    }

    @PostMapping
    public ApiResponse<Boolean> create(@RequestBody PatrolRecordEntity entity) {
        AuthenticatedUser user = currentUserService.requireClientType(AuthService.ClientType.H5);
        entity.setUserId(user.id());
        if (entity.getPatrolType() == null) entity.setPatrolType("NORMAL");
        if (entity.getStatus() == null) entity.setStatus("NORMAL");
        return ApiResponse.ok(service.create(entity));
    }

    /**
     * 巡查轨迹：按用户分组返回有坐标的巡查记录，用于地图轨迹绘制
     */
    @GetMapping("/trajectories")
    public ApiResponse<List<Map<String, Object>>> trajectories(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) Long userId) {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        StringBuilder sql = new StringBuilder(
            "SELECT r.id, r.user_id, u.real_name as userName, r.grid_id, g.grid_name as gridName, " +
            "r.patrol_type, r.content, r.address, r.status, " +
            "CAST(r.longitude AS DECIMAL(10,6)) as lng, CAST(r.latitude AS DECIMAL(10,6)) as lat, " +
            "r.created_at " +
            "FROM cmn_patrol_record r " +
            "LEFT JOIN sys_user u ON u.id = r.user_id " +
            "LEFT JOIN cmn_grid g ON g.id = r.grid_id " +
            "WHERE r.longitude IS NOT NULL AND r.latitude IS NOT NULL");
        List<Object> params = new ArrayList<>();
        if (userId != null) { sql.append(" AND r.user_id = ?"); params.add(userId); }
        if (startDate != null && !startDate.isEmpty()) { sql.append(" AND r.created_at >= ?"); params.add(startDate); }
        if (endDate != null && !endDate.isEmpty()) { sql.append(" AND r.created_at <= ?"); params.add(endDate); }
        sql.append(" ORDER BY r.user_id, r.created_at ASC");
        List<Map<String, Object>> points = jdbcTemplate.queryForList(sql.toString(), params.toArray());

        // 按用户分组为轨迹
        Map<Long, Map<String, Object>> grouped = new LinkedHashMap<>();
        for (Map<String, Object> p : points) {
            Long uid = toLong(p.get("user_id"));
            if (uid == null) continue;
            Map<String, Object> track = grouped.computeIfAbsent(uid, k -> {
                Map<String, Object> m = new HashMap<>();
                m.put("userId", k);
                m.put("userName", p.get("userName"));
                m.put("coords", new ArrayList<>());
                return m;
            });
            @SuppressWarnings("unchecked")
            List<List<Double>> coords = (List<List<Double>>) track.get("coords");
            coords.add(List.of(toDouble(p.get("lng")), toDouble(p.get("lat"))));
        }
        return ApiResponse.ok(new ArrayList<>(grouped.values()));
    }

    private Long toLong(Object o) {
        if (o == null) return null;
        if (o instanceof Number) return ((Number) o).longValue();
        try { return Long.parseLong(o.toString()); } catch (NumberFormatException e) { return null; }
    }

    private Double toDouble(Object o) {
        if (o == null) return null;
        if (o instanceof Number) return ((Number) o).doubleValue();
        try { return Double.parseDouble(o.toString()); } catch (NumberFormatException e) { return null; }
    }
}
