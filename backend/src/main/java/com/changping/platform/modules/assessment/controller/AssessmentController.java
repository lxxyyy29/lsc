package com.changping.platform.modules.assessment.controller;

import com.changping.platform.common.response.ApiResponse;
import com.changping.platform.modules.auth.security.PermissionCodes;
import com.changping.platform.modules.auth.security.PermissionGuard;
import com.changping.platform.modules.auth.service.AuthService;
import com.changping.platform.modules.auth.service.CurrentUserService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/assessment")
public class AssessmentController {

    private final JdbcTemplate jdbcTemplate;
    private final CurrentUserService currentUserService;
    private final PermissionGuard permissionGuard;

    public AssessmentController(JdbcTemplate jdbcTemplate, CurrentUserService currentUserService, PermissionGuard permissionGuard) {
        this.jdbcTemplate = jdbcTemplate;
        this.currentUserService = currentUserService;
        this.permissionGuard = permissionGuard;
    }

    /**
     * 总览统计
     */
    @GetMapping("/overview")
    public ApiResponse<Map<String, Object>> overview() {
        requireAssessmentPermission();
        Map<String, Object> result = new HashMap<>();

        // 事件统计
        Long totalEvents = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM biz_event", Long.class);
        Long waitingDispatch = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM biz_event WHERE status = 'WAITING_DISPATCH'", Long.class);
        Long dispatched = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM biz_event WHERE status = 'DISPATCHED_TO_WORK_ORDER'", Long.class);
        Long closed = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM biz_event WHERE status = 'CLOSED'", Long.class);
        Long pendingAudit = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM biz_event WHERE status IN ('PENDING_AUDIT', 'IN_AUDIT')", Long.class);

        Map<String, Long> events = new HashMap<>();
        events.put("total", totalEvents != null ? totalEvents : 0);
        events.put("waitingDispatch", waitingDispatch != null ? waitingDispatch : 0);
        events.put("dispatched", dispatched != null ? dispatched : 0);
        events.put("closed", closed != null ? closed : 0);
        events.put("pendingAudit", pendingAudit != null ? pendingAudit : 0);
        result.put("events", events);

        // 工单统计
        Long totalOrders = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM biz_work_order", Long.class);
        Long processingOrders = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM biz_work_order WHERE status = 'PROCESSING'", Long.class);
        Long completedOrders = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM biz_work_order WHERE status = 'COMPLETED'", Long.class);

        Map<String, Long> orders = new HashMap<>();
        orders.put("total", totalOrders != null ? totalOrders : 0);
        orders.put("processing", processingOrders != null ? processingOrders : 0);
        orders.put("completed", completedOrders != null ? completedOrders : 0);
        result.put("orders", orders);

        // 紧急程度分布
        List<Map<String, Object>> urgencyDist = jdbcTemplate.queryForList(
                "SELECT urgency_level as level, COUNT(*) as count FROM biz_event GROUP BY urgency_level");
        result.put("urgencyDistribution", urgencyDist);

        // 事件类型分布
        List<Map<String, Object>> typeDist = jdbcTemplate.queryForList(
                "SELECT event_type as type, COUNT(*) as count FROM biz_event GROUP BY event_type ORDER BY count DESC LIMIT 10");
        result.put("eventTypeDistribution", typeDist);

        // 网格事件排名（以二级网格为基准；未关联网格的历史迁移事件归入"未分配网格"）
        List<Map<String, Object>> gridRanking = jdbcTemplate.queryForList(
                "SELECT g.grid_name as gridName, " +
                "COUNT(e.id) as eventCount, " +
                "SUM(CASE WHEN e.status = 'CLOSED' THEN 1 ELSE 0 END) as closedCount, " +
                "COALESCE(ROUND(SUM(CASE WHEN e.status = 'CLOSED' THEN 1 ELSE 0 END) * 100.0 / NULLIF(COUNT(e.id), 0), 1), 0) as completionRate " +
                "FROM cmn_grid g LEFT JOIN biz_event e ON e.grid_id = g.id " +
                "WHERE g.status = 'ACTIVE' AND g.grid_level = 2 " +
                "GROUP BY g.id, g.grid_name ORDER BY eventCount DESC");
        Map<String, Object> unassigned = jdbcTemplate.queryForMap(
                "SELECT '未分配网格' as gridName, " +
                "COUNT(*) as eventCount, " +
                "SUM(CASE WHEN status = 'CLOSED' THEN 1 ELSE 0 END) as closedCount, " +
                "ROUND(SUM(CASE WHEN status = 'CLOSED' THEN 1 ELSE 0 END) * 100.0 / NULLIF(COUNT(*), 0), 1) as completionRate " +
                "FROM biz_event WHERE grid_id IS NULL OR grid_id NOT IN (SELECT id FROM cmn_grid)");
        if (((Number) unassigned.get("eventCount")).longValue() > 0) {
            gridRanking.add(unassigned);
        }
        result.put("gridRanking", gridRanking);

        // 按月统计
        List<Map<String, Object>> monthlyStats = jdbcTemplate.queryForList(
                "SELECT DATE_FORMAT(created_at, '%Y-%m') as month, COUNT(*) as count FROM biz_event GROUP BY DATE_FORMAT(created_at, '%Y-%m') ORDER BY month DESC LIMIT 12");
        result.put("monthlyStats", monthlyStats);

        return ApiResponse.ok(result);
    }

