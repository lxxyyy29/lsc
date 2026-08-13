package com.changping.platform.modules.safety.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * 卫生防疫/爱卫 — 蚊媒孳生地三色分级 + 消杀记录 + 重点场所卫生监测（C4）
 * 三色分级：RED=红(紧急险情，需立即消杀) / YELLOW=黄(重点) / GREEN=绿(一般)
 */
@Service
public class MosquitoService {

    private static final Logger log = LoggerFactory.getLogger(MosquitoService.class);

    private final JdbcTemplate jdbcTemplate;

    private final Random random = new Random();

    public MosquitoService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // ==================== 蚊媒孳生地 ====================

    public Map<String, Object> listSites(String status, String level, int page, int size) {
        List<Object> params = new ArrayList<>();
        StringBuilder where = new StringBuilder(" WHERE 1=1");
        if (status != null && !status.isBlank()) {
            where.append(" AND status = ?");
            params.add(status.trim());
        }
        if (level != null && !level.isBlank()) {
            where.append(" AND risk_level = ?");
            params.add(level.trim());
        }
        Long total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM biz_mosquito_site" + where, Long.class, params.toArray());
        List<Object> pageParams = new ArrayList<>(params);
        pageParams.add(size);
        pageParams.add((page - 1) * size);
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            "SELECT * FROM biz_mosquito_site" + where + " ORDER BY FIELD(risk_level,'RED','YELLOW','GREEN'), id DESC LIMIT ? OFFSET ?",
            pageParams.toArray());
        Map<String, Object> result = new HashMap<>();
        result.put("total", total != null ? total : 0L);
        result.put("items", rows);
        return result;
    }

    /** 孳生地统计：红黄绿数量 / 在管 / 已消除 / 本月消杀 */
    public Map<String, Object> siteStatistics() {
        Map<String, Object> result = new HashMap<>();
        result.put("red", count("SELECT COUNT(*) FROM biz_mosquito_site WHERE risk_level = 'RED' AND status = 'ACTIVE'"));
        result.put("yellow", count("SELECT COUNT(*) FROM biz_mosquito_site WHERE risk_level = 'YELLOW' AND status = 'ACTIVE'"));
        result.put("green", count("SELECT COUNT(*) FROM biz_mosquito_site WHERE risk_level = 'GREEN' AND status = 'ACTIVE'"));
        result.put("active", count("SELECT COUNT(*) FROM biz_mosquito_site WHERE status = 'ACTIVE'"));
        result.put("eliminated", count("SELECT COUNT(*) FROM biz_mosquito_site WHERE status = 'ELIMINATED'"));
        result.put("monthDisinfection", count("SELECT COUNT(*) FROM biz_disinfection_record WHERE operator_date >= DATE_FORMAT(NOW(), '%Y-%m-01')"));
        return result;
    }

    @Transactional
    public Long createSite(Map<String, Object> body) {
        String siteNo = "MS-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                + "-" + String.format("%04d", (int) (Math.random() * 9000 + 1000));
        jdbcTemplate.update(
            "INSERT INTO biz_mosquito_site (site_no, site_name, site_type, address, grid_id, grid_name, risk_level, " +
            "longitude, latitude, owner_name, owner_phone, status, last_check_at, remark, created_at, updated_at) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'ACTIVE', NOW(), ?, NOW(), NOW())",
            siteNo,
            str(body, "siteName"),
            str(body, "siteType", "OTHER"),
            str(body, "address"),
            num(body, "gridId"),
            str(body, "gridName"),
            str(body, "riskLevel", "GREEN"),
            dec(body, "longitude"),
            dec(body, "latitude"),
            str(body, "ownerName"),
            str(body, "ownerPhone"),
            str(body, "remark"));
        return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    @Transactional
    public boolean updateSite(Long id, Map<String, Object> body) {
        return jdbcTemplate.update(
            "UPDATE biz_mosquito_site SET site_name = ?, site_type = ?, address = ?, grid_id = ?, grid_name = ?, " +
            "risk_level = ?, longitude = ?, latitude = ?, owner_name = ?, owner_phone = ?, remark = ?, " +
            "last_check_at = IF(? IS NULL, last_check_at, NOW()), updated_at = NOW() WHERE id = ?",
            str(body, "siteName"),
            str(body, "siteType", "OTHER"),
            str(body, "address"),
            num(body, "gridId"),
            str(body, "gridName"),
            str(body, "riskLevel", "GREEN"),
            dec(body, "longitude"),
            dec(body, "latitude"),
            str(body, "ownerName"),
            str(body, "ownerPhone"),
            str(body, "remark"),
            body.get("checkNow") != null ? 1 : null,
            id) > 0;
    }

    /** 标记消除（消杀完成并确认消除） */
    @Transactional
    public boolean eliminateSite(Long id) {
        return jdbcTemplate.update(
            "UPDATE biz_mosquito_site SET status = 'ELIMINATED', last_check_at = NOW(), updated_at = NOW() WHERE id = ?",
            id) > 0;
    }

    @Transactional
    public boolean deleteSite(Long id) {
        return jdbcTemplate.update("DELETE FROM biz_mosquito_site WHERE id = ?", id) > 0;
    }

    // ==================== 消杀记录 ====================

    public Map<String, Object> listDisinfections(Long siteId, int page, int size) {
        List<Object> params = new ArrayList<>();
        StringBuilder where = new StringBuilder(" WHERE 1=1");
        if (siteId != null) {
            where.append(" AND site_id = ?");
            params.add(siteId);
        }
        Long total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM biz_disinfection_record" + where, Long.class, params.toArray());
        List<Object> pageParams = new ArrayList<>(params);
        pageParams.add(size);
        pageParams.add((page - 1) * size);
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            "SELECT * FROM biz_disinfection_record" + where + " ORDER BY operator_date DESC, id DESC LIMIT ? OFFSET ?",
            pageParams.toArray());
        Map<String, Object> result = new HashMap<>();
        result.put("total", total != null ? total : 0L);
        result.put("items", rows);
        return result;
    }

    @Transactional
    public Long createDisinfection(Map<String, Object> body) {
        String recordNo = "DS-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                + "-" + String.format("%04d", (int) (Math.random() * 9000 + 1000));
        jdbcTemplate.update(
            "INSERT INTO biz_disinfection_record (record_no, site_id, site_name, disinfection_type, disinfectant, " +
            "operator_name, operator_date, area_sqm, result, remark, created_at, updated_at) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())",
            recordNo,
            num(body, "siteId"),
            str(body, "siteName"),
            str(body, "disinfectionType", "CHEMICAL"),
            str(body, "disinfectant"),
            str(body, "operatorName"),
            str(body, "operatorDate", LocalDate.now().toString()),
            dec(body, "areaSqm"),
            str(body, "result", "GOOD"),
            str(body, "remark"));
        // 关联孳生地：同步刷新最近检查时间
        Object siteId = body.get("siteId");
        if (siteId instanceof Number) {
            jdbcTemplate.update("UPDATE biz_mosquito_site SET last_check_at = NOW(), updated_at = NOW() WHERE id = ?",
                    ((Number) siteId).longValue());
        }
        return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    @Transactional
    public boolean deleteDisinfection(Long id) {
        return jdbcTemplate.update("DELETE FROM biz_disinfection_record WHERE id = ?", id) > 0;
    }

    // ==================== 重点场所卫生监测 ====================

    public Map<String, Object> listMonitors(int page, int size) {
        Long total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM biz_health_monitor", Long.class);
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            "SELECT * FROM biz_health_monitor ORDER BY monitor_date DESC, id DESC LIMIT ? OFFSET ?",
            size, (page - 1) * size);
        Map<String, Object> result = new HashMap<>();
        result.put("total", total != null ? total : 0L);
        result.put("items", rows);
        return result;
    }

    /** 卫生监测统计：按风险等级 + 平均分 */
    public Map<String, Object> monitorStatistics() {
        Map<String, Object> result = new HashMap<>();
        result.put("red", count("SELECT COUNT(*) FROM biz_health_monitor WHERE risk_level = 'RED'"));
        result.put("yellow", count("SELECT COUNT(*) FROM biz_health_monitor WHERE risk_level = 'YELLOW'"));
        result.put("green", count("SELECT COUNT(*) FROM biz_health_monitor WHERE risk_level = 'GREEN'"));
        Double avg = jdbcTemplate.queryForObject("SELECT AVG(score) FROM biz_health_monitor", Double.class);
        result.put("avgScore", avg != null ? Math.round(avg * 10) / 10.0 : 0);
        result.put("total", count("SELECT COUNT(*) FROM biz_health_monitor"));
        return result;
    }

    @Transactional
    public Long createMonitor(Map<String, Object> body) {
        jdbcTemplate.update(
            "INSERT INTO biz_health_monitor (place_name, place_type, address, grid_id, grid_name, monitor_item, " +
            "score, risk_level, monitor_date, monitor_org, remark, created_at, updated_at) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())",
            str(body, "placeName"),
            str(body, "placeType", "OTHER"),
            str(body, "address"),
            num(body, "gridId"),
            str(body, "gridName"),
            str(body, "monitorItem"),
            intVal(body, "score", 100),
            str(body, "riskLevel", "GREEN"),
            str(body, "monitorDate", LocalDate.now().toString()),
            str(body, "monitorOrg"),
            str(body, "remark"));
        return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    @Transactional
    public boolean updateMonitor(Long id, Map<String, Object> body) {
        return jdbcTemplate.update(
            "UPDATE biz_health_monitor SET place_name = ?, place_type = ?, address = ?, grid_id = ?, grid_name = ?, " +
            "monitor_item = ?, score = ?, risk_level = ?, monitor_date = ?, monitor_org = ?, remark = ?, updated_at = NOW() WHERE id = ?",
            str(body, "placeName"),
            str(body, "placeType", "OTHER"),
            str(body, "address"),
            num(body, "gridId"),
            str(body, "gridName"),
            str(body, "monitorItem"),
            intVal(body, "score", 100),
            str(body, "riskLevel", "GREEN"),
            str(body, "monitorDate", LocalDate.now().toString()),
            str(body, "monitorOrg"),
            str(body, "remark"),
            id) > 0;
    }

    @Transactional
    public boolean deleteMonitor(Long id) {
        return jdbcTemplate.update("DELETE FROM biz_health_monitor WHERE id = ?", id) > 0;
    }

    // ==================== 检测设备接入（设备台账 + 监测数据流） ====================

    /** 各指标默认预警阈值：DENSITY=成蚊密度(只/灯·夜) CAPTURE=捕获数 BITE=叮咬率 BREEDING=孳生指数 */
    private static final Map<String, Double> DEFAULT_THRESHOLDS = Map.of(
            "DENSITY", 10.0, "CAPTURE", 50.0, "BITE", 5.0, "BREEDING", 30.0);

    /** 模拟数据各指标基准（按孳生地风险等级 RED/YELLOW/GREEN 三档），15% 概率超标 */
    private static final Map<String, double[]> SIM_BASE = Map.of(
            "DENSITY", new double[]{9.5, 6.5, 3.5, 10.0},
            "CAPTURE", new double[]{48, 30, 16, 50.0},
            "BITE", new double[]{4.6, 3.2, 1.6, 5.0},
            "BREEDING", new double[]{28, 19, 10, 30.0});

    /**
     * 设备上报数据入库（设备接入入口）：自动注册设备、关联孳生地、超标判定、刷新在线状态
     * body: deviceNo/deviceName/deviceType/siteId/siteName/metricType/metricValue/threshold/collectedAt
     */
    @Transactional
    public Map<String, Object> ingestDeviceData(Map<String, Object> body) {
        String deviceNo = str(body, "deviceNo");
        if (deviceNo == null || deviceNo.isBlank()) {
            throw new IllegalArgumentException("设备编号 deviceNo 不能为空");
        }
        String metricType = str(body, "metricType", "DENSITY");
        if (!DEFAULT_THRESHOLDS.containsKey(metricType)) {
            throw new IllegalArgumentException("不支持的指标类型: " + metricType);
        }
        java.math.BigDecimal value = dec(body, "metricValue");
        if (value == null) {
            throw new IllegalArgumentException("指标数值 metricValue 不能为空");
        }
        LocalDateTime collectedAt = LocalDateTime.now();
        Object t = body.get("collectedAt");
        if (t != null) {
            try {
                collectedAt = LocalDateTime.parse(String.valueOf(t).trim().replace(" ", "T"));
            } catch (Exception ignored) {
                // 时间格式非法时使用当前时间
            }
        }
        // 自动注册设备台账（不存在时）
        registerDeviceIfAbsent(deviceNo, str(body, "deviceName"), str(body, "deviceType", "MOSQUITO_TRAP"),
                num(body, "siteId"), str(body, "siteName"));
        // 阈值：默认按指标类型，可覆盖
        java.math.BigDecimal threshold = dec(body, "threshold");
        if (threshold == null) {
            threshold = java.math.BigDecimal.valueOf(DEFAULT_THRESHOLDS.getOrDefault(metricType, 10.0));
        }
        String alarmLevel = value.compareTo(threshold) > 0 ? "OVER" : "NORMAL";
        // 去重写入（同一设备同一指标同一采集时间只保留一条）
        Integer exists = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM biz_mosquito_device_data WHERE device_no = ? AND metric_type = ? AND collected_at = ?",
                Integer.class, deviceNo, metricType, Timestamp.valueOf(collectedAt));
        if (exists == null || exists == 0) {
            jdbcTemplate.update(
                "INSERT INTO biz_mosquito_device_data (device_no, site_id, site_name, metric_type, metric_value, threshold, alarm_level, collected_at, created_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, NOW())",
                deviceNo, num(body, "siteId"), str(body, "siteName"), metricType, value, threshold,
                alarmLevel, Timestamp.valueOf(collectedAt));
        }
        // 刷新设备在线状态
        jdbcTemplate.update("UPDATE biz_mosquito_device SET status = 'ONLINE', last_online_at = ?, updated_at = NOW() WHERE device_no = ?",
                Timestamp.valueOf(collectedAt), deviceNo);
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("alarmLevel", alarmLevel);
        result.put("collectedAt", collectedAt.toString());
        return result;
    }

    /** 设备台账列表（含最近一次上报的指标/数值/时间） */
    public Map<String, Object> listDevices(int page, int size) {
        Long total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM biz_mosquito_device", Long.class);
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            "SELECT d.*, " +
            "(SELECT dd.metric_type FROM biz_mosquito_device_data dd WHERE dd.device_no = d.device_no ORDER BY dd.collected_at DESC, dd.id DESC LIMIT 1) AS last_metric, " +
            "(SELECT dd.metric_value FROM biz_mosquito_device_data dd WHERE dd.device_no = d.device_no ORDER BY dd.collected_at DESC, dd.id DESC LIMIT 1) AS last_metric_value, " +
            "(SELECT dd.collected_at FROM biz_mosquito_device_data dd WHERE dd.device_no = d.device_no ORDER BY dd.collected_at DESC, dd.id DESC LIMIT 1) AS last_data_at " +
            "FROM biz_mosquito_device d ORDER BY d.id DESC LIMIT ? OFFSET ?",
            size, (page - 1) * size);
        Map<String, Object> result = new HashMap<>();
        result.put("total", total != null ? total : 0L);
        result.put("items", rows);
        return result;
    }

    /** 监测数据明细（可按孳生地/设备/指标筛选） */
    public Map<String, Object> listDeviceData(Long siteId, String deviceNo, String metricType, int page, int size) {
        List<Object> params = new ArrayList<>();
        StringBuilder where = new StringBuilder(" WHERE 1=1");
        if (siteId != null) {
            where.append(" AND site_id = ?");
            params.add(siteId);
        }
        if (deviceNo != null && !deviceNo.isBlank()) {
            where.append(" AND device_no = ?");
            params.add(deviceNo.trim());
        }
        if (metricType != null && !metricType.isBlank()) {
            where.append(" AND metric_type = ?");
            params.add(metricType.trim());
        }
        Long total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM biz_mosquito_device_data" + where, Long.class, params.toArray());
        List<Object> pageParams = new ArrayList<>(params);
        pageParams.add(size);
        pageParams.add((page - 1) * size);
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            "SELECT * FROM biz_mosquito_device_data" + where + " ORDER BY collected_at DESC, id DESC LIMIT ? OFFSET ?",
            pageParams.toArray());
        Map<String, Object> result = new HashMap<>();
        result.put("total", total != null ? total : 0L);
        result.put("items", rows);
        return result;
    }

    /** 趋势点（按孳生地+指标，近 N 小时，供图表绘制） */
    public List<Map<String, Object>> deviceTrend(Long siteId, String deviceNo, String metricType, Integer hours) {
        int safeHours = hours != null && hours > 0 ? Math.min(hours, 24 * 30) : 24;
        List<Object> params = new ArrayList<>();
        StringBuilder where = new StringBuilder(" WHERE metric_type = ?");
        params.add(metricType != null && !metricType.isBlank() ? metricType.trim() : "DENSITY");
        if (siteId != null) {
            where.append(" AND site_id = ?");
            params.add(siteId);
        } else if (deviceNo != null && !deviceNo.isBlank()) {
            where.append(" AND device_no = ?");
            params.add(deviceNo.trim());
        }
        where.append(" AND collected_at >= DATE_SUB(NOW(), INTERVAL ? HOUR)");
        params.add(safeHours);
        return jdbcTemplate.queryForList(
            "SELECT collected_at, metric_value, threshold, alarm_level, device_no, site_name FROM biz_mosquito_device_data" +
            where + " ORDER BY collected_at ASC", params.toArray());
    }

    /** 设备监测统计：设备总数/在线/今日上报/近7日超标/覆盖孳生地 */
    public Map<String, Object> deviceStatistics() {
        Map<String, Object> result = new HashMap<>();
        result.put("devices", count("SELECT COUNT(*) FROM biz_mosquito_device"));
        result.put("online", count("SELECT COUNT(*) FROM biz_mosquito_device WHERE last_online_at >= DATE_SUB(NOW(), INTERVAL 30 MINUTE)"));
        result.put("todayReports", count("SELECT COUNT(*) FROM biz_mosquito_device_data WHERE collected_at >= CURDATE()"));
        result.put("overThreshold", count("SELECT COUNT(*) FROM biz_mosquito_device_data WHERE alarm_level = 'OVER' AND collected_at >= DATE_SUB(NOW(), INTERVAL 7 DAY)"));
        result.put("coveredSites", count("SELECT COUNT(DISTINCT site_id) FROM biz_mosquito_device WHERE site_id IS NOT NULL"));
        return result;
    }

    /**
     * 一键生成演示数据：为每个在管孳生地注册设备并生成最近 N 天时间序列（4 类指标，2 小时间隔，部分超标）
     */
    @Transactional
    public Map<String, Object> simulateDeviceData(int days) {
        int safeDays = days <= 0 ? 3 : Math.min(days, 14);
        List<Map<String, Object>> sites = jdbcTemplate.queryForList(
            "SELECT id, site_no, site_name, risk_level FROM biz_mosquito_site WHERE status = 'ACTIVE' ORDER BY id");
        if (sites.isEmpty()) {
            return Map.of("success", true, "message", "暂无可生成的在管孳生地", "generated", 0);
        }
        int total = 0, over = 0;
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime start = now.minusDays(safeDays).withHour(8).withMinute(0).withSecond(0).withNano(0);
        if (start.isAfter(now)) {
            start = start.minusDays(1);
        }
        for (Map<String, Object> site : sites) {
            Long siteId = ((Number) site.get("id")).longValue();
            String siteName = String.valueOf(site.get("site_name"));
            String risk = String.valueOf(site.get("risk_level"));
            String deviceNo = ensureDemoDevice(siteId, siteName);
            for (Map.Entry<String, double[]> entry : SIM_BASE.entrySet()) {
                String metricType = entry.getKey();
                double[] cfg = entry.getValue();
                double base = risk.equals("RED") ? cfg[0] : risk.equals("YELLOW") ? cfg[1] : cfg[2];
                double threshold = cfg[3];
                for (LocalDateTime t = start; !t.isAfter(now); t = t.plusHours(2)) {
                    double v = base + (random.nextDouble() * 2 - 1) * base * 0.3;
                    if (random.nextDouble() < 0.15) {
                        v = base + threshold * 0.3 + random.nextDouble() * threshold * 0.5; // 15% 超标
                    }
                    String level = v > threshold ? "OVER" : "NORMAL";
                    insertDeviceDataIfAbsent(deviceNo, siteId, siteName, metricType, v, threshold, level, t);
                    total++;
                    if ("OVER".equals(level)) {
                        over++;
                    }
                }
            }
            jdbcTemplate.update("UPDATE biz_mosquito_device SET status = 'ONLINE', last_online_at = NOW(), updated_at = NOW() WHERE device_no = ?", deviceNo);
        }
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("generated", total);
        result.put("overThreshold", over);
        result.put("sites", sites.size());
        return result;
    }

    /** 定时补充最新数据点：为每个在管孳生地的设备生成当前时刻一条 DENSITY 数据（去重跳过） */
    @Transactional
    public void simulateLatestPoint() {
        List<Map<String, Object>> devices = jdbcTemplate.queryForList(
            "SELECT d.device_no, d.site_id, d.site_name FROM biz_mosquito_device d " +
            "JOIN biz_mosquito_site s ON s.id = d.site_id AND s.status = 'ACTIVE'");
        if (devices.isEmpty()) {
            return;
        }
        LocalDateTime now = LocalDateTime.now().withSecond(0).withNano(0);
        for (Map<String, Object> d : devices) {
            String deviceNo = String.valueOf(d.get("device_no"));
            Long siteId = d.get("site_id") instanceof Number ? ((Number) d.get("site_id")).longValue() : null;
            String siteName = String.valueOf(d.get("site_name"));
            double v = 3.5 + random.nextDouble() * 7;
            insertDeviceDataIfAbsent(deviceNo, siteId, siteName, "DENSITY", v, 10.0, v > 10 ? "OVER" : "NORMAL", now);
        }
    }

    private void insertDeviceDataIfAbsent(String deviceNo, Long siteId, String siteName, String metricType,
                                          double value, double threshold, String level, LocalDateTime collectedAt) {
        Integer exists = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM biz_mosquito_device_data WHERE device_no = ? AND metric_type = ? AND collected_at = ?",
            Integer.class, deviceNo, metricType, Timestamp.valueOf(collectedAt));
        if (exists != null && exists > 0) {
            return;
        }
        jdbcTemplate.update(
            "INSERT INTO biz_mosquito_device_data (device_no, site_id, site_name, metric_type, metric_value, threshold, alarm_level, collected_at, created_at) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, NOW())",
            deviceNo, siteId, siteName, metricType,
            java.math.BigDecimal.valueOf(Math.round(value * 100) / 100.0),
            java.math.BigDecimal.valueOf(threshold), level, Timestamp.valueOf(collectedAt));
    }

    private void registerDeviceIfAbsent(String deviceNo, String deviceName, String deviceType, Long siteId, String siteName) {
        Integer exists = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM biz_mosquito_device WHERE device_no = ?", Integer.class, deviceNo);
        if (exists != null && exists > 0) {
            // 首次上报时补全孳生地关联
            if (siteId != null) {
                jdbcTemplate.update(
                    "UPDATE biz_mosquito_device SET site_id = IFNULL(site_id, ?), site_name = IFNULL(site_name, ?), updated_at = NOW() WHERE device_no = ?",
                    siteId, siteName, deviceNo);
            }
            return;
        }
        jdbcTemplate.update(
            "INSERT INTO biz_mosquito_device (device_no, device_name, device_type, site_id, site_name, status, last_online_at, remark, created_at, updated_at) " +
            "VALUES (?, ?, ?, ?, ?, 'ONLINE', NOW(), ?, NOW(), NOW())",
            deviceNo,
            deviceName != null && !deviceName.isBlank() ? deviceName : deviceNo + " 监测仪",
            deviceType, siteId, siteName, bodyRemark(siteName));
    }

    private String bodyRemark(String siteName) {
        return siteName != null && !siteName.isBlank() ? "设备部署于: " + siteName : null;
    }

    /** 演示用：为在管孳生地确保有设备（无则注册） */
    private String ensureDemoDevice(Long siteId, String siteName) {
        List<Map<String, Object>> existing = jdbcTemplate.queryForList(
            "SELECT device_no FROM biz_mosquito_device WHERE site_id = ? LIMIT 1", siteId);
        if (!existing.isEmpty()) {
            return String.valueOf(existing.get(0).get("device_no"));
        }
        String deviceNo = "MD-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                + "-" + String.format("%04d", (int) (Math.random() * 9000 + 1000));
        jdbcTemplate.update(
            "INSERT INTO biz_mosquito_device (device_no, device_name, device_type, site_id, site_name, status, last_online_at, remark, created_at, updated_at) " +
            "VALUES (?, ?, 'MOSQUITO_TRAP', ?, ?, 'ONLINE', NOW(), ?, NOW(), NOW())",
            deviceNo, siteName + " 智能捕蚊器", siteId, siteName, "自动注册演示设备");
        return deviceNo;
    }

    // ==================== 工具方法 ====================

    private long count(String sql) {
        Long c = jdbcTemplate.queryForObject(sql, Long.class);
        return c != null ? c : 0L;
    }

    private String str(Map<String, Object> body, String key) {
        Object v = body.get(key);
        return v == null ? null : String.valueOf(v).trim();
    }

    private String str(Map<String, Object> body, String key, String def) {
        String v = str(body, key);
        return (v == null || v.isEmpty()) ? def : v;
    }

    private Long num(Map<String, Object> body, String key) {
        Object v = body.get(key);
        if (v == null) return null;
        try {
            return Long.parseLong(String.valueOf(v));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private java.math.BigDecimal dec(Map<String, Object> body, String key) {
        Object v = body.get(key);
        if (v == null) return null;
        try {
            return new java.math.BigDecimal(String.valueOf(v));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private int intVal(Map<String, Object> body, String key, int def) {
        Object v = body.get(key);
        if (v == null) return def;
        try {
            return Integer.parseInt(String.valueOf(v));
        } catch (NumberFormatException e) {
            return def;
        }
    }
}
