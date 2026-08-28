package com.changping.platform.modules.system.service;

import com.changping.platform.common.exception.BusinessException;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Set;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * @Author lxy
 * @Description //系统菜单服务，提供菜单树查询、菜单创建、更新及删除的业务逻辑
 * @Date 2026/04/18 09:55
 */
@Service
public class SystemMenuService {

    private final JdbcTemplate jdbcTemplate;
    private final SystemPermissionService systemPermissionService;

    /**
     * @Author lxy
     * @Description //构造函数，注入数据库操作模板和权限服务
     * @Date 2026/04/18 09:55
     * @Param [jdbcTemplate 数据库操作模板, systemPermissionService 权限服务]
     * @return
     */
    public SystemMenuService(JdbcTemplate jdbcTemplate, SystemPermissionService systemPermissionService) {
        this.jdbcTemplate = jdbcTemplate;
        this.systemPermissionService = systemPermissionService;
    }

    /**
     * @Author lxy
     * @Description //查询菜单树，返回目录、菜单、按钮类型的权限树形结构
     * @Date 2026/04/18 09:55
     * @Param []
     * @return List<SystemPermissionService.PermissionTreeNode> 菜单权限树
     */
    @Transactional(readOnly = true)
    public List<SystemPermissionService.PermissionTreeNode> listMenuTree() {
        return systemPermissionService.listMenuTree();
    }

