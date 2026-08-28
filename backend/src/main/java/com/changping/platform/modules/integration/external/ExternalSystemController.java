package com.changping.platform.modules.integration.external;

import com.changping.platform.common.response.ApiResponse;
import com.changping.platform.common.response.PagedResult;
import com.changping.platform.modules.auth.security.PermissionCodes;
import com.changping.platform.modules.auth.security.PermissionGuard;
import com.changping.platform.modules.auth.service.AuthService;
import com.changping.platform.modules.auth.service.CurrentUserService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 信息互通 - 外部系统对接管理控制器
 */
@RestController
@RequestMapping("/integration/systems")
public class ExternalSystemController {

    private final JdbcTemplate jdbcTemplate;
    private final PermissionGuard permissionGuard;
    private final CurrentUserService currentUserService;
    private final ExternalSystemSyncService syncService;

    public ExternalSystemController(JdbcTemplate jdbcTemplate, PermissionGuard permissionGuard,
                                     CurrentUserService currentUserService, ExternalSystemSyncService syncService) {
        this.jdbcTemplate = jdbcTemplate;
        this.permissionGuard = permissionGuard;
        this.currentUserService = currentUserService;
        this.syncService = syncService;
    }

    /**
     * 查询外部系统列表
     */
    @GetMapping
    public ApiResponse<PagedResult<Map<String, Object>>> listSystems(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String keyword) {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.API_INTEGRATION_VIEW);

        int offset = (page - 1) * pageSize;
        StringBuilder where = new StringBuilder(" WHERE 1=1");
        List<Object> params = new java.util.ArrayList<>();

        if (keyword != null && !keyword.isBlank()) {
            where.append(" AND (system_code LIKE ? OR system_name LIKE ?)");
            params.add("%" + keyword + "%");
            params.add("%" + keyword + "%");
        }

        Long total = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM biz_external_system" + where, Long.class, params.toArray());

        List<Object> queryParams = new java.util.ArrayList<>(params);
        queryParams.add(pageSize);
        queryParams.add(offset);
        List<Map<String, Object>> items = jdbcTemplate.queryForList(
            "SELECT id, system_code, system_name, system_type, api_base_url, sync_enabled, " +
            "last_sync_at, last_sync_status, status, remark, created_at, updated_at " +
            "FROM biz_external_system" + where + " ORDER BY id ASC LIMIT ? OFFSET ?",
            queryParams.toArray());

