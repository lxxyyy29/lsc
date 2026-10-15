package com.changping.platform.modules.system.controller;

import com.changping.platform.common.response.ApiResponse;
import com.changping.platform.modules.auth.service.AuthService;
import com.changping.platform.modules.auth.service.CurrentUserService;
import com.changping.platform.modules.system.service.SuppressWindowService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 时段抑制（夜间静默）规则管理接口 —— 后台可自助调整时段。
 *
 * 规则命中后：事件按动作（HIDE/NO_DISPATCH/NO_NOTIFY）被抑制，
 * 抑制截止时间到点后自动恢复可见（无需人工操作）。
 */
@RestController
@RequestMapping("/system/suppress-windows")
public class SuppressWindowController {

    private final SuppressWindowService service;
    private final CurrentUserService currentUserService;

    public SuppressWindowController(SuppressWindowService service, CurrentUserService currentUserService) {
        this.service = service;
        this.currentUserService = currentUserService;
    }

    /** 规则列表 */
    @GetMapping
    public ApiResponse<List<Map<String, Object>>> list() {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        return ApiResponse.ok(service.list());
    }

    /** 新增规则 */
    @PostMapping
    public ApiResponse<Map<String, Object>> create(@RequestBody Map<String, Object> body) {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        return ApiResponse.ok(service.create(body));
    }

    /** 修改规则（支持后台随时调整时段，立即生效，无需重启） */
    @PutMapping("/{id}")
    public ApiResponse<Map<String, Object>> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        return ApiResponse.ok(service.update(id, body));
    }

    /** 删除规则 */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        service.delete(id);
        return ApiResponse.ok(null);
    }

    /** 启停规则 */
    @PutMapping("/{id}/enabled")
    public ApiResponse<Void> setEnabled(@PathVariable Long id, @RequestParam boolean enabled) {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        service.setEnabled(id, enabled);
        return ApiResponse.ok(null);
    }

    /**
     * 命中预演：保存前先看某时间点是否会被抑制、抑制到何时。
     * 例：?sourceSystem=THIRD_PARTY_DRONE&eventType=市容环境&urgency=GREEN&at=2026-10-15T23:30
     */
    @GetMapping("/test")
    public ApiResponse<Map<String, Object>> test(
            @RequestParam(required = false) String sourceSystem,
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) String urgency,
            @RequestParam(required = false) String at) {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        LocalDateTime moment = null;
        if (at != null && !at.isBlank()) {
            try {
                moment = LocalDateTime.parse(at.trim().length() == 16 ? at.trim() + ":00" : at.trim());
            } catch (Exception e) {
                moment = LocalDateTime.parse(at.trim().replace(" ", "T"));
            }
        }
        return ApiResponse.ok(service.test(sourceSystem, eventType, urgency, moment));
    }
}
