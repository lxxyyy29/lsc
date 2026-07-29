package com.changping.platform.modules.community.controller;

import com.changping.platform.common.response.ApiResponse;
import com.changping.platform.modules.community.entity.OrgMemberEntity;
import com.changping.platform.modules.community.service.OrgMemberService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/community/org-members")
public class OrgMemberController {

    private final OrgMemberService service;
    private final JdbcTemplate jdbcTemplate;

    public OrgMemberController(OrgMemberService service, JdbcTemplate jdbcTemplate) {
        this.service = service;
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping
    public ApiResponse<List<OrgMemberEntity>> list(@RequestParam(required = false) Long gridId) {
        return ApiResponse.ok(service.list(gridId));
    }
    @GetMapping("/{id}")
    public ApiResponse<OrgMemberEntity> detail(@PathVariable Long id) {
        return ApiResponse.ok(service.detail(id));
    }
    @PostMapping
    public ApiResponse<Boolean> create(@RequestBody OrgMemberEntity entity) {
        // 1. 创建组织人员
        service.create(entity);
        // 2. 如果是网格员，同步创建系统用户（用于考核研判和派单）
        if ("GRID_WORKER".equals(entity.getMemberType())) {
            syncToSysUser(entity);
        }
        return ApiResponse.ok(true);
    }
    @PutMapping("/{id}")
    public ApiResponse<Boolean> update(@PathVariable Long id, @RequestBody OrgMemberEntity entity) {
        entity.setId(id);
        return ApiResponse.ok(service.update(entity));
    }
    @DeleteMapping("/{id}")
    public ApiResponse<Boolean> delete(@PathVariable Long id) {
        return ApiResponse.ok(service.delete(id));
    }

    /**
     * 将现有网格员（sys_user）同步到组织人员表
     */
    @PostMapping("/sync-from-users")
    public ApiResponse<Map<String, Object>> syncFromUsers() {
        Map<String, Object> result = new java.util.HashMap<>();
        int synced = 0;
        int skipped = 0;

        try {
            // 获取所有 GRID_WORKER 角色的用户
            List<Map<String, Object>> gridUsers = jdbcTemplate.queryForList(
                "SELECT u.id, u.real_name, u.phone FROM sys_user u " +
                "JOIN sys_user_role ur ON ur.user_id = u.id " +
                "JOIN sys_role r ON r.id = ur.role_id " +
                "WHERE r.role_code = 'GRID_WORKER' AND u.deleted = 0");

            for (Map<String, Object> user : gridUsers) {
                String name = (String) user.get("real_name");
                Long userId = ((Number) user.get("id")).longValue();
                // 检查是否已存在（按 sys_user_id 或 name）
                Integer exists = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM cmn_org_member WHERE sys_user_id = ? OR name = ?", Integer.class, userId, name);
                if (exists != null && exists > 0) {
                    skipped++;
                    continue;
                }
                // 创建组织人员
                jdbcTemplate.update(
                    "INSERT INTO cmn_org_member (grid_id, sys_user_id, member_type, name, phone, status, remark, created_at, updated_at) " +
                    "VALUES (?, ?, 'GRID_WORKER', ?, ?, 'ACTIVE', '从系统用户同步', NOW(), NOW())",
                    null, user.get("id"), name, user.get("phone"));
                synced++;
            }
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "同步失败: " + e.getMessage());
            return ApiResponse.ok(result);
        }

        result.put("success", true);
        result.put("synced", synced);
        result.put("skipped", skipped);
        result.put("message", String.format("同步完成：新增 %d 条，跳过 %d 条（已存在）", synced, skipped));
        return ApiResponse.ok(result);
    }

    /**
     * 同步组织人员到系统用户表（考核研判数据来源）
     */
    private void syncToSysUser(OrgMemberEntity entity) {
        try {
            // 检查是否已存在同名用户
            String username = entity.getName().replaceAll("\\s+", "");
            Integer exists = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sys_user WHERE username = ? AND deleted = 0", Integer.class, username);
            if (exists != null && exists > 0) return;

            // 确保 GRID_WORKER 角色存在
            Integer roleCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sys_role WHERE role_code = 'GRID_WORKER'", Integer.class);
            if (roleCount == null || roleCount == 0) {
                jdbcTemplate.update(
                    "INSERT INTO sys_role (role_code, role_name, status, remark, created_at, updated_at) VALUES ('GRID_WORKER', '网格员', 'ACTIVE', '网格巡查与事件处置人员', NOW(), NOW())");
            }
            Long roleId = jdbcTemplate.queryForObject("SELECT id FROM sys_role WHERE role_code = 'GRID_WORKER'", Long.class);

            // 创建系统用户
            BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
            jdbcTemplate.update(
                "INSERT INTO sys_user (username, password_hash, real_name, phone, status, password_version, deleted, created_at, updated_at) " +
                "VALUES (?, ?, ?, ?, 'ACTIVE', 1, 0, NOW(), NOW())",
                username, encoder.encode("123456"), entity.getName(), entity.getPhone());
            Long userId = jdbcTemplate.queryForObject("SELECT id FROM sys_user WHERE username = ?", Long.class, username);
            // 分配 GRID_WORKER 角色
            jdbcTemplate.update("INSERT IGNORE INTO sys_user_role (user_id, role_id) VALUES (?, ?)", userId, roleId);
        } catch (Exception e) {
            // 同步失败不影响主流程
            org.slf4j.LoggerFactory.getLogger(OrgMemberController.class).warn("同步网格员到系统用户失败: {}", e.getMessage());
        }
    }
}
