package com.changping.platform.modules.event.service.impl;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.changping.platform.common.exception.BusinessException;
import com.changping.platform.common.response.PagedResult;
import com.changping.platform.modules.auth.security.AuthenticatedUserContextHolder;
import com.changping.platform.modules.biz.service.BizManagementService;
import com.changping.platform.modules.event.domain.EventStatus;
import com.changping.platform.modules.event.dto.CreateEventRequest;
import com.changping.platform.modules.event.entity.EventEntity;
import com.changping.platform.modules.event.mapper.EventMapper;
import com.changping.platform.modules.event.service.AlarmWorkflowStatusSyncService;
import com.changping.platform.modules.event.service.EventService;
import com.changping.platform.modules.event.vo.EventDetailVo;
import com.changping.platform.modules.integration.alarm.document.AlarmEventDocument;
import com.changping.platform.modules.integration.alarm.service.AlarmEventMongoService;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * @Author tangxinglin
 * @Description //事件业务服务实现类，负责事件的创建、查询、分页列表及级联删除，整合MySQL与MongoDB双数据源
 * @Date 2026/04/18 10:00
 */
@Service
public class EventServiceImpl implements EventService {

    private static final String EVENT_INTAKE = "EVENT_INTAKE";
    private static final Logger log = LoggerFactory.getLogger(EventServiceImpl.class);

    private final EventMapper eventMapper;
    private final AlarmWorkflowStatusSyncService alarmWorkflowStatusSyncService;
    private final AlarmEventMongoService alarmEventMongoService;
    private final JdbcTemplate jdbcTemplate;
    private final BizManagementService bizManagementService;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    /**
     * @Author tangxinglin
     * @Description //构造器，注入事件Mapper、告警状态同步服务、MongoDB服务、JdbcTemplate和业务管理服务
     * @Date 2026/04/18 10:00
     * @Param [eventMapper 事件数据访问层, alarmWorkflowStatusSyncService 告警工作流状态同步服务, alarmEventMongoService MongoDB告警事件服务, jdbcTemplate JDBC模板, bizManagementService 业务管理服务]
     * @return void
     */
    public EventServiceImpl(
            EventMapper eventMapper,
            AlarmWorkflowStatusSyncService alarmWorkflowStatusSyncService,
            AlarmEventMongoService alarmEventMongoService,
            JdbcTemplate jdbcTemplate,
            BizManagementService bizManagementService,
            com.fasterxml.jackson.databind.ObjectMapper objectMapper) {
        this.eventMapper = eventMapper;
        this.alarmWorkflowStatusSyncService = alarmWorkflowStatusSyncService;
        this.alarmEventMongoService = alarmEventMongoService;
        this.jdbcTemplate = jdbcTemplate;
        this.bizManagementService = bizManagementService;
        this.objectMapper = objectMapper;
    }

