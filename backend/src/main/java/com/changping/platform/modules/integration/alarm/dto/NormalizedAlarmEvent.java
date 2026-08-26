package com.changping.platform.modules.integration.alarm.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * @Author lxy
 * @Description //归一化的告警事件数据传输对象，将第三方原始告警载荷标准化为平台统一字段结构，
 * 包含事件标识、来源信息、位置坐标、证据引用和原始载荷
 * @Date 2026/04/18 10:00
 */
public record NormalizedAlarmEvent(
        String externalEventId,
        String dedupKey,
        String sourceSystem,
        String sourceType,
        String eventType,
        String title,
        String description,
        String status,
        LocalDateTime occurredAt,
        String address,
        BigDecimal longitude,
        BigDecimal latitude,
        List<String> evidenceReferences,
        Map<String, Object> normalizedPayload,
        Map<String, Object> rawPayload) {
}
