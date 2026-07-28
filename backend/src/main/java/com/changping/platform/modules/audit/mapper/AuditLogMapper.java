package com.changping.platform.modules.audit.mapper;

import com.changping.platform.modules.audit.entity.AuditLogEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.util.List;

@Component
public class AuditLogMapper {

    private final JdbcTemplate jdbcTemplate;

    private static final RowMapper<AuditLogEntity> ROW_MAPPER = (rs, rowNum) -> {
        AuditLogEntity e = new AuditLogEntity();
        e.setId(rs.getLong("id"));
        e.setTableName(rs.getString("table_name"));
        e.setRecordId(rs.getString("record_id"));
        e.setOperationType(rs.getString("operation_type"));
        e.setOldValues(rs.getString("old_values"));
        e.setNewValues(rs.getString("new_values"));
        e.setChangedFields(rs.getString("changed_fields"));
        long opId = rs.getLong("operator_id");
        e.setOperatorId(rs.wasNull() ? null : opId);
        e.setOperatorName(rs.getString("operator_name"));
        java.sql.Timestamp ts = rs.getTimestamp("operation_time");
        e.setOperationTime(ts != null ? ts.toLocalDateTime() : null);
        e.setRemark(rs.getString("remark"));
        return e;
    };

    public AuditLogMapper(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void insert(AuditLogEntity entity) {
        String sql = "INSERT INTO sys_audit_log (table_name, record_id, operation_type, old_values, new_values, changed_fields, operator_id, operator_name, operation_time, remark) VALUES (?, ?, ?, ?, ?, ?, ?, ?, NOW(), ?)";
        jdbcTemplate.update(sql,
                entity.getTableName(),
                entity.getRecordId(),
                entity.getOperationType(),
                entity.getOldValues(),
                entity.getNewValues(),
                entity.getChangedFields(),
                entity.getOperatorId(),
                entity.getOperatorName(),
                entity.getRemark());
    }

    public List<AuditLogEntity> findByTableAndRecord(String tableName, String recordId) {
        return jdbcTemplate.query(
                "SELECT * FROM sys_audit_log WHERE table_name = ? AND record_id = ? ORDER BY operation_time ASC",
                ROW_MAPPER, tableName, recordId);
    }

    public List<AuditLogEntity> findByPage(String tableName, String recordId, int page, int size) {
        int offset = (Math.max(1, page) - 1) * size;
        StringBuilder sql = new StringBuilder("SELECT * FROM sys_audit_log WHERE 1=1");
        java.util.List<Object> params = new java.util.ArrayList<>();
        if (tableName != null && !tableName.isEmpty()) {
            sql.append(" AND table_name = ?");
            params.add(tableName);
        }
        if (recordId != null && !recordId.isEmpty()) {
            sql.append(" AND record_id = ?");
            params.add(recordId);
        }
        sql.append(" ORDER BY operation_time DESC LIMIT ? OFFSET ?");
        params.add(size);
        params.add(offset);
        return jdbcTemplate.query(sql.toString(), ROW_MAPPER, params.toArray());
    }

    public long count(String tableName, String recordId) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM sys_audit_log WHERE 1=1");
        java.util.List<Object> params = new java.util.ArrayList<>();
        if (tableName != null && !tableName.isEmpty()) {
            sql.append(" AND table_name = ?");
            params.add(tableName);
        }
        if (recordId != null && !recordId.isEmpty()) {
            sql.append(" AND record_id = ?");
            params.add(recordId);
        }
        Long count = jdbcTemplate.queryForObject(sql.toString(), Long.class, params.toArray());
        return count != null ? count : 0;
    }

    public AuditLogEntity findById(Long id) {
        List<AuditLogEntity> list = jdbcTemplate.query(
                "SELECT * FROM sys_audit_log WHERE id = ?", ROW_MAPPER, id);
        return list.isEmpty() ? null : list.get(0);
    }

    public List<String> findDistinctTables() {
        return jdbcTemplate.queryForList(
                "SELECT DISTINCT table_name FROM sys_audit_log ORDER BY table_name", String.class);
    }
}
