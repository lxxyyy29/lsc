package com.changping.platform.modules.integration.alarm.service;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.changping.platform.modules.biz.service.BizManagementService;
import com.changping.platform.modules.event.domain.EventStatus;
import com.changping.platform.modules.event.entity.EventEntity;
import com.changping.platform.modules.event.mapper.EventMapper;
import com.changping.platform.modules.integration.alarm.dto.NormalizedAlarmEvent;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * @Author lxy
 * @Description //事件投影桥接服务，将已归一化的告警事件投影为SQL事件表中的EventEntity，
 * 若同一外部事件ID已存在则复用，并同步工作流状态快照到MongoDB
 * @Date 2026/04/18 10:00
 */
@Service
public class EventProjectionBridgeService {

    private static final String EVENT_INTAKE = "EVENT_INTAKE";

    private final EventMapper eventMapper;
    private final AlarmEventMongoService alarmEventMongoService;
    private final BizManagementService bizManagementService;
    private final JdbcTemplate jdbcTemplate;

    /**
     * @Author lxy
     * @Description //构造函数，注入事件Mapper、MongoDB告警服务、业务管理服务及JdbcTemplate
     * @Date 2026/04/18 10:00
     * @Param [eventMapper 事件数据库Mapper, alarmEventMongoService MongoDB告警事件服务, bizManagementService 业务管理服务, jdbcTemplate JDBC模板]
     * @return void
     */
    public EventProjectionBridgeService(EventMapper eventMapper, AlarmEventMongoService alarmEventMongoService,
                                        BizManagementService bizManagementService, JdbcTemplate jdbcTemplate) {
        this.eventMapper = eventMapper;
        this.alarmEventMongoService = alarmEventMongoService;
        this.bizManagementService = bizManagementService;
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * @Author lxy
     * @Description //根据归一化告警事件创建或复用SQL事件投影，若已存在则同步工作流状态并返回，
     * 若不存在则插入新事件、解析区域、保存证据引用并触发状态同步
     * @Date 2026/04/18 10:00
     * @Param [normalizedEvent 归一化的告警事件]
     * @return EventEntity 创建或复用的SQL事件实体
     */
    @Transactional
    public EventEntity createOrReuseProjection(NormalizedAlarmEvent normalizedEvent) {
        EventEntity existing = eventMapper.selectByExternalEventId(normalizedEvent.externalEventId());
        if (existing != null) {
            syncWorkflowSnapshot(existing.getExternalEventId(), existing.getId(), existing.getStatus());
            return existing;
        }

        EventEntity entity = new EventEntity();
        entity.setEventCode("EVT-" + IdWorker.getIdStr());
        entity.setExternalEventId(normalizedEvent.externalEventId());
        entity.setSourceType(normalizedEvent.sourceType());
        entity.setSourceSystem(normalizedEvent.sourceSystem());
        entity.setEventType(normalizedEvent.eventType());
        entity.setTitle(normalizedEvent.title());
        entity.setDescription(normalizedEvent.description());
        entity.setOccurredAt(normalizedEvent.occurredAt());
        entity.setLocation(normalizedEvent.address());
        entity.setLongitude(normalizedEvent.longitude());
        entity.setLatitude(normalizedEvent.latitude());
        entity.setStatus(EventStatus.WAITING_DISPATCH.name());
        eventMapper.insert(entity);

        // Resolve area from coordinates
        BizManagementService.AreaOptionItem area = bizManagementService.resolveAreaByCoordinates(entity.getLongitude(), entity.getLatitude());
        if (area != null) {
            entity.setAreaId(area.id());
            entity.setAreaName(area.areaName());
            jdbcTemplate.update("UPDATE biz_event SET area_id = ?, area_name = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?",
                    area.id(), area.areaName(), entity.getId());
        }

        List<String> evidenceReferences = normalizedEvent.evidenceReferences() == null ? List.of() : normalizedEvent.evidenceReferences();
        for (String reference : evidenceReferences) {
            eventMapper.insertEvidenceReference(entity.getId(), extractFileName(reference), reference);
        }
        eventMapper.insertEventRecord(entity.getId(), null, EventStatus.WAITING_DISPATCH.name(), EVENT_INTAKE, EVENT_INTAKE);
        syncWorkflowSnapshot(entity.getExternalEventId(), entity.getId(), entity.getStatus());
        return entity;
    }

    /**
     * @Author lxy
     * @Description //将SQL事件的当前工作流状态同步到MongoDB告警文档
     * @Date 2026/04/18 10:00
     * @Param [externalEventId 外部事件ID, sqlEventId SQL事件ID, currentStatus 当前工作流状态]
     * @return void
     */
    public void syncWorkflowSnapshot(String externalEventId, Long sqlEventId, String currentStatus) {
        alarmEventMongoService.updateWorkflowStatus(externalEventId, sqlEventId, currentStatus, LocalDateTime.now());
    }

    /**
     * @Author lxy
     * @Description //从证据引用URL中提取文件名，无法提取时返回默认值"evidence"
     * @Date 2026/04/18 10:00
     * @Param [reference 证据引用URL]
     * @return String 文件名
     */
    private String extractFileName(String reference) {
        if (!StringUtils.hasText(reference)) {
            return "evidence";
        }
        int index = reference.lastIndexOf('/');
        if (index >= 0 && index < reference.length() - 1) {
            return reference.substring(index + 1);
        }
        return reference;
    }
}