    /**
     * 启动时回填存量事件紧急程度：Mongo 文档缺少 urgencyLevel 的历史事件，
     * 从 MySQL biz_event.urgency_level 同步补齐（否则列表按紧急程度筛选查不到存量数据）
     */
    @PostConstruct
    public void backfillUrgencyOnStartup() {
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                    "SELECT external_event_id, urgency_level FROM biz_event WHERE external_event_id IS NOT NULL");
            Map<String, String> externalIdToUrgency = new HashMap<>();
            for (Map<String, Object> row : rows) {
                Object id = row.get("external_event_id");
                Object level = row.get("urgency_level");
                if (id != null && level != null) {
                    externalIdToUrgency.put(String.valueOf(id), String.valueOf(level));
                }
            }
            int updated = alarmEventMongoService.backfillUrgencyLevels(externalIdToUrgency);
            if (updated > 0) {
                log.info("[urgency-backfill] 存量事件紧急程度回填完成，共 {} 条", updated);
            }
        } catch (Exception e) {
            log.warn("[urgency-backfill] 存量事件紧急程度回填失败（不影响启动）: {}", e.getMessage());
        }
    }

    /**
     * @Author tangxinglin
     * @Description //创建事件，校验证据和外部ID唯一性后持久化，并同步区域信息、证据文件、生命周期记录到MySQL和MongoDB
     * @Date 2026/04/18 10:00
     * @Param [request 创建事件请求对象]
     * @return EventDetailVo 新建的事件详情
     */
    @Override
    @Transactional
    public EventDetailVo createEvent(CreateEventRequest request) {
        if (eventMapper.selectByExternalEventId(request.externalEventId()) != null) {
            throw new BusinessException("EVENT_EXTERNAL_ID_DUPLICATE", "外部事件 ID 已存在");
        }

        EventEntity entity = new EventEntity();
        entity.setEventCode(generateEventCode());
        entity.setExternalEventId(request.externalEventId());
        entity.setSourceType(request.sourceType());
        entity.setSourceSystem(request.sourceSystem());
        entity.setEventType(request.eventType());
        entity.setTitle(request.title());
        entity.setDescription(request.description());
        entity.setOccurredAt(request.occurredAt());
        entity.setLocation(request.location());
        entity.setLongitude(request.longitude());
        entity.setLatitude(request.latitude());
        // 紧急程度：创建表单三色必选；未传或非法时默认 GREEN（历史导入/三方回调兼容）
        String urgency = request.urgencyLevel();
        if (urgency == null || !List.of("GREEN", "YELLOW", "RED").contains(urgency)) {
            urgency = "GREEN";
        }
        entity.setUrgencyLevel(urgency);
        // 事件接入后先进入待审核，由审核员审核通过后进入待派单（闭环处置）
        entity.setStatus(EventStatus.PENDING_AUDIT.name());

        try {
            eventMapper.insert(entity);
        } catch (DuplicateKeyException exception) {
            throw new BusinessException("EVENT_EXTERNAL_ID_DUPLICATE", "外部事件 ID 已存在");
        }

        // Store area as query index (display uses dynamic resolution)
        BizManagementService.AreaOptionItem areaAtIntake = bizManagementService.resolveAreaByCoordinates(entity.getLongitude(), entity.getLatitude());
        if (areaAtIntake != null) {
            entity.setAreaId(areaAtIntake.id());
            entity.setAreaName(areaAtIntake.areaName());
            jdbcTemplate.update("UPDATE biz_event SET area_id = ?, area_name = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?",
                    areaAtIntake.id(), areaAtIntake.areaName(), entity.getId());
        }

        // 所属网格：前端已选则用所选；未选但有坐标时按坐标射线法自动匹配最小网格（grid_level=3 优先，回退 level=2）
        Long gridId = request.gridId();
        if (gridId == null) {
            gridId = resolveGridByCoordinates(entity.getLongitude(), entity.getLatitude());
        }
        if (gridId != null) {
            entity.setGridId(gridId);
            jdbcTemplate.update("UPDATE biz_event SET grid_id = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?", gridId, entity.getId());
        }

        // H5 端工作人员上报：记录上报人并标记来源为网格员，便于“我的上报”按人追溯
        if ("H5".equalsIgnoreCase(request.sourceType())) {
            AuthenticatedUserContextHolder.getOptional().ifPresent(user -> jdbcTemplate.update(
                    "UPDATE biz_event SET report_user_id = ?, report_user_name = ?, report_source = 'GRID_MEMBER', updated_at = CURRENT_TIMESTAMP WHERE id = ?",
                    user.id(), user.userName(), entity.getId()));
        }

        // 证据图片为可选字段，未传时按空列表处理，避免 NPE
        for (String reference : request.evidenceReferences() != null ? request.evidenceReferences() : java.util.List.<String>of()) {
            eventMapper.insertEvidenceReference(entity.getId(), extractFileName(reference), reference);
        }
        eventMapper.insertEventRecord(entity.getId(), null, EventStatus.PENDING_AUDIT.name(), EVENT_INTAKE, EVENT_INTAKE);
        alarmEventMongoService.upsertManualEvent(request, entity.getId(), entity.getEventCode(), entity.getStatus());
        alarmWorkflowStatusSyncService.syncWorkflowStatus(entity.getId(), EventStatus.PENDING_AUDIT.name());
        return getEventDetail(entity.getId());
    }

    @Override
    public EventDetailVo importFrom12345(String title, String description, String eventType, String location,
                                        String reporterName, String reporterPhone, String externalNo) {
        String externalId = externalNo != null && !externalNo.isBlank() ? externalNo : "12345-" + System.currentTimeMillis();
        CreateEventRequest request = new CreateEventRequest(
                externalId, "PUBLIC", "12345",
                eventType != null ? eventType : "COMPLAINT", title, description,
                java.time.LocalDateTime.now(), location,
                null, null, java.util.List.of(), null, null);
        EventDetailVo vo = createEvent(request);
        // 更新来源标记为 12345，并记录来电人信息
        jdbcTemplate.update("UPDATE biz_event SET report_source = '12345', report_user_name = ?, report_phone = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?",
                reporterName, reporterPhone, vo.id());
        // 追加转办单号到操作记录
        jdbcTemplate.update(
                "INSERT INTO biz_event_record (event_id, from_status, to_status, action_type, operator_name, remark, created_at, updated_at) VALUES (?, ?, ?, '12345_IMPORT', '系统', ?, NOW(), NOW())",
                vo.id(), EventStatus.PENDING_AUDIT.name(), EventStatus.PENDING_AUDIT.name(),
                "12345 热线转办导入，单号：" + externalNo);
        return vo;
    }

    @Override
    public EventDetailVo reportFromProperty(String title, String description, String eventType, String location,
                                          String reporterName, String propertyName) {
        String externalId = "PROPERTY-" + System.currentTimeMillis();
        String locationFull = location != null ? location : "";
        if (propertyName != null && !propertyName.isBlank()) {
            locationFull = propertyName + (locationFull.isBlank() ? "" : " - " + locationFull);
        }
        CreateEventRequest request = new CreateEventRequest(
                externalId, "PROPERTY", "PROPERTY_REPORT",
                eventType != null ? eventType : "COMPLAINT", title, description,
                java.time.LocalDateTime.now(), locationFull.isBlank() ? "拔蛟窝社区" : locationFull,
                null, null, java.util.List.of(), null, null);
        EventDetailVo vo = createEvent(request);
        // 更新来源标记为 PROPERTY，记录上报人
        jdbcTemplate.update("UPDATE biz_event SET report_source = 'PROPERTY', report_user_name = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?",
                reporterName, vo.id());
        jdbcTemplate.update(
                "INSERT INTO biz_event_record (event_id, from_status, to_status, action_type, operator_name, remark, created_at, updated_at) VALUES (?, ?, ?, 'PROPERTY_REPORT', '系统', ?, NOW(), NOW())",
                vo.id(), EventStatus.PENDING_AUDIT.name(), EventStatus.PENDING_AUDIT.name(),
                "物业上报，上报人：" + (reporterName != null ? reporterName : "匿名"));
        return vo;
    }

    @Override
    @Transactional
    public EventDetailVo reportFromResident(String title, String description, String eventType, String location,
                                            String reporterName, String reporterPhone, Long reporterUserId,
                                            java.math.BigDecimal longitude, java.math.BigDecimal latitude) {
        String externalId = "RESIDENT-" + System.currentTimeMillis();
        CreateEventRequest request = new CreateEventRequest(
                externalId, "PUBLIC", "RESIDENT_REPORT",
                eventType != null && !eventType.isBlank() ? eventType : "OTHER", title, description,
                java.time.LocalDateTime.now(),
                location != null && !location.isBlank() ? location : "拔蛟窝社区",
                longitude, latitude, java.util.List.of(), null, null);
        EventDetailVo vo = createEvent(request);
        // 来源标记为 RESIDENT，记录上报人信息，便于“我的上报”与事件详情追溯
        jdbcTemplate.update("UPDATE biz_event SET report_source = 'RESIDENT', report_user_id = ?, report_user_name = ?, report_phone = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?",
                reporterUserId, reporterName, reporterPhone, vo.id());
        jdbcTemplate.update(
                "INSERT INTO biz_event_record (event_id, from_status, to_status, action_type, operator_name, remark, created_at, updated_at) VALUES (?, ?, ?, 'RESIDENT_REPORT', '系统', ?, NOW(), NOW())",
                vo.id(), EventStatus.PENDING_AUDIT.name(), EventStatus.PENDING_AUDIT.name(),
                "居民上报统一归口，上报人：" + (reporterName != null ? reporterName : "匿名"));
        return vo;
    }

    /**
     * @Author tangxinglin
     * @Description //根据主键ID获取事件详情，优先从MongoDB取数据并与MySQL合并，降级为仅MySQL数据
     * @Date 2026/04/18 10:00
     * @Param [id 事件主键ID]
     * @return EventDetailVo 事件详情
     */
    @Override
    public EventDetailVo getEventDetail(Long id) {
        EventEntity entity = null;
        if (id != null) {
            entity = eventMapper.selectDetailById(id);
        }
        if (entity == null) {
            throw new BusinessException("EVENT_NOT_FOUND", "事件未找到");
        }
        Optional<AlarmEventDocument> document = alarmEventMongoService.findBySqlEventId(id);
        if (document.isEmpty()) {
            document = alarmEventMongoService.findByExternalEventId(entity.getExternalEventId());
        }
        AlarmEventDocument doc = document.orElse(null);
        if (doc != null) {
            return toDetailVo(entity, doc);
        }
        return toDetailVo(entity);
    }

    @Override
    public EventDetailVo getEventDetailByExternalEventId(String externalEventId) {
        // 先尝试从MySQL查询
        EventEntity entity = eventMapper.selectByExternalEventId(externalEventId);
        // 从MongoDB获取文档
        Optional<AlarmEventDocument> document = alarmEventMongoService.findByExternalEventId(externalEventId);
        if (entity == null && document.isEmpty()) {
            throw new BusinessException("EVENT_NOT_FOUND", "事件未找到");
        }
        if (entity != null) {
            return document.map(value -> toDetailVo(entity, value)).orElseGet(() -> toDetailVo(entity));
        }
        // 仅有MongoDB数据的情况
        AlarmEventDocument doc = document.get();
        return toListVo(doc, null, java.util.Collections.emptyMap());
    }

    /**
     * @Author tangxinglin
     * @Description //分页查询事件列表，以MongoDB为主数据源分页，再用MySQL数据补全状态和工作流快照
     * @Date 2026/04/18 10:00
     * @Param [externalEventId 外部事件ID（可选）, page 页码, size 每页条数, status 状态过滤（可选）, startDate 开始日期（可选）, endDate 结束日期（可选）, areaId 区域ID（可选）]
     * @return PagedResult<EventDetailVo> 分页事件详情列表
     */
    @Override
    public PagedResult<EventDetailVo> queryEvents(String externalEventId, int page, int size, String status, String urgencyLevel, String startDate, String endDate, Long areaId, boolean excludeHidden, boolean includeArchived, String sourceSystem, boolean onlyArchived) {
        int safePage = Math.max(1, page);
        int safeSize = Math.max(1, Math.min(size, 100));

        // 事件中心列表默认排除已派单与已忽略的事件,保持列表只展示真正需要处理的事件；
        // 勾选"显示已归档"时不排除任何状态，否则归档的 IGNORED/DISPATCHED 事件仍被过滤导致筛选失效
        List<String> excludeStatuses = includeArchived
                ? List.of()
                : List.of(EventStatus.DISPATCHED_TO_WORK_ORDER.name(), EventStatus.IGNORED.name());

        // 解析日期范围(支持 yyyy-MM-dd 或 ISO 时间格式)，结束日期取当天末尾保证全天包含
        LocalDateTime start = parseStartDate(startDate);
        LocalDateTime end = parseEndDate(endDate);

        // Get total count and current page directly from MongoDB (native pagination)
        long total = alarmEventMongoService.countEvents(externalEventId, excludeStatuses, status, start, end, excludeHidden, urgencyLevel, includeArchived, sourceSystem, onlyArchived);
        List<AlarmEventDocument> pageDocuments = alarmEventMongoService.queryEvents(externalEventId, safePage, safeSize, excludeStatuses, status, start, end, excludeHidden, urgencyLevel, includeArchived, sourceSystem, onlyArchived);

        if (pageDocuments.isEmpty()) {
            return PagedResult.of(List.of(), total, safePage, safeSize);
        }

        // Enrich with MySQL data
        Map<String, EventEntity> sqlEventMap = eventMapper.selectByExternalEventIds(pageDocuments.stream()
                .map(AlarmEventDocument::getExternalEventId)
                .filter(StringUtils::hasText)
                .toList());
        Map<Long, WorkflowSnapshot> workflowSnapshots = buildWorkflowSnapshots(sqlEventMap.values().stream()
                .map(EventEntity::getId)
                .filter(id -> id != null)
                .toList());
        List<EventDetailVo> items = pageDocuments.stream()
                .map(document -> toListVo(document, sqlEventMap.get(document.getExternalEventId()), workflowSnapshots))
                .toList();
        return PagedResult.of(items, total, safePage, safeSize);
    }

    /**
     * @Author tangxinglin
     * @Description //解析开始日期字符串，支持 yyyy-MM-dd 或 ISO 日期时间格式
     * @Date 2026/08/12 10:00
     * @Param [value 日期字符串(可为空)]
     * @return LocalDateTime 解析结果，非法或为空时返回 null
     */
    // ---- 网格多边形缓存（30s TTL，按坐标自动关联网格用） ----

    private record CachedGridPolygon(long id, int gridLevel, double[] polyLng, double[] polyLat) {}
    private volatile List<CachedGridPolygon> cachedGridPolygons = null;
    private volatile long gridCacheTimestamp = 0;

    /**
     * 按坐标自动匹配网格：优先匹配最小网格（grid_level=3 小网格），未命中则回退大网格（grid_level=2）。
     * 缓存 30 秒 TTL，避免每次创建事件都查库。坐标为空或无匹配时返回 null。
     */
    private Long resolveGridByCoordinates(java.math.BigDecimal longitude, java.math.BigDecimal latitude) {
        if (longitude == null || latitude == null) return null;
        double lng = longitude.doubleValue();
        double lat = latitude.doubleValue();
        List<CachedGridPolygon> grids = getCachedGridPolygons();
        // 优先小网格（level=3），更精确
        for (CachedGridPolygon g : grids) {
            if (g.gridLevel() == 3 && pointInPolygon(lng, lat, g.polyLng(), g.polyLat())) {
                return g.id();
            }
        }
        // 回退大网格（level=2）
        for (CachedGridPolygon g : grids) {
            if (g.gridLevel() == 2 && pointInPolygon(lng, lat, g.polyLng(), g.polyLat())) {
                return g.id();
            }
        }
        return null;
    }

    private List<CachedGridPolygon> getCachedGridPolygons() {
        long now = System.currentTimeMillis();
        List<CachedGridPolygon> cached = cachedGridPolygons;
        if (cached != null && (now - gridCacheTimestamp) < 30_000) {
            return cached;
        }
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT id, grid_level, roi_json FROM cmn_grid WHERE status = 'ACTIVE' AND roi_json IS NOT NULL");
        List<CachedGridPolygon> result = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            String roiJson = (String) row.get("roi_json");
            if (roiJson == null || roiJson.isBlank()) continue;
            try {
                List<double[]> pts = objectMapper.readValue(roiJson, new com.fasterxml.jackson.core.type.TypeReference<List<double[]>>() {});
                if (pts.size() < 3) continue;
                double[] polyLng = new double[pts.size()];
                double[] polyLat = new double[pts.size()];
                for (int i = 0; i < pts.size(); i++) {
                    polyLng[i] = pts.get(i)[0];
                    polyLat[i] = pts.get(i)[1];
                }
                int level = ((Number) row.get("grid_level")).intValue();
                result.add(new CachedGridPolygon(((Number) row.get("id")).longValue(), level, polyLng, polyLat));
            } catch (Exception ignored) {}
        }
        cachedGridPolygons = result;
        gridCacheTimestamp = now;
        return result;
    }

    private boolean pointInPolygon(double x, double y, double[] polyX, double[] polyY) {
        int n = polyX.length;
        boolean inside = false;
        for (int i = 0, j = n - 1; i < n; j = i++) {
            if ((polyY[i] > y) != (polyY[j] > y)
                    && x < (polyX[j] - polyX[i]) * (y - polyY[i]) / (polyY[j] - polyY[i]) + polyX[i]) {
                inside = !inside;
            }
        }
        return inside;
    }

    private LocalDateTime parseStartDate(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String v = value.trim();
        try {
            return LocalDateTime.parse(v);
        } catch (Exception ignored) {
            try {
                return java.time.LocalDate.parse(v).atStartOfDay();
            } catch (Exception ignored2) {
                return null;
            }
        }
    }

    /**
     * @Author tangxinglin
     * @Description //解析结束日期字符串，日期格式时取当天 23:59:59.999，保证当天数据包含在内
     * @Date 2026/08/12 10:00
     * @Param [value 日期字符串(可为空)]
     * @return LocalDateTime 解析结果，非法或为空时返回 null
     */
    private LocalDateTime parseEndDate(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String v = value.trim();
        try {
            return LocalDateTime.parse(v);
        } catch (Exception ignored) {
            try {
                return java.time.LocalDate.parse(v).atTime(java.time.LocalTime.MAX);
            } catch (Exception ignored2) {
                return null;
            }
        }
    }

    /**
     * @Author tangxinglin
     * @Description //级联删除事件及其所有关联数据（工单、审核流程、媒体文件、生命周期记录等），并清理MongoDB文档
     * @Date 2026/04/18 10:00
     * @Param [eventId 事件主键ID]
     * @return void
     */
    @Override
    @Transactional
    public void deleteEvent(Long eventId) {
        EventEntity entity = eventMapper.selectDetailById(eventId);
        if (entity == null) {
            throw new BusinessException("EVENT_NOT_FOUND", "事件未找到");
        }

        // Phase 1: Clean work order sub-tree
        List<Map<String, Object>> workOrders = jdbcTemplate.queryForList(
                "SELECT id, process_instance_id FROM biz_work_order WHERE source_event_id = ?", eventId);
        for (Map<String, Object> wo : workOrders) {
            Long piId = wo.get("process_instance_id") != null
                    ? ((Number) wo.get("process_instance_id")).longValue() : null;
            if (piId != null) {
                jdbcTemplate.update(
                        "DELETE FROM biz_media_file WHERE business_type = 'ACTION_RECORD' AND business_id IN (SELECT id FROM biz_process_action_record WHERE process_instance_id = ?)",
                        piId);
                jdbcTemplate.update("DELETE FROM biz_process_action_record WHERE process_instance_id = ?", piId);
                jdbcTemplate.update("DELETE FROM biz_process_instance_node WHERE process_instance_id = ?", piId);
            }
            jdbcTemplate.update("DELETE FROM biz_work_order WHERE id = ?", ((Number) wo.get("id")).longValue());
            if (piId != null) {
                jdbcTemplate.update("DELETE FROM biz_process_instance WHERE id = ?", piId);
            }
        }

        // Phase 2: Clean audit sub-tree
        List<Map<String, Object>> auditPIs = jdbcTemplate.queryForList(
                "SELECT id FROM biz_process_instance WHERE business_type = 'EVENT_AUDIT' AND business_id = ?", eventId);
        for (Map<String, Object> pi : auditPIs) {
            Long piId = ((Number) pi.get("id")).longValue();
            jdbcTemplate.update("DELETE FROM biz_process_action_record WHERE process_instance_id = ?", piId);
            jdbcTemplate.update("DELETE FROM biz_process_instance_node WHERE process_instance_id = ?", piId);
        }
        jdbcTemplate.update("DELETE FROM biz_audit_record WHERE event_id = ?", eventId);
        for (Map<String, Object> pi : auditPIs) {
            jdbcTemplate.update("DELETE FROM biz_process_instance WHERE id = ?", ((Number) pi.get("id")).longValue());
        }

        // Phase 3: Clean event direct children
        jdbcTemplate.update("DELETE FROM biz_media_file WHERE business_type = 'EVENT' AND business_id = ?", eventId);
        jdbcTemplate.update("DELETE FROM biz_event_record WHERE event_id = ?", eventId);
        jdbcTemplate.update("DELETE FROM biz_event_ignore_record WHERE event_id = ?", eventId);
        jdbcTemplate.update("UPDATE biz_patrol_task SET linked_event_id = NULL WHERE linked_event_id = ?", eventId);

        // Phase 4: Delete event itself
        jdbcTemplate.update("DELETE FROM biz_event WHERE id = ?", eventId);

        // Phase 5: MongoDB cleanup
        if (StringUtils.hasText(entity.getExternalEventId())) {
            alarmEventMongoService.deleteByExternalEventId(entity.getExternalEventId());
        }
    }

    /**
     * @Author tangxinglin
     * @Description //仅用MySQL数据构建事件详情VO（无MongoDB文档时的降级路径）
     * @Date 2026/04/18 10:00
     * @Param [entity 事件实体]
     * @return EventDetailVo 事件详情
     */
    private EventDetailVo toDetailVo(EventEntity entity) {
        return toDetailVo(entity, eventMapper.selectEvidenceReferences(entity.getId()));
    }

    /**
     * @Author tangxinglin
     * @Description //合并MySQL事件实体和MongoDB文档构建事件详情VO（单条详情页路径）
     * @Date 2026/04/18 10:00
     * @Param [entity MySQL事件实体, document MongoDB告警事件文档]
     * @return EventDetailVo 合并后的事件详情
     */
    private EventDetailVo toDetailVo(EventEntity entity, AlarmEventDocument document) {
        List<String> evidenceReferences = normalizeEvidenceReferences(document.getEvidenceReferences());
        if (evidenceReferences.isEmpty()) {
            evidenceReferences = eventMapper.selectEvidenceReferences(entity.getId());
        }
        String currentStatus = firstNonBlank(workflowStatus(document), entity.getStatus(), document.getStatus());
        String area = firstNonBlank(address(document), entity.getLocation());
        WorkflowSnapshot workflowSnapshot = workflowSnapshot(entity.getId());
        return new EventDetailVo(
                entity.getId(),
                firstNonBlank(entity.getEventCode(), stringValue(document.getNormalizedPayload(), "eventCode"), generateFallbackEventCode(entity.getId())),
                entity.getExternalEventId(),
                firstNonBlank(document.getSourceType(), entity.getSourceType()),
                firstNonBlank(document.getSourceSystem(), entity.getSourceSystem()),
                firstNonBlank(document.getEventType(), entity.getEventType()),
                firstNonBlank(document.getTitle(), entity.getTitle()),
                firstNonBlank(document.getDescription(), entity.getDescription()),
                currentStatus,
                currentStatus,
                document.getOccurredAt() == null ? entity.getOccurredAt() : document.getOccurredAt(),
                area,
                area,
                firstNonNull(longitude(document), entity.getLongitude()),
                firstNonNull(latitude(document), entity.getLatitude()),
                evidenceReferences,
                lifecycleRecords(document),
                workflowSnapshot.processTemplateId(),
                workflowSnapshot.processTemplateName(),
                workflowSnapshot.currentNodeName(),
                workflowSnapshot.currentNodeStatus(),
                workflowSnapshot.dispatchable(),
                entity.getAreaId(),
                entity.getAreaName(),
                entity.getGridId(),
                null,
                entity.getUrgencyLevel(),
                entity.getReportSource(),
                entity.getArchived() != null && entity.getArchived() == 1,
                Boolean.TRUE.equals(document.getHidden()));
    }

    /**
     * @Author tangxinglin
     * @Description //合并MongoDB文档和MySQL实体构建列表项VO，工作流快照从预加载Map中获取
     * @Date 2026/04/18 10:00
     * @Param [document MongoDB告警事件文档, entity MySQL事件实体（可为null）, workflowSnapshots 工作流快照Map]
     * @return EventDetailVo 列表项事件详情
     */
    private EventDetailVo toListVo(AlarmEventDocument document, EventEntity entity, Map<Long, WorkflowSnapshot> workflowSnapshots) {
        Long sqlEventId = entity != null ? entity.getId() : workflowSqlEventId(document);
        String externalEventId = entity != null ? entity.getExternalEventId() : document.getExternalEventId();
        String currentStatus = firstNonBlank(workflowStatus(document), entity == null ? null : entity.getStatus(), document.getStatus());
        String area = firstNonBlank(address(document), entity == null ? null : entity.getLocation());
        WorkflowSnapshot workflowSnapshot = sqlEventId == null ? WorkflowSnapshot.empty() : workflowSnapshots.getOrDefault(sqlEventId, WorkflowSnapshot.empty());
        return new EventDetailVo(
                sqlEventId,
                firstNonBlank(entity == null ? null : entity.getEventCode(), stringValue(document.getNormalizedPayload(), "eventCode"), generateFallbackEventCode(sqlEventId)),
                externalEventId,
                firstNonBlank(document.getSourceType(), entity == null ? null : entity.getSourceType()),
                firstNonBlank(document.getSourceSystem(), entity == null ? null : entity.getSourceSystem()),
                firstNonBlank(document.getEventType(), entity == null ? null : entity.getEventType()),
                firstNonBlank(document.getTitle(), entity == null ? null : entity.getTitle()),
                firstNonBlank(document.getDescription(), entity == null ? null : entity.getDescription()),
                currentStatus,
                currentStatus,
                document.getOccurredAt(),
                area,
                area,
                firstNonNull(longitude(document), entity == null ? null : entity.getLongitude()),
                firstNonNull(latitude(document), entity == null ? null : entity.getLatitude()),
                normalizeEvidenceReferences(document.getEvidenceReferences()),
                lifecycleRecords(document),
                workflowSnapshot.processTemplateId(),
                workflowSnapshot.processTemplateName(),
                workflowSnapshot.currentNodeName(),
                workflowSnapshot.currentNodeStatus(),
                workflowSnapshot.dispatchable(),
                entity == null ? null : entity.getAreaId(),
                entity == null ? null : entity.getAreaName(),
                entity == null ? null : entity.getGridId(),
                null,
                entity == null ? null : entity.getUrgencyLevel(),
                entity == null ? null : entity.getReportSource(),
                entity != null && entity.getArchived() != null && entity.getArchived() == 1,
                Boolean.TRUE.equals(document.getHidden()));
    }

    /**
     * @Author tangxinglin
     * @Description //对MongoDB文档列表去重，以sqlEventId或externalEventId为键，保留最新或数据最完整的文档
     * @Date 2026/04/18 10:00
     * @Param [documents MongoDB告警事件文档列表]
     * @return List<AlarmEventDocument> 去重后的文档列表
     */
    private List<AlarmEventDocument> deduplicateDocuments(List<AlarmEventDocument> documents) {
        java.util.LinkedHashMap<String, AlarmEventDocument> deduplicated = new java.util.LinkedHashMap<>();
        for (AlarmEventDocument document : documents) {
            String key = documentKey(document);
            AlarmEventDocument existing = deduplicated.get(key);
            if (existing == null || shouldReplace(existing, document)) {
                deduplicated.put(key, document);
            }
        }
        return new java.util.ArrayList<>(deduplicated.values());
    }

    /**
     * @Author tangxinglin
     * @Description //生成文档去重Key：优先使用sqlEventId，其次externalEventId，最后mongoId
     * @Date 2026/04/18 10:00
     * @Param [document MongoDB告警事件文档]
     * @return String 去重Key字符串
     */
    private String documentKey(AlarmEventDocument document) {
        Long sqlEventId = workflowSqlEventId(document);
        if (sqlEventId != null) {
            return "sql:" + sqlEventId;
        }
        if (StringUtils.hasText(document.getExternalEventId())) {
            return "external:" + document.getExternalEventId().trim();
        }
        return "mongo:" + firstNonBlank(document.getId(), stringValue(document.getNormalizedPayload(), "eventCode"), stringValue(document.getRawPayload(), "eventCode"));
    }

    /**
     * @Author tangxinglin
     * @Description //批量构建事件ID到工作流快照的映射，查询审核流程实例当前节点状态
     * @Date 2026/04/18 10:00
     * @Param [eventIds 事件主键ID列表]
     * @return Map<Long, WorkflowSnapshot> 事件ID到工作流快照的映射
     */
    private Map<Long, WorkflowSnapshot> buildWorkflowSnapshots(List<Long> eventIds) {
        if (eventIds == null || eventIds.isEmpty()) {
            return Map.of();
        }
        String placeholders = String.join(", ", eventIds.stream().map(id -> "?").toList());
        Map<Long, WorkflowSnapshot> snapshots = new HashMap<>();
        jdbcTemplate.query(
                "SELECT pi.business_id AS event_id, pi.template_id, pt.template_name, pin.node_name, pin.status AS node_status, pi.status AS process_status "
                        + "FROM biz_process_instance pi "
                        + "LEFT JOIN biz_process_template pt ON pt.id = pi.template_id "
                        + "LEFT JOIN biz_process_instance_node pin ON pin.process_instance_id = pi.id AND pin.is_current = 1 "
                        + "WHERE pi.business_type IN ('EVENT_AUDIT', 'EVENT_WORK_ORDER') AND pi.business_id IN (" + placeholders + ")",
                rs -> {
                    snapshots.put(
                            rs.getLong("event_id"),
                            new WorkflowSnapshot(
                                    rs.getLong("template_id"),
                                    rs.getString("template_name"),
                                    rs.getString("node_name"),
                                    rs.getString("node_status"),
                                    "WAITING_DISPATCH".equalsIgnoreCase(rs.getString("process_status"))));
                },
                eventIds.toArray());
        return snapshots;
    }

    /**
     * @Author tangxinglin
     * @Description //查询单个事件的工作流快照（当前审核节点信息）
     * @Date 2026/04/18 10:00
     * @Param [eventId 事件主键ID]
     * @return WorkflowSnapshot 工作流快照，事件不存在时返回空快照
     */
    private WorkflowSnapshot workflowSnapshot(Long eventId) {
        if (eventId == null) {
            return WorkflowSnapshot.empty();
        }
        return buildWorkflowSnapshots(List.of(eventId)).getOrDefault(eventId, WorkflowSnapshot.empty());
    }

    /**
     * @Author tangxinglin
     * @Description //判断候选文档是否应替换已存在文档：优先有sqlEventId > 有workflowStatus > 更新时间更新
     * @Date 2026/04/18 10:00
     * @Param [existing 已存在的文档, candidate 候选文档]
     * @return boolean true表示候选文档应替换已存在文档
     */
    private boolean shouldReplace(AlarmEventDocument existing, AlarmEventDocument candidate) {
        boolean existingHasSql = workflowSqlEventId(existing) != null;
        boolean candidateHasSql = workflowSqlEventId(candidate) != null;
        if (candidateHasSql != existingHasSql) {
            return candidateHasSql;
        }

        boolean existingHasWorkflowStatus = StringUtils.hasText(workflowStatus(existing));
        boolean candidateHasWorkflowStatus = StringUtils.hasText(workflowStatus(candidate));
        if (candidateHasWorkflowStatus != existingHasWorkflowStatus) {
            return candidateHasWorkflowStatus;
        }

        java.time.LocalDateTime existingTime = firstNonNull(existing.getUpdatedAt(), existing.getOccurredAt(), existing.getCreatedAt());
        java.time.LocalDateTime candidateTime = firstNonNull(candidate.getUpdatedAt(), candidate.getOccurredAt(), candidate.getCreatedAt());
        if (existingTime == null) {
            return candidateTime != null;
        }
        if (candidateTime == null) {
            return false;
        }
        return candidateTime.isAfter(existingTime);
    }

    /**
     * @Author tangxinglin
     * @Description //用MySQL实体和证据列表构建事件详情VO（无MongoDB文档时的完整降级路径）
     * @Date 2026/04/18 10:00
     * @Param [entity MySQL事件实体, evidenceReferences 证据文件URL列表]
     * @return EventDetailVo 事件详情
     */
    private EventDetailVo toDetailVo(EventEntity entity, List<String> evidenceReferences) {
        WorkflowSnapshot workflowSnapshot = workflowSnapshot(entity.getId());
        return new EventDetailVo(
                entity.getId(),
                entity.getEventCode(),
                entity.getExternalEventId(),
                entity.getSourceType(),
                entity.getSourceSystem(),
                entity.getEventType(),
                entity.getTitle(),
                entity.getDescription(),
                entity.getStatus(),
                entity.getStatus(),
                entity.getOccurredAt(),
                entity.getLocation(),
                entity.getLocation(),
                entity.getLongitude(),
                entity.getLatitude(),
                evidenceReferences == null ? List.of() : evidenceReferences,
                List.of(),
                workflowSnapshot.processTemplateId(),
                workflowSnapshot.processTemplateName(),
                workflowSnapshot.currentNodeName(),
                workflowSnapshot.currentNodeStatus(),
                workflowSnapshot.dispatchable(),
                entity.getAreaId(),
                entity.getAreaName(),
                entity.getGridId(),
                null,
                entity.getUrgencyLevel(),
                entity.getReportSource(),
                entity.getArchived() != null && entity.getArchived() == 1,
                entity.getHidden() != null && entity.getHidden() == 1);
    }

    /**
     * @Author tangxinglin
     * @Description //从MongoDB文档的生命周期记录列表转换为VO列表
     * @Date 2026/04/18 10:00
     * @Param [document MongoDB告警事件文档]
     * @return List<EventDetailVo.LifecycleRecordVo> 生命周期记录VO列表
     */
    private List<EventDetailVo.LifecycleRecordVo> lifecycleRecords(AlarmEventDocument document) {
        if (document == null || document.getLifecycle() == null) {
            return List.of();
        }
        return document.getLifecycle().stream()
                .map(record -> new EventDetailVo.LifecycleRecordVo(record.getAction(), record.getStatus(), record.getRemark(), record.getOccurredAt()))
                .toList();
    }

    /**
     * @Author tangxinglin
     * @Description //过滤证据引用列表中的空白项，返回有效URL列表
     * @Date 2026/04/18 10:00
     * @Param [evidenceReferences 原始证据文件URL列表]
     * @return List<String> 过滤后的有效证据URL列表
     */
    private List<String> normalizeEvidenceReferences(List<String> evidenceReferences) {
        if (evidenceReferences == null || evidenceReferences.isEmpty()) {
            return List.of();
        }
        List<String> normalized = new ArrayList<>();
        for (String evidenceReference : evidenceReferences) {
            if (StringUtils.hasText(evidenceReference)) {
                normalized.add(evidenceReference);
            }
        }
        return normalized;
    }

    /**
     * @Author tangxinglin
     * @Description //从MongoDB文档的workflowStatus中提取当前状态字符串
     * @Date 2026/04/18 10:00
     * @Param [document MongoDB告警事件文档]
     * @return String 当前工作流状态，文档为null时返回null
     */
    private String workflowStatus(AlarmEventDocument document) {
        if (document == null || document.getWorkflowStatus() == null) {
            return null;
        }
        return document.getWorkflowStatus().getCurrentStatus();
    }

    /**
     * @Author tangxinglin
     * @Description //从MongoDB文档的workflowStatus中提取关联的MySQL事件ID
     * @Date 2026/04/18 10:00
     * @Param [document MongoDB告警事件文档]
     * @return Long MySQL事件ID，文档为null时返回null
     */
    private Long workflowSqlEventId(AlarmEventDocument document) {
        if (document == null || document.getWorkflowStatus() == null) {
            return null;
        }
        return document.getWorkflowStatus().getSqlEventId();
    }

    /**
     * @Author tangxinglin
     * @Description //从MongoDB文档的location中提取地址字符串
     * @Date 2026/04/18 10:00
     * @Param [document MongoDB告警事件文档]
     * @return String 地址字符串，文档为null时返回null
     */
    private String address(AlarmEventDocument document) {
        if (document == null || document.getLocation() == null) {
            return null;
        }
        return document.getLocation().getAddress();
    }

    /**
     * @Author tangxinglin
     * @Description //从MongoDB文档的location中提取经度
     * @Date 2026/04/18 10:00
     * @Param [document MongoDB告警事件文档]
     * @return BigDecimal 经度值，文档为null时返回null
     */
    private java.math.BigDecimal longitude(AlarmEventDocument document) {
        if (document == null || document.getLocation() == null) {
            return null;
        }
        return document.getLocation().getLongitude();
    }

    /**
     * @Author tangxinglin
     * @Description //从MongoDB文档的location中提取纬度
     * @Date 2026/04/18 10:00
     * @Param [document MongoDB告警事件文档]
     * @return BigDecimal 纬度值，文档为null时返回null
     */
    private java.math.BigDecimal latitude(AlarmEventDocument document) {
        if (document == null || document.getLocation() == null) {
            return null;
        }
        return document.getLocation().getLatitude();
    }

    /**
     * @Author tangxinglin
     * @Description //从Map中安全读取指定key的字符串值
     * @Date 2026/04/18 10:00
     * @Param [values 键值Map, key 要读取的键]
     * @return String 字符串值，Map为null或key不存在则返回null
     */
    private String stringValue(Map<String, Object> values, String key) {
        if (values == null || !StringUtils.hasText(key)) {
            return null;
        }
        Object value = values.get(key);
        return value == null ? null : String.valueOf(value);
    }

    /**
     * @Author tangxinglin
     * @Description //返回参数列表中第一个非空白的字符串
     * @Date 2026/04/18 10:00
     * @Param [values 字符串参数列表]
     * @return String 第一个非空白字符串，全为空白则返回null
     */
    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return null;
    }

    /**
     * @Author tangxinglin
     * @Description //返回参数列表中第一个非null的值
     * @Date 2026/04/18 10:00
     * @Param [values 泛型参数列表]
     * @return T 第一个非null的值，全为null则返回null
     */
    private <T> T firstNonNull(T... values) {
        for (T value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    /**
     * @Author tangxinglin
     * @Description //根据MySQL事件ID生成兜底事件编码（格式：EVT-{id}）
     * @Date 2026/04/18 10:00
     * @Param [sqlEventId MySQL事件ID]
     * @return String 兜底事件编码，ID为null时返回null
     */
    private String generateFallbackEventCode(Long sqlEventId) {
        return sqlEventId == null ? null : "EVT-" + sqlEventId;
    }

    /**
     * @Author tangxinglin
     * @Description //使用雪花算法生成全局唯一的事件编码（格式：EVT-{snowflakeId}）
     * @Date 2026/04/18 10:00
     * @Param []
     * @return String 全局唯一的事件编码
     */
    private String generateEventCode() {
        return "EVT-" + IdWorker.getIdStr();
    }

    /**
     * @Author tangxinglin
     * @Description //从URL路径中提取文件名，取最后一个斜杠后的部分
     * @Date 2026/04/18 10:00
     * @Param [reference 文件URL或路径字符串]
     * @return String 文件名，URL为空时返回默认值 "evidence"
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

    @Override
    public boolean updateUrgencyLevel(Long eventId, String urgencyLevel) {
        String sql = "UPDATE biz_event SET urgency_level = ?, updated_at = NOW() WHERE id = ?";
        int rows = jdbcTemplate.update(sql, urgencyLevel, eventId);
        if (rows == 0) {
            throw new BusinessException("EVENT_NOT_FOUND", "事件不存在");
        }
        return true;
    }

    @Override
    public void autoEscalateUrgency() {
        // 获取所有活跃事件（未关闭且未归档的）；created_at 允许为空，避免单条脏数据 NPE 中断整轮扫描
        List<EventEntity> activeEvents = jdbcTemplate.query(
            "SELECT id, urgency_level, created_at FROM biz_event WHERE COALESCE(archived, 0) = 0 AND status NOT IN ('CLOSED', 'COMPLETED')",
            (rs, rowNum) -> {
                EventEntity e = new EventEntity();
                e.setId(rs.getLong("id"));
                e.setUrgencyLevel(rs.getString("urgency_level"));
                java.sql.Timestamp createdAt = rs.getTimestamp("created_at");
                e.setCreatedAt(createdAt == null ? null : createdAt.toLocalDateTime());
                return e;
            });

        LocalDateTime now = LocalDateTime.now();
        for (EventEntity event : activeEvents) {
            String currentLevel = event.getUrgencyLevel();
            if (currentLevel == null) currentLevel = "GREEN";

            LocalDateTime createdAt = event.getCreatedAt();
            if (createdAt == null) continue;

            long hours = java.time.Duration.between(createdAt, now).toHours();
            String newLevel = currentLevel;

            if (hours >= 48 && !"RED".equals(currentLevel)) {
                newLevel = "RED";
            } else if (hours >= 24 && "GREEN".equals(currentLevel)) {
                newLevel = "YELLOW";
            }

            if (!newLevel.equals(currentLevel)) {
                jdbcTemplate.update("UPDATE biz_event SET urgency_level = ?, updated_at = NOW() WHERE id = ?",
                    newLevel, event.getId());
                // 写入督办记录，便于追溯并向责任人预警
                jdbcTemplate.update(
                    "INSERT INTO biz_event_record (event_id, from_status, to_status, action_type, operator_name, remark, created_at, updated_at) VALUES (?, ?, ?, 'SUPERVISION_ESCALATE', '系统', ?, NOW(), NOW())",
                    event.getId(), currentLevel, newLevel,
                    "超期自动升级督办：" + currentLevel + "→" + newLevel + "（已超" + hours + "小时）");
            }
        }
    }

    @Override
    @Transactional
    public void archiveEvent(Long id) {
        EventEntity entity = eventMapper.selectDetailById(id);
        if (entity == null) {
            throw new BusinessException("EVENT_NOT_FOUND", "事件不存在");
        }
        String status = entity.getStatus();
        // 仅关闭或忽略状态的事件可归档
        if (!EventStatus.CLOSED.name().equals(status) && !EventStatus.IGNORED.name().equals(status)) {
            throw new BusinessException("EVENT_ARCHIVE_STATUS_INVALID", "仅关闭或忽略状态的事件可归档");
        }
        jdbcTemplate.update("UPDATE biz_event SET archived = 1, archived_at = NOW(), updated_at = NOW() WHERE id = ?", id);
        jdbcTemplate.update(
            "INSERT INTO biz_event_record (event_id, from_status, to_status, action_type, operator_name, remark, created_at) VALUES (?, ?, ?, 'ARCHIVE', '系统', '归档留存', NOW())",
            entity.getId(), status, status);
        // 同步 MongoDB 归档标记：默认列表/看板不再展示已归档事件
        alarmEventMongoService.setArchived(id, entity.getExternalEventId(), true);
    }

    @Override
    @Transactional
    public boolean auditEvent(Long id, boolean passed, String remark) {
        EventEntity entity = eventMapper.selectDetailById(id);
        if (entity == null) {
            throw new BusinessException("EVENT_NOT_FOUND", "事件不存在");
        }
        String fromStatus = entity.getStatus();
        // 仅待审核/审核中/已驳回的事件可执行审核（驳回后允许重新提交审核）
        if (!EventStatus.PENDING_AUDIT.name().equals(fromStatus)
                && !EventStatus.IN_AUDIT.name().equals(fromStatus)
                && !EventStatus.AUDIT_REJECTED.name().equals(fromStatus)) {
            throw new BusinessException("EVENT_AUDIT_STATUS_INVALID", "事件当前状态不可审核，仅待审核或已驳回的事件可审核");
        }
        String operatorName = AuthenticatedUserContextHolder.getOptional()
                .map(user -> user.userName()).orElse("系统");
        String opinion = remark != null && !remark.isBlank() ? remark : (passed ? "审核通过" : "审核驳回");
        if (passed) {
            // 通过：待审核/已驳回 → 已通过 → 待派单（进入闭环处置）
            int updated = jdbcTemplate.update(
                    "UPDATE biz_event SET status = ?, updated_at = NOW() WHERE id = ? AND status = ?",
                    EventStatus.AUDIT_APPROVED.name(), id, fromStatus);
            if (updated == 0) {
                throw new BusinessException("EVENT_AUDIT_STATUS_INVALID", "事件状态已变化，请刷新后重试");
            }
            jdbcTemplate.update(
                    "INSERT INTO biz_event_record (event_id, from_status, to_status, action_type, operator_name, remark, created_at) VALUES (?, ?, ?, 'AUDIT_PASS', ?, ?, NOW())",
                    id, fromStatus, EventStatus.AUDIT_APPROVED.name(), operatorName, opinion);

            // 地理路由：检查事件所属网格是否有组长
            EventEntity eventEntity = eventMapper.selectDetailById(id);
            Long gridId = eventEntity != null ? eventEntity.getGridId() : null;
            log.info("[GEO-ROUTE] 审核通过触发地理路由, eventId={}, gridId={}, operator={}", id, gridId, operatorName);
            boolean hasLeader = false;
            Integer leaderCount = 0;
            if (gridId != null) {
                leaderCount = jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM cmn_org_member WHERE grid_id = ? AND status = 'ACTIVE' "
                                + "AND (position LIKE '%组长%' OR position LIKE '%网格长%' OR member_type = 'LEADER')",
                        Integer.class, gridId);
                hasLeader = leaderCount != null && leaderCount > 0;
                log.info("[GEO-ROUTE] 网格组长查询完成, eventId={}, gridId={}, leaderCount={}, hasLeader={}", id, gridId, leaderCount, hasLeader);
            } else {
                log.info("[GEO-ROUTE] 事件未关联网格, eventId={}, gridId=null, 直接走待派单流程", id);
            }

            if (hasLeader) {
                log.info("[GEO-ROUTE] 路由决策: eventId={}, gridId={} → WAITING_LEADER_REVIEW (组长审核), leaderCount={}", id, gridId, leaderCount);
                // 有组长 → 进入组长审核队列
                jdbcTemplate.update(
                        "UPDATE biz_event SET status = ?, updated_at = NOW() WHERE id = ? AND status = ?",
                        EventStatus.WAITING_LEADER_REVIEW.name(), id, EventStatus.AUDIT_APPROVED.name());
                jdbcTemplate.update(
                        "INSERT INTO biz_event_record (event_id, from_status, to_status, action_type, operator_name, remark, created_at) VALUES (?, ?, ?, 'WAIT_LEADER_REVIEW', ?, '审核通过，进入组长审核', NOW())",
                        id, EventStatus.AUDIT_APPROVED.name(), EventStatus.WAITING_LEADER_REVIEW.name(), operatorName);
                alarmWorkflowStatusSyncService.syncWorkflowStatus(id, EventStatus.WAITING_LEADER_REVIEW.name());
                log.info("[GEO-ROUTE] 路由落地完成: eventId={} 已更新为 WAITING_LEADER_REVIEW", id);
            } else {
                log.info("[GEO-ROUTE] 路由决策: eventId={}, gridId={} → WAITING_DISPATCH (无组长，走普通派单)", id, gridId);
                // 无组长 → 走原待派单流程
                jdbcTemplate.update(
                        "UPDATE biz_event SET status = ?, updated_at = NOW() WHERE id = ? AND status = ?",
                        EventStatus.WAITING_DISPATCH.name(), id, EventStatus.AUDIT_APPROVED.name());
                jdbcTemplate.update(
                        "INSERT INTO biz_event_record (event_id, from_status, to_status, action_type, operator_name, remark, created_at) VALUES (?, ?, ?, 'WAIT_DISPATCH', ?, '审核通过，进入待派单', NOW())",
                        id, EventStatus.AUDIT_APPROVED.name(), EventStatus.WAITING_DISPATCH.name(), operatorName);
                alarmWorkflowStatusSyncService.syncWorkflowStatus(id, EventStatus.WAITING_DISPATCH.name());
                log.info("[GEO-ROUTE] 路由落地完成: eventId={} 已更新为 WAITING_DISPATCH", id);
            }
        } else {
            // 驳回：待审核/审核中 → 已驳回
            int updated = jdbcTemplate.update(
                    "UPDATE biz_event SET status = ?, updated_at = NOW() WHERE id = ? AND status = ?",
                    EventStatus.AUDIT_REJECTED.name(), id, fromStatus);
            if (updated == 0) {
                throw new BusinessException("EVENT_AUDIT_STATUS_INVALID", "事件状态已变化，请刷新后重试");
            }
            jdbcTemplate.update(
                    "INSERT INTO biz_event_record (event_id, from_status, to_status, action_type, operator_name, remark, created_at) VALUES (?, ?, ?, 'AUDIT_REJECT', ?, ?, NOW())",
                    id, fromStatus, EventStatus.AUDIT_REJECTED.name(), operatorName, opinion);
            alarmWorkflowStatusSyncService.syncWorkflowStatus(id, EventStatus.AUDIT_REJECTED.name());
        }
        return true;
    }

    @Override
    @Transactional
    public void closeEvent(Long eventId, String reason) {
        EventEntity entity = eventMapper.selectDetailById(eventId);
        if (entity == null) {
            throw new BusinessException("EVENT_NOT_FOUND", "事件不存在");
        }
        if (EventStatus.CLOSED.name().equals(entity.getStatus())) {
            throw new BusinessException("EVENT_ALREADY_CLOSED", "事件已关闭");
        }
        jdbcTemplate.update("UPDATE biz_event SET status = ?, archived = 1, archived_at = NOW(), updated_at = NOW() WHERE id = ?",
                EventStatus.CLOSED.name(), eventId);
        // 记录关闭操作到事件记录表
        jdbcTemplate.update(
                "INSERT INTO biz_event_record (event_id, from_status, to_status, action_type, operator_name, remark, created_at) VALUES (?, ?, ?, 'CLOSE', '系统', ?, NOW())",
                entity.getId(), entity.getStatus(), EventStatus.CLOSED.name(), reason != null ? reason : "手动关闭");
    }

    @Override
    @Transactional
    public void reopenEvent(Long eventId) {
        EventEntity entity = eventMapper.selectDetailById(eventId);
        if (entity == null) {
            throw new BusinessException("EVENT_NOT_FOUND", "事件不存在");
        }
        if (!EventStatus.CLOSED.name().equals(entity.getStatus())) {
            throw new BusinessException("EVENT_NOT_CLOSED", "事件未关闭，无法重新打开");
        }
        jdbcTemplate.update("UPDATE biz_event SET status = ?, archived = 0, archived_at = NULL, updated_at = NOW() WHERE id = ?",
                EventStatus.WAITING_DISPATCH.name(), eventId);
        jdbcTemplate.update(
                "INSERT INTO biz_event_record (event_id, from_status, to_status, action_type, operator_name, remark, created_at) VALUES (?, ?, ?, 'REOPEN', '系统', '重新打开事件', NOW())",
                entity.getId(), EventStatus.CLOSED.name(), EventStatus.WAITING_DISPATCH.name());
        // 同步 MongoDB：恢复为活跃事件
        alarmEventMongoService.setArchived(eventId, entity.getExternalEventId(), false);
    }

    @Override
    public List<EventDetailVo.LifecycleRecordVo> getTimeline(Long eventId) {
        List<EventDetailVo.LifecycleRecordVo> timeline = new ArrayList<>();
        // 从事件记录表获取操作历史
        List<Map<String, Object>> records = jdbcTemplate.queryForList(
                "SELECT action_type, from_status, to_status, operator_name, remark, created_at FROM biz_event_record WHERE event_id = ? ORDER BY created_at ASC, id ASC",
                eventId);
        for (Map<String, Object> record : records) {
            String action = mapActionName((String) record.get("action_type"));
            String status = mapStatusLabel((String) record.get("to_status"));
            String operator = (String) record.get("operator_name");
            String remark = (String) record.get("remark");
            java.sql.Timestamp ts = (java.sql.Timestamp) record.get("created_at");
            timeline.add(new EventDetailVo.LifecycleRecordVo(
                    action, status != null ? status : "",
                    (operator != null ? operator : "") + (remark != null ? " — " + remark : ""),
                    ts != null ? ts.toLocalDateTime() : LocalDateTime.now()));
        }
        return timeline;
    }

    @Override
    public EventStatistics getStatistics() {
        // 与事件列表页口径一致：仅计未归档的活跃事件，避免各页面同类指标数量不一致
        Long total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM biz_event WHERE COALESCE(archived, 0) = 0", Long.class);
        Long waiting = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM biz_event WHERE COALESCE(archived, 0) = 0 AND status = 'WAITING_DISPATCH'", Long.class);
        Long waitingLeader = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM biz_event WHERE COALESCE(archived, 0) = 0 AND status = 'WAITING_LEADER_REVIEW'", Long.class);
        Long dispatched = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM biz_event WHERE COALESCE(archived, 0) = 0 AND status = 'DISPATCHED_TO_WORK_ORDER'", Long.class);
        Long closed = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM biz_event WHERE COALESCE(archived, 0) = 0 AND status = 'CLOSED'", Long.class);
        Long ignored = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM biz_event WHERE COALESCE(archived, 0) = 0 AND status = 'IGNORED'", Long.class);
        Long green = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM biz_event WHERE COALESCE(archived, 0) = 0 AND urgency_level = 'GREEN' AND status NOT IN ('CLOSED','IGNORED')", Long.class);
        Long yellow = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM biz_event WHERE COALESCE(archived, 0) = 0 AND urgency_level = 'YELLOW' AND status NOT IN ('CLOSED','IGNORED')", Long.class);
        Long red = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM biz_event WHERE COALESCE(archived, 0) = 0 AND urgency_level = 'RED' AND status NOT IN ('CLOSED','IGNORED')", Long.class);
        return new EventStatistics(
                total != null ? total : 0,
                waiting != null ? waiting : 0,
                waitingLeader != null ? waitingLeader : 0,
                dispatched != null ? dispatched : 0,
                closed != null ? closed : 0,
                ignored != null ? ignored : 0,
                green != null ? green : 0,
                yellow != null ? yellow : 0,
                red != null ? red : 0);
    }

    @Override
    public boolean rateEvent(Long id, int rating, String comment) {
        // 更新事件的评价信息
        jdbcTemplate.update(
            "UPDATE biz_event SET rating = ?, rating_comment = ?, rated_at = NOW() WHERE id = ? AND status = 'CLOSED'",
            rating, comment, id);
        return true;
    }

    /**
     * @Description //设置事件展示隐藏：同步更新 MySQL 与 MongoDB，隐藏后大屏/GIS/热力图不再展示
     * @Param [eventId 事件主键ID, hidden 是否隐藏]
     * @return boolean 操作是否成功
     */
    @Override
    public boolean setEventHidden(Long eventId, boolean hidden) {
        EventEntity entity = eventMapper.selectDetailById(eventId);
        if (entity == null) {
            throw new BusinessException("EVENT_NOT_FOUND", "事件不存在");
        }
        eventMapper.updateHidden(eventId, hidden ? 1 : 0);
        // 列表与大屏数据源自 MongoDB，同步更新文档标记
        if (StringUtils.hasText(entity.getExternalEventId())) {
            alarmEventMongoService.setHidden(entity.getExternalEventId(), hidden);
        }
        return true;
    }

    public List<Map<String, Object>> getHeatmapData(String startDate, String endDate, String eventType) {
        StringBuilder sql = new StringBuilder(
            "SELECT id, title, event_type, urgency_level, status, " +
            "CAST(longitude AS DECIMAL(10,6)) as lng, CAST(latitude AS DECIMAL(10,6)) as lat, " +
            "created_at FROM biz_event " +
            "WHERE longitude IS NOT NULL AND latitude IS NOT NULL AND archived = 0 AND COALESCE(hidden, 0) = 0");
        List<Object> params = new ArrayList<>();
        if (startDate != null && !startDate.isEmpty()) { sql.append(" AND created_at >= ?"); params.add(startDate); }
        if (endDate != null && !endDate.isEmpty()) { sql.append(" AND created_at <= ?"); params.add(endDate); }
        if (eventType != null && !eventType.isEmpty()) { sql.append(" AND event_type = ?"); params.add(eventType); }
        sql.append(" ORDER BY created_at DESC LIMIT 1000");
        return jdbcTemplate.queryForList(sql.toString(), params.toArray());
    }

    private String mapActionName(String actionType) {
        if (actionType == null) return "操作";
        return switch (actionType) {
            case "CREATE" -> "创建事件";
            case "DISPATCH" -> "派发工单";
            case "CLOSE" -> "关闭事件";
            case "REOPEN" -> "重新打开";
            case "IGNORE" -> "忽略事件";
            case "URGENCY_UPDATE" -> "更新紧急程度";
            case "SUPERVISION_ESCALATE" -> "超期升级督办";
            case "ARCHIVE" -> "归档留存";
            case "12345_IMPORT" -> "12345 热线转办";
            case "PROPERTY_REPORT" -> "物业上报";
            case "RESIDENT_REPORT" -> "居民上报";
            case "AUDIT_PASS" -> "审核通过";
            case "AUDIT_REJECT" -> "审核驳回";
            case "WAIT_DISPATCH" -> "进入待派单";
            case "WAIT_LEADER_REVIEW" -> "进入组长审核";
            case "LEADER_DISPATCH" -> "组长派单";
            case "IN_AUDIT" -> "进入审核";
            case "EVENT_INTAKE" -> "事件登记";
            case "EVENT_IGNORE" -> "忽略事件";
            case "AUDIT_APPROVE" -> "审核通过";
            case "WORK_ORDER_DISPATCH" -> "工单派发";
            case "WORK_ORDER_NOT_TRUE" -> "工单不属实";
            case "WORK_ORDER_NEEDS_EVIDENCE" -> "工单需补证";
            case "WORK_ORDER_WAITING_CLOSE" -> "工单待关闭确认";
            case "CONFIRM_CLOSE" -> "确认关闭";
            case "REJECT_CLOSE" -> "驳回关闭";
            default -> actionType;
        };
    }

    private String mapStatusLabel(String status) {
        if (status == null) return "";
        return switch (status) {
            case "PENDING_AUDIT" -> "待审核";
            case "IN_AUDIT" -> "审核中";
            case "AUDIT_APPROVED" -> "已通过";
            case "AUDIT_REJECTED" -> "已驳回";
            case "WAITING_DISPATCH" -> "待派单";
            case "WAITING_LEADER_REVIEW" -> "组长审核";
            case "DISPATCHED_TO_WORK_ORDER" -> "已派单";
            case "CLOSED" -> "已关闭";
            case "IGNORED" -> "已忽略";
            default -> status;
        };
    }

    private record WorkflowSnapshot(
            Long processTemplateId,
            String processTemplateName,
            String currentNodeName,
            String currentNodeStatus,
            Boolean dispatchable) {
        private static WorkflowSnapshot empty() {
            return new WorkflowSnapshot(null, null, null, null, null);
        }
    }
}
