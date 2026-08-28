package com.changping.platform.modules.community.service;

import com.changping.platform.modules.community.entity.BuildingEntity;
import java.util.List;

public interface BuildingService {
    List<BuildingEntity> list(Long gridId);
    BuildingEntity detail(Long id);
    boolean create(BuildingEntity entity);
    boolean update(BuildingEntity entity);
    boolean delete(Long id);
}
