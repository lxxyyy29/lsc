package com.changping.platform.modules.community.controller;

import com.changping.platform.common.response.ApiResponse;
import com.changping.platform.modules.auth.security.PermissionCodes;
import com.changping.platform.modules.auth.security.PermissionGuard;
import com.changping.platform.modules.auth.service.AuthService;
import com.changping.platform.modules.auth.service.CurrentUserService;
import com.changping.platform.modules.community.entity.GridEntity;
import com.changping.platform.modules.community.service.GridService;
import com.changping.platform.modules.community.vo.GridTreeVo;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/community/grids")
public class GridController {

    private final GridService gridService;
    private final CurrentUserService currentUserService;
    private final PermissionGuard permissionGuard;

    public GridController(GridService gridService, CurrentUserService currentUserService, PermissionGuard permissionGuard) {
        this.gridService = gridService;
        this.currentUserService = currentUserService;
        this.permissionGuard = permissionGuard;
    }

    @GetMapping("/tree")
    public ApiResponse<List<GridTreeVo>> tree() {
        requireGridReadPermission();
        return ApiResponse.ok(gridService.tree());
    }

    @GetMapping("/{id}")
    public ApiResponse<GridEntity> detail(@PathVariable Long id) {
        requireGridReadPermission();
        return ApiResponse.ok(gridService.detail(id));
    }

    @GetMapping("/{id}/children")
    public ApiResponse<List<GridEntity>> children(@PathVariable Long id) {
        requireGridReadPermission();
        return ApiResponse.ok(gridService.children(id));
    }

    @PostMapping
    public ApiResponse<Boolean> create(@RequestBody GridEntity entity) {
        requireGridManagePermission();
        return ApiResponse.ok(gridService.create(entity));
    }

    @PutMapping("/{id}")
    public ApiResponse<Boolean> update(@PathVariable Long id, @RequestBody GridEntity entity) {
        requireGridManagePermission();
        entity.setId(id);
        return ApiResponse.ok(gridService.updateGrid(entity));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Boolean> delete(@PathVariable Long id) {
        requireGridManagePermission();
        return ApiResponse.ok(gridService.delete(id));
    }

    private void requireGridReadPermission() {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.requireAny(
                PermissionCodes.MENU_COMMUNITY_GRID,
                PermissionCodes.MENU_COMMUNITY_POPULATION,
                PermissionCodes.MENU_COMMUNITY_BUILDING,
                PermissionCodes.MENU_COMMUNITY_PLACE,
                PermissionCodes.MENU_COMMUNITY_ORG_MEMBER,
                PermissionCodes.MENU_COMMUNITY_DASHBOARD,
                PermissionCodes.MENU_BIG_SCREEN_VIEW);
    }

    private void requireGridManagePermission() {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.MENU_COMMUNITY_GRID);
    }
}
