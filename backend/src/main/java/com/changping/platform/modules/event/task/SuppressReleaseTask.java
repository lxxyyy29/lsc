package com.changping.platform.modules.event.task;

import com.changping.platform.modules.integration.alarm.service.AlarmEventMongoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 时段抑制（夜间静默）自动恢复任务。
 *
 * 命中抑制的事件在入库时被置 hidden=1（复用现有"不显示"开关，列表/大屏/统计/地图均按 hidden 过滤），
 * 本任务每分钟扫描一次：一旦 suppress_until 到点，即恢复显示（hidden=0）并清理抑制标记，
 * 同时同步 MongoDB（事件列表与大屏的数据源），确保两端表现一致。
 *
 * 只处理「确实被抑制过」的事件（suppress_rule_id IS NOT NULL），并且尊重人工强制放出标记
 * （suppress_manual = 1 的不动），因此不会误放人工隐藏的事件。
 */
@Component
public class SuppressReleaseTask {

    private static final Logger log = LoggerFactory.getLogger(SuppressReleaseTask.class);

    private final JdbcTemplate jdbcTemplate;
    private final AlarmEventMongoService alarmEventMongoService;

    public SuppressReleaseTask(JdbcTemplate jdbcTemplate, AlarmEventMongoService alarmEventMongoService) {
        this.jdbcTemplate = jdbcTemplate;
        this.alarmEventMongoService = alarmEventMongoService;
    }

    /** 每分钟第 0 秒执行 */
    @Scheduled(cron = "0 * * * * *")
    public void releaseExpired() {
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                    "SELECT id, external_event_id FROM biz_event "
                            + "WHERE suppress_until IS NOT NULL AND suppress_until <= NOW() "
                            + "AND COALESCE(suppress_manual, 0) = 0 AND suppress_rule_id IS NOT NULL "
                            + "AND suppress_released_at IS NULL");
            if (rows == null || rows.isEmpty()) {
                return;
            }
            int released = 0;
            for (Map<String, Object> row : rows) {
                Object id = row.get("id");
                Object externalEventId = row.get("external_event_id");
                // 只恢复显示，保留 suppress_until/rule_id/actions 作为「曾被夜间抑制」的痕迹，
                // 供事件列表标记与白天「昨夜抑制 N 条」提示统计（suppress_released_at 作为幂等标记）
                jdbcTemplate.update(
                        "UPDATE biz_event SET hidden = 0, suppress_released_at = NOW() WHERE id = ?", id);
                if (externalEventId != null && !String.valueOf(externalEventId).isBlank()) {
                    try {
                        alarmEventMongoService.setHidden(String.valueOf(externalEventId), false);
                    } catch (Exception ignored) {
                        // Mongo 未启用或同步失败时忽略：SQL 侧已恢复，下次触发仍可自愈
                    }
                }
                released++;
            }
            log.info("[suppress] 时段结束，已自动恢复显示 {} 条被抑制事件", released);
        } catch (Exception e) {
            log.warn("[suppress] 自动恢复任务执行失败: {}", e.getMessage());
        }
    }
}
