package com.changping.platform.modules.auth.service;

import com.changping.platform.common.exception.BusinessException;
import com.changping.platform.modules.auth.model.AuthenticatedUser;
import com.changping.platform.modules.auth.security.JwtTokenService;
import com.changping.platform.modules.auth.security.LoginAttemptService;
import com.changping.platform.modules.auth.security.PermissionCodes;
import com.changping.platform.modules.auth.vo.CurrentUserVo;
import com.changping.platform.modules.auth.vo.LoginResponse;
import com.changping.platform.modules.system.service.SystemPermissionService;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @Author lxy
 * @Description //认证服务，负责用户登录验证、JWT令牌生成、用户权限加载及客户端类型管理，支持 Web 端和 H5 端分离的入口权限校验
 * @Date 2026/04/18 10:05
 */
@Service
public class AuthService {

    private static final String H5_MENU_PREFIX = "menu:h5:";

    private static final List<String> H5_ENTRY_PERMISSIONS = List.of(
            PermissionCodes.MENU_H5_WORKBENCH_VIEW,
            PermissionCodes.MENU_H5_WORKORDER_LIST);

    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;
    private final SystemPermissionService systemPermissionService;
    private final LoginAttemptService loginAttemptService;

    /**
     * @Author lxy
     * @Description //构造函数注入 JDBC 模板、密码编码器和 JWT 令牌服务
     * @Date 2026/04/18 10:05
     * @Param [jdbcTemplate JDBC 模板, passwordEncoder 密码编码器, jwtTokenService JWT 令牌服务]
     * @return void
     */
    public AuthService(
            JdbcTemplate jdbcTemplate,
            PasswordEncoder passwordEncoder,
            JwtTokenService jwtTokenService,
            SystemPermissionService systemPermissionService,
            LoginAttemptService loginAttemptService) {
        this.jdbcTemplate = jdbcTemplate;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
        this.systemPermissionService = systemPermissionService;
        this.loginAttemptService = loginAttemptService;
    }

    /**
     * @Author lxy
     * @Description //用户登录：校验账号密码、验证用户状态、加载角色和权限、校验客户端入口权限，成功后生成 JWT 令牌并返回登录响应
     * @Date 2026/04/18 10:05
     * @Param [account 登录账号, rawPassword 明文密码, clientType 客户端类型（WEB 或 H5）]
     * @return LoginResponse 登录成功响应，包含 JWT 令牌和用户权限信息
     */
    @Transactional(readOnly = true)
    public LoginResponse login(String account, String rawPassword, ClientType clientType) {
        loginAttemptService.assertAllowed(account, clientType.name());
        try {
            UserRecord user = loadUserByAccount(account);
            validateUserStatus(user);
            verifyPassword(rawPassword, user.passwordHash());

            List<String> roleCodes = loadRoleCodes(user.id(), user.legacyRoleId());
            List<String> permissionCodes = loadPermissionCodes(user.id(), user.legacyRoleId());
            enforceEntryPermission(clientType, permissionCodes);

            AuthenticatedUser authenticatedUser = new AuthenticatedUser(
                    user.id(),
                    user.username(),
                    user.realName(),
                    clientType.name(),
                    roleCodes,
                    permissionCodes,
                    user.passwordVersion());

            String accessToken = jwtTokenService.generateAccessToken(authenticatedUser);
            List<SystemPermissionService.PermissionTreeNode> menuTree = resolveMenuTree(permissionCodes, clientType.name());
            loginAttemptService.recordSuccess(account, clientType.name());
            return new LoginResponse(
                    accessToken,
                    user.id(),
                    user.realName(),
                    user.username(),
                    roleCodes,
                    permissionCodes,
                    menuTree);
        } catch (BusinessException exception) {
            if ("AUTH_INVALID_CREDENTIALS".equals(exception.getCode())) {
                loginAttemptService.recordFailure(account, clientType.name());
            }
            throw exception;
        }
    }

