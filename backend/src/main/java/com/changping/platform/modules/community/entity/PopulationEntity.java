package com.changping.platform.modules.community.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("cmn_population")
public class PopulationEntity {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long gridId;

    private String name;

    private String idCard;

    private String phone;

    private String gender;

    private LocalDate birthday;

    private String householdType;

    private String address;

    private String buildingNo;

    private String roomNo;

    private String tags;

    private String photoUrl;

    private String status;

    private String remark;

    /** 关联网格名称（查询时 JOIN 填充，非表字段） */
    @TableField(exist = false)
    private String gridName;

    @TableField(fill = com.baomidou.mybatisplus.annotation.FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = com.baomidou.mybatisplus.annotation.FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
