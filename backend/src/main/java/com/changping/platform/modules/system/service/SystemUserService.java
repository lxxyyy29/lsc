package com.changping.platform.modules.system.service;

import com.changping.platform.common.exception.BusinessException;
import com.changping.platform.common.response.PagedResult;
import com.changping.platform.modules.auth.service.AuthService;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * @Author tangxinglin
 * @Description //系统用户服务，提供用户查询、创建、更新、状态切换、角色分配及密码修改的业务逻辑
 * @Date 2026/04/18 10:10
 */
@Service
public class SystemUserService {

    private static final Pattern PHONE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z][A-Za-z0-9_-]{3,31}$");

    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;
    private final SystemRoleService systemRoleService;
    private final AuthService authService;

    /**
     * @Author tangxinglin
     * @Description //构造函数，注入数据库模板、密码编码器、角色服务和认证服务
     * @Date 2026/04/18 10:10
     * @Param [jdbcTemplate 数据库操作模板, passwordEncoder 密码编码器, systemRoleService 角色服务, authService 认证服务]
     * @return
     */
    public SystemUserService(
            JdbcTemplate jdbcTemplate,
            PasswordEncoder passwordEncoder,
            SystemRoleService systemRoleService,
            AuthService authService) {
        this.jdbcTemplate = jdbcTemplate;
        this.passwordEncoder = passwordEncoder;
        this.systemRoleService = systemRoleService;
        this.authService = authService;
    }

    /**
     * @Author tangxinglin
     * @Description //查询所有用户列表，包含角色编码和角色名称
     * @Date 2026/04/18 10:10
     * @Param []
     * @return List<UserListItem> 用户列表
     */
    @Transactional(readOnly = true)
    public List<UserListItem> listUsers() {
        return jdbcTemplate.query(
                "SELECT u.id, u.username, u.real_name, u.phone, u.status, GROUP_CONCAT(r.role_code ORDER BY r.role_code SEPARATOR ',') AS role_codes, "
                        + "GROUP_CONCAT(r.role_name ORDER BY r.role_code SEPARATOR ',') AS role_names "
                        + "FROM sys_user u "
                        + "LEFT JOIN sys_user_role sur ON sur.user_id = u.id "
                        + "LEFT JOIN sys_role r ON r.id = sur.role_id "
                        + "WHERE u.deleted = 0 "
                        + "GROUP BY u.id, u.username, u.real_name, u.phone, u.status ORDER BY u.id ASC",
                (rs, rowNum) -> new UserListItem(
                        rs.getLong("id"),
                        rs.getString("username"),
                        rs.getString("real_name"),
                        rs.getString("phone"),
                        rs.getString("status"),
                        splitCsv(rs.getString("role_codes")),
                        splitCsv(rs.getString("role_names"))));
    }

    /**
     * @Author tangxinglin
     * @Description //分页查询用户列表，支持按关键字（用户名/姓名/手机号）和状态过滤
     * @Date 2026/04/18 10:10
     * @Param [page 页码, pageSize 每页条数, keyword 关键字，可为null, status 状态，可为null]
     * @return PagedResult<UserListItem> 分页用户列表
     */
    @Transactional(readOnly = true)
    public PagedResult<UserListItem> listUsersPaged(int page, int pageSize, String keyword, String status) {
        int offset = (page - 1) * pageSize;
        StringBuilder where = new StringBuilder();
        java.util.List<Object> params = new java.util.ArrayList<>();
        where.append(" WHERE u.deleted = 0");
        if (org.springframework.util.StringUtils.hasText(keyword)) {
            where.append(" AND (u.username LIKE ? OR u.real_name LIKE ? OR u.phone LIKE ?)");
            String like = "%" + keyword.trim() + "%";
            params.add(like); params.add(like); params.add(like);
        }
        if (org.springframework.util.StringUtils.hasText(status)) {
            where.append(" AND u.status = ?");
            params.add(status.trim().toUpperCase());
        }
        String countSql = "SELECT COUNT(DISTINCT u.id) FROM sys_user u" + where;
        Integer total = jdbcTemplate.queryForObject(countSql, Integer.class, params.toArray());
        int totalCount = total != null ? total : 0;
        String dataSql = "SELECT u.id, u.username, u.real_name, u.phone, u.status, "
                + "GROUP_CONCAT(r.role_code ORDER BY r.role_code SEPARATOR ',') AS role_codes, "
                + "GROUP_CONCAT(r.role_name ORDER BY r.role_code SEPARATOR ',') AS role_names "
                + "FROM sys_user u "
                + "LEFT JOIN sys_user_role sur ON sur.user_id = u.id "
                + "LEFT JOIN sys_role r ON r.id = sur.role_id"
                + where
                + " GROUP BY u.id, u.username, u.real_name, u.phone, u.status ORDER BY u.id ASC LIMIT ? OFFSET ?";
        java.util.List<Object> dataParams = new java.util.ArrayList<>(params);
        dataParams.add(pageSize);
        dataParams.add(offset);
        List<UserListItem> items = jdbcTemplate.query(
                dataSql,
                (rs, rowNum) -> new UserListItem(
                        rs.getLong("id"),
                        rs.getString("username"),
                        rs.getString("real_name"),
                        rs.getString("phone"),
                        rs.getString("status"),
                        splitCsv(rs.getString("role_codes")),
                        splitCsv(rs.getString("role_names"))),
                dataParams.toArray());
        return new PagedResult<>(items, totalCount, page, pageSize);
    }

    /**
     * @Author tangxinglin
     * @Description //获取指定用户详情，包含角色列表和Web端权限编码列表
     * @Date 2026/04/18 10:10
     * @Param [userId 用户ID]
     * @return UserDetail 用户详情
     */
    @Transactional(readOnly = true)
    public UserDetail getUserDetail(Long userId) {
        UserRecord user = requireUser(userId);
        List<RoleAssignment> roles = loadUserRoles(userId);
        List<String> permissionCodes = authService.loadAuthenticatedUser(userId, AuthService.ClientType.WEB.name()).permissionCodes();
        return toUserDetail(user, roles, permissionCodes);
    }

    /**
     * @Author tangxinglin
     * @Description //创建新用户，加密密码后写入数据库并关联角色
     * @Date 2026/04/18 10:10
     * @Param [request 创建用户请求，包含用户名、密码、姓名、手机号、状态和角色ID列表]
     * @return UserDetail 新建的用户详情
     */
    @Transactional
    public UserDetail createUser(CreateUserRequest request) {
        validateCreateRequest(request);
        // 若存在同名软删除记录则复用该行（避免唯一索引冲突），按新账号重建
        Long softDeletedId = jdbcTemplate.query(
                "SELECT id FROM sys_user WHERE username = ? AND deleted = 1",
                rs -> rs.next() ? rs.getLong("id") : null,
                request.username().trim());
        Long userId;
        if (softDeletedId != null) {
            jdbcTemplate.update(
                    "UPDATE sys_user SET password_hash = ?, real_name = ?, phone = ?, status = ?, deleted = 0, role_id = NULL, password_version = password_version + 1, created_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP WHERE id = ?",
                    passwordEncoder.encode(request.password().trim()),
                    request.realName().trim(),
                    normalizeNullable(request.phone()),
                    normalizeStatus(request.status()),
                    softDeletedId);
            userId = softDeletedId;
        } else {
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(connection -> {
                PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO sys_user (username, password_hash, real_name, phone, status, role_id, created_at, updated_at) VALUES (?, ?, ?, ?, ?, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                        Statement.RETURN_GENERATED_KEYS);
                statement.setString(1, request.username().trim());
                statement.setString(2, passwordEncoder.encode(request.password().trim()));
                statement.setString(3, request.realName().trim());
                statement.setString(4, normalizeNullable(request.phone()));
                statement.setString(5, normalizeStatus(request.status()));
                return statement;
            }, keyHolder);
            userId = extractGeneratedId(keyHolder);
        }
        assignRolesInternal(userId, request.roleIds());
        return getUserDetail(userId);
    }

    /**
     * @Author tangxinglin
     * @Description //更新指定用户的基本信息（用户名、姓名、手机号、状态）
     * @Date 2026/04/18 10:10
     * @Param [userId 用户ID, request 更新用户请求]
     * @return UserDetail 更新后的用户详情
     */
    @Transactional
    public UserDetail updateUser(Long userId, UpdateUserRequest request) {
        UserRecord existing = requireUser(userId);
        validateUpdateRequest(request, userId, existing.phone());
        jdbcTemplate.update(
                "UPDATE sys_user SET username = ?, real_name = ?, phone = ?, status = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?",
                request.username().trim(),
                request.realName().trim(),
                normalizeNullable(request.phone()),
                normalizeStatus(request.status()),
                userId);
        return getUserDetail(userId);
    }

    /**
     * @Author tangxinglin
     * @Description //切换指定用户的账号状态（启用/禁用）
     * @Date 2026/04/18 10:10
     * @Param [userId 用户ID, request 状态更新请求，包含目标状态]
     * @return UserDetail 更新后的用户详情
     */
    @Transactional
    public UserDetail toggleStatus(Long userId, UpdateUserStatusRequest request) {
        requireUser(userId);
        String status = normalizeStatus(request.status());
        jdbcTemplate.update("UPDATE sys_user SET status = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?", status, userId);
        UserRecord updatedUser = requireUser(userId);
        List<RoleAssignment> roles = loadUserRoles(userId);
        return toUserDetail(updatedUser, roles, List.of());
    }

    /**
     * @Author tangxinglin
     * @Description //修改指定用户的登录密码，密码长度须在6到64位之间
     * @Date 2026/04/18 10:10
     * @Param [userId 用户ID, newPassword 新密码]
     * @return void
     */
    @Transactional
    public void changePassword(Long userId, String newPassword) {
        requireUser(userId);
        if (!org.springframework.util.StringUtils.hasText(newPassword)) {
            throw new BusinessException("VALIDATION_ERROR", "新密码不能为空");
        }
        String trimmed = newPassword.trim();
        if (trimmed.length() < 6 || trimmed.length() > 64) {
            throw new BusinessException("VALIDATION_ERROR", "密码长度须在 6 到 64 位之间");
        }
        String newHash = passwordEncoder.encode(trimmed);
        jdbcTemplate.update(
                "UPDATE sys_user SET password_hash = ?, password_version = password_version + 1, updated_at = CURRENT_TIMESTAMP WHERE id = ?",
                newHash, userId);
    }

    /**
     * @Author tangxinglin
     * @Description //逻辑删除系统用户：禁止删除当前登录用户，清空角色绑定、停用账号并提升密码版本使存量令牌失效
     * @Date 2026/05/21 17:40
     * @Param [userId 用户ID, currentUserId 当前登录用户ID]
     * @return void
     */
    @Transactional
    public void deleteUser(Long userId, Long currentUserId) {
        requireUser(userId);
        if (userId != null && userId.equals(currentUserId)) {
            throw new BusinessException("SYSTEM_USER_DELETE_SELF_FORBIDDEN", "不能删除当前登录账号");
        }
        jdbcTemplate.update("DELETE FROM sys_user_role WHERE user_id = ?", userId);
        int affected = jdbcTemplate.update(
                "UPDATE sys_user SET deleted = 1, status = 'DISABLED', role_id = NULL, password_version = password_version + 1, updated_at = CURRENT_TIMESTAMP WHERE id = ? AND deleted = 0",
                userId);
        if (affected == 0) {
            throw new BusinessException("SYSTEM_USER_NOT_FOUND", "用户未找到");
        }
    }

    /**
     * @Author tangxinglin
     * @Description //为指定用户分配角色，先清空原有角色关系再插入新角色
     * @Date 2026/04/18 10:10
     * @Param [userId 用户ID, request 角色分配请求，包含角色ID列表]
     * @return UserDetail 更新后的用户详情
     */
    @Transactional
    public UserDetail assignRoles(Long userId, AssignUserRolesRequest request) {
        requireUser(userId);
        assignRolesInternal(userId, request.roleIds());
        return getUserDetail(userId);
    }

    /**
     * @Author tangxinglin
     * @Description //根据用户ID查询用户记录，不存在时抛出业务异常
     * @Date 2026/04/18 10:10
     * @Param [userId 用户ID]
     * @return UserRecord 用户记录
     */
    @Transactional(readOnly = true)
    public UserRecord requireUser(Long userId) {
        List<UserRecord> users = jdbcTemplate.query(
                "SELECT id, username, real_name, phone, status FROM sys_user WHERE id = ? AND deleted = 0",
                (rs, rowNum) -> new UserRecord(
                        rs.getLong("id"),
                        rs.getString("username"),
                        rs.getString("real_name"),
                        rs.getString("phone"),
                        rs.getString("status")),
                userId);
        if (users.isEmpty()) {
            throw new BusinessException("SYSTEM_USER_NOT_FOUND", "用户未找到");
        }
        return users.get(0);
    }

    /**
     * @Author tangxinglin
     * @Description //内部分配角色逻辑：清空用户原有角色并批量插入新角色，同步更新主角色ID
     * @Date 2026/04/18 10:10
     * @Param [userId 用户ID, roleIds 角色ID列表]
     * @return void
     */
    private void assignRolesInternal(Long userId, List<Long> roleIds) {
        List<Long> normalizedRoleIds = systemRoleService.requireRoleIds(roleIds);
        jdbcTemplate.update("DELETE FROM sys_user_role WHERE user_id = ?", userId);
        Long primaryRoleId = null;
        for (Long roleId : normalizedRoleIds) {
            if (primaryRoleId == null) {
                primaryRoleId = roleId;
            }
            jdbcTemplate.update(
                    "INSERT INTO sys_user_role (user_id, role_id, created_at, updated_at) VALUES (?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                    userId,
                    roleId);
        }
        jdbcTemplate.update("UPDATE sys_user SET role_id = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?", primaryRoleId, userId);
    }

    /**
     * @Author tangxinglin
     * @Description //加载指定用户关联的角色列表
     * @Date 2026/04/18 10:10
     * @Param [userId 用户ID]
     * @return List<RoleAssignment> 角色分配列表
     */
    private List<RoleAssignment> loadUserRoles(Long userId) {
        return jdbcTemplate.query(
                "SELECT r.id, r.role_code, r.role_name FROM sys_user_role sur JOIN sys_role r ON r.id = sur.role_id WHERE sur.user_id = ? ORDER BY r.role_code ASC",
                (rs, rowNum) -> new RoleAssignment(rs.getLong("id"), rs.getString("role_code"), rs.getString("role_name")),
                userId);
    }

    /**
     * @Author tangxinglin
     * @Description //校验创建用户请求的字段，包括用户名唯一性、密码非空及基础字段
     * @Date 2026/04/18 10:10
     * @Param [request 创建用户请求]
     * @return void
     */
    private void validateCreateRequest(CreateUserRequest request) {
        validateUsername(request.username(), null);
        if (!StringUtils.hasText(request.phone())) {
            throw new BusinessException("VALIDATION_ERROR", "新增用户时手机号不能为空");
        }
        validatePhone(request.phone(), null);
        if (!StringUtils.hasText(request.password())) {
            throw new BusinessException("VALIDATION_ERROR", "密码不能为空");
        }
        // 每个账号必须且只能对应一个角色
        if (request.roleIds() == null || request.roleIds().size() != 1) {
            throw new BusinessException("VALIDATION_ERROR", "新增账号时必须且只能选择一个角色");
        }
        validateUserFields(request.realName(), request.status());
    }

    /**
     * @Author tangxinglin
     * @Description //校验更新用户请求的字段，包括用户名唯一性和基础字段
     * @Date 2026/04/18 10:10
     * @Param [request 更新用户请求, userId 当前用户ID]
     * @return void
     */
    private void validateUpdateRequest(UpdateUserRequest request, Long userId, String existingPhone) {
        validateUsername(request.username(), userId);
        validatePhone(request.phone(), userId, existingPhone);
        validateUserFields(request.realName(), request.status());
    }

    /**
     * @Author tangxinglin
     * @Description //校验用户名非空及唯一性
     * @Date 2026/04/18 10:10
     * @Param [username 用户名, userId 当前用户ID（更新时传入）]
     * @return void
     */
    private void validateUsername(String username, Long userId) {
        if (!StringUtils.hasText(username)) {
            throw new BusinessException("VALIDATION_ERROR", "用户名不能为空");
        }
        String normalizedUsername = username.trim();
        if (!USERNAME_PATTERN.matcher(normalizedUsername).matches()) {
            throw new BusinessException("VALIDATION_ERROR", "账号需以字母开头，支持字母、数字、下划线和短横线，4-32位");
        }
        Integer count = jdbcTemplate.queryForObject(
                userId == null
                        ? "SELECT COUNT(*) FROM sys_user WHERE username = ? AND deleted = 0"
                        : "SELECT COUNT(*) FROM sys_user WHERE username = ? AND id <> ? AND deleted = 0",
                Integer.class,
                userId == null ? new Object[]{normalizedUsername} : new Object[]{normalizedUsername, userId});
        if (count != null && count > 0) {
            throw new BusinessException("SYSTEM_USER_USERNAME_EXISTS", "用户名已存在");
        }
    }

    private void validatePhone(String phone, Long userId) {
        validatePhone(phone, userId, null);
    }

    private void validatePhone(String phone, Long userId, String existingPhone) {
        String normalizedPhone = normalizeNullable(phone);
        if (normalizedPhone == null || normalizedPhone.equals(normalizeNullable(existingPhone))) {
            return;
        }
        if (!PHONE_PATTERN.matcher(normalizedPhone).matches()) {
            throw new BusinessException("VALIDATION_ERROR", "请输入正确的11位手机号");
        }
        Integer count = jdbcTemplate.queryForObject(
                userId == null
                        ? "SELECT COUNT(*) FROM sys_user WHERE phone = ? AND deleted = 0"
                        : "SELECT COUNT(*) FROM sys_user WHERE phone = ? AND id <> ? AND deleted = 0",
                Integer.class,
                userId == null ? new Object[]{normalizedPhone} : new Object[]{normalizedPhone, userId});
        if (count != null && count > 0) {
            throw new BusinessException("SYSTEM_USER_PHONE_EXISTS", "手机号已被其他账号使用");
        }
    }

    /**
     * @Author tangxinglin
     * @Description //校验用户姓名和状态字段的合法性
     * @Date 2026/04/18 10:10
     * @Param [realName 姓名, status 状态]
     * @return void
     */
    private void validateUserFields(String realName, String status) {
        if (!StringUtils.hasText(realName)) {
            throw new BusinessException("VALIDATION_ERROR", "姓名不能为空");
        }
        normalizeStatus(status);
    }

    /**
     * @Author tangxinglin
     * @Description //规范化用户状态，默认为ACTIVE，校验合法值（ACTIVE/DISABLED）
     * @Date 2026/04/18 10:10
     * @Param [status 状态字符串]
     * @return String 规范化后的状态值
     */
    private String normalizeStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return "ACTIVE";
        }
        String normalized = status.trim().toUpperCase();
        if (!Set.of("ACTIVE", "DISABLED").contains(normalized)) {
            throw new BusinessException("VALIDATION_ERROR", "不支持的用户状态: " + status);
        }
        return normalized;
    }

    /**
     * @Author tangxinglin
     * @Description //规范化可空字符串，空白时返回null
     * @Date 2026/04/18 10:10
     * @Param [value 输入字符串]
     * @return String 规范化后的字符串或null
     */
    private String normalizeNullable(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    /**
     * @Author tangxinglin
     * @Description //将逗号分隔的字符串拆分为列表，过滤空白项
     * @Date 2026/04/18 10:10
     * @Param [value 逗号分隔字符串]
     * @return List<String> 拆分后的字符串列表
     */
    private List<String> splitCsv(String value) {
        if (!StringUtils.hasText(value)) {
            return List.of();
        }
        return java.util.Arrays.stream(value.split(",")).map(String::trim).filter(StringUtils::hasText).toList();
    }

    /**
     * @Author tangxinglin
     * @Description //将用户记录、角色分配和权限编码列表组装为UserDetail对象
     * @Date 2026/04/18 10:10
     * @Param [user 用户记录, roles 角色分配列表, permissionCodes 权限编码列表]
     * @return UserDetail 用户详情
     */
    private UserDetail toUserDetail(UserRecord user, List<RoleAssignment> roles, List<String> permissionCodes) {
        return new UserDetail(
                user.id(),
                user.username(),
                user.realName(),
                user.phone(),
                user.status(),
                roles.stream().map(RoleAssignment::roleId).toList(),
                roles.stream().map(RoleAssignment::roleCode).toList(),
                roles.stream().map(RoleAssignment::roleName).toList(),
                permissionCodes);
    }

    /**
     * @Author tangxinglin
     * @Description //从KeyHolder中提取自动生成的主键ID
     * @Date 2026/04/18 10:10
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
        throw new BusinessException("SYSTEM_USER_CREATE_FAILED", "创建用户失败");
    }

    public record CreateUserRequest(String username, String password, String realName, String phone, String status, List<Long> roleIds) {
    }

    public record UpdateUserRequest(String username, String realName, String phone, String status) {
    }

    public record UpdateUserStatusRequest(String status) {
    }

    public record AssignUserRolesRequest(List<Long> roleIds) {
    }

    public record UserListItem(Long id, String username, String realName, String phone, String status, List<String> roleCodes, List<String> roleNames) {
    }

    public record UserDetail(
            Long id,
            String username,
            String realName,
            String phone,
            String status,
            List<Long> roleIds,
            List<String> roleCodes,
            List<String> roleNames,
            List<String> permissionCodes) {
    }

    public record UserRecord(Long id, String username, String realName, String phone, String status) {
    }

    public record RoleAssignment(Long roleId, String roleCode, String roleName) {
    }
}
