package com.changping.platform.modules.integration.alarm.service;

import com.changping.platform.modules.event.dto.CreateEventRequest;
import com.changping.platform.modules.integration.alarm.config.AlarmIntegrationProperties;
import com.changping.platform.modules.integration.alarm.document.AlarmEventDocument;
import com.changping.platform.modules.integration.alarm.dto.NormalizedAlarmEvent;
import com.changping.platform.modules.integration.alarm.repository.AlarmEventRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * @Author tangxinglin
 * @Description //告警事件MongoDB服务，提供告警事件的保存、更新、查询及归一化解析功能，
 * 支持第三方推送告警和手动创建事件两种来源，通过dedupKey防重入
 * @Date 2026/04/18 10:00
 */
@Service
public class AlarmEventMongoService {

    private final AlarmEventRepository alarmEventRepository;
    private final AlarmIntegrationProperties properties;
    private final MongoTemplate mongoTemplate;

    /**
     * @Author tangxinglin
     * @Description //构造函数，注入告警事件MongoDB仓库、集成配置属性及MongoTemplate
     * @Date 2026/04/18 10:00
     * @Param [alarmEventRepository 告警事件MongoDB仓库, properties 告警集成配置属性, mongoTemplate MongoDB模板]
     * @return void
     */
    public AlarmEventMongoService(AlarmEventRepository alarmEventRepository, AlarmIntegrationProperties properties, MongoTemplate mongoTemplate) {
        this.alarmEventRepository = alarmEventRepository;
        this.properties = properties;
        this.mongoTemplate = mongoTemplate;
    }

    /**
     * @Author tangxinglin
     * @Description //将归一化的告警事件保存为新的MongoDB文档，重复摄入时返回已存在文档
     * @Date 2026/04/18 10:00
     * @Param [normalizedEvent 归一化的告警事件, verified 是否通过签名验证]
     * @return AlarmEventDocument 保存或已存在的告警事件文档
     */
    public AlarmEventDocument saveNewAlarm(NormalizedAlarmEvent normalizedEvent, boolean verified) {
        AlarmEventDocument document = new AlarmEventDocument();
        document.setExternalEventId(normalizedEvent.externalEventId());
        document.setSourceSystem(normalizedEvent.sourceSystem());
        document.setSourceType(normalizedEvent.sourceType());
        document.setEventType(normalizedEvent.eventType());
        document.setTitle(normalizedEvent.title());
        document.setDescription(normalizedEvent.description());
        document.setStatus(normalizedEvent.status());
        document.setOccurredAt(normalizedEvent.occurredAt());
        AlarmEventDocument.Location location = new AlarmEventDocument.Location();
        location.setAddress(normalizedEvent.address());
        location.setLongitude(normalizedEvent.longitude());
        location.setLatitude(normalizedEvent.latitude());
        document.setLocation(location);
        document.setEvidenceReferences(normalizedEvent.evidenceReferences() == null ? new ArrayList<>() : new ArrayList<>(normalizedEvent.evidenceReferences()));
        document.setRawPayload(copyMap(normalizedEvent.rawPayload()));
        document.setNormalizedPayload(copyMap(normalizedEvent.normalizedPayload()));
        AlarmEventDocument.Ingestion ingestion = new AlarmEventDocument.Ingestion();
        ingestion.setReceivedAt(LocalDateTime.now());
        ingestion.setDedupKey(normalizedEvent.dedupKey());
        ingestion.setVerified(verified);
        ingestion.setResult("INGESTED");
        document.setIngestion(ingestion);
        AlarmEventDocument.WorkflowStatus workflowStatus = new AlarmEventDocument.WorkflowStatus();
        workflowStatus.setCurrentStatus(normalizedEvent.status());
        workflowStatus.setLastSyncedAt(LocalDateTime.now());
        document.setWorkflowStatus(workflowStatus);
        document.setLifecycle(new ArrayList<>(List.of(lifecycleRecord("EVENT_INTAKE", normalizedEvent.status(), "callback received"))));
        document.setCreatedAt(LocalDateTime.now());
        document.setUpdatedAt(LocalDateTime.now());
        try {
            return alarmEventRepository.save(document);
        } catch (DuplicateKeyException exception) {
            return alarmEventRepository.findByIngestionDedupKey(normalizedEvent.dedupKey()).orElseThrow(() -> exception);
        }
    }

