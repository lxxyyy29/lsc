package com.changping.platform.modules.community.mapper;

import com.changping.platform.modules.community.entity.ResidentReportEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.UUID;

@Component
public class ResidentReportMapper {

    private final JdbcTemplate jdbcTemplate;

    private static final RowMapper<ResidentReportEntity> ROW_MAPPER = (rs, rowNum) -> {
        ResidentReportEntity e = new ResidentReportEntity();
        e.setId(rs.getLong("id"));
        e.setGridId(rs.getLong("grid_id"));
        e.setResidentName(rs.getString("resident_name"));
        e.setResidentPhone(rs.getString("resident_phone"));
        e.setReportType(rs.getString("report_type"));
        e.setTitle(rs.getString("title"));
        e.setContent(rs.getString("content"));
        e.setPhotoUrls(rs.getString("photo_urls"));
        e.setLongitude(rs.getBigDecimal("longitude"));
        e.setLatitude(rs.getBigDecimal("latitude"));
        e.setQueryCode(rs.getString("query_code"));
        long eventId = rs.getLong("event_id");
        e.setEventId(rs.wasNull() ? null : eventId);
        e.setStatus(rs.getString("status"));
        e.setHandleResult(rs.getString("handle_result"));
        e.setGridName(rs.getString("grid_name"));
        e.setHandlerUserName(rs.getString("real_name"));
        e.setCreatedAt(rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toLocalDateTime() : null);
        java.sql.Timestamp handledAt = rs.getTimestamp("handled_at");
        e.setHandledAt(handledAt != null ? handledAt.toLocalDateTime() : null);
        e.setUpdatedAt(rs.getTimestamp("updated_at") != null ? rs.getTimestamp("updated_at").toLocalDateTime() : null);
        return e;
    };

    public ResidentReportMapper(JdbcTemplate jdbcTemplate) { this.jdbcTemplate = jdbcTemplate; }

    public List<ResidentReportEntity> findAll() {
        return findByStatus(null);
    }

    /** 按状态筛选（status 为空时查全部） */
    public List<ResidentReportEntity> findByStatus(String status) {
        StringBuilder sql = new StringBuilder(
            "SELECT r.*, g.grid_name, hu.real_name FROM cmn_resident_report r " +
            "LEFT JOIN cmn_grid g ON r.grid_id = g.id " +
            "LEFT JOIN sys_user hu ON r.handler_user_id = hu.id");
        if (status != null && !status.isBlank()) {
            sql.append(" WHERE r.status = ?");
        }
        sql.append(" ORDER BY r.created_at DESC");
        return status != null && !status.isBlank()
                ? jdbcTemplate.query(sql.toString(), ROW_MAPPER, status)
                : jdbcTemplate.query(sql.toString(), ROW_MAPPER);
    }

    public ResidentReportEntity findByQueryCode(String queryCode) {
        List<ResidentReportEntity> results = jdbcTemplate.query(
            "SELECT r.*, g.grid_name, hu.real_name FROM cmn_resident_report r " +
            "LEFT JOIN cmn_grid g ON r.grid_id = g.id " +
            "LEFT JOIN sys_user hu ON r.handler_user_id = hu.id " +
            "WHERE r.query_code = ?", ROW_MAPPER, queryCode);
        return results.isEmpty() ? null : results.get(0);
    }

    public ResidentReportEntity findById(Long id) {
        List<ResidentReportEntity> results = jdbcTemplate.query(
            "SELECT r.*, g.grid_name, hu.real_name FROM cmn_resident_report r " +
            "LEFT JOIN cmn_grid g ON r.grid_id = g.id " +
            "LEFT JOIN sys_user hu ON r.handler_user_id = hu.id " +
            "WHERE r.id = ?", ROW_MAPPER, id);
        return results.isEmpty() ? null : results.get(0);
    }

    public void updateEventId(Long id, Long eventId) {
        jdbcTemplate.update(
            "UPDATE cmn_resident_report SET event_id = ?, updated_at = NOW() WHERE id = ?",
            eventId, id);
    }

    public Long insert(ResidentReportEntity e) {
        if (e.getQueryCode() == null) {
            e.setQueryCode(UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        }
        String sql = "INSERT INTO cmn_resident_report (grid_id, resident_name, resident_phone, report_type, title, content, photo_urls, longitude, latitude, query_code, status, created_at, updated_at) VALUES (?,?,?,?,?,?,?,?,?,?,?,NOW(),NOW())";
        jdbcTemplate.update(sql, e.getGridId(), e.getResidentName(), e.getResidentPhone(),
                e.getReportType(), e.getTitle(), e.getContent(), e.getPhotoUrls(),
                e.getLongitude(), e.getLatitude(), e.getQueryCode(), e.getStatus());
        return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }
}
