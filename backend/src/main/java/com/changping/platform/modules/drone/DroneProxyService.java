package com.changping.platform.modules.drone;

import com.changping.platform.common.exception.BusinessException;
import com.changping.platform.modules.drone.client.DroneApiClient;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartFile;

/**
 * @Author tangxinglin
 * @Description //无人机代理服务，封装对无人机第三方平台API的所有代理调用，包括工作空间、设备、航线、任务、AI模型、喊话器、载荷及媒体文件等功能
 * @Date 2026/04/18 10:00
 */
@Service
public class DroneProxyService {

    private static final String WORKSPACE_LIST_PATH = "/dj-prod-api/manage/api/v1/workspaces/getWorkspaceListPageVo";
    private static final String DEVICE_LIST_PATH = "/dj-prod-api/manage/api/v1/devices/getDeviceListPageVo";
    private static final String WAYLINE_LIST_PATH =
            "/dj-prod-api/wayline/api/v1/workspaces/%s/waylines?order_by=update_time%%20desc&page=%s&page_size=%s&type=point";
    private static final String WAYLINE_POINTS_PATH = "/dj-prod-api/wayline/api/v1/workspaces/%s/getWaylinePoint/%s";
    private static final String JOB_CREATE_PATH = "/dj-prod-api/wayline/api/v1/workspaces/createImmediateJob";
    private static final String JOB_PAUSE_RESUME_PATH = "/dj-prod-api/wayline/api/v1/workspaces/pauseResumeJob";
    private static final String JOB_RETURN_HOME_PATH = "/dj-prod-api/wayline/api/v1/workspaces/returnHomeJob";
    private static final String JOB_LIST_PATH = "/dj-prod-api/wayline/api/v1/workspaces/getJobListPageVo";
    private static final String AI_MODEL_LIST_PATH = "/dj-prod-api/manage/api/v1/ai/getAlgorithmModelListPageVo";
    private static final String AI_BINDING_DETAIL_PATH = "/dj-prod-api/manage/api/v1/ai/getWayLineAiBindingDetail";
    private static final String AI_BINDING_PATH = "/dj-prod-api/manage/api/v1/ai/bindingAiAlgorithm";
    private static final String SPEAKER_FILE_LIST_PATH = "/dj-prod-api/manage/api/v1/speaker/psdk/pcmFile/getPcmFileListPageVo";
    private static final String SPEAKER_FILE_UPLOAD_PATH = "/dj-prod-api/manage/api/v1/speaker/psdk/pcmFile/upload";
    private static final String SPEAKER_FILE_DELETE_PATH = "/dj-prod-api/manage/api/v1/speaker/deleteById/%s";
    private static final String SPEAKER_PLAY_PATH = "/dj-prod-api/manage/api/v1/speaker/psdk/%s/audioPlayStart";
    private static final String SPEAKER_STOP_PATH = "/dj-prod-api/manage/api/v1/speaker/psdk/%s/stopPlay";
    private static final String SPEAKER_VOLUME_PATH = "/dj-prod-api/manage/api/v1/speaker/psdk/%s/volumeSet";
    private static final String PAYLOAD_COMMAND_PATH = "/dj-prod-api/control/api/v1/devices/%s/payload/commands";
    private static final String MEDIA_FILE_LIST_PATH = "/dj-prod-api/media/api/v1/files/%s/recursion";
    private static final String MEDIA_FILE_BY_JOB_PATH = "/dj-prod-api/media/api/v1/files/%s/getRecursionFilesByJobId/%s";

    private final DroneApiClient droneApiClient;
    private final ObjectMapper objectMapper;

    /**
     * @Author tangxinglin
     * @Description //构造函数，注入无人机API客户端及JSON序列化工具
     * @Date 2026/04/18 10:00
     * @Param [droneApiClient 无人机API客户端, objectMapper JSON序列化工具]
     * @return void
     */
    public DroneProxyService(DroneApiClient droneApiClient, ObjectMapper objectMapper) {
        this.droneApiClient = droneApiClient;
        this.objectMapper = objectMapper;
    }

    /**
     * @Author tangxinglin
     * @Description //分页查询工作空间列表
     * @Date 2026/04/18 10:00
     * @Param [page 页码, pageSize 每页条数]
     * @return PageResult<Map<String, Object>> 工作空间分页结果
     */
    public PageResult<Map<String, Object>> listWorkspaces(int page, int pageSize) {
        try {
            Map<String, Object> data = postForMap(WORKSPACE_LIST_PATH, Map.of(
                    "region_code", droneApiClient.getRegionCode(),
                    "page_num", page,
                    "page_size", pageSize));
            List<Map<String, Object>> items = extractItems(data);
            return pageResult(data, items, page, pageSize);
        } catch (Exception e) {
            return new PageResult<>(List.of(), 0, page, pageSize);
        }
    }