    /**
     * @Author tangxinglin
     * @Description //根据手动创建事件请求插入或更新MongoDB告警文档，绑定SQL事件ID和工作流状态
     * @Date 2026/04/18 10:00
     * @Param [request 创建事件请求, sqlEventId 关联的SQL事件ID, eventCode 事件编码, status 当前状态]
     * @return void
     */
    public void upsertManualEvent(CreateEventRequest request, Long sqlEventId, String eventCode, String status) {
        Optional<AlarmEventDocument> existing = alarmEventRepository.findByExternalEventId(request.externalEventId());
        AlarmEventDocument document = existing.orElseGet(AlarmEventDocument::new);
        document.setExternalEventId(request.externalEventId());
        document.setSourceSystem(request.sourceSystem());
        document.setSourceType(request.sourceType());
        document.setEventType(request.eventType());
        document.setTitle(request.title());
        document.setDescription(request.description());
        document.setStatus(status);
        document.setUrgencyLevel(request.urgencyLevel());
        document.setOccurredAt(request.occurredAt());
        AlarmEventDocument.Location location = document.getLocation() == null ? new AlarmEventDocument.Location() : document.getLocation();
        location.setAddress(request.location());
        location.setLongitude(request.longitude());
        location.setLatitude(request.latitude());
        document.setLocation(location);
        document.setEvidenceReferences(request.evidenceReferences() == null ? new ArrayList<>() : new ArrayList<>(request.evidenceReferences()));
        Map<String, Object> normalized = new LinkedHashMap<>();
        normalized.put("eventCode", eventCode);
        normalized.put("externalEventId", request.externalEventId());
        document.setNormalizedPayload(normalized);
        if (document.getRawPayload() == null) {
            document.setRawPayload(new LinkedHashMap<>());
        }
        AlarmEventDocument.Ingestion ingestion = document.getIngestion() == null ? new AlarmEventDocument.Ingestion() : document.getIngestion();
        ingestion.setReceivedAt(document.getCreatedAt() == null ? LocalDateTime.now() : document.getCreatedAt());
        ingestion.setDedupKey("manual:" + request.externalEventId());
        ingestion.setVerified(true);
        ingestion.setResult("MANUAL_CREATE");
        document.setIngestion(ingestion);
        AlarmEventDocument.WorkflowStatus workflowStatus = document.getWorkflowStatus() == null ? new AlarmEventDocument.WorkflowStatus() : document.getWorkflowStatus();
        workflowStatus.setSqlEventId(sqlEventId);
        workflowStatus.setCurrentStatus(status);
        workflowStatus.setLastSyncedAt(LocalDateTime.now());
        document.setWorkflowStatus(workflowStatus);
        List<AlarmEventDocument.LifecycleRecord> lifecycle = document.getLifecycle() == null ? new ArrayList<>() : document.getLifecycle();
        if (lifecycle.isEmpty()) {
            lifecycle.add(lifecycleRecord("EVENT_INTAKE", status, "manual create"));
        }
        document.setLifecycle(lifecycle);
        if (document.getCreatedAt() == null) {
            document.setCreatedAt(LocalDateTime.now());
        }
        document.setUpdatedAt(LocalDateTime.now());
        alarmEventRepository.save(document);
    }

    /**
     * @Author tangxinglin
     * @Description //根据外部事件ID查询告警事件文档
     * @Date 2026/04/18 10:00
     * @Param [externalEventId 外部事件ID]
     * @return Optional<AlarmEventDocument> 查询结果，不存在时为空
     */
    public Optional<AlarmEventDocument> findByExternalEventId(String externalEventId) {
        return alarmEventRepository.findByExternalEventId(externalEventId);
    }

    /**
     * @Author tangxinglin
     * @Description //根据外部事件ID删除对应的MongoDB告警文档，ID为空时直接返回
     * @Date 2026/04/18 10:00
     * @Param [externalEventId 外部事件ID]
     * @return void
     */
    public void deleteByExternalEventId(String externalEventId) {
        if (!StringUtils.hasText(externalEventId)) {
            return;
        }
        alarmEventRepository.deleteAllByExternalEventId(externalEventId);
    }

    /**
     * @Author tangxinglin
     * @Description //根据SQL事件ID查询关联的MongoDB告警文档
     * @Date 2026/04/18 10:00
     * @Param [sqlEventId SQL事件ID]
     * @return Optional<AlarmEventDocument> 查询结果，不存在时为空
     */
    public Optional<AlarmEventDocument> findBySqlEventId(Long sqlEventId) {
        if (sqlEventId == null) {
            return Optional.empty();
        }
        List<AlarmEventDocument> documents = alarmEventRepository.findByWorkflowStatusSqlEventIdIn(List.of(sqlEventId));
        return documents.stream().findFirst();
    }

