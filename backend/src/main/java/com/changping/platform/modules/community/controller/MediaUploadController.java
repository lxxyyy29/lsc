package com.changping.platform.modules.community.controller;

import com.changping.platform.common.response.ApiResponse;
import com.changping.platform.modules.auth.model.AuthenticatedUser;
import com.changping.platform.modules.auth.security.AuthenticatedUserContextHolder;
import com.changping.platform.modules.auth.service.AuthService;
import com.changping.platform.modules.auth.service.CurrentUserService;
import com.changping.platform.modules.community.mapper.MediaUploadMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;

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
            // 允许 WEB 与 H5 两种客户端类型上传（网格员巡查签到、居民上报等移动端场景需要上传照片）
            AuthenticatedUser user = AuthenticatedUserContextHolder.getOptional()
                    .orElseThrow(() -> new com.changping.platform.common.exception.BusinessException("AUTH_TOKEN_REQUIRED", "请提供认证令牌"));
            if (!AuthService.ClientType.WEB.name().equals(user.clientType())
                    && !AuthService.ClientType.H5.name().equals(user.clientType())) {
                throw new com.changping.platform.common.exception.BusinessException("AUTH_CLIENT_TYPE_FORBIDDEN", "认证令牌不适用于该客户端类型");
            }

            // 扉平存储：直接存到 uploads 根目录（文件名已是 UUID，不会冲突）。
            // 注：不能用 yyyy/MM/dd 日期子目录，Spring 6 静态资源解析出于安全考虑
            // 会拦截 yyyy-MM-dd 形式的 URL 路径段，导致文件永远无法通过 HTTP 访问。
            Path storageDir = Paths.get(uploadDir);
            Files.createDirectories(storageDir);

            // 生成文件名（UUID + 白名单内的扩展名）
            String storedFilename = UUID.randomUUID().toString().replace("-", "") + extension;

            // 保存文件
            Path targetPath = storageDir.resolve(storedFilename);
            file.transferTo(targetPath.toFile());

            // 构建访问URL（通过 /media/files/{filename} 接口读取，兼容旧日期子目录中的存量文件）
            String fileUrl = storedFilename;
            String fullUrl = accessUrl + storedFilename;

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

    /**
     * 文件读取接口：按文件名返回上传文件。
     * 背景：Spring 6 静态资源解析会拦截 yyyy-MM-dd 形式的 URL 路径段，
     * 旧版按日期子目录存储的文件无法通过 /media/files/** 资源映射访问，
     * 故改用本接口按文件名查找（先在根目录找，再搜日期子目录兼容存量文件）。
     */
    @GetMapping("/files/{filename:.+}")
    public ResponseEntity<byte[]> serveFile(@PathVariable String filename) {
        // 严格文件名白名单：UUID(32位16进制)+扩展名，或带 yyyy/MM/dd 相对路径形式的存量文件名
        if (!filename.matches("[A-Za-z0-9_-]+\\.[A-Za-z0-9]+")) {
            return ResponseEntity.badRequest().build();
        }
        try {
            Path root = Paths.get(uploadDir);
            Path direct = root.resolve(filename).normalize();
            if (!direct.startsWith(root) || !Files.isRegularFile(direct)) {
                // 兼容存量文件：在日期子目录中查找
                try (Stream<Path> stream = Files.walk(root, 4)) {
                    direct = stream.filter(Files::isRegularFile)
                            .filter(p -> p.getFileName().toString().equals(filename))
                            .findFirst().orElse(null);
                }
            }
            if (direct == null || !Files.isRegularFile(direct)) {
                return ResponseEntity.notFound().build();
            }
            byte[] content = Files.readAllBytes(direct);
            String ext = filename.substring(filename.lastIndexOf('.')).toLowerCase();
            MediaType mediaType = switch (ext) {
                case ".png" -> MediaType.IMAGE_PNG;
                case ".gif" -> MediaType.IMAGE_GIF;
                case ".webp", ".bmp" -> MediaType.APPLICATION_OCTET_STREAM;
                case ".pdf" -> MediaType.APPLICATION_PDF;
                case ".mp4" -> MediaType.parseMediaType("video/mp4");
                default -> MediaType.IMAGE_JPEG;
            };
            return ResponseEntity.ok()
                    .contentType(mediaType)
                    .header(HttpHeaders.CACHE_CONTROL, "public, max-age=86400")
                    .body(content);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
