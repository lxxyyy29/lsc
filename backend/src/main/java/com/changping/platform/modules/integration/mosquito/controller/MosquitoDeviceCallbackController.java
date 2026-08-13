package com.changping.platform.modules.integration.mosquito.controller;

import com.changping.platform.modules.safety.service.MosquitoService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 爱卫蚊媒 — 检测设备数据上报回调控制器
 * 接收蚊媒监测设备（智能捕蚊器/密度监测仪等）推送的监测数据，
 * 完成自动设备注册、孳生地关联、超标判定并落库。
 * 参照第三方告警回调先例设计为公开接口（设备无平台账号），生产环境建议增加签名校验。
 */
@RestController
@RequestMapping("/integrations/mosquito")
public class MosquitoDeviceCallbackController {

    private static final Logger log = LoggerFactory.getLogger(MosquitoDeviceCallbackController.class);

    private final MosquitoService mosquitoService;
    private final ObjectMapper objectMapper;

    public MosquitoDeviceCallbackController(MosquitoService mosquitoService, ObjectMapper objectMapper) {
        this.mosquitoService = mosquitoService;
        this.objectMapper = objectMapper;
    }

    /**
     * 设备上报监测数据
     * body: {"deviceNo":"MD-xxx","deviceName":"xx智能捕蚊器","deviceType":"MOSQUITO_TRAP",
     *        "siteId":1,"siteName":"xx孳生地","metricType":"DENSITY","metricValue":12.5,
     *        "threshold":10,"collectedAt":"2026-08-12 10:00:00"}
     */
    @PostMapping("/device-data")
    public Map<String, Object> ingest(@RequestBody(required = false) String rawBody, HttpServletRequest request) {
        Map<String, Object> response = new LinkedHashMap<>();
        try {
            Map<String, Object> payload = parseBody(rawBody);
            Map<String, Object> result = mosquitoService.ingestDeviceData(payload);
            response.put("code", 200);
            response.put("success", true);
            response.put("message", "OK");
            response.put("data", result);
        } catch (IllegalArgumentException e) {
            log.warn("设备上报参数不合法: {}", e.getMessage());
            response.put("code", 400);
            response.put("success", false);
            response.put("message", e.getMessage());
        } catch (Exception e) {
            log.error("设备上报处理失败", e);
            response.put("code", 500);
            response.put("success", false);
            response.put("message", "设备数据入库失败: " + e.getMessage());
        }
        return response;
    }

    private Map<String, Object> parseBody(String rawBody) throws Exception {
        if (rawBody == null || rawBody.isBlank()) {
            return new LinkedHashMap<>();
        }
        return objectMapper.readValue(rawBody, new TypeReference<>() {});
    }
}
