package com.changping.platform.modules.parking.controller;

import com.changping.platform.common.exception.BusinessException;
import com.changping.platform.common.response.ApiResponse;
import com.changping.platform.modules.auth.security.PermissionCodes;
import com.changping.platform.modules.auth.security.PermissionGuard;
import com.changping.platform.modules.auth.service.AuthService;
import com.changping.platform.modules.auth.service.CurrentUserService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/parking")
public class ParkingController {

    private final JdbcTemplate jdbcTemplate;
    private final CurrentUserService currentUserService;
    private final PermissionGuard permissionGuard;

    public ParkingController(JdbcTemplate jdbcTemplate, CurrentUserService currentUserService, PermissionGuard permissionGuard) {
        this.jdbcTemplate = jdbcTemplate;
        this.currentUserService = currentUserService;
        this.permissionGuard = permissionGuard;
    }

    /**
     * 车位统计
     */
    @GetMapping("/statistics")
    public ApiResponse<Map<String, Object>> statistics() {
        requireParkingViewPermission();
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
        requireParkingViewPermission();
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
        requireParkingViewPermission();
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
        requireParkingViewPermission();
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
        requireParkingManagePermission();
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
        requireParkingManagePermission();
        String status = body.get("status");
        jdbcTemplate.update("UPDATE biz_parking_space SET status = ?, updated_at = NOW() WHERE id = ?", status, id);
        return ApiResponse.ok(true);
    }

    /**
     * 添加违停记录
     */
    @PostMapping("/violations")
    public ApiResponse<Boolean> addViolation(@RequestBody Map<String, Object> body) {
        requireParkingManagePermission();
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
        requireParkingManagePermission();
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
        requireParkingManagePermission();
        String status = body.get("status");
        if ("CLOSED".equals(status)) {
            jdbcTemplate.update("UPDATE biz_parking_violation SET status = ?, processed_at = NOW(), updated_at = NOW() WHERE id = ?", status, id);
        } else {
            jdbcTemplate.update("UPDATE biz_parking_violation SET status = ?, updated_at = NOW() WHERE id = ?", status, id);
        }
        return ApiResponse.ok(true);
    }

    /**
     * 违停预警：消防通道占用、特殊车位占用等需紧急处置的情况
     */
    @GetMapping("/violation-alerts")
    public ApiResponse<List<Map<String, Object>>> violationAlerts() {
        requireParkingViewPermission();
        // 消防通道占用 + 未处理的严重违停
        List<Map<String, Object>> alerts = jdbcTemplate.queryForList(
                "SELECT v.id, v.vehicle_plate as vehiclePlate, v.violation_type as violationType, " +
                "v.address, v.occurred_at as occurredAt, v.status, g.grid_name as gridName, " +
                "CASE WHEN v.violation_type = 'FIRE_LANE' THEN 'HIGH' ELSE 'MEDIUM' END as priority " +
                "FROM biz_parking_violation v " +
                "LEFT JOIN biz_parking_space ps ON ps.id = v.space_id " +
                "LEFT JOIN cmn_grid g ON g.id = ps.grid_id " +
                "WHERE v.status IN ('PENDING', 'DISPATCHED') " +
                "AND (v.violation_type = 'FIRE_LANE' OR ps.space_type IN ('FIRE_LANE','DISABLED','CHARGING')) " +
                "ORDER BY FIELD(v.violation_type, 'FIRE_LANE') DESC, v.occurred_at DESC LIMIT 50");
        return ApiResponse.ok(alerts);
    }

    /**
     * 车位实时查询：按网格/类型/状态聚合
     */
    @GetMapping("/spaces-realtime")
    public ApiResponse<Map<String, Object>> spacesRealtime(
            @RequestParam(required = false) Long gridId,
            @RequestParam(required = false) String spaceType) {
        requireParkingViewPermission();
        StringBuilder sql = new StringBuilder(
                "SELECT ps.*, g.grid_name FROM biz_parking_space ps LEFT JOIN cmn_grid g ON g.id = ps.grid_id WHERE 1=1");
        List<Object> params = new ArrayList<>();
        if (gridId != null) {
            sql.append(" AND ps.grid_id = ?");
            params.add(gridId);
        }
        if (spaceType != null && !spaceType.isEmpty()) {
            sql.append(" AND ps.space_type = ?");
            params.add(spaceType);
        }
        sql.append(" ORDER BY ps.space_code");

        List<Map<String, Object>> list = jdbcTemplate.queryForList(sql.toString(), params.toArray());

        // 汇总统计
        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("total", list.size());
        result.put("free", list.stream().filter(m -> "FREE".equals(m.get("status"))).count());
        result.put("occupied", list.stream().filter(m -> "OCCUPIED".equals(m.get("status"))).count());
        return ApiResponse.ok(result);
    }

    /**
     * 违停联动工单：将违停记录转为工单，派发给网格员处置
     */
    @PostMapping("/violations/{id}/link-workorder")
    public ApiResponse<Map<String, Object>> linkWorkOrder(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        requireParkingManagePermission();
        Map<String, Object> violation = jdbcTemplate.queryForMap(
                "SELECT * FROM biz_parking_violation WHERE id = ?", id);
        if (violation == null) {
            throw new BusinessException("VALIDATION_ERROR", "违停记录不存在");
        }
        Long dispatcherId = body.get("dispatcherId") == null ? null : ((Number) body.get("dispatcherId")).longValue();
        if (dispatcherId == null) {
            throw new BusinessException("VALIDATION_ERROR", "请选择派单网格员");
        }

        // 创建事件
        String eventCode = "EVT-" + System.currentTimeMillis();
        String externalId = "PARKING-" + id;
        jdbcTemplate.update(
                "INSERT INTO biz_event (event_code, external_event_id, source_type, source_system, event_type, title, description, incident_address, status, created_at, updated_at) " +
                "VALUES (?, ?, 'PARKING', 'PARKING_VIOLATION', 'ILLEGAL_PARKING', ?, ?, ?, 'WAITING_DISPATCH', NOW(), NOW())",
                eventCode, externalId,
                "违停处置：" + violation.get("violation_type"),
                "车牌 " + violation.get("vehicle_plate") + " 在 " + violation.get("address") + " 违停，请前往处置",
                violation.get("address"));
        Long eventId = jdbcTemplate.queryForObject("SELECT id FROM biz_event WHERE event_code = ?", Long.class, eventCode);

        // 更新违停状态为已派单，记录关联
        jdbcTemplate.update(
                "UPDATE biz_parking_violation SET status = 'DISPATCHED', dispatcher_id = ?, remark = CONCAT(IFNULL(remark,''), ' [已联动工单事件', ?, ']', ''), updated_at = NOW() WHERE id = ?",
                dispatcherId, eventId, id);

        Map<String, Object> result = new HashMap<>();
        result.put("eventId", eventId);
        result.put("eventCode", eventCode);
        result.put("message", "已创建关联工单事件，可在事件中心派单处置");
        return ApiResponse.ok(result);
    }

    private void requireParkingViewPermission() {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.API_PARKING_VIEW);
    }

    private void requireParkingManagePermission() {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.API_PARKING_MANAGE);
    }
}
