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

    /** 所属户ID（cmn_household.id），流动人口为空 */
    private Long householdId;

    private String name;

    private String idCard;

    private String phone;

    private String gender;

    /** 年龄（出生日期自动推算 + 可手动修改） */
    private Integer age;

    private LocalDate birthday;

    private String householdType;

    /** 是否特殊人群 0否1是 */
    private Integer specialPopulation;

    /** 特殊人群类型（预置+自定义，逗号分隔） */
    private String specialPopulationType;

    /** 是否党员 0否1是（勾选后按手机号匹配系统账号，匹配到即自动在 sys_party_member 建档） */
    private Integer isPartyMember;

    /** 与户主关系（户主/妻/长子/女等） */
    private String relation;

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

    /** 所属户地址（查询时 JOIN 填充，非表字段） */
    @TableField(exist = false)
    private String householdAddress;

    /** 所属户户号（查询时 JOIN 填充，非表字段） */
    @TableField(exist = false)
    private String householdNo;

    @TableField(fill = com.baomidou.mybatisplus.annotation.FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = com.baomidou.mybatisplus.annotation.FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
