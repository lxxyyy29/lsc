package com.changping.platform.modules.assessment.controller;

import com.changping.platform.common.response.ApiResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/assessment")
public class AssessmentController {

    private final JdbcTemplate jdbcTemplate;

    public AssessmentController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 总览统计
     */
    @GetMapping("/overview")
    public ApiResponse<Map<String, Object>> overview() {
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

        // 网格事件排名
        List<Map<String, Object>> gridRanking = jdbcTemplate.queryForList(
                "SELECT g.grid_name as gridName, COUNT(e.id) as eventCount FROM biz_event e LEFT JOIN cmn_grid g ON g.id = e.grid_id GROUP BY g.grid_name ORDER BY eventCount DESC LIMIT 10");
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
}
