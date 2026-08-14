package com.changping.platform.modules.vehicle.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 车辆/人员轨迹追踪（C2）— R10 停车管理「车辆与人员轨迹追踪」
 * 结合视频监控 AI 分析,记录车辆进出与移动轨迹,支持至少 7 天历史回溯。
 * 真实环境:车牌识别相机推送(ENTER/EXIT/MOVE 抓拍);当前为演示数据生成 + 轨迹查询回放。
 */
@Service
public class VehicleTrackService {

    private static final Logger log = LoggerFactory.getLogger(VehicleTrackService.class);

    private static final DateTimeFormatter TS_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** 轨迹类型字典 */
    private static final Map<String, String> TYPE_NAMES = Map.of(
            "ENTER", "进入", "EXIT", "离开", "MOVE", "移动");

    private final JdbcTemplate jdbcTemplate;

    public VehicleTrackService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /** 演示数据:10 辆车最近 7 天的进出与移动轨迹(沿社区路网点随机游走) */
    @Transactional
    public Map<String, Object> generateDemo() {
        // 路网点:视频点位 + 车位 + 违停点坐标
        List<double[]> points = new ArrayList<>();
        List<Map<String, Object>> cameras = jdbcTemplate.queryForList(
                "SELECT id, camera_name, longitude, latitude FROM biz_video_camera WHERE longitude IS NOT NULL ORDER BY id");
        for (Map<String, Object> c : cameras) {
            points.add(new double[]{toDouble(c.get("longitude")), toDouble(c.get("latitude"))});
        }
        for (Map<String, Object> r : jdbcTemplate.queryForList(
                "SELECT longitude, latitude FROM biz_parking_space WHERE longitude IS NOT NULL LIMIT 6")) {
            points.add(new double[]{toDouble(r.get("longitude")), toDouble(r.get("latitude"))});
        }
        if (points.size() < 4) {
            points.add(new double[]{113.9395, 22.9712});
            points.add(new double[]{113.9400, 22.9720});
            points.add(new double[]{113.9410, 22.9730});
            points.add(new double[]{113.9420, 22.9740});
        }

        String[] plates = {"粤S·A1B2C3", "粤S·D8E9F0", "粤S·K2L5M8", "粤S·P3Q7R6", "粤S·T9U1V4",
                "粤S·W6X8Y2", "粤S·Z4C7D1", "粤S·F5G9H3", "粤S·J7N2P8", "粤S·M1S6T9"};
        String[] addrs = {"社区大门", "A区1号路", "A区消防通道", "B区2号路", "B区充电车位",
                "C区主干路", "社区内部道路", "龙景小区出入口", "学校门口", "A区停车场"};

        jdbcTemplate.update("DELETE FROM biz_vehicle_track_record");

        LocalDateTime now = LocalDateTime.now();
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        int total = 0;

        for (String plate : plates) {
            // 每辆车在 7 天内 1~2 段行程/天,早晚高峰概率高
            for (int day = 6; day >= 0; day--) {
                int trips = rnd.nextInt(3); // 0~2 段
                for (int t = 0; t < trips; t++) {
                    LocalDateTime enterTime = now.minusDays(day).minusHours(rnd.nextInt(12) + (t == 0 ? 1 : 4));
                    // 从入口点位进入
                    double[] enter = points.get(rnd.nextInt(Math.min(3, points.size())));
                    insertRecord(plate, "ENTER", cameras.isEmpty() ? null : num(cameras.get(0).get("id")),
                            cameras.isEmpty() ? null : String.valueOf(cameras.get(0).get("camera_name")),
                            enter[0], enter[1], addrs[0], rnd.nextInt(15, 35), enterTime);
                    total++;
                    // 中间移动点:随机游走 2~5 点
                    int moveCount = rnd.nextInt(2, 6);
                    int cur = rnd.nextInt(points.size());
                    for (int m = 0; m < moveCount; m++) {
                        cur = (cur + rnd.nextInt(1, 4)) % points.size();
                        double[] p = points.get(cur);
                        enterTime = enterTime.plusMinutes(rnd.nextInt(2, 9));
                        insertRecord(plate, "MOVE", null, null, p[0], p[1],
                                addrs[rnd.nextInt(addrs.length)], rnd.nextInt(5, 45), enterTime);
                        total++;
                    }
                    // 离开
                    enterTime = enterTime.plusMinutes(rnd.nextInt(2, 9));
                    insertRecord(plate, "EXIT", null, null,
                            enter[0], enter[1], addrs[0], rnd.nextInt(15, 35), enterTime);
                    total++;
                }
            }
        }
        // 让部分车辆当前仍在社区内:随机 3~4 辆删除其最后一条 EXIT 记录
        int stayCount = rnd.nextInt(3, 5);
        List<String> shuffled = new ArrayList<>(List.of(plates));
        Collections.shuffle(shuffled, new Random(System.currentTimeMillis()));
        for (int i = 0; i < stayCount && i < shuffled.size(); i++) {
            jdbcTemplate.update("DELETE FROM biz_vehicle_track_record WHERE id = " +
                    "(SELECT id FROM (SELECT id FROM biz_vehicle_track_record WHERE vehicle_plate = ? " +
                    "ORDER BY captured_at DESC, id DESC LIMIT 1) t)", shuffled.get(i));
        }
        log.info("车辆轨迹演示数据已生成:{} 条记录,{} 辆车在社区内", total - stayCount, stayCount);
        Map<String, Object> result = new HashMap<>();
        result.put("vehicles", plates.length);
        result.put("records", total - stayCount);
        result.put("inside", stayCount);
        return result;
    }

