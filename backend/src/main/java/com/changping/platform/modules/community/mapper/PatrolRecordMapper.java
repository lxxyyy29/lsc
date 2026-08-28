package com.changping.platform.modules.community.mapper;

import com.changping.platform.modules.community.entity.PatrolRecordEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class PatrolRecordMapper {

    private final JdbcTemplate jdbcTemplate;

    private static final RowMapper<PatrolRecordEntity> ROW_MAPPER = (rs, rowNum) -> {
        PatrolRecordEntity e = new PatrolRecordEntity();
        e.setId(rs.getLong("id"));
        e.setGridId(rs.getLong("grid_id"));
        e.setUserId(rs.getLong("user_id"));
        e.setPatrolType(rs.getString("patrol_type"));
        e.setLongitude(rs.getBigDecimal("longitude"));
        e.setLatitude(rs.getBigDecimal("latitude"));
        e.setAddress(rs.getString("address"));
        e.setContent(rs.getString("content"));
        e.setRemark(rs.getString("remark"));
        e.setPhotoUrls(rs.getString("photo_urls"));
        e.setClientRequestId(rs.getString("client_request_id"));
        e.setStatus(rs.getString("status"));
        e.setGridName(rs.getString("grid_name"));
        e.setUserName(rs.getString("real_name"));
        e.setCreatedAt(rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toLocalDateTime() : null);
        e.setUpdatedAt(rs.getTimestamp("updated_at") != null ? rs.getTimestamp("updated_at").toLocalDateTime() : null);
        return e;
    };

    public PatrolRecordMapper(JdbcTemplate jdbcTemplate) { this.jdbcTemplate = jdbcTemplate; }

    public List<PatrolRecordEntity> findByUserId(Long userId) {
        return jdbcTemplate.query(
            "SELECT r.*, g.grid_name, u.real_name FROM cmn_patrol_record r " +
            "LEFT JOIN cmn_grid g ON r.grid_id = g.id " +
            "LEFT JOIN sys_user u ON r.user_id = u.id " +
            "WHERE r.user_id = ? ORDER BY r.created_at DESC", ROW_MAPPER, userId);
    }

    public List<PatrolRecordEntity> findAll() {
        return jdbcTemplate.query(
            "SELECT r.*, g.grid_name, u.real_name FROM cmn_patrol_record r " +
            "LEFT JOIN cmn_grid g ON r.grid_id = g.id " +
            "LEFT JOIN sys_user u ON r.user_id = u.id " +
            "ORDER BY r.created_at DESC", ROW_MAPPER);
    }

    public Long countByClientRequestId(String clientRequestId) {
        return jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM cmn_patrol_record WHERE client_request_id = ?", Long.class, clientRequestId);
    }

    public Long insert(PatrolRecordEntity e) {
        String sql = "INSERT INTO cmn_patrol_record (grid_id, user_id, patrol_type, longitude, latitude, address, content, remark, photo_urls, client_request_id, status, created_at, updated_at) VALUES (?,?,?,?,?,?,?,?,?,?,?,NOW(),NOW())";
        jdbcTemplate.update(sql, e.getGridId(), e.getUserId(), e.getPatrolType(),
                e.getLongitude(), e.getLatitude(), e.getAddress(), e.getContent(), e.getRemark(),
                e.getPhotoUrls(), e.getClientRequestId(), e.getStatus());
        return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }
}