    /**
     * @Author tangxinglin
     * @Description //分页查询告警事件列表，支持按外部事件ID模糊过滤，按发生时间倒序排列
     * @Date 2026/04/18 10:00
     * @Param [externalEventId 外部事件ID模糊关键字（可选）, page 页码（从1开始）, size 每页条数]
     * @return List<AlarmEventDocument> 告警事件文档列表
     */
    public List<AlarmEventDocument> queryEvents(String externalEventId, int page, int size) {
        return queryEvents(externalEventId, page, size, null);
    }

    /**
     * @Author tangxinglin
     * @Description //分页查询告警事件列表,支持按外部事件ID模糊过滤,并可排除指定工作流状态
     * @Date 2026/04/22 00:00
     * @Param [externalEventId 外部事件ID模糊关键字(可选), page 页码(从1开始), size 每页条数, excludeStatuses 需要排除的工作流状态(可为空)]
     * @return List<AlarmEventDocument> 告警事件文档列表
     */
    public List<AlarmEventDocument> queryEvents(String externalEventId, int page, int size, java.util.Collection<String> excludeStatuses) {
        int safePage = Math.max(page - 1, 0);
        int safeSize = Math.max(1, Math.min(size, 500));
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(
                safePage, safeSize, Sort.by(Sort.Order.desc("occurredAt"), Sort.Order.desc("createdAt")));
        boolean hasKeyword = StringUtils.hasText(externalEventId);
        boolean hasExclude = excludeStatuses != null && !excludeStatuses.isEmpty();
        if (hasKeyword && hasExclude) {
            return alarmEventRepository.findByExternalEventIdContainingIgnoreCaseAndWorkflowStatusCurrentStatusNotIn(
                    externalEventId.trim(), excludeStatuses, pageable);
        }
        if (hasKeyword) {
            return alarmEventRepository.findByExternalEventIdContainingIgnoreCaseOrderByOccurredAtDesc(externalEventId.trim(), pageable);
        }
        if (hasExclude) {
            return alarmEventRepository.findByWorkflowStatusCurrentStatusNotIn(excludeStatuses, pageable);
        }
        return alarmEventRepository.findAll(pageable).getContent();
    }

    /**
     * @Author tangxinglin
     * @Description //统计告警事件总数,支持按外部事件ID模糊过滤
     * @Date 2026/04/18 10:00
     * @Param [externalEventId 外部事件ID模糊关键字(可选)]
     * @return long 事件总数
     */
    public long countEvents(String externalEventId) {
        return countEvents(externalEventId, null);
    }

    /**
     * @Author tangxinglin
     * @Description //统计告警事件总数,支持按外部事件ID模糊过滤,并可排除指定工作流状态
     * @Date 2026/04/22 00:00
     * @Param [externalEventId 外部事件ID模糊关键字(可选), excludeStatuses 需要排除的工作流状态(可为空)]
     * @return long 事件总数
     */
    public long countEvents(String externalEventId, java.util.Collection<String> excludeStatuses) {
        boolean hasKeyword = StringUtils.hasText(externalEventId);
        boolean hasExclude = excludeStatuses != null && !excludeStatuses.isEmpty();
        if (hasKeyword && hasExclude) {
            return alarmEventRepository.countByExternalEventIdContainingIgnoreCaseAndWorkflowStatusCurrentStatusNotIn(
                    externalEventId.trim(), excludeStatuses);
        }
        if (hasKeyword) {
            return alarmEventRepository.countByExternalEventIdContainingIgnoreCase(externalEventId.trim());
        }
        if (hasExclude) {
            return alarmEventRepository.countByWorkflowStatusCurrentStatusNotIn(excludeStatuses);
        }
        return alarmEventRepository.count();
    }

    /**
     * @Author tangxinglin
     * @Description //分页查询告警事件列表，支持按外部事件ID模糊过滤、排除指定工作流状态、按状态与发生时间范围过滤
     * @Date 2026/08/12 10:00
     * @Param [externalEventId 外部事件ID模糊关键字(可选), page 页码(从1开始), size 每页条数, excludeStatuses 需要排除的工作流状态(可为空), status 工作流状态精确过滤(可为空), startDate 开始时间(可为空), endDate 结束时间(可为空)]
     * @return List<AlarmEventDocument> 告警事件文档列表
     */
    public List<AlarmEventDocument> queryEvents(String externalEventId, int page, int size,
                                                Collection<String> excludeStatuses, String status,
                                                LocalDateTime startDate, LocalDateTime endDate) {
        return queryEvents(externalEventId, page, size, excludeStatuses, status, startDate, endDate, false);
    }

