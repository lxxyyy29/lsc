package com.changping.platform.modules.integration.alarm.service;

import com.changping.platform.modules.drone.service.WaylineAlgoViolationAreaService;
import com.changping.platform.modules.event.entity.EventEntity;
import com.changping.platform.modules.integration.alarm.document.AlarmEventDocument;
import com.changping.platform.modules.integration.alarm.dto.NormalizedAlarmEvent;
import com.changping.platform.modules.integration.alarm.dto.ThirdPartyAlarmIngestResult;
import java.math.BigDecimal;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @Author tangxinglin
 * @Description //第三方告警摄入服务,协调MongoDB告警事件保存和SQL事件投影创建,
 * 完成告警从第三方推送到平台工作流的完整接入流程
 * @Date 2026/04/18 10:00
 */
@Service
public class ThirdPartyAlarmIngestService {

    private static final Logger log = LoggerFactory.getLogger(ThirdPartyAlarmIngestService.class);
    /** 上游手飞场景航线ID约定值,这种告警不参与违章区域过滤,直接放行 */
    private static final String VIRTUAL_FLY_LINE_ID = "virtual-flyline-id";

    private final AlarmEventMongoService alarmEventMongoService;
    private final EventProjectionBridgeService eventProjectionBridgeService;
    private final WaylineAlgoViolationAreaService waylineAlgoViolationAreaService;

    public ThirdPartyAlarmIngestService(
            AlarmEventMongoService alarmEventMongoService,
            EventProjectionBridgeService eventProjectionBridgeService,
            WaylineAlgoViolationAreaService waylineAlgoViolationAreaService) {
        this.alarmEventMongoService = alarmEventMongoService;
        this.eventProjectionBridgeService = eventProjectionBridgeService;
        this.waylineAlgoViolationAreaService = waylineAlgoViolationAreaService;
    }

    /**
     * 摄入第三方告警。入库前用经纬度做违章区域过滤:
     * - 手飞 / 虚拟航线(flyLineId == virtual-flyline-id) → 直接放行,不过滤
     * - 真实航线 → 必须 (flyLineId + label) 在本地有区域配置且坐标命中其中一个区域,否则丢弃
     * 上游与本地都使用高德 GCJ-02 坐标系,无需转换
     */
    @Transactional
    public ThirdPartyAlarmIngestResult ingest(Map<String, Object> rawPayload, boolean verified) {
        NormalizedAlarmEvent normalizedEvent = alarmEventMongoService.normalize(rawPayload);

        String flyLineId = extractFlyLineId(rawPayload);
        if (!VIRTUAL_FLY_LINE_ID.equals(flyLineId)) {
            String modelLabel = extractAlgorithmTypeValue(rawPayload);
            Double lng = toDouble(normalizedEvent.longitude());
            Double lat = toDouble(normalizedEvent.latitude());
            if (!waylineAlgoViolationAreaService.allowAlarm(flyLineId, modelLabel, lng, lat)) {
                log.info("Alarm dropped by violation-area filter: flyLineId={}, label={}, lng={}, lat={}, externalEventId={}",
                        flyLineId, modelLabel, lng, lat, normalizedEvent.externalEventId());
                return new ThirdPartyAlarmIngestResult(null, normalizedEvent.externalEventId(), normalizedEvent.dedupKey(), false, "FILTERED_OUT_OF_AREA");
            }
        }

        AlarmEventDocument document = alarmEventMongoService.saveNewAlarm(normalizedEvent, verified);
        boolean duplicate = document.getWorkflowStatus() != null && document.getWorkflowStatus().getSqlEventId() != null;
        EventEntity projection = eventProjectionBridgeService.createOrReuseProjection(normalizedEvent);
        return new ThirdPartyAlarmIngestResult(
                projection.getId(),
                projection.getExternalEventId(),
                normalizedEvent.dedupKey(),
                duplicate,
                projection.getStatus());
    }

    private String extractFlyLineId(Map<String, Object> payload) {
        if (payload == null) return null;
        Object v = firstNonNull(payload.get("flyLineId"), payload.get("fly_line_id"), payload.get("flylineId"));
        return v == null ? null : String.valueOf(v).trim();
    }

    /**
     * 上游推送的 modelName 格式为 "{算法模型名}-{label}",如 "AI检测-摊贩摆摊"。
     * 本地绑定时用的是 label(如 "摊贩摆摊"),需要从 modelName 里提取后半段。
     * 兼容:如果 modelName 不含 "-",直接返回整个 modelName。
     */
    private String extractAlgorithmTypeValue(Map<String, Object> payload) {
        if (payload == null) return null;

        // 优先从 modelName 提取 label(上游拼接格式: "算法名-label")
        Object modelNameObj = firstNonNull(payload.get("modelName"), payload.get("model_name"));
        if (modelNameObj != null) {
            String modelName = String.valueOf(modelNameObj).trim();
            int lastDash = modelName.lastIndexOf('-');
            if (lastDash > 0 && lastDash < modelName.length() - 1) {
                return modelName.substring(lastDash + 1);
            }
            // 如果没有 "-",返回整个 modelName 作为兜底
            return modelName;
        }

        // 兜底:尝试直接字段(理论上不会走到这里)
        Object v = firstNonNull(
                payload.get("algorithmTypeValue"),
                payload.get("algorithm_type_value"),
                payload.get("label"),
                payload.get("modelLabel"),
                payload.get("model_label"));
        return v == null ? null : String.valueOf(v).trim();
    }

    private Object firstNonNull(Object... values) {
        for (Object v : values) {
            if (v != null && !String.valueOf(v).isBlank()) return v;
        }
        return null;
    }

    private Double toDouble(BigDecimal value) {
        return value == null ? null : value.doubleValue();
    }
}
