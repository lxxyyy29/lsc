package com.changping.platform.modules.community.service.impl;

import com.changping.platform.modules.community.entity.PlaceEntity;
import com.changping.platform.modules.community.mapper.PlaceMapper;
import com.changping.platform.modules.community.service.PlaceService;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class PlaceServiceImpl implements PlaceService {

    private final PlaceMapper mapper;
    public PlaceServiceImpl(PlaceMapper mapper) { this.mapper = mapper; }

    @Override
    public List<PlaceEntity> list(Long gridId) {
        return gridId != null ? mapper.findByGridId(gridId) : mapper.findAllActive();
    }
    @Override
    public PlaceEntity detail(Long id) { return mapper.findById(id); }
    @Override
    public boolean create(PlaceEntity e) {
        if (e.getStatus() == null) e.setStatus("ACTIVE");
        mapper.insert(e);
        return true;
    }
    @Override
    public boolean update(PlaceEntity e) { return mapper.update(e) > 0; }
    @Override
    public boolean delete(Long id) { return mapper.deleteById(id) > 0; }
}
