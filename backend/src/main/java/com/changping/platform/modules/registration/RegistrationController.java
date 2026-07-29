package com.changping.platform.modules.registration;

import com.changping.platform.common.response.ApiResponse;
import com.changping.platform.modules.auth.security.PermissionCodes;
import com.changping.platform.modules.auth.security.PermissionGuard;
import com.changping.platform.modules.auth.service.AuthService;
import com.changping.platform.modules.auth.service.CurrentUserService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 网格员注册审批管理
 */
@RestController
@RequestMapping("/registration")
public class RegistrationController {

    private final JdbcTemplate jdbcTemplate;
    private final PermissionGuard permissionGuard;
    private final CurrentUserService currentUserService;

    public RegistrationController(JdbcTemplate jdbcTemplate, PermissionGuard permissionGuard, CurrentUserService currentUserService) {
        this.jdbcTemplate = jdbcTemplate;
        this.permissionGuard = permissionGuard;
        this.currentUserService = currentUserService;
    }

    /**
     * 提交注册申请（H5端）
     */
    @PostMapping("/submit")
    public ApiResponse<Map<String, Object>> submit(@RequestBody Map<String, Object> body) {
        String account = (String) body.get("account");
        String password = (String) body.get("password");
        String realName = (String) body.get("realName");
        String phone = (String) body.get("phone");

        if (account == null || account.isBlank() || password == null || password.isBlank()) {
            return ApiResponse.fail("VALIDATION_ERROR", "账号和密码不能为空");
        }
        if (realName == null || realName.isBlank()) {
            return ApiResponse.fail("VALIDATION_ERROR", "真实姓名不能为空");
        }

        // 检查账号是否已存在
        Integer userExists = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM sys_user WHERE username = ? AND deleted = 0", Integer.class, account);
        if (userExists != null && userExists > 0) {
            return ApiResponse.fail("DUPLICATE_ACCOUNT", "账号已存在");
        }

        // 检查是否已有待审批申请
        Integer pendingExists = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM biz_registration WHERE account = ? AND status = 'PENDING'", Integer.class, account);
        if (pendingExists != null && pendingExists > 0) {
            return ApiResponse.fail("DUPLICATE_APPLICATION", "已有待审批的申请");
        }

        // 创建申请
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        jdbcTemplate.update(
            "INSERT INTO biz_registration (account, password_hash, real_name, phone, status) VALUES (?, ?, ?, ?, 'PENDING')",
            account, encoder.encode(password), realName, phone);

        Long id = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);

        Map<String, Object> result = new HashMap<>();
        result.put("id", id);
        result.put("message", "申请已提交，等待审批");
        return ApiResponse.ok(result);
    }

    /**
     * 获取待审批列表（Web管理端）
     */
    @GetMapping("/pending")
    public ApiResponse<List<Map<String, Object>>> listPending() {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.API_SYSTEM_USER_LIST);

        List<Map<String, Object>> list = jdbcTemplate.queryForList(
            "SELECT id, account, real_name, phone, status, remark, created_at " +
            "FROM biz_registration WHERE status = 'PENDING' ORDER BY id DESC");
        return ApiResponse.ok(list);
    }

    /**
     * 获取所有申请记录
     */
    @GetMapping("/list")
    public ApiResponse<List<Map<String, Object>>> listAll() {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.API_SYSTEM_USER_LIST);

        List<Map<String, Object>> list = jdbcTemplate.queryForList(
            "SELECT id, account, real_name, phone, status, remark, created_at, reviewed_at " +
            "FROM biz_registration ORDER BY id DESC");
        return ApiResponse.ok(list);
    }

    /**
     * 审批通过
     */
    @PostMapping("/{id}/approve")
    public ApiResponse<Map<String, Object>> approve(@PathVariable Long id, @RequestBody Map<String, String> body) {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.API_SYSTEM_USER_LIST);

        // 获取申请信息
        Map<String, Object> reg = jdbcTemplate.queryForMap(
            "SELECT * FROM biz_registration WHERE id = ? AND status = 'PENDING'", id);

        if (reg == null) {
            return ApiResponse.fail("NOT_FOUND", "申请不存在或已处理");
        }

        String account = (String) reg.get("account");
        String passwordHash = (String) reg.get("password_hash");
        String realName = (String) reg.get("real_name");
        String phone = (String) reg.get("phone");
        String remark = body.get("remark");

        // 1. 创建系统用户
        jdbcTemplate.update(
            "INSERT INTO sys_user (username, password_hash, real_name, phone, status, password_version, deleted, created_at, updated_at) " +
            "VALUES (?, ?, ?, ?, 'ACTIVE', 1, 0, NOW(), NOW())",
            account, passwordHash, realName, phone);
        Long userId = jdbcTemplate.queryForObject("SELECT id FROM sys_user WHERE username = ?", Long.class, account);

        // 2. 分配 GRID_WORKER 角色
        Long roleId = jdbcTemplate.queryForObject("SELECT id FROM sys_role WHERE role_code = 'GRID_WORKER'", Long.class);
        if (roleId != null) {
            jdbcTemplate.update("INSERT IGNORE INTO sys_user_role (user_id, role_id) VALUES (?, ?)", userId, roleId);
        }

        // 3. 同步到组织人员表
        jdbcTemplate.update(
            "INSERT INTO cmn_org_member (grid_id, sys_user_id, member_type, name, phone, status, remark, created_at, updated_at) " +
            "VALUES (?, ?, 'GRID_WORKER', ?, ?, 'ACTIVE', '注册审批通过', NOW(), NOW())",
            null, userId, realName, phone);

        // 4. 更新申请状态
        Long reviewerId = currentUserService.requireClientType(AuthService.ClientType.WEB).id();
        jdbcTemplate.update(
            "UPDATE biz_registration SET status = 'APPROVED', remark = ?, reviewer_id = ?, reviewed_at = NOW() WHERE id = ?",
            remark, reviewerId, id);

        Map<String, Object> result = new HashMap<>();
        result.put("userId", userId);
        result.put("message", "审批通过，网格员已创建");
        return ApiResponse.ok(result);
    }

    /**
     * 审批驳回
     */
    @PostMapping("/{id}/reject")
    public ApiResponse<Void> reject(@PathVariable Long id, @RequestBody Map<String, String> body) {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.API_SYSTEM_USER_LIST);

        String remark = body.get("remark");
        if (remark == null || remark.isBlank()) {
            return ApiResponse.fail("VALIDATION_ERROR", "请填写驳回原因");
        }

        Long reviewerId = currentUserService.requireClientType(AuthService.ClientType.WEB).id();
        int updated = jdbcTemplate.update(
            "UPDATE biz_registration SET status = 'REJECTED', remark = ?, reviewer_id = ?, reviewed_at = NOW() WHERE id = ? AND status = 'PENDING'",
            remark, reviewerId, id);

        if (updated == 0) {
            return ApiResponse.fail("NOT_FOUND", "申请不存在或已处理");
        }

        return ApiResponse.ok(null);
    }
}
