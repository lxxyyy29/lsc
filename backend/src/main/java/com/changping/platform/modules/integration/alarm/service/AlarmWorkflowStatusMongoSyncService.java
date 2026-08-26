package com.changping.platform.modules.integration.alarm.service;

import com.changping.platform.modules.event.entity.EventEntity;
import com.changping.platform.modules.event.mapper.EventMapper;
import com.changping.platform.modules.event.service.AlarmWorkflowStatusSyncService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * @Author lxy
 * @Description //告警工作流状态MongoDB同步服务，实现AlarmWorkflowStatusSyncService接口，
 * 将SQL事件的工作流状态变更同步更新到MongoDB告警事件文档
 * @Date 2026/04/18 10:00
 */
@Service
public class AlarmWorkflowStatusMongoSyncService implements AlarmWorkflowStatusSyncService {

    private final EventMapper eventMapper;
    private final AlarmEventMongoService alarmEventMongoService;

    /**
     * @Author lxy
     * @Description //构造函数，注入事件Mapper及MongoDB告警事件服务
     * @Date 2026/04/18 10:00
     * @Param [eventMapper 事件数据库Mapper, alarmEventMongoService MongoDB告警事件服务]
     * @return void
     */
    public AlarmWorkflowStatusMongoSyncService(EventMapper eventMapper, AlarmEventMongoService alarmEventMongoService) {
        this.eventMapper = eventMapper;
        this.alarmEventMongoService = alarmEventMongoService;
    }

    /**
     * @Author lxy
     * @Description //同步工作流状态快照到MongoDB：根据SQL事件ID查找外部事件ID，再更新对应的MongoDB文档
     * @Date 2026/04/18 10:00
     * @Param [snapshot 工作流状态快照，包含SQL事件ID、当前状态和同步时间]
     * @return void
     */
    @Override
    public void syncWorkflowStatus(WorkflowStatusSnapshot snapshot) {
        if (snapshot == null || snapshot.sqlEventId() == null || !StringUtils.hasText(snapshot.currentStatus())) {
            return;
        }
        EventEntity event = eventMapper.selectDetailById(snapshot.sqlEventId());
        if (event == null || !StringUtils.hasText(event.getExternalEventId())) {
            return;
        }
        alarmEventMongoService.updateWorkflowStatus(
                event.getExternalEventId(),
                snapshot.sqlEventId(),
                snapshot.currentStatus(),
                snapshot.lastSyncedAt());
    }
}
