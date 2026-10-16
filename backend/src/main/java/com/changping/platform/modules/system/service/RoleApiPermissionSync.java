package com.changping.platform.modules.system.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 角色 API 权限按菜单自动同步（第 2 步：治本）。
 *
 * 问题：角色管理页只维护「菜单权限」，新建的自定义角色拿不到接口权限，
 * 其用户访问页面时报「当前用户没有所需权限: api:xxx:list」。
 *
 * 做法：建立「菜单权限 → API 权限路径前缀」映射，启动时对每个角色按其已授予的菜单
 * 推导出应得的 API 权限并补齐（幂等，只增不减，不会误删管理员手动授予的权限）。
 *
 * 粒度说明：按"模块路径前缀"授权（例如勾选「事件闭环处置」→ 授 /api/events/**），
 * 即该角色对该模块的读写接口均可用，符合"拥有该菜单即需要操作该模块"的直觉；
 * 若日后需要更细粒度（只读/只写分离），可在映射里改为精确 permission_code 列表。
 *
 * 安全边界：只授给角色**已经拥有对应菜单**的模块；未勾选任何系统管理菜单的角色
 * 不会获得 api:system:** 权限。
 */
@Component
@Order(21)
public class RoleApiPermissionSync implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(RoleApiPermissionSync.class);

    /** 菜单权限码 → 应授予的 API 路径前缀（匹配 sys_permission.path） */
    private static final Map<String, List<String>> MENU_TO_API_PREFIXES = buildMapping();

    private static Map<String, List<String>> buildMapping() {
        Map<String, List<String>> m = new LinkedHashMap<>();
        m.put("web:menu:dashboard", List.of("/api/community/dashboard"));
        m.put("web:menu:big-screen", List.of("/api/community/dashboard"));
        m.put("web:menu:gis", List.of("/api/community/grids", "/api/community/dashboard", "/api/community/buildings"));
        m.put("web:menu:events", List.of("/api/events", "/api/event-ratings"));
        m.put("web:menu:audits", List.of("/api/audits", "/api/events"));
        m.put("web:menu:work-orders", List.of("/api/work-orders", "/api/events"));
        m.put("web:menu:abnormal-orders", List.of("/api/work-orders", "/api/events"));
        m.put("web:menu:audit-logs", List.of("/api/audit-logs"));
        m.put("web:menu:drones", List.of("/api/drone", "/api/video"));
        m.put("web:menu:patrol", List.of("/api/patrol", "/api/community/patrol", "/api/community/grids"));
        m.put("web:menu:population", List.of("/api/community/population", "/api/community/households"));
        m.put("web:menu:buildings", List.of("/api/community/buildings"));
        m.put("web:menu:places", List.of("/api/community/places"));
        m.put("web:menu:ledger", List.of("/api/community/places", "/api/community/ledger"));
        m.put("web:menu:resident-reports", List.of("/api/resident", "/api/events"));
        m.put("web:menu:repairs", List.of("/api/resident/repairs", "/api/repairs"));
        m.put("web:menu:policy-resources", List.of("/api/community/policy-resources"));
        m.put("web:menu:party", List.of("/api/community/parties", "/api/party"));
        m.put("web:menu:reports", List.of("/api/assessment", "/api/community/dashboard"));
        m.put("web:menu:assessment", List.of("/api/assessment"));
        m.put("web:menu:trend-alerts", List.of("/api/trend-alerts", "/api/community/dashboard"));
        m.put("web:menu:grid-manage", List.of("/api/community/grids"));
        m.put("web:menu:emergency", List.of("/api/community/emergency", "/api/events"));
        m.put("web:menu:mosquito", List.of("/api/community/mosquito"));
        m.put("web:menu:safety", List.of("/api/community/safety"));
        m.put("web:menu:parking", List.of("/api/community/parking"));
        m.put("web:menu:vehicle-track", List.of("/api/community/vehicle-tracks"));
        m.put("web:menu:system-users", List.of("/api/system/users"));
        m.put("web:menu:system-roles", List.of("/api/system/roles", "/api/system/permissions"));
        m.put("web:menu:system-menus", List.of("/api/system/menus"));
        m.put("web:menu:system-dicts", List.of("/api/system/dicts"));
        m.put("web:menu:system-suppress", List.of("/api/system/suppress-windows"));
        return m;
    }

    private final JdbcTemplate jdbcTemplate;

    public RoleApiPermissionSync(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            List<Map<String, Object>> roles = jdbcTemplate.queryForList(
                    "SELECT r.id AS role_id, r.role_code, m.permission_code AS menu_code "
                            + "FROM sys_role r "
                            + "JOIN sys_role_permission rp ON rp.role_id = r.id "
                            + "JOIN sys_permission m ON m.id = rp.permission_id AND m.permission_code LIKE 'web:menu:%'");

            Map<Long, List<String>> prefixesByRole = new LinkedHashMap<>();
            for (Map<String, Object> row : roles) {
                Long roleId = ((Number) row.get("role_id")).longValue();
                String menuCode = String.valueOf(row.get("menu_code"));
                List<String> prefixes = MENU_TO_API_PREFIXES.get(menuCode);
                if (prefixes == null || prefixes.isEmpty()) {
                    continue;
                }
                prefixesByRole.computeIfAbsent(roleId, k -> new ArrayList<>()).addAll(prefixes);
            }

            int totalGranted = 0;
            for (Map.Entry<Long, List<String>> entry : prefixesByRole.entrySet()) {
                Long roleId = entry.getKey();
                List<String> prefixes = entry.getValue();
                for (String prefix : prefixes) {
                    try {
                        int granted = jdbcTemplate.update(
                                "INSERT INTO sys_role_permission (role_id, permission_id, created_at, updated_at) "
                                        + "SELECT ?, p.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM sys_permission p "
                                        + "LEFT JOIN sys_role_permission rp ON rp.role_id = ? AND rp.permission_id = p.id "
                                        + "WHERE p.permission_code LIKE 'api:%' AND p.status = 'ACTIVE' "
                                        + "AND p.path LIKE ? AND rp.role_id IS NULL",
                                roleId, roleId, prefix + "%");
                        totalGranted += granted;
                    } catch (Exception e) {
                        log.warn("[role-api-sync] 角色 {} 前缀 {} 授权失败: {}", roleId, prefix, e.getMessage());
                    }
                }
            }
            if (totalGranted > 0) {
                log.info("[role-api-sync] 按菜单自动补齐 API 权限 {} 条（角色数 {}）", totalGranted, prefixesByRole.size());
            }
        } catch (Exception e) {
            // 同步失败不影响启动
            log.warn("[role-api-sync] 执行失败: {}", e.getMessage());
        }
    }
}
