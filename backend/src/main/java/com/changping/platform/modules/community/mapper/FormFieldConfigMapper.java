package com.changping.platform.modules.community.mapper;

import com.changping.platform.modules.community.entity.FormFieldConfigEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class FormFieldConfigMapper {

    private final JdbcTemplate jdbcTemplate;

    private static final RowMapper<FormFieldConfigEntity> ROW_MAPPER = (rs, rowNum) -> {
        FormFieldConfigEntity e = new FormFieldConfigEntity();
        e.setId(rs.getLong("id"));
        e.setModule(rs.getString("module"));
        e.setFieldKey(rs.getString("field_key"));
        e.setFieldLabel(rs.getString("field_label"));
        e.setFieldType(rs.getString("field_type"));
        e.setOptions(rs.getString("options"));
        e.setEnabled(rs.getInt("enabled"));
        e.setSortOrder(rs.getInt("sort_order"));
        e.setRequired(rs.getInt("required"));
        e.setCreatedAt(rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toLocalDateTime() : null);
        e.setUpdatedAt(rs.getTimestamp("updated_at") != null ? rs.getTimestamp("updated_at").toLocalDateTime() : null);
        return e;
    };

    public FormFieldConfigMapper(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<FormFieldConfigEntity> listByModule(String module) {
        return jdbcTemplate.query(
                "SELECT * FROM sys_form_field_config WHERE module = ? ORDER BY sort_order ASC",
                ROW_MAPPER, module);
    }

    public void update(FormFieldConfigEntity e) {
        jdbcTemplate.update(
                "UPDATE sys_form_field_config SET field_label = ?, enabled = ?, sort_order = ?, required = ?, updated_at = NOW() WHERE id = ? AND module = ?",
                e.getFieldLabel(), e.getEnabled(), e.getSortOrder(), e.getRequired(), e.getId(), e.getModule());
    }
}
