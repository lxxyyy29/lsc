package com.changping.platform.modules.community.controller;

import com.changping.platform.common.response.ApiResponse;
import com.changping.platform.modules.auth.security.PermissionCodes;
import com.changping.platform.modules.auth.security.PermissionGuard;
import com.changping.platform.modules.auth.service.AuthService;
import com.changping.platform.modules.auth.service.CurrentUserService;
import com.changping.platform.modules.community.service.ImportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@RestController
@RequestMapping("/community/import")
public class ImportController {

    private final ImportService importService;
    private final CurrentUserService currentUserService;
    private final PermissionGuard permissionGuard;

    public ImportController(ImportService importService, CurrentUserService currentUserService,
                            PermissionGuard permissionGuard) {
        this.importService = importService;
        this.currentUserService = currentUserService;
        this.permissionGuard = permissionGuard;
    }

    /**
     * 预览导入数据（不写入 DB）
     */
    @PostMapping("/preview")
    public ApiResponse<Map<String, Object>> preview(
            @RequestParam String type,
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "10") int previewRows) {
        requirePermission(PermissionCodes.MENU_COMMUNITY_POPULATION);
        return ApiResponse.ok(importService.previewImport(type, file, previewRows));
    }

    /**
     * 执行导入
     */
    @PostMapping("/execute")
    public ApiResponse<Map<String, Object>> execute(
            @RequestParam String type,
            @RequestParam("file") MultipartFile file) {
        requirePermission(PermissionCodes.MENU_COMMUNITY_POPULATION);
        return ApiResponse.ok(importService.executeImport(type, file));
    }

    /**
     * 下载导入模板（xlsx，表头与导入解析规则严格一致）
     */
    @GetMapping("/template")
    public ResponseEntity<byte[]> template(@RequestParam String type) {
        requirePermission(PermissionCodes.MENU_COMMUNITY_POPULATION);
        byte[] bytes = importService.buildTemplate(type);
        String filename = URLEncoder.encode("导入模板-" + type + ".xlsx", StandardCharsets.UTF_8);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + filename);
        return new ResponseEntity<>(bytes, headers, HttpStatus.OK);
    }

    private void requirePermission(String permissionCode) {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(permissionCode);
    }
}
