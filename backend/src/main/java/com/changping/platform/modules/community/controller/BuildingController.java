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

    /**
     * 房屋列表。除网格外支持三类筛选（需求）：房东（姓名/电话模糊）、是否群租房、消防风险等级。
     */
    @GetMapping
    public ApiResponse<List<BuildingEntity>> list(
            @RequestParam(required = false) Long gridId,
            @RequestParam(required = false) String landlordName,
            @RequestParam(required = false) String isGroupRental,
            @RequestParam(required = false) String fireRiskLevel) {
        requireBuildingPermission();
        return ApiResponse.ok(service.search(gridId, landlordName, parseGroupRental(isGroupRental), fireRiskLevel));
    }

    /**
     * 群租房筛选值容错解析。
     *
     * 页面下拉传的是 '0'/'1'，但外部调用或历史链接可能传 true/false/是/否；
     * 该参数原先声明为 Integer，收到 "true" 时 Spring 类型转换失败 → 整个请求 500
     * （用户看到的是"服务器内部错误"，筛选直接不可用）。这里统一容错，
     * 无法识别时按"不筛选"处理，绝不因为一个查询参数把列表打成 500。
     */
    private Integer parseGroupRental(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String v = raw.trim().toLowerCase();
        if (java.util.List.of("1", "true", "yes", "y", "是", "群租", "群租房").contains(v)) {
            return 1;
        }
        if (java.util.List.of("0", "false", "no", "n", "否", "非群租", "非群租房").contains(v)) {
            return 0;
        }
        return null;
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