    /**
     * 网格员效能考核
     */
    @GetMapping("/worker-performance")
    public ApiResponse<List<Map<String, Object>>> workerPerformance() {
        requireAssessmentPermission();
        List<Map<String, Object>> result = jdbcTemplate.queryForList(
                "SELECT " +
                "  u.id as userId, " +
                "  u.real_name as realName, " +
                "  u.username, " +
                "  COUNT(DISTINCT wo.id) as totalOrders, " +
                "  SUM(CASE WHEN wo.status = 'COMPLETED' THEN 1 ELSE 0 END) as completedOrders, " +
                "  SUM(CASE WHEN wo.status = 'PROCESSING' THEN 1 ELSE 0 END) as processingOrders, " +
                "  ROUND(SUM(CASE WHEN wo.status = 'COMPLETED' THEN 1 ELSE 0 END) * 100.0 / NULLIF(COUNT(DISTINCT wo.id), 0), 1) as completionRate " +
                "FROM sys_user u " +
                "LEFT JOIN biz_work_order wo ON wo.assignee_user_id = u.id " +
                "WHERE u.status = 'ACTIVE' " +
                "GROUP BY u.id, u.real_name, u.username " +
                "ORDER BY completedOrders DESC");
        return ApiResponse.ok(result);
    }

    /**
     * 事件处置时效统计
     */
    @GetMapping("/response-time")
    public ApiResponse<Map<String, Object>> responseTime() {
        requireAssessmentPermission();
        Map<String, Object> result = new HashMap<>();

        // 平均处置时间（从事件创建到关闭的小时数）
        Double avgHours = jdbcTemplate.queryForObject(
                "SELECT AVG(TIMESTAMPDIFF(HOUR, e.created_at, er.created_at)) " +
                "FROM biz_event e JOIN biz_event_record er ON er.event_id = e.id " +
                "WHERE er.to_status = 'CLOSED' AND e.status = 'CLOSED'", Double.class);
        result.put("avgResolutionHours", avgHours != null ? Math.round(avgHours * 10.0) / 10.0 : 0);

        // 超期未处置数量（超过24小时未派单）
        Long overdueDispatch = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM biz_event WHERE status = 'WAITING_DISPATCH' AND TIMESTAMPDIFF(HOUR, created_at, NOW()) > 24", Long.class);
        result.put("overdueDispatch", overdueDispatch != null ? overdueDispatch : 0);

        // 超期未处置数量（超过48小时未关闭）
        Long overdueClose = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM biz_event WHERE status NOT IN ('CLOSED', 'IGNORED') AND TIMESTAMPDIFF(HOUR, created_at, NOW()) > 48", Long.class);
        result.put("overdueClose", overdueClose != null ? overdueClose : 0);

        return ApiResponse.ok(result);
    }

