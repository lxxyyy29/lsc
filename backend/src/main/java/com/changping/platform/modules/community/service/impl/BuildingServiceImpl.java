package com.changping.platform.modules.community.service.impl;

import com.changping.platform.modules.community.entity.BuildingEntity;
import com.changping.platform.modules.community.mapper.BuildingMapper;
import com.changping.platform.modules.community.service.BuildingService;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class BuildingServiceImpl implements BuildingService {

    private final BuildingMapper mapper;
    public BuildingServiceImpl(BuildingMapper mapper) { this.mapper = mapper; }

    @Override
    public List<BuildingEntity> list(Long gridId) {
        return gridId != null ? mapper.findByGridId(gridId) : mapper.findAllActive();
    }
    @Override
    public BuildingEntity detail(Long id) { return mapper.findById(id); }
    @Override
    public boolean create(BuildingEntity e) {
        if (e.getStatus() == null) e.setStatus("ACTIVE");
        if (e.getIsGroupRental() == null) e.setIsGroupRental(0);
        mapper.insert(e);
        return true;
    }
    @Override
    public boolean update(BuildingEntity e) { return mapper.update(e) > 0; }
    @Override
    public boolean delete(Long id) { return mapper.deleteById(id) > 0; }
}
