package com.changping.platform.modules.community.service;

import com.changping.platform.modules.community.entity.PatrolRecordEntity;
import java.util.List;

public interface PatrolRecordService {
    List<PatrolRecordEntity> listByUser(Long userId);
    boolean create(PatrolRecordEntity entity);
}
