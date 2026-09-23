package com.changping.platform.modules.integration.alarm.task;

import com.changping.platform.modules.integration.alarm.service.AlarmPullService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 三方平台告警定时拉取任务。
 *
 * <p>默认关闭（{@code drone.alarm-integration.pull.enabled=false}）：
 * 平台侧用「推送」模式时无需开启；改用主动拉取时再打开，
 * 避免平台没有告警查询接口的环境每轮刷错误日志。</p>
 */
@Component
@ConditionalOnProperty(prefix = "drone.alarm-integration.pull", name = "enabled", havingValue = "true")
public class AlarmPullTask {

    private static final Logger log = LoggerFactory.getLogger(AlarmPullTask.class);

    private final AlarmPullService alarmPullService;

    public AlarmPullTask(AlarmPullService alarmPullService) {
        this.alarmPullService = alarmPullService;
    }

    /** 默认每 5 分钟拉取一次，可用 drone.alarm-integration.pull.cron 调整 */
    @Scheduled(cron = "${drone.alarm-integration.pull.cron:0 */5 * * * *}")
    public void pull() {
        try {
            log.debug("[AlarmPullTask] 开始拉取三方告警...");
            alarmPullService.pullOnce();
        } catch (Exception exception) {
            // 定时任务不能让异常冒泡，否则后续轮次会被调度器取消
            log.warn("[AlarmPullTask] 拉取三方告警异常：{}", exception.toString());
        }
    }
}
