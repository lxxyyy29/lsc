package com.changping.platform.modules.oss.controller;

import com.changping.platform.common.exception.BusinessException;
import com.changping.platform.common.response.ApiResponse;
import com.changping.platform.modules.auth.service.AuthService;
import com.changping.platform.modules.auth.service.CurrentUserService;
import com.changping.platform.modules.oss.config.OssProperties;
import com.changping.platform.modules.oss.service.OssService;
import jakarta.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * @Author lxy
 * @Description //文件上传控制器，在 oss.enable=true 时提供文件上传、访问前缀获取、删除及下载接口
 * @Date 2026/04/18 10:18
 */
@RestController
@RequestMapping("/upload")
@ConditionalOnProperty(prefix = "oss", name = "enable", havingValue = "true")
public class UploadController {

    private final OssService ossService;
    private final OssProperties ossProperties;
    private final CurrentUserService currentUserService;

    /**
     * @Author lxy
     * @Description //构造函数，注入 OSS 服务、OSS 配置属性和当前用户服务
     * @Date 2026/04/18 10:18
     * @Param [ossService OSS 服务, ossProperties OSS 配置属性, currentUserService 当前用户服务]
     * @return
     */
    public UploadController(OssService ossService, OssProperties ossProperties, CurrentUserService currentUserService) {
        this.ossService = ossService;
        this.ossProperties = ossProperties;
        this.currentUserService = currentUserService;
    }

    /**
     * @Author lxy
     * @Description //上传文件到 OSS，返回对象名称、原始文件名和文件大小
     * @Date 2026/04/18 10:18
     * @Param [file 上传的文件, bizType 业务类型，默认 common, clientType 客户端类型，用于校验]
     * @return ApiResponse<Map<String, String>> 包含 objectName、name、size 的响应
     */
    @PostMapping
    public ApiResponse<Map<String, String>> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "bizType", defaultValue = "common") String bizType,
            @RequestParam(value = "clientType", required = false) String clientType) {
        requireSupportedClient(clientType);
        if (file == null || file.isEmpty()) {
            throw new BusinessException("VALIDATION_ERROR", "请选择要上传的文件");
        }
        String objectName = ossService.uploadFile(file, bizType);
        Map<String, String> result = new LinkedHashMap<>();
        result.put("objectName", objectName);
        result.put("name", file.getOriginalFilename());
        result.put("size", String.valueOf(file.getSize()));
        return ApiResponse.ok(result);
    }

    /**
     * @Author lxy
     * @Description //获取 OSS 文件公开访问前缀 URL
     * @Date 2026/04/18 10:18
     * @Param [clientType 客户端类型，用于校验]
     * @return ApiResponse<Map<String, String>> 包含 accessPrefix 的响应
     */
    @GetMapping("/access-prefix")
    public ApiResponse<Map<String, String>> accessPrefix(
            @RequestParam(value = "clientType", required = false) String clientType) {
        requireSupportedClient(clientType);
        String access = ossProperties.getAccess();
        if (StringUtils.hasText(access) && !access.endsWith("/")) {
            access += "/";
        }
        Map<String, String> result = new LinkedHashMap<>();
        result.put("accessPrefix", access != null ? access : "");
        return ApiResponse.ok(result);
    }

    /**
     * @Author lxy
     * @Description //删除 OSS 中的指定文件
     * @Date 2026/04/18 10:18
     * @Param [fileUrl 文件访问 URL, clientType 客户端类型，用于校验]
     * @return ApiResponse<Boolean> 删除是否成功
     */
    @DeleteMapping
    public ApiResponse<Boolean> delete(
            @RequestParam("url") String fileUrl,
            @RequestParam(value = "clientType", required = false) String clientType) {
        requireSupportedClient(clientType);
        return ApiResponse.ok(ossService.deleteFile(fileUrl));
    }

    /**
     * @Author lxy
     * @Description //代理下载 OSS 文件，将文件流写入 HTTP 响应，支持自定义文件名
     * @Date 2026/04/18 10:18
     * @Param [fileUrl 文件访问 URL, fileName 下载文件名（可选）, clientType 客户端类型，用于校验, response HTTP 响应对象]
     * @return void
     */
    @GetMapping("/download")
    public void download(
            @RequestParam("url") String fileUrl,
            @RequestParam(value = "fileName", required = false) String fileName,
            @RequestParam(value = "clientType", required = false) String clientType,
            HttpServletResponse response) {
        requireSupportedClient(clientType);
        validateDownloadUrl(fileUrl);
        try {
            URL url = new URL(fileUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(30000);

            String resolvedFileName = StringUtils.hasText(fileName)
                    ? fileName
                    : fileUrl.substring(fileUrl.lastIndexOf('/') + 1);

            response.setContentType("application/octet-stream");
            response.setHeader(
                    "Content-Disposition",
                    "attachment; filename=\"" + URLEncoder.encode(resolvedFileName, StandardCharsets.UTF_8) + "\"");

            try (InputStream inputStream = connection.getInputStream();
                    OutputStream outputStream = response.getOutputStream()) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
                outputStream.flush();
            }
        } catch (Exception exception) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * @Author lxy
     * @Description //根据 clientType 参数校验当前请求的客户端类型是否匹配
     * @Date 2026/04/18 10:18
     * @Param [clientType 客户端类型字符串，H5 或 WEB]
     * @return void
     */
    private void requireSupportedClient(String clientType) {
        AuthService.ClientType resolved = "H5".equalsIgnoreCase(clientType)
                ? AuthService.ClientType.H5
                : AuthService.ClientType.WEB;
        currentUserService.requireClientType(resolved);
    }

    /**
     * @Author lxy
     * @Description //校验下载 URL 是否属于允许的访问前缀，防止越权下载
     * @Date 2026/04/18 10:18
     * @Param [fileUrl 待校验的文件访问 URL]
     * @return void
     */
    private void validateDownloadUrl(String fileUrl) {
        String access = ossProperties.getAccess();
        if (!StringUtils.hasText(access) || !StringUtils.hasText(fileUrl)) {
            throw new BusinessException("OSS_DOWNLOAD_FORBIDDEN", "不允许下载该文件");
        }
        try {
            URI accessUri = URI.create(access.endsWith("/") ? access : access + "/");
            URI fileUri = URI.create(fileUrl);
            if (!sameText(accessUri.getScheme(), fileUri.getScheme())
                    || !sameText(accessUri.getHost(), fileUri.getHost())
                    || effectivePort(accessUri) != effectivePort(fileUri)) {
                throw new BusinessException("OSS_DOWNLOAD_FORBIDDEN", "不允许下载该文件");
            }
            String allowedPath = accessUri.getPath() == null ? "/" : accessUri.getPath();
            if (!allowedPath.endsWith("/")) {
                allowedPath += "/";
            }
            String filePath = fileUri.getPath() == null ? "" : fileUri.getPath();
            if (!filePath.startsWith(allowedPath)) {
                throw new BusinessException("OSS_DOWNLOAD_FORBIDDEN", "不允许下载该文件");
            }
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BusinessException("OSS_DOWNLOAD_FORBIDDEN", "不允许下载该文件");
        }
    }

    private boolean sameText(String left, String right) {
        return left != null && left.equalsIgnoreCase(right);
    }

    private int effectivePort(URI uri) {
        if (uri.getPort() >= 0) {
            return uri.getPort();
        }
        if ("https".equalsIgnoreCase(uri.getScheme())) {
            return 443;
        }
        if ("http".equalsIgnoreCase(uri.getScheme())) {
            return 80;
        }
        return -1;
    }
}
