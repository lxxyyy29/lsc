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
 * @Author lxy
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
    /** 时段抑制（夜间静默）规则：命中则在事件上打标记，查询层据此过滤 */
    private final com.changping.platform.modules.system.service.SuppressWindowService suppressWindowService;
    private final org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    public ThirdPartyAlarmIngestService(
            AlarmEventMongoService alarmEventMongoService,
            EventProjectionBridgeService eventProjectionBridgeService,
            WaylineAlgoViolationAreaService waylineAlgoViolationAreaService,
            com.changping.platform.modules.system.service.SuppressWindowService suppressWindowService,
            org.springframework.jdbc.core.JdbcTemplate jdbcTemplate) {
        this.alarmEventMongoService = alarmEventMongoService;
        this.eventProjectionBridgeService = eventProjectionBridgeService;
        this.waylineAlgoViolationAreaService = waylineAlgoViolationAreaService;
        this.suppressWindowService = suppressWindowService;
        this.jdbcTemplate = jdbcTemplate;
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

        // 时段抑制（夜间静默）：命中规则则打标记；查询层按 suppress_until 统一过滤 → 时段内"不显示"，
        // 时段结束后自动恢复可见（无需定时任务）。标记写入失败不影响告警入库。
        try {
            Object extRaw = rawPayload.get("externalEventId");
            String externalEventId = extRaw == null ? null : String.valueOf(extRaw);
            String eventType = rawPayload.get("eventType") == null ? null : String.valueOf(rawPayload.get("eventType"));
            String urgency = rawPayload.get("urgencyLevel") == null ? null : String.valueOf(rawPayload.get("urgencyLevel"));
            if (externalEventId != null && !externalEventId.isBlank()) {
                java.util.Optional<com.changping.platform.modules.system.service.SuppressWindowService.Hit> hit =
                        suppressWindowService.evaluate("THIRD_PARTY_DRONE", eventType, urgency, java.time.LocalDateTime.now());
                if (hit.isPresent()) {
                    com.changping.platform.modules.system.service.SuppressWindowService.Hit h = hit.get();
                    jdbcTemplate.update(
                            "UPDATE biz_event SET suppress_until = ?, suppress_rule_id = ? WHERE external_event_id = ?",
                            java.sql.Timestamp.valueOf(h.until()), h.ruleId(), externalEventId);
                    // 「不显示」复用 hidden 开关：现有事件列表/大屏/统计/地图（SQL 与 MongoDB）均已按 hidden 过滤，
                    // 因此无需逐个改动查询；由 SuppressReleaseTask 在 suppress_until 到点后自动恢复显示。
                    if (h.actions().contains("HIDE")) {
                        jdbcTemplate.update("UPDATE biz_event SET hidden = 1 WHERE external_event_id = ?", externalEventId);
                        try {
                            alarmEventMongoService.setHidden(externalEventId, true);
                        } catch (Exception ignoredMongo) {
                            // Mongo 未启用或同步失败时忽略：SQL 侧已标记，不影响抑制效果
                        }
                    }
                    // TODO(阶段B)：Mongo 文档同步写 suppressUntil 并在 Mongo 查询追加可见性条件，
                    //           否则以 MongoDB 为数据源的列表/大屏仍会显示被抑制事件。
                }
            }
        } catch (Exception ignored) {
            // 抑制标记属于「锦上添花」，任何异常都不应影响告警正常入库
        }

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