    /**
     * @Author tangxinglin
     * @Description //分页查询指定工作空间下的无人机设备列表
     * @Date 2026/04/18 10:00
     * @Param [workspaceId 工作空间ID, page 页码, pageSize 每页条数]
     * @return PageResult<Map<String, Object>> 设备分页结果
     */
    public PageResult<Map<String, Object>> listDevices(String workspaceId, int page, int pageSize) {
        try {
            Map<String, Object> data = postForMap(DEVICE_LIST_PATH, Map.of(
                    "workspace_id", workspaceId,
                    "page_num", page,
                    "page_size", pageSize,
                    "device_type_list", List.of(2, 3)));
            List<Map<String, Object>> items = extractItems(data);
            return pageResult(data, items, page, pageSize);
        } catch (Exception e) {
            return new PageResult<>(List.of(), 0, page, pageSize);
        }
    }

    /**
     * @Author tangxinglin
     * @Description //分页查询指定工作空间下的航线列表
     * @Date 2026/04/18 10:00
     * @Param [workspaceId 工作空间ID, page 页码, pageSize 每页条数]
     * @return PageResult<Map<String, Object>> 航线分页结果
     */
    public PageResult<Map<String, Object>> listWaylines(String workspaceId, int page, int pageSize) {
        Map<String, Object> data = getForMap(WAYLINE_LIST_PATH.formatted(workspaceId, page, pageSize));
        List<Map<String, Object>> items = extractItems(data);
        return pageResult(data, items, page, pageSize);
    }

    /**
     * @Author tangxinglin
     * @Description //获取指定工作空间和航线ID的航点坐标列表，返回经纬度坐标数组
     * @Date 2026/04/18 10:00
     * @Param [workspaceId 工作空间ID, waylineId 航线ID]
     * @return Map<String, Object> 包含waylines坐标数组的结果
     */
    public Map<String, Object> getWaylinePoints(String workspaceId, String waylineId) {
        Map<String, Object> data = getForMap(WAYLINE_POINTS_PATH.formatted(workspaceId, waylineId));
        List<List<Object>> waylines = new ArrayList<>();
        for (Map<String, Object> point : extractItems(data)) {
            Object lng = firstNonNull(point, "longitude", "lng", "lon");
            Object lat = firstNonNull(point, "latitude", "lat");
            if (lng != null && lat != null) {
                waylines.add(List.of(lng, lat));
            }
        }
        if (waylines.isEmpty()) {
            Object directLng = firstNonNull(data, "longitude", "lng", "lon");
            Object directLat = firstNonNull(data, "latitude", "lat");
            if (directLng != null && directLat != null) {
                waylines.add(List.of(directLng, directLat));
            }
        }
        return Map.of("waylines", waylines);
    }

    /**
     * @Author tangxinglin
     * @Description //分页查询指定工作空间下的任务列表，支持按状态过滤
     * @Date 2026/04/18 10:00
     * @Param [workspaceId 工作空间ID, page 页码, pageSize 每页条数, status 任务状态过滤（可选）]
     * @return PageResult<Map<String, Object>> 任务分页结果
     */
    public PageResult<Map<String, Object>> listJobs(String workspaceId, int page, int pageSize, Integer status) {
        try {
            Map<String, Object> request = new LinkedHashMap<>();
            request.put("workspace_id", workspaceId);
            request.put("page_num", page);
            request.put("page_size", pageSize);
            if (status != null) {
                request.put("status", status);
            }
            Map<String, Object> data = postForMap(JOB_LIST_PATH, request);
            List<Map<String, Object>> items = extractItems(data);
            return pageResult(data, items, page, pageSize);
        } catch (Exception e) {
            return new PageResult<>(List.of(), 0, page, pageSize);
        }
    }

    /**
     * @Author tangxinglin
     * @Description //创建无人机立即执行任务
     * @Date 2026/04/18 10:00
     * @Param [workspaceId 工作空间ID, dockSn 机巢序列号, fileId 航线文件ID]
     * @return Map<String, Object> 创建任务结果
     */
    public Map<String, Object> createJob(String workspaceId, String dockSn, String fileId) {
        return postForMap(JOB_CREATE_PATH, Map.of(
                "workspace_id", workspaceId,
                "dock_sn", dockSn,
                "file_id", fileId));
    }

