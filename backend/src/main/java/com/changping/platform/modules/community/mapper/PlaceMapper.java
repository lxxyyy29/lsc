package com.changping.platform.modules.community.mapper;

import com.changping.platform.modules.community.entity.PlaceEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;
import java.sql.ResultSet;
import java.util.List;

@Component
public class PlaceMapper {

    private final JdbcTemplate jdbcTemplate;

    private static final RowMapper<PlaceEntity> ROW_MAPPER = (rs, rowNum) -> {
        PlaceEntity e = new PlaceEntity();
        e.setId(rs.getLong("id"));
        e.setGridId(rs.getLong("grid_id"));
        e.setPlaceName(rs.getString("place_name"));
        e.setPlaceType(rs.getString("place_type"));
        e.setAddress(rs.getString("address"));
        e.setContactName(rs.getString("contact_name"));
        e.setContactPhone(rs.getString("contact_phone"));
        e.setFireFacilities(rs.getString("fire_facilities"));
        e.setRiskTags(rs.getString("risk_tags"));
        e.setLongitude(rs.getBigDecimal("longitude"));
        e.setLatitude(rs.getBigDecimal("latitude"));
        e.setStatus(rs.getString("status"));
        e.setRemark(rs.getString("remark"));
        e.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        e.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
        return e;
    };

    public PlaceMapper(JdbcTemplate jdbcTemplate) { this.jdbcTemplate = jdbcTemplate; }

    public List<PlaceEntity> findByGridId(Long gridId) {
        return jdbcTemplate.query("SELECT * FROM cmn_place WHERE grid_id = ? AND status = 'ACTIVE' ORDER BY id DESC", ROW_MAPPER, gridId);
    }
    public List<PlaceEntity> findAllActive() {
        return jdbcTemplate.query("SELECT * FROM cmn_place WHERE status = 'ACTIVE' ORDER BY id DESC", ROW_MAPPER);
    }
    public PlaceEntity findById(Long id) {
        return jdbcTemplate.queryForObject("SELECT * FROM cmn_place WHERE id = ?", ROW_MAPPER, id);
    }
    public Long insert(PlaceEntity e) {
        String sql = "INSERT INTO cmn_place (grid_id, place_name, place_type, address, contact_name, contact_phone, fire_facilities, risk_tags, longitude, latitude, status, remark, created_at, updated_at) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,NOW(),NOW())";
        jdbcTemplate.update(sql, e.getGridId(), e.getPlaceName(), e.getPlaceType(), e.getAddress(),
                e.getContactName(), e.getContactPhone(), e.getFireFacilities(), e.getRiskTags(),
                e.getLongitude(), e.getLatitude(), e.getStatus(), e.getRemark());
        return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }
    public int update(PlaceEntity e) {
        String sql = "UPDATE cmn_place SET grid_id=?, place_name=?, place_type=?, address=?, contact_name=?, contact_phone=?, fire_facilities=?, risk_tags=?, longitude=?, latitude=?, status=?, remark=?, updated_at=NOW() WHERE id=?";
        return jdbcTemplate.update(sql, e.getGridId(), e.getPlaceName(), e.getPlaceType(), e.getAddress(),
                e.getContactName(), e.getContactPhone(), e.getFireFacilities(), e.getRiskTags(),
                e.getLongitude(), e.getLatitude(), e.getStatus(), e.getRemark(), e.getId());
    }
    public int deleteById(Long id) {
        return jdbcTemplate.update("DELETE FROM cmn_place WHERE id = ?", id);
    }
}
