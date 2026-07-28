package com.changping.platform.modules.community.task;

import com.changping.platform.modules.community.service.PatrolTaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 巡查任务定时调度
 * - 每周一凌晨1点生成本周巡查任务
 * - 每天凌晨2点标记超期任务
 */
@Component
public class PatrolTaskScheduler {

    private static final Logger log = LoggerFactory.getLogger(PatrolTaskScheduler.class);

    private final PatrolTaskService patrolTaskService;

    public PatrolTaskScheduler(PatrolTaskService patrolTaskService) {
        this.patrolTaskService = patrolTaskService;
    }

    /**
     * 每周一凌晨1:00 自动生成本周巡查任务
     */
    @Scheduled(cron = "0 0 1 * * MON")
    public void generateWeeklyTasks() {
        try {
            log.info("[PatrolTaskScheduler] 开始生成周巡查任务...");
            int count = patrolTaskService.generateWeeklyTasks();
            log.info("[PatrolTaskScheduler] 生成完成，共 {} 个任务", count);
        } catch (Exception e) {
            log.error("[PatrolTaskScheduler] 生成任务异常: {}", e.getMessage(), e);
        }
    }

    /**
     * 每天凌晨2:00 标记超期未完成的巡查任务
     */
    @Scheduled(cron = "0 0 2 * * *")
    public void markOverdueTasks() {
        try {
            log.info("[PatrolTaskScheduler] 开始标记超期巡查任务...");
            int count = patrolTaskService.markOverdueTasks();
            log.info("[PatrolTaskScheduler] 标记完成，共 {} 个超期任务", count);
        } catch (Exception e) {
            log.error("[PatrolTaskScheduler] 标记超期异常: {}", e.getMessage(), e);
        }
    }
}
