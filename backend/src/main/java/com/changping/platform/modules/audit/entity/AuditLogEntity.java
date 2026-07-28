package com.changping.platform.modules.audit.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_audit_log")
public class AuditLogEntity {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String tableName;

    private String recordId;

    private String operationType;

    private String oldValues;

    private String newValues;

    private String changedFields;

    private Long operatorId;

    private String operatorName;

    private LocalDateTime operationTime;

    private String remark;
}
