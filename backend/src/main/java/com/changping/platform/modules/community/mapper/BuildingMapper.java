package com.changping.platform.modules.community.mapper;

import com.changping.platform.modules.community.entity.BuildingEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class BuildingMapper {

    private final JdbcTemplate jdbcTemplate;

    private static final RowMapper<BuildingEntity> ROW_MAPPER = (rs, rowNum) -> {
        BuildingEntity e = new BuildingEntity();
        e.setId(rs.getLong("id"));
        e.setGridId(rs.getLong("grid_id"));
        e.setBuildingNo(rs.getString("building_no"));
        e.setAddress(rs.getString("address"));
        e.setHouseholdCount(rs.getInt("household_count"));
        e.setLandlordName(rs.getString("landlord_name"));
        e.setLandlordPhone(rs.getString("landlord_phone"));
        e.setFireRiskLevel(rs.getString("fire_risk_level"));
        e.setIsGroupRental(rs.getInt("is_group_rental"));
        e.setStatus(rs.getString("status"));
        e.setRemark(rs.getString("remark"));
        e.setCreatedAt(rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toLocalDateTime() : null);
        e.setUpdatedAt(rs.getTimestamp("updated_at") != null ? rs.getTimestamp("updated_at").toLocalDateTime() : null);
        return e;
    };

    public BuildingMapper(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<BuildingEntity> findByGridId(Long gridId) {
        return jdbcTemplate.query("SELECT * FROM cmn_building WHERE grid_id = ? AND status = 'ACTIVE' ORDER BY id DESC", ROW_MAPPER, gridId);
    }

    public List<BuildingEntity> findAllActive() {
        return jdbcTemplate.query("SELECT * FROM cmn_building WHERE status = 'ACTIVE' ORDER BY id DESC", ROW_MAPPER);
    }

    public BuildingEntity findById(Long id) {
        return jdbcTemplate.queryForObject("SELECT * FROM cmn_building WHERE id = ?", ROW_MAPPER, id);
    }

    public Long insert(BuildingEntity e) {
        String sql = "INSERT INTO cmn_building (grid_id, building_no, address, household_count, landlord_name, landlord_phone, fire_risk_level, is_group_rental, status, remark, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())";
        jdbcTemplate.update(sql, e.getGridId(), e.getBuildingNo(), e.getAddress(), e.getHouseholdCount(),
                e.getLandlordName(), e.getLandlordPhone(), e.getFireRiskLevel(), e.getIsGroupRental(),
                e.getStatus(), e.getRemark());
        return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    public int update(BuildingEntity e) {
        String sql = "UPDATE cmn_building SET grid_id=?, building_no=?, address=?, household_count=?, landlord_name=?, landlord_phone=?, fire_risk_level=?, is_group_rental=?, status=?, remark=?, updated_at=NOW() WHERE id=?";
        return jdbcTemplate.update(sql, e.getGridId(), e.getBuildingNo(), e.getAddress(), e.getHouseholdCount(),
                e.getLandlordName(), e.getLandlordPhone(), e.getFireRiskLevel(), e.getIsGroupRental(),
                e.getStatus(), e.getRemark(), e.getId());
    }

    public int deleteById(Long id) {
        return jdbcTemplate.update("DELETE FROM cmn_building WHERE id = ?", id);
    }
}
