package com.changping.platform.modules.video.controller;

import com.changping.platform.common.response.ApiResponse;
import com.changping.platform.modules.auth.security.PermissionCodes;
import com.changping.platform.modules.auth.security.PermissionGuard;
import com.changping.platform.modules.auth.service.AuthService;
import com.changping.platform.modules.auth.service.CurrentUserService;
import com.changping.platform.modules.video.service.VideoCameraService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 固定摄像头视频接入/轮巡（D）：监控点位台账 CRUD + 播放流地址 + HLS 转流代理
 * 轮巡为前端逻辑（定时切换点位），本模块提供点位数据与流地址底座。
 */
@RestController
@RequestMapping("/video")
public class VideoSurveillanceController {

    private final VideoCameraService videoCameraService;
    private final CurrentUserService currentUserService;
    private final PermissionGuard permissionGuard;

    public VideoSurveillanceController(VideoCameraService videoCameraService,
                                       CurrentUserService currentUserService,
                                       PermissionGuard permissionGuard) {
        this.videoCameraService = videoCameraService;
        this.currentUserService = currentUserService;
        this.permissionGuard = permissionGuard;
    }

    /** 监控点位分页列表（keyword/gridId/status 筛选） */
    @GetMapping("/cameras")
    public ApiResponse<Map<String, Object>> cameras(@RequestParam(required = false) String keyword,
                                                    @RequestParam(required = false) Long gridId,
                                                    @RequestParam(required = false) String status,
                                                    @RequestParam(defaultValue = "1") int page,
                                                    @RequestParam(defaultValue = "20") int size) {
        requireVideoPermission(PermissionCodes.API_VIDEO_CAMERA_LIST);
        return ApiResponse.ok(videoCameraService.listCameras(keyword, gridId, status, page, size));
    }

    /** 点位统计 */
    @GetMapping("/cameras/statistics")
    public ApiResponse<Map<String, Object>> statistics() {
        requireVideoPermission(PermissionCodes.API_VIDEO_CAMERA_LIST);
        return ApiResponse.ok(videoCameraService.statistics());
    }

    /** 新增监控点位 */
    @PostMapping("/cameras")
    public ApiResponse<Map<String, Object>> createCamera(@RequestBody Map<String, Object> body) {
        requireVideoPermission(PermissionCodes.API_VIDEO_CAMERA_MANAGE);
        Long id = videoCameraService.createCamera(body);
        return ApiResponse.ok(Map.of("id", id));
    }

    /** 更新监控点位（未传字段保留原值） */
    @PutMapping("/cameras/{id}")
    public ApiResponse<Void> updateCamera(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        requireVideoPermission(PermissionCodes.API_VIDEO_CAMERA_MANAGE);
        videoCameraService.updateCamera(id, body);
        return ApiResponse.ok(null);
    }

    /** 删除监控点位 */
    @DeleteMapping("/cameras/{id}")
    public ApiResponse<Void> deleteCamera(@PathVariable Long id) {
        requireVideoPermission(PermissionCodes.API_VIDEO_CAMERA_MANAGE);
        videoCameraService.deleteCamera(id);
        return ApiResponse.ok(null);
    }

    /** 获取监控视频流地址（HLS/FLV 直出，RTSP 返回平台转流代理） */
    @GetMapping("/cameras/{id}/stream")
    public ApiResponse<Map<String, Object>> getStream(@PathVariable Long id) {
        requireVideoPermission(PermissionCodes.API_VIDEO_CAMERA_LIST);
        return ApiResponse.ok(videoCameraService.getStream(id));
    }

    /** HLS 转流代理：m3u8 播放列表（RTSP 点位经宿主机 ffmpeg 转流后由此提供） */
    @GetMapping("/stream/{id}/index.m3u8")
    public ResponseEntity<Resource> hlsPlaylist(@PathVariable Long id) {
        requireVideoPermission(PermissionCodes.API_VIDEO_CAMERA_LIST);
        Resource resource = videoCameraService.streamFile(id, "index.m3u8");
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/vnd.apple.mpegurl"))
                .header(HttpHeaders.CACHE_CONTROL, "no-cache")
                .body(resource);
    }

    /** HLS 转流代理：TS 视频分片 */
    @GetMapping("/stream/{id}/{segment}.ts")
    public ResponseEntity<Resource> hlsSegment(@PathVariable Long id, @PathVariable String segment) {
        requireVideoPermission(PermissionCodes.API_VIDEO_CAMERA_LIST);
        Resource resource = videoCameraService.streamFile(id, segment + ".ts");
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("video/mp2t"))
                .header(HttpHeaders.CACHE_CONTROL, "no-cache")
                .body(resource);
    }

    /** 录像回放：有录像的日期列表（近 7 天） */
    @GetMapping("/cameras/{id}/records/dates")
    public ApiResponse<List<String>> recordDates(@PathVariable Long id) {
        requireVideoPermission(PermissionCodes.API_VIDEO_CAMERA_LIST);
        return ApiResponse.ok(videoCameraService.listRecordDates(id));
    }

    /** 录像回放：指定日期（yyyyMMdd）的录像分段列表 */
    @GetMapping("/cameras/{id}/records")
    public ApiResponse<List<Map<String, Object>>> records(@PathVariable Long id,
                                                          @RequestParam String date) {
        requireVideoPermission(PermissionCodes.API_VIDEO_CAMERA_LIST);
        return ApiResponse.ok(videoCameraService.listRecords(id, date));
    }

    /** 录像回放：mp4 分段文件播放（支持 Range 拖动） */
    @GetMapping("/record/{id}/{date}/{file}")
    public ResponseEntity<Resource> recordPlay(@PathVariable Long id,
                                               @PathVariable String date,
                                               @PathVariable String file) {
        requireVideoPermission(PermissionCodes.API_VIDEO_CAMERA_LIST);
        Resource resource = videoCameraService.recordFile(id, date, file);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("video/mp4"))
                .header(HttpHeaders.CACHE_CONTROL, "no-cache")
                .body(resource);
    }

    /** 外部视频源列表（对接第三方平台，演示数据） */
    @GetMapping("/external-sources")
    public ApiResponse<List<Map<String, Object>>> externalSources() {
        requireVideoPermission(PermissionCodes.API_VIDEO_CAMERA_LIST);
        List<Map<String, Object>> sources = List.of(
            Map.of("id", 1, "name", "无人机实时图传", "type", "DRONE", "status", "ONLINE",
                   "wsUrl", "ws://drone.kfktec.cn:9080/ws/drone/live", "thumbnail", "/api/drone/thumbnail"),
            Map.of("id", 2, "name", "社区监控平台", "type", "CCTV", "status", "ONLINE",
                   "url", "http://flight.scities.net.cn:9080/video", "thumbnail", "/api/video/thumbnail"),
            Map.of("id", 3, "name", "AI分析平台", "type", "AI", "status", "ONLINE",
                   "url", "http://flight.scities.net.cn:9080/ai/stream", "thumbnail", "/api/ai/thumbnail")
        );
        return ApiResponse.ok(sources);
    }

    private void requireVideoPermission(String code) {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(code);
    }
}
