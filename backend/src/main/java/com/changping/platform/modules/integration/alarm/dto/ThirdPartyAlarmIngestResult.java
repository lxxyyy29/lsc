package com.changping.platform.modules.integration.alarm.dto;

/**
 * @Author tangxinglin
 * @Description //第三方告警摄入结果DTO，返回摄入操作的结果信息，
 * 包含关联的SQL事件ID、外部事件ID、去重键、是否重复及当前状态
 * @Date 2026/04/18 10:00
 */
public record ThirdPartyAlarmIngestResult(
        Long sqlEventId,
        String externalEventId,
        String dedupKey,
        boolean duplicate,
        String status) {
}
