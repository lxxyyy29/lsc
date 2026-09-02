package com.changping.platform.modules.community.mapper;

import com.changping.platform.modules.community.entity.PopulationEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

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
        java.util.Set<Long> gridIds = new java.util.LinkedHashSet<>();
        collectChildGridIds(gridId, gridIds);
        StringBuilder sql = new StringBuilder("SELECT * FROM cmn_population WHERE status = 'ACTIVE' AND grid_id IN (");
        for (int i = 0; i < gridIds.size(); i++) {
            if (i > 0) sql.append(",");
            sql.append("?");
        }
        sql.append(") ORDER BY id DESC");
        return jdbcTemplate.query(sql.toString(), ROW_MAPPER, gridIds.toArray());
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
        // 网格筛选：选中父网格时递归包含所有子网格
        if (gridId != null) {
            java.util.Set<Long> gridIds = new java.util.LinkedHashSet<>();
            collectChildGridIds(gridId, gridIds);
            if (!gridIds.isEmpty()) {
                sql.append(" AND p.grid_id IN (");
                for (int i = 0; i < gridIds.size(); i++) {
                    if (i > 0) sql.append(",");
                    sql.append("?");
                }
                sql.append(")");
                params.addAll(gridIds);
            }
        }
        sql.append(" ORDER BY p.address, p.id DESC");
        return jdbcTemplate.query(sql.toString(), (rs, rowNum) -> {
            PopulationEntity e = ROW_MAPPER.mapRow(rs, rowNum);
            e.setGridName(rs.getString("grid_name"));
            return e;
        }, params.toArray());
    }

    /** 递归收集指定网格及其所有子网格的 ID */
    private void collectChildGridIds(Long parentId, java.util.Set<Long> result) {
        result.add(parentId);
        try {
            List<Map<String, Object>> children = jdbcTemplate.queryForList(
                    "SELECT id FROM cmn_grid WHERE parent_id = ? AND status = 'ACTIVE'", parentId);
            for (Map<String, Object> child : children) {
                Long childId = ((Number) child.get("id")).longValue();
                collectChildGridIds(childId, result);
            }
        } catch (Exception e) {
            // 查询失败时仅使用父网格 ID
        }
    }

    /**
     * 整户带出：按地址集合查询全部常驻成员（排除流动），仅保留网格过滤；
     * 不应用关键字/户籍类型过滤，确保同一住址的家庭成员完整返回。
     * 地址用 TRIM 归一，与 tree() 分组键（address.trim()）保持一致。
     */
    public List<PopulationEntity> findResidentsByAddresses(List<String> addresses, Long gridId) {
        StringBuilder sql = new StringBuilder(
                "SELECT p.*, g.grid_name FROM cmn_population p LEFT JOIN cmn_grid g ON g.id = p.grid_id "
                        + "WHERE p.status = 'ACTIVE' AND COALESCE(p.household_type, '') <> 'FLOATING' AND TRIM(p.address) IN (");
        List<Object> params = new java.util.ArrayList<>();
        for (int i = 0; i < addresses.size(); i++) {
            if (i > 0) sql.append(",");
            sql.append("?");
            params.add(addresses.get(i));
        }
        sql.append(")");
        // 网格筛选：选中父网格时递归包含所有子网格
        if (gridId != null) {
            java.util.Set<Long> gridIds = new java.util.LinkedHashSet<>();
            collectChildGridIds(gridId, gridIds);
            if (!gridIds.isEmpty()) {
                sql.append(" AND p.grid_id IN (");
                for (int i = 0; i < gridIds.size(); i++) {
                    if (i > 0) sql.append(",");
                    sql.append("?");
                }
                sql.append(")");
                params.addAll(gridIds);
            }
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
