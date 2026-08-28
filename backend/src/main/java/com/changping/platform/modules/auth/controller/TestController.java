package com.changping.platform.modules.auth.controller;

import com.changping.platform.common.response.ApiResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 测试控制器 - 创建测试用户、修复迁移等开发调试用
 * 仅在 app.test.enabled=true 时启用，生产环境请保持关闭
 */
@RestController
@RequestMapping("/test")
@ConditionalOnProperty(prefix = "app", name = "test-enabled", havingValue = "true", matchIfMissing = false)
public class TestController {

    private final JdbcTemplate jdbcTemplate;

    public TestController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 创建测试用普通群众账号
     */
    @PostMapping("/create-public-user")
    public ApiResponse<Map<String, Object>> createPublicUser() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String hashedPassword = encoder.encode("123456");

        // 检查用户是否已存在
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM sys_user WHERE username = ? AND deleted = 0", Integer.class, "yonghu");
        if (count != null && count > 0) {
            Map<String, Object> result = new HashMap<>();
            result.put("message", "用户已存在");
            result.put("username", "yonghu");
            result.put("password", "123456");
            return ApiResponse.ok(result);
        }

        // 插入用户
        jdbcTemplate.update(
            "INSERT INTO sys_user (username, password_hash, real_name, phone, status, password_version, deleted, created_at, updated_at) VALUES (?, ?, ?, ?, 'ACTIVE', 1, 0, NOW(), NOW())",
            "yonghu", hashedPassword, "普通用户", "13800000000");

        // 获取用户ID
        Long userId = jdbcTemplate.queryForObject("SELECT id FROM sys_user WHERE username = ?", Long.class, "yonghu");

        // 查找普通群众角色（非管理员角色）
        List<Long> roleIds = jdbcTemplate.queryForList(
            "SELECT id FROM sys_role WHERE role_code NOT IN ('SUPER_ADMIN') ORDER BY id LIMIT 1", Long.class);

        // 分配角色
        if (!roleIds.isEmpty()) {
            jdbcTemplate.update("INSERT INTO sys_user_role (user_id, role_id) VALUES (?, ?)", userId, roleIds.get(0));
        }

