package com.changping.platform.modules.system.service;

import com.changping.platform.common.exception.BusinessException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * @Author lxy
 * @Description //系统权限服务，提供权限树查询、菜单树查询、权限记录获取及权限ID校验等功能
 * @Date 2026/04/18 10:00
 */
@Service
public class SystemPermissionService {

    private final JdbcTemplate jdbcTemplate;

    /**
     * @Author lxy
     * @Description //构造函数，注入数据库操作模板
     * @Date 2026/04/18 10:00
     * @Param [jdbcTemplate 数据库操作模板]
     * @return
     */
    public SystemPermissionService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * @Author lxy
     * @Description //查询权限树，支持按权限类型过滤，不传则返回全量权限树
     * @Date 2026/04/18 10:00
     * @Param [permissionType 权限类型筛选，可为null]
     * @return List<PermissionTreeNode> 权限树节点列表
     */
    @Transactional(readOnly = true)
    public List<PermissionTreeNode> listPermissionTree(String permissionType) {
        List<PermissionRecord> permissions;
        if (StringUtils.hasText(permissionType)) {
            permissions = jdbcTemplate.query(
                    "SELECT id, permission_code, permission_name, permission_type, client_type, parent_id, path, component, icon, sort_order, status, remark "
                            + "FROM sys_permission WHERE permission_type = ? ORDER BY sort_order ASC, id ASC",
                    (rs, rowNum) -> new PermissionRecord(
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
                    permissionType.trim().toUpperCase());
        } else {
            permissions = loadAllPermissions();
        }
        return buildTree(permissions);
    }

    /**
     * @Author lxy
     * @Description //查询菜单树，仅返回类型为CATALOG/MENU/BUTTON的权限树
     * @Date 2026/04/18 10:00
     * @Param []
     * @return List<PermissionTreeNode> 菜单权限树节点列表
     */
    @Transactional(readOnly = true)
    public List<PermissionTreeNode> listMenuTree() {
        return buildTree(jdbcTemplate.query(
                "SELECT id, permission_code, permission_name, permission_type, client_type, parent_id, path, component, icon, sort_order, status, remark "
                        + "FROM sys_permission WHERE permission_type IN ('CATALOG', 'MENU', 'BUTTON') ORDER BY sort_order ASC, id ASC",
                (rs, rowNum) -> new PermissionRecord(
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
                        rs.getString("remark"))));
    }

    /**
     * @Author lxy
     * @Description //根据当前用户权限码返回指定客户端可访问菜单树，包含已授权菜单的祖先目录，避免树结构断裂
     * @Date 2026/05/21 10:00
     * @Param [permissionCodes 当前用户权限码列表, clientType 客户端类型]
     * @return List<PermissionTreeNode> 当前用户可访问菜单树
     */
    @Transactional(readOnly = true)
    public List<PermissionTreeNode> listAccessibleMenuTree(List<String> permissionCodes, String clientType) {
        if (permissionCodes == null || permissionCodes.isEmpty() || !StringUtils.hasText(clientType)) {
            return List.of();
        }

        String normalizedClientType = clientType.trim().toUpperCase();
        List<PermissionRecord> activeMenus = jdbcTemplate.query(
                "SELECT id, permission_code, permission_name, permission_type, client_type, parent_id, path, component, icon, sort_order, status, remark "
                        + "FROM sys_permission WHERE status = 'ACTIVE' AND client_type = ? "
                        + "AND permission_type IN ('CATALOG', 'MENU') ORDER BY sort_order ASC, id ASC",
                (rs, rowNum) -> new PermissionRecord(
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
                normalizedClientType);
        if (activeMenus.isEmpty()) {
            return List.of();
        }

        Set<String> grantedPermissionCodes = permissionCodes.stream()
                .filter(StringUtils::hasText)
                .collect(Collectors.toSet());
        Map<Long, PermissionRecord> permissionMap = activeMenus.stream()
                .collect(Collectors.toMap(PermissionRecord::id, permission -> permission, (left, right) -> left, LinkedHashMap::new));
        Set<Long> includedIds = new LinkedHashSet<>();
        for (PermissionRecord permission : activeMenus) {
            if (!"MENU".equals(permission.permissionType()) || !grantedPermissionCodes.contains(permission.permissionCode())) {
                continue;
            }
            PermissionRecord current = permission;
            while (current != null && includedIds.add(current.id())) {
                current = current.parentId() == null ? null : permissionMap.get(current.parentId());
            }
        }
        if (includedIds.isEmpty()) {
            return List.of();
        }

        List<PermissionRecord> accessibleMenus = activeMenus.stream()
                .filter(permission -> includedIds.contains(permission.id()))
                .toList();
        return buildTree(accessibleMenus);
    }

    /**
     * @Author lxy
     * @Description //根据ID获取单条权限记录，不存在时抛出业务异常
     * @Date 2026/04/18 10:00
     * @Param [permissionId 权限ID]
     * @return PermissionRecord 权限记录
     */
    @Transactional(readOnly = true)
    public PermissionRecord getPermission(Long permissionId) {
        List<PermissionRecord> permissions = jdbcTemplate.query(
                "SELECT id, permission_code, permission_name, permission_type, client_type, parent_id, path, component, icon, sort_order, status, remark "
                        + "FROM sys_permission WHERE id = ?",
                (rs, rowNum) -> new PermissionRecord(
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
                permissionId);
        if (permissions.isEmpty()) {
            throw new BusinessException("SYSTEM_PERMISSION_NOT_FOUND", "权限未找到");
        }
        return permissions.get(0);
    }

    /**
     * @Author lxy
     * @Description //校验权限ID列表的合法性，确保所有ID在数据库中存在，返回去重后的ID列表
     * @Date 2026/04/18 10:00
     * @Param [permissionIds 权限ID列表]
     * @return List<Long> 去重后的合法权限ID列表
     */
    @Transactional(readOnly = true)
    public List<Long> requirePermissionIds(List<Long> permissionIds) {
        if (permissionIds == null || permissionIds.isEmpty()) {
            return List.of();
        }
        String placeholders = String.join(",", java.util.Collections.nCopies(permissionIds.size(), "?"));
        List<Long> existingIds = jdbcTemplate.query(
                "SELECT id FROM sys_permission WHERE id IN (" + placeholders + ") ORDER BY id ASC",
                (rs, rowNum) -> rs.getLong("id"),
                permissionIds.toArray());
        if (existingIds.size() != permissionIds.size()) {
            throw new BusinessException("SYSTEM_PERMISSION_NOT_FOUND", "权限不存在");
        }
        return permissionIds.stream().distinct().toList();
    }

    /**
     * @Author lxy
     * @Description //加载所有权限记录，按排序值和ID升序排列
     * @Date 2026/04/18 10:00
     * @Param []
     * @return List<PermissionRecord> 全量权限记录列表
     */
    @Transactional(readOnly = true)
    public List<PermissionRecord> loadAllPermissions() {
        return jdbcTemplate.query(
                "SELECT id, permission_code, permission_name, permission_type, client_type, parent_id, path, component, icon, sort_order, status, remark "
                        + "FROM sys_permission ORDER BY sort_order ASC, id ASC",
                (rs, rowNum) -> new PermissionRecord(
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
                        rs.getString("remark")));
    }

    /**
     * @Author lxy
     * @Description //将权限记录列表构建为树形结构，无父节点或父节点不在列表中的记录作为根节点
     * @Date 2026/04/18 10:00
     * @Param [permissions 权限记录列表]
     * @return List<PermissionTreeNode> 树形结构的权限节点列表
     */
    List<PermissionTreeNode> buildTree(List<PermissionRecord> permissions) {
        Map<Long, PermissionTreeNode> nodeMap = new LinkedHashMap<>();
        List<PermissionTreeNode> roots = new ArrayList<>();
        for (PermissionRecord permission : permissions) {
            nodeMap.put(permission.id(), new PermissionTreeNode(
                    permission.id(),
                    permission.permissionCode(),
                    permission.permissionName(),
                    permission.permissionType(),
                    permission.clientType(),
                    permission.parentId(),
                    permission.path(),
                    permission.component(),
                    permission.icon(),
                    permission.sortOrder(),
                    permission.status(),
                    permission.remark(),
                    new ArrayList<>()));
        }
        for (PermissionTreeNode node : nodeMap.values()) {
            if (node.parentId() == null || !nodeMap.containsKey(node.parentId())) {
                roots.add(node);
            } else {
                nodeMap.get(node.parentId()).children().add(node);
            }
        }
        return roots;
    }

    public record PermissionRecord(
            Long id,
            String permissionCode,
            String permissionName,
            String permissionType,
            String clientType,
            Long parentId,
            String path,
            String component,
            String icon,
            Integer sortOrder,
            String status,
            String remark) {
    }

    public record PermissionTreeNode(
            Long id,
            String permissionCode,
            String permissionName,
            String permissionType,
            String clientType,
            Long parentId,
            String path,
            String component,
            String icon,
            Integer sortOrder,
            String status,
            String remark,
            List<PermissionTreeNode> children) {
    }
}
