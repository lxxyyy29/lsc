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
