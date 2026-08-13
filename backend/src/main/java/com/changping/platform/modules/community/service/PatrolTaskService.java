package com.changping.platform.modules.community.service;

import com.changping.platform.modules.community.entity.PatrolTaskEntity;
import com.changping.platform.modules.community.mapper.PatrolTaskMapper;
import com.changping.platform.modules.notification.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class PatrolTaskService {

    private static final Logger log = LoggerFactory.getLogger(PatrolTaskService.class);

    private final PatrolTaskMapper mapper;
    private final JdbcTemplate jdbcTemplate;
    private final NotificationService notificationService;

    public PatrolTaskService(PatrolTaskMapper mapper, JdbcTemplate jdbcTemplate, NotificationService notificationService) {
        this.mapper = mapper;
        this.jdbcTemplate = jdbcTemplate;
        this.notificationService = notificationService;
    }

    /**
     * 为所有活跃网格生成本周巡查任务
     * 优先小网格（level=3），如果没有则使用大网格（level=2）
     */
    public int generateWeeklyTasks() {
        // 获取所有活跃的小网格
        List<Map<String, Object>> grids = mapper.findActiveSmallGrids();
        // 如果没有小网格，使用大网格
        if (grids.isEmpty()) {
            grids = mapper.findActiveLargeGrids();
        }
        LocalDate today = LocalDate.now();
        LocalDate startOfWeek = today.with(DayOfWeek.MONDAY);
        int count = 0;

        for (Map<String, Object> grid : grids) {
            Long gridId = ((Number) grid.get("grid_id")).longValue();
            String gridName = (String) grid.get("grid_name");
            // 检查是否已生成本周任务
            if (mapper.existsTaskForWeek(gridId, startOfWeek)) {
                continue;
            }
            PatrolTaskEntity task = new PatrolTaskEntity();
            task.setGridId(gridId);
            task.setTaskName(gridName + " - 周巡查任务");
            task.setPlannedDate(startOfWeek.plusDays(count % 7));
            task.setStatus("PENDING");
            mapper.insert(task);
            count++;
        }
        return count;
    }

    /**
     * 检查超期未巡任务并标记为 OVERDUE
     */
    public int markOverdueTasks() {
        return mapper.markOverdueTasks(LocalDate.now());
    }

    public List<PatrolTaskEntity> listByUser(Long userId) {
        return mapper.findByUserId(userId);
    }

    public List<PatrolTaskEntity> listByGrid(Long gridId) {
        return mapper.findByGridId(gridId);
    }

    public boolean completeTask(Long taskId) {
        return mapper.completeTask(taskId, LocalDate.now()) > 0;
    }

    /**
     * 获取所有巡查任务（Web端管理用）
     */
    public List<PatrolTaskEntity> listAll() {
        return mapper.findAll();
    }

    /**
     * 巡查任务统计
     */
    public PatrolTaskMapper.PatrolTaskStatistics getStatistics() {
        return mapper.getStatistics();
    }

    /**
     * 到期未巡自动预警（C1）
     * 扫描今日到期 / 明日到期仍未完成的任务，向任务负责人（或该网格网格员）发送站内提醒；
     * 同一任务只提醒一次（related_type=PATROL_TASK 去重）。
     * @return 本次发送的提醒数
     */
    public int checkUpcomingTasks() {
        List<Map<String, Object>> tasks = jdbcTemplate.queryForList(
            "SELECT t.id, t.grid_id, t.user_id, t.task_name, t.planned_date, g.grid_name " +
            "FROM cmn_patrol_task t LEFT JOIN cmn_grid g ON g.id = t.grid_id " +
            "WHERE t.status = 'PENDING' AND t.planned_date BETWEEN CURDATE() AND DATE_ADD(CURDATE(), INTERVAL 1 DAY)");
        int sent = 0;
        for (Map<String, Object> task : tasks) {
            Long taskId = ((Number) task.get("id")).longValue();
            if (notificationExists(taskId)) {
                continue;
            }
            Set<Long> recipients = resolveRecipients(task);
            if (recipients.isEmpty()) {
                log.info("[PatrolRemind] 任务 {} 无负责人，跳过提醒", taskId);
                continue;
            }
            String gridName = (String) task.get("grid_name");
            String taskName = (String) task.get("task_name");
            java.sql.Date planned = (java.sql.Date) task.get("planned_date");
            boolean today = planned != null && planned.toLocalDate().equals(LocalDate.now());
            String title = today ? "巡查任务今日到期提醒" : "巡查任务明日到期提醒";
            String content = "网格「" + (gridName != null ? gridName : taskName) + "」的巡查任务（" + taskName + "）将于 "
                    + (planned != null ? planned : "") + " 到期，请及时完成巡查，逾期将标记超期。";
            for (Long uid : recipients) {
                try {
                    notificationService.createNotification(uid, title, content, "PATROL_TASK",
                            today ? "URGENT" : "NORMAL", "PATROL_TASK", taskId);
                    sent++;
                } catch (Exception e) {
                    log.error("[PatrolRemind] 通知用户 {} 异常: {}", uid, e.getMessage());
                }
            }
        }
        log.info("[PatrolRemind] 扫描完成，{} 个到期任务，发送 {} 条提醒", tasks.size(), sent);
        return sent;
    }

    /** 同一任务是否已发过到期提醒 */
    private boolean notificationExists(Long taskId) {
        Long count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM sys_notification WHERE related_type = 'PATROL_TASK' AND related_id = ? AND type = 'PATROL_TASK'",
            Long.class, taskId);
        return count != null && count > 0;
    }

    /** 通知对象：任务指派人员 + 该网格组织成员（去重） */
    private Set<Long> resolveRecipients(Map<String, Object> task) {
        Set<Long> recipients = new LinkedHashSet<>();
        Number userId = (Number) task.get("user_id");
        if (userId != null) {
            recipients.add(userId.longValue());
        }
        Number gridId = (Number) task.get("grid_id");
        if (gridId != null) {
            List<Long> members = jdbcTemplate.queryForList(
                "SELECT DISTINCT sys_user_id FROM cmn_org_member WHERE grid_id = ? AND status = 'ACTIVE' AND sys_user_id IS NOT NULL",
                Long.class, gridId.longValue());
            recipients.addAll(members);
        }
        return recipients;
    }
}
