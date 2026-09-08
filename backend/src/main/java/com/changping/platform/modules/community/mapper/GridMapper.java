package com.changping.platform.modules.community.mapper;

import com.changping.platform.modules.community.entity.GridEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.util.List;

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
