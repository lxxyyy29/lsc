package com.changping.platform.modules.community.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("cmn_patrol_record")
public class PatrolRecordEntity {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long gridId;

    private Long userId;

    private String patrolType;

    private BigDecimal longitude;

    private BigDecimal latitude;

    private String address;

    private String content;

    private String photoUrls;

    private String status;

    @TableField(fill = com.baomidou.mybatisplus.annotation.FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = com.baomidou.mybatisplus.annotation.FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
