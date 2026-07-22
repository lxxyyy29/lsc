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
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/media")
public class MediaUploadController {

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

        try {
            AuthenticatedUser user = currentUserService.requireClientType(AuthService.ClientType.WEB);

            // 创建存储目录
            String dateDir = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
            Path storageDir = Paths.get(uploadDir, dateDir);
            Files.createDirectories(storageDir);

            // 生成文件名
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String storedFilename = UUID.randomUUID().toString().replace("-", "") + extension;

            // 保存文件
            Path targetPath = storageDir.resolve(storedFilename);
            file.transferTo(targetPath.toFile());

            // 构建访问URL
            String fileUrl = dateDir + "/" + storedFilename;
            String fullUrl = accessUrl + fileUrl;

            // 确定文件类型
            if (fileType == null) {
                String contentType = file.getContentType();
                if (contentType != null && contentType.startsWith("video/")) {
                    fileType = "VIDEO";
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
            record.put("mimeType", file.getContentType());
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
            response.put("mimeType", file.getContentType());
            response.put("status", "ACTIVE");

            return ApiResponse.ok(response);

        } catch (IOException e) {
            throw new RuntimeException("文件上传失败: " + e.getMessage(), e);
        }
    }

    @GetMapping
    public ApiResponse<java.util.List<Map<String, Object>>> list(
            @RequestParam(required = false) String businessType,
            @RequestParam(required = false) Long businessId) {
        return ApiResponse.ok(mediaUploadMapper.findByBusiness(businessType, businessId));
    }
}
