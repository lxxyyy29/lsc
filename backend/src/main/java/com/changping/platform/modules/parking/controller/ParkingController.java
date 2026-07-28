package com.changping.platform.modules.parking.controller;

import com.changping.platform.common.response.ApiResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/parking")
public class ParkingController {

    private final JdbcTemplate jdbcTemplate;

    public ParkingController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 车位统计
     */
    @GetMapping("/statistics")
    public ApiResponse<Map<String, Object>> statistics() {
        Map<String, Object> result = new HashMap<>();
        Long total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM biz_parking_space", Long.class);
        Long free = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM biz_parking_space WHERE status = 'FREE'", Long.class);
        Long occupied = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM biz_parking_space WHERE status = 'OCCUPIED'", Long.class);
        Long fireLane = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM biz_parking_space WHERE space_type = 'FIRE_LANE'", Long.class);

        result.put("total", total != null ? total : 0);
        result.put("free", free != null ? free : 0);
        result.put("occupied", occupied != null ? occupied : 0);
        result.put("fireLane", fireLane != null ? fireLane : 0);
        return ApiResponse.ok(result);
    }

    /**
     * 车位列表
     */
    @GetMapping("/spaces")
    public ApiResponse<List<Map<String, Object>>> spaces(@RequestParam(required = false) String status) {
        StringBuilder sql = new StringBuilder(
            "SELECT ps.*, g.grid_name FROM biz_parking_space ps LEFT JOIN cmn_grid g ON g.id = ps.grid_id WHERE 1=1");
        List<Object> params = new ArrayList<>();
        if (status != null && !status.isEmpty()) {
            sql.append(" AND ps.status = ?");
            params.add(status);
        }
        sql.append(" ORDER BY ps.space_code");
        return ApiResponse.ok(jdbcTemplate.queryForList(sql.toString(), params.toArray()));
    }

    /**
     * 违停记录
     */
    @GetMapping("/violations")
    public ApiResponse<List<Map<String, Object>>> violations(@RequestParam(required = false) String status) {
        StringBuilder sql = new StringBuilder(
            "SELECT v.*, g.grid_name, u.real_name as dispatcher_name " +
            "FROM biz_parking_violation v " +
            "LEFT JOIN cmn_grid g ON g.id = v.space_id " +
            "LEFT JOIN sys_user u ON u.id = v.dispatcher_id WHERE 1=1");
        List<Object> params = new ArrayList<>();
        if (status != null && !status.isEmpty()) {
            sql.append(" AND v.status = ?");
            params.add(status);
        }
        sql.append(" ORDER BY v.occurred_at DESC");
        return ApiResponse.ok(jdbcTemplate.queryForList(sql.toString(), params.toArray()));
    }

    /**
     * 违停统计
     */
    @GetMapping("/violation-stats")
    public ApiResponse<Map<String, Object>> violationStats() {
        Map<String, Object> result = new HashMap<>();
        Long total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM biz_parking_violation", Long.class);
        Long pending = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM biz_parking_violation WHERE status = 'PENDING'", Long.class);
        Long dispatched = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM biz_parking_violation WHERE status = 'DISPATCHED'", Long.class);
        Long closed = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM biz_parking_violation WHERE status = 'CLOSED'", Long.class);

        result.put("total", total != null ? total : 0);
        result.put("pending", pending != null ? pending : 0);
        result.put("dispatched", dispatched != null ? dispatched : 0);
        result.put("closed", closed != null ? closed : 0);

        // 按类型统计
        List<Map<String, Object>> byType = jdbcTemplate.queryForList(
            "SELECT violation_type as type, COUNT(*) as count FROM biz_parking_violation GROUP BY violation_type");
        result.put("byType", byType);

        return ApiResponse.ok(result);
    }

    /**
     * 添加车位
     */
    @PostMapping("/spaces")
    public ApiResponse<Boolean> addSpace(@RequestBody Map<String, Object> body) {
        jdbcTemplate.update(
            "INSERT INTO biz_parking_space (space_code, space_type, grid_id, longitude, latitude, address, status) VALUES (?, ?, ?, ?, ?, ?, 'FREE')",
            body.get("spaceCode"), body.get("spaceType"), body.get("gridId"),
            body.get("longitude"), body.get("latitude"), body.get("address"));
        return ApiResponse.ok(true);
    }

    /**
     * 更新车位状态
     */
    @PutMapping("/spaces/{id}/status")
    public ApiResponse<Boolean> updateSpaceStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String status = body.get("status");
        jdbcTemplate.update("UPDATE biz_parking_space SET status = ?, updated_at = NOW() WHERE id = ?", status, id);
        return ApiResponse.ok(true);
    }

    /**
     * 添加违停记录
     */
    @PostMapping("/violations")
    public ApiResponse<Boolean> addViolation(@RequestBody Map<String, Object> body) {
        jdbcTemplate.update(
            "INSERT INTO biz_parking_violation (space_id, vehicle_plate, violation_type, longitude, latitude, address, photo_url, status, occurred_at) VALUES (?, ?, ?, ?, ?, ?, ?, 'PENDING', NOW())",
            body.get("spaceId"), body.get("vehiclePlate"), body.get("violationType"),
            body.get("longitude"), body.get("latitude"), body.get("address"), body.get("photoUrl"));
        return ApiResponse.ok(true);
    }

    /**
     * 派单处理违停
     */
    @PostMapping("/violations/{id}/dispatch")
    public ApiResponse<Boolean> dispatchViolation(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        jdbcTemplate.update(
            "UPDATE biz_parking_violation SET status = 'DISPATCHED', dispatcher_id = ?, remark = ?, updated_at = NOW() WHERE id = ?",
            body.get("dispatcherId"), body.get("remark"), id);
        return ApiResponse.ok(true);
    }

    /**
     * 更新违停状态
     */
    @PutMapping("/violations/{id}/status")
    public ApiResponse<Boolean> updateViolationStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String status = body.get("status");
        if ("CLOSED".equals(status)) {
            jdbcTemplate.update("UPDATE biz_parking_violation SET status = ?, processed_at = NOW(), updated_at = NOW() WHERE id = ?", status, id);
        } else {
            jdbcTemplate.update("UPDATE biz_parking_violation SET status = ?, updated_at = NOW() WHERE id = ?", status, id);
        }
        return ApiResponse.ok(true);
    }
}
