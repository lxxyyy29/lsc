package com.changping.platform.modules.registration.service;

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
    public void approve(Long id, Long operatorId, String remark) {
        String sql = "UPDATE sys_user SET status = 'ACTIVE', updated_at = NOW() WHERE id = ? AND status = 'PENDING'";
        jdbcTemplate.update(sql, id);
    }

    @Transactional
    public void reject(Long id, String remark) {
        String sql = "UPDATE sys_user SET status = 'INACTIVE', updated_at = NOW() WHERE id = ? AND status = 'PENDING'";
        jdbcTemplate.update(sql, id);
    }
}
