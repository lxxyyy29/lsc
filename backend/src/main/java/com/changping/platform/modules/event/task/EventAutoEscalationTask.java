package com.changping.platform.modules.event.task;

import com.changping.platform.modules.event.service.EventService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 事件紧急程度自动升级定时任务
 * 每30分钟执行一次：
 * - GREEN 超过24小时 → 自动升级为 YELLOW
 * - YELLOW 超过48小时 → 自动升级为 RED
 */
@Component
public class EventAutoEscalationTask {

    private static final Logger log = LoggerFactory.getLogger(EventAutoEscalationTask.class);

    private final EventService eventService;

    public EventAutoEscalationTask(EventService eventService) {
        this.eventService = eventService;
    }

    @Scheduled(cron = "0 */30 * * * *")
    public void autoEscalate() {
        try {
            log.info("[EventAutoEscalation] 开始执行事件紧急程度自动升级...");
            long start = System.currentTimeMillis();
            eventService.autoEscalateUrgency();
            long cost = System.currentTimeMillis() - start;
            log.info("[EventAutoEscalation] 执行完成，耗时 {}ms", cost);
        } catch (Exception e) {
            log.error("[EventAutoEscalation] 执行异常: {}", e.getMessage(), e);
        }
    }
}
