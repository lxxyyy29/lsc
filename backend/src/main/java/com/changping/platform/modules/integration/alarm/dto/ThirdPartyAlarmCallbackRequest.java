package com.changping.platform.modules.integration.alarm.dto;

import java.util.Map;

/**
 * @Author lxy
 * @Description //第三方告警回调请求DTO，封装第三方系统推送的原始告警载荷Map
 * @Date 2026/04/18 10:00
 */
public record ThirdPartyAlarmCallbackRequest(Map<String, Object> payload) {
}
