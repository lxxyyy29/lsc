package com.changping.platform.modules.community.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 表单字段配置（字段配置器）：控制人口库表单/导入的字段启用、排序、必填
 */
@Data
@TableName("sys_form_field_config")
public class FormFieldConfigEntity {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 配置模块（如 population） */
    private String module;

    /** 字段键（对应实体属性） */
    private String fieldKey;

    /** 字段显示名 */
    private String fieldLabel;

    /** 字段类型：text/select/date/checkbox/textarea */
    private String fieldType;

    /** select 选项（逗号分隔） */
    private String options;

    /** 是否启用 0否1是 */
    private Integer enabled;

    /** 排序 */
    private Integer sortOrder;

    /** 是否必填 0否1是 */
    private Integer required;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
