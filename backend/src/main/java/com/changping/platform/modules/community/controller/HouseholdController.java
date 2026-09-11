package com.changping.platform.modules.community.controller;

import com.changping.platform.common.response.ApiResponse;
import com.changping.platform.modules.auth.security.PermissionCodes;
import com.changping.platform.modules.auth.security.PermissionGuard;
import com.changping.platform.modules.auth.service.AuthService;
import com.changping.platform.modules.auth.service.CurrentUserService;
import com.changping.platform.modules.community.entity.HouseholdEntity;
import com.changping.platform.modules.community.service.HouseholdService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 实有人口「户」管理：户的增删改查、成员挂靠、户主变更（关系自动重算）。
 * 复用实有人口菜单权限 menu:community:population。
 */
@RestController
@RequestMapping("/community/household")
public class HouseholdController {

    private final HouseholdService householdService;
    private final CurrentUserService currentUserService;
    private final PermissionGuard permissionGuard;

    public HouseholdController(
            HouseholdService householdService,
            CurrentUserService currentUserService,
            PermissionGuard permissionGuard) {
        this.householdService = householdService;
        this.currentUserService = currentUserService;
        this.permissionGuard = permissionGuard;
    }

    @GetMapping
    public ApiResponse<?> list(@RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long gridId) {
        requirePermission();
        return ApiResponse.ok(householdService.list(keyword, gridId));
    }

    @GetMapping("/{id}")
    public ApiResponse<?> detail(@PathVariable Long id) {
        requirePermission();
        return ApiResponse.ok(householdService.detail(id));
    }

    @PostMapping
    public ApiResponse<Long> create(@RequestBody HouseholdEntity entity) {
        requirePermission();
        return ApiResponse.ok(householdService.create(entity));
    }

    @PutMapping("/{id}")
    public ApiResponse<Boolean> update(@PathVariable Long id, @RequestBody HouseholdEntity entity) {
        requirePermission();
        entity.setId(id);
        return ApiResponse.ok(householdService.update(entity));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Boolean> delete(@PathVariable Long id) {
        requirePermission();
        return ApiResponse.ok(householdService.delete(id));
    }

    /** 把人员挂到户下：body { populationId, relation } */
    @PostMapping("/{id}/members")
    public ApiResponse<Boolean> addMember(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        requirePermission();
        Long populationId = toLong(body.get("populationId"));
        String relation = body.get("relation") == null ? null : String.valueOf(body.get("relation"));
        householdService.addMember(id, populationId, relation);
        return ApiResponse.ok(true);
    }

    /** 把人员移出户 */
    @DeleteMapping("/{id}/members/{populationId}")
    public ApiResponse<Boolean> removeMember(@PathVariable Long id, @PathVariable Long populationId) {
        requirePermission();
        householdService.removeMember(id, populationId);
        return ApiResponse.ok(true);
    }

    /** 预览户主变更后的成员关系：body { newHeadId, relationToOldHead } */
    @PostMapping("/{id}/change-head/preview")
    public ApiResponse<?> previewChangeHead(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        requirePermission();
        return ApiResponse.ok(householdService.previewChangeHead(
                id, toLong(body.get("newHeadId")), str(body.get("relationToOldHead"))));
    }

    /** 变更户主并按亲属规则自动重算成员关系 */
    @PostMapping("/{id}/change-head")
    public ApiResponse<?> changeHead(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        requirePermission();
        return ApiResponse.ok(householdService.changeHead(
                id, toLong(body.get("newHeadId")), str(body.get("relationToOldHead"))));
    }

    private static Long toLong(Object v) {
        if (v == null) {
            return null;
        }
        if (v instanceof Number n) {
            return n.longValue();
        }
        String s = String.valueOf(v).trim();
        return s.isEmpty() ? null : Long.valueOf(s);
    }

    private static String str(Object v) {
        return v == null ? null : String.valueOf(v);
    }

    private void requirePermission() {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.MENU_COMMUNITY_POPULATION);
    }
}
