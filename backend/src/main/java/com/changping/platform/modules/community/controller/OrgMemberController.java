package com.changping.platform.modules.community.controller;

import com.changping.platform.common.response.ApiResponse;
import com.changping.platform.modules.community.entity.OrgMemberEntity;
import com.changping.platform.modules.community.service.OrgMemberService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;
import java.util.List;

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
        // 2. 同步创建系统用户（用于考核研判和派单）
        syncToSysUser(entity);
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
