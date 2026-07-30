package com.changping.platform.modules.community.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 政策资源实体，对应 cmn_policy_resource 表
 * 覆盖低保/养老/救助/医保/惠民等政策，支撑政策找人
 */
@Data
@TableName("cmn_policy_resource")
public class PolicyResourceEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 政策编码 */
    private String policyCode;

    /** 政策标题 */
    private String title;

    /** 政策类型：LOW_INCOME=低保 ELDERLY=养老 RESCUE=救助 MEDICAL=医保 BENEFIT=惠民 OTHER=其他 */
    private String policyType;

    /** 政策说明 */
    private String description;

    /** 资格条件说明 */
    private String eligibility;

    /** 匹配标签（逗号分隔），用于政策找人 */
    private String tags;

    /** 状态：ACTIVE=启用 DISABLED=停用 */
    private String status;

    /** 发布日期 */
    private LocalDate publishDate;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