    /**
     * @Description //分页查询告警事件列表，支持排除已隐藏事件（大屏/GIS 等对外面板使用）
     * @Param [excludeHidden 为 true 时过滤 hidden=true 的事件]
     */
    public List<AlarmEventDocument> queryEvents(String externalEventId, int page, int size,
                                                Collection<String> excludeStatuses, String status,
                                                LocalDateTime startDate, LocalDateTime endDate,
                                                boolean excludeHidden) {
        return queryEvents(externalEventId, page, size, excludeStatuses, status, startDate, endDate, excludeHidden, null, false);
    }

    /**
     * @Description //分页查询告警事件列表（带紧急程度筛选），紧急程度 null 时不过滤；
     *              includeArchived 默认 false 时排除已归档事件，避免前端过滤后出现空白页
     */
    public List<AlarmEventDocument> queryEvents(String externalEventId, int page, int size,
                                                Collection<String> excludeStatuses, String status,
                                                LocalDateTime startDate, LocalDateTime endDate,
                                                boolean excludeHidden, String urgencyLevel, boolean includeArchived) {
        return queryEvents(externalEventId, page, size, excludeStatuses, status, startDate, endDate, excludeHidden, urgencyLevel, includeArchived, null);
    }

    /**
     * @Description //分页查询告警事件列表（带来源系统筛选），sourceSystem null 时不过滤
     */
    public List<AlarmEventDocument> queryEvents(String externalEventId, int page, int size,
                                                Collection<String> excludeStatuses, String status,
                                                LocalDateTime startDate, LocalDateTime endDate,
                                                boolean excludeHidden, String urgencyLevel, boolean includeArchived, String sourceSystem) {
        int safePage = Math.max(page - 1, 0);
        int safeSize = Math.max(1, Math.min(size, 500));
        Query query = new Query(buildCriteria(externalEventId, excludeStatuses, status, startDate, endDate, excludeHidden, urgencyLevel, includeArchived, sourceSystem))
                .with(PageRequest.of(safePage, safeSize,
                        Sort.by(Sort.Order.desc("occurredAt"), Sort.Order.desc("createdAt"))));
        return mongoTemplate.find(query, AlarmEventDocument.class);
    }

    /**
     * @Author tangxinglin
     * @Description //统计告警事件总数，支持按外部事件ID模糊过滤、排除指定工作流状态、按状态与发生时间范围过滤
     * @Date 2026/08/12 10:00
     * @Param [externalEventId 外部事件ID模糊关键字(可选), excludeStatuses 需要排除的工作流状态(可为空), status 工作流状态精确过滤(可为空), startDate 开始时间(可为空), endDate 结束时间(可为空)]
     * @return long 事件总数
     */
    public long countEvents(String externalEventId, Collection<String> excludeStatuses, String status,
                            LocalDateTime startDate, LocalDateTime endDate) {
        return countEvents(externalEventId, excludeStatuses, status, startDate, endDate, false);
    }

    /**
     * @Description //统计告警事件总数，支持排除已隐藏事件
     */
    public long countEvents(String externalEventId, Collection<String> excludeStatuses, String status,
                            LocalDateTime startDate, LocalDateTime endDate, boolean excludeHidden) {
        return countEvents(externalEventId, excludeStatuses, status, startDate, endDate, excludeHidden, null, false);
    }

    /**
     * @Description //统计告警事件总数（带紧急程度筛选），紧急程度 null 时不过滤；includeArchived 默认 false 排除已归档
     */
    public long countEvents(String externalEventId, Collection<String> excludeStatuses, String status,
                            LocalDateTime startDate, LocalDateTime endDate, boolean excludeHidden, String urgencyLevel, boolean includeArchived) {
        return countEvents(externalEventId, excludeStatuses, status, startDate, endDate, excludeHidden, urgencyLevel, includeArchived, null);
    }

