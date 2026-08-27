package com.changping.platform.modules.community.controller;

import com.changping.platform.common.response.ApiResponse;
import com.changping.platform.modules.auth.security.PermissionCodes;
import com.changping.platform.modules.auth.security.PermissionGuard;
import com.changping.platform.modules.auth.service.AuthService;
import com.changping.platform.modules.auth.service.CurrentUserService;
import com.changping.platform.modules.community.entity.PlaceEntity;
import com.changping.platform.modules.community.service.PlaceService;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/community/places")
public class PlaceController {

    private final PlaceService service;
    private final CurrentUserService currentUserService;
    private final PermissionGuard permissionGuard;

    public PlaceController(PlaceService service, CurrentUserService currentUserService, PermissionGuard permissionGuard) {
        this.service = service;
        this.currentUserService = currentUserService;
        this.permissionGuard = permissionGuard;
    }

    @GetMapping
    public ApiResponse<List<PlaceEntity>> list(@RequestParam(required = false) Long gridId,
                                               @RequestParam(required = false) String keyword,
                                               @RequestParam(required = false) String placeType,
                                               @RequestParam(required = false) String businessCategory) {
        requirePlacePermission();
        return ApiResponse.ok(service.list(gridId, keyword, placeType, businessCategory));
    }
    /** 经营类别下拉选项（从台账提取去重） */
    @GetMapping("/categories")
    public ApiResponse<List<String>> categories() {
        requirePlacePermission();
        return ApiResponse.ok(service.listBusinessCategories());
    }
    @GetMapping("/{id}")
    public ApiResponse<PlaceEntity> detail(@PathVariable Long id) {
        requirePlacePermission();
        return ApiResponse.ok(service.detail(id));
    }
    @PostMapping
    public ApiResponse<Boolean> create(@RequestBody PlaceEntity entity) {
        requirePlacePermission();
        return ApiResponse.ok(service.create(entity));
    }
    @PutMapping("/{id}")
    public ApiResponse<Boolean> update(@PathVariable Long id, @RequestBody PlaceEntity entity) {
        requirePlacePermission();
        entity.setId(id);
        return ApiResponse.ok(service.update(entity));
    }
    @DeleteMapping("/{id}")
    public ApiResponse<Boolean> delete(@PathVariable Long id) {
        requirePlacePermission();
        return ApiResponse.ok(service.delete(id));
    }

    private void requirePlacePermission() {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.MENU_COMMUNITY_PLACE);
    }
}
