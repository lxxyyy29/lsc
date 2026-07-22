package com.changping.platform.modules.event.service.impl;

import com.changping.platform.common.exception.BusinessException;
import com.changping.platform.common.response.PagedResult;
import com.changping.platform.modules.event.domain.EventStatus;
import com.changping.platform.modules.event.entity.EventEntity;
import com.changping.platform.modules.event.mapper.EventMapper;
import com.changping.platform.modules.event.service.AlarmWorkflowStatusSyncService;
import com.changping.platform.modules.event.service.EventIgnoreService;
import com.changping.platform.modules.event.vo.EventIgnoreRecordVo;
import com.changping.platform.modules.integration.alarm.service.AlarmEventMongoService;
import java.sql.Timestamp;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @Author tangxinglin
 * @Description //事件忽略（误报）服务实现类，处理将事件标记为误报的完整业务逻辑，包括状态校验、记录写入和MongoDB同步
 * @Date 2026/04/18 10:00
 */
@Service
public class EventIgnoreServiceImpl implements EventIgnoreService {

    private static final String IGNORED_STATUS = EventStatus.IGNORED.name();
    private static final String ACTION_EVENT_IGNORE = "EVENT_IGNORE";

    private static final RowMapper<EventIgnoreRecordVo> IGNORE_RECORD_ROW_MAPPER = (rs, rowNum) ->
            new EventIgnoreRecordVo(
                    rs.getLong("r_id"),
                    rs.getLong("e_id"),
                    rs.getString("event_code"),
                    rs.getString("title"),
                    rs.getString("event_type"),
                    rs.getLong("operator_id"),
                    rs.getString("operator_name"),
                    rs.getString("reason"),
                    rs.getTimestamp("r_created_at") == null ? null
                            : rs.getTimestamp("r_created_at").toLocalDateTime());

    private final EventMapper eventMapper;
    private final JdbcTemplate jdbcTemplate;
    private final AlarmWorkflowStatusSyncService alarmWorkflowStatusSyncService;
    private final AlarmEventMongoService alarmEventMongoService;

    /**
     * @Author tangxinglin
     * @Description //构造器，注入事件Mapper、JdbcTemplate、告警工作流同步服务和MongoDB告警服务
     * @Date 2026/04/18 10:00
     * @Param [eventMapper 事件数据访问层, jdbcTemplate JDBC模板, alarmWorkflowStatusSyncService 告警工作流状态同步服务, alarmEventMongoService MongoDB告警事件服务]
     * @return void
     */
    public EventIgnoreServiceImpl(
            EventMapper eventMapper,
            JdbcTemplate jdbcTemplate,
            AlarmWorkflowStatusSyncService alarmWorkflowStatusSyncService,
            AlarmEventMongoService alarmEventMongoService) {
        this.eventMapper = eventMapper;
        this.jdbcTemplate = jdbcTemplate;
        this.alarmWorkflowStatusSyncService = alarmWorkflowStatusSyncService;
        this.alarmEventMongoService = alarmEventMongoService;
    }

    /**
     * @Author tangxinglin
     * @Description //将事件标记为误报，校验状态合法性后更新事件状态、写入忽略记录和生命周期记录，并同步至MongoDB
     * @Date 2026/04/18 10:00
     * @Param [eventId 事件主键ID, operatorId 操作人用户ID, operatorName 操作人名称, reason 忽略原因]
     * @return void
     */
    @Override
    @Transactional
    public void ignoreEvent(Long eventId, Long operatorId, String operatorName, String reason) {
        EventEntity event = eventMapper.selectDetailById(eventId);
        if (event == null) {
            throw new BusinessException("EVENT_NOT_FOUND", "事件未找到");
        }
        if (IGNORED_STATUS.equals(event.getStatus())) {
            throw new BusinessException("EVENT_ALREADY_IGNORED", "事件已被标记为误报");
        }
        if (EventStatus.DISPATCHED_TO_WORK_ORDER.name().equals(event.getStatus())
                || EventStatus.CLOSED.name().equals(event.getStatus())) {
            throw new BusinessException("EVENT_STATUS_INVALID",
                    "已派单或已关闭的事件不能标记为误报");
        }

        String previousStatus = event.getStatus();

        // Update biz_event status to IGNORED
        jdbcTemplate.update(
                "UPDATE biz_event SET status = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?",
                IGNORED_STATUS, eventId);

        // Write lifecycle record into biz_event_record
        eventMapper.insertEventRecord(eventId, previousStatus, IGNORED_STATUS, ACTION_EVENT_IGNORE, reason);

        // Persist ignore record into biz_event_ignore_record
        jdbcTemplate.update(
                "INSERT INTO biz_event_ignore_record (event_id, operator_id, operator_name, reason, created_at, updated_at) "
                        + "VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                eventId, operatorId, operatorName, reason);

        // Sync IGNORED status to MongoDB (AlarmEventDocument.workflowStatus)
        alarmWorkflowStatusSyncService.syncWorkflowStatus(eventId, IGNORED_STATUS);

        // Also update MongoDB document status via AlarmEventMongoService if externalEventId is known
        if (event.getExternalEventId() != null && !event.getExternalEventId().isBlank()) {
            alarmEventMongoService.updateWorkflowStatus(
                    event.getExternalEventId(),
                    eventId,
                    IGNORED_STATUS,
                    java.time.LocalDateTime.now());
        }
    }

    /**
     * @Author tangxinglin
     * @Description //分页查询误报事件记录列表，联表查询事件基本信息
     * @Date 2026/04/18 10:00
     * @Param [page 页码, size 每页条数]
     * @return PagedResult<EventIgnoreRecordVo> 分页误报记录列表
     */
    @Override
    public PagedResult<EventIgnoreRecordVo> listIgnoreRecords(int page, int size) {
        int safePage = Math.max(1, page);
        int safeSize = Math.max(1, Math.min(size, 100));
        int offset = (safePage - 1) * safeSize;

        long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM biz_event_ignore_record", Long.class);

        if (total == 0) {
            return PagedResult.of(List.of(), 0L, safePage, safeSize);
        }

        List<EventIgnoreRecordVo> items = jdbcTemplate.query(
                "SELECT r.id AS r_id, r.event_id AS e_id, e.event_code, e.title, e.event_type, "
                        + "r.operator_id, r.operator_name, r.reason, r.created_at AS r_created_at "
                        + "FROM biz_event_ignore_record r "
                        + "JOIN biz_event e ON e.id = r.event_id "
                        + "ORDER BY r.id DESC "
                        + "LIMIT ? OFFSET ?",
                IGNORE_RECORD_ROW_MAPPER,
                safeSize, offset);

        return PagedResult.of(items, total, safePage, safeSize);
    }
}
