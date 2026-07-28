package com.changping.platform.modules.ledger.mapper;

import com.changping.platform.modules.ledger.entity.LedgerTemplateEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.util.List;

@Component
public class LedgerTemplateMapper {

    private final JdbcTemplate jdbcTemplate;

    private static final RowMapper<LedgerTemplateEntity> ROW_MAPPER = (rs, rowNum) -> {
        LedgerTemplateEntity e = new LedgerTemplateEntity();
        e.setId(rs.getLong("id"));
        e.setTemplateName(rs.getString("template_name"));
        e.setTemplateType(rs.getString("template_type"));
        e.setDescription(rs.getString("description"));
        e.setColumnsJson(rs.getString("columns_json"));
        e.setFiltersJson(rs.getString("filters_json"));
        e.setSortField(rs.getString("sort_field"));
        e.setSortOrder(rs.getString("sort_order"));
        e.setStatus(rs.getString("status"));
        long cb = rs.getLong("created_by");
        e.setCreatedBy(rs.wasNull() ? null : cb);
        e.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        e.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
        return e;
    };

    public LedgerTemplateMapper(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<LedgerTemplateEntity> findAll() {
        return jdbcTemplate.query("SELECT * FROM sys_ledger_template WHERE status = 'ACTIVE' ORDER BY template_type, id", ROW_MAPPER);
    }

    public List<LedgerTemplateEntity> findByType(String type) {
        return jdbcTemplate.query("SELECT * FROM sys_ledger_template WHERE status = 'ACTIVE' AND template_type = ? ORDER BY id", ROW_MAPPER, type);
    }

    public LedgerTemplateEntity findById(Long id) {
        List<LedgerTemplateEntity> list = jdbcTemplate.query("SELECT * FROM sys_ledger_template WHERE id = ?", ROW_MAPPER, id);
        return list.isEmpty() ? null : list.get(0);
    }

    public void insert(LedgerTemplateEntity e) {
        jdbcTemplate.update(
                "INSERT INTO sys_ledger_template (template_name, template_type, description, columns_json, filters_json, sort_field, sort_order, status, created_by, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, 'ACTIVE', ?, NOW(), NOW())",
                e.getTemplateName(), e.getTemplateType(), e.getDescription(), e.getColumnsJson(),
                e.getFiltersJson(), e.getSortField(), e.getSortOrder(), e.getCreatedBy());
    }

    public void update(LedgerTemplateEntity e) {
        jdbcTemplate.update(
                "UPDATE sys_ledger_template SET template_name = ?, description = ?, columns_json = ?, filters_json = ?, sort_field = ?, sort_order = ?, updated_at = NOW() WHERE id = ?",
                e.getTemplateName(), e.getDescription(), e.getColumnsJson(), e.getFiltersJson(),
                e.getSortField(), e.getSortOrder(), e.getId());
    }

    public void delete(Long id) {
        jdbcTemplate.update("UPDATE sys_ledger_template SET status = 'DELETED', updated_at = NOW() WHERE id = ?", id);
    }

    public JdbcTemplate getJdbc() {
        return jdbcTemplate;
    }
}
