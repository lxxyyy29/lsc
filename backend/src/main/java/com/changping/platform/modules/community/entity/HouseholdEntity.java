package com.changping.platform.modules.community.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 实有人口-户（cmn_household）
 * 户是家庭成员的稳定归属单位：户主变更、成员增减都以户为操作对象。
 */
@Data
@TableName("cmn_household")
public class HouseholdEntity {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 户号（可为空，系统不强制生成） */
    private String householdNo;

    /** 居住地址（归户分组依据） */
    private String address;

    private Long gridId;

    /** 户主（cmn_population.id） */
    private Long headPopulationId;

    private String householdType;

    private String status;

    private String remark;

    @TableField(fill = com.baomidou.mybatisplus.annotation.FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = com.baomidou.mybatisplus.annotation.FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
