package com.changping.platform.modules.event.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * @Author lxy
 * @Description //事件详情视图对象，聚合MySQL事件数据与MongoDB告警文档数据，用于前端展示
 * @Date 2026/04/18 10:00
 */
public record EventDetailVo(
        Long id,
        String eventCode,
        String externalEventId,
        String sourceType,
        String sourceSystem,
        String eventType,
        String title,
        String description,
        String status,
        String currentStatus,
        LocalDateTime occurredAt,
        String location,
        String area,
        BigDecimal longitude,
        BigDecimal latitude,
        List<String> evidenceReferences,
        List<LifecycleRecordVo> lifecycleRecords,
        Long processTemplateId,
        String processTemplateName,
        String currentNodeName,
        String currentNodeStatus,
        Boolean dispatchable,
        Long areaId,
        String areaName,
        Long gridId,
        String gridName,
        String urgencyLevel,
        String reportSource,
        Boolean archived,
        Boolean hidden,
        Boolean deleted,
        String deletedReason) {

    @JsonProperty
    public String statusLabel() {
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

    @JsonProperty
    public String urgencyLabel() {
        if (urgencyLevel == null) return "";
        return switch (urgencyLevel) {
            case "GREEN" -> "一般";
            case "YELLOW" -> "重点";
            case "RED" -> "紧急";
            default -> urgencyLevel;
        };
    }

    public record LifecycleRecordVo(
            String action,
            String status,
            String remark,
            LocalDateTime occurredAt) {
    }
}
