package com.changping.platform.modules.community.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.changping.platform.modules.community.entity.PolicyResourceEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PolicyResourceMapper extends BaseMapper<PolicyResourceEntity> {

    /** 按类型查询启用中的政策 */
    List<PolicyResourceEntity> findActiveByType(@Param("policyType") String policyType);

    /** 查询所有启用中的政策 */
    List<PolicyResourceEntity> findAllActive();
}
