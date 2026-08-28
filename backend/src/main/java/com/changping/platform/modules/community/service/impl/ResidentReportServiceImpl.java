package com.changping.platform.modules.community.service.impl;

import com.changping.platform.modules.community.entity.ResidentReportEntity;
import com.changping.platform.modules.community.mapper.ResidentReportMapper;
import com.changping.platform.modules.community.service.ResidentReportService;
import com.changping.platform.modules.event.service.EventService;
import com.changping.platform.modules.event.vo.EventDetailVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class ResidentReportServiceImpl implements ResidentReportService {

    private static final Logger log = LoggerFactory.getLogger(ResidentReportServiceImpl.class);

    private final ResidentReportMapper mapper;
    private final EventService eventService;

    public ResidentReportServiceImpl(ResidentReportMapper mapper, EventService eventService) {
        this.mapper = mapper;
        this.eventService = eventService;
    }

    @Override
    public List<ResidentReportEntity> listAll() { return mapper.findAll(); }

    @Override
    public List<ResidentReportEntity> listByStatus(String status) { return mapper.findByStatus(status); }

    @Override
    public ResidentReportEntity findByCode(String queryCode) { return mapper.findByQueryCode(queryCode); }

    @Override
    public ResidentReportEntity findById(Long id) { return mapper.findById(id); }

    @Override
    public boolean create(ResidentReportEntity entity) {
        if (entity.getStatus() == null) entity.setStatus("PENDING");
        Long id = mapper.insert(entity);
        // 居民上报统一归口至事件闭环处理中心：自动生成事件并回写关联，处置派单一律走事件中心
        try {
            EventDetailVo event = eventService.reportFromResident(
                    entity.getTitle(), entity.getContent(), entity.getReportType(), null,
                    entity.getResidentName(), entity.getResidentPhone(), null,
                    entity.getLongitude(), entity.getLatitude());
            entity.setEventId(event.id());
            mapper.updateEventId(id, event.id());
        } catch (Exception e) {
            // 建事件失败不阻断上报记录落库，由管理员在事件中心补录
            log.warn("居民上报归口建事件失败（上报记录已保存）: {}", e.getMessage());
        }
        return true;
    }
}
