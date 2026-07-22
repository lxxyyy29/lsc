package com.changping.platform.modules.community.service.impl;

import com.changping.platform.modules.community.entity.ResidentReportEntity;
import com.changping.platform.modules.community.mapper.ResidentReportMapper;
import com.changping.platform.modules.community.service.ResidentReportService;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class ResidentReportServiceImpl implements ResidentReportService {

    private final ResidentReportMapper mapper;
    public ResidentReportServiceImpl(ResidentReportMapper mapper) { this.mapper = mapper; }

    @Override
    public List<ResidentReportEntity> listAll() { return mapper.findAll(); }

    @Override
    public ResidentReportEntity findByCode(String queryCode) { return mapper.findByQueryCode(queryCode); }

    @Override
    public ResidentReportEntity findById(Long id) { return mapper.findById(id); }

    @Override
    public boolean create(ResidentReportEntity entity) {
        if (entity.getStatus() == null) entity.setStatus("PENDING");
        mapper.insert(entity);
        return true;
    }

    @Override
    public boolean handleReport(Long id, Long handlerUserId, String handleResult) {
        mapper.updateHandle(id, handlerUserId, handleResult);
        return true;
    }
}
