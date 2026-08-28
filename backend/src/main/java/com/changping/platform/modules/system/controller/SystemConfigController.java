package com.changping.platform.modules.system.controller;

import com.changping.platform.common.response.ApiResponse;
import com.changping.platform.modules.auth.security.PermissionCodes;
import com.changping.platform.modules.auth.security.PermissionGuard;
import com.changping.platform.modules.auth.service.AuthService;
import com.changping.platform.modules.auth.service.CurrentUserService;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 系统配置（key-value）：
 * 当前支持地图中心点 map.center.lng / map.center.lat，
 * 地图类页面（看板/GIS/大屏）初始化时读取作为默认中心。
 */
@RestController
@RequestMapping("/system/config")
public class SystemConfigController {

    private final JdbcTemplate jdbcTemplate;
    private final CurrentUserService currentUserService;
    private final PermissionGuard permissionGuard;

    public SystemConfigController(JdbcTemplate jdbcTemplate,
                                  CurrentUserService currentUserService,
                                  PermissionGuard permissionGuard) {
        this.jdbcTemplate = jdbcTemplate;
        this.currentUserService = currentUserService;
        this.permissionGuard = permissionGuard;
    }

    /** 查询全部配置 */
    @GetMapping
    public ApiResponse<List<Map<String, Object>>> listConfigs() {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.API_SYSTEM_ROLE_LIST);
        return ApiResponse.ok(jdbcTemplate.queryForList(
                "SELECT config_key AS configKey, config_value AS configValue, remark, updated_at AS updatedAt FROM sys_config ORDER BY config_key"));
    }

    /** 读取单个配置（地图页初始化使用，登录即可读） */
    @GetMapping("/{key}")
    public ApiResponse<String> getConfig(@PathVariable String key) {
        List<String> values = jdbcTemplate.queryForList(
                "SELECT config_value FROM sys_config WHERE config_key = ?", String.class, key);
        return ApiResponse.ok(values.isEmpty() ? "" : values.get(0));
    }

    /** 更新配置值（不存在则插入） */
    @PutMapping("/{key}")
    public ApiResponse<Boolean> updateConfig(@PathVariable String key, @RequestBody Map<String, String> body) {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.API_SYSTEM_ROLE_LIST);
        String value = body.getOrDefault("value", "");
        jdbcTemplate.update(
                "INSERT INTO sys_config (config_key, config_value, updated_at) VALUES (?, ?, NOW()) " +
                "ON DUPLICATE KEY UPDATE config_value = ?, updated_at = NOW()",
                key, value, value);
        return ApiResponse.ok(true);
    }
}
