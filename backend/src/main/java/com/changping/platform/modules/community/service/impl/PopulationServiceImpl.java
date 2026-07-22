package com.changping.platform.modules.community.service.impl;

import com.changping.platform.modules.community.entity.PopulationEntity;
import com.changping.platform.modules.community.mapper.PopulationMapper;
import com.changping.platform.modules.community.service.PopulationService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PopulationServiceImpl implements PopulationService {

    private final PopulationMapper populationMapper;

    public PopulationServiceImpl(PopulationMapper populationMapper) {
        this.populationMapper = populationMapper;
    }

    @Override
    public List<PopulationEntity> list(Long gridId) {
        if (gridId != null) {
            return populationMapper.findByGridId(gridId);
        }
        return populationMapper.findAllActive();
    }

    @Override
    public PopulationEntity detail(Long id) {
        return populationMapper.findById(id);
    }

    @Override
    public boolean create(PopulationEntity entity) {
        if (entity.getStatus() == null) {
            entity.setStatus("ACTIVE");
        }
        populationMapper.insert(entity);
        return true;
    }

    @Override
    public boolean update(PopulationEntity entity) {
        return populationMapper.update(entity) > 0;
    }

    @Override
    public boolean delete(Long id) {
        return populationMapper.deleteById(id) > 0;
    }
}
