package com.changping.platform.modules.audit.service;

import com.changping.platform.common.async.BatchInsertWorker;
import com.changping.platform.modules.audit.entity.AuditLogEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 审计日志异步批量写入器：AOP 切面记录的审计日志改为内存攒批后批量落库，
 * 消除每个写接口同步追加一条 INSERT 的开销。
 * SQL 与 AuditLogMapper.insert 保持一致。
 */
@Component
public class AuditBatchWriter extends BatchInsertWorker<AuditLogEntity> {

    private static final String INSERT_SQL =
            "INSERT INTO sys_audit_log (table_name, record_id, operation_type, old_values, new_values, changed_fields, operator_id, operator_name, operation_time, remark) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, NOW(), ?)";

    public AuditBatchWriter(JdbcTemplate jdbcTemplate) {
        super(jdbcTemplate, INSERT_SQL, 500, 500, "audit-log-batch-writer");
    }

    @Override
    protected Object[] toArgs(AuditLogEntity entity) {
        return new Object[]{
                entity.getTableName(),
                entity.getRecordId(),
                entity.getOperationType(),
                entity.getOldValues(),
                entity.getNewValues(),
                entity.getChangedFields(),
                entity.getOperatorId(),
                entity.getOperatorName(),
                entity.getRemark()
        };
    }
}
