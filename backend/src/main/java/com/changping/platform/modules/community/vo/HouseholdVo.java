package com.changping.platform.modules.community.vo;

import lombok.Data;

/**
 * 户列表/详情视图对象
 */
@Data
public class HouseholdVo {

    private Long id;

    private String householdNo;

    private String address;

    private Long gridId;

    private String gridName;

    /** 户主人口ID */
    private Long headPopulationId;

    /** 户主姓名 */
    private String headName;

    /** 户主性别（关系推算用） */
    private String headGender;

    private String householdType;

    /** 户内成员数 */
    private Integer memberCount;

    private String remark;

    /** 是否已指定户主 */
    private Boolean hasHead;

    /** 是否存在多个"户主"标记（数据异常提示） */
    private Boolean multipleHeads;
}
