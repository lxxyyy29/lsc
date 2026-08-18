package com.changping.platform.modules.passwordreset.service;

import com.changping.platform.common.exception.BusinessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 密码重置申请：小程序端用户忘记密码时提交（账号+注册手机号校验，无需登录），
 * web 管理员审批后一键重置（新密码=手机号后6位），由管理员线下转达用户。
 */
@Service
public class PasswordResetService {

    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;

    public PasswordResetService(JdbcTemplate jdbcTemplate, PasswordEncoder passwordEncoder) {
        this.jdbcTemplate = jdbcTemplate;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * 提交重置申请（公开接口）：账号+手机号必须与库内 ACTIVE 用户完全匹配；
     * 不区分"账号不存在"与"手机号不符"，统一提示，防止账号探测。
     */
    @Transactional
    public void submit(String account, String phone) {
        if (account == null || account.isBlank() || phone == null || phone.isBlank()) {
            throw new BusinessException("VALIDATION_ERROR", "请输入账号和手机号");
        }
        Map<String, Object> user;
        try {
            user = jdbcTemplate.queryForMap(
                    "SELECT id, real_name FROM sys_user WHERE username = ? AND phone = ? AND deleted = 0 AND status = 'ACTIVE'",
                    account.trim(), phone.trim());
        } catch (EmptyResultDataAccessException e) {
            throw new BusinessException("PWD_RESET_NO_MATCH", "未找到匹配的账号，请确认账号与注册手机号一致");
        }
        Long userId = ((Number) user.get("id")).longValue();
        // 已有待处理申请则不重复提交
        Integer pendingCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM pwd_reset_request WHERE user_id = ? AND status = 'PENDING'",
                Integer.class, userId);
        if (pendingCount != null && pendingCount > 0) {
            throw new BusinessException("PWD_RESET_DUPLICATE", "您的重置申请正在处理中，请耐心等待管理员操作");
        }
        // 防刷：10 分钟内只允许提交一次
        Integer recentCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM pwd_reset_request WHERE user_id = ? AND created_at > DATE_SUB(NOW(), INTERVAL 10 MINUTE)",
                Integer.class, userId);
        if (recentCount != null && recentCount > 0) {
            throw new BusinessException("PWD_RESET_TOO_FREQUENT", "提交过于频繁，请 10 分钟后再试");
        }
        jdbcTemplate.update(
                "INSERT INTO pwd_reset_request (user_id, account, phone, status) VALUES (?, ?, ?, 'PENDING')",
                userId, account.trim(), phone.trim());
        Long requestId = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        notifyAdmins(String.valueOf(user.get("real_name")), account.trim(), requestId);
    }

    /**
     * 查询申请进度（公开接口）：返回最新一条申请的状态。
     */
    public Map<String, Object> status(String account, String phone) {
        Map<String, Object> result = new LinkedHashMap<>();
        List<Map<String, Object>> rows;
        try {
            rows = jdbcTemplate.queryForList(
                    "SELECT r.status, r.created_at, r.handled_at, r.remark " +
                    "FROM pwd_reset_request r JOIN sys_user u ON u.id = r.user_id " +
                    "WHERE u.username = ? AND u.phone = ? AND u.deleted = 0 " +
                    "ORDER BY r.id DESC LIMIT 1",
                    account == null ? "" : account.trim(), phone == null ? "" : phone.trim());
        } catch (Exception e) {
            rows = List.of();
        }
        if (rows.isEmpty()) {
            result.put("found", false);
            return result;
        }
        Map<String, Object> row = rows.get(0);
        result.put("found", true);
        result.put("status", row.get("status"));
        result.put("createdAt", row.get("created_at"));
        result.put("handledAt", row.get("handled_at"));
        result.put("remark", row.get("remark"));
        return result;
    }

    /**
     * 管理员查看申请列表：status 为空返回全部（待处理在前）。
     */
    public List<Map<String, Object>> list(String status) {
        StringBuilder sql = new StringBuilder(
                "SELECT r.id, r.account, r.phone, r.status, r.remark, r.created_at, r.handled_at, " +
                "u.real_name, h.real_name AS handled_by_name " +
                "FROM pwd_reset_request r " +
                "JOIN sys_user u ON u.id = r.user_id " +
                "LEFT JOIN sys_user h ON h.id = r.handled_by ");
        if (status != null && !status.isBlank()) {
            sql.append("WHERE r.status = ? ");
        }
        sql.append("ORDER BY (r.status = 'PENDING') DESC, r.id DESC LIMIT 100");
        if (status != null && !status.isBlank()) {
            return jdbcTemplate.queryForList(sql.toString(), status.trim());
        }
        return jdbcTemplate.queryForList(sql.toString());
    }

    /**
     * 管理员批准：重置密码为手机号后 6 位并返回给管理员（由其线下转达用户）；
     * password_version 提升使该用户存量令牌全部失效。
     */
    @Transactional
    public String approve(Long id, Long operatorId) {
        Map<String, Object> request;
        try {
            request = jdbcTemplate.queryForMap(
                    "SELECT r.id, r.status, r.user_id, r.phone, u.username " +
                    "FROM pwd_reset_request r JOIN sys_user u ON u.id = r.user_id " +
                    "WHERE r.id = ?", id);
        } catch (EmptyResultDataAccessException e) {
            throw new BusinessException("PWD_RESET_NOT_FOUND", "重置申请不存在");
        }
        if (!"PENDING".equals(request.get("status"))) {
            throw new BusinessException("PWD_RESET_ALREADY_HANDLED", "该申请已处理，请刷新列表");
        }
        String phone = String.valueOf(request.get("phone"));
        String newPassword = phone.length() >= 6 ? phone.substring(phone.length() - 6) : phone;
        jdbcTemplate.update(
                "UPDATE sys_user SET password_hash = ?, password_version = password_version + 1, updated_at = NOW() WHERE id = ?",
                passwordEncoder.encode(newPassword), request.get("user_id"));
        jdbcTemplate.update(
                "UPDATE pwd_reset_request SET status = 'APPROVED', remark = '已重置', handled_by = ?, handled_at = NOW() WHERE id = ?",
                operatorId, id);
        return newPassword;
    }

    /**
     * 管理员驳回。
     */
    public void reject(Long id, Long operatorId, String remark) {
        int updated = jdbcTemplate.update(
                "UPDATE pwd_reset_request SET status = 'REJECTED', remark = ?, handled_by = ?, handled_at = NOW() " +
                "WHERE id = ? AND status = 'PENDING'",
                remark, operatorId, id);
        if (updated == 0) {
            throw new BusinessException("PWD_RESET_ALREADY_HANDLED", "该申请已处理或不存在，请刷新列表");
        }
    }

    /**
     * 给所有启用状态的 web 管理员（超管+普通管理员）发站内通知。
     */
    private void notifyAdmins(String realName, String account, Long requestId) {
        List<Long> adminIds = jdbcTemplate.queryForList(
                "SELECT DISTINCT ur.user_id FROM sys_user_role ur " +
                "JOIN sys_role r ON r.id = ur.role_id AND r.role_code IN ('SUPER_ADMIN','EVENT_OPERATOR') " +
                "JOIN sys_user u ON u.id = ur.user_id AND u.deleted = 0 AND u.status = 'ACTIVE'",
                Long.class);
        for (Long adminId : adminIds) {
            jdbcTemplate.update(
                    "INSERT INTO sys_notification (user_id, title, content, type, level, related_type, related_id, is_read, created_at) " +
                    "VALUES (?, ?, ?, 'PWD_RESET', 'INFO', 'PWD_RESET', ?, 0, NOW())",
                    adminId, "密码重置申请",
                    "用户 " + realName + "（" + account + "）提交了密码重置申请，请及时处理",
                    requestId);
        }
    }
}
