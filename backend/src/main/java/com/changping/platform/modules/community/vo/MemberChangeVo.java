package com.changping.platform.modules.community.vo;

import lombok.Data;

/**
 * 户主变更时单个成员的关系变更明细（用于预览与结果回显）
 */
@Data
public class MemberChangeVo {

    private Long id;

    private String name;

    private String gender;

    /** 变更前与户主关系 */
    private String oldRelation;

    /** 变更后与户主关系 */
    private String newRelation;

    /** 本次是否发生变化 */
    private Boolean changed;

    /** 是否被设为新户主 */
    private Boolean newHead;
}
