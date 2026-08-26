package com.changping.platform.modules.event.vo;

import java.time.LocalDateTime;

/**
 * @Author lxy
 * @Description //事件误报记录视图对象，用于前端展示误报（已忽略）事件的操作记录详情
 * @Date 2026/04/18 10:00
 */
public record EventIgnoreRecordVo(
        Long id,
        Long eventId,
        String eventCode,
        String eventTitle,
        String eventType,
        Long operatorId,
        String ignoredBy,
        String reason,
        LocalDateTime ignoredAt) {
}