    /**
     * 群众评价统计
     */
    @GetMapping("/rating-stats")
    public ApiResponse<Map<String, Object>> ratingStats() {
        requireAssessmentPermission();
        Map<String, Object> result = new HashMap<>();

        // 平均评分
        Double avgRating = jdbcTemplate.queryForObject(
                "SELECT AVG(rating) FROM biz_event WHERE rating IS NOT NULL", Double.class);
        result.put("avgRating", avgRating != null ? Math.round(avgRating * 10.0) / 10.0 : 0);

        // 已评价数量
        Long totalRated = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM biz_event WHERE rating IS NOT NULL", Long.class);
        result.put("totalRated", totalRated != null ? totalRated : 0);

        // 满意率（4-5星占比）
        Long satisfied = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM biz_event WHERE rating >= 4", Long.class);
        Long totalWithRating = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM biz_event WHERE rating IS NOT NULL", Long.class);
        if (totalWithRating != null && totalWithRating > 0) {
            result.put("satisfactionRate", Math.round((satisfied != null ? satisfied : 0) * 100.0 / totalWithRating) + "%");
        } else {
            result.put("satisfactionRate", "0%");
        }

        return ApiResponse.ok(result);
    }

    /**
     * 月度治理月报：高频问题、高发网格、关键指标自动汇总
     */
    @GetMapping("/monthly-report")
    public ApiResponse<Map<String, Object>> monthlyReport() {
        requireAssessmentPermission();
        Map<String, Object> result = new HashMap<>();

        // 本月高频问题 TOP5
        List<Map<String, Object>> topIssues = jdbcTemplate.queryForList(
                "SELECT event_type as type, COUNT(*) as count FROM biz_event " +
                "WHERE created_at >= DATE_FORMAT(NOW(), '%Y-%m-01') " +
                "GROUP BY event_type ORDER BY count DESC LIMIT 5");
        result.put("topIssues", topIssues);

        // 本月高发网格 TOP5
        List<Map<String, Object>> topGrids = jdbcTemplate.queryForList(
                "SELECT g.grid_name as gridName, COUNT(e.id) as count FROM biz_event e " +
                "LEFT JOIN cmn_grid g ON g.id = e.grid_id " +
                "WHERE e.created_at >= DATE_FORMAT(NOW(), '%Y-%m-01') " +
                "GROUP BY g.grid_name ORDER BY count DESC LIMIT 5");
        result.put("topGrids", topGrids);

        // 本月关键指标
        Long monthTotal = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM biz_event WHERE created_at >= DATE_FORMAT(NOW(), '%Y-%m-01')", Long.class);
        Long monthClosed = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM biz_event WHERE status = 'CLOSED' AND created_at >= DATE_FORMAT(NOW(), '%Y-%m-01')", Long.class);
        Double monthAvgRating = jdbcTemplate.queryForObject(
                "SELECT AVG(rating) FROM biz_event WHERE rating IS NOT NULL AND created_at >= DATE_FORMAT(NOW(), '%Y-%m-01')", Double.class);
        result.put("monthTotal", monthTotal != null ? monthTotal : 0);
        result.put("monthClosed", monthClosed != null ? monthClosed : 0);
        result.put("monthCompletionRate", monthTotal != null && monthTotal > 0
                ? Math.round((monthClosed != null ? monthClosed : 0) * 100.0 / monthTotal) + "%" : "0%");
        result.put("monthAvgRating", monthAvgRating != null ? Math.round(monthAvgRating * 10.0) / 10.0 : 0);

        return ApiResponse.ok(result);
    }

