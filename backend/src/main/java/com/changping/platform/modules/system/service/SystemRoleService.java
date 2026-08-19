package com.changping.platform.modules.system.service;

import com.changping.platform.common.exception.BusinessException;
import com.changping.platform.common.response.PagedResult;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * @Author tangxinglin
 * @Description //系统角色服务，提供角色列表查询、创建、更新及权限分配的业务逻辑
 * @Date 2026/04/18 10:05
 */
@Service
public class SystemRoleService {

    private final JdbcTemplate jdbcTemplate;
    private final SystemPermissionService systemPermissionService;

    /**
     * @Author tangxinglin
     * @Description //构造函数，注入数据库操作模板和权限服务
     * @Date 2026/04/18 10:05
     * @Param [jdbcTemplate 数据库操作模板, systemPermissionService 权限服务]
     * @return
     */
    public SystemRoleService(JdbcTemplate jdbcTemplate, SystemPermissionService systemPermissionService) {
        this.jdbcTemplate = jdbcTemplate;
        this.systemPermissionService = systemPermissionService;
    }

    /**
     * @Author tangxinglin
     * @Description //查询所有角色列表，包含每个角色的用户数和权限数统计
     * @Date 2026/04/18 10:05
     * @Param []
     * @return List<RoleListItem> 角色列表
     */
    @Transactional(readOnly = true)
    public List<RoleListItem> listRoles() {
        return jdbcTemplate.query(
                "SELECT r.id, r.role_code, r.role_name, r.status, r.remark, COUNT(DISTINCT sur.user_id) AS user_count, COUNT(DISTINCT srp.permission_id) AS permission_count "
                        + "FROM sys_role r "
                        + "LEFT JOIN sys_user_role sur ON sur.role_id = r.id "
                        + "LEFT JOIN sys_role_permission srp ON srp.role_id = r.id "
                        + "GROUP BY r.id, r.role_code, r.role_name, r.status, r.remark ORDER BY r.id ASC",
                (rs, rowNum) -> new RoleListItem(
                        rs.getLong("id"),
                        rs.getString("role_code"),
                        rs.getString("role_name"),
                        rs.getString("status"),
                        rs.getString("remark"),
                        rs.getInt("user_count"),
                        rs.getInt("permission_count")));
    }

    /**
     * @Author tangxinglin
     * @Description //分页查询角色列表
     * @Date 2026/04/18 10:05
     * @Param [page 页码, pageSize 每页条数]
     * @return PagedResult<RoleListItem> 分页角色列表
     */
    @Transactional(readOnly = true)
    public PagedResult<RoleListItem> listRolesPaged(int page, int pageSize) {
        int offset = (page - 1) * pageSize;
        Integer total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM sys_role", Integer.class);
        int totalCount = total != null ? total : 0;
        List<RoleListItem> items = jdbcTemplate.query(
                "SELECT r.id, r.role_code, r.role_name, r.status, r.remark, COUNT(DISTINCT sur.user_id) AS user_count, COUNT(DISTINCT srp.permission_id) AS permission_count "
                        + "FROM sys_role r "
                        + "LEFT JOIN sys_user_role sur ON sur.role_id = r.id "
                        + "LEFT JOIN sys_role_permission srp ON srp.role_id = r.id "
                        + "GROUP BY r.id, r.role_code, r.role_name, r.status, r.remark ORDER BY r.id ASC "
                        + "LIMIT ? OFFSET ?",
                (rs, rowNum) -> new RoleListItem(
                        rs.getLong("id"),
                        rs.getString("role_code"),
                        rs.getString("role_name"),
                        rs.getString("status"),
                        rs.getString("remark"),
                        rs.getInt("user_count"),
                        rs.getInt("permission_count")),
                pageSize, offset);
        return new PagedResult<>(items, totalCount, page, pageSize);
    }

