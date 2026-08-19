package com.changping.platform.modules.community.mapper;

import com.changping.platform.modules.community.entity.PatrolTaskEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Component
public class PatrolTaskMapper {

    private final JdbcTemplate jdbcTemplate;

    private static final RowMapper<PatrolTaskEntity> ROW_MAPPER = (rs, rowNum) -> {
        PatrolTaskEntity e = new PatrolTaskEntity();
        e.setId(rs.getLong("id"));
        e.setGridId(rs.getLong("grid_id"));
        long userId = rs.getLong("user_id");
        e.setUserId(rs.wasNull() ? null : userId);
        e.setTaskName(rs.getString("task_name"));
        java.sql.Date plannedDate = rs.getDate("planned_date");
        e.setPlannedDate(plannedDate != null ? plannedDate.toLocalDate() : null);
        java.sql.Timestamp completedAt = rs.getTimestamp("completed_at");
        e.setCompletedAt(completedAt != null ? completedAt.toLocalDateTime() : null);
        e.setStatus(rs.getString("status"));
        e.setRemark(rs.getString("remark"));
        e.setCreatedAt(rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toLocalDateTime() : null);
        e.setUpdatedAt(rs.getTimestamp("updated_at") != null ? rs.getTimestamp("updated_at").toLocalDateTime() : null);
        return e;
    };

    public PatrolTaskMapper(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Map<String, Object>> findActiveSmallGrids() {
        return jdbcTemplate.queryForList(
            "SELECT id AS grid_id, grid_name FROM cmn_grid WHERE status = 'ACTIVE' AND grid_level = 3");
    }

    public List<Map<String, Object>> findActiveLargeGrids() {
        return jdbcTemplate.queryForList(
            "SELECT id AS grid_id, grid_name FROM cmn_grid WHERE status = 'ACTIVE' AND grid_level = 2");
    }

    public boolean existsTaskForWeek(Long gridId, LocalDate startOfWeek) {
        Long count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM cmn_patrol_task WHERE grid_id = ? AND planned_date >= ? AND planned_date < ?",
            Long.class, gridId, startOfWeek, startOfWeek.plusWeeks(1));
        return count != null && count > 0;
    }

    public Long insert(PatrolTaskEntity e) {
        String sql = "INSERT INTO cmn_patrol_task (grid_id, user_id, task_name, planned_date, status, remark, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, NOW(), NOW())";
        jdbcTemplate.update(sql, e.getGridId(), e.getUserId(), e.getTaskName(), e.getPlannedDate(), e.getStatus(), e.getRemark());
        return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    public int markOverdueTasks(LocalDate today) {
        return jdbcTemplate.update(
            "UPDATE cmn_patrol_task SET status = 'OVERDUE', updated_at = NOW() WHERE status = 'PENDING' AND planned_date < ?", today);
    }

    public List<PatrolTaskEntity> findByUserId(Long userId) {
        return jdbcTemplate.query("SELECT * FROM cmn_patrol_task WHERE user_id = ? ORDER BY planned_date DESC", ROW_MAPPER, userId);
    }

    public List<PatrolTaskEntity> findByGridId(Long gridId) {
        return jdbcTemplate.query("SELECT * FROM cmn_patrol_task WHERE grid_id = ? ORDER BY planned_date DESC", ROW_MAPPER, gridId);
    }

    public int completeTask(Long taskId, LocalDate completedDate) {
        return jdbcTemplate.update(
            "UPDATE cmn_patrol_task SET status = 'COMPLETED', completed_at = NOW(), updated_at = NOW() WHERE id = ?", taskId);
    }

    public List<PatrolTaskEntity> findAll() {
        return jdbcTemplate.query(
            "SELECT t.*, g.grid_name FROM cmn_patrol_task t LEFT JOIN cmn_grid g ON g.id = t.grid_id ORDER BY t.planned_date DESC, t.id DESC",
            (rs, rowNum) -> {
                PatrolTaskEntity e = ROW_MAPPER.mapRow(rs, rowNum);
                try { e.setTaskName(rs.getString("grid_name") != null ? rs.getString("grid_name") + " - " + e.getTaskName() : e.getTaskName()); } catch (Exception ignored) {}
                return e;
            });
    }

    public PatrolTaskStatistics getStatistics() {
        Long total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM cmn_patrol_task", Long.class);
        Long pending = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM cmn_patrol_task WHERE status = 'PENDING'", Long.class);
        Long completed = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM cmn_patrol_task WHERE status = 'COMPLETED'", Long.class);
        Long overdue = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM cmn_patrol_task WHERE status = 'OVERDUE'", Long.class);
        return new PatrolTaskStatistics(
                total != null ? total : 0,
                pending != null ? pending : 0,
                completed != null ? completed : 0,
                overdue != null ? overdue : 0);
    }

    public record PatrolTaskStatistics(long total, long pending, long completed, long overdue) {}
}
