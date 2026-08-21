package com.changping.platform.modules.event.mapper;

import com.changping.platform.modules.event.entity.EventEntity;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

/**
 * @Author tangxinglin
 * @Description //事件数据访问层，基于 JdbcTemplate 实现对 biz_event 及相关表的增删查操作
 * @Date 2026/04/18 10:00
 */
@Repository
public class EventMapper {

    private static final RowMapper<EventEntity> EVENT_ROW_MAPPER = (rs, rowNum) -> {
        EventEntity entity = new EventEntity();
        entity.setId(rs.getLong("id"));
        entity.setEventCode(rs.getString("event_code"));
        entity.setExternalEventId(rs.getString("external_event_id"));
        entity.setTitle(rs.getString("title"));
        entity.setDescription(rs.getString("description"));
        entity.setSourceType(rs.getString("source_type"));
        entity.setSourceSystem(rs.getString("source_system"));
        entity.setEventType(rs.getString("event_type"));
        entity.setStatus(rs.getString("status"));
        entity.setLocation(rs.getString("incident_address"));
        entity.setLongitude(rs.getBigDecimal("longitude"));
        entity.setLatitude(rs.getBigDecimal("latitude"));
        entity.setAreaId(rs.getObject("area_id", Long.class));
        entity.setAreaName(rs.getString("area_name"));
        entity.setGridId(rs.getObject("grid_id", Long.class));
        entity.setUrgencyLevel(rs.getString("urgency_level"));
        entity.setReportSource(rs.getString("report_source"));
        entity.setArchived(rs.getObject("archived") != null ? rs.getInt("archived") : 0);
        entity.setHidden(rs.getObject("hidden") != null ? rs.getInt("hidden") : 0);
        Timestamp occurredAt = rs.getTimestamp("occurred_at");
        if (occurredAt != null) {
            entity.setOccurredAt(occurredAt.toLocalDateTime());
        }
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            entity.setCreatedAt(createdAt.toLocalDateTime());
        }
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            entity.setUpdatedAt(updatedAt.toLocalDateTime());
        }
        return entity;
    };

    private final JdbcTemplate jdbcTemplate;

    /**
     * @Author tangxinglin
     * @Description //构造器，注入 JdbcTemplate
     * @Date 2026/04/18 10:00
     * @Param [jdbcTemplate Spring JDBC模板]
     * @return void
     */
    public EventMapper(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * @Author tangxinglin
     * @Description //根据外部事件ID查询单条事件记录
     * @Date 2026/04/18 10:00
     * @Param [externalEventId 外部系统事件ID]
     * @return EventEntity 匹配的事件实体，未找到则返回null
     */
    public EventEntity selectByExternalEventId(String externalEventId) {
        List<EventEntity> results = jdbcTemplate.query(
                "SELECT id, event_code, external_event_id, title, description, source_type, source_system, event_type, "
                        + "status, incident_address, longitude, latitude, area_id, area_name, grid_id, urgency_level, report_source, occurred_at, created_at, updated_at, archived, hidden "
                        + "FROM biz_event WHERE external_event_id = ? LIMIT 1",
                EVENT_ROW_MAPPER,
                externalEventId);
        return results.isEmpty() ? null : results.get(0);
    }

    /**
     * @Author tangxinglin
     * @Description //根据主键ID查询单条事件详情
     * @Date 2026/04/18 10:00
     * @Param [id 事件主键ID]
     * @return EventEntity 匹配的事件实体，未找到则返回null
     */
    public EventEntity selectDetailById(Long id) {
        List<EventEntity> results = jdbcTemplate.query(
                "SELECT id, event_code, external_event_id, title, description, source_type, source_system, event_type, "
                        + "status, incident_address, longitude, latitude, area_id, area_name, grid_id, urgency_level, report_source, occurred_at, created_at, updated_at, archived, hidden "
                        + "FROM biz_event WHERE id = ?",
                EVENT_ROW_MAPPER,
                id);
        return results.isEmpty() ? null : results.get(0);
    }

    /**
     * @Author tangxinglin
     * @Description //根据主键ID列表批量查询事件，返回以ID为键的Map
     * @Date 2026/04/18 10:00
     * @Param [eventIds 事件主键ID列表]
     * @return Map<Long, EventEntity> 以事件ID为键、事件实体为值的有序Map
     */
    public Map<Long, EventEntity> selectByIds(List<Long> eventIds) {
        if (eventIds == null || eventIds.isEmpty()) {
            return Collections.emptyMap();
        }
        LinkedHashSet<Long> distinctIds = new LinkedHashSet<>(eventIds);
        String placeholders = distinctIds.stream().map(id -> "?").collect(Collectors.joining(", "));
        List<EventEntity> events = jdbcTemplate.query(
                "SELECT id, event_code, external_event_id, title, description, source_type, source_system, event_type, "
                        + "status, incident_address, longitude, latitude, area_id, area_name, grid_id, urgency_level, report_source, occurred_at, created_at, updated_at, archived, hidden "
                        + "FROM biz_event WHERE id IN (" + placeholders + ")",
                EVENT_ROW_MAPPER,
                distinctIds.toArray());
        Map<Long, EventEntity> eventMap = new LinkedHashMap<>();
        for (EventEntity event : events) {
            eventMap.put(event.getId(), event);
        }
        return eventMap;
    }

    /**
     * @Author tangxinglin
     * @Description //根据外部事件ID列表批量查询事件，返回以外部事件ID为键的Map
     * @Date 2026/04/18 10:00
     * @Param [externalEventIds 外部系统事件ID列表]
     * @return Map<String, EventEntity> 以外部事件ID为键、事件实体为值的有序Map
     */
    public Map<String, EventEntity> selectByExternalEventIds(List<String> externalEventIds) {
        if (externalEventIds == null || externalEventIds.isEmpty()) {
            return Collections.emptyMap();
        }
        LinkedHashSet<String> distinctIds = externalEventIds.stream()
                .filter(id -> id != null && !id.isBlank())
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (distinctIds.isEmpty()) {
            return Collections.emptyMap();
        }
        String placeholders = distinctIds.stream().map(id -> "?").collect(Collectors.joining(", "));
        List<EventEntity> events = jdbcTemplate.query(
                "SELECT id, event_code, external_event_id, title, description, source_type, source_system, event_type, "
                        + "status, incident_address, longitude, latitude, area_id, area_name, grid_id, urgency_level, report_source, occurred_at, created_at, updated_at, archived, hidden "
                        + "FROM biz_event WHERE external_event_id IN (" + placeholders + ")",
                EVENT_ROW_MAPPER,
                distinctIds.toArray());
        Map<String, EventEntity> eventMap = new LinkedHashMap<>();
        for (EventEntity event : events) {
            eventMap.put(event.getExternalEventId(), event);
        }
        return eventMap;
    }

    /**
     * @Author tangxinglin
     * @Description //分页查询事件列表，可按外部事件ID过滤，按ID倒序排列
     * @Date 2026/04/18 10:00
     * @Param [externalEventId 外部事件ID（为空则查全部）, offset 偏移量, limit 每页条数]
     * @return List<EventEntity> 事件实体列表
     */
    public List<EventEntity> queryEvents(String externalEventId, int offset, int limit) {
        if (externalEventId == null || externalEventId.isBlank()) {
            return jdbcTemplate.query(
                    "SELECT id, event_code, external_event_id, title, description, source_type, source_system, event_type, "
                            + "status, incident_address, longitude, latitude, area_id, area_name, grid_id, urgency_level, report_source, occurred_at, created_at, updated_at, archived, hidden "
                            + "FROM biz_event ORDER BY id DESC LIMIT ? OFFSET ?",
                    EVENT_ROW_MAPPER,
                    limit,
                    offset);
        }
        return jdbcTemplate.query(
                "SELECT id, event_code, external_event_id, title, description, source_type, source_system, event_type, "
                        + "status, incident_address, longitude, latitude, area_id, area_name, grid_id, urgency_level, report_source, occurred_at, created_at, updated_at, archived, hidden "
                        + "FROM biz_event WHERE external_event_id = ? ORDER BY id DESC LIMIT ? OFFSET ?",
                EVENT_ROW_MAPPER,
                externalEventId,
                limit,
                offset);
    }

    /**
     * @Description //更新事件展示隐藏标记（MySQL 侧）
     * @Param [eventId 事件主键ID, hidden 0=显示 1=隐藏]
     * @return int 影响行数
     */
    public int updateHidden(Long eventId, int hidden) {
        return jdbcTemplate.update("UPDATE biz_event SET hidden = ? WHERE id = ?", hidden, eventId);
    }

    /**
     * @Author tangxinglin
     * @Description //查询指定事件的证据文件URL列表
     * @Date 2026/04/18 10:00
     * @Param [eventId 事件主键ID]
     * @return List<String> 证据文件URL列表
     */
    public List<String> selectEvidenceReferences(Long eventId) {
        return jdbcTemplate.queryForList(
                "SELECT file_url FROM biz_media_file WHERE business_type = 'EVENT' AND business_id = ? ORDER BY id ASC",
                String.class,
                eventId);
    }

    /**
     * @Author tangxinglin
     * @Description //批量查询多个事件的证据文件URL列表，返回以事件ID为键的Map
     * @Date 2026/04/18 10:00
     * @Param [eventIds 事件主键ID列表]
     * @return Map<Long, List<String>> 以事件ID为键、证据URL列表为值的Map
     */
    public Map<Long, List<String>> selectEvidenceReferencesByEventIds(List<Long> eventIds) {
        if (eventIds == null || eventIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, List<String>> evidenceMap = new LinkedHashMap<>();
        for (Long eventId : eventIds) {
            evidenceMap.put(eventId, new ArrayList<>());
        }
        String placeholders = eventIds.stream().map(id -> "?").collect(Collectors.joining(", "));
        jdbcTemplate.query(
                "SELECT business_id, file_url FROM biz_media_file WHERE business_type = 'EVENT' AND business_id IN (" + placeholders + ") ORDER BY business_id ASC, id ASC",
                (org.springframework.jdbc.core.RowCallbackHandler) rs -> evidenceMap
                        .get(rs.getLong("business_id"))
                        .add(rs.getString("file_url")),
                eventIds.toArray());
        return evidenceMap;
    }

    /**
     * @Author tangxinglin
     * @Description //为事件插入一条证据文件引用记录
     * @Date 2026/04/18 10:00
     * @Param [eventId 事件主键ID, fileName 文件名称, fileUrl 文件访问URL]
     * @return int 受影响的行数
     */
    public int insertEvidenceReference(Long eventId, String fileName, String fileUrl) {
        return jdbcTemplate.update(
                "INSERT INTO biz_media_file (business_type, business_id, file_name, file_url, file_type, mime_type, status, created_at, updated_at) "
                        + "VALUES ('EVENT', ?, ?, ?, 'REFERENCE', NULL, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                eventId,
                fileName,
                fileUrl);
    }

    /**
     * @Author tangxinglin
     * @Description //插入事件流转记录到 biz_event_record
     * @Date 2026/04/18 10:00
     * @Param [eventId 事件主键ID, fromStatus 流转前状态, toStatus 流转后状态, actionType 操作类型, remark 备注]
     * @return int 受影响的行数
     */
    public int insertEventRecord(Long eventId, String fromStatus, String toStatus, String actionType, String remark) {
        return jdbcTemplate.update(
                "INSERT INTO biz_event_record (event_id, from_status, to_status, action_type, remark, created_at, updated_at) "
                        + "VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                eventId,
                fromStatus,
                toStatus,
                actionType,
                remark);
    }

    /**
     * @Author tangxinglin
     * @Description //插入事件实体到数据库，并将自动生成的主键回填到实体对象
     * @Date 2026/04/18 10:00
     * @Param [entity 待插入的事件实体]
     * @return int 受影响的行数
     */
    public int insert(EventEntity entity) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        int updated = jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO biz_event (event_code, external_event_id, title, description, source_type, source_system, event_type, status, incident_address, longitude, latitude, occurred_at, created_at, updated_at) "
                            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                    new String[] {"id"});
            statement.setString(1, entity.getEventCode());
            statement.setString(2, entity.getExternalEventId());
            statement.setString(3, entity.getTitle());
            statement.setString(4, entity.getDescription());
            statement.setString(5, entity.getSourceType());
            statement.setString(6, entity.getSourceSystem());
            statement.setString(7, entity.getEventType());
            statement.setString(8, entity.getStatus());
            statement.setString(9, entity.getLocation());
            statement.setBigDecimal(10, entity.getLongitude());
            statement.setBigDecimal(11, entity.getLatitude());
            if (entity.getOccurredAt() == null) {
                statement.setTimestamp(12, null);
            } else {
                statement.setTimestamp(12, Timestamp.valueOf(entity.getOccurredAt()));
            }
            return statement;
        }, keyHolder);
        Number generatedKey = keyHolder.getKey();
        if (generatedKey == null) {
            generatedKey = jdbcTemplate.queryForObject(
                    "SELECT id FROM biz_event WHERE external_event_id = ?",
                    Number.class,
                    entity.getExternalEventId());
        }
        if (generatedKey != null) {
            entity.setId(generatedKey.longValue());
        }
        return updated;
    }
}