    /**
     * @Author tangxinglin
     * @Description //获取指定角色的详情，包含关联权限的ID、编码和名称列表
     * @Date 2026/04/18 10:05
     * @Param [roleId 角色ID]
     * @return RoleDetail 角色详情
     */
    @Transactional(readOnly = true)
    public RoleDetail getRoleDetail(Long roleId) {
        RoleRecord role = requireRole(roleId);
        List<SystemPermissionService.PermissionRecord> permissions = loadPermissions(roleId);
        return new RoleDetail(
                role.id(),
                role.roleCode(),
                role.roleName(),
                role.status(),
                role.remark(),
                permissions.stream().map(SystemPermissionService.PermissionRecord::id).toList(),
                permissions.stream().map(SystemPermissionService.PermissionRecord::permissionCode).toList(),
                permissions.stream().map(SystemPermissionService.PermissionRecord::permissionName).toList());
    }

    /**
     * @Author tangxinglin
     * @Description //创建新角色，校验编码唯一性后写入数据库
     * @Date 2026/04/18 10:05
     * @Param [request 创建角色请求，包含角色编码、名称、状态和备注]
     * @return RoleDetail 新建的角色详情
     */
    @Transactional
    public RoleDetail createRole(CreateRoleRequest request) {
        validateUpsertRequest(request.roleCode(), request.roleName(), null);
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO sys_role (role_code, role_name, status, remark, created_at, updated_at) VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                    Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, request.roleCode().trim());
            statement.setString(2, request.roleName().trim());
            statement.setString(3, normalizeStatus(request.status()));
            statement.setString(4, normalizeRemark(request.remark()));
            return statement;
        }, keyHolder);
        return getRoleDetail(extractGeneratedId(keyHolder));
    }

    /**
     * @Author tangxinglin
     * @Description //更新指定角色的基本信息
     * @Date 2026/04/18 10:05
     * @Param [roleId 角色ID, request 更新角色请求]
     * @return RoleDetail 更新后的角色详情
     */
    @Transactional
    public RoleDetail updateRole(Long roleId, UpdateRoleRequest request) {
        requireRole(roleId);
        validateUpsertRequest(request.roleCode(), request.roleName(), roleId);
        jdbcTemplate.update(
                "UPDATE sys_role SET role_code = ?, role_name = ?, status = ?, remark = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?",
                request.roleCode().trim(),
                request.roleName().trim(),
                normalizeStatus(request.status()),
                normalizeRemark(request.remark()),
                roleId);
        return getRoleDetail(roleId);
    }

    /**
     * @Author tangxinglin
     * @Description //为指定角色批量分配权限，先清空原有权限再插入新权限
     * @Date 2026/04/18 10:05
     * @Param [roleId 角色ID, request 权限分配请求，包含权限ID列表]
     * @return RoleDetail 更新后的角色详情
     */
    @Transactional
    public RoleDetail assignPermissions(Long roleId, AssignPermissionsRequest request) {
        requireRole(roleId);
        List<Long> permissionIds = normalizeRolePermissionIds(systemPermissionService.requirePermissionIds(request.permissionIds()));
        jdbcTemplate.update("DELETE FROM sys_role_permission WHERE role_id = ?", roleId);
        for (Long permissionId : permissionIds) {
            jdbcTemplate.update(
                    "INSERT INTO sys_role_permission (role_id, permission_id, created_at, updated_at) VALUES (?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                    roleId,
                    permissionId);
        }
        return getRoleDetail(roleId);
    }

    /**
     * @Description //删除角色：内置角色与仍有用户绑定的角色不可删，删除时同步清理角色-权限关联
     * @Param [roleId 角色ID]
     * @return void
     */
    @Transactional
    public void deleteRole(Long roleId) {
        RoleRecord role = requireRole(roleId);
        // 内置角色禁止删除，避免删掉超管后系统不可维护
        if ("SUPER_ADMIN".equals(role.roleCode())) {
            throw new BusinessException("SYSTEM_ROLE_BUILTIN", "超级管理员角色不可删除");
        }
        Integer userCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sys_user_role WHERE role_id = ?", Integer.class, roleId);
        if (userCount != null && userCount > 0) {
            throw new BusinessException("SYSTEM_ROLE_IN_USE", "该角色下还有 " + userCount + " 个用户，请先调整用户角色后再删除");
        }
        jdbcTemplate.update("DELETE FROM sys_role_permission WHERE role_id = ?", roleId);
        jdbcTemplate.update("DELETE FROM sys_role WHERE id = ?", roleId);
    }

    /**
     * @Author tangxinglin
     * @Description //校验角色ID列表合法性，确保所有ID在数据库中存在，返回去重后的ID列表
     * @Date 2026/04/18 10:05
     * @Param [roleIds 角色ID列表]
     * @return List<Long> 去重后的合法角色ID列表
     */
    @Transactional(readOnly = true)
    public List<Long> requireRoleIds(List<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return List.of();
        }
        List<Long> normalizedIds = roleIds.stream().distinct().toList();
        String placeholders = String.join(",", java.util.Collections.nCopies(normalizedIds.size(), "?"));
        List<Long> existingIds = jdbcTemplate.query(
                "SELECT id FROM sys_role WHERE id IN (" + placeholders + ") ORDER BY id ASC",
                (rs, rowNum) -> rs.getLong("id"),
                normalizedIds.toArray());
        if (existingIds.size() != normalizedIds.size()) {
            throw new BusinessException("SYSTEM_ROLE_NOT_FOUND", "角色不存在");
        }
        return normalizedIds;
    }

    /**
     * @Author tangxinglin
     * @Description //根据角色ID查询角色记录，不存在时抛出业务异常
     * @Date 2026/04/18 10:05
     * @Param [roleId 角色ID]
     * @return RoleRecord 角色记录
     */
    @Transactional(readOnly = true)
    public RoleRecord requireRole(Long roleId) {
        List<RoleRecord> roles = jdbcTemplate.query(
                "SELECT id, role_code, role_name, status, remark FROM sys_role WHERE id = ?",
                (rs, rowNum) -> new RoleRecord(
                        rs.getLong("id"),
                        rs.getString("role_code"),
                        rs.getString("role_name"),
                        rs.getString("status"),
                        rs.getString("remark")),
                roleId);
        if (roles.isEmpty()) {
            throw new BusinessException("SYSTEM_ROLE_NOT_FOUND", "角色未找到");
        }
        return roles.get(0);
    }

    /**
     * @Author tangxinglin
     * @Description //加载指定角色关联的权限记录列表
     * @Date 2026/04/18 10:05
     * @Param [roleId 角色ID]
     * @return List<SystemPermissionService.PermissionRecord> 权限记录列表
     */
    private List<SystemPermissionService.PermissionRecord> loadPermissions(Long roleId) {
        return jdbcTemplate.query(
                "SELECT p.id, p.permission_code, p.permission_name, p.permission_type, p.client_type, p.parent_id, p.path, p.component, p.icon, p.sort_order, p.status, p.remark "
                        + "FROM sys_role_permission srp JOIN sys_permission p ON p.id = srp.permission_id "
                        + "WHERE srp.role_id = ? ORDER BY p.sort_order ASC, p.id ASC",
                (rs, rowNum) -> new SystemPermissionService.PermissionRecord(
                        rs.getLong("id"),
                        rs.getString("permission_code"),
                        rs.getString("permission_name"),
                        rs.getString("permission_type"),
                        rs.getString("client_type"),
                        rs.getObject("parent_id") == null ? null : rs.getLong("parent_id"),
                        rs.getString("path"),
                        rs.getString("component"),
                        rs.getString("icon"),
                        rs.getInt("sort_order"),
                        rs.getString("status"),
                        rs.getString("remark")),
                roleId);
    }

    private List<Long> normalizeRolePermissionIds(List<Long> permissionIds) {
        if (permissionIds == null || permissionIds.isEmpty()) {
            return List.of();
        }
        String placeholders = String.join(",", java.util.Collections.nCopies(permissionIds.size(), "?"));
        Set<Long> configurablePermissionIds = new LinkedHashSet<>(jdbcTemplate.query(
                "SELECT id FROM sys_permission WHERE id IN (" + placeholders + ") AND permission_type IN ('CATALOG', 'MENU') ORDER BY sort_order ASC, id ASC",
                (rs, rowNum) -> rs.getLong("id"),
                permissionIds.toArray()));

        // 角色授权只保存菜单体系权限；API 是接口入口，不作为角色配置项持久化。
        return permissionIds.stream()
                .filter(configurablePermissionIds::contains)
                .distinct()
                .toList();
    }

    /**
     * @Author tangxinglin
     * @Description //校验创建/更新角色请求的字段合法性，包括编码唯一性校验
     * @Date 2026/04/18 10:05
     * @Param [roleCode 角色编码, roleName 角色名称, roleId 当前角色ID（更新时传入）]
     * @return void
     */
    private void validateUpsertRequest(String roleCode, String roleName, Long roleId) {
        if (!StringUtils.hasText(roleCode)) {
            throw new BusinessException("VALIDATION_ERROR", "角色编码不能为空");
        }
        if (!StringUtils.hasText(roleName)) {
            throw new BusinessException("VALIDATION_ERROR", "角色名称不能为空");
        }
        Integer count = jdbcTemplate.queryForObject(
                roleId == null
                        ? "SELECT COUNT(*) FROM sys_role WHERE role_code = ?"
                        : "SELECT COUNT(*) FROM sys_role WHERE role_code = ? AND id <> ?",
                Integer.class,
                roleId == null ? new Object[]{roleCode.trim()} : new Object[]{roleCode.trim(), roleId});
        if (count != null && count > 0) {
            throw new BusinessException("SYSTEM_ROLE_CODE_EXISTS", "角色编码已存在");
        }
    }

    /**
     * @Author tangxinglin
     * @Description //规范化角色状态，默认为ACTIVE，校验合法值（ACTIVE/DISABLED）
     * @Date 2026/04/18 10:05
     * @Param [status 状态字符串]
     * @return String 规范化后的状态值
     */
    private String normalizeStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return "ACTIVE";
        }
        String normalized = status.trim().toUpperCase();
        if (!Set.of("ACTIVE", "DISABLED").contains(normalized)) {
            throw new BusinessException("VALIDATION_ERROR", "不支持的角色状态: " + status);
        }
        return normalized;
    }

    /**
     * @Author tangxinglin
     * @Description //规范化备注字段，空白时返回null
     * @Date 2026/04/18 10:05
     * @Param [remark 备注字符串]
     * @return String 规范化后的备注或null
     */
    private String normalizeRemark(String remark) {
        return StringUtils.hasText(remark) ? remark.trim() : null;
    }

    /**
     * @Author tangxinglin
     * @Description //从KeyHolder中提取自动生成的主键ID
     * @Date 2026/04/18 10:05
     * @Param [keyHolder 持有生成主键的KeyHolder]
     * @return Long 生成的主键ID
     */
    private Long extractGeneratedId(KeyHolder keyHolder) {
        java.util.Map<String, Object> keys = keyHolder.getKeys();
        if (keys != null && keys.containsKey("ID")) {
            return ((Number) keys.get("ID")).longValue();
        }
        Number key = keyHolder.getKey();
        if (key != null) {
            return key.longValue();
        }
        throw new BusinessException("SYSTEM_ROLE_CREATE_FAILED", "创建角色失败");
    }

    public record CreateRoleRequest(String roleCode, String roleName, String status, String remark) {
    }

    public record UpdateRoleRequest(String roleCode, String roleName, String status, String remark) {
    }

    public record AssignPermissionsRequest(List<Long> permissionIds) {
    }

    public record RoleListItem(Long id, String roleCode, String roleName, String status, String remark, Integer userCount, Integer permissionCount) {
    }

    public record RoleDetail(
            Long id,
            String roleCode,
            String roleName,
            String status,
            String remark,
            List<Long> permissionIds,
            List<String> permissionCodes,
            List<String> permissionNames) {
    }

    public record RoleRecord(Long id, String roleCode, String roleName, String status, String remark) {
    }
}
