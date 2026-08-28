package com.changping.platform.modules.ledger.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_ledger_template")
public class LedgerTemplateEntity {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String templateName;

    private String templateType;

    private String description;

    private String columnsJson;

    private String filtersJson;

    private String sortField;

    private String sortOrder;

    private String status;

    private Long createdBy;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