    private void insertRecord(String plate, String type, Long cameraId, String cameraName,
                              double lng, double lat, String addr, int speed, LocalDateTime time) {
        jdbcTemplate.update("INSERT INTO biz_vehicle_track_record " +
                        "(vehicle_plate, track_type, camera_id, camera_name, longitude, latitude, address, speed, captured_at) " +
                        "VALUES (?,?,?,?,?,?,?,?,?)",
                plate, type, cameraId, cameraName, lng, lat, addr, (double) speed, time);
    }

    /** 统计:在管车辆数/7天轨迹点/今日进出/当前在社区内 */
    public Map<String, Object> stats() {
        Map<String, Object> result = new HashMap<>();
        result.put("vehicleCount", firstLong("SELECT COUNT(DISTINCT vehicle_plate) FROM biz_vehicle_track_record"));
        result.put("trackPoints7d", firstLong(
                "SELECT COUNT(*) FROM biz_vehicle_track_record WHERE captured_at >= DATE_SUB(NOW(), INTERVAL 7 DAY)"));
        result.put("todayInOut", firstLong(
                "SELECT COUNT(*) FROM biz_vehicle_track_record WHERE track_type IN ('ENTER','EXIT') AND captured_at >= CURDATE()"));
        result.put("insideCount", firstLong(
                "SELECT COUNT(*) FROM (SELECT o.vehicle_plate FROM biz_vehicle_track_record o GROUP BY o.vehicle_plate " +
                        "HAVING (SELECT r2.track_type FROM biz_vehicle_track_record r2 WHERE r2.vehicle_plate = o.vehicle_plate " +
                        "ORDER BY r2.captured_at DESC, r2.id DESC LIMIT 1) <> 'EXIT') t"));
        return result;
    }

