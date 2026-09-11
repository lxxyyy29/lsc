package com.changping.platform.modules.community.mapper;

import com.changping.platform.modules.community.entity.HouseholdEntity;
import com.changping.platform.modules.community.vo.HouseholdVo;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 实有人口-户 数据访问（JdbcTemplate 手写 SQL，与 PopulationMapper 风格一致）
 */
@Component
public class HouseholdMapper {

    private final JdbcTemplate jdbcTemplate;

    private static final RowMapper<HouseholdEntity> ROW_MAPPER = (rs, rowNum) -> {
        HouseholdEntity e = new HouseholdEntity();
        e.setId(rs.getLong("id"));
        e.setHouseholdNo(rs.getString("household_no"));
        e.setAddress(rs.getString("address"));
        long gridId = rs.getLong("grid_id");
        e.setGridId(rs.wasNull() ? null : gridId);
        long headId = rs.getLong("head_population_id");
        e.setHeadPopulationId(rs.wasNull() ? null : headId);
        e.setHouseholdType(rs.getString("household_type"));
        e.setStatus(rs.getString("status"));
        e.setRemark(rs.getString("remark"));
        e.setCreatedAt(rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toLocalDateTime() : null);
        e.setUpdatedAt(rs.getTimestamp("updated_at") != null ? rs.getTimestamp("updated_at").toLocalDateTime() : null);
        return e;
    };

