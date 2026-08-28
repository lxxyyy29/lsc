package com.changping.platform.modules.video.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 固定摄像头视频接入/轮巡（D）— 监控点位台账 + 视频流地址 + HLS 转流代理
 * 流类型：HLS（浏览器 hls.js 直播）/ FLV（flv.js 直播）/ RTSP（需转流，演示环境由宿主机 ffmpeg 转 HLS）
 * HLS 转流目录：/app/video-hls/{deviceNo}/index.m3u8（宿主机 /opt/zhsq/video-hls 挂载）
 */
@Service
public class VideoCameraService {

    private static final Logger log = LoggerFactory.getLogger(VideoCameraService.class);

    /** 容器内 HLS 转流根目录（docker-compose 挂载宿主机 /opt/zhsq/video-hls） */
    private static final String HLS_ROOT = "/app/video-hls";

    private final JdbcTemplate jdbcTemplate;

    public VideoCameraService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /** 点位分页列表（keyword 匹配名称/设备号/位置，可按网格/状态筛选） */
    public Map<String, Object> listCameras(String keyword, Long gridId, String status, int page, int size) {
        List<Object> params = new ArrayList<>();
        StringBuilder where = new StringBuilder(" WHERE 1=1");
        if (keyword != null && !keyword.isBlank()) {
            where.append(" AND (camera_name LIKE ? OR device_no LIKE ? OR address LIKE ?)");
            String kw = "%" + keyword.trim() + "%";
            params.add(kw);
            params.add(kw);
            params.add(kw);
        }
        if (gridId != null) {
            where.append(" AND grid_id = ?");
            params.add(gridId);
        }
        if (status != null && !status.isBlank()) {
            where.append(" AND status = ?");
            params.add(status.trim());
        }
        Long total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM biz_video_camera" + where, Long.class, params.toArray());
        List<Object> pageParams = new ArrayList<>(params);
        pageParams.add(size);
        pageParams.add((page - 1) * size);
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            "SELECT * FROM biz_video_camera" + where + " ORDER BY FIELD(status,'ACTIVE','MAINTENANCE','OFFLINE'), id DESC LIMIT ? OFFSET ?",
            pageParams.toArray());
        // 附加是否已有转流文件（HLS 可播放标记）
        for (Map<String, Object> row : rows) {
            row.put("playable", hasHlsFiles(String.valueOf(row.get("device_no"))));
        }
        Map<String, Object> result = new HashMap<>();
        result.put("total", total != null ? total : 0L);
        result.put("items", rows);
        return result;
    }

    /** 点位统计（总数/在线/离线/已接入转流数） */
    public Map<String, Object> statistics() {
        Long total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM biz_video_camera", Long.class);
        Long online = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM biz_video_camera WHERE status='ACTIVE'", Long.class);
        Long offline = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM biz_video_camera WHERE status='OFFLINE'", Long.class);
        Map<String, Object> result = new HashMap<>();
        result.put("total", total != null ? total : 0L);
        result.put("online", online != null ? online : 0L);
        result.put("offline", offline != null ? offline : 0L);
        return result;
    }

    /** 新增点位 */
    @Transactional
    public Long createCamera(Map<String, Object> body) {
        String cameraName = str(body.get("cameraName"));
        if (cameraName.isBlank()) {
            throw new IllegalArgumentException("点位名称 cameraName 不能为空");
        }
        String deviceNo = str(body.get("deviceNo"));
        if (deviceNo.isBlank()) {
            throw new IllegalArgumentException("设备编号 deviceNo 不能为空");
        }
        Long exists = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM biz_video_camera WHERE device_no = ?", Long.class, deviceNo);
        if (exists != null && exists > 0) {
            throw new IllegalArgumentException("设备编号 " + deviceNo + " 已存在");
        }
        String streamType = str(body.getOrDefault("streamType", "HLS")).toUpperCase();
        if (!List.of("HLS", "FLV", "RTSP").contains(streamType)) {
            throw new IllegalArgumentException("流类型 streamType 仅支持 HLS/FLV/RTSP");
        }
        String streamUrl = str(body.get("streamUrl"));
        if (streamUrl.isBlank()) {
            throw new IllegalArgumentException("视频流地址 streamUrl 不能为空");
        }
        jdbcTemplate.update(
            "INSERT INTO biz_video_camera (camera_name, camera_type, device_no, stream_type, stream_url, longitude, latitude, address, grid_id, grid_name, status, remark) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            cameraName,
            str(body.getOrDefault("cameraType", "FIXED")).toUpperCase(),
            deviceNo,
            streamType,
            streamUrl,
            body.get("longitude"),
            body.get("latitude"),
            str(body.get("address")),
            body.get("gridId"),
            str(body.get("gridName")),
            str(body.getOrDefault("status", "ACTIVE")).toUpperCase(),
            str(body.get("remark")));
        Long id = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        log.info("新增摄像头点位: id={}, name={}, deviceNo={}, streamType={}", id, cameraName, deviceNo, streamType);
        return id;
    }

    /** 更新点位（未传字段保留原值） */
    @Transactional
    public void updateCamera(Long id, Map<String, Object> body) {
        Map<String, Object> exist = jdbcTemplate.queryForMap("SELECT * FROM biz_video_camera WHERE id = ?", id);
        String cameraName = body.containsKey("cameraName") ? str(body.get("cameraName")) : str(exist.get("camera_name"));
        if (cameraName.isBlank()) {
            throw new IllegalArgumentException("点位名称不能为空");
        }
        jdbcTemplate.update(
            "UPDATE biz_video_camera SET camera_name=?, camera_type=?, stream_type=?, stream_url=?, longitude=?, latitude=?, address=?, grid_id=?, grid_name=?, status=?, remark=? WHERE id=?",
            cameraName,
            body.containsKey("cameraType") ? str(body.get("cameraType")).toUpperCase() : str(exist.get("camera_type")),
            body.containsKey("streamType") ? str(body.get("streamType")).toUpperCase() : str(exist.get("stream_type")),
            body.containsKey("streamUrl") ? str(body.get("streamUrl")) : str(exist.get("stream_url")),
            body.containsKey("longitude") ? body.get("longitude") : exist.get("longitude"),
            body.containsKey("latitude") ? body.get("latitude") : exist.get("latitude"),
            body.containsKey("address") ? str(body.get("address")) : str(exist.get("address")),
            body.containsKey("gridId") ? body.get("gridId") : exist.get("grid_id"),
            body.containsKey("gridName") ? str(body.get("gridName")) : str(exist.get("grid_name")),
            body.containsKey("status") ? str(body.get("status")).toUpperCase() : str(exist.get("status")),
            body.containsKey("remark") ? str(body.get("remark")) : str(exist.get("remark")),
            id);
        log.info("更新摄像头点位: id={}", id);
    }

    /** 删除点位 */
    @Transactional
    public void deleteCamera(Long id) {
        jdbcTemplate.update("DELETE FROM biz_video_camera WHERE id = ?", id);
        log.info("删除摄像头点位: id={}", id);
    }

    /** 获取播放流地址：HLS/FLV 直出；RTSP 返回本平台转流代理地址 */
    public Map<String, Object> getStream(Long id) {
        Map<String, Object> camera = jdbcTemplate.queryForMap("SELECT * FROM biz_video_camera WHERE id = ?", id);
        String streamType = str(camera.get("stream_type"));
        String deviceNo = str(camera.get("device_no"));
        Map<String, Object> result = new HashMap<>();
        result.put("cameraId", id);
        result.put("cameraName", camera.get("camera_name"));
        result.put("streamType", streamType);
        if ("RTSP".equals(streamType)) {
            // RTSP 浏览器不可直播：统一走平台转流代理（宿主机 ffmpeg 输出 /opt/zhsq/video-hls/{deviceNo}/）
            result.put("streamUrl", "/api/video/stream/" + id + "/index.m3u8");
            result.put("converted", true);
        } else if (str(camera.get("stream_url")).startsWith("proxy://")) {
            // 演示点位：流地址存协议占位，实际由平台 HLS 转流目录提供
            result.put("streamUrl", "/api/video/stream/" + id + "/index.m3u8");
            result.put("converted", true);
        } else {
            String raw = str(camera.get("stream_url"));
            // HTTP(S) 外部流：统一走 Web 端 /ext-video/ 同源代理，避免 HTTPS 页面因 Mixed Content 拦截导致无画面
            // 代理格式: /ext-video/http://host:port/path -> proxy_pass http://host:port/path (nginx-web.conf location 正则)
            if (raw.startsWith("http://") || raw.startsWith("https://")) {
                raw = "/ext-video/" + raw;
            }
            result.put("streamUrl", raw);
            result.put("converted", false);
        }
        return result;
    }

    /** 代理读取 HLS 转流文件（m3u8 或 ts 分片），deviceNo 校验防路径穿越 */
    public Resource streamFile(Long cameraId, String filename) {
        Map<String, Object> camera = jdbcTemplate.queryForMap("SELECT * FROM biz_video_camera WHERE id = ?", cameraId);
        String deviceNo = str(camera.get("device_no"));
        if (deviceNo.isBlank() || !deviceNo.matches("[A-Za-z0-9_\\-]+")) {
            throw new IllegalArgumentException("设备编号非法");
        }
        File file = new File(HLS_ROOT, deviceNo + File.separator + filename);
        if (!file.exists() || !file.isFile() || !file.getAbsolutePath().startsWith(HLS_ROOT)) {
            throw new IllegalArgumentException("转流文件不存在: " + filename);
        }
        return new FileSystemResource(file);
    }

    /** 录像目录：/opt/zhsq/video-hls/record/{deviceNo}/{yyyyMMdd}/seg_%03d.mp4（宿主机 ffmpeg 录制，保留 7 天） */
    private static final String RECORD_ROOT = "/app/video-hls/record";

    /** 有录像的日期列表（近 7 天，按日期倒序） */
    public List<String> listRecordDates(Long cameraId) {
        Map<String, Object> camera = jdbcTemplate.queryForMap("SELECT * FROM biz_video_camera WHERE id = ?", cameraId);
        String deviceNo = str(camera.get("device_no"));
        if (deviceNo.isBlank() || !deviceNo.matches("[A-Za-z0-9_\\-]+")) {
            return List.of();
        }
        File dir = new File(RECORD_ROOT, deviceNo);
        File[] days = dir.listFiles(File::isDirectory);
        if (days == null) {
            return List.of();
        }
        return Arrays.stream(days)
                .map(File::getName)
                .filter(d -> d.matches("\\d{8}"))
                .sorted(Comparator.reverseOrder())
                .collect(Collectors.toList());
    }

    /** 指定日期的录像文件列表（每段 10 分钟，含开始时间/时长/大小） */
    public List<Map<String, Object>> listRecords(Long cameraId, String date) {
        Map<String, Object> camera = jdbcTemplate.queryForMap("SELECT * FROM biz_video_camera WHERE id = ?", cameraId);
        String deviceNo = str(camera.get("device_no"));
        if (deviceNo.isBlank() || !deviceNo.matches("[A-Za-z0-9_\\-]+") || !date.matches("\\d{8}")) {
            return List.of();
        }
        File dir = new File(new File(RECORD_ROOT, deviceNo), date);
        File[] files = dir.listFiles(f -> f.isFile() && f.getName().matches("seg_\\d+\\.mp4"));
        if (files == null) {
            return List.of();
        }
        List<Map<String, Object>> list = new ArrayList<>();
        for (File f : files) {
            // 段序号 → 开始时间（每段 600 秒=10 分钟）
            int idx = Integer.parseInt(f.getName().replaceAll("seg_(\\d+)\\.mp4", "$1"));
            long startSec = (long) idx * 600;
            Map<String, Object> row = new HashMap<>();
            row.put("file", f.getName());
            row.put("startTime", String.format("%s %02d:%02d:00", date, startSec / 3600, (startSec % 3600) / 60));
            row.put("duration", 600);
            row.put("size", f.length());
            list.add(row);
        }
        list.sort(Comparator.comparing(m -> String.valueOf(m.get("file"))));
        return list;
    }

    /** 录像文件代理播放（校验路径防穿越，支持 Range 拖动） */
    public Resource recordFile(Long cameraId, String date, String filename) {
        Map<String, Object> camera = jdbcTemplate.queryForMap("SELECT * FROM biz_video_camera WHERE id = ?", cameraId);
        String deviceNo = str(camera.get("device_no"));
        if (deviceNo.isBlank() || !deviceNo.matches("[A-Za-z0-9_\\-]+") || !date.matches("\\d{8}") || !filename.matches("seg_\\d+\\.mp4")) {
            throw new IllegalArgumentException("录像文件参数非法");
        }
        File file = new File(new File(new File(RECORD_ROOT, deviceNo), date), filename);
        if (!file.exists() || !file.isFile()) {
            throw new IllegalArgumentException("录像文件不存在: " + filename);
        }
        return new FileSystemResource(file);
    }

    private boolean hasHlsFiles(String deviceNo) {
        if (deviceNo == null || deviceNo.isBlank() || !deviceNo.matches("[A-Za-z0-9_\\-]+")) {
            return false;
        }
        return new File(HLS_ROOT, deviceNo + File.separator + "index.m3u8").exists();
    }

    private String str(Object o) {
        return o == null ? "" : String.valueOf(o).trim();
    }
}