    /**
     * @Author lxy
     * @Description //创建菜单权限项，校验编码唯一性及字段合法性后写入数据库
     * @Date 2026/04/18 09:55
     * @Param [request 创建菜单请求，包含权限编码、名称、类型、客户端类型、路径等]
     * @return SystemPermissionService.PermissionRecord 新建的权限记录
     */
    @Transactional
    public SystemPermissionService.PermissionRecord createMenu(CreateMenuRequest request) {
        validateRequest(request.permissionCode(), request.permissionName(), request.permissionType(), request.clientType(), request.parentId(), request.sortOrder(), null);
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO sys_permission (permission_code, permission_name, permission_type, client_type, parent_id, path, component, icon, sort_order, status, remark, created_at, updated_at) "
                            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                    Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, request.permissionCode().trim());
            statement.setString(2, request.permissionName().trim());
            statement.setString(3, normalizePermissionType(request.permissionType()));
            statement.setString(4, normalizeClientType(request.clientType()));
            if (request.parentId() == null) {
                statement.setObject(5, null);
            } else {
                statement.setLong(5, request.parentId());
            }
            statement.setString(6, normalizeNullable(request.path()));
            statement.setString(7, normalizeNullable(request.component()));
            statement.setString(8, normalizeNullable(request.icon()));
            statement.setInt(9, request.sortOrder() == null ? 0 : request.sortOrder());
            statement.setString(10, normalizeStatus(request.status()));
            statement.setString(11, normalizeNullable(request.remark()));
            return statement;
        }, keyHolder);
        return systemPermissionService.getPermission(extractGeneratedId(keyHolder));
    }

    /**
     * @Author lxy
     * @Description //更新指定菜单权限项，校验编码唯一性后更新数据库
     * @Date 2026/04/18 09:55
     * @Param [permissionId 权限ID, request 更新菜单请求]
     * @return SystemPermissionService.PermissionRecord 更新后的权限记录
     */
    @Transactional
    public SystemPermissionService.PermissionRecord updateMenu(Long permissionId, UpdateMenuRequest request) {
        SystemPermissionService.PermissionRecord current = systemPermissionService.getPermission(permissionId);
        validateRequest(request.permissionCode(), request.permissionName(), request.permissionType(), request.clientType(), request.parentId(), request.sortOrder(), permissionId);
        jdbcTemplate.update(
                "UPDATE sys_permission SET permission_code = ?, permission_name = ?, permission_type = ?, client_type = ?, parent_id = ?, path = ?, component = ?, icon = ?, sort_order = ?, status = ?, remark = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?",
                request.permissionCode().trim(),
                request.permissionName().trim(),
                normalizePermissionType(request.permissionType()),
                normalizeClientType(request.clientType()),
                request.parentId(),
                normalizeNullable(request.path()),
                normalizeNullable(request.component()),
                normalizeNullable(request.icon()),
                request.sortOrder() == null ? current.sortOrder() : request.sortOrder(),
                normalizeStatus(request.status()),
                normalizeNullable(request.remark()),
                permissionId);
        return systemPermissionService.getPermission(permissionId);
    }

    /**
     * @Author lxy
     * @Description //删除指定菜单权限项，校验无子项且未被角色引用后执行删除
     * @Date 2026/04/18 09:55
     * @Param [permissionId 权限ID]
     * @return void
     */
    @Transactional
    public void deleteMenu(Long permissionId) {
        SystemPermissionService.PermissionRecord permission = systemPermissionService.getPermission(permissionId);
        if (!Set.of("CATALOG", "MENU", "BUTTON").contains(permission.permissionType())) {
            throw new BusinessException("SYSTEM_MENU_DELETE_FORBIDDEN", "此处只能删除菜单资源");
        }
        Integer childCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM sys_permission WHERE parent_id = ?", Integer.class, permissionId);
        if (childCount != null && childCount > 0) {
            throw new BusinessException("SYSTEM_MENU_HAS_CHILDREN", "菜单包含子资源，无法删除");
        }
        Integer roleRefCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM sys_role_permission WHERE permission_id = ?", Integer.class, permissionId);
        if (roleRefCount != null && roleRefCount > 0) {
            throw new BusinessException("SYSTEM_MENU_ROLE_REFERENCED", "菜单被角色引用，无法删除");
        }
        jdbcTemplate.update("DELETE FROM sys_permission WHERE id = ?", permissionId);
    }

    /**
     * @Author lxy
     * @Description //校验创建/更新菜单请求的字段合法性，包括编码唯一性、上级菜单存在性等
     * @Date 2026/04/18 09:55
     * @Param [permissionCode 权限编码, permissionName 权限名称, permissionType 权限类型, clientType 客户端类型, parentId 上级ID, sortOrder 排序值, permissionId 当前权限ID（更新时传入）]
     * @return void
     */
    private void validateRequest(String permissionCode, String permissionName, String permissionType, String clientType, Long parentId, Integer sortOrder, Long permissionId) {
        if (!StringUtils.hasText(permissionCode)) {
            throw new BusinessException("VALIDATION_ERROR", "权限编码不能为空");
        }
        if (!StringUtils.hasText(permissionName)) {
            throw new BusinessException("VALIDATION_ERROR", "权限名称不能为空");
        }
        normalizePermissionType(permissionType);
        normalizeClientType(clientType);
        if (sortOrder != null && sortOrder < 0) {
            throw new BusinessException("VALIDATION_ERROR", "排序值不能为负数");
        }
        if (parentId != null) {
            systemPermissionService.getPermission(parentId);
            if (permissionId != null && permissionId.equals(parentId)) {
                throw new BusinessException("VALIDATION_ERROR", "上级菜单不能是自身");
            }
        }
        Integer count = jdbcTemplate.queryForObject(
                permissionId == null
                        ? "SELECT COUNT(*) FROM sys_permission WHERE permission_code = ?"
                        : "SELECT COUNT(*) FROM sys_permission WHERE permission_code = ? AND id <> ?",
                Integer.class,
                permissionId == null ? new Object[]{permissionCode.trim()} : new Object[]{permissionCode.trim(), permissionId});
        if (count != null && count > 0) {
            throw new BusinessException("SYSTEM_PERMISSION_CODE_EXISTS", "权限编码已存在");
        }
    }

    /**
     * @Author lxy
     * @Description //规范化权限类型，转换为大写并校验合法值（CATALOG/MENU/BUTTON）
     * @Date 2026/04/18 09:55
     * @Param [permissionType 权限类型字符串]
     * @return String 规范化后的权限类型
     */
    private String normalizePermissionType(String permissionType) {
        if (!StringUtils.hasText(permissionType)) {
            throw new BusinessException("VALIDATION_ERROR", "权限类型不能为空");
        }
        String normalized = permissionType.trim().toUpperCase();
        if (!Set.of("CATALOG", "MENU", "BUTTON").contains(normalized)) {
            throw new BusinessException("VALIDATION_ERROR", "不支持的菜单权限类型: " + permissionType);
        }
        return normalized;
    }

    /**
     * @Author lxy
     * @Description //规范化客户端类型，默认为WEB，转换为大写并校验合法值（WEB/H5）
     * @Date 2026/04/18 09:55
     * @Param [clientType 客户端类型字符串]
     * @return String 规范化后的客户端类型
     */
    private String normalizeClientType(String clientType) {
        if (!StringUtils.hasText(clientType)) {
            return "WEB";
        }
        String normalized = clientType.trim().toUpperCase();
        if (!Set.of("WEB", "H5").contains(normalized)) {
            throw new BusinessException("VALIDATION_ERROR", "不支持的客户端类型: " + clientType);
        }
        return normalized;
    }

    /**
     * @Author lxy
     * @Description //规范化状态值，默认为ACTIVE，转换为大写并校验合法值（ACTIVE/DISABLED）
     * @Date 2026/04/18 09:55
     * @Param [status 状态字符串]
     * @return String 规范化后的状态值
     */
    private String normalizeStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return "ACTIVE";
        }
        String normalized = status.trim().toUpperCase();
        if (!Set.of("ACTIVE", "DISABLED").contains(normalized)) {
            throw new BusinessException("VALIDATION_ERROR", "不支持的状态: " + status);
        }
        return normalized;
    }

    /**
     * @Author lxy
     * @Description //规范化可空字符串，去除首尾空格，空白时返回null
     * @Date 2026/04/18 09:55
     * @Param [value 输入字符串]
     * @return String 规范化后的字符串或null
     */
    private String normalizeNullable(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    /**
     * @Author lxy
     * @Description //从KeyHolder中提取自动生成的主键ID
     * @Date 2026/04/18 09:55
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
        throw new BusinessException("SYSTEM_MENU_CREATE_FAILED", "创建菜单失败");
    }

    public record CreateMenuRequest(
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

    public record UpdateMenuRequest(
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
}
