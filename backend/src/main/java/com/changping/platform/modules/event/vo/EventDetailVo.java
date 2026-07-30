package com.changping.platform.modules.event.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * @Author tangxinglin
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
        Boolean archived) {

    public record LifecycleRecordVo(
            String action,
            String status,
            String remark,
            LocalDateTime occurredAt) {
    }
}
