package com.changping.platform.modules.system.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 内置管理端角色 API 权限兜底（第 1 步：止血）。
 *
 * 背景：新版本上线新增 API 权限后，内置「管理员」角色常常没有同步授予，
 * 导致管理员账号访问新页面时报「当前用户没有所需权限: api:xxx:list」——
 * 与 V130 给 SUPER_ADMIN 做全量兜底的动机相同。
 *
 * 与一次性迁移的区别：Flyway 迁移只执行一次，无法覆盖"以后新增的接口权限"；
 * 本组件在**每次启动**都把「当前存在但角色未授予」的 API 权限补齐，因此能长期防复发。
 *
 * 范围严格限定：
 *   - 仅内置管理端角色（EVENT_OPERATOR）；
 *   - 仅 api:% 权限，且**排除 api:system:%**（系统管理类保留超管专属）；
 *   - 幂等（只插入缺失的授权）。
 *
 * 自定义角色不在此兜底范围内 —— 其权限应由「角色管理」按菜单精确授予（第 2 步）。
 */
@Component
@Order(20)
public class AdminRolePermissionFallback implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminRolePermissionFallback.class);

    /** 需要兜底的**内置**管理端角色编码 */
    private static final List<String> BUILTIN_ADMIN_ROLE_CODES = List.of("EVENT_OPERATOR");

    private static final String GRANT_SQL =
            "INSERT INTO sys_role_permission (role_id, permission_id, created_at, updated_at) "
                    + "SELECT r.id, p.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP "
                    + "FROM sys_role r "
                    + "JOIN sys_permission p ON p.permission_code LIKE 'api:%' "
                    + "     AND p.permission_code NOT LIKE 'api:system:%' "
                    + "     AND p.status = 'ACTIVE' "
                    + "LEFT JOIN sys_role_permission rp ON rp.role_id = r.id AND rp.permission_id = p.id "
                    + "WHERE r.role_code = ? AND rp.role_id IS NULL";

    private final JdbcTemplate jdbcTemplate;

    public AdminRolePermissionFallback(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        for (String roleCode : BUILTIN_ADMIN_ROLE_CODES) {
            try {
                int granted = jdbcTemplate.update(GRANT_SQL, roleCode);
                if (granted > 0) {
                    log.info("[perm-fallback] 内置管理端角色 {} 补授 {} 个 API 权限（新增接口未同步授予）", roleCode, granted);
                }
            } catch (Exception e) {
                // 兜底失败绝不能影响服务启动；记录后可下次启动重试
                log.warn("[perm-fallback] 角色 {} 权限兜底失败: {}", roleCode, e.getMessage());
            }
        }
    }
}
