package com.changping.platform.modules.community.controller;

import com.changping.platform.common.response.ApiResponse;
import com.changping.platform.modules.auth.model.AuthenticatedUser;
import com.changping.platform.modules.auth.service.AuthService;
import com.changping.platform.modules.auth.service.CurrentUserService;
import com.changping.platform.modules.community.mapper.MediaUploadMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping("/media")
public class MediaUploadController {

    /**
     * 允许上传的文件扩展名白名单
     */
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            // 图片（不允许 SVG，避免存储型脚本风险）
            ".jpg", ".jpeg", ".png", ".gif", ".bmp", ".webp",
            // 文档
            ".pdf", ".doc", ".docx", ".xls", ".xlsx", ".ppt", ".pptx", ".txt", ".csv",
            // 视频
            ".mp4", ".avi", ".mov", ".wmv", ".flv", ".mkv", ".webm",
            // 音频
            ".mp3", ".wav", ".ogg", ".aac", ".flac",
            // 压缩包
            ".zip", ".rar", ".7z", ".tar", ".gz"
    );

    /**
     * 禁止上传的危险扩展名（双重检查）
     */
    private static final Set<String> BLOCKED_EXTENSIONS = Set.of(
            ".jsp", ".jspx", ".php", ".asp", ".aspx", ".exe", ".sh", ".bat", ".cmd",
            ".jar", ".war", ".class", ".py", ".rb", ".pl", ".cgi", ".htaccess", ".shtml"
    );

    private final CurrentUserService currentUserService;
    private final MediaUploadMapper mediaUploadMapper;

    @Value("${media.upload-dir:uploads/}")
    private String uploadDir;

    @Value("${media.access-url:http://localhost:8080/media/files/}")
    private String accessUrl;

    public MediaUploadController(CurrentUserService currentUserService, MediaUploadMapper mediaUploadMapper) {
        this.currentUserService = currentUserService;
        this.mediaUploadMapper = mediaUploadMapper;
    }

    @PostMapping("/upload")
    public ApiResponse<Map<String, Object>> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "businessType", required = false) String businessType,
            @RequestParam(value = "businessId", required = false) Long businessId,
            @RequestParam(value = "fileType", required = false) String fileType) {

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("文件不能为空");
        }

        // 校验文件扩展名
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();
        }

        if (extension.isEmpty()) {
            throw new IllegalArgumentException("文件必须有扩展名");
        }

        if (BLOCKED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("禁止上传该类型的文件: " + extension);
        }

        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("不支持的文件类型: " + extension + "，允许的类型: " + ALLOWED_EXTENSIONS);
        }

        // 校验 Content-Type 与扩展名是否匹配（基础检查）
        String contentType = file.getContentType();
        if (contentType != null && !contentType.startsWith("image/") && !contentType.startsWith("video/")
                && !contentType.startsWith("audio/") && !contentType.equals("application/pdf")
                && !contentType.startsWith("application/vnd.")
                && !contentType.equals("text/plain")
                && !contentType.equals("text/csv")
                && !contentType.equals("application/zip")
                && !contentType.equals("application/x-rar-compressed")
                && !contentType.equals("application/x-7z-compressed")) {
            throw new IllegalArgumentException("文件类型与内容不匹配");
        }

        try {
            AuthenticatedUser user = currentUserService.requireClientType(AuthService.ClientType.WEB);

            // 创建存储目录
            String dateDir = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
            Path storageDir = Paths.get(uploadDir, dateDir);
            Files.createDirectories(storageDir);

            // 生成文件名（UUID + 白名单内的扩展名）
            String storedFilename = UUID.randomUUID().toString().replace("-", "") + extension;

            // 保存文件
            Path targetPath = storageDir.resolve(storedFilename);
            file.transferTo(targetPath.toFile());

            // 构建访问URL
            String fileUrl = dateDir + "/" + storedFilename;
            String fullUrl = accessUrl + fileUrl;

            // 确定文件类型
            if (fileType == null) {
                if (contentType != null && contentType.startsWith("video/")) {
                    fileType = "VIDEO";
                } else if (contentType != null && contentType.startsWith("audio/")) {
                    fileType = "AUDIO";
                } else {
                    fileType = "IMAGE";
                }
            }

            // 保存媒体记录到数据库
            Map<String, Object> record = new HashMap<>();
            record.put("businessType", businessType != null ? businessType : "GENERAL");
            record.put("businessId", businessId);
            record.put("fileName", originalFilename);
            record.put("fileUrl", fileUrl);
            record.put("fileType", fileType);
            record.put("mimeType", contentType);
            record.put("status", "ACTIVE");
            record.put("uploaderUserId", user.id());
            record.put("uploaderName", user.userName());
            mediaUploadMapper.insert(record);

            // 返回响应
            Map<String, Object> response = new HashMap<>();
            response.put("id", record.get("id"));
            response.put("businessType", record.get("businessType"));
            response.put("businessId", businessId);
            response.put("fileName", originalFilename);
            response.put("fileUrl", fullUrl);
            response.put("fileType", fileType);
            response.put("mimeType", contentType);
            response.put("status", "ACTIVE");

            return ApiResponse.ok(response);

        } catch (IOException e) {
            throw new RuntimeException("文件上传失败: " + e.getMessage(), e);
        }
    }

    @GetMapping
    public ApiResponse<List<Map<String, Object>>> list(
            @RequestParam(required = false) String businessType,
            @RequestParam(required = false) Long businessId) {
        return ApiResponse.ok(mediaUploadMapper.findByBusiness(businessType, businessId));
    }
}