        return ApiResponse.ok(new PagedResult<>(items, total != null ? total : 0, page, pageSize));
    }

    /**
     * 获取单个外部系统详情
     */
    @GetMapping("/{id}")
    public ApiResponse<Map<String, Object>> getSystem(@PathVariable Long id) {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.API_INTEGRATION_VIEW);

        List<Map<String, Object>> list = jdbcTemplate.queryForList(
            "SELECT * FROM biz_external_system WHERE id = ?", id);
        if (list.isEmpty()) {
            return ApiResponse.fail("NOT_FOUND", "系统不存在");
        }
        return ApiResponse.ok(list.get(0));
    }

    /**
     * 创建外部系统配置
     */
    @PostMapping
    public ApiResponse<Map<String, Object>> createSystem(@RequestBody Map<String, Object> body) {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.API_INTEGRATION_MANAGE);

        String systemCode = (String) body.get("systemCode");
        String systemName = (String) body.get("systemName");
        String systemType = (String) body.getOrDefault("systemType", "API");

        if (systemCode == null || systemCode.isBlank()) {
            return ApiResponse.fail("VALIDATION_ERROR", "系统编码不能为空");
        }

        jdbcTemplate.update(
            "INSERT INTO biz_external_system (system_code, system_name, system_type, api_base_url, remark, status) " +
            "VALUES (?, ?, ?, ?, ?, 'ACTIVE')",
            systemCode, systemName, systemType,
            body.get("apiBaseUrl"), body.get("remark"));

        Long id = jdbcTemplate.queryForObject(
            "SELECT id FROM biz_external_system WHERE system_code = ?", Long.class, systemCode);

        Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("id", id);
        result.put("message", "创建成功");
        return ApiResponse.ok(result);
    }

    /**
     * 更新外部系统配置
     */
    @PutMapping("/{id}")
    public ApiResponse<Map<String, Object>> updateSystem(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.API_INTEGRATION_MANAGE);

        Boolean syncEnabled = (Boolean) body.get("syncEnabled");
        jdbcTemplate.update(
            "UPDATE biz_external_system SET system_name = ?, system_type = ?, api_base_url = ?, " +
            "remark = ?, sync_enabled = ? WHERE id = ?",
            body.get("systemName"), body.getOrDefault("systemType", "API"),
            body.get("apiBaseUrl"), body.get("remark"),
            syncEnabled != null ? (syncEnabled ? 1 : 0) : 0, id);

        Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("message", "更新成功");
        return ApiResponse.ok(result);
    }

    /**
     * 删除外部系统配置
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteSystem(@PathVariable Long id) {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.API_INTEGRATION_MANAGE);

        jdbcTemplate.update("DELETE FROM biz_external_system WHERE id = ?", id);
        return ApiResponse.ok(null);
    }

    /**
     * 手动触发同步
     */
    @PostMapping("/{id}/sync")
    public ApiResponse<Map<String, Object>> triggerSync(@PathVariable Long id) {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.API_INTEGRATION_MANAGE);

        Map<String, Object> system = getSystemById(id);
        if (system == null) {
            return ApiResponse.fail("NOT_FOUND", "系统不存在");
        }

        Map<String, Object> syncResult = syncService.syncSystem((String) system.get("system_code"));

        Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("syncResult", syncResult);
        result.put("message", "同步完成");
        return ApiResponse.ok(result);
    }

    /**
     * 获取同步日志
     */
    @GetMapping("/sync-logs")
    public ApiResponse<PagedResult<Map<String, Object>>> getSyncLogs(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String systemCode) {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.API_INTEGRATION_VIEW);

        int offset = (page - 1) * pageSize;
        StringBuilder where = new StringBuilder(" WHERE 1=1");
        List<Object> params = new java.util.ArrayList<>();

        if (systemCode != null && !systemCode.isBlank()) {
            where.append(" AND system_code = ?");
            params.add(systemCode);
        }

        Long total = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM biz_sync_log" + where, Long.class, params.toArray());

        List<Object> queryParams = new java.util.ArrayList<>(params);
        queryParams.add(pageSize);
        queryParams.add(offset);
        List<Map<String, Object>> items = jdbcTemplate.queryForList(
            "SELECT id, system_code, sync_type, sync_action, records_total, records_success, " +
            "records_failed, status, error_message, started_at, finished_at " +
            "FROM biz_sync_log" + where + " ORDER BY id DESC LIMIT ? OFFSET ?",
            queryParams.toArray());

        return ApiResponse.ok(new PagedResult<>(items, total != null ? total : 0, page, pageSize));
    }

    /**
     * 获取同步统计数据
     */
    @GetMapping("/statistics")
    public ApiResponse<Map<String, Object>> getStatistics() {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.API_INTEGRATION_VIEW);

        Map<String, Object> stats = new java.util.LinkedHashMap<>();

        // 系统总数
        Long totalSystems = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM biz_external_system", Long.class);
        stats.put("totalSystems", totalSystems);

        // 启用同步的系统数
        Long enabledSystems = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM biz_external_system WHERE sync_enabled = 1", Long.class);
        stats.put("enabledSystems", enabledSystems);

        // 总同步次数
        Long totalSyncs = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM biz_sync_log", Long.class);
        stats.put("totalSyncs", totalSyncs);

        // 今日同步成功数
        Long todaySuccess = jdbcTemplate.queryForObject(
            "SELECT COALESCE(SUM(records_success), 0) FROM biz_sync_log WHERE status = 'SUCCESS' " +
            "AND DATE(started_at) = CURDATE()", Long.class);
        stats.put("todaySuccessRecords", todaySuccess);

        // 各系统同步状态
        List<Map<String, Object>> systemStatuses = jdbcTemplate.queryForList(
            "SELECT system_code, system_name, last_sync_at, last_sync_status FROM biz_external_system ORDER BY id");
        stats.put("systemStatuses", systemStatuses);

        return ApiResponse.ok(stats);
    }

    /**
     * 健康检查 - 检查所有外部系统连接状态
     */
    @GetMapping("/health-check")
    public ApiResponse<Map<String, Object>> healthCheck() {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.API_INTEGRATION_VIEW);

        List<Map<String, Object>> systems = jdbcTemplate.queryForList(
            "SELECT id, system_code, system_name, api_base_url, status FROM biz_external_system WHERE status = 'ACTIVE'");

        int healthy = 0, unhealthy = 0;
        List<Map<String, Object>> details = new java.util.ArrayList<>();

        for (Map<String, Object> sys : systems) {
            String apiUrl = (String) sys.get("api_base_url");
            Map<String, Object> detail = new java.util.LinkedHashMap<>();
            detail.put("systemCode", sys.get("system_code"));
            detail.put("systemName", sys.get("system_name"));

            if (apiUrl != null && !apiUrl.isBlank()) {
                try {
                    java.net.HttpURLConnection conn = (java.net.HttpURLConnection) new java.net.URL(apiUrl).openConnection();
                    conn.setRequestMethod("HEAD");
                    conn.setConnectTimeout(5000);
                    conn.setReadTimeout(5000);
                    int code = conn.getResponseCode();
                    detail.put("reachable", code < 500);
                    detail.put("statusCode", code);
                    if (code < 500) healthy++; else unhealthy++;
                } catch (Exception e) {
                    detail.put("reachable", false);
                    detail.put("error", e.getMessage());
                    unhealthy++;
                }
            } else {
                detail.put("reachable", null);
                detail.put("error", "未配置API地址");
            }
            details.add(detail);
        }

        Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("total", systems.size());
        result.put("healthy", healthy);
        result.put("unhealthy", unhealthy);
        result.put("details", details);
        return ApiResponse.ok(result);
    }

    private Map<String, Object> getSystemById(Long id) {
        List<Map<String, Object>> list = jdbcTemplate.queryForList(
            "SELECT * FROM biz_external_system WHERE id = ?", id);
        return list.isEmpty() ? null : list.get(0);
    }
}