    /**
     * @Description //统计告警事件总数（带来源系统筛选），sourceSystem null 时不过滤
     */
    public long countEvents(String externalEventId, Collection<String> excludeStatuses, String status,
                            LocalDateTime startDate, LocalDateTime endDate, boolean excludeHidden, String urgencyLevel, boolean includeArchived, String sourceSystem) {
        return mongoTemplate.count(new Query(buildCriteria(externalEventId, excludeStatuses, status, startDate, endDate, excludeHidden, urgencyLevel, includeArchived, sourceSystem)),
                AlarmEventDocument.class);
    }

    /**
     * @Description //设置事件归档标记：归档后默认列表/看板不再展示（保持与 MySQL archived 一致）
     * @Param [sqlEventId 事件主键ID, externalEventId 外部事件ID, archived 是否归档]
     * @return boolean 是否命中并更新了文档
     */
    public boolean setArchived(Long sqlEventId, String externalEventId, boolean archived) {
        Query query;
        if (StringUtils.hasText(externalEventId)) {
            query = new Query(Criteria.where("externalEventId").is(externalEventId.trim()));
        } else if (sqlEventId != null) {
            query = new Query(Criteria.where("normalizedPayload.eventCode").is(sqlEventId.toString()));
        } else {
            return false;
        }
        Update update = new Update().set("archived", archived).set("updatedAt", LocalDateTime.now());
        return mongoTemplate.updateMulti(query, update, AlarmEventDocument.class).getModifiedCount() > 0;
    }

    /**
     * @Description //设置事件展示隐藏标记：隐藏后仅事件闭环/工单中心可见，大屏/GIS 面板不再展示
     * @Param [externalEventId 外部事件ID, hidden 是否隐藏]
     * @return boolean 是否命中并更新了文档
     */
    public boolean setHidden(String externalEventId, boolean hidden) {
        if (!StringUtils.hasText(externalEventId)) {
            return false;
        }
        Update update = new Update().set("hidden", hidden).set("updatedAt", LocalDateTime.now());
        return mongoTemplate.updateFirst(
                new Query(Criteria.where("externalEventId").is(externalEventId.trim())),
                update,
                AlarmEventDocument.class).getModifiedCount() > 0;
    }

    /**
     * @Author tangxinglin
     * @Description //构建告警事件查询条件：外部事件ID模糊、排除状态、状态精确匹配、发生时间范围
     * @Date 2026/08/12 10:00
     * @Param [externalEventId 外部事件ID模糊关键字(可选), excludeStatuses 需要排除的工作流状态(可为空), status 工作流状态精确过滤(可为空), startDate 开始时间(可为空), endDate 结束时间(可为空)]
     * @return Criteria MongoDB查询条件
     */
    private Criteria buildCriteria(String externalEventId, Collection<String> excludeStatuses, String status,
                                   LocalDateTime startDate, LocalDateTime endDate) {
        return buildCriteria(externalEventId, excludeStatuses, status, startDate, endDate, false);
    }

    private Criteria buildCriteria(String externalEventId, Collection<String> excludeStatuses, String status,
                                   LocalDateTime startDate, LocalDateTime endDate, boolean excludeHidden) {
        return buildCriteria(externalEventId, excludeStatuses, status, startDate, endDate, excludeHidden, null, false);
    }

    private Criteria buildCriteria(String externalEventId, Collection<String> excludeStatuses, String status,
                                   LocalDateTime startDate, LocalDateTime endDate, boolean excludeHidden, String urgencyLevel) {
        return buildCriteria(externalEventId, excludeStatuses, status, startDate, endDate, excludeHidden, urgencyLevel, false);
    }

    private Criteria buildCriteria(String externalEventId, Collection<String> excludeStatuses, String status,
                                   LocalDateTime startDate, LocalDateTime endDate, boolean excludeHidden, String urgencyLevel, boolean includeArchived) {
        return buildCriteria(externalEventId, excludeStatuses, status, startDate, endDate, excludeHidden, urgencyLevel, includeArchived, null);
    }

