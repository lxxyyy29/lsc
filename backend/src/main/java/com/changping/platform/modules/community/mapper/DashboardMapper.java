package com.changping.platform.modules.community.mapper;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class DashboardMapper {

    private final JdbcTemplate jdbcTemplate;

    public DashboardMapper(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Map<String, Object> getOverview() {
        Map<String, Object> result = new HashMap<>();

        // 网格统计
        Long gridCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM cmn_grid WHERE status = 'ACTIVE'", Long.class);
        result.put("gridCount", gridCount != null ? gridCount : 0);

        // 人口统计
        Long populationTotal = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM cmn_population WHERE status = 'ACTIVE'", Long.class);
        result.put("populationTotal", populationTotal != null ? populationTotal : 0);

        // 房屋统计
        Long buildingCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM cmn_building WHERE status = 'ACTIVE'", Long.class);
        result.put("buildingCount", buildingCount != null ? buildingCount : 0);

        // 场所统计
        Long placeCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM cmn_place WHERE status = 'ACTIVE'", Long.class);
        result.put("placeCount", placeCount != null ? placeCount : 0);

        // 组织力量统计
        Long orgMemberCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM cmn_org_member WHERE status = 'ACTIVE'", Long.class);
        result.put("orgMemberCount", orgMemberCount != null ? orgMemberCount : 0);

        // 商户统计（真实台账数据）
        Long merchantCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM biz_merchant", Long.class);
        result.put("merchantCount", merchantCount != null ? merchantCount : 0);

        // 场所台账分类统计
        Long rentalHouseCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM cmn_place_ledger WHERE place_category = 'RENTAL_HOUSE'", Long.class);
        Long smallShopCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM cmn_place_ledger WHERE place_category IN ('SMALL_SHOP','SMALL_WORKSHOP')", Long.class);
        Long otherPlaceCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM cmn_place_ledger WHERE place_category NOT IN ('RENTAL_HOUSE','SMALL_SHOP','SMALL_WORKSHOP')", Long.class);
        result.put("rentalHouseCount", rentalHouseCount != null ? rentalHouseCount : 0);
        result.put("smallShopCount", smallShopCount != null ? smallShopCount : 0);
        result.put("otherPlaceCount", otherPlaceCount != null ? otherPlaceCount : 0);

        // 事件统计
        Long eventTotal = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM biz_event", Long.class);
        result.put("eventTotal", eventTotal != null ? eventTotal : 0);

        Long eventPending = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM biz_event WHERE status NOT IN ('CLOSED', 'COMPLETED')", Long.class);
        result.put("eventPending", eventPending != null ? eventPending : 0);

        // 三色分级统计
        Long eventGreen = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM biz_event WHERE urgency_level = 'GREEN'", Long.class);
        Long eventYellow = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM biz_event WHERE urgency_level = 'YELLOW'", Long.class);
        Long eventRed = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM biz_event WHERE urgency_level = 'RED'", Long.class);
        result.put("eventGreen", eventGreen != null ? eventGreen : 0);
        result.put("eventYellow", eventYellow != null ? eventYellow : 0);
        result.put("eventRed", eventRed != null ? eventRed : 0);

        // 巡查统计
        Long patrolCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM cmn_patrol_record", Long.class);
        result.put("patrolCount", patrolCount != null ? patrolCount : 0);

        // 居民上报统计
        Long reportCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM cmn_resident_report", Long.class);
        result.put("reportCount", reportCount != null ? reportCount : 0);

        Long reportPending = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM cmn_resident_report WHERE status = 'PENDING'", Long.class);
        result.put("reportPending", reportPending != null ? reportPending : 0);

        return result;
    }

    /**
     * 大屏汇总数据 — 一次性返回所有指标
     */
    public Map<String, Object> getBigScreenData() {
        Map<String, Object> result = new HashMap<>();

        // === 核心指标卡片 ===
        Long eventTotal = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM biz_event", Long.class);
        Long eventToday = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM biz_event WHERE DATE(created_at) = CURDATE()", Long.class);
        Long eventPending = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM biz_event WHERE status NOT IN ('CLOSED')", Long.class);
        Long eventClosed = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM biz_event WHERE status = 'CLOSED'", Long.class);
        Long workOrderTotal = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM biz_work_order", Long.class);
        Long workOrderProcessing = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM biz_work_order WHERE status IN ('WAITING_ACCEPT','PROCESSING')", Long.class);

        Map<String, Object> kpis = new HashMap<>();
        kpis.put("eventTotal", eventTotal != null ? eventTotal : 0);
        kpis.put("eventToday", eventToday != null ? eventToday : 0);
        kpis.put("eventPending", eventPending != null ? eventPending : 0);
        kpis.put("eventClosed", eventClosed != null ? eventClosed : 0);
        kpis.put("workOrderTotal", workOrderTotal != null ? workOrderTotal : 0);
        kpis.put("workOrderProcessing", workOrderProcessing != null ? workOrderProcessing : 0);
        result.put("kpis", kpis);

        // === 紧急程度分布 ===
        List<Map<String, Object>> urgencyDist = jdbcTemplate.queryForList(
            "SELECT urgency_level AS level, COUNT(id) AS count FROM biz_event GROUP BY urgency_level");
        result.put("urgencyDist", urgencyDist);

        // === 事件类型分布 ===
        List<Map<String, Object>> eventTypeDist = jdbcTemplate.queryForList(
            "SELECT event_type AS type, COUNT(id) AS count FROM biz_event GROUP BY event_type ORDER BY count DESC LIMIT 8");
        result.put("eventTypeDist", eventTypeDist);

        // === 近 7 天趋势 ===
        List<Map<String, Object>> weeklyTrend = jdbcTemplate.queryForList(
            "SELECT DATE(created_at) AS date, COUNT(id) AS count FROM biz_event " +
            "WHERE created_at >= DATE_SUB(CURDATE(), INTERVAL 7 DAY) " +
            "GROUP BY DATE(created_at) ORDER BY date ASC");
        result.put("weeklyTrend", weeklyTrend);

        // === 各网格事件排名 ===
        List<Map<String, Object>> gridRanking = jdbcTemplate.queryForList(
            "SELECT g.grid_name AS gridName, COUNT(e.id) AS eventCount " +
            "FROM cmn_grid g LEFT JOIN biz_event e ON g.id = e.grid_id " +
            "WHERE g.status = 'ACTIVE' AND g.grid_level = 2 " +
            "GROUP BY g.id, g.grid_name ORDER BY eventCount DESC LIMIT 10");
        result.put("gridRanking", gridRanking);

        // === 最新工单 ===
        List<Map<String, Object>> recentWorkOrders = jdbcTemplate.queryForList(
            "SELECT wo.work_order_no AS workOrderNo, wo.status, wo.assignee_name AS assigneeName, " +
            "wo.created_at AS createdAt FROM biz_work_order wo ORDER BY wo.created_at DESC LIMIT 10");
        result.put("recentWorkOrders", recentWorkOrders);

        return result;
    }

    /**
     * 月度汇总报表
     */
    public Map<String, Object> getMonthlySummary(String year, String month) {
        Map<String, Object> result = new HashMap<>();

        String startDate = year + "-" + month + "-01";
        String endDate = year + "-" + month + "-31";

        // 本月事件数
        Long eventCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM biz_event WHERE created_at >= ? AND created_at <= ?",
            Long.class, startDate, endDate);
        result.put("eventCount", eventCount != null ? eventCount : 0);

        // 本月工单数
        Long workOrderCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM biz_work_order WHERE created_at >= ? AND created_at <= ?",
            Long.class, startDate, endDate);
        result.put("workOrderCount", workOrderCount != null ? workOrderCount : 0);

        // 本月完成工单
        Long completedCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM biz_work_order WHERE status = 'COMPLETED' AND completed_at >= ? AND completed_at <= ?",
            Long.class, startDate, endDate);
        result.put("completedCount", completedCount != null ? completedCount : 0);

        result.put("year", year);
        result.put("month", month);
        return result;
    }

    /**
     * 网格处置排名
     */
    public Map<String, Object> getGridRanking(String period) {
        Map<String, Object> result = new HashMap<>();
        String timeFilter = "month".equals(period)
            ? "created_at >= DATE_SUB(NOW(), INTERVAL 30 DAY)"
            : "created_at >= DATE_SUB(NOW(), INTERVAL 7 DAY)";

        List<Map<String, Object>> rankings = jdbcTemplate.queryForList(
            "SELECT g.grid_name AS gridName, " +
            "COUNT(e.id) AS eventCount, " +
            "SUM(CASE WHEN e.status = 'CLOSED' THEN 1 ELSE 0 END) AS closedCount, " +
            "ROUND(SUM(CASE WHEN e.status = 'CLOSED' THEN 1 ELSE 0 END) * 100.0 / NULLIF(COUNT(e.id), 0), 1) AS closeRate " +
            "FROM cmn_grid g LEFT JOIN biz_event e ON g.id = e.grid_id AND " + timeFilter + " " +
            "WHERE g.status = 'ACTIVE' AND g.grid_level = 2 " +
            "GROUP BY g.id, g.grid_name ORDER BY closeRate DESC");
        result.put("period", period);
        result.put("rankings", rankings);
        return result;
    }

    public Map<String, Object> getGridStats() {
        Map<String, Object> result = new HashMap<>();

        // 各网格人口排名
        List<Map<String, Object>> populationRanking = jdbcTemplate.queryForList(
            "SELECT g.grid_name AS gridName, COUNT(p.id) AS populationCount " +
            "FROM cmn_grid g LEFT JOIN cmn_population p ON g.id = p.grid_id AND p.status = 'ACTIVE' " +
            "WHERE g.status = 'ACTIVE' AND g.grid_level = 2 " +
            "GROUP BY g.id, g.grid_name ORDER BY populationCount DESC");
        result.put("populationRanking", populationRanking);

        // 各网格事件统计
        List<Map<String, Object>> eventStats = jdbcTemplate.queryForList(
            "SELECT g.grid_name AS gridName, COUNT(e.id) AS eventCount " +
            "FROM cmn_grid g LEFT JOIN biz_event e ON g.id = e.grid_id " +
            "WHERE g.status = 'ACTIVE' AND g.grid_level = 2 " +
            "GROUP BY g.id, g.grid_name ORDER BY eventCount DESC");
        result.put("eventStats", eventStats);

        // 高频问题类型
        List<Map<String, Object>> hotIssues = jdbcTemplate.queryForList(
            "SELECT report_type AS reportType, COUNT(id) AS count " +
            "FROM cmn_resident_report GROUP BY report_type ORDER BY count DESC LIMIT 5");
        result.put("hotIssues", hotIssues);

        return result;
    }
}
