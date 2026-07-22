package com.changping.platform.modules.community.mapper;

import com.changping.platform.modules.community.entity.ResidentReportEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;
import java.sql.ResultSet;
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
        e.setStatus(rs.getString("status"));
        e.setHandleResult(rs.getString("handle_result"));
        e.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        java.sql.Timestamp handledAt = rs.getTimestamp("handled_at");
        e.setHandledAt(handledAt != null ? handledAt.toLocalDateTime() : null);
        e.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
        return e;
    };

    public ResidentReportMapper(JdbcTemplate jdbcTemplate) { this.jdbcTemplate = jdbcTemplate; }

    public List<ResidentReportEntity> findAll() {
        return jdbcTemplate.query("SELECT * FROM cmn_resident_report ORDER BY created_at DESC", ROW_MAPPER);
    }

    public ResidentReportEntity findByQueryCode(String queryCode) {
        List<ResidentReportEntity> results = jdbcTemplate.query(
            "SELECT * FROM cmn_resident_report WHERE query_code = ?", ROW_MAPPER, queryCode);
        return results.isEmpty() ? null : results.get(0);
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
