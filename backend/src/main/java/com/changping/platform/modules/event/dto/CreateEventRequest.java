package com.changping.platform.modules.event.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * @Author lxy
 * @Description //创建事件请求 DTO，包含外部事件ID、来源信息、事件类型、发生位置和证据引用等必要字段
 * @Date 2026/04/18 10:25
 */
public record CreateEventRequest(
        @NotBlank(message = "外部事件 ID 不能为空")
        String externalEventId,
        @NotBlank(message = "来源类型不能为空")
        String sourceType,
        @NotBlank(message = "来源系统不能为空")
        String sourceSystem,
        @NotBlank(message = "事件类型不能为空")
        String eventType,
        @NotBlank(message = "标题不能为空")
        String title,
        String description,
        @NotNull(message = "发生时间不能为空")
        LocalDateTime occurredAt,
        @NotBlank(message = "地点不能为空")
        String location,
        BigDecimal longitude,
        BigDecimal latitude,
        List<String> evidenceReferences,
        /** 紧急程度：GREEN/YELLOW/RED，不传默认 GREEN（Web 创建表单必选） */
        String urgencyLevel,
        /** 所属网格ID（可选）：未传时若有坐标则按坐标自动匹配最小网格 */
        Long gridId) {
}
