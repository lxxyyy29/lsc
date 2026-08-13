package com.changping.platform.modules.event.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 趋势预判/反复投诉自动预警（考核研判 A3）
 * 规则：
 *  - EVENT_TYPE 维度：同一网格内同类型事件 7 天内 ≥ 3 件 → 类型频发预警
 *  - ADDRESS 维度：同一地点（incident_address）7 天内 ≥ 2 次 → 反复投诉预警
 * 同维度已有未处理预警时只刷新统计，不重复创建、不重复通知。
 */
@Service
public class TrendAlertService {

    private static final Logger log = LoggerFactory.getLogger(TrendAlertService.class);

    /** 事件类型 → 中文名（与 Web 端 EVENT_TYPES 保持一致） */
    private static final Map<String, String> EVENT_TYPE_NAMES = new HashMap<>();

    static {
        EVENT_TYPE_NAMES.put("ROAD", "道路损坏");
        EVENT_TYPE_NAMES.put("LIGHT", "路灯故障");
        EVENT_TYPE_NAMES.put("PIPE", "管道破损");
        EVENT_TYPE_NAMES.put("FIRE_SAFETY", "消防安全");
        EVENT_TYPE_NAMES.put("ILLEGAL_BUILDING", "违章建筑");
        EVENT_TYPE_NAMES.put("PUBLIC_SAFETY", "公共安全");
        EVENT_TYPE_NAMES.put("DRONE_ALARM", "无人机告警");
        EVENT_TYPE_NAMES.put("COMPLAINT", "市民投诉");
        EVENT_TYPE_NAMES.put("REPAIR", "物业报修");
        EVENT_TYPE_NAMES.put("HEALTH", "卫生事件");
        EVENT_TYPE_NAMES.put("FIRE", "消防安全");
        EVENT_TYPE_NAMES.put("SAFETY", "安全生产");
        EVENT_TYPE_NAMES.put("SAFE", "安全生产");
        EVENT_TYPE_NAMES.put("ENVIRONMENT", "市容环境");
        EVENT_TYPE_NAMES.put("OTHER", "其他");
        EVENT_TYPE_NAMES.put("UNSPECIFIED", "未分类");
    }

    private static final int WINDOW_DAYS = 7;
    private static final int TYPE_THRESHOLD = 3;
    private static final int ADDRESS_THRESHOLD = 2;

    private final JdbcTemplate jdbcTemplate;
    private final com.changping.platform.modules.notification.service.NotificationService notificationService;

    public TrendAlertService(JdbcTemplate jdbcTemplate,
                             com.changping.platform.modules.notification.service.NotificationService notificationService) {
        this.jdbcTemplate = jdbcTemplate;
        this.notificationService = notificationService;
    }

    /** 事件类型中文名，未映射时回退原始值 */
    private String typeName(String eventType) {
        if (eventType == null || eventType.isBlank()) return "未分类";
        return EVENT_TYPE_NAMES.getOrDefault(eventType, EVENT_TYPE_NAMES.getOrDefault(eventType.toUpperCase(), eventType));
    }

    /**
     * 扫描事件库并生成/刷新趋势预警
     * @return 本次新生成的预警数量
     */
    @Transactional
    public int scan() {
        int created = 0;
        created += scanByEventType();
        created += scanByAddress();
        log.info("[TrendAlert] 扫描完成，新生成 {} 条预警", created);
        return created;
    }