    /**
     * @Author tangxinglin
     * @Description //暂停或恢复指定任务
     * @Date 2026/04/18 10:00
     * @Param [jobId 任务ID, workspaceId 工作空间ID, status 操作状态（0暂停，1恢复）]
     * @return Map<String, Object> 操作结果
     */
    public Map<String, Object> pauseResumeJob(String jobId, String workspaceId, Integer status) {
        return postForMap(JOB_PAUSE_RESUME_PATH, Map.of(
                "job_id", jobId,
                "workspace_id", workspaceId,
                "status", status));
    }

    /**
     * @Author tangxinglin
     * @Description //向指定机巢发送一键返航指令
     * @Date 2026/04/18 10:00
     * @Param [dockSn 机巢序列号]
     * @return Map<String, Object> 返航指令结果
     */
    public Map<String, Object> returnHome(String dockSn) {
        return postForMap(JOB_RETURN_HOME_PATH, Map.of("dock_sn", dockSn));
    }

    /**
     * @Author tangxinglin
     * @Description //分页查询指定工作空间的AI算法模型列表
     * @Date 2026/04/18 10:00
     * @Param [workspaceId 工作空间ID, page 页码, pageSize 每页条数]
     * @return PageResult<Map<String, Object>> AI模型分页结果
     */
    public PageResult<Map<String, Object>> listAiModels(String workspaceId, int page, int pageSize) {
        try {
            Map<String, Object> data = postForMap(AI_MODEL_LIST_PATH, Map.of(
                    "workspace_id", workspaceId,
                    "page_num", page,
                    "page_size", pageSize));
            List<Map<String, Object>> items = extractItems(data).stream()
                    .map(this::mapAiModel)
                    .toList();
            return pageResult(data, items, page, pageSize);
        } catch (Exception e) {
            return new PageResult<>(List.of(), 0, page, pageSize);
        }
    }

    /**
     * @Author tangxinglin
     * @Description //获取指定航线的AI模型绑定详情，合并普通算法绑定和千问算法绑定
     * @Date 2026/04/18 10:00
     * @Param [flyLineId 航线ID]
     * @return List<Map<String, Object>> AI模型绑定详情列表
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getWaylineAiBindingDetail(String flyLineId) {
        Map<String, Object> data = postForMap(AI_BINDING_DETAIL_PATH, Map.of("flyLineId", flyLineId, "fly_line_id", flyLineId));
        List<Map<String, Object>> result = new ArrayList<>();

        // Extract normal algorithm bindings
        Object list = data.get("list");
        if (list instanceof List<?> rawList) {
            rawList.stream()
                    .filter(item -> item instanceof Map)
                    .map(item -> (Map<String, Object>) item)
                    .forEach(result::add);
        }

        // Extract Qwen (千问) algorithm bindings
        Object qwenList = data.get("qwenList");
        if (qwenList instanceof List<?> rawQwenList) {
            rawQwenList.stream()
                    .filter(item -> item instanceof Map)
                    .map(item -> (Map<String, Object>) item)
                    .forEach(result::add);
        }

        return result;
    }

    /**
     * @Author tangxinglin
     * @Description //将千问AI模型绑定到指定航线，键名转换为snake_case后提交到上游平台
     * @Date 2026/04/18 10:00
     * @Param [flyLineId 航线ID, qwenBindings 千问模型绑定关系列表]
     * @return Map<String, Object> 绑定操作结果
     */
    public Map<String, Object> bindQwenToWayline(String flyLineId, List<Map<String, Object>> qwenBindings) {
        String userId = droneApiClient.extractUserIdFromToken();

        // Upstream uses global SNAKE_CASE naming strategy — all keys must be snake_case
        List<Map<String, Object>> snakeBindings = qwenBindings.stream().map(b -> {
            Map<String, Object> sb = new LinkedHashMap<>();
            sb.put("label", b.get("label"));
            sb.put("start_point_index", b.get("startPointIndex"));
            sb.put("end_point_index", b.get("endPointIndex"));
            sb.put("interval_second", b.get("intervalSecond"));
            return sb;
        }).toList();

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("user_id", userId);
        body.put("fly_line_id", flyLineId);
        body.put("waypoint_algorithm_list", List.of());
        body.put("qwen_algorithm_list", snakeBindings);

        return postForMap(AI_BINDING_PATH, body);
    }

