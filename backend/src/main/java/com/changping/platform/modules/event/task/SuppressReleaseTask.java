package com.changping.platform.modules.event.task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 时段抑制（夜间静默）到点结算任务。
 *
 * <p><b>2026-10-17 需求变更（重要）</b>：原实现「到点自动恢复显示（hidden=0）」已被废弃。
 * 业务反馈：夜间无人机抓拍的违停告警在白天仍占据事件列表/大屏（10-16 夜间一次放了 118 条），
 * 等同于没有静默。现改为 <b>夜间抑制的事件白天也保持隐藏</b>，必须人工「放出」才进入正常流程：
 * <ul>
 *   <li>窗口内（suppress_until &gt; NOW()）：hidden=1，不派单、不通知（NO_DISPATCH/NO_NOTIFY 生效）；</li>
 *   <li>窗口结束（suppress_until &lt;= NOW()）：<b>不做任何改动</b>，继续 hidden=1，等待人工放出；</li>
 *   <li>人工放出（PUT /events/{id}/suppress-manual 或 /events/suppress-manual/release-pending）：
 *       hidden=0、suppress_manual=1、记 suppress_released_at，此后按正常事件派单与通知。</li>
 * </ul>
 *
 * 因此本任务不再写库，只做「待放行」观测：待放行条数变化时打一条日志，
 * 便于运维在日志里看到累积量（前端另有提示条与「只看夜间抑制」筛选可自助处理）。
 */
@Component
public class SuppressReleaseTask {

    private static final Logger log = LoggerFactory.getLogger(SuppressReleaseTask.class);

    private final JdbcTemplate jdbcTemplate;

    /** 上一次播报的待放行条数，用于避免每分钟重复刷日志 */
    private volatile long lastReported = -1L;

    public SuppressReleaseTask(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /** 每分钟第 0 秒执行：只统计「窗口已结束但仍隐藏」的待放行条数，不改数据 */
    @Scheduled(cron = "0 * * * * *")
    public void reportPendingRelease() {
        try {
            Long pending = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM biz_event "
                            + "WHERE suppress_rule_id IS NOT NULL AND COALESCE(suppress_manual, 0) = 0 "
                            + "AND COALESCE(deleted, 0) = 0 AND COALESCE(hidden, 0) = 1 "
                            + "AND suppress_until IS NOT NULL AND suppress_until <= NOW()",
                    Long.class);
            long count = pending == null ? 0L : pending;
            if (count > 0 && count != lastReported) {
                log.info("[suppress] 夜间抑制已到点，待人工放出 {} 条（事件列表勾选「只看夜间抑制」处理，"
                        + "或调用 /api/events/suppress-manual/release-pending 批量放出）", count);
            } else if (count == 0 && lastReported > 0) {
                log.info("[suppress] 待放行的夜间抑制事件已全部处理完毕");
            }
            lastReported = count;
        } catch (Exception e) {
            log.warn("[suppress] 待放行统计失败: {}", e.getMessage());
        }
    }
}