    /**
     * 趋势预判预警：近7天同网格+同类事件≥3次 → 反复投诉/隐患预警
     */
    @GetMapping("/trend-alert")
    public ApiResponse<List<Map<String, Object>>> trendAlert() {
        requireAssessmentPermission();
        List<Map<String, Object>> alerts = jdbcTemplate.queryForList(
                "SELECT e.event_type as type, g.grid_name as gridName, COUNT(*) as count, " +
                "MAX(e.created_at) as latestTime, GROUP_CONCAT(DISTINCT e.title SEPARATOR ' | ') as sampleTitles " +
                "FROM biz_event e LEFT JOIN cmn_grid g ON g.id = e.grid_id " +
                "WHERE e.created_at >= DATE_SUB(NOW(), INTERVAL 7 DAY) " +
                "AND e.status NOT IN ('CLOSED', 'IGNORED') " +
                "GROUP BY e.event_type, g.grid_name HAVING COUNT(*) >= 3 " +
                "ORDER BY count DESC LIMIT 20");
        return ApiResponse.ok(alerts);
    }

    /**
     * 巡查覆盖率：已巡网格数 / 总网格数
     */
    @GetMapping("/patrol-coverage")
    public ApiResponse<Map<String, Object>> patrolCoverage() {
        requireAssessmentPermission();
        Map<String, Object> result = new HashMap<>();

        Long totalGrids = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM cmn_grid WHERE status = 'ACTIVE'", Long.class);
        // 本月有巡查记录的网格数
        Long patrolledGrids = jdbcTemplate.queryForObject(
                "SELECT COUNT(DISTINCT grid_id) FROM cmn_patrol_record " +
                "WHERE created_at >= DATE_FORMAT(NOW(), '%Y-%m-01')", Long.class);
        // 本月有巡查任务的网格数（含未巡）
        Long totalPatrolTasks = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM biz_patrol_task WHERE created_at >= DATE_FORMAT(NOW(), '%Y-%m-01')", Long.class);
        long tg = totalGrids != null ? totalGrids : 0;
        long pg = patrolledGrids != null ? patrolledGrids : 0;
        result.put("totalGrids", tg);
        result.put("patrolledGrids", pg);
        result.put("coverageRate", tg > 0 ? Math.round(pg * 100.0 / tg) + "%" : "0%");
        result.put("totalPatrolTasks", totalPatrolTasks != null ? totalPatrolTasks : 0);

        return ApiResponse.ok(result);
    }

    /**
     * 隐患整改率：已整改 / 总隐患（基于安全巡查）
     */
    @GetMapping("/rectification-rate")
    public ApiResponse<Map<String, Object>> rectificationRate() {
        requireAssessmentPermission();
        Map<String, Object> result = new HashMap<>();

        Long totalInspections = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM biz_safety_inspection", Long.class);
        // 已整改 = safety_status 为 RECTIFIED 或类似状态
        Long rectified = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM biz_safety_inspection WHERE safety_status IN ('RECTIFIED','VERIFIED','CLOSED')", Long.class);
        // 待整改
        Long pending = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM biz_safety_inspection WHERE safety_status IN ('PENDING','OVERDUE','UNRECTIFIED')", Long.class);
        long ti = totalInspections != null ? totalInspections : 0;
        long rf = rectified != null ? rectified : 0;
        result.put("totalInspections", ti);
        result.put("rectified", rf);
        result.put("pending", pending != null ? pending : 0);
        result.put("rectificationRate", ti > 0 ? Math.round(rf * 100.0 / ti) + "%" : "0%");

        // 按整改状态分布
        List<Map<String, Object>> statusDist = jdbcTemplate.queryForList(
                "SELECT safety_status as status, COUNT(*) as count FROM biz_safety_inspection GROUP BY safety_status");
        result.put("statusDistribution", statusDist);

        return ApiResponse.ok(result);
    }

    private void requireAssessmentPermission() {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.API_ASSESSMENT_VIEW);
    }
}
