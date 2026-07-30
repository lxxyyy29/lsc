package com.changping.platform.modules.community.controller;

import com.changping.platform.common.response.ApiResponse;
import com.changping.platform.modules.auth.security.PermissionCodes;
import com.changping.platform.modules.auth.security.PermissionGuard;
import com.changping.platform.modules.auth.service.AuthService;
import com.changping.platform.modules.auth.service.CurrentUserService;
import com.changping.platform.modules.community.entity.PopulationEntity;
import com.changping.platform.modules.community.service.PopulationService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/community/population")
public class PopulationController {

    private final PopulationService populationService;
    private final CurrentUserService currentUserService;
    private final PermissionGuard permissionGuard;

    public PopulationController(
            PopulationService populationService,
            CurrentUserService currentUserService,
            PermissionGuard permissionGuard) {
        this.populationService = populationService;
        this.currentUserService = currentUserService;
        this.permissionGuard = permissionGuard;
    }

    @GetMapping
    public ApiResponse<List<PopulationEntity>> list(@RequestParam(required = false) Long gridId) {
        requirePopulationPermission();
        return ApiResponse.ok(populationService.list(gridId));
    }

    @GetMapping("/{id}")
    public ApiResponse<PopulationEntity> detail(@PathVariable Long id) {
        requirePopulationPermission();
        return ApiResponse.ok(populationService.detail(id));
    }

    @PostMapping
    public ApiResponse<Boolean> create(@RequestBody PopulationEntity entity) {
        requirePopulationPermission();
        return ApiResponse.ok(populationService.create(entity));
    }

    @PutMapping("/{id}")
    public ApiResponse<Boolean> update(@PathVariable Long id, @RequestBody PopulationEntity entity) {
        requirePopulationPermission();
        entity.setId(id);
        return ApiResponse.ok(populationService.update(entity));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Boolean> delete(@PathVariable Long id) {
        requirePopulationPermission();
        return ApiResponse.ok(populationService.delete(id));
    }

    private void requirePopulationPermission() {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.MENU_COMMUNITY_POPULATION);
    }
}