    /** 同类型同网格 7 天内频发 */
    private int scanByEventType() {
        List<Map<String, Object>> groups = jdbcTemplate.queryForList(
            "SELECT event_type, grid_id, g.grid_name AS grid_name, COUNT(*) AS cnt, " +
            "MIN(e.created_at) AS first_at, MAX(e.created_at) AS last_at " +
            "FROM biz_event e LEFT JOIN cmn_grid g ON g.id = e.grid_id " +
            "WHERE e.created_at >= DATE_SUB(NOW(), INTERVAL " + WINDOW_DAYS + " DAY) " +
            "  AND e.archived = 0 " +
            "GROUP BY e.event_type, e.grid_id HAVING cnt >= " + TYPE_THRESHOLD);
        int created = 0;
        for (Map<String, Object> g : groups) {
            String eventType = (String) g.get("event_type");
            Number gridIdNum = (Number) g.get("grid_id");
            Long gridId = gridIdNum != null ? gridIdNum.longValue() : null;
            int cnt = ((Number) g.get("cnt")).intValue();
            String gridName = (String) g.get("grid_name");
            String typeCn = typeName(eventType);

            Long existingId = findOpen("EVENT_TYPE", eventType, gridId, null);
            if (existingId != null) {
                // 已存在未处理预警 → 刷新统计与时间窗口
                jdbcTemplate.update(
                    "UPDATE biz_trend_alert SET alert_count = ?, last_event_at = ?, window_end = NOW(), updated_at = NOW() WHERE id = ?",
                    cnt, g.get("last_at"), existingId);
                continue;
            }
            insertAlert("EVENT_TYPE", eventType, typeCn, gridId, gridName, null,
                    cnt, TYPE_THRESHOLD, g.get("first_at"), g.get("last_at"),
                    cnt >= 5 ? "URGENT" : "NORMAL",
                    "【" + typeCn + "】事件频发预警",
                    "近 " + WINDOW_DAYS + " 天内" + (gridName != null ? gridName + " " : "") + "「" + typeCn + "」类事件累计 " + cnt + " 起，呈高发趋势，请关注研判。");
            created++;
        }
        return created;
    }

    /** 同一地点反复投诉/上报 */
    private int scanByAddress() {
        List<Map<String, Object>> groups = jdbcTemplate.queryForList(
            "SELECT TRIM(incident_address) AS addr, COUNT(*) AS cnt, " +
            "MIN(created_at) AS first_at, MAX(created_at) AS last_at " +
            "FROM biz_event " +
            "WHERE incident_address IS NOT NULL AND TRIM(incident_address) != '' " +
            "  AND created_at >= DATE_SUB(NOW(), INTERVAL " + WINDOW_DAYS + " DAY) " +
            "  AND archived = 0 " +
            "GROUP BY TRIM(incident_address) HAVING cnt >= " + ADDRESS_THRESHOLD);
        int created = 0;
        for (Map<String, Object> g : groups) {
            String addr = (String) g.get("addr");
            int cnt = ((Number) g.get("cnt")).intValue();

            Long existingId = findOpen("ADDRESS", null, null, addr);
            if (existingId != null) {
                jdbcTemplate.update(
                    "UPDATE biz_trend_alert SET alert_count = ?, last_event_at = ?, window_end = NOW(), updated_at = NOW() WHERE id = ?",
                    cnt, g.get("last_at"), existingId);
                continue;
            }
            insertAlert("ADDRESS", null, "反复投诉", null, null, addr,
                    cnt, ADDRESS_THRESHOLD, g.get("first_at"), g.get("last_at"),
                    cnt >= 4 ? "URGENT" : "NORMAL",
                    "【" + addr + "】反复投诉预警",
                    "近 " + WINDOW_DAYS + " 天内「" + addr + "」被反复上报 " + cnt + " 次，存在同类问题反复发生，请核查处置。");
            created++;
        }
        return created;
    }

