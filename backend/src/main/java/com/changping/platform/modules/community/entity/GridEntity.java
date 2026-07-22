package com.changping.platform.modules.community.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("cmn_grid")
public class GridEntity {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String gridCode;

    private String gridName;

    private Integer gridLevel;

    private Long parentId;

    private String roiJson;

    private BigDecimal area;

    private Integer population;

    private Integer buildingCount;

    private Integer sortOrder;

    private String status;

    private String remark;

    @TableField(fill = com.baomidou.mybatisplus.annotation.FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = com.baomidou.mybatisplus.annotation.FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