    private Criteria buildCriteria(String externalEventId, Collection<String> excludeStatuses, String status,
                                   LocalDateTime startDate, LocalDateTime endDate, boolean excludeHidden, String urgencyLevel, boolean includeArchived, String sourceSystem) {
        List<Criteria> ands = new ArrayList<>();
        if (StringUtils.hasText(externalEventId)) {
            ands.add(Criteria.where("externalEventId").regex(Pattern.quote(externalEventId.trim()), "i"));
        }
        if (excludeStatuses != null && !excludeStatuses.isEmpty()) {
            ands.add(Criteria.where("workflowStatus.currentStatus").nin(excludeStatuses));
        }
        if (StringUtils.hasText(status)) {
            ands.add(Criteria.where("workflowStatus.currentStatus").is(status.trim()));
        }
        if (StringUtils.hasText(urgencyLevel)) {
            ands.add(Criteria.where("urgencyLevel").is(urgencyLevel.trim()));
        }
        if (StringUtils.hasText(sourceSystem)) {
            ands.add(Criteria.where("sourceSystem").is(sourceSystem.trim()));
        }
        if (!includeArchived) {
            // 默认排除已归档：归档事件仅勾选"显示已归档"时展示，避免列表 total 虚高出现空白页
            ands.add(new Criteria().orOperator(
                    Criteria.where("archived").exists(false),
                    Criteria.where("archived").is(false)));
        }
        if (startDate != null) {
            ands.add(Criteria.where("occurredAt").gte(startDate));
        }
        if (endDate != null) {
            ands.add(Criteria.where("occurredAt").lte(endDate));
        }
        if (excludeHidden) {
            // hidden 为 null/false 均视为显示，仅排除显式标记为 true 的事件
            ands.add(new Criteria().orOperator(
                    Criteria.where("hidden").exists(false),
                    Criteria.where("hidden").is(false)));
        }
        if (ands.isEmpty()) {
            return new Criteria();
        }
        return new Criteria().andOperator(ands.toArray(new Criteria[0]));
    }

    /**
     * @Author tangxinglin
     * @Description //更新指定外部事件ID对应的MongoDB文档的工作流状态，并追加生命周期记录
     * @Date 2026/04/18 10:00
     * @Param [externalEventId 外部事件ID, sqlEventId 关联的SQL事件ID, currentStatus 当前工作流状态, syncedAt 同步时间]
     * @return void
     */
    public void updateWorkflowStatus(String externalEventId, Long sqlEventId, String currentStatus, LocalDateTime syncedAt) {
        if (!StringUtils.hasText(externalEventId)) {
            return;
        }
        alarmEventRepository.findByExternalEventId(externalEventId).ifPresent(document -> {
            AlarmEventDocument.WorkflowStatus workflowStatus = document.getWorkflowStatus() == null ? new AlarmEventDocument.WorkflowStatus() : document.getWorkflowStatus();
            workflowStatus.setSqlEventId(sqlEventId);
            workflowStatus.setCurrentStatus(currentStatus);
            workflowStatus.setLastSyncedAt(syncedAt);
            document.setWorkflowStatus(workflowStatus);
            document.setStatus(currentStatus);
            List<AlarmEventDocument.LifecycleRecord> lifecycle = document.getLifecycle() == null ? new ArrayList<>() : document.getLifecycle();
            lifecycle.add(lifecycleRecord("WORKFLOW_SYNC", currentStatus, "sql workflow synced"));
            document.setLifecycle(lifecycle);
            document.setUpdatedAt(LocalDateTime.now());
            alarmEventRepository.save(document);
        });
    }

