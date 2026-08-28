package com.changping.platform.modules.community.vo;

import com.changping.platform.modules.community.entity.PopulationEntity;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 常住人口"户→成员"树节点
 * 一级 = 户（按居住地址分组，children 存放家庭成员，户主置顶）
 * 二级 = 家庭成员节点（person 为成员实体，isHead 标记是否户主）
 */
@Data
public class PopulationTreeVo {

    /** 节点标识：house-0 / person-1 */
    private String id;

    /** 展示文本：户 = 地址；成员 = 姓名（性别 年龄 · 关系） */
    private String label;

    /** 是否户节点 */
    private Boolean isHouse;

    /** 户：居住地址 */
    private String address;

    /** 户：户主实体（无户主时为空） */
    private PopulationEntity head;

    /** 成员：是否户主 */
    private Boolean isHead;

    /** 成员：人员全量实体 */
    private PopulationEntity person;

    /** 户节点的家庭成员列表（一级节点有值，成员节点为空列表） */
    private List<PopulationTreeVo> children = new ArrayList<>();
}