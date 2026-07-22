package com.changping.platform.modules.community.mapper;

import com.changping.platform.modules.community.entity.PopulationEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Component
public class PopulationMapper {

    private final JdbcTemplate jdbcTemplate;

    private static final RowMapper<PopulationEntity> ROW_MAPPER = (rs, rowNum) -> {
        PopulationEntity entity = new PopulationEntity();
        entity.setId(rs.getLong("id"));
        entity.setGridId(rs.getLong("grid_id"));
        entity.setName(rs.getString("name"));
        entity.setIdCard(rs.getString("id_card"));
        entity.setPhone(rs.getString("phone"));
        entity.setGender(rs.getString("gender"));
        java.sql.Date birthday = rs.getDate("birthday");
        entity.setBirthday(birthday != null ? birthday.toLocalDate() : null);
        entity.setHouseholdType(rs.getString("household_type"));
        entity.setAddress(rs.getString("address"));
        entity.setBuildingNo(rs.getString("building_no"));
        entity.setRoomNo(rs.getString("room_no"));
        entity.setTags(rs.getString("tags"));
        entity.setPhotoUrl(rs.getString("photo_url"));
        entity.setStatus(rs.getString("status"));
        entity.setRemark(rs.getString("remark"));
        entity.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        entity.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
        return entity;
    };

    public PopulationMapper(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<PopulationEntity> findByGridId(Long gridId) {
        return jdbcTemplate.query(
                "SELECT * FROM cmn_population WHERE grid_id = ? AND status = 'ACTIVE' ORDER BY id DESC",
                ROW_MAPPER, gridId);
    }

    public List<PopulationEntity> findAllActive() {
        return jdbcTemplate.query(
                "SELECT * FROM cmn_population WHERE status = 'ACTIVE' ORDER BY id DESC",
                ROW_MAPPER);
    }

    public PopulationEntity findById(Long id) {
        return jdbcTemplate.queryForObject(
                "SELECT * FROM cmn_population WHERE id = ?",
                ROW_MAPPER, id);
    }

    public Long insert(PopulationEntity entity) {
        String sql = "INSERT INTO cmn_population (grid_id, name, id_card, phone, gender, birthday, household_type, address, building_no, room_no, tags, photo_url, status, remark, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())";
        jdbcTemplate.update(sql,
                entity.getGridId(), entity.getName(), entity.getIdCard(),
                entity.getPhone(), entity.getGender(), entity.getBirthday(),
                entity.getHouseholdType(), entity.getAddress(),
                entity.getBuildingNo(), entity.getRoomNo(),
                entity.getTags(), entity.getPhotoUrl(),
                entity.getStatus(), entity.getRemark());
        return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    public int update(PopulationEntity entity) {
        String sql = "UPDATE cmn_population SET grid_id = ?, name = ?, id_card = ?, phone = ?, gender = ?, birthday = ?, household_type = ?, address = ?, building_no = ?, room_no = ?, tags = ?, photo_url = ?, status = ?, remark = ?, updated_at = NOW() WHERE id = ?";
        return jdbcTemplate.update(sql,
                entity.getGridId(), entity.getName(), entity.getIdCard(),
                entity.getPhone(), entity.getGender(), entity.getBirthday(),
                entity.getHouseholdType(), entity.getAddress(),
                entity.getBuildingNo(), entity.getRoomNo(),
                entity.getTags(), entity.getPhotoUrl(),
                entity.getStatus(), entity.getRemark(),
                entity.getId());
    }

    public int deleteById(Long id) {
        return jdbcTemplate.update("DELETE FROM cmn_population WHERE id = ?", id);
    }

    public long countByGridId(Long gridId) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM cmn_population WHERE grid_id = ? AND status = 'ACTIVE'", Long.class, gridId);
        return count != null ? count : 0;
    }
}
