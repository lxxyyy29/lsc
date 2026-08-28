-- 数据变更审计日志表
CREATE TABLE IF NOT EXISTS sys_audit_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    table_name VARCHAR(64) NOT NULL COMMENT '操作的表名',
    record_id VARCHAR(64) NOT NULL COMMENT '记录ID',
    operation_type VARCHAR(16) NOT NULL COMMENT '操作类型: CREATE/UPDATE/DELETE',
    old_values JSON COMMENT '变更前的值',
    new_values JSON COMMENT '变更后的值',
    changed_fields JSON COMMENT '变更的字段列表',
    operator_id BIGINT COMMENT '操作人ID',
    operator_name VARCHAR(64) COMMENT '操作人姓名',
    operation_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    remark VARCHAR(255) COMMENT '备注',
    INDEX idx_table_record (table_name, record_id),
    INDEX idx_operation_time (operation_time),
    INDEX idx_operator (operator_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据变更审计日志';
