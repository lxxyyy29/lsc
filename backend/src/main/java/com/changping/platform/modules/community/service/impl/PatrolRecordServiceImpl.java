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
    public List<PatrolRecordEntity> listAll() {
        return mapper.findAll();
    }

    @Override
    public boolean create(PatrolRecordEntity entity) {
        // 离线采集重试幂等:带客户端请求ID的重复提交直接视为成功
        if (entity.getClientRequestId() != null && !entity.getClientRequestId().isBlank()) {
            Long count = mapper.countByClientRequestId(entity.getClientRequestId());
            if (count != null && count > 0) {
                return true;
            }
        }
        mapper.insert(entity);
        return true;
    }
}
