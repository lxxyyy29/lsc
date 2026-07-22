package com.changping.platform.modules.integration.alarm.controller;

import com.changping.platform.common.response.ApiResponse;
import com.changping.platform.modules.integration.alarm.dto.ThirdPartyAlarmIngestResult;
import com.changping.platform.modules.integration.alarm.security.ThirdPartyCallbackVerifier;
import com.changping.platform.modules.integration.alarm.service.ThirdPartyAlarmIngestService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author tangxinglin
 * @Description //第三方告警回调控制器，接收第三方系统推送的告警事件，
 * 完成签名验证、数据解析及告警摄入，返回上游兼容的整型响应码格式
 * @Date 2026/04/18 10:00
 */
@RestController
@RequestMapping("/integrations/alarms")
public class ThirdPartyAlarmCallbackController {

    private final ThirdPartyAlarmIngestService thirdPartyAlarmIngestService;
    private final ThirdPartyCallbackVerifier thirdPartyCallbackVerifier;
    private final ObjectMapper objectMapper;

    /**
     * @Author tangxinglin
     * @Description //构造函数，注入告警摄入服务、回调验证器及JSON序列化工具
     * @Date 2026/04/18 10:00
     * @Param [thirdPartyAlarmIngestService 告警摄入服务, thirdPartyCallbackVerifier 回调验证器, objectMapper JSON序列化工具]
     * @return void
     */
    public ThirdPartyAlarmCallbackController(
            ThirdPartyAlarmIngestService thirdPartyAlarmIngestService,
            ThirdPartyCallbackVerifier thirdPartyCallbackVerifier,
            ObjectMapper objectMapper) {
        this.thirdPartyAlarmIngestService = thirdPartyAlarmIngestService;
        this.thirdPartyCallbackVerifier = thirdPartyCallbackVerifier;
        this.objectMapper = objectMapper;
    }

    /**
     * @Author tangxinglin
     * @Description //接收第三方告警回调，完成验证和摄入，返回上游兼容的整型响应码格式
     * @Date 2026/04/18 10:00
     * @Param [rawBody 原始请求体字符串, request HTTP请求对象（用于读取验证头）]
     * @return Map<String, Object> 包含code=200的响应Map
     */
    @PostMapping("/callback")
    public Map<String, Object> callback(
            @RequestBody(required = false) String rawBody,
            HttpServletRequest request) throws Exception {
        boolean verified = thirdPartyCallbackVerifier.verify(request, rawBody);
        Map<String, Object> payload = parseBody(rawBody);
        ThirdPartyAlarmIngestResult result = thirdPartyAlarmIngestService.ingest(payload, verified);
        // Upstream expects {"code": 200} as integer — ApiResponse returns {"code": "OK"} which fails upstream check
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("code", 200);
        response.put("success", true);
        response.put("message", "OK");
        response.put("data", result);
        return response;
    }

    /**
     * @Author tangxinglin
     * @Description //将原始请求体字符串解析为Map，空体时返回空Map
     * @Date 2026/04/18 10:00
     * @Param [rawBody 原始JSON字符串]
     * @return Map<String, Object> 解析后的载荷Map
     */
    private Map<String, Object> parseBody(String rawBody) throws Exception {
        if (rawBody == null || rawBody.isBlank()) {
            return new LinkedHashMap<>();
        }
        return objectMapper.readValue(rawBody, new TypeReference<>() {});
    }
}