    /**
     * @Author lxy
     * @Description //手机号验证码登录：按手机号查用户、校验状态、加载角色权限、按角色决定客户端类型（网格员=H5/居民=WEB），生成 JWT
     * @Date 2026/08/11 14:00
     * @Param [phone 手机号]
     * @return LoginResponse 登录成功响应
     */
    @Transactional(readOnly = true)
    public LoginResponse loginByPhone(String phone) {
        UserRecord user = loadUserByPhone(phone);
        validateUserStatus(user);

        List<String> roleCodes = loadRoleCodes(user.id(), user.legacyRoleId());
        List<String> permissionCodes = loadPermissionCodes(user.id(), user.legacyRoleId());

        // 按角色决定客户端类型：拥有 H5 入口权限（menu:h5:workbench:view / menu:h5:workorder:list）视为网格员，走 H5；
        // 否则视为居民，走 WEB
        boolean isGridWorker = permissionCodes.stream().anyMatch(H5_ENTRY_PERMISSIONS::contains);
        ClientType clientType = isGridWorker ? ClientType.H5 : ClientType.WEB;

        AuthenticatedUser authenticatedUser = new AuthenticatedUser(
                user.id(),
                user.username(),
                user.realName(),
                clientType.name(),
                roleCodes,
                permissionCodes,
                user.passwordVersion());

        String accessToken = jwtTokenService.generateAccessToken(authenticatedUser);
        List<SystemPermissionService.PermissionTreeNode> menuTree = resolveMenuTree(permissionCodes, clientType.name());
        loginAttemptService.recordSuccess(phone, clientType.name());
        return new LoginResponse(
                accessToken,
                user.id(),
                user.realName(),
                user.username(),
                roleCodes,
                permissionCodes,
                menuTree);
    }

    /**
     * @Author lxy
     * @Description //将已认证用户对象转换为当前用户视图对象，过滤出菜单类权限码
     * @Date 2026/04/18 10:05
     * @Param [user 已认证用户对象]
     * @return CurrentUserVo 当前用户视图对象
     */
    public CurrentUserVo toCurrentUserVo(AuthenticatedUser user) {
        List<String> menuPermissionCodes = filterMenuPermissionCodes(user.permissionCodes());
        List<SystemPermissionService.PermissionTreeNode> menuTree = resolveMenuTree(user.permissionCodes(), user.clientType());
        return new CurrentUserVo(
                user.id(),
                user.account(),
                user.userName(),
                null,
                user.roleCodes(),
                user.permissionCodes(),
                menuPermissionCodes,
                menuTree);
    }

    /**
     * @Author lxy
     * @Description //根据用户ID从数据库加载用户并转换为当前用户视图对象
     * @Date 2026/04/18 10:05
     * @Param [userId 用户ID]
     * @return CurrentUserVo 当前用户视图对象
     */
    @Transactional(readOnly = true)
    public CurrentUserVo loadCurrentUser(Long userId) {
        return toCurrentUserVo(loadAuthenticatedUser(userId));
    }

    /**
     * @Author lxy
     * @Description //根据用户ID从数据库加载已认证用户对象（不含客户端类型）
     * @Date 2026/04/18 10:05
     * @Param [userId 用户ID]
     * @return AuthenticatedUser 已认证用户对象
     */
    @Transactional(readOnly = true)
    public AuthenticatedUser loadAuthenticatedUser(Long userId) {
        UserRecord user = loadUserById(userId);
        return toAuthenticatedUser(user, null);
    }

    /**
     * @Author lxy
     * @Description //根据用户ID和客户端类型从数据库加载已认证用户对象，校验用户状态并加载角色权限
     * @Date 2026/04/18 10:05
     * @Param [userId 用户ID, clientType 客户端类型字符串]
     * @return AuthenticatedUser 已认证用户对象
     */
    @Transactional(readOnly = true)
    public AuthenticatedUser loadAuthenticatedUser(Long userId, String clientType) {
        UserRecord user = loadUserById(userId);
        return toAuthenticatedUser(user, clientType);
    }