    /** 车辆列表(最后位置/最后时间/轨迹点数/是否在社区内) */
    public List<Map<String, Object>> vehicles(String keyword) {
        List<Object> params = new ArrayList<>();
        StringBuilder where = new StringBuilder();
        if (keyword != null && !keyword.isBlank()) {
            where.append(" WHERE vehicle_plate LIKE ?");
            params.add("%" + keyword.trim() + "%");
        }
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT vehicle_plate, COUNT(*) AS point_count, MAX(captured_at) AS last_at, " +
                        "SUBSTRING_INDEX(GROUP_CONCAT(track_type ORDER BY captured_at DESC, id DESC), ',', 1) AS last_type, " +
                        "SUBSTRING_INDEX(GROUP_CONCAT(COALESCE(CAST(longitude AS CHAR), '') ORDER BY captured_at DESC, id DESC), ',', 1) AS last_lng, " +
                        "SUBSTRING_INDEX(GROUP_CONCAT(COALESCE(CAST(latitude AS CHAR), '') ORDER BY captured_at DESC, id DESC), ',', 1) AS last_lat, " +
                        "SUBSTRING_INDEX(GROUP_CONCAT(COALESCE(address, '') ORDER BY captured_at DESC, id DESC), ',', 1) AS last_addr " +
                        "FROM biz_vehicle_track_record" + where +
                        " GROUP BY vehicle_plate ORDER BY last_at DESC",
                params.toArray());
        for (Map<String, Object> row : rows) {
            row.put("inside", !"EXIT".equals(row.get("last_type")));
            row.put("type_name", TYPE_NAMES.getOrDefault(row.get("last_type"), String.valueOf(row.get("last_type"))));
        }
        return rows;
    }

    /** 单车轨迹(时间升序,支持 7 天回溯) */
    public List<Map<String, Object>> trajectory(String plate, String start, String end) {
        StringBuilder sql = new StringBuilder(
                "SELECT id, vehicle_plate, track_type, camera_name, longitude, latitude, address, speed, captured_at " +
                        "FROM biz_vehicle_track_record WHERE vehicle_plate = ?");
        List<Object> params = new ArrayList<>();
        params.add(plate);
        if (start != null && !start.isBlank()) {
            sql.append(" AND captured_at >= ?");
            params.add(start.trim());
        }
        if (end != null && !end.isBlank()) {
            sql.append(" AND captured_at <= ?");
            params.add(end.trim());
        }
        sql.append(" ORDER BY captured_at ASC, id ASC");
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql.toString(), params.toArray());
        for (Map<String, Object> row : rows) {
            row.put("type_name", TYPE_NAMES.getOrDefault(row.get("track_type"), String.valueOf(row.get("track_type"))));
        }
        return rows;
    }

    /** 进出记录分页 */
    public Map<String, Object> records(String type, String start, String end, int page, int size) {
        List<Object> params = new ArrayList<>();
        StringBuilder where = new StringBuilder(" WHERE track_type IN ('ENTER','EXIT')");
        if (type != null && !type.isBlank()) {
            where.append(" AND track_type = ?");
            params.add(type.trim());
        }
        if (start != null && !start.isBlank()) {
            where.append(" AND captured_at >= ?");
            params.add(start.trim());
        }
        if (end != null && !end.isBlank()) {
            where.append(" AND captured_at <= ?");
            params.add(end.trim());
        }
        Long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM biz_vehicle_track_record" + where, Long.class, params.toArray());
        List<Object> pageParams = new ArrayList<>(params);
        pageParams.add(size);
        pageParams.add((page - 1) * size);
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT id, vehicle_plate, track_type, camera_name, address, speed, captured_at " +
                        "FROM biz_vehicle_track_record" + where + " ORDER BY captured_at DESC, id DESC LIMIT ? OFFSET ?",
                pageParams.toArray());
        for (Map<String, Object> row : rows) {
            row.put("type_name", TYPE_NAMES.getOrDefault(row.get("track_type"), String.valueOf(row.get("track_type"))));
        }
        Map<String, Object> result = new HashMap<>();
        result.put("total", total != null ? total : 0L);
        result.put("items", rows);
        result.put("page", page);
        result.put("size", size);
        return result;
    }

    private Long firstLong(String sql) {
        Long v = jdbcTemplate.queryForObject(sql, Long.class);
        return v != null ? v : 0L;
    }

    private double toDouble(Object o) {
        return o == null ? 0 : Double.parseDouble(String.valueOf(o));
    }

    private Long num(Object o) {
        return o == null ? null : Long.parseLong(String.valueOf(o));
    }
}
