package com.changping.platform.modules.drone.controller;

import com.changping.platform.common.response.ApiResponse;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/video")
public class VideoSurveillanceController {

    /**
     * 监控点位列表（对接外部视频平台数据）
     */
    @GetMapping("/cameras")
    public ApiResponse<List<Map<String, Object>>> cameras() {
        // 返回监控点位数据（可从外部视频平台API获取）
        List<Map<String, Object>> cameras = Arrays.asList(
            createCamera(1, "社区入口摄像头", "FIXED", 113.939500, 22.971200, "社区主入口", "ACTIVE"),
            createCamera(2, "学校门口摄像头", "FIXED", 113.940000, 22.972000, "拔蛟窝学校门口", "ACTIVE"),
            createCamera(3, "市场监控", "PTZ", 113.941000, 22.973000, "建材城市场", "ACTIVE"),
            createCamera(4, "小区出入口", "FIXED", 113.938000, 22.970500, "龙景小区大门", "ACTIVE"),
            createCamera(5, "道路监控", "FIXED", 113.942000, 22.974000, "社区内部道路", "OFFLINE"),
            createCamera(6, "停车场监控", "PTZ", 113.939000, 22.971500, "A区停车场", "ACTIVE")
        );
        return ApiResponse.ok(cameras);
    }

    /**
     * 获取监控视频流地址
     */
    @GetMapping("/cameras/{id}/stream")
    public ApiResponse<Map<String, Object>> getStream(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        result.put("cameraId", id);
        result.put("streamUrl", "rtmp://drone.kfktec.cn:1935/live/camera_" + id);
        result.put("wsUrl", "ws://drone.kfktec.cn:9080/ws/video/" + id);
        return ApiResponse.ok(result);
    }

    /**
     * 外部视频源列表（对接第三方平台）
     */
    @GetMapping("/external-sources")
    public ApiResponse<List<Map<String, Object>>> externalSources() {
        List<Map<String, Object>> sources = Arrays.asList(
            Map.of("id", 1, "name", "无人机实时图传", "type", "DRONE", "status", "ONLINE",
                   "wsUrl", "ws://drone.kfktec.cn:9080/ws/drone/live", "thumbnail", "/api/drone/thumbnail"),
            Map.of("id", 2, "name", "社区监控平台", "type", "CCTV", "status", "ONLINE",
                   "url", "http://flight.scities.net.cn:9080/video", "thumbnail", "/api/video/thumbnail"),
            Map.of("id", 3, "name", "AI分析平台", "type", "AI", "status", "ONLINE",
                   "url", "http://flight.scities.net.cn:9080/ai/stream", "thumbnail", "/api/ai/thumbnail")
        );
        return ApiResponse.ok(sources);
    }

    private Map<String, Object> createCamera(int id, String name, String type, double lng, double lat, String address, String status) {
        Map<String, Object> camera = new HashMap<>();
        camera.put("id", id);
        camera.put("cameraName", name);
        camera.put("cameraType", type);
        camera.put("longitude", lng);
        camera.put("latitude", lat);
        camera.put("address", address);
        camera.put("status", status);
        camera.put("streamUrl", "rtmp://drone.kfktec.cn:1935/live/camera_" + id);
        return camera;
    }
}
