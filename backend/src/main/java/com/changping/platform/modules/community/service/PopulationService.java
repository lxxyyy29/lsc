package com.changping.platform.modules.community.service;

import com.changping.platform.modules.community.entity.PopulationEntity;

import java.util.List;

public interface PopulationService {

    List<PopulationEntity> list(Long gridId);

    PopulationEntity detail(Long id);

    boolean create(PopulationEntity entity);

    boolean update(PopulationEntity entity);

    boolean delete(Long id);
}
