package com.changping.platform.modules.community.mapper;

import com.changping.platform.modules.community.entity.FormFieldConfigEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class FormFieldConfigMapper {

    /**
     * 系统必需字段（归一化键）：这些字段被表单推算、户关联、列表展示等逻辑依赖，
     * 允许改标签/排序/启用，但不允许删除。
     * - name          姓名：库中 NOT NULL，列表/户卡片/导出均以其为主显示
     * - idCard        身份证号：驱动性别/年龄/出生日期推算，且参与关键字搜索
     * - relation      与户主关系：户卡片、户主变更重算依赖
     * - gridId        所属网格：决定数据归属与网格范围可见性
     */
    private static final Set<String> SYSTEM_REQUIRED_KEYS = Set.of("name", "idcard", "relation", "gridid");

    /** 归一化字段键：去掉下划线并转小写，使 id_card / idCard 命中同一保护规则 */
    public static String normalizeKey(String fieldKey) {
        return fieldKey == null ? "" : fieldKey.replace("_", "").trim().toLowerCase();
    }

    /** 是否为系统必需字段（不可删除） */
    public static boolean isSystemRequired(String fieldKey) {
        return SYSTEM_REQUIRED_KEYS.contains(normalizeKey(fieldKey));
    }

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
        e.setSystemRequired(isSystemRequired(rs.getString("field_key")));
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

    /** 更新可调整项：标签、启用、排序、必填（字段键与类型为标识，不允许改名，避免历史值失联） */
    public void update(FormFieldConfigEntity e) {
        jdbcTemplate.update(
                "UPDATE sys_form_field_config SET field_label = ?, enabled = ?, sort_order = ?, required = ?, updated_at = NOW() WHERE id = ? AND module = ?",
                e.getFieldLabel(), e.getEnabled(), e.getSortOrder(), e.getRequired(), e.getId(), e.getModule());
    }

    /** 新增字段（字段配置器中添加） */
    public Long insert(FormFieldConfigEntity e) {
        jdbcTemplate.update(
                "INSERT INTO sys_form_field_config (module, field_key, field_label, field_type, options, enabled, sort_order, required, created_at, updated_at) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())",
                e.getModule(), e.getFieldKey(), e.getFieldLabel(), e.getFieldType(), e.getOptions(),
                e.getEnabled() != null ? e.getEnabled() : 1,
                e.getSortOrder() != null ? e.getSortOrder() : 0,
                e.getRequired() != null ? e.getRequired() : 0);
        return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    /** 按主键批量删除字段配置 */
    public int deleteByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }
        String placeholders = ids.stream().map(id -> "?").collect(Collectors.joining(", "));
        return jdbcTemplate.update(
                "DELETE FROM sys_form_field_config WHERE id IN (" + placeholders + ")",
                ids.toArray());
    }

    /**
     * 判断字段键是否已存在（按归一化键比较，避免 id_card 与 idCard 被当成两个字段）
     */
    public boolean existsKey(String module, String fieldKey) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sys_form_field_config WHERE module = ? "
                        + "AND LOWER(REPLACE(field_key, '_', '')) = LOWER(REPLACE(?, '_', ''))",
                Integer.class, module, fieldKey);
        return count != null && count > 0;
    }
}
