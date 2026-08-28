package com.changping.platform.modules.community.controller;

import com.changping.platform.common.response.ApiResponse;
import com.changping.platform.modules.auth.security.PermissionCodes;
import com.changping.platform.modules.auth.security.PermissionGuard;
import com.changping.platform.modules.auth.service.AuthService;
import com.changping.platform.modules.auth.service.CurrentUserService;
import com.changping.platform.modules.community.entity.FormFieldConfigEntity;
import com.changping.platform.modules.community.mapper.FormFieldConfigMapper;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 表单字段配置器（仅人口库）：查询/批量保存字段配置
 */
@RestController
@RequestMapping("/community/form-field-config")
public class FormFieldConfigController {

    private final FormFieldConfigMapper mapper;
    private final CurrentUserService currentUserService;
    private final PermissionGuard permissionGuard;

    public FormFieldConfigController(FormFieldConfigMapper mapper,
                                     CurrentUserService currentUserService,
                                     PermissionGuard permissionGuard) {
        this.mapper = mapper;
        this.currentUserService = currentUserService;
        this.permissionGuard = permissionGuard;
    }

    /** 查询某模块的字段配置 */
    @GetMapping
    public ApiResponse<List<FormFieldConfigEntity>> list(@RequestParam String module) {
        requirePermission();
        return ApiResponse.ok(mapper.listByModule(module));
    }

    /** 批量保存字段配置（启用/排序/必填/标签）；仅允许 population 模块 */
    @PostMapping
    public ApiResponse<Boolean> save(@RequestBody List<FormFieldConfigEntity> fields) {
        requirePermission();
        for (FormFieldConfigEntity f : fields) {
            if (f.getId() == null) continue;
            // 仅允许更新人口库字段配置，防止越权改动其它模块
            f.setModule("population");
            mapper.update(f);
        }
        return ApiResponse.ok(true);
    }

    private void requirePermission() {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.MENU_COMMUNITY_POPULATION);
    }
}
