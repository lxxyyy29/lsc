package com.changping.platform.modules.community.controller;

import com.changping.platform.common.response.ApiResponse;
import com.changping.platform.modules.auth.security.PermissionCodes;
import com.changping.platform.modules.auth.security.PermissionGuard;
import com.changping.platform.modules.auth.service.AuthService;
import com.changping.platform.modules.auth.service.CurrentUserService;
import com.changping.platform.modules.community.entity.BuildingEntity;
import com.changping.platform.modules.community.service.BuildingService;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/community/buildings")
public class BuildingController {

    private final BuildingService service;
    private final CurrentUserService currentUserService;
    private final PermissionGuard permissionGuard;

    public BuildingController(BuildingService service, CurrentUserService currentUserService, PermissionGuard permissionGuard) {
        this.service = service;
        this.currentUserService = currentUserService;
        this.permissionGuard = permissionGuard;
    }

    @GetMapping
    public ApiResponse<List<BuildingEntity>> list(@RequestParam(required = false) Long gridId) {
        requireBuildingPermission();
        return ApiResponse.ok(service.list(gridId));
    }
    @GetMapping("/{id}")
    public ApiResponse<BuildingEntity> detail(@PathVariable Long id) {
        requireBuildingPermission();
        return ApiResponse.ok(service.detail(id));
    }
    @PostMapping
    public ApiResponse<Boolean> create(@RequestBody BuildingEntity entity) {
        requireBuildingPermission();
        return ApiResponse.ok(service.create(entity));
    }
    @PutMapping("/{id}")
    public ApiResponse<Boolean> update(@PathVariable Long id, @RequestBody BuildingEntity entity) {
        requireBuildingPermission();
        entity.setId(id);
        return ApiResponse.ok(service.update(entity));
    }
    @DeleteMapping("/{id}")
    public ApiResponse<Boolean> delete(@PathVariable Long id) {
        requireBuildingPermission();
        return ApiResponse.ok(service.delete(id));
    }

    private void requireBuildingPermission() {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.MENU_COMMUNITY_BUILDING);
    }
}
