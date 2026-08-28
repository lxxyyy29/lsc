package com.changping.platform.modules.community.service;

import com.changping.platform.modules.community.entity.PopulationEntity;
import com.changping.platform.modules.community.vo.PopulationTreeVo;

import java.util.List;

public interface PopulationService {

    List<PopulationEntity> list(Long gridId);

    /** 台账条件查询：关键字模糊搜索（姓名/电话/地址）+ 户籍类型 + 网格 + 人口类型(常驻/流动)筛选 */
    List<PopulationEntity> search(String keyword, String householdType, Long gridId, String populationType);

    /** 常住人口"户→成员"树：按居住地址分组为户，户主置顶，children 存放成员 */
    List<PopulationTreeVo> tree(String keyword, String householdType, Long gridId);

    PopulationEntity detail(Long id);

    boolean create(PopulationEntity entity);

    boolean update(PopulationEntity entity);

    boolean delete(Long id);
}
