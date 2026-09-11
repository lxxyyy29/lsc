package com.changping.platform.modules.community.service;

import com.changping.platform.modules.community.entity.HouseholdEntity;
import com.changping.platform.modules.community.vo.HouseholdVo;
import com.changping.platform.modules.community.vo.MemberChangeVo;

import java.util.List;
import java.util.Map;

public interface HouseholdService {

    /** 户列表（关键字：地址/户号/户主姓名/网格名） */
    List<HouseholdVo> list(String keyword, Long gridId);

    /** 户详情：{ household: HouseholdVo, members: List<PopulationEntity> } */
    Map<String, Object> detail(Long id);

    Long create(HouseholdEntity entity);

    boolean update(HouseholdEntity entity);

    /** 删除户（仅允许空户） */
    boolean delete(Long id);

    /** 把人员挂到户下；relation 为空时：户无户主则设为户主，否则置「其他」 */
    void addMember(Long householdId, Long populationId, String relation);

    /** 把人员移出户（若其为户主则清空户主） */
    void removeMember(Long householdId, Long populationId);

    /** 预览户主变更后的成员关系（不落库） */
    List<MemberChangeVo> previewChangeHead(Long householdId, Long newHeadId, String relationToOldHead);

    /** 变更户主并按亲属规则重算成员「与户主关系」 */
    Map<String, Object> changeHead(Long householdId, Long newHeadId, String relationToOldHead);

    /** 按 网格+地址 查找或创建户（导入数据归户用），返回户ID */
    Long ensureHousehold(Long gridId, String address, String householdType);
}
