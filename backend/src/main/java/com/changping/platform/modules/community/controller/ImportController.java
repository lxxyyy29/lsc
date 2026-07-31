package com.changping.platform.modules.community.controller;

import com.changping.platform.common.response.ApiResponse;
import com.changping.platform.modules.auth.security.PermissionCodes;
import com.changping.platform.modules.auth.security.PermissionGuard;
import com.changping.platform.modules.auth.service.AuthService;
import com.changping.platform.modules.auth.service.CurrentUserService;
import com.changping.platform.modules.community.service.ImportService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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

    private void requirePermission(String permissionCode) {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(permissionCode);
    }
}