    /**
     * @Author tangxinglin
     * @Description //分页查询喊话器音频文件列表
     * @Date 2026/04/18 10:00
     * @Param [page 页码, pageSize 每页条数]
     * @return PageResult<Map<String, Object>> 喊话器文件分页结果
     */
    public PageResult<Map<String, Object>> listSpeakerFiles(int page, int pageSize) {
        Map<String, Object> data = postForMap(SPEAKER_FILE_LIST_PATH, Map.of(
                "page_num", page,
                "page_size", pageSize));
        return pageResult(data, extractItems(data), page, pageSize);
    }

    /**
     * @Author tangxinglin
     * @Description //上传喊话器音频文件到上游平台
     * @Date 2026/04/18 10:00
     * @Param [file 要上传的音频文件]
     * @return Map<String, Object> 上传结果
     */
    public Map<String, Object> uploadSpeakerFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("VALIDATION_ERROR", "请选择要上传的文件");
        }
        try {
            HttpHeaders partHeaders = new HttpHeaders();
            partHeaders.setContentDisposition(ContentDisposition.formData()
                    .name("file")
                    .filename(file.getOriginalFilename())
                    .build());
            partHeaders.setContentType(MediaType.parseMediaType(
                    file.getContentType() == null ? MediaType.APPLICATION_OCTET_STREAM_VALUE : file.getContentType()));

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new HttpEntity<>(file.getBytes(), partHeaders));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            return droneApiClient.postMultipart(SPEAKER_FILE_UPLOAD_PATH, new HttpEntity<>(body, headers), Map.class);
        } catch (IOException exception) {
            throw new BusinessException("DRONE_FILE_UPLOAD_FAILED", "读取上传文件失败");
        }
    }

    /**
     * @Author tangxinglin
     * @Description //删除指定ID的喊话器音频文件
     * @Date 2026/04/18 10:00
     * @Param [id 音频文件ID]
     * @return Map<String, Object> 删除结果
     */
    public Map<String, Object> deleteSpeakerFile(String id) {
        return getForMap(SPEAKER_FILE_DELETE_PATH.formatted(id));
    }

    /**
     * @Author tangxinglin
     * @Description //控制指定设备的喊话器播放指定音频文件
     * @Date 2026/04/18 10:00
     * @Param [deviceSn 设备序列号, fileId 音频文件ID]
     * @return Map<String, Object> 播放指令结果
     */
    public Map<String, Object> playSpeaker(String deviceSn, Long fileId) {
        return postForMap(SPEAKER_PLAY_PATH.formatted(deviceSn), Map.of("id", fileId));
    }

    /**
     * @Author tangxinglin
     * @Description //停止指定设备喊话器的播放
     * @Date 2026/04/18 10:00
     * @Param [deviceSn 设备序列号]
     * @return Map<String, Object> 停止播放结果
     */
    public Map<String, Object> stopSpeaker(String deviceSn) {
        return postForMap(SPEAKER_STOP_PATH.formatted(deviceSn), Map.of());
    }

    /**
     * @Author tangxinglin
     * @Description //设置指定设备喊话器的音量
     * @Date 2026/04/18 10:00
     * @Param [deviceSn 设备序列号, volume 音量值（0-100）]
     * @return Map<String, Object> 音量设置结果
     */
    public Map<String, Object> setSpeakerVolume(String deviceSn, Integer volume) {
        return postForMap(SPEAKER_VOLUME_PATH.formatted(deviceSn), Map.of("volume", volume));
    }

    /**
     * @Author tangxinglin
     * @Description //切换指定设备的摄像头模式
     * @Date 2026/04/18 10:00
     * @Param [deviceSn 设备序列号, cameraMode 摄像头模式值]
     * @return Map<String, Object> 模式切换结果
     */
    public Map<String, Object> switchCameraMode(String deviceSn, Integer cameraMode) {
        return payloadCommand(deviceSn, Map.of(
                "cmd", "camera_mode_switch",
                "data", Map.of("camera_mode", cameraMode)));
    }

    /**
     * @Author tangxinglin
     * @Description //启动指定设备的摄像头录像
     * @Date 2026/04/18 10:00
     * @Param [deviceSn 设备序列号]
     * @return Map<String, Object> 开始录像结果
     */
    public Map<String, Object> startRecording(String deviceSn) {
        return payloadCommand(deviceSn, Map.of("cmd", "camera_recording_start"));
    }

    /**
     * @Author tangxinglin
     * @Description //停止指定设备的摄像头录像
     * @Date 2026/04/18 10:00
     * @Param [deviceSn 设备序列号]
     * @return Map<String, Object> 停止录像结果
     */
    public Map<String, Object> stopRecording(String deviceSn) {
        return payloadCommand(deviceSn, Map.of("cmd", "camera_recording_stop"));
    }

    /**
     * @Author tangxinglin
     * @Description //查询媒体文件夹列表，支持按文件名和时间范围过滤，第三方返回Map结构时转换为统一列表格式
     * @Date 2026/04/18 10:00
     * @Param [workspaceId 工作空间ID, fileName 文件名过滤（可选）, startTime 开始时间（可选）, endTime 结束时间（可选）]
     * @return List<Map<String, Object>> 媒体文件夹列表
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> listMediaFolders(String workspaceId, String fileName, String startTime, String endTime) {
        StringBuilder path = new StringBuilder(MEDIA_FILE_LIST_PATH.formatted(workspaceId));
        path.append("?fileName=").append(fileName != null ? fileName : "");
        path.append("&startTime=").append(startTime != null ? startTime : "");
        path.append("&endTime=").append(endTime != null ? endTime : "");
        Object data = droneApiClient.get(path.toString(), Object.class);
        // Third-party returns a Map<folderName, {job_id, ...}> structure
        if (data instanceof Map<?, ?> dataMap) {
            List<Map<String, Object>> result = new ArrayList<>();
            for (Map.Entry<?, ?> entry : dataMap.entrySet()) {
                String folderName = String.valueOf(entry.getKey());
                Map<String, Object> folder = new LinkedHashMap<>();
                folder.put("fileName", folderName);
                if (entry.getValue() instanceof Map<?, ?> valueMap) {
                    Map<String, Object> details = (Map<String, Object>) valueMap;
                    folder.put("jobId", stringValue(firstNonNull(details, "job_id", "jobId", "id")));
                    folder.put("createTime", firstNonNull(details, "create_time", "createTime", "created_time"));
                } else {
                    folder.put("jobId", stringValue(entry.getValue()));
                }
                folder.put("isDir", true);
                result.add(folder);
            }
            return result;
        }
        return toItemList(data).stream()
                .map(this::mapMediaFolder)
                .toList();
    }

    /**
     * @Author tangxinglin
     * @Description //根据任务ID获取指定工作空间下该任务的所有媒体文件列表
     * @Date 2026/04/18 10:00
     * @Param [workspaceId 工作空间ID, jobId 任务ID]
     * @return List<Map<String, Object>> 媒体文件列表
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getMediaFilesByJobId(String workspaceId, String jobId) {
        String path = MEDIA_FILE_BY_JOB_PATH.formatted(workspaceId, jobId);
        Object data = droneApiClient.get(path, Object.class);
        return toItemList(data).stream()
                .map(this::mapMediaFile)
                .toList();
    }

    /**
     * @Author tangxinglin
     * @Description //将任意数据对象转换为Map列表，兼容List和Map两种上游返回结构
     * @Date 2026/04/18 10:00
     * @Param [data 上游返回的原始数据]
     * @return List<Map<String, Object>> 统一的Map列表
     */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> toItemList(Object data) {
        if (data instanceof List<?> list) {
            return list.stream()
                    .filter(Map.class::isInstance)
                    .map(item -> (Map<String, Object>) item)
                    .toList();
        }
        if (data instanceof Map<?, ?> map) {
            return extractItems((Map<String, Object>) map);
        }
        return List.of();
    }

    /**
     * @Author tangxinglin
     * @Description //将原始媒体文件夹数据映射为统一的输出字段结构
     * @Date 2026/04/18 10:00
     * @Param [item 原始媒体文件夹数据]
     * @return Map<String, Object> 标准化后的媒体文件夹数据
     */
    private Map<String, Object> mapMediaFolder(Map<String, Object> item) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("jobId", stringValue(firstNonNull(item, "job_id", "jobId", "id")));
        result.put("fileName", firstNonNull(item, "file_name", "fileName", "name"));
        result.put("isDir", true);
        result.put("createTime", firstNonNull(item, "create_time", "createTime", "created_time"));
        return result;
    }

    /**
     * @Author tangxinglin
     * @Description //将原始媒体文件数据映射为统一的输出字段结构
     * @Date 2026/04/18 10:00
     * @Param [item 原始媒体文件数据]
     * @return Map<String, Object> 标准化后的媒体文件数据
     */
    private Map<String, Object> mapMediaFile(Map<String, Object> item) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", stringValue(firstNonNull(item, "id", "file_id", "fileId")));
        result.put("fileName", firstNonNull(item, "file_name", "fileName", "name"));
        result.put("fileType", firstNonNull(item, "file_type", "fileType", "type"));
        result.put("droneSn", stringValue(firstNonNull(item, "drone_sn", "droneSn", "device_sn", "drone")));
        result.put("payloadName", sanitize(firstNonNull(item, "payload_name", "payloadName", "payload_model_key", "payload")));
        result.put("createTime", firstNonNull(item, "create_time", "createTime", "created_time"));
        result.put("objectKey", sanitize(stringValue(firstNonNull(item, "object_key", "objectKey", "key"))));
        result.put("subFileType", integerValue(firstNonNull(item, "sub_file_type", "subFileType")));
        result.put("previewUrl", sanitize(firstNonNull(item, "preview_url", "previewUrl", "tiny_fingerprint", "tinyFingerprint", "tinny_fingerprint")));
        return result;
    }

    /**
     * @Author tangxinglin
     * @Description //向指定设备发送载荷控制指令
     * @Date 2026/04/18 10:00
     * @Param [deviceSn 设备序列号, body 指令请求体]
     * @return Map<String, Object> 指令执行结果
     */
    private Map<String, Object> payloadCommand(String deviceSn, Map<String, Object> body) {
        return postForMap(PAYLOAD_COMMAND_PATH.formatted(deviceSn), body);
    }

    /**
     * @Author tangxinglin
     * @Description //将原始工作空间数据映射为统一的输出字段结构
     * @Date 2026/04/18 10:00
     * @Param [item 原始工作空间数据]
     * @return Map<String, Object> 标准化后的工作空间数据
     */
    private Map<String, Object> mapWorkspace(Map<String, Object> item) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", firstNonNull(item, "id", "workspace_id"));
        result.put("workspaceId", stringValue(firstNonNull(item, "workspace_id", "workspaceId", "id")));
        result.put("workspaceName", firstNonNull(item, "workspace_name", "workspaceName", "name"));
        result.put("workspaceDesc", firstNonNull(item, "workspace_desc", "workspaceDesc", "description"));
        result.put("regionCode", firstNonNull(item, "region_code", "regionCode", "region"));
        result.put("platformName", firstNonNull(item, "platform_name", "platformName"));
        result.put("bindCode", firstNonNull(item, "bind_code", "bindCode"));
        return result;
    }

    /**
     * @Author tangxinglin
     * @Description //将原始设备数据映射为统一的输出字段结构，包含嵌套的无人机信息
     * @Date 2026/04/18 10:00
     * @Param [item 原始设备数据]
     * @return Map<String, Object> 标准化后的设备数据
     */
    private Map<String, Object> mapDevice(Map<String, Object> item) {
        Map<String, Object> droneInfoSource = nestedMap(item, "drone_info", "droneInfo", "aircraft");
        Map<String, Object> droneInfo = new LinkedHashMap<>();
        droneInfo.put("droneSn", firstNonNull(droneInfoSource, "drone_sn", "droneSn", "device_sn", "sn"));
        droneInfo.put("deviceName", firstNonNull(droneInfoSource, "device_name", "deviceName", "name"));
        droneInfo.put("modeCode", firstNonNull(droneInfoSource, "mode_code", "modeCode", "status"));
        droneInfo.put("longitude", firstNonNull(droneInfoSource, "longitude", "lng", "lon"));
        droneInfo.put("latitude", firstNonNull(droneInfoSource, "latitude", "lat"));
        droneInfo.put("videoPlayUrl", firstNonNull(droneInfoSource, "video_play_url", "videoPlayUrl", "playUrl"));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", firstNonNull(item, "id", "workspace_id"));
        result.put("workspaceId", firstNonNull(item, "workspace_id", "workspaceId"));
        result.put("workspaceName", firstNonNull(item, "workspace_name", "workspaceName"));
        result.put("deviceSn", firstNonNull(item, "device_sn", "deviceSn", "sn"));
        result.put("deviceName", firstNonNull(item, "device_name", "deviceName", "name"));
        result.put("firmwareVersion", firstNonNull(item, "firmware_version", "firmwareVersion", "version"));
        result.put("modeCode", firstNonNull(item, "mode_code", "modeCode", "status"));
        result.put("longitude", firstNonNull(item, "longitude", "lng", "lon"));
        result.put("latitude", firstNonNull(item, "latitude", "lat"));
        result.put("videoPlayUrl", firstNonNull(item, "video_play_url", "videoPlayUrl", "playUrl"));
        result.put("droneInfo", droneInfo);
        return result;
    }

    /**
     * @Author tangxinglin
     * @Description //将原始航线数据映射为统一的输出字段结构
     * @Date 2026/04/18 10:00
     * @Param [item 原始航线数据]
     * @return Map<String, Object> 标准化后的航线数据
     */
    private Map<String, Object> mapWayline(Map<String, Object> item) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", firstNonNull(item, "id", "file_id", "fileId"));
        result.put("name", firstNonNull(item, "name", "file_name", "fileName"));
        result.put("droneModelKey", firstNonNull(item, "drone_model_key", "droneModelKey", "drone_model"));
        result.put("updateTime", firstNonNull(item, "update_time", "updateTime", "updated_at"));
        return result;
    }

    /**
     * @Author tangxinglin
     * @Description //将原始任务数据映射为统一的输出字段结构
     * @Date 2026/04/18 10:00
     * @Param [item 原始任务数据]
     * @return Map<String, Object> 标准化后的任务数据
     */
    private Map<String, Object> mapJob(Map<String, Object> item) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", firstNonNull(item, "id", "job_id", "jobId"));
        result.put("jobId", stringValue(firstNonNull(item, "job_id", "jobId", "id")));
        result.put("workspaceId", stringValue(firstNonNull(item, "workspace_id", "workspaceId")));
        result.put("executeTime", firstNonNull(item, "execute_time", "executeTime", "plan_time"));
        result.put("beginTime", firstNonNull(item, "begin_time", "beginTime", "start_time"));
        result.put("status", firstNonNull(item, "status", "job_status"));
        result.put("jobName", firstNonNull(item, "job_name", "jobName", "name"));
        result.put("taskType", firstNonNull(item, "task_type", "taskType", "type"));
        result.put("fileId", stringValue(firstNonNull(item, "file_id", "fileId", "wayline_id")));
        result.put("fileName", firstNonNull(item, "file_name", "fileName", "wayline_name"));
        result.put("dockSn", stringValue(firstNonNull(item, "dock_sn", "dockSn", "device_sn")));
        result.put("dockName", firstNonNull(item, "dock_name", "dockName", "device_name"));
        result.put("usernameCn", firstNonNull(item, "username_cn", "usernameCn", "creator_name"));
        result.put("mediaCount", integerValue(firstNonNull(item, "media_count", "mediaCount")));
        result.put("uploadedCount", integerValue(firstNonNull(item, "uploaded_count", "uploadedCount")));
        return result;
    }

    /**
     * @Author tangxinglin
     * @Description //将原始AI模型数据映射为统一的输出字段结构
     * @Date 2026/04/18 10:00
     * @Param [item 原始AI模型数据]
     * @return Map<String, Object> 标准化后的AI模型数据
     */
    private Map<String, Object> mapAiModel(Map<String, Object> item) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", firstNonNull(item, "id", "model_id", "modelId"));
        result.put("name", firstNonNull(item, "name", "model_name", "modelName"));
        result.put("modelNo", firstNonNull(item, "model_no", "modelNo", "serial_no"));
        result.put("labelList", parseLabelList(firstNonNull(item, "label_list", "labelList")));
        result.put("status", firstNonNull(item, "status", "model_status"));
        result.put("latestTrainingTime", firstNonNull(item, "latest_training_time", "latestTrainingTime"));
        result.put("onlineTime", firstNonNull(item, "online_time", "onlineTime"));
        result.put("createTime", firstNonNull(item, "create_time", "createTime"));
        return result;
    }

    /**
     * @Author tangxinglin
     * @Description //解析标签列表字段，兼容List类型和JSON字符串两种格式
     * @Date 2026/04/18 10:00
     * @Param [rawValue 原始标签值]
     * @return List<Object> 解析后的标签列表
     */
    private List<Object> parseLabelList(Object rawValue) {
        if (rawValue == null) {
            return List.of();
        }
        if (rawValue instanceof List<?> list) {
            return List.copyOf(list);
        }
        String text = String.valueOf(rawValue).trim();
        if (text.isEmpty()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(text, new TypeReference<List<Object>>() {
            });
        } catch (IOException exception) {
            return List.of(text);
        }
    }

    /**
     * @Author tangxinglin
     * @Description //构建分页结果，从上游分页元数据中提取总数，若总数为0但有数据则以实际条数为准
     * @Date 2026/04/18 10:00
     * @Param [data 上游原始数据, items 已提取的数据项列表, page 当前页码, pageSize 每页条数]
     * @return PageResult<Map<String, Object>> 分页结果
     */
    private PageResult<Map<String, Object>> pageResult(Map<String, Object> data, List<Map<String, Object>> items, int page, int pageSize) {
        Map<String, Object> pagination = nestedMap(data, "pagination", "page", "pager");
        long total = longValue(firstNonNull(pagination, "total", "total_count", "count", "records"));
        if (total == 0 && !items.isEmpty()) {
            total = items.size();
        }
        return new PageResult<>(items, total, page, pageSize);
    }

    /**
     * @Author tangxinglin
     * @Description //从上游响应数据中提取数据项列表，兼容多种键名（items/list/records等）
     * @Date 2026/04/18 10:00
     * @Param [data 上游原始Map数据]
     * @return List<Map<String, Object>> 提取的数据项列表
     */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> extractItems(Map<String, Object> data) {
        Object value = firstNonNull(data, "items", "list", "records", "content", "waylines", "points", "data", "jobFiles");
        if (value instanceof List<?> list) {
            return list.stream()
                    .filter(Map.class::isInstance)
                    .map(item -> (Map<String, Object>) item)
                    .toList();
        }
        if (value instanceof Map<?, ?> singleMap) {
            return List.of((Map<String, Object>) singleMap);
        }
        return List.of();
    }

    /**
     * @Author tangxinglin
     * @Description //从源Map中按多个候选键名提取嵌套Map对象
     * @Date 2026/04/18 10:00
     * @Param [source 源Map, keys 候选键名列表]
     * @return Map<String, Object> 嵌套的Map对象，未找到时返回空Map
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> nestedMap(Map<String, Object> source, String... keys) {
        Object value = firstNonNull(source, keys);
        return value instanceof Map<?, ?> map ? (Map<String, Object>) map : Map.of();
    }

    /**
     * @Author tangxinglin
     * @Description //从源Map中按多个候选键名顺序取第一个非null值
     * @Date 2026/04/18 10:00
     * @Param [source 源Map, keys 候选键名列表]
     * @return Object 第一个非null值，全为null时返回null
     */
    private Object firstNonNull(Map<String, Object> source, String... keys) {
        if (source == null) {
            return null;
        }
        for (String key : keys) {
            if (source.containsKey(key) && source.get(key) != null) {
                return source.get(key);
            }
        }
        return null;
    }

    /**
     * @Author tangxinglin
     * @Description //以POST方式调用上游API并将响应反序列化为Map
     * @Date 2026/04/18 10:00
     * @Param [path 接口路径, request 请求体]
     * @return Map<String, Object> 响应数据Map
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> postForMap(String path, Object request) {
        Object result = droneApiClient.post(path, request, Object.class);
        if (result instanceof Map) {
            return (Map<String, Object>) result;
        }
        return result == null ? Map.of() : Map.of("result", result);
    }

    /**
     * @Author tangxinglin
     * @Description //以GET方式调用上游API并将响应反序列化为Map
     * @Date 2026/04/18 10:00
     * @Param [path 接口路径]
     * @return Map<String, Object> 响应数据Map
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> getForMap(String path) {
        Object result = droneApiClient.get(path, Object.class);
        if (result instanceof Map) {
            return (Map<String, Object>) result;
        }
        return result == null ? Map.of() : Map.of("result", result);
    }

    /**
     * @Author tangxinglin
     * @Description //对null源Map做空Map保护，避免NPE
     * @Date 2026/04/18 10:00
     * @Param [source 源Map]
     * @return Map<String, Object> 原Map或空Map
     */
    private Map<String, Object> nullableMap(Map<String, Object> source) {
        return source == null ? Map.of() : source;
    }

    /**
     * @Author tangxinglin
     * @Description //将任意值转换为字符串，null值返回null
     * @Date 2026/04/18 10:00
     * @Param [value 原始值]
     * @return String 字符串值或null
     */
    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    /** 将上游平台返回的 "undefined"、"null" 字符串及空字符串统一归为 null */
    private Object sanitize(Object value) {
        if (value == null) return null;
        String s = String.valueOf(value).trim();
        if (s.isEmpty() || "undefined".equalsIgnoreCase(s) || "null".equalsIgnoreCase(s)) return null;
        return value;
    }

    /**
     * @Author tangxinglin
     * @Description //将任意值转换为long类型，null或格式错误时返回0
     * @Date 2026/04/18 10:00
     * @Param [value 原始值]
     * @return long 转换后的long值
     */
    private long longValue(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value == null) {
            return 0L;
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException exception) {
            return 0L;
        }
    }

    /**
     * @Author tangxinglin
     * @Description //将任意值转换为Integer类型，null或格式错误时返回null
     * @Date 2026/04/18 10:00
     * @Param [value 原始值]
     * @return Integer 转换后的Integer值或null
     */
    private Integer integerValue(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value == null) {
            return null;
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    public record PageResult<T>(List<T> items, long total, int page, int pageSize) {
    }
}
