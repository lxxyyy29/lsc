package com.changping.platform.modules.workorder.task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 工单超时自动处理定时任务
 * - 超过 48h 未接单的工单 → 自动关闭并通知管理员
 * - 超过 72h 未处置的工单 → 自动升级紧急度并通知
 */
@Component
public class WorkOrderAutoHandleTask {

    private static final Logger log = LoggerFactory.getLogger(WorkOrderAutoHandleTask.class);

    private final JdbcTemplate jdbcTemplate;

    public WorkOrderAutoHandleTask(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 每小时检查工单超时情况
     */
    @Scheduled(cron = "0 0 * * * *")
    public void autoHandleOverdue() {
        try {
            log.info("[WorkOrderAutoHandle] 开始检查工单超时...");

            // 1. 超过 48h 未接单 → 自动关闭
            int closed = jdbcTemplate.update(
                "UPDATE biz_work_order SET status = 'CLOSED', close_reason = '超时未接单自动关闭', " +
                "closed_at = NOW(), updated_at = NOW() " +
                "WHERE status = 'WAITING_ACCEPT' AND created_at < DATE_SUB(NOW(), INTERVAL 48 HOUR)");
            if (closed > 0) {
                log.info("[WorkOrderAutoHandle] 自动关闭 {} 个超时未接手工单", closed);
            }

            // 2. 超过 72h 未处置 → 升级紧急度为 RED
            int escalated = jdbcTemplate.update(
                "UPDATE biz_work_order SET urgency_level = 'RED', updated_at = NOW() " +
                "WHERE status = 'PROCESSING' AND urgency_level != 'RED' " +
                "AND created_at < DATE_SUB(NOW(), INTERVAL 72 HOUR)");
            if (escalated > 0) {
                log.info("[WorkOrderAutoHandle] 升级 {} 个超时工单为紧急", escalated);
            }

            // 3. 生成通知给超期工单管理员
            List<Map<String, Object>> overdueOrders = jdbcTemplate.queryForList(
                "SELECT id, work_order_no, assignee_user_id, assignee_name, " +
                "TIMESTAMPDIFF(HOUR, created_at, NOW()) as hours " +
                "FROM biz_work_order WHERE status IN ('WAITING_ACCEPT', 'PROCESSING') " +
                "AND created_at < DATE_SUB(NOW(), INTERVAL 48 HOUR)");

            for (Map<String, Object> order : overdueOrders) {
                Long assigneeId = ((Number) order.get("assignee_user_id")).longValue();
                String workOrderNo = (String) order.get("work_order_no");
                int hours = ((Number) order.get("hours")).intValue();

                // 检查是否已发过通知
                Long existCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM sys_notification WHERE related_type = 'WORK_ORDER' " +
                    "AND related_id = ? AND type = 'WORK_ORDER_OVERDUE'",
                    Long.class, ((Number) order.get("id")).longValue());

                if (existCount == null || existCount == 0) {
                    jdbcTemplate.update(
                        "INSERT INTO sys_notification (user_id, title, content, type, level, related_type, related_id, is_read, created_at) " +
                        "VALUES (?, ?, ?, 'WORK_ORDER_OVERDUE', 'URGENT', 'WORK_ORDER', ?, 0, NOW())",
                        assigneeId,
                        "工单严重超期",
                        "工单 " + workOrderNo + " 已超期 " + hours + " 小时，请尽快处理",
                        ((Number) order.get("id")).longValue());
                }
            }

            log.info("[WorkOrderAutoHandle] 检查完成");
        } catch (Exception e) {
            log.error("[WorkOrderAutoHandle] 执行异常: {}", e.getMessage(), e);
        }
    }
}
