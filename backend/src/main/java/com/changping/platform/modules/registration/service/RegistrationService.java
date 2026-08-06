package com.changping.platform.modules.registration.service;

import com.changping.platform.common.exception.BusinessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class RegistrationService {

    private final JdbcTemplate jdbcTemplate;

    public RegistrationService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Map<String, Object>> listPending() {
        String sql = "SELECT id, username, real_name as realName, phone, created_at as createdAt " +
                     "FROM sys_user WHERE status = 'PENDING' ORDER BY created_at DESC";
        try {
            return jdbcTemplate.queryForList(sql);
        } catch (Exception e) {
            // 如果没有 PENDING 状态的用户，返回空列表（可能是字段不存在）
            return List.of();
        }
    }

    @Transactional
    public void submit(String account, String passwordHash, String realName, String phone) {
        // 预检查账号是否已存在（含待审批/已激活），存在时返回业务错误而非数据库唯一索引 500
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sys_user WHERE username = ? AND deleted = 0", Integer.class, account);
        if (count != null && count > 0) {
            throw new BusinessException("DUPLICATE_ACCOUNT", "账号已存在");
        }
        String sql = "INSERT INTO sys_user (username, password_hash, real_name, phone, status, password_version, deleted) " +
                     "VALUES (?, ?, ?, ?, 'PENDING', 0, 0)";
        try {
            jdbcTemplate.update(sql, account, passwordHash, realName, phone);
        } catch (DuplicateKeyException e) {
            // 并发场景下唯一索引兜底，同样返回业务错误
            throw new BusinessException("DUPLICATE_ACCOUNT", "账号已存在");
        }
    }

    @Transactional
    public void approve(Long id, Long operatorId, String remark, String memberType) {
        // 1. 审批通过：用户状态置为 ACTIVE
        int updated = jdbcTemplate.update("UPDATE sys_user SET status = 'ACTIVE', updated_at = NOW() WHERE id = ? AND status = 'PENDING'", id);
        if (updated == 0) {
            return; // 非 PENDING 状态（如已审批/已拒绝），幂等返回
        }

        // 2. 查询用户信息（用于组织人员同步）
        List<Map<String, Object>> users = jdbcTemplate.queryForList(
                "SELECT id, real_name, phone FROM sys_user WHERE id = ? AND deleted = 0", id);
        if (users.isEmpty()) {
            throw new BusinessException("USER_NOT_FOUND", "用户不存在");
        }
        Map<String, Object> user = users.get(0);
        String realName = user.get("real_name") != null ? String.valueOf(user.get("real_name")) : "";
        String phone = user.get("phone") != null ? String.valueOf(user.get("phone")) : null;

        // 3. 根据审批身份分配角色（默认为网格员 GRID_WORKER，可登录 H5 端巡查）
        String roleCode = "GRID_WORKER";
        String orgMemberType = "GRID_WORKER";
        String position = "网格员";
        if ("STAFF".equals(memberType)) {
            roleCode = "EVENT_OPERATOR";
            orgMemberType = "STAFF";
            position = "社区工作人员";
        }
        Long roleId = jdbcTemplate.queryForObject(
                "SELECT id FROM sys_role WHERE role_code = ? AND status = 'ACTIVE' ORDER BY id LIMIT 1", Long.class, roleCode);
        if (roleId != null) {
            // 分配角色（INSERT IGNORE 保证幂等）
            jdbcTemplate.update("INSERT IGNORE INTO sys_user_role (user_id, role_id) VALUES (?, ?)", id, roleId);
        }

        // 4. 同步到组织人员管理（cmn_org_member），已存在则跳过
        Integer exists = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM cmn_org_member WHERE sys_user_id = ? OR name = ?", Integer.class, id, realName);
        if (exists == null || exists == 0) {
            jdbcTemplate.update(
                    "INSERT INTO cmn_org_member (grid_id, sys_user_id, member_type, name, phone, position, status, remark, created_at, updated_at) " +
                            "VALUES (?, ?, ?, ?, ?, ?, 'ACTIVE', '注册审批通过', NOW(), NOW())",
                    null, id, orgMemberType, realName, phone, position);
        }
    }

    @Transactional
    public void reject(Long id, String remark) {
        String sql = "UPDATE sys_user SET status = 'INACTIVE', updated_at = NOW() WHERE id = ? AND status = 'PENDING'";
        jdbcTemplate.update(sql, id);
    }
}
