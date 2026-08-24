package com.changping.platform.modules.drone.controller;

import com.changping.platform.common.response.ApiResponse;
import com.changping.platform.modules.auth.security.PermissionCodes;
import com.changping.platform.modules.auth.security.PermissionGuard;
import com.changping.platform.modules.auth.service.AuthService;
import com.changping.platform.modules.auth.service.CurrentUserService;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 无人机本地设备档案 CRUD：
 * 手工维护设备台账（名称/型号/SN/连接地址），用于多设备管理与展示；
 * 真实飞行/视频流仍走大疆三方平台，本地档案仅作台账与任务绑定参考。
 */
@RestController
@RequestMapping("/drone/devices/local")
public class DroneDeviceLocalController {

    private final JdbcTemplate jdbcTemplate;
    private final CurrentUserService currentUserService;
    private final PermissionGuard permissionGuard;

    public DroneDeviceLocalController(JdbcTemplate jdbcTemplate,
                                      CurrentUserService currentUserService,
                                      PermissionGuard permissionGuard) {
        this.jdbcTemplate = jdbcTemplate;
        this.currentUserService = currentUserService;
        this.permissionGuard = permissionGuard;
    }

    @GetMapping
    public ApiResponse<List<Map<String, Object>>> list() {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.API_DRONE_DEVICE_LIST);
        return ApiResponse.ok(jdbcTemplate.queryForList(
                "SELECT id, device_name AS deviceName, model, sn, dock_sn AS dockSn, connect_addr AS connectAddr, status, remark, created_at AS createdAt FROM drone_device_local ORDER BY id DESC"));
    }

    @PostMapping
    public ApiResponse<Boolean> create(@RequestBody Map<String, Object> body) {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.API_DRONE_DEVICE_LIST);
        String name = body.get("deviceName") == null ? "" : String.valueOf(body.get("deviceName")).trim();
        if (name.isEmpty()) {
            return ApiResponse.fail("DEVICE_NAME_REQUIRED", "设备名称不能为空");
        }
        jdbcTemplate.update(
                "INSERT INTO drone_device_local (device_name, model, sn, dock_sn, connect_addr, status, remark) VALUES (?, ?, ?, ?, ?, ?, ?)",
                name, body.get("model"), body.get("sn"), body.get("dockSn"), body.get("connectAddr"),
                body.getOrDefault("status", "ACTIVE"), body.get("remark"));
        return ApiResponse.ok(true);
    }

    @PutMapping("/{id}")
    public ApiResponse<Boolean> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.API_DRONE_DEVICE_LIST);
        jdbcTemplate.update(
                "UPDATE drone_device_local SET device_name = ?, model = ?, sn = ?, dock_sn = ?, connect_addr = ?, status = ?, remark = ?, updated_at = NOW() WHERE id = ?",
                body.get("deviceName"), body.get("model"), body.get("sn"), body.get("dockSn"), body.get("connectAddr"),
                body.getOrDefault("status", "ACTIVE"), body.get("remark"), id);
        return ApiResponse.ok(true);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Boolean> delete(@PathVariable Long id) {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.API_DRONE_DEVICE_LIST);
        jdbcTemplate.update("DELETE FROM drone_device_local WHERE id = ?", id);
        return ApiResponse.ok(true);
    }
}
