package com.changping.platform.modules.drone;

import com.changping.platform.common.response.ApiResponse;
import com.changping.platform.modules.auth.security.PermissionCodes;
import com.changping.platform.modules.auth.security.PermissionGuard;
import com.changping.platform.modules.auth.service.AuthService;
import com.changping.platform.modules.auth.service.CurrentUserService;
import com.changping.platform.modules.drone.config.DroneApiProperties;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author tangxinglin
 * @Description //无人机媒体文件控制器，提供媒体文件夹列表查询、按任务ID获取媒体文件及打包下载接口
 * @Date 2026/04/18 10:00
 */
@RestController
@RequestMapping("/drone/media")
public class DroneMediaController {

    private static final Logger log = LoggerFactory.getLogger(DroneMediaController.class);

    private final DroneProxyService droneProxyService;
    private final CurrentUserService currentUserService;
    private final PermissionGuard permissionGuard;
    private final DroneApiProperties droneApiProperties;

    /**
     * @Author tangxinglin
     * @Description //构造函数，注入无人机代理服务、当前用户服务、权限守卫及API配置属性
     * @Date 2026/04/18 10:00
     * @Param [droneProxyService 无人机代理服务, currentUserService 当前用户服务, permissionGuard 权限守卫, droneApiProperties 无人机API配置]
     * @return void
     */
    public DroneMediaController(
            DroneProxyService droneProxyService,
            CurrentUserService currentUserService,
            PermissionGuard permissionGuard,
            DroneApiProperties droneApiProperties) {
        this.droneProxyService = droneProxyService;
        this.currentUserService = currentUserService;
        this.permissionGuard = permissionGuard;
        this.droneApiProperties = droneApiProperties;
    }

    /**
     * @Author tangxinglin
     * @Description //查询媒体文件夹列表，支持按文件名、时间范围过滤
     * @Date 2026/04/18 10:00
     * @Param [fileName 文件名过滤（可选）, startTime 开始时间（可选）, endTime 结束时间（可选）]
     * @return ApiResponse<List<Map<String, Object>>> 媒体文件夹列表
     */
    @GetMapping("/files")
    public ApiResponse<List<Map<String, Object>>> listMediaFolders(
            @RequestParam(required = false) String fileName,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.API_DRONE_MEDIA_LIST);
        return ApiResponse.ok(droneProxyService.listMediaFolders(
                droneApiProperties.getFixedWorkspaceId(), fileName, startTime, endTime));
    }

    /**
     * @Author tangxinglin
     * @Description //根据任务ID获取该任务下的所有媒体文件列表
     * @Date 2026/04/18 10:00
     * @Param [jobId 任务ID]
     * @return ApiResponse<List<Map<String, Object>>> 媒体文件列表
     */
    @GetMapping("/files/{jobId}")
    public ApiResponse<List<Map<String, Object>>> getMediaFilesByJobId(@PathVariable String jobId) {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.API_DRONE_MEDIA_FILES);
        return ApiResponse.ok(droneProxyService.getMediaFilesByJobId(
                droneApiProperties.getFixedWorkspaceId(), jobId));
    }

    /**
     * 将指定任务（jobId）的所有媒体文件打包为 ZIP 下载。
     * 从各文件的 objectKey（MinIO 完整 URL）逐个拉取流，在内存中压缩后输出，无临时文件落盘。
     */
    @GetMapping(value = "/download/{jobId}", produces = "application/octet-stream")
    public void downloadJobFiles(
            @PathVariable String jobId,
            HttpServletResponse response) throws IOException {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.API_DRONE_MEDIA_FILES);

        List<Map<String, Object>> files = droneProxyService.getMediaFilesByJobId(
                droneApiProperties.getFixedWorkspaceId(), jobId);

        String zipName = URLEncoder.encode(jobId + ".zip", StandardCharsets.UTF_8);
        response.setContentType("application/octet-stream");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-Disposition", "attachment;filename=" + zipName);

        try (ZipOutputStream zipOut = new ZipOutputStream(response.getOutputStream())) {
            for (Map<String, Object> file : files) {
                Object objectKeyObj = file.get("objectKey");
                Object fileNameObj = file.get("fileName");
                if (objectKeyObj == null || fileNameObj == null) continue;

                String objectKey = objectKeyObj.toString();
                String fileName = fileNameObj.toString();

                try {
                    HttpURLConnection conn = (HttpURLConnection) new URL(objectKey).openConnection();
                    conn.setConnectTimeout(10_000);
                    conn.setReadTimeout(120_000);
                    conn.setRequestProperty("User-Agent", "dgcp-oa-proxy/1.0");

                    zipOut.putNextEntry(new ZipEntry(fileName));
                    try (InputStream in = conn.getInputStream()) {
                        byte[] buf = new byte[8192];
                        int len;
                        while ((len = in.read(buf)) > 0) {
                            zipOut.write(buf, 0, len);
                        }
                    }
                    zipOut.closeEntry();
                } catch (Exception e) {
                    log.warn("跳过无法下载的文件: {} — {}", fileName, e.getMessage());
                }
            }
        }
    }
}
