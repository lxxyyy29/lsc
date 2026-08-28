package com.changping.platform.modules.system.controller;

import com.changping.platform.common.response.ApiResponse;
import com.changping.platform.modules.auth.security.PermissionCodes;
import com.changping.platform.modules.auth.security.PermissionGuard;
import com.changping.platform.modules.auth.service.AuthService;
import com.changping.platform.modules.auth.service.CurrentUserService;
import com.changping.platform.modules.system.service.SystemDictService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Description //系统字典控制器：字典类型与字典项的查询与维护，业务下拉选项统一由字典驱动
 */
@Validated
@RestController
@RequestMapping("/system/dicts")
public class SystemDictController {

    private final SystemDictService systemDictService;
    private final CurrentUserService currentUserService;
    private final PermissionGuard permissionGuard;

    public SystemDictController(
            SystemDictService systemDictService,
            CurrentUserService currentUserService,
            PermissionGuard permissionGuard) {
        this.systemDictService = systemDictService;
        this.currentUserService = currentUserService;
        this.permissionGuard = permissionGuard;
    }

    /** 查询全部字典类型（含字典项数量） */
    @GetMapping
    public ApiResponse<List<SystemDictService.DictTypeItem>> listTypes() {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.API_SYSTEM_DICT_LIST);
        return ApiResponse.ok(systemDictService.listTypes());
    }

    /** 查询指定字典的字典项；业务表单读取传 activeOnly=true 仅取启用项 */
    @GetMapping("/{code}/items")
    public ApiResponse<List<SystemDictService.DictItem>> listItems(
            @PathVariable("code") String code,
            @RequestParam(defaultValue = "false") boolean activeOnly) {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.API_SYSTEM_DICT_LIST);
        return ApiResponse.ok(systemDictService.listItems(code, activeOnly));
    }

    /** 新建字典类型 */
    @PostMapping
    public ApiResponse<SystemDictService.DictTypeItem> createType(
            @Valid @RequestBody SystemDictService.UpsertTypeRequest request) {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.API_SYSTEM_DICT_MANAGE);
        return ApiResponse.ok(systemDictService.createType(request));
    }

    /** 更新字典类型（编码不可改） */
    @PutMapping("/{id}")
    public ApiResponse<SystemDictService.DictTypeItem> updateType(
            @PathVariable Long id,
            @Valid @RequestBody SystemDictService.UpsertTypeRequest request) {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.API_SYSTEM_DICT_MANAGE);
        return ApiResponse.ok(systemDictService.updateType(id, request));
    }

    /** 删除字典类型（级联删除字典项） */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteType(@PathVariable Long id) {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.API_SYSTEM_DICT_MANAGE);
        systemDictService.deleteType(id);
        return ApiResponse.ok(null);
    }

    /** 新增字典项 */
    @PostMapping("/{code}/items")
    public ApiResponse<SystemDictService.DictItem> createItem(
            @PathVariable("code") String code,
            @Valid @RequestBody SystemDictService.UpsertItemRequest request) {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.API_SYSTEM_DICT_MANAGE);
        return ApiResponse.ok(systemDictService.createItem(code, request));
    }

    /** 更新字典项 */
    @PutMapping("/items/{itemId}")
    public ApiResponse<SystemDictService.DictItem> updateItem(
            @PathVariable Long itemId,
            @Valid @RequestBody SystemDictService.UpsertItemRequest request) {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.API_SYSTEM_DICT_MANAGE);
        return ApiResponse.ok(systemDictService.updateItem(itemId, request));
    }

    /** 删除字典项 */
    @DeleteMapping("/items/{itemId}")
    public ApiResponse<Void> deleteItem(@PathVariable Long itemId) {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.API_SYSTEM_DICT_MANAGE);
        systemDictService.deleteItem(itemId);
        return ApiResponse.ok(null);
    }
}