    /**
     * @Author tangxinglin
     * @Description //将第三方原始告警载荷归一化为平台统一的NormalizedAlarmEvent，
     * 兼容多种字段名称和时间格式，自动合成缺失的externalEventId和dedupKey
     * @Date 2026/04/18 10:00
     * @Param [rawPayload 第三方推送的原始载荷Map]
     * @return NormalizedAlarmEvent 归一化后的告警事件对象
     */
    public NormalizedAlarmEvent normalize(Map<String, Object> rawPayload) {
        String taskNo = stringValue(rawPayload.get("taskNo"));
        String alarmId = firstNonBlank(stringValue(rawPayload.get("alarmId")), stringValue(rawPayload.get("id")));

        // When alarmId is missing (e.g. upstream AlarmData push), synthesize from taskNo + modelNo + alarmTime
        if (!StringUtils.hasText(alarmId) && StringUtils.hasText(taskNo)) {
            String modelNo = stringValue(rawPayload.get("modelNo"));
            String alarmTime = stringValue(rawPayload.get("alarmTime"));
            alarmId = firstNonBlank(
                    joinWithDash(modelNo, alarmTime),
                    String.valueOf(System.currentTimeMillis()));
        }

        String externalEventId = firstNonBlank(stringValue(rawPayload.get("externalEventId")), joinWithDash(taskNo, alarmId));
        String dedupKey = firstNonBlank(joinWithColon(taskNo, alarmId), externalEventId);

        // Title: try title → alarmName → name (upstream sends "modelName+告警" as name) → modelName → fallback
        String title = firstNonBlank(
                stringValue(rawPayload.get("title")),
                stringValue(rawPayload.get("alarmName")),
                stringValue(rawPayload.get("name")),
                stringValue(rawPayload.get("modelName")),
                "无人机告警");
        String description = firstNonBlank(
                stringValue(rawPayload.get("description")),
                stringValue(rawPayload.get("content")),
                title);
        String eventType = firstNonBlank(stringValue(rawPayload.get("eventType")), properties.getDefaultEventType());
        String address = firstNonBlank(stringValue(rawPayload.get("address")), nestedString(rawPayload, "location", "address"));
        LocalDateTime occurredAt = parseDateTime(rawPayload.get("occurredAt"));
        if (occurredAt == null) {
            occurredAt = parseDateTime(rawPayload.get("alarmTime"));
        }
        if (occurredAt == null) {
            // Try alarmTime as epoch millis (upstream may send Long timestamp)
            occurredAt = parseEpochMillis(rawPayload.get("alarmTime"));
        }
        if (occurredAt == null) {
            occurredAt = LocalDateTime.now();
        }

        // Evidence: try evidenceReferences → alarmImage (upstream sends single image URL)
        List<String> evidenceReferences = extractEvidenceReferences(rawPayload.get("evidenceReferences"));
        if (evidenceReferences.isEmpty()) {
            String alarmImage = stringValue(rawPayload.get("alarmImage"));
            if (StringUtils.hasText(alarmImage)) {
                evidenceReferences = List.of(alarmImage);
            }
        }

        Map<String, Object> normalizedPayload = new LinkedHashMap<>();
        normalizedPayload.put("taskNo", taskNo);
        normalizedPayload.put("alarmId", alarmId);
        normalizedPayload.put("externalEventId", externalEventId);
        return new NormalizedAlarmEvent(
                externalEventId,
                dedupKey,
                properties.getSourceSystem(),
                properties.getSourceType(),
                eventType,
                title,
                description,
                "WAITING_DISPATCH",
                occurredAt,
                address,
                parseDecimal(firstNonNull(rawPayload.get("longitude"), nestedValue(rawPayload, "location", "longitude"))),
                parseDecimal(firstNonNull(rawPayload.get("latitude"), nestedValue(rawPayload, "location", "latitude"))),
                evidenceReferences,
                normalizedPayload,
                copyMap(rawPayload));
    }

    /**
     * @Author tangxinglin
     * @Description //创建一条生命周期记录
     * @Date 2026/04/18 10:00
     * @Param [action 操作名称, status 状态, remark 备注]
     * @return AlarmEventDocument.LifecycleRecord 生命周期记录对象
     */
    private AlarmEventDocument.LifecycleRecord lifecycleRecord(String action, String status, String remark) {
        AlarmEventDocument.LifecycleRecord record = new AlarmEventDocument.LifecycleRecord();
        record.setAction(action);
        record.setStatus(status);
        record.setRemark(remark);
        record.setOccurredAt(LocalDateTime.now());
        return record;
    }

    /**
     * @Author tangxinglin
     * @Description //深拷贝Map，source为null时返回空LinkedHashMap
     * @Date 2026/04/18 10:00
     * @Param [source 源Map]
     * @return Map<String, Object> 拷贝后的新Map
     */
    private Map<String, Object> copyMap(Map<String, Object> source) {
        return source == null ? new LinkedHashMap<>() : new LinkedHashMap<>(source);
    }

    /**
     * @Author tangxinglin
     * @Description //从原始值中提取证据引用列表，兼容List和单个字符串两种格式
     * @Date 2026/04/18 10:00
     * @Param [value 原始证据值]
     * @return List<String> 证据引用URL列表
     */
    private List<String> extractEvidenceReferences(Object value) {
        if (value instanceof List<?> list) {
            List<String> references = new ArrayList<>();
            for (Object item : list) {
                String reference = stringValue(item);
                if (StringUtils.hasText(reference)) {
                    references.add(reference);
                }
            }
            return references;
        }
        String reference = stringValue(value);
        return StringUtils.hasText(reference) ? List.of(reference) : List.of();
    }

    /**
     * @Author tangxinglin
     * @Description //从载荷中按父子键路径取嵌套值
     * @Date 2026/04/18 10:00
     * @Param [payload 源载荷Map, parent 父键名, child 子键名]
     * @return Object 嵌套值，不存在时返回null
     */
    private Object nestedValue(Map<String, Object> payload, String parent, String child) {
        if (!(payload.get(parent) instanceof Map<?, ?> nested)) {
            return null;
        }
        return nested.get(child);
    }

