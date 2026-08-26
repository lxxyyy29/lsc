package com.changping.platform.modules.audit.controller;

import com.changping.platform.common.response.ApiResponse;
import com.changping.platform.modules.auth.security.PermissionCodes;
import com.changping.platform.modules.auth.security.PermissionGuard;
import com.changping.platform.modules.process.entity.ProcessInstanceEntity;
import com.changping.platform.modules.process.service.ProcessInstanceService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Author lxy
 * @Description //审核控制器，提供针对事件的审核发起和审核详情查询接口，需要对应 API 权限码
 * @Date 2026/04/18 10:10
 */
@RestController
@RequestMapping("/audits")
public class AuditController {

    private final ProcessInstanceService processInstanceService;
    private final PermissionGuard permissionGuard;
    private final JdbcTemplate jdbcTemplate;

    /**
     * @Author lxy
     * @Description //构造函数注入流程实例服务和权限守卫
     * @Date 2026/04/18 10:10
     * @Param [processInstanceService 流程实例服务, permissionGuard 权限守卫]
     * @return void
     */
    public AuditController(ProcessInstanceService processInstanceService, PermissionGuard permissionGuard, JdbcTemplate jdbcTemplate) {
        this.processInstanceService = processInstanceService;
        this.permissionGuard = permissionGuard;
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * @Author lxy
     * @Description //针对指定事件发起审核流程，可选传入流程模板ID，需要 API_AUDIT_START 权限
     * @Date 2026/04/18 10:10
     * @Param [eventId 事件ID, request 发起审核请求（可选，包含流程模板ID和是否强制重新发起标志）]
     * @return ApiResponse<ProcessInstanceEntity> 创建的流程实例信息
     */
    @PostMapping("/{eventId}/start")
    public ApiResponse<ProcessInstanceEntity> startAudit(
            @PathVariable Long eventId,
            @RequestBody(required = false) ProcessInstanceService.StartAuditRequest request) {
        permissionGuard.require(PermissionCodes.API_AUDIT_START);
        ProcessInstanceService.StartAuditRequest safeRequest = request == null
                ? new ProcessInstanceService.StartAuditRequest(null, false)
                : request;
        return ApiResponse.ok(processInstanceService.startAudit(eventId, safeRequest));
    }

    /**
     * @Author lxy
     * @Description //查询指定事件的审核流程详情，需要 API_AUDIT_DETAIL 权限
     * @Date 2026/04/18 10:10
     * @Param [eventId 事件ID]
     * @return ApiResponse<ProcessInstanceEntity> 流程实例详情
     */
    @GetMapping("/{eventId}")
    public ApiResponse<ProcessInstanceEntity> getAuditDetail(@PathVariable Long eventId) {
        permissionGuard.require(PermissionCodes.API_AUDIT_DETAIL);
        return ApiResponse.ok(processInstanceService.getAuditDetail(eventId));
    }

    /**
     * 审核中心列表：按归一化审核状态（待审核/已通过/已驳回）分页查询事件，
     * 并返回三个状态的统计数。待审核=事件状态 PENDING_AUDIT/IN_AUDIT；
     * 已通过=存在 AUDIT_PASS 流转记录的事件（当前可能已派单/关闭）；
     * 已驳回=事件状态 AUDIT_REJECTED。
     */
    @GetMapping
    public ApiResponse<Map<String, Object>> listAudits(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String searchKey) {
        permissionGuard.require(PermissionCodes.API_AUDIT_DETAIL);

        int safePage = Math.max(1, page);
        int safeSize = Math.max(1, Math.min(pageSize, 100));

        List<String> where = new ArrayList<>();
        List<Object> params = new ArrayList<>();
        // 与全站口径一致：仅统计未归档的活跃事件
        where.add("COALESCE(e.archived, 0) = 0");
        if ("PENDING".equalsIgnoreCase(status)) {
            where.add("e.status IN ('PENDING_AUDIT','IN_AUDIT')");
        } else if ("APPROVED".equalsIgnoreCase(status)) {
            where.add("EXISTS (SELECT 1 FROM biz_event_record r WHERE r.event_id = e.id AND r.action_type = 'AUDIT_PASS')");
        } else if ("REJECTED".equalsIgnoreCase(status)) {
            where.add("e.status = 'AUDIT_REJECTED'");
        } else {
            // 全部状态 = 审核相关事件的并集（待审核+已通过+已驳回），与统计卡口径一致
            where.add("(e.status IN ('PENDING_AUDIT','IN_AUDIT')"
                    + " OR EXISTS (SELECT 1 FROM biz_event_record r WHERE r.event_id = e.id AND r.action_type = 'AUDIT_PASS')"
                    + " OR e.status = 'AUDIT_REJECTED')");
        }
        if (searchKey != null && !searchKey.isBlank()) {
            where.add("(e.event_code LIKE ? OR e.title LIKE ?)");
            params.add("%" + searchKey.trim() + "%");
            params.add("%" + searchKey.trim() + "%");
        }
        String whereSql = where.isEmpty() ? "" : " WHERE " + String.join(" AND ", where);

        Long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM biz_event e" + whereSql, Long.class, params.toArray());

        // 三个状态的统计数（不受分页影响）
        Map<String, Long> stats = new HashMap<>();
        stats.put("pending", jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM biz_event e WHERE COALESCE(e.archived, 0) = 0 AND e.status IN ('PENDING_AUDIT','IN_AUDIT')", Long.class));
        stats.put("approved", jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM biz_event e WHERE COALESCE(e.archived, 0) = 0 AND EXISTS (SELECT 1 FROM biz_event_record r WHERE r.event_id = e.id AND r.action_type = 'AUDIT_PASS')", Long.class));
        stats.put("rejected", jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM biz_event e WHERE COALESCE(e.archived, 0) = 0 AND e.status = 'AUDIT_REJECTED'", Long.class));

        List<Map<String, Object>> items = new ArrayList<>();
        if (total != null && total > 0) {
            List<Object> pageParams = new ArrayList<>(params);
            pageParams.add(safeSize);
            pageParams.add((long) (safePage - 1) * safeSize);
            items = jdbcTemplate.queryForList(
                    "SELECT e.id, e.event_code, e.title, e.status, e.created_at, e.updated_at, "
                            + "(SELECT r.created_at FROM biz_event_record r WHERE r.event_id = e.id AND r.action_type IN ('AUDIT_PASS','AUDIT_REJECT') ORDER BY r.created_at DESC LIMIT 1) AS audit_time, "
                            + "(SELECT r.remark FROM biz_event_record r WHERE r.event_id = e.id AND r.action_type IN ('AUDIT_PASS','AUDIT_REJECT') ORDER BY r.created_at DESC LIMIT 1) AS audit_remark, "
                            + "(SELECT r.operator_name FROM biz_event_record r WHERE r.event_id = e.id AND r.action_type IN ('AUDIT_PASS','AUDIT_REJECT') ORDER BY r.created_at DESC LIMIT 1) AS auditor_name "
                            + "FROM biz_event e" + whereSql
                            + " ORDER BY e.created_at DESC LIMIT ? OFFSET ?",
                    pageParams.toArray());
        }

        Map<String, Object> result = new HashMap<>();
        result.put("items", items);
        result.put("total", total == null ? 0 : total);
        result.put("page", safePage);
        result.put("pageSize", safeSize);
        result.put("stats", stats);
        return ApiResponse.ok(result);
    }
}
