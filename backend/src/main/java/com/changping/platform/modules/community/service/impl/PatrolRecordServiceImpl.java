package com.changping.platform.modules.community.service.impl;

import com.changping.platform.modules.community.entity.PatrolRecordEntity;
import com.changping.platform.modules.community.mapper.PatrolRecordMapper;
import com.changping.platform.modules.community.service.PatrolRecordService;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class PatrolRecordServiceImpl implements PatrolRecordService {

    private final PatrolRecordMapper mapper;
    public PatrolRecordServiceImpl(PatrolRecordMapper mapper) { this.mapper = mapper; }

    @Override
    public List<PatrolRecordEntity> listByUser(Long userId) {
        return mapper.findByUserId(userId);
    }

    @Override
    public boolean create(PatrolRecordEntity entity) {
        mapper.insert(entity);
        return true;
    }
}
