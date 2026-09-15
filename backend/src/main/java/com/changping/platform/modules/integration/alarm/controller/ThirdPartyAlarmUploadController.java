package com.changping.platform.modules.integration.alarm.controller;

import com.changping.platform.common.exception.BusinessException;
import com.changping.platform.modules.integration.alarm.config.AlarmIntegrationProperties;
import com.changping.platform.modules.integration.alarm.security.ThirdPartyCallbackVerifier;
import com.changping.platform.modules.oss.config.OssProperties;
import com.changping.platform.modules.oss.service.OssService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * @Author lxy
 * @Description //板端违停抓拍图上传控制器。
 * 板端不具备平台用户登录态，无法调用 /upload、/media/upload 等需登录令牌的接口，
 * 故提供本接口：仅使用回调令牌（X-Callback-Token）鉴权，上传成功返回可直接访问的图片 URL，
 * 供板端作为 evidenceReferences 随告警事件一并推送。
 * @Date 2026/09/15 10:00
 */
@RestController
@RequestMapping("/integrations/alarms")
@ConditionalOnProperty(prefix = "oss", name = "enable", havingValue = "true")
public class ThirdPartyAlarmUploadController {

    /** 上传到 OSS 的对象路径前缀，与告警证据区分便于运维检索 */
    private static final String BIZ_TYPE = "alarm-evidence";

    private final ThirdPartyCallbackVerifier thirdPartyCallbackVerifier;
    private final AlarmIntegrationProperties properties;
    private final OssService ossService;
    private final OssProperties ossProperties;

    public ThirdPartyAlarmUploadController(
            ThirdPartyCallbackVerifier thirdPartyCallbackVerifier,
            AlarmIntegrationProperties properties,
            OssService ossService,
            OssProperties ossProperties) {
        this.thirdPartyCallbackVerifier = thirdPartyCallbackVerifier;
        this.properties = properties;
        this.ossService = ossService;
        this.ossProperties = ossProperties;
    }

    /**
     * 板端上传抓拍图，返回公网可访问的绝对 URL。
     * 响应结构与告警回调保持一致（code 为整型 200），便于板端统一解析。
     */
    @PostMapping("/upload")
    public Map<String, Object> upload(
            @RequestParam(value = "files", required = false) MultipartFile[] files,
            HttpServletRequest request) {
        thirdPartyCallbackVerifier.verifyTokenOnly(request);

        AlarmIntegrationProperties.Upload config = properties.getUpload();
        if (files == null || files.length == 0) {
            throw new BusinessException("VALIDATION_ERROR", "请选择要上传的图片（表单字段名：files）");
        }
        if (files.length > config.getMaxFiles()) {
            throw new BusinessException("TOO_MANY_FILES",
                    "单次最多上传 " + config.getMaxFiles() + " 张图片，当前 " + files.length + " 张");
        }

        Set<String> allowedExtensions = Arrays.stream(config.getAllowedExtensions().split(","))
                .map(String::trim)
                .filter(text -> !text.isEmpty())
                .map(text -> text.toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());
        long maxBytes = (long) config.getMaxFileSizeMb() * 1024 * 1024;
        String baseUrl = resolvePublicBaseUrl();

        List<String> urls = new ArrayList<>();
        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                throw new BusinessException("VALIDATION_ERROR", "上传的图片内容为空");
            }
            String extension = extensionOf(file.getOriginalFilename());
            if (!allowedExtensions.contains(extension)) {
                throw new BusinessException("UNSUPPORTED_FILE_TYPE",
                        "不支持的图片格式：" + (extension.isEmpty() ? "未知" : extension)
                                + "，仅支持 " + config.getAllowedExtensions());
            }
            if (file.getSize() > maxBytes) {
                throw new BusinessException("FILE_TOO_LARGE",
                        "单张图片不能超过 " + config.getMaxFileSizeMb() + "MB");
            }
            String objectName = ossService.uploadFile(file, BIZ_TYPE);
            urls.add(baseUrl + objectName);
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("urls", urls);
        data.put("count", urls.size());

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("code", 200);
        response.put("success", true);
        response.put("message", "OK");
        response.put("data", data);
        return response;
    }

    /**
     * 解析返回给板端的公网访问前缀。
     * 优先取 drone.alarm-integration.upload.public-base-url；
     * 未配置时回退 oss.access —— 该值在部分环境为内网地址，板端不可达，生产环境应显式配置。
     */
    private String resolvePublicBaseUrl() {
        String base = properties.getUpload().getPublicBaseUrl();
        if (!StringUtils.hasText(base)) {
            base = ossProperties.getAccess();
        }
        if (!StringUtils.hasText(base)) {
            throw new BusinessException("UPLOAD_URL_NOT_CONFIGURED",
                    "未配置图片公网访问前缀，请设置 drone.alarm-integration.upload.public-base-url");
        }
        return base.endsWith("/") ? base : base + "/";
    }

    private String extensionOf(String filename) {
        if (!StringUtils.hasText(filename)) {
            return "";
        }
        int dot = filename.lastIndexOf('.');
        return dot >= 0 && dot < filename.length() - 1
                ? filename.substring(dot + 1).toLowerCase(Locale.ROOT)
                : "";
    }
}
