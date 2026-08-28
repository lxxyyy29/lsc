package com.changping.platform.modules.integration.alarm.document;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * @Author lxy
 * @Description //MongoDB告警事件文档实体，存储第三方推送告警的完整数据，
 * 包含事件基本信息、位置、证据、摄入元数据、工作流状态和生命周期记录，
 * 通过dedupKey唯一索引防止重复摄入
 * @Date 2026/04/18 10:00
 */
@Document(collection = "alarm_events")
@CompoundIndexes({
        @CompoundIndex(name = "uk_alarm_ingest_dedup", def = "{'ingestion.dedupKey': 1}", unique = true),
        @CompoundIndex(name = "idx_alarm_workflow_status", def = "{'workflowStatus.currentStatus': 1, 'occurredAt': -1}")
})
public class AlarmEventDocument {

    @Id
    private String id;

    @Indexed(unique = true, sparse = true)
    private String externalEventId;

    private String sourceSystem;
    private String sourceType;
    private String eventType;
    private String title;
    private String description;
    private String status;
    @Indexed
    private LocalDateTime occurredAt;
    private Location location = new Location();
    private List<String> evidenceReferences = new ArrayList<>();
    private Map<String, Object> rawPayload = new LinkedHashMap<>();
    private Map<String, Object> normalizedPayload = new LinkedHashMap<>();
    private Ingestion ingestion = new Ingestion();
    private WorkflowStatus workflowStatus = new WorkflowStatus();
    private List<LifecycleRecord> lifecycle = new ArrayList<>();
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * 展示隐藏标记：true=隐藏（仅事件闭环/工单中心可见，大屏/GIS 面板不展示），null/false=正常显示
     */
    private Boolean hidden;

    /** 紧急程度：GREEN/YELLOW/RED，用于列表按紧急程度筛选 */
    private String urgencyLevel;

    /** 是否已归档：true=归档留存（默认列表不展示），null/false=活跃 */
    private Boolean archived;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getExternalEventId() { return externalEventId; }
    public void setExternalEventId(String externalEventId) { this.externalEventId = externalEventId; }
    public String getSourceSystem() { return sourceSystem; }
    public void setSourceSystem(String sourceSystem) { this.sourceSystem = sourceSystem; }
    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getUrgencyLevel() { return urgencyLevel; }
    public void setUrgencyLevel(String urgencyLevel) { this.urgencyLevel = urgencyLevel; }
    public Boolean getArchived() { return archived; }
    public void setArchived(Boolean archived) { this.archived = archived; }
    public LocalDateTime getOccurredAt() { return occurredAt; }
    public void setOccurredAt(LocalDateTime occurredAt) { this.occurredAt = occurredAt; }
    public Location getLocation() { return location; }
    public void setLocation(Location location) { this.location = location; }
    public List<String> getEvidenceReferences() { return evidenceReferences; }
    public void setEvidenceReferences(List<String> evidenceReferences) { this.evidenceReferences = evidenceReferences; }
    public Map<String, Object> getRawPayload() { return rawPayload; }
    public void setRawPayload(Map<String, Object> rawPayload) { this.rawPayload = rawPayload; }
    public Map<String, Object> getNormalizedPayload() { return normalizedPayload; }
    public void setNormalizedPayload(Map<String, Object> normalizedPayload) { this.normalizedPayload = normalizedPayload; }
    public Ingestion getIngestion() { return ingestion; }
    public void setIngestion(Ingestion ingestion) { this.ingestion = ingestion; }
    public WorkflowStatus getWorkflowStatus() { return workflowStatus; }
    public void setWorkflowStatus(WorkflowStatus workflowStatus) { this.workflowStatus = workflowStatus; }
    public List<LifecycleRecord> getLifecycle() { return lifecycle; }
    public void setLifecycle(List<LifecycleRecord> lifecycle) { this.lifecycle = lifecycle; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public Boolean getHidden() { return hidden; }
    public void setHidden(Boolean hidden) { this.hidden = hidden; }

    public static class Location {
        private String address;
        private BigDecimal longitude;
        private BigDecimal latitude;
        public String getAddress() { return address; }
        public void setAddress(String address) { this.address = address; }
        public BigDecimal getLongitude() { return longitude; }
        public void setLongitude(BigDecimal longitude) { this.longitude = longitude; }
        public BigDecimal getLatitude() { return latitude; }
        public void setLatitude(BigDecimal latitude) { this.latitude = latitude; }
    }

    public static class Ingestion {
        private LocalDateTime receivedAt;
        private String dedupKey;
        private boolean verified;
        private String result;
        public LocalDateTime getReceivedAt() { return receivedAt; }
        public void setReceivedAt(LocalDateTime receivedAt) { this.receivedAt = receivedAt; }
        public String getDedupKey() { return dedupKey; }
        public void setDedupKey(String dedupKey) { this.dedupKey = dedupKey; }
        public boolean isVerified() { return verified; }
        public void setVerified(boolean verified) { this.verified = verified; }
        public String getResult() { return result; }
        public void setResult(String result) { this.result = result; }
    }

    public static class WorkflowStatus {
        private Long sqlEventId;
        private String currentStatus;
        private LocalDateTime lastSyncedAt;
        public Long getSqlEventId() { return sqlEventId; }
        public void setSqlEventId(Long sqlEventId) { this.sqlEventId = sqlEventId; }
        public String getCurrentStatus() { return currentStatus; }
        public void setCurrentStatus(String currentStatus) { this.currentStatus = currentStatus; }
        public LocalDateTime getLastSyncedAt() { return lastSyncedAt; }
        public void setLastSyncedAt(LocalDateTime lastSyncedAt) { this.lastSyncedAt = lastSyncedAt; }
    }

    public static class LifecycleRecord {
        private String action;
        private String status;
        private String remark;
        private LocalDateTime occurredAt;
        public String getAction() { return action; }
        public void setAction(String action) { this.action = action; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getRemark() { return remark; }
        public void setRemark(String remark) { this.remark = remark; }
        public LocalDateTime getOccurredAt() { return occurredAt; }
        public void setOccurredAt(LocalDateTime occurredAt) { this.occurredAt = occurredAt; }
    }
}
