package com.changping.platform.modules.resident.controller;

import com.changping.platform.common.exception.BusinessException;
import com.changping.platform.common.response.ApiResponse;
import com.changping.platform.modules.auth.security.AuthenticatedUserContextHolder;
import com.changping.platform.modules.auth.security.PermissionCodes;
import com.changping.platform.modules.auth.security.PermissionGuard;
import com.changping.platform.modules.auth.service.AuthService;
import com.changping.platform.modules.auth.service.CurrentUserService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Author tangxinglin
 * @Description //Web端便民报修管理控制器：报修列表查询、详情查看与状态流转（受理/完成/驳回）
 * @Date 2026/08/10 15:00
 */
@RestController
@RequestMapping("/repairs")
public class RepairAdminController {

    private static final List<String> VALID_STATUSES = List.of("PENDING", "ASSIGNED", "PROCESSING", "COMPLETED", "REJECTED");

    private final JdbcTemplate jdbcTemplate;
    private final PermissionGuard permissionGuard;
    private final CurrentUserService currentUserService;

    /**
     * @Author tangxinglin
     * @Description //构造函数，注入 JDBC 模板、权限校验与当前用户服务
     * @Date 2026/08/10 15:00
     */
    public RepairAdminController(JdbcTemplate jdbcTemplate,
                                 PermissionGuard permissionGuard,
                                 CurrentUserService currentUserService) {
        this.jdbcTemplate = jdbcTemplate;
        this.permissionGuard = permissionGuard;
        this.currentUserService = currentUserService;
    }

    /**
     * @Author tangxinglin
     * @Description //分页查询报修列表，支持按状态、类型、关键词（标题/上报人）筛选
     * @Date 2026/08/10 15:00
     * @Param [page 页码默认1, pageSize 每页条数默认20, status 状态筛选, type 报修类型筛选, keyword 标题/上报人关键词]
     * @return ApiResponse<Map> 含 items 列表与 total 总数
     */
    @GetMapping
    public ApiResponse<Map<String, Object>> listRepairs(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String keyword) {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.API_REPAIR_VIEW);
        int offset = Math.max(0, (page - 1) * pageSize);
        String kw = keyword == null ? "" : keyword.trim();

        List<Map<String, Object>> items = jdbcTemplate.queryForList(
            "SELECT r.id, r.repair_type as repairType, r.title, r.description, r.address, r.status, " +
            "  r.reporter_name as reporterName, r.reporter_phone as reporterPhone, r.reporter_user_id as reporterUserId, " +
            "  r.handler_user_id as handlerUserId, r.handle_result as handleResult, " +
            "  DATE_FORMAT(r.handled_at, '%Y-%m-%d %H:%i') as handledAt, " +
            "  DATE_FORMAT(r.created_at, '%Y-%m-%d %H:%i') as createdAt, " +
            "  IFNULL(h.real_name, '-') as handlerName " +
            "FROM biz_repair_request r " +
            "LEFT JOIN sys_user h ON h.id = r.handler_user_id " +
            "WHERE (? = '' OR r.status = ?) AND (? = '' OR r.repair_type = ?) " +
            "  AND (? = '' OR r.title LIKE CONCAT('%', ?, '%') OR r.reporter_name LIKE CONCAT('%', ?, '%')) " +
            "ORDER BY r.id DESC LIMIT ? OFFSET ?",
            status == null ? "" : status, status == null ? "" : status,
            type == null ? "" : type, type == null ? "" : type,
            kw, kw, kw, pageSize, offset);

        Long total = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM biz_repair_request r " +
            "WHERE (? = '' OR r.status = ?) AND (? = '' OR r.repair_type = ?) " +
            "  AND (? = '' OR r.title LIKE CONCAT('%', ?, '%') OR r.reporter_name LIKE CONCAT('%', ?, '%'))",
            Long.class,
            status == null ? "" : status, status == null ? "" : status,
            type == null ? "" : type, type == null ? "" : type,
            kw, kw, kw);

        Map<String, Object> result = new HashMap<>();
        result.put("items", items);
        result.put("total", total);
        return ApiResponse.ok(result);
    }

    /**
     * @Author tangxinglin
     * @Description //查询报修详情（含处理人姓名）
     * @Date 2026/08/10 15:00
     * @Param [id 报修ID]
     * @return ApiResponse<Map> 报修详情
     */
    @GetMapping("/{id}")
    public ApiResponse<Map<String, Object>> repairDetail(@PathVariable Long id) {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.API_REPAIR_VIEW);
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            "SELECT r.id, r.repair_type as repairType, r.title, r.description, r.address, r.status, " +
            "  r.reporter_name as reporterName, r.reporter_phone as reporterPhone, r.reporter_user_id as reporterUserId, " +
            "  r.handler_user_id as handlerUserId, r.handle_result as handleResult, " +
            "  DATE_FORMAT(r.handled_at, '%Y-%m-%d %H:%i') as handledAt, " +
            "  DATE_FORMAT(r.created_at, '%Y-%m-%d %H:%i') as createdAt, " +
            "  IFNULL(h.real_name, '-') as handlerName " +
            "FROM biz_repair_request r " +
            "LEFT JOIN sys_user h ON h.id = r.handler_user_id " +
            "WHERE r.id = ?", id);
        if (rows.isEmpty()) {
            throw new BusinessException("REPAIR_NOT_FOUND", "报修记录不存在");
        }
        return ApiResponse.ok(rows.get(0));
    }

    /**
     * @Author tangxinglin
     * @Description //报修状态流转：受理/派单（ASSIGNED）、处理中（PROCESSING）、完成（COMPLETED）、驳回（REJECTED）
     *  完成与驳回必须填写处理结果/驳回原因，状态变更时记录处理人与处理时间
     * @Date 2026/08/10 15:00
     * @Param [id 报修ID, body 含 status 目标状态与 handleResult 处理结果]
     * @return ApiResponse<Boolean> 更新结果
     */
    @PutMapping("/{id}/status")
    public ApiResponse<Boolean> updateRepairStatus(@PathVariable Long id,
                                                   @RequestBody Map<String, Object> body) {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.API_REPAIR_MANAGE);
        String status = body.get("status") == null ? "" : String.valueOf(body.get("status"));
        if (!VALID_STATUSES.contains(status)) {
            throw new BusinessException("REPAIR_INVALID_STATUS", "非法的报修状态：" + status);
        }
        String handleResult = body.get("handleResult") == null ? "" : String.valueOf(body.get("handleResult")).trim();
        if ((status.equals("COMPLETED") || status.equals("REJECTED")) && handleResult.isEmpty()) {
            throw new BusinessException("REPAIR_RESULT_REQUIRED", status.equals("COMPLETED") ? "请填写处理结果" : "请填写驳回原因");
        }
        Long handlerUserId = AuthenticatedUserContextHolder.getRequired().id();
        int updated = jdbcTemplate.update(
            "UPDATE biz_repair_request SET status = ?, handler_user_id = ?, handle_result = ?, " +
            "handled_at = CASE WHEN ? IN ('COMPLETED', 'REJECTED') THEN NOW() ELSE handled_at END " +
            "WHERE id = ?",
            status, handlerUserId, handleResult.isEmpty() ? null : handleResult, status, id);
        if (updated == 0) {
            throw new BusinessException("REPAIR_NOT_FOUND", "报修记录不存在");
        }
        return ApiResponse.ok(true);
    }
}
