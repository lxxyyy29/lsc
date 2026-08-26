package com.changping.platform.modules.report.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @Author lxy
 * @Description //区域报告服务，按区域聚合事件和工单的统计数据，支持日期范围和区域过滤
 * @Date 2026/04/18 10:00
 */
@Service
public class DistrictReportService {

    private final JdbcTemplate jdbcTemplate;

    /**
     * @Author lxy
     * @Description //构造器，注入 JdbcTemplate
     * @Date 2026/04/18 10:00
     * @Param [jdbcTemplate JDBC模板]
     * @return void
     */
    public DistrictReportService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * @Author lxy
     * @Description //按区域聚合事件和工单统计数据，支持按日期范围和区域过滤，返回各区域的汇总信息
     * @Date 2026/04/18 10:00
     * @Param [startDate 开始日期（可选）, endDate 结束日期（可选）, areaId 区域ID（可选）]
     * @return List<DistrictSummaryVo> 区域汇总统计列表
     */
    @Transactional(readOnly = true)
    public List<DistrictSummaryVo> getDistrictSummary(LocalDate startDate, LocalDate endDate, Long areaId) {
        List<Object> params = new ArrayList<>();
        StringBuilder where = new StringBuilder(" WHERE 1=1");

        if (startDate != null) {
            where.append(" AND (be.created_at IS NULL OR be.created_at >= ?)");
            params.add(java.sql.Date.valueOf(startDate));
        }
        if (endDate != null) {
            where.append(" AND (be.created_at IS NULL OR be.created_at < ?)");
            params.add(java.sql.Date.valueOf(endDate.plusDays(1)));
        }
        if (areaId != null) {
            where.append(" AND ba.id = ?");
            params.add(areaId);
        }

        String sql = "SELECT"
                + "  ba.id AS area_id,"
                + "  ba.area_name,"
                + "  COUNT(DISTINCT be.id) AS total_events,"
                + "  COUNT(DISTINCT CASE WHEN be.status IN ('PENDING_AUDIT','IN_AUDIT','AUDIT_APPROVED','AUDIT_REJECTED') THEN be.id END) AS pending_events,"
                + "  COUNT(DISTINCT CASE WHEN be.status = 'WAITING_DISPATCH' THEN be.id END) AS waiting_dispatch_events,"
                + "  COUNT(DISTINCT CASE WHEN be.status = 'DISPATCHED_TO_WORK_ORDER' THEN be.id END) AS processing_events,"
                + "  COUNT(DISTINCT CASE WHEN be.status = 'CLOSED' THEN be.id END) AS closed_events,"
                + "  COUNT(DISTINCT CASE WHEN be.status = 'IGNORED' THEN be.id END) AS ignored_events,"
                + "  COUNT(DISTINCT bwo.id) AS total_work_orders,"
                + "  COUNT(DISTINCT CASE WHEN bwo.status IN ('COMPLETED','CLOSED') THEN bwo.id END) AS completed_work_orders,"
                + "  AVG(CASE WHEN bwo.status IN ('COMPLETED','CLOSED') AND bwo.completed_at IS NOT NULL"
                + "       THEN TIMESTAMPDIFF(MINUTE, bwo.created_at, bwo.completed_at) / 60.0"
                + "       ELSE NULL END) AS avg_completion_hours"
                + " FROM biz_area ba"
                + " LEFT JOIN biz_event be ON be.area_id = ba.id"
                + " LEFT JOIN biz_work_order bwo ON bwo.source_event_id = be.id"
                + where
                + " GROUP BY ba.id, ba.area_name"
                + " ORDER BY ba.id ASC";

        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            BigDecimal avgHoursRaw = rs.getBigDecimal("avg_completion_hours");
            Double avgCompletionHours = avgHoursRaw == null ? null
                    : avgHoursRaw.setScale(2, RoundingMode.HALF_UP).doubleValue();
            return new DistrictSummaryVo(
                    rs.getLong("area_id"),
                    rs.getString("area_name"),
                    rs.getLong("total_events"),
                    rs.getLong("pending_events"),
                    rs.getLong("waiting_dispatch_events"),
                    rs.getLong("processing_events"),
                    rs.getLong("closed_events"),
                    rs.getLong("ignored_events"),
                    rs.getLong("total_work_orders"),
                    rs.getLong("completed_work_orders"),
                    avgCompletionHours);
        }, params.toArray());
    }
}
