package com.changping.platform.modules.community.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("cmn_building")
public class BuildingEntity {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long gridId;

    private String buildingNo;

    private String address;

    private Integer householdCount;

    private String landlordName;

    private String landlordPhone;

    private String fireRiskLevel;

    private Integer isGroupRental;

    private String status;

    private String remark;

    @TableField(fill = com.baomidou.mybatisplus.annotation.FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = com.baomidou.mybatisplus.annotation.FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
