package com.changping.platform.modules.community.mapper;

import com.changping.platform.modules.community.entity.GridEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class GridMapper {

    private final JdbcTemplate jdbcTemplate;

    private static final RowMapper<GridEntity> ROW_MAPPER = (rs, rowNum) -> {
        GridEntity entity = new GridEntity();
        entity.setId(rs.getLong("id"));
        entity.setGridCode(rs.getString("grid_code"));
        entity.setGridName(rs.getString("grid_name"));
        entity.setGridLevel(rs.getInt("grid_level"));
        long parentId = rs.getLong("parent_id");
        entity.setParentId(rs.wasNull() ? null : parentId);
        entity.setRoiJson(rs.getString("roi_json"));
        entity.setArea(rs.getBigDecimal("area"));
        entity.setPopulation(rs.getInt("population"));
        entity.setBuildingCount(rs.getInt("building_count"));
        entity.setSortOrder(rs.getInt("sort_order"));
        entity.setStatus(rs.getString("status"));
        entity.setRemark(rs.getString("remark"));
        entity.setCreatedAt(rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toLocalDateTime() : null);
        entity.setUpdatedAt(rs.getTimestamp("updated_at") != null ? rs.getTimestamp("updated_at").toLocalDateTime() : null);
        return entity;
    };

    public GridMapper(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<GridEntity> findAllActive() {
        return jdbcTemplate.query(
                "SELECT * FROM cmn_grid WHERE status = 'ACTIVE' ORDER BY sort_order, id",
                ROW_MAPPER);
    }

    public GridEntity findById(Long id) {
        return jdbcTemplate.queryForObject(
                "SELECT * FROM cmn_grid WHERE id = ?",
                ROW_MAPPER, id);
    }

    public List<GridEntity> findByParentId(Long parentId) {
        return jdbcTemplate.query(
                "SELECT * FROM cmn_grid WHERE parent_id = ? AND status = 'ACTIVE' ORDER BY sort_order, id",
                ROW_MAPPER, parentId);
    }

    public Long insert(GridEntity entity) {
        String sql = "INSERT INTO cmn_grid (grid_code, grid_name, grid_level, parent_id, roi_json, area, population, building_count, sort_order, status, remark, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())";
        jdbcTemplate.update(sql,
                entity.getGridCode(), entity.getGridName(), entity.getGridLevel(),
                entity.getParentId(), entity.getRoiJson(), entity.getArea(),
                entity.getPopulation(), entity.getBuildingCount(),
                entity.getSortOrder(), entity.getStatus(), entity.getRemark());
        return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    public int update(GridEntity entity) {
        String sql = "UPDATE cmn_grid SET grid_code = ?, grid_name = ?, grid_level = ?, parent_id = ?, roi_json = ?, area = ?, population = ?, building_count = ?, sort_order = ?, status = ?, remark = ?, updated_at = NOW() WHERE id = ?";
        return jdbcTemplate.update(sql,
                entity.getGridCode(), entity.getGridName(), entity.getGridLevel(),
                entity.getParentId(), entity.getRoiJson(), entity.getArea(),
                entity.getPopulation(), entity.getBuildingCount(),
                entity.getSortOrder(), entity.getStatus(), entity.getRemark(),
                entity.getId());
    }

    public int deleteById(Long id) {
        return jdbcTemplate.update("DELETE FROM cmn_grid WHERE id = ?", id);
    }

    /**
     * 统计仍引用该网格的各业务表行数。
     * cmn_grid 被多张业务表通过 grid_id 外键引用（biz_event、cmn_population 等），
     * 删除前先统计，才能给出可读提示而不是让外键约束把接口打成 500。
     * 注意：新增引用网格的表时，这里和 GridServiceImpl.GRID_REFERENCE_LABELS 要同步维护。
     */
    public Map<String, Long> countReferences(Long gridId) {
        String sql = "SELECT 'biz_event' AS ref_table, COUNT(*) AS ref_count FROM biz_event WHERE grid_id = ? "
                + "UNION ALL SELECT 'cmn_population', COUNT(*) FROM cmn_population WHERE grid_id = ? "
                + "UNION ALL SELECT 'cmn_building', COUNT(*) FROM cmn_building WHERE grid_id = ? "
                + "UNION ALL SELECT 'cmn_place', COUNT(*) FROM cmn_place WHERE grid_id = ? "
                + "UNION ALL SELECT 'cmn_household', COUNT(*) FROM cmn_household WHERE grid_id = ? "
                + "UNION ALL SELECT 'cmn_resident_report', COUNT(*) FROM cmn_resident_report WHERE grid_id = ? "
                + "UNION ALL SELECT 'cmn_patrol_record', COUNT(*) FROM cmn_patrol_record WHERE grid_id = ? "
                + "UNION ALL SELECT 'cmn_patrol_task', COUNT(*) FROM cmn_patrol_task WHERE grid_id = ? "
                + "UNION ALL SELECT 'cmn_org_member', COUNT(*) FROM cmn_org_member WHERE grid_id = ?";
        Map<String, Long> counts = new LinkedHashMap<>();
        jdbcTemplate.query(sql, rs -> {
            counts.put(rs.getString("ref_table"), rs.getLong("ref_count"));
        }, gridId, gridId, gridId, gridId, gridId, gridId, gridId, gridId, gridId);
        return counts;
    }

    public long countChildren(Long parentId) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM cmn_grid WHERE parent_id = ?", Long.class, parentId);
        return count != null ? count : 0;
    }

    /** 网格编码是否已被占用（含已停用记录，避免自动编号撞唯一索引） */
    public boolean existsByCode(String gridCode) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM cmn_grid WHERE grid_code = ?", Long.class, gridCode);
        return count != null && count > 0;
    }

    /**
     * 同一父级下是否已存在同名网格（排除 excludeId 自身）。
     * cmn_grid.grid_name 没有唯一索引，重名会让树上出现「看起来一模一样」的重复节点。
     */
    public boolean existsSameNameUnderParent(String gridName, Long parentId, Long excludeId) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM cmn_grid WHERE grid_name = ? "
                        + "AND COALESCE(parent_id, 0) = COALESCE(?, 0) "
                        + "AND (? IS NULL OR id <> ?)",
                Long.class, gridName, parentId, excludeId, excludeId);
        return count != null && count > 0;
    }

    /** 收集以 rootId 为根的全部节点 ID（含自身），用于 parent_id 成环校验 */
    public List<Long> collectSubtreeIds(Long rootId) {
        return jdbcTemplate.query(
                "WITH RECURSIVE subtree AS ("
                        + "  SELECT id FROM cmn_grid WHERE id = ?"
                        + "  UNION ALL"
                        + "  SELECT g.id FROM cmn_grid g JOIN subtree s ON g.parent_id = s.id"
                        + ") SELECT id FROM subtree",
                (rs, rowNum) -> rs.getLong("id"), rootId);
    }
}
