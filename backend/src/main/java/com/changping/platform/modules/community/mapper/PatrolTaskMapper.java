package com.changping.platform.modules.community.mapper;

import com.changping.platform.modules.community.entity.PatrolTaskEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.List;

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
        e.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        e.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
        return e;
    };

    public PatrolTaskMapper(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<PatrolTaskEntity> findActiveSmallGrids() {
        return jdbcTemplate.query(
            "SELECT id AS grid_id, grid_name, NULL AS responsible_user_id FROM cmn_grid WHERE status = 'ACTIVE' AND grid_level = 3",
            (rs, rowNum) -> {
                PatrolTaskEntity e = new PatrolTaskEntity();
                e.setGridId(rs.getLong("grid_id"));
                e.setTaskName(rs.getString("grid_name"));
                return e;
            });
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
}