    /**
     * @Author lxy
     * @Description //根据账号或手机号查询用户记录（支持手机号登录），账号不存在时抛出凭证错误业务异常
     * @Date 2026/04/18 10:05
     * @Param [account 登录账号（账号或手机号）]
     * @return UserRecord 用户数据库记录
     */
    private UserRecord loadUserByAccount(String account) {
        List<UserRecord> users = jdbcTemplate.query(
                "SELECT id, username, password_hash, real_name, phone, status, role_id, password_version FROM sys_user WHERE (username = ? OR phone = ?) AND deleted = 0",
                (rs, rowNum) -> new UserRecord(
                        rs.getLong("id"),
                        rs.getString("username"),
                        rs.getString("password_hash"),
                        rs.getString("real_name"),
                        rs.getString("phone"),
                        rs.getString("status"),
                        rs.getObject("role_id") == null ? null : rs.getLong("role_id"),
                        rs.getInt("password_version")),
                account, account);
        if (users.isEmpty()) {
            throw new BusinessException("AUTH_INVALID_CREDENTIALS", "账号或密码错误");
        }
        return users.get(0);
    }

    /**
     * @Author lxy
     * @Description //根据手机号查询用户记录，手机号未绑定账号时抛出凭证错误业务异常
     * @Date 2026/08/11 14:00
     * @Param [phone 手机号]
     * @return UserRecord 用户数据库记录
     */
    private UserRecord loadUserByPhone(String phone) {
        List<UserRecord> users = jdbcTemplate.query(
                "SELECT id, username, password_hash, real_name, phone, status, role_id, password_version FROM sys_user WHERE phone = ? AND deleted = 0",
                (rs, rowNum) -> new UserRecord(
                        rs.getLong("id"),
                        rs.getString("username"),
                        rs.getString("password_hash"),
                        rs.getString("real_name"),
                        rs.getString("phone"),
                        rs.getString("status"),
                        rs.getObject("role_id") == null ? null : rs.getLong("role_id"),
                        rs.getInt("password_version")),
                phone);
        if (users.isEmpty()) {
            throw new BusinessException("AUTH_INVALID_CREDENTIALS", "该手机号未绑定账号");
        }
        if (users.size() > 1) {
            throw new BusinessException("AUTH_PHONE_DUPLICATED", "该手机号绑定多个账号，请联系管理员处理");
        }
        return users.get(0);
    }

    /**
     * @Author lxy
     * @Description //根据用户ID查询用户记录，用户不存在时抛出令牌无效业务异常
     * @Date 2026/04/18 10:05
     * @Param [userId 用户ID]
     * @return UserRecord 用户数据库记录
     */
    private UserRecord loadUserById(Long userId) {
        List<UserRecord> users = jdbcTemplate.query(
                "SELECT id, username, password_hash, real_name, phone, status, role_id, password_version FROM sys_user WHERE id = ? AND deleted = 0",
                (rs, rowNum) -> new UserRecord(
                        rs.getLong("id"),
                        rs.getString("username"),
                        rs.getString("password_hash"),
                        rs.getString("real_name"),
                        rs.getString("phone"),
                        rs.getString("status"),
                        rs.getObject("role_id") == null ? null : rs.getLong("role_id"),
                        rs.getInt("password_version")),
                userId);
        if (users.isEmpty()) {
            throw new BusinessException("AUTH_TOKEN_INVALID", "认证令牌无效");
        }
        return users.get(0);
    }

    /**
     * @Author lxy
     * @Description //校验用户账号状态，非 ACTIVE 状态时抛出用户已禁用业务异常
     * @Date 2026/04/18 10:05
     * @Param [user 用户数据库记录]
     * @return void
     */
    private void validateUserStatus(UserRecord user) {
        if ("PENDING".equalsIgnoreCase(user.status())) {
            // 注册待审批账号：提示审核中，避免误以为账号被禁用
            throw new BusinessException("AUTH_USER_PENDING", "账号审核中，请等待管理员审批后再登录");
        }
        if (!"ACTIVE".equalsIgnoreCase(user.status())) {
            throw new BusinessException("AUTH_USER_DISABLED", "用户已被禁用");
        }
    }

