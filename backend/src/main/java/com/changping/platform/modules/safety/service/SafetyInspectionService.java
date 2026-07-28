package com.changping.platform.modules.safety.service;

import com.changping.platform.modules.safety.entity.SafetyInspectionEntity;
import com.changping.platform.modules.safety.mapper.SafetyInspectionMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SafetyInspectionService {

    private final SafetyInspectionMapper mapper;

    public SafetyInspectionService(SafetyInspectionMapper mapper) {
        this.mapper = mapper;
    }

    public void createInspection(SafetyInspectionEntity entity) {
        mapper.insert(entity);
    }

    public List<SafetyInspectionEntity> getByMerchant(Long merchantId) {
        return mapper.findByMerchantId(merchantId);
    }

    public List<SafetyInspectionEntity> getAll() {
        return mapper.findAll();
    }

    public Object getStatistics() {
        return mapper.getStatistics();
    }

    public int markOverdue() {
        return mapper.markOverdue();
    }

    public boolean updateStatus(Long id, String status) {
        return mapper.updateRectificationStatus(id, status) > 0;
    }
}