    private Long findOpen(String dimension, String eventType, Long gridId, String address) {
        List<Object> params = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
            "SELECT id FROM biz_trend_alert WHERE dimension = ? AND status = 'OPEN'");
        params.add(dimension);
        if (dimension.equals("EVENT_TYPE")) {
            sql.append(" AND event_type = ? AND ((grid_id IS NULL AND ? IS NULL) OR grid_id = ?)");
            params.add(eventType);
            params.add(gridId);
            params.add(gridId);
        } else {
            sql.append(" AND address = ?");
            params.add(address);
        }
        sql.append(" ORDER BY id DESC LIMIT 1");
        List<Long> ids = jdbcTemplate.queryForList(sql.toString(), Long.class, params.toArray());
        return ids.isEmpty() ? null : ids.get(0);
    }

    private void insertAlert(String dimension, String eventType, String typeCn, Long gridId, String gridName,
                             String address, int cnt, int threshold, Object firstAt, Object lastAt,
                             String level, String title, String content) {
        String alertNo = "TDA-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + "-" + (int) (Math.random() * 900 + 100);
        jdbcTemplate.update(
            "INSERT INTO biz_trend_alert (alert_no, dimension, event_type, event_type_name, grid_id, grid_name, address, " +
            "alert_count, threshold, window_start, window_end, first_event_at, last_event_at, level, status, title, content, created_at, updated_at) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, DATE_SUB(NOW(), INTERVAL " + WINDOW_DAYS + " DAY), NOW(), ?, ?, ?, 'OPEN', ?, ?, NOW(), NOW())",
            alertNo, dimension, eventType, typeCn, gridId, gridName, address,
            cnt, threshold, firstAt, lastAt, level, title, content);
        notifyDutyUsers(title, content);
    }

    /** 给事件专员/派单员角色用户发站内通知 */
    private void notifyDutyUsers(String title, String content) {
        try {
            List<Long> userIds = jdbcTemplate.queryForList(
                "SELECT DISTINCT u.id FROM sys_user u " +
                "JOIN sys_user_role ur ON ur.user_id = u.id " +
                "JOIN sys_role r ON r.id = ur.role_id " +
                "WHERE r.role_code IN ('EVENT_OPERATOR', 'DISPATCHER') AND u.status = 'ACTIVE'",
                Long.class);
            for (Long uid : userIds) {
                notificationService.createNotification(uid, title, content, "SYSTEM", "URGENT", "TREND_ALERT", null);
            }
        } catch (Exception e) {
            log.error("[TrendAlert] 发送预警通知异常: {}", e.getMessage());
        }
    }

    /** 分页查询预警列表 */
    public Map<String, Object> list(String status, String dimension, int page, int size) {
        List<Object> params = new ArrayList<>();
        StringBuilder where = new StringBuilder(" WHERE 1=1");
        if (status != null && !status.isBlank()) {
            where.append(" AND status = ?");
            params.add(status.trim());
        }
        if (dimension != null && !dimension.isBlank()) {
            where.append(" AND dimension = ?");
            params.add(dimension.trim());
        }
        Long total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM biz_trend_alert" + where, Long.class, params.toArray());
        List<Object> pageParams = new ArrayList<>(params);
        pageParams.add(size);
        pageParams.add((page - 1) * size);
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            "SELECT * FROM biz_trend_alert" + where + " ORDER BY status = 'OPEN' DESC, level = 'URGENT' DESC, id DESC LIMIT ? OFFSET ?",
            pageParams.toArray());
        Map<String, Object> result = new HashMap<>();
        result.put("total", total != null ? total : 0L);
        result.put("items", rows);
        return result;
    }

    /** 处理预警 */
    @Transactional
    public boolean handle(Long id, String remark, Long operatorId, String operatorName) {
        int updated = jdbcTemplate.update(
            "UPDATE biz_trend_alert SET status = 'HANDLED', handle_remark = ?, handled_at = NOW(), handler_id = ?, handler_name = ?, updated_at = NOW() WHERE id = ? AND status = 'OPEN'",
            remark, operatorId, operatorName, id);
        return updated > 0;
    }

    /** 预警统计（待处理/紧急/各维度） */
    public Map<String, Object> statistics() {
        Map<String, Object> result = new HashMap<>();
        Long open = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM biz_trend_alert WHERE status = 'OPEN'", Long.class);
        Long urgent = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM biz_trend_alert WHERE status = 'OPEN' AND level = 'URGENT'", Long.class);
        Long byType = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM biz_trend_alert WHERE status = 'OPEN' AND dimension = 'EVENT_TYPE'", Long.class);
        Long byAddress = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM biz_trend_alert WHERE status = 'OPEN' AND dimension = 'ADDRESS'", Long.class);
        Long total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM biz_trend_alert", Long.class);
        result.put("open", open != null ? open : 0L);
        result.put("urgent", urgent != null ? urgent : 0L);
        result.put("byType", byType != null ? byType : 0L);
        result.put("byAddress", byAddress != null ? byAddress : 0L);
        result.put("total", total != null ? total : 0L);
        return result;
    }
}
