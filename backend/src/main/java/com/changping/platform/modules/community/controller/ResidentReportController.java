package com.changping.platform.modules.community.controller;

import com.changping.platform.common.response.ApiResponse;
import com.changping.platform.modules.auth.security.PermissionCodes;
import com.changping.platform.modules.auth.security.PermissionGuard;
import com.changping.platform.modules.auth.service.AuthService;
import com.changping.platform.modules.auth.service.CurrentUserService;
import com.changping.platform.modules.community.entity.ResidentReportEntity;
import com.changping.platform.modules.community.service.ResidentReportService;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/**
 * 居民上报记录控制器：仅提供只读记录查询。
 * 居民上报已统一归口至事件闭环处理中心，提交时自动生成事件，
 * 处置与派单一律通过事件中心进行，本模块不再提供直接处置操作。
 */
@RestController
@RequestMapping("/community/resident-reports")
public class ResidentReportController {

    private final ResidentReportService service;
    private final CurrentUserService currentUserService;
    private final PermissionGuard permissionGuard;

    public ResidentReportController(ResidentReportService service,
            CurrentUserService currentUserService,
            PermissionGuard permissionGuard) {
        this.service = service;
        this.currentUserService = currentUserService;
        this.permissionGuard = permissionGuard;
    }

    /** 全部为管理端功能：居民上报记录含姓名/手机号等个人信息，禁止 H5/居民令牌访问 */
    @GetMapping
    public ApiResponse<List<ResidentReportEntity>> list(@RequestParam(required = false) String status) {
        requireWebResidentReportAccess();
        return ApiResponse.ok(service.listByStatus(status));
    }

    @GetMapping("/{id}")
    public ApiResponse<ResidentReportEntity> getById(@PathVariable Long id) {
        requireWebResidentReportAccess();
        return ApiResponse.ok(service.findById(id));
    }

    @GetMapping("/code/{queryCode}")
    public ApiResponse<ResidentReportEntity> findByCode(@PathVariable String queryCode) {
        requireWebResidentReportAccess();
        return ApiResponse.ok(service.findByCode(queryCode));
    }

    @PostMapping
    public ApiResponse<Boolean> create(@RequestBody ResidentReportEntity entity) {
        requireWebResidentReportAccess();
        return ApiResponse.ok(service.create(entity));
    }

    /** 管理端页面/API 门禁：WEB 客户端且持有任一居民上报菜单权限（历史 menu:community:resident-report 或 web:menu:resident-reports） */
    private void requireWebResidentReportAccess() {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.requireAny(
                PermissionCodes.MENU_COMMUNITY_RESIDENT_REPORT,
                "web:menu:resident-reports");
    }
}