    /**
     * @Author tangxinglin
     * @Description //从载荷中按父子键路径取嵌套字符串值
     * @Date 2026/04/18 10:00
     * @Param [payload 源载荷Map, parent 父键名, child 子键名]
     * @return String 嵌套字符串值或null
     */
    private String nestedString(Map<String, Object> payload, String parent, String child) {
        return stringValue(nestedValue(payload, parent, child));
    }

    /**
     * @Author tangxinglin
     * @Description //返回两个值中第一个非null值
     * @Date 2026/04/18 10:00
     * @Param [first 第一个值, second 第二个值]
     * @return Object 第一个非null值
     */
    private Object firstNonNull(Object first, Object second) {
        return first != null ? first : second;
    }

    /**
     * @Author tangxinglin
     * @Description //用短横线拼接两个非空字符串，任一为空则返回另一个
     * @Date 2026/04/18 10:00
     * @Param [left 左值, right 右值]
     * @return String 拼接结果
     */
    private String joinWithDash(String left, String right) {
        if (StringUtils.hasText(left) && StringUtils.hasText(right)) {
            return left + "-" + right;
        }
        return firstNonBlank(left, right, "");
    }

    /**
     * @Author tangxinglin
     * @Description //用冒号拼接两个非空字符串，任一为空则返回另一个
     * @Date 2026/04/18 10:00
     * @Param [left 左值, right 右值]
     * @return String 拼接结果
     */
    private String joinWithColon(String left, String right) {
        if (StringUtils.hasText(left) && StringUtils.hasText(right)) {
            return left + ":" + right;
        }
        return firstNonBlank(left, right, "");
    }

    /**
     * @Author tangxinglin
     * @Description //从可变参数中返回第一个非空非空白字符串
     * @Date 2026/04/18 10:00
     * @Param [values 候选字符串列表]
     * @return String 第一个非空字符串，全为空则返回null
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
     * @Description //将任意值安全转换为字符串，null值返回null
     * @Date 2026/04/18 10:00
     * @Param [value 原始值]
     * @return String 字符串值或null
     */
    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    /**
     * @Author tangxinglin
     * @Description //将任意值解析为BigDecimal，null或格式非法时返回null
     * @Date 2026/04/18 10:00
     * @Param [value 原始值]
     * @return java.math.BigDecimal 解析结果或null
     */
    private java.math.BigDecimal parseDecimal(Object value) {
        if (value == null || !StringUtils.hasText(String.valueOf(value))) {
            return null;
        }
        try {
            return new java.math.BigDecimal(String.valueOf(value));
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    /**
     * @Author tangxinglin
     * @Description //将任意值解析为LocalDateTime，支持ISO8601、带偏移量及自定义格式，解析失败返回null
     * @Date 2026/04/18 10:00
     * @Param [value 原始值]
     * @return LocalDateTime 解析结果或null
     */
    private LocalDateTime parseDateTime(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        if (!StringUtils.hasText(text)) {
            return null;
        }
        // ISO 8601: "2025-03-15T14:30:00"
        try {
            return LocalDateTime.parse(text);
        } catch (Exception ignored) {
        }
        // ISO with offset: "2025-03-15T14:30:00+08:00"
        try {
            return java.time.OffsetDateTime.parse(text).toLocalDateTime();
        } catch (Exception ignored) {
        }
        // Upstream format: "2025-03-15 14:30:00"
        try {
            return LocalDateTime.parse(text, java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        } catch (Exception ignored) {
        }
        return null;
    }

    /**
     * @Author tangxinglin
     * @Description //将毫秒时间戳解析为LocalDateTime，解析失败返回null
     * @Date 2026/04/18 10:00
     * @Param [value 毫秒时间戳（数字或字符串）]
     * @return LocalDateTime 解析结果或null
     */
    private LocalDateTime parseEpochMillis(Object value) {
        if (value == null) {
            return null;
        }
        try {
            long millis;
            if (value instanceof Number number) {
                millis = number.longValue();
            } else {
                millis = Long.parseLong(String.valueOf(value).trim());
            }
            if (millis > 0) {
                return java.time.Instant.ofEpochMilli(millis)
                        .atZone(java.time.ZoneId.systemDefault())
                        .toLocalDateTime();
            }
        } catch (Exception ignored) {
        }
        return null;
    }
}