    /**
     * @Author lxy
     * @Description //校验明文密码与数据库存储的哈希密码是否匹配，不匹配时抛出凭证错误业务异常
     * @Date 2026/04/18 10:05
     * @Param [rawPassword 明文密码, passwordHash 数据库中的密码哈希]
     * @return void
     */
    private void verifyPassword(String rawPassword, String passwordHash) {
        if (!passwordEncoder.matches(rawPassword, passwordHash)) {
            throw new BusinessException("AUTH_INVALID_CREDENTIALS", "账号或密码错误");
        }
    }

    /**
     * @Author lxy
     * @Description //加载用户的角色码列表，优先通过关联表查询，关联表为空时回退到旧版单角色字段
     * @Date 2026/04/18 10:05
     * @Param [userId 用户ID, legacyRoleId 旧版角色ID（可为null）]
     * @return List<String> 角色码列表
     */
    private List<String> loadRoleCodes(Long userId, Long legacyRoleId) {
        Set<String> roleCodes = new LinkedHashSet<>(jdbcTemplate.query(
                "SELECT DISTINCT r.role_code FROM sys_user_role sur JOIN sys_role r ON r.id = sur.role_id WHERE sur.user_id = ? ORDER BY r.role_code",
                (rs, rowNum) -> rs.getString("role_code"),
                userId));
        if (roleCodes.isEmpty() && legacyRoleId != null) {
            roleCodes.addAll(jdbcTemplate.query(
                    "SELECT role_code FROM sys_role WHERE id = ?",
                    (rs, rowNum) -> rs.getString("role_code"),
                    legacyRoleId));
        }
        return List.copyOf(roleCodes);
    }

    /**
     * @Author lxy
     * @Description //加载用户所有角色对应的权限码列表，通过角色权限关联表查询状态为 ACTIVE 的权限
     * @Date 2026/04/18 10:05
     * @Param [userId 用户ID, legacyRoleId 旧版角色ID（可为null）]
     * @return List<String> 权限码列表
     */
    private List<String> loadPermissionCodes(Long userId, Long legacyRoleId) {
        Set<Long> roleIds = new LinkedHashSet<>(jdbcTemplate.query(
                "SELECT DISTINCT role_id FROM sys_user_role WHERE user_id = ?",
                (rs, rowNum) -> rs.getLong("role_id"),
                userId));
        if (roleIds.isEmpty() && legacyRoleId != null) {
            roleIds.add(legacyRoleId);
        }
        if (roleIds.isEmpty()) {
            return List.of();
        }

        String placeholders = String.join(",", java.util.Collections.nCopies(roleIds.size(), "?"));
        String sql = "SELECT DISTINCT p.permission_code "
                + "FROM sys_role_permission srp "
                + "JOIN sys_permission p ON p.id = srp.permission_id "
                + "WHERE srp.role_id IN (" + placeholders + ") AND p.status = 'ACTIVE' ORDER BY p.permission_code";
        return jdbcTemplate.query(sql, (rs, rowNum) -> rs.getString("permission_code"), roleIds.toArray());
    }

