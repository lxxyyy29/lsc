package com.changping.platform.modules.community.service;

import com.changping.platform.modules.community.entity.ResidentReportEntity;
import java.util.List;

public interface ResidentReportService {
    List<ResidentReportEntity> listAll();
    ResidentReportEntity findByCode(String queryCode);
    ResidentReportEntity findById(Long id);
    boolean create(ResidentReportEntity entity);
    boolean handleReport(Long id, Long handlerUserId, String handleResult);
}
