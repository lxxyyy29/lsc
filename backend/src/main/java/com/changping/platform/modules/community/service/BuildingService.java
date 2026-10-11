package com.changping.platform.modules.community.service;

import com.changping.platform.modules.community.entity.BuildingEntity;
import java.util.List;

public interface BuildingService {
    List<BuildingEntity> list(Long gridId);
    /** 带筛选：房东（姓名/电话模糊）、是否群租房、消防风险等级 */
    List<BuildingEntity> search(Long gridId, String landlordName, Integer isGroupRental, String fireRiskLevel);
    BuildingEntity detail(Long id);
    boolean create(BuildingEntity entity);
    boolean update(BuildingEntity entity);
    boolean delete(Long id);
}
