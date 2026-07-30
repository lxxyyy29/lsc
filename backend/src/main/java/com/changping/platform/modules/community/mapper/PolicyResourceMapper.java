package com.changping.platform.modules.community.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.changping.platform.modules.community.entity.PolicyResourceEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface PolicyResourceMapper extends BaseMapper<PolicyResourceEntity> {

    /** 按类型查询启用中的政策 */
    @Select("SELECT * FROM cmn_policy_resource WHERE status = 'ACTIVE' AND policy_type = #{policyType} ORDER BY publish_date DESC")
    List<PolicyResourceEntity> findActiveByType(@Param("policyType") String policyType);

    /** 查询所有启用中的政策 */
    @Select("SELECT * FROM cmn_policy_resource WHERE status = 'ACTIVE' ORDER BY publish_date DESC")
    List<PolicyResourceEntity> findAllActive();
}