    public HouseholdMapper(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public HouseholdEntity findById(Long id) {
        List<HouseholdEntity> rows = jdbcTemplate.query(
                "SELECT * FROM cmn_household WHERE id = ?", ROW_MAPPER, id);
        return rows.isEmpty() ? null : rows.get(0);
    }

    /**
     * 户列表：带出户主姓名/性别、成员数、同户"户主"标记数（用于异常提示）
     */
    public List<HouseholdVo> list(String keyword, Long gridId) {
        StringBuilder sql = new StringBuilder(
                "SELECT h.*, g.grid_name, p.name AS head_name, p.gender AS head_gender, "
                        + "(SELECT COUNT(*) FROM cmn_population m WHERE m.household_id = h.id AND m.status = 'ACTIVE') AS member_count, "
                        + "(SELECT COUNT(*) FROM cmn_population m2 WHERE m2.household_id = h.id AND m2.status = 'ACTIVE' AND m2.relation = '户主') AS head_count "
                        + "FROM cmn_household h "
                        + "LEFT JOIN cmn_grid g ON g.id = h.grid_id "
                        + "LEFT JOIN cmn_population p ON p.id = h.head_population_id "
                        + "WHERE h.status = 'ACTIVE'");
        List<Object> params = new ArrayList<>();
        if (keyword != null && !keyword.isBlank()) {
            String like = "%" + keyword.trim() + "%";
            sql.append(" AND (h.address LIKE ? OR h.household_no LIKE ? OR p.name LIKE ? OR g.grid_name LIKE ?)");
            params.add(like);
            params.add(like);
            params.add(like);
            params.add(like);
        }
        if (gridId != null) {
            Set<Long> gridIds = new LinkedHashSet<>();
            collectChildGridIds(gridId, gridIds);
            if (!gridIds.isEmpty()) {
                sql.append(" AND h.grid_id IN (");
                for (int i = 0; i < gridIds.size(); i++) {
                    if (i > 0) {
                        sql.append(",");
                    }
                    sql.append("?");
                }
                sql.append(")");
                params.addAll(gridIds);
            }
        }
        sql.append(" ORDER BY h.id DESC");
        return jdbcTemplate.query(sql.toString(), (rs, rowNum) -> {
            HouseholdVo vo = new HouseholdVo();
            vo.setId(rs.getLong("id"));
            vo.setHouseholdNo(rs.getString("household_no"));
            vo.setAddress(rs.getString("address"));
            long gid = rs.getLong("grid_id");
            vo.setGridId(rs.wasNull() ? null : gid);
            long headId = rs.getLong("head_population_id");
            vo.setHeadPopulationId(rs.wasNull() ? null : headId);
            vo.setHeadName(rs.getString("head_name"));
            vo.setHeadGender(rs.getString("head_gender"));
            vo.setHouseholdType(rs.getString("household_type"));
            vo.setGridName(rs.getString("grid_name"));
            vo.setRemark(rs.getString("remark"));
            int memberCount = rs.getInt("member_count");
            int headCount = rs.getInt("head_count");
            vo.setMemberCount(memberCount);
            vo.setHasHead(headId != 0 || headCount > 0);
            vo.setMultipleHeads(headCount > 1);
            return vo;
        }, params.toArray());
    }

    public Long insert(HouseholdEntity e) {
        jdbcTemplate.update(
                "INSERT INTO cmn_household (household_no, address, grid_id, head_population_id, household_type, status, remark, created_at, updated_at) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, NOW(), NOW())",
                e.getHouseholdNo(), e.getAddress(), e.getGridId(), e.getHeadPopulationId(),
                e.getHouseholdType(), e.getStatus() == null ? "ACTIVE" : e.getStatus(), e.getRemark());
        return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    public int update(HouseholdEntity e) {
        return jdbcTemplate.update(
                "UPDATE cmn_household SET household_no = ?, address = ?, grid_id = ?, household_type = ?, status = ?, remark = ?, updated_at = NOW() WHERE id = ?",
                e.getHouseholdNo(), e.getAddress(), e.getGridId(), e.getHouseholdType(),
                e.getStatus(), e.getRemark(), e.getId());
    }

    public int updateHead(Long householdId, Long populationId) {
        return jdbcTemplate.update(
                "UPDATE cmn_household SET head_population_id = ?, updated_at = NOW() WHERE id = ?",
                populationId, householdId);
    }

    /** 仅在户尚无户主时设置户主（批量导入用，避免多行"户主"互相覆盖） */
    public int updateHeadIfAbsent(Long householdId, Long populationId) {
        return jdbcTemplate.update(
                "UPDATE cmn_household SET head_population_id = ?, updated_at = NOW() WHERE id = ? AND head_population_id IS NULL",
                populationId, householdId);
    }

    public int deleteById(Long id) {
        return jdbcTemplate.update("DELETE FROM cmn_household WHERE id = ?", id);
    }

    public int countActiveMembers(Long householdId) {
        Long c = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM cmn_population WHERE household_id = ? AND status = 'ACTIVE'",
                Long.class, householdId);
        return c == null ? 0 : c.intValue();
    }

    /** 按 网格 + 地址 查户（导入/批量建户去重用），无则返回 null */
    public Long findIdByAddress(Long gridId, String address) {
        if (address == null || address.isBlank()) {
            return null;
        }
        List<Long> ids = jdbcTemplate.queryForList(
                "SELECT id FROM cmn_household WHERE status = 'ACTIVE' AND address = ? "
                        + "AND COALESCE(grid_id, -1) = COALESCE(?, -1) ORDER BY id LIMIT 1",
                Long.class, address.trim(), gridId);
        return ids.isEmpty() ? null : ids.get(0);
    }

    /** 递归收集指定网格及其所有子网格的 ID */
    private void collectChildGridIds(Long parentId, Set<Long> result) {
        if (parentId == null || !result.add(parentId)) {
            return;
        }
        try {
            List<Map<String, Object>> children = jdbcTemplate.queryForList(
                    "SELECT id FROM cmn_grid WHERE parent_id = ? AND status = 'ACTIVE'", parentId);
            for (Map<String, Object> child : children) {
                collectChildGridIds(((Number) child.get("id")).longValue(), result);
            }
        } catch (Exception ignored) {
            // 查询失败时仅使用父网格 ID
        }
    }
}