    /**
     * @Author lxy
     * @Description //校验用户是否具有登录对应客户端所需的入口权限，不满足时抛出登录禁止业务异常
     * @Date 2026/04/18 10:05
     * @Param [clientType 客户端类型, permissionCodes 用户权限码列表]
     * @return void
     */
    private void enforceEntryPermission(ClientType clientType, List<String> permissionCodes) {
        // WEB 准入：拥有任一非 H5 的菜单权限即可——兼容老式 menu:* 与新版 web:menu:*
        // （超管在角色管理页新建角色只能勾选 web:menu:*，若只认 menu:* 会导致新角色账号无法登录）
        boolean allowed = clientType == ClientType.WEB
                ? permissionCodes.stream().anyMatch(permissionCode -> permissionCode != null
                        && ((permissionCode.startsWith("menu:") && !permissionCode.startsWith(H5_MENU_PREFIX))
                                || permissionCode.startsWith("web:menu:")))
                : permissionCodes.stream().anyMatch(H5_ENTRY_PERMISSIONS::contains);
        if (!allowed) {
            throw new BusinessException(
                    "AUTH_LOGIN_FORBIDDEN",
                    clientType == ClientType.WEB
                            ? "该账号不允许 Web 端登录"
                            : "该账号不允许 H5 端登录");
        }
    }

    /**
     * @Author lxy
     * @Description //从全量权限码列表中过滤出以 "menu:" 前缀开头的菜单权限码
     * @Date 2026/04/18 10:05
     * @Param [permissionCodes 全量权限码列表]
     * @return List<String> 菜单权限码列表
     */
    private List<String> filterMenuPermissionCodes(List<String> permissionCodes) {
        return permissionCodes.stream()
                .filter(permissionCode -> permissionCode != null && permissionCode.startsWith("menu:"))
                .toList();
    }

    /**
     * @Author lxy
     * @Description //根据权限码和客户端类型解析可访问菜单树，H5 当前返回空树以保持兼容
     * @Date 2026/05/21 10:00
     * @Param [permissionCodes 权限码列表, clientType 客户端类型]
     * @return List<SystemPermissionService.PermissionTreeNode> 菜单树
     */
    private List<SystemPermissionService.PermissionTreeNode> resolveMenuTree(List<String> permissionCodes, String clientType) {
        if (clientType == null) {
            return List.of();
        }
        // 支持 WEB 和 H5 两种客户端类型的菜单树
        if (AuthService.ClientType.WEB.name().equalsIgnoreCase(clientType)
                || AuthService.ClientType.H5.name().equalsIgnoreCase(clientType)) {
            return systemPermissionService.listAccessibleMenuTree(permissionCodes, clientType.toUpperCase());
        }
        return List.of();
    }

    /**
     * @Description //解析指定用户的可见菜单树（供 /auth/menu-tree 实时刷新侧边栏，菜单名称/排序/显隐以数据库为准）
     * @Param [user 已认证用户]
     * @return List<SystemPermissionService.PermissionTreeNode> 菜单树
     */
    public List<SystemPermissionService.PermissionTreeNode> resolveMenuTreeFor(AuthenticatedUser user) {
        return resolveMenuTree(user.permissionCodes(), user.clientType());
    }

    /**
     * @Author lxy
     * @Description //将数据库用户记录和客户端类型转换为已认证用户对象，包含角色码和权限码
     * @Date 2026/04/18 10:05
     * @Param [user 用户数据库记录, clientType 客户端类型字符串（可为null）]
     * @return AuthenticatedUser 已认证用户对象
     */
    private AuthenticatedUser toAuthenticatedUser(UserRecord user, String clientType) {
        validateUserStatus(user);
        List<String> roleCodes = loadRoleCodes(user.id(), user.legacyRoleId());
        List<String> permissionCodes = loadPermissionCodes(user.id(), user.legacyRoleId());
        return new AuthenticatedUser(
                user.id(),
                user.username(),
                user.realName(),
                clientType == null ? null : clientType,
                roleCodes,
                permissionCodes,
                user.passwordVersion());
    }

    private record UserRecord(
            Long id,
            String username,
            String passwordHash,
            String realName,
            String phone,
            String status,
            Long legacyRoleId,
            int passwordVersion) {
    }

    /**
     * @Author lxy
     * @Description //客户端类型枚举，区分 Web 管理端和 H5 移动端，用于入口权限校验和令牌隔离
     * @Date 2026/04/18 10:05
     */
    public enum ClientType {
        /** Web管理端 */
        WEB,
        /** H5移动端 */
        H5
    }
}
