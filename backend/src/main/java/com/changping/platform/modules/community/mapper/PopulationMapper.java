package com.changping.platform.modules.community.mapper;

import com.changping.platform.modules.community.entity.PopulationEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

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
        entity.setAge(rs.getObject("age") != null ? rs.getInt("age") : null);
        java.sql.Date birthday = rs.getDate("birthday");
        entity.setBirthday(birthday != null ? birthday.toLocalDate() : null);
        entity.setHouseholdType(rs.getString("household_type"));
        entity.setSpecialPopulation(rs.getObject("special_population") != null ? rs.getInt("special_population") : null);
        entity.setSpecialPopulationType(rs.getString("special_population_type"));
        entity.setRelation(rs.getString("relation"));
        entity.setAddress(rs.getString("address"));
        entity.setBuildingNo(rs.getString("building_no"));
        entity.setRoomNo(rs.getString("room_no"));
        entity.setTags(rs.getString("tags"));
        entity.setPhotoUrl(rs.getString("photo_url"));
        entity.setStatus(rs.getString("status"));
        entity.setRemark(rs.getString("remark"));
        entity.setCreatedAt(rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toLocalDateTime() : null);
        entity.setUpdatedAt(rs.getTimestamp("updated_at") != null ? rs.getTimestamp("updated_at").toLocalDateTime() : null);
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

    /**
     * 台账条件查询：姓名/电话/地址模糊搜索 + 户籍类型/网格筛选，JOIN 带出网格名称
     * populationType: RESIDENT=常驻(非FLOATING) / FLOATING=流动
     */
    public List<PopulationEntity> search(String keyword, String householdType, Long gridId, String populationType) {
        StringBuilder sql = new StringBuilder(
                "SELECT p.*, g.grid_name FROM cmn_population p LEFT JOIN cmn_grid g ON g.id = p.grid_id WHERE p.status = 'ACTIVE'");
        java.util.List<Object> params = new java.util.ArrayList<>();
        if (keyword != null && !keyword.isBlank()) {
            String like = "%" + keyword.trim() + "%";
            // 模糊匹配姓名/电话/地址/证件/楼栋/房号，并支持按网格名称搜索（如"第一网格一区和二区"）
            sql.append(" AND (p.name LIKE ? OR p.phone LIKE ? OR p.address LIKE ? OR p.id_card LIKE ? OR p.building_no LIKE ? OR p.room_no LIKE ? OR g.grid_name LIKE ?)");
            params.add(like); params.add(like); params.add(like); params.add(like); params.add(like); params.add(like); params.add(like);
        }
        if (householdType != null && !householdType.isBlank()) {
            sql.append(" AND p.household_type = ?");
            params.add(householdType);
        }
        // 常驻 = 非 FLOATING；流动 = FLOATING
        if ("FLOATING".equalsIgnoreCase(populationType)) {
            sql.append(" AND p.household_type = 'FLOATING'");
        } else if ("RESIDENT".equalsIgnoreCase(populationType)) {
            sql.append(" AND COALESCE(p.household_type, '') <> 'FLOATING'");
        }
        if (gridId != null) {
            sql.append(" AND p.grid_id = ?");
            params.add(gridId);
        }
        sql.append(" ORDER BY p.address, p.id DESC");
        return jdbcTemplate.query(sql.toString(), (rs, rowNum) -> {
            PopulationEntity e = ROW_MAPPER.mapRow(rs, rowNum);
            e.setGridName(rs.getString("grid_name"));
            return e;
        }, params.toArray());
    }

    public PopulationEntity findById(Long id) {
        return jdbcTemplate.queryForObject(
                "SELECT * FROM cmn_population WHERE id = ?",
                ROW_MAPPER, id);
    }

    public Long insert(PopulationEntity entity) {
        String sql = "INSERT INTO cmn_population (grid_id, name, id_card, phone, gender, age, birthday, household_type, special_population, special_population_type, relation, address, building_no, room_no, tags, photo_url, status, remark, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())";
        jdbcTemplate.update(sql,
                entity.getGridId(), entity.getName(), entity.getIdCard(),
                entity.getPhone(), entity.getGender(), entity.getAge(),
                entity.getBirthday(), entity.getHouseholdType(),
                entity.getSpecialPopulation(), entity.getSpecialPopulationType(), entity.getRelation(),
                entity.getAddress(), entity.getBuildingNo(), entity.getRoomNo(),
                entity.getTags(), entity.getPhotoUrl(),
                entity.getStatus(), entity.getRemark());
        return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    public int update(PopulationEntity entity) {
        String sql = "UPDATE cmn_population SET grid_id = ?, name = ?, id_card = ?, phone = ?, gender = ?, age = ?, birthday = ?, household_type = ?, special_population = ?, special_population_type = ?, relation = ?, address = ?, building_no = ?, room_no = ?, tags = ?, photo_url = ?, status = ?, remark = ?, updated_at = NOW() WHERE id = ?";
        return jdbcTemplate.update(sql,
                entity.getGridId(), entity.getName(), entity.getIdCard(),
                entity.getPhone(), entity.getGender(), entity.getAge(),
                entity.getBirthday(), entity.getHouseholdType(),
                entity.getSpecialPopulation(), entity.getSpecialPopulationType(), entity.getRelation(),
                entity.getAddress(), entity.getBuildingNo(), entity.getRoomNo(),
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
