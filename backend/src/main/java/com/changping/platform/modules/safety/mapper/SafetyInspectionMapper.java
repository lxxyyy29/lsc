package com.changping.platform.modules.safety.mapper;

import com.changping.platform.modules.safety.entity.SafetyInspectionEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.util.List;

@Component
public class SafetyInspectionMapper {

    private final JdbcTemplate jdbcTemplate;

    private static final RowMapper<SafetyInspectionEntity> ROW_MAPPER = (rs, rowNum) -> {
        SafetyInspectionEntity e = new SafetyInspectionEntity();
        e.setId(rs.getLong("id"));
        long mId = rs.getLong("merchant_id");
        e.setMerchantId(rs.wasNull() ? null : mId);
        long iId = rs.getLong("inspector_id");
        e.setInspectorId(rs.wasNull() ? null : iId);
        e.setInspectorName(rs.getString("inspector_name"));
        java.sql.Date inspDate = rs.getDate("inspection_date");
        e.setInspectionDate(inspDate != null ? inspDate.toLocalDate() : null);
        e.setFireRiskLevel(rs.getString("fire_risk_level"));
        e.setSafetyStatus(rs.getString("safety_status"));
        e.setHazardsFound(rs.getString("hazards_found"));
        e.setRectificationRequired(rs.getBoolean("rectification_required"));
        java.sql.Date deadline = rs.getDate("rectification_deadline");
        e.setRectificationDeadline(deadline != null ? deadline.toLocalDate() : null);
        e.setRectificationStatus(rs.getString("rectification_status"));
        e.setRemarks(rs.getString("remarks"));
        e.setPhotoUrls(rs.getString("photo_urls"));
        e.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        e.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
        return e;
    };

    public SafetyInspectionMapper(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void insert(SafetyInspectionEntity e) {
        String sql = "INSERT INTO biz_safety_inspection (merchant_id, inspector_id, inspector_name, inspection_date, fire_risk_level, safety_status, hazards_found, rectification_required, rectification_deadline, rectification_status, remarks, photo_urls, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())";
        jdbcTemplate.update(sql, e.getMerchantId(), e.getInspectorId(), e.getInspectorName(),
                e.getInspectionDate(), e.getFireRiskLevel(), e.getSafetyStatus(),
                e.getHazardsFound(), e.getRectificationRequired(), e.getRectificationDeadline(),
                e.getRectificationStatus(), e.getRemarks(), e.getPhotoUrls());
    }

    public List<SafetyInspectionEntity> findByMerchantId(Long merchantId) {
        return jdbcTemplate.query("SELECT * FROM biz_safety_inspection WHERE merchant_id = ? ORDER BY inspection_date DESC", ROW_MAPPER, merchantId);
    }

    public List<SafetyInspectionEntity> findAll() {
        return jdbcTemplate.query("SELECT i.*, m.merchant_name FROM biz_safety_inspection i LEFT JOIN biz_merchant m ON m.id = i.merchant_id ORDER BY i.inspection_date DESC LIMIT 200", ROW_MAPPER);
    }

    public SafetyStatistics getStatistics() {
        Long total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM biz_safety_inspection", Long.class);
        Long pending = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM biz_safety_inspection WHERE rectification_status = 'PENDING'", Long.class);
        Long inProgress = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM biz_safety_inspection WHERE rectification_status = 'IN_PROGRESS'", Long.class);
        Long completed = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM biz_safety_inspection WHERE rectification_status = 'COMPLETED'", Long.class);
        Long overdue = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM biz_safety_inspection WHERE rectification_status = 'OVERDUE' OR (rectification_deadline < CURDATE() AND rectification_status IN ('PENDING','IN_PROGRESS'))", Long.class);
        return new SafetyStatistics(
                total != null ? total : 0,
                pending != null ? pending : 0,
                inProgress != null ? inProgress : 0,
                completed != null ? completed : 0,
                overdue != null ? overdue : 0);
    }

    public int markOverdue() {
        return jdbcTemplate.update("UPDATE biz_safety_inspection SET rectification_status = 'OVERDUE', updated_at = NOW() WHERE rectification_deadline < CURDATE() AND rectification_status IN ('PENDING', 'IN_PROGRESS')");
    }

    public int updateRectificationStatus(Long id, String status) {
        return jdbcTemplate.update("UPDATE biz_safety_inspection SET rectification_status = ?, updated_at = NOW() WHERE id = ?", status, id);
    }

    public record SafetyStatistics(long total, long pending, long inProgress, long completed, long overdue) {}
}
