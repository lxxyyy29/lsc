package com.changping.platform.modules.event.task;

import com.changping.platform.modules.event.service.TrendAlertService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 趋势预判/反复投诉自动预警定时任务
 * 每 30 分钟扫描一次：同类型同网格 7 天 ≥3 件、同地点 7 天 ≥2 次 → 自动预警
 */
@Component
public class TrendAlertTask {

    private static final Logger log = LoggerFactory.getLogger(TrendAlertTask.class);

    private final TrendAlertService trendAlertService;

    public TrendAlertTask(TrendAlertService trendAlertService) {
        this.trendAlertService = trendAlertService;
    }

    @Scheduled(cron = "0 */30 * * * *")
    public void scanTrend() {
        try {
            log.info("[TrendAlertTask] 开始扫描趋势预警...");
            long start = System.currentTimeMillis();
            int created = trendAlertService.scan();
            long cost = System.currentTimeMillis() - start;
            log.info("[TrendAlertTask] 扫描完成，新生成 {} 条预警，耗时 {}ms", created, cost);
        } catch (Exception e) {
            log.error("[TrendAlertTask] 扫描异常: {}", e.getMessage(), e);
        }
    }
}
