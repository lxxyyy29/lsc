package com.changping.platform.modules.notification.task;

import com.changping.platform.modules.notification.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 通知定时任务 - 检查工单超期并生成通知
 * 每小时执行一次
 */
@Component
public class NotificationScheduler {

    private static final Logger log = LoggerFactory.getLogger(NotificationScheduler.class);

    private final JdbcTemplate jdbcTemplate;
    private final NotificationService notificationService;

    public NotificationScheduler(JdbcTemplate jdbcTemplate, NotificationService notificationService) {
        this.jdbcTemplate = jdbcTemplate;
        this.notificationService = notificationService;
    }

    /**
     * 每小时检查工单超期情况
     * 超期定义：工单创建超过 24h 且状态为 WAITING_ACCEPT 或 PROCESSING
     */
    @Scheduled(cron = "0 0 * * * *")
    public void checkWorkOrderOverdue() {
        try {
            log.info("[NotificationScheduler] 开始检查工单超期...");

            // 查找超期工单（创建超过 24h 仍未完成）
            String sql = "SELECT wo.id, wo.work_order_no, wo.assignee_user_id, wo.assignee_name, " +
                    "TIMESTAMPDIFF(HOUR, wo.created_at, NOW()) as hours_elapsed " +
                    "FROM biz_work_order wo " +
                    "WHERE wo.status IN ('WAITING_ACCEPT', 'PROCESSING') " +
                    "AND wo.created_at < DATE_SUB(NOW(), INTERVAL 24 HOUR) " +
                    "AND wo.urgency_level != 'RED'";  // 已标红的不再重复通知

            List<Map<String, Object>> overdueOrders = jdbcTemplate.queryForList(sql);

            for (Map<String, Object> order : overdueOrders) {
                Long assigneeId = ((Number) order.get("assignee_user_id")).longValue();
                String workOrderNo = (String) order.get("work_order_no");
                int hours = ((Number) order.get("hours_elapsed")).intValue();

                // 检查是否已为此工单发过超期通知（避免重复）
                if (notificationExists("WORK_ORDER", ((Number) order.get("id")).longValue())) {
                    continue;
                }

                notificationService.createNotification(
                        assigneeId,
                        "工单超期提醒",
                        "工单 " + workOrderNo + " 已超期 " + hours + " 小时，请尽快处理",
                        "WORK_ORDER",
                        hours > 48 ? "URGENT" : "NORMAL",
                        "WORK_ORDER",
                        ((Number) order.get("id")).longValue()
                );
            }

            log.info("[NotificationScheduler] 检查完成，发现 {} 个超期工单", overdueOrders.size());
        } catch (Exception e) {
            log.error("[NotificationScheduler] 执行异常: {}", e.getMessage(), e);
        }
    }

    private boolean notificationExists(String relatedType, Long relatedId) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sys_notification WHERE related_type = ? AND related_id = ? AND type = 'WORK_ORDER'",
                Long.class, relatedType, relatedId);
        return count != null && count > 0;
    }
}