        Map<String, Object> result = new HashMap<>();
        result.put("userId", userId);
        result.put("username", "yonghu");
        result.put("password", "123456");
        result.put("roleId", roleIds.isEmpty() ? null : roleIds.get(0));
        result.put("message", "创建成功");
        return ApiResponse.ok(result);
    }

    /**
     * 查看所有角色
     */
    @GetMapping("/roles")
    public ApiResponse<List<Map<String, Object>>> listRoles() {
        List<Map<String, Object>> roles = jdbcTemplate.queryForList(
            "SELECT id, role_code, role_name FROM sys_role ORDER BY id");
        return ApiResponse.ok(roles);
    }

    /**
     * 手动修复迁移 - 添加评价字段
     */
    @PostMapping("/fix-migration")
    public ApiResponse<Map<String, Object>> fixMigration() {
        Map<String, Object> result = new HashMap<>();
        try {
            // 1. 删除失败的 V55 记录
            Integer deleted = jdbcTemplate.update("DELETE FROM flyway_schema_history WHERE version = '55'");
            result.put("deleted_v55", deleted);

            // 2. 添加评价字段（如果不存在）
            try {
                jdbcTemplate.queryForObject("SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'biz_event' AND COLUMN_NAME = 'rating'", Integer.class);
                Integer colCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'biz_event' AND COLUMN_NAME = 'rating'", Integer.class);
                if (colCount != null && colCount == 0) {
                    jdbcTemplate.update("ALTER TABLE biz_event ADD COLUMN rating INT DEFAULT NULL");
                    result.put("added_rating", true);
                } else {
                    result.put("rating_exists", true);
                }
            } catch (Exception e) {
                result.put("rating_error", e.getMessage());
            }

            try {
                Integer colCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'biz_event' AND COLUMN_NAME = 'rating_comment'", Integer.class);
                if (colCount != null && colCount == 0) {
                    jdbcTemplate.update("ALTER TABLE biz_event ADD COLUMN rating_comment VARCHAR(500) DEFAULT NULL");
                    result.put("added_rating_comment", true);
                } else {
                    result.put("rating_comment_exists", true);
                }
            } catch (Exception e) {
                result.put("rating_comment_error", e.getMessage());
            }

            try {
                Integer colCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'biz_event' AND COLUMN_NAME = 'rated_at'", Integer.class);
                if (colCount != null && colCount == 0) {
                    jdbcTemplate.update("ALTER TABLE biz_event ADD COLUMN rated_at DATETIME DEFAULT NULL");
                    result.put("added_rated_at", true);
                } else {
                    result.put("rated_at_exists", true);
                }
            } catch (Exception e) {
                result.put("rated_at_error", e.getMessage());
            }

            // 3. 插入 V56 迁移记录
            try {
                jdbcTemplate.update(
                    "INSERT INTO flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success) " +
                    "VALUES ((SELECT COALESCE(MAX(installed_rank), 0) + 1 FROM flyway_schema_history f), '56', 'add event rating', 'SQL', 'V56__add_event_rating.sql', NULL, 'manual', NOW(), 1, 1)");
                result.put("inserted_v56", true);
            } catch (Exception e) {
                result.put("v56_insert_error", e.getMessage());
            }

            // 4. 清理失败的 V58 迁移记录（修复 SQL 语法后可重试）
            try {
                Integer deletedV58 = jdbcTemplate.update(
                    "DELETE FROM flyway_schema_history WHERE version = '58'");
                if (deletedV58 != null && deletedV58 > 0) {
                    result.put("deleted_failed_v58", deletedV58);
                }
            } catch (Exception e) {
                result.put("v58_cleanup_error", e.getMessage());
            }

            result.put("success", true);
            return ApiResponse.ok(result);
        } catch (Exception e) {
            result.put("error", e.getMessage());
            return ApiResponse.ok(result);
        }
    }

    /**
     * 查看 Flyway 迁移状态
     */
    @GetMapping("/flyway-status")
    public ApiResponse<List<Map<String, Object>>> flywayStatus() {
        try {
            // 先查列名
            List<Map<String, Object>> cols = jdbcTemplate.queryForList(
                "SELECT COLUMN_NAME FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'flyway_schema_history' ORDER BY ORDINAL_POSITION");
            List<Map<String, Object>> history = jdbcTemplate.queryForList("SELECT * FROM flyway_schema_history LIMIT 50");
            Map<String, Object> result = new HashMap<>();
            result.put("columns", cols);
            result.put("history", history);
            List<Map<String, Object>> list = new ArrayList<>();
            list.add(result);
            return ApiResponse.ok(list);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            List<Map<String, Object>> list = new ArrayList<>();
            list.add(error);
            return ApiResponse.ok(list);
        }
    }

    /**
     * 修复工单表 CHECK 约束（添加缺失的状态值）
     */
    @PostMapping("/fix-workorder-constraint")
    public ApiResponse<Map<String, Object>> fixWorkOrderConstraint() {
        Map<String, Object> result = new HashMap<>();
        try {
            // 删除旧的 CHECK 约束
            try {
                jdbcTemplate.update("ALTER TABLE biz_work_order DROP CHECK chk_biz_work_order_status");
                result.put("dropped_old_constraint", true);
            } catch (Exception e) {
                result.put("drop_error", e.getMessage());
            }

            // 添加新的 CHECK 约束（包含所有状态）
            jdbcTemplate.update(
                "ALTER TABLE biz_work_order ADD CONSTRAINT chk_biz_work_order_status CHECK (status IN (" +
                "'WAITING_ACCEPT', 'PROCESSING', 'WAITING_VERIFY', 'WAITING_CLOSE_CONFIRM', " +
                "'COMPLETED', 'CLOSED', 'TIMEOUT'))");
            result.put("added_new_constraint", true);

            // 修复 biz_event 表的 CHECK 约束 - 仅删除旧约束，允许所有状态
            try {
                jdbcTemplate.update("ALTER TABLE biz_event DROP CHECK chk_biz_event_status");
                result.put("dropped_event_constraint", true);
            } catch (Exception e) {
                result.put("drop_event_error", e.getMessage());
            }
            // 不添加新约束，避免现有数据违反约束
            result.put("event_constraint_skipped", "现有数据包含多种状态，暂不添加 CHECK 约束");

            result.put("success", true);
        } catch (Exception e) {
            result.put("error", e.getMessage());
        }
        return ApiResponse.ok(result);
    }

    /**
     * 初始化缺失的权限（分配给 SUPER_ADMIN）
     */
    @PostMapping("/init-permissions")
    public ApiResponse<Map<String, Object>> initPermissions() {
        Map<String, Object> result = new HashMap<>();
        List<String> added = new ArrayList<>();

        // 添加确认关闭工单权限
        String[][] newPermissions = {
            {"api:workorder:confirm-close", "确认关闭工单", "API", "WEB", "/api/work-orders/{id}/confirm-close", "136", "确认或驳回关闭工单"},
            {"api:integration:view", "信息互通查看", "API", "WEB", "/api/integration/systems", "140", "查看外部系统配置"},
            {"api:integration:manage", "信息互通管理", "API", "WEB", "/api/integration/systems", "141", "管理外部系统配置"},
            {"menu:integration:view", "信息互通菜单", "MENU", "WEB", "/integration", "100", "信息互通管理菜单入口"}
        };

        for (String[] perm : newPermissions) {
            Integer permCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sys_permission WHERE permission_code = ?", Integer.class, perm[0]);
            if (permCount == null || permCount == 0) {
                jdbcTemplate.update(
                    "INSERT INTO sys_permission (permission_code, permission_name, permission_type, client_type, path, sort_order, status, remark, created_at, updated_at) " +
                    "VALUES (?, ?, ?, ?, ?, ?, 'ACTIVE', ?, NOW(), NOW())",
                    perm[0], perm[1], perm[2], perm[3], perm[4], perm[5], perm[6]);
                added.add(perm[0]);
            }
        }

        // 分配给 SUPER_ADMIN 角色
        List<Long> adminRoleIds = jdbcTemplate.queryForList(
            "SELECT id FROM sys_role WHERE role_code = 'SUPER_ADMIN'", Long.class);
        for (Long roleId : adminRoleIds) {
            for (String[] perm : newPermissions) {
                jdbcTemplate.update(
                    "INSERT IGNORE INTO sys_role_permission (role_id, permission_id, created_at, updated_at) " +
                    "SELECT ?, id, NOW(), NOW() FROM sys_permission WHERE permission_code = ?",
                    roleId, perm[0]);
            }
        }

        result.put("addedPermissions", added);
        result.put("message", "权限初始化完成");
        return ApiResponse.ok(result);
    }

    /**
     * 查看所有用户
     */
    @GetMapping("/users")
    public ApiResponse<List<Map<String, Object>>> listUsers() {
        List<Map<String, Object>> users = jdbcTemplate.queryForList(
            "SELECT u.id, u.username, u.real_name, u.status, GROUP_CONCAT(r.role_code) as roles " +
            "FROM sys_user u LEFT JOIN sys_user_role sur ON sur.user_id = u.id " +
            "LEFT JOIN sys_role r ON r.id = sur.role_id " +
            "WHERE u.deleted = 0 GROUP BY u.id ORDER BY u.id");
        return ApiResponse.ok(users);
    }

    /**
     * 批量创建网格员测试账号（用于测试闭环流程）
     * 密码统一为: 123456
     */
    @PostMapping("/create-grid-workers")
    public ApiResponse<Map<String, Object>> createGridWorkers() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String hashedPassword = encoder.encode("123456");

        // 确保 GRID_WORKER 角色存在
        Integer roleCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM sys_role WHERE role_code = 'GRID_WORKER'", Integer.class);
        if (roleCount == null || roleCount == 0) {
            jdbcTemplate.update(
                "INSERT INTO sys_role (role_code, role_name, status, remark, created_at, updated_at) VALUES ('GRID_WORKER', '网格员', 'ACTIVE', '网格巡查与事件处置人员', NOW(), NOW())");
        }
        Long roleId = jdbcTemplate.queryForObject(
            "SELECT id FROM sys_role WHERE role_code = 'GRID_WORKER'", Long.class);

        // 为 GRID_WORKER 分配 H5 端权限
        jdbcTemplate.update(
            "INSERT IGNORE INTO sys_role_permission (role_id, permission_id, created_at, updated_at) " +
            "SELECT ?, p.id, NOW(), NOW() FROM sys_permission p WHERE p.permission_code IN (" +
            "'menu:h5:workbench:view','menu:h5:workorder:list','api:auth:h5:me'," +
            "'api:h5:workbench:view','api:h5:workorder:list','api:h5:workorder:detail'," +
            "'api:h5:workorder:accept','api:h5:workorder:arrive','api:h5:workorder:handle'," +
            "'api:h5:workorder:verify')",
            roleId);

        // 定义网格员数据
        String[][] workers = {
            {"grid01", "张网格", "13900001001"},
            {"grid02", "李巡查", "13900001002"},
            {"grid03", "王处置", "13900001003"},
            {"grid04", "赵现场", "13900001004"},
            {"grid05", "刘核实", "13900001005"}
        };

        List<Map<String, Object>> created = new ArrayList<>();
        for (String[] w : workers) {
            String username = w[0], realName = w[1], phone = w[2];
            // 检查是否已存在
            Integer exists = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sys_user WHERE username = ? AND deleted = 0", Integer.class, username);
            if (exists != null && exists > 0) {
                Map<String, Object> info = new HashMap<>();
                info.put("username", username);
                info.put("status", "已存在");
                created.add(info);
                continue;
            }
            // 插入用户
            jdbcTemplate.update(
                "INSERT INTO sys_user (username, password_hash, real_name, phone, status, password_version, deleted, created_at, updated_at) " +
                "VALUES (?, ?, ?, ?, 'ACTIVE', 1, 0, NOW(), NOW())",
                username, hashedPassword, realName, phone);
            Long userId = jdbcTemplate.queryForObject(
                "SELECT id FROM sys_user WHERE username = ?", Long.class, username);
            // 分配角色
            jdbcTemplate.update(
                "INSERT IGNORE INTO sys_user_role (user_id, role_id) VALUES (?, ?)", userId, roleId);
            Map<String, Object> info = new HashMap<>();
            info.put("username", username);
            info.put("realName", realName);
            info.put("status", "创建成功");
            created.add(info);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("workers", created);
        result.put("password", "123456");
        result.put("roleId", roleId);
        result.put("message", "网格员账号准备完成");
        return ApiResponse.ok(result);
    }

    /**
     * 群众注册接口
     */
    @PostMapping("/register")
    public ApiResponse<Map<String, Object>> register(
            @RequestBody Map<String, Object> request) {
        String account = (String) request.get("account");
        String password = (String) request.get("password");
        String realName = (String) request.get("realName");
        String phone = (String) request.get("phone");

        if (account == null || account.isBlank() || password == null || password.isBlank()) {
            return ApiResponse.fail("VALIDATION_ERROR", "账号和密码不能为空");
        }
        if (account.length() < 4) {
            return ApiResponse.fail("VALIDATION_ERROR", "账号至少4位");
        }
        if (password.length() < 6) {
            return ApiResponse.fail("VALIDATION_ERROR", "密码至少6位");
        }

        // 检查账号是否已存在
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM sys_user WHERE username = ? AND deleted = 0", Integer.class, account);
        if (count != null && count > 0) {
            return ApiResponse.fail("DUPLICATE_ACCOUNT", "账号已存在");
        }

        // 加密密码
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String hashedPassword = encoder.encode(password);

        // 插入用户
        jdbcTemplate.update(
            "INSERT INTO sys_user (username, password_hash, real_name, phone, status, password_version, deleted, created_at, updated_at) VALUES (?, ?, ?, ?, 'ACTIVE', 1, 0, NOW(), NOW())",
            account, hashedPassword, realName, phone);

        // 获取用户ID
        Long userId = jdbcTemplate.queryForObject("SELECT id FROM sys_user WHERE username = ?", Long.class, account);

        // 分配普通群众角色（优先 PUBLIC，PUBLIC 不存在时兜底 EVENT_OPERATOR）
        List<Long> roleIds = jdbcTemplate.queryForList(
            "SELECT id FROM sys_role WHERE role_code = 'PUBLIC' ORDER BY id LIMIT 1", Long.class);
        if (roleIds.isEmpty()) {
            roleIds = jdbcTemplate.queryForList(
                "SELECT id FROM sys_role WHERE role_code = 'EVENT_OPERATOR' ORDER BY id LIMIT 1", Long.class);
        }
        if (!roleIds.isEmpty()) {
            jdbcTemplate.update("INSERT INTO sys_user_role (user_id, role_id) VALUES (?, ?)", userId, roleIds.get(0));
        }

        Map<String, Object> result = new HashMap<>();
        result.put("userId", userId);
        result.put("username", account);
        result.put("message", "注册成功");
        return ApiResponse.ok(result);
    }
}
