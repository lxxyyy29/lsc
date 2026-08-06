package com.changping.platform.modules.auth.controller;

import com.changping.platform.common.response.ApiResponse;
import com.changping.platform.modules.auth.dto.LoginRequest;
import com.changping.platform.modules.auth.security.PermissionCodes;
import com.changping.platform.modules.auth.service.AuthService;
import com.changping.platform.modules.auth.service.CurrentUserService;
import com.changping.platform.modules.auth.vo.CurrentUserVo;
import com.changping.platform.modules.auth.vo.LoginResponse;
import com.changping.platform.modules.common.security.RateLimit;
import jakarta.validation.Valid;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Author tangxinglin
 * @Description //Web 端认证控制器，提供 Web 管理端用户登录、获取当前用户信息及登出接口
 * @Date 2026/04/18 09:30
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    private final CurrentUserService currentUserService;
    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;

    /**
     * @Author tangxinglin
     * @Description //构造函数注入认证服务和当前用户服务
     * @Date 2026/04/18 09:30
     * @Param [authService 认证服务, currentUserService 当前用户服务]
     * @return void
     */
    public AuthController(AuthService authService, CurrentUserService currentUserService,
                          JdbcTemplate jdbcTemplate, PasswordEncoder passwordEncoder) {
        this.authService = authService;
        this.currentUserService = currentUserService;
        this.jdbcTemplate = jdbcTemplate;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * @Author tangxinglin
     * @Description //Web 端用户登录接口，校验账号密码后返回 JWT 令牌及用户权限信息
     * @Date 2026/04/18 09:30
     * @Param [request 登录请求对象，包含账号和密码]
     * @return ApiResponse<LoginResponse> 登录成功响应，包含令牌和用户信息
     */
    @RateLimit(limit = 5, window = 60, type = RateLimit.RateLimitType.IP, message = "登录尝试过于频繁，请稍后再试")
    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(authService.login(request.account(), request.password(), AuthService.ClientType.WEB));
    }

    /**
     * @Author tangxinglin
     * @Description //获取 Web 端当前登录用户信息，包含角色和权限列表
     * @Date 2026/04/18 09:30
     * @Param []
     * @return ApiResponse<CurrentUserVo> 当前用户信息响应
     */
    @GetMapping("/me")
    public ApiResponse<CurrentUserVo> me() {
        return ApiResponse.ok(currentUserService.getCurrentUser(AuthService.ClientType.WEB, PermissionCodes.API_AUTH_WEB_ME));
    }

    /**
     * @Author tangxinglin
     * @Description //群众注册接口（小程序端匿名注册入口），新注册用户统一绑定 PUBLIC 角色（仅限小程序端使用）
     * @Date 2026/08/06 10:30
     * @Param [request 注册请求，包含账号、密码、姓名、手机号]
     * @return ApiResponse<Map<String, Object>> 注册结果，包含用户ID和账号
     */
    @RateLimit(limit = 10, window = 60, type = RateLimit.RateLimitType.IP, message = "注册过于频繁，请稍后再试")
    @PostMapping("/register")
    public ApiResponse<Map<String, Object>> register(@RequestBody Map<String, Object> request) {
        String account = (String) request.get("account");
        String password = (String) request.get("password");
        String realName = (String) request.get("realName");
        String phone = (String) request.get("phone");

        if (account == null || account.isBlank() || password == null || password.isBlank()) {
            return ApiResponse.fail("VALIDATION_ERROR", "账号和密码不能为空");
        }
        if (account.length() < 4) {
            return ApiResponse.fail("VALIDATION_ERROR", "账号至少4位");
        }
        if (password.length() < 6) {
            return ApiResponse.fail("VALIDATION_ERROR", "密码至少6位");
        }

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sys_user WHERE username = ? AND deleted = 0", Integer.class, account);
        if (count != null && count > 0) {
            return ApiResponse.fail("DUPLICATE_ACCOUNT", "账号已存在");
        }

        String hashedPassword = passwordEncoder.encode(password);
        jdbcTemplate.update(
                "INSERT INTO sys_user (username, password_hash, real_name, phone, status, password_version, deleted, created_at, updated_at) VALUES (?, ?, ?, ?, 'ACTIVE', 1, 0, NOW(), NOW())",
                account, hashedPassword, realName, phone);

        Long userId = jdbcTemplate.queryForObject("SELECT id FROM sys_user WHERE username = ?", Long.class, account);

        // 分配普通群众角色（优先 PUBLIC，PUBLIC 不存在时兜底 EVENT_OPERATOR）
        List<Long> roleIds = jdbcTemplate.queryForList(
                "SELECT id FROM sys_role WHERE role_code = 'PUBLIC' ORDER BY id LIMIT 1", Long.class);
        if (roleIds.isEmpty()) {
            roleIds = jdbcTemplate.queryForList(
                    "SELECT id FROM sys_role WHERE role_code = 'EVENT_OPERATOR' ORDER BY id LIMIT 1", Long.class);
        }
        if (!roleIds.isEmpty()) {
            jdbcTemplate.update("INSERT INTO sys_user_role (user_id, role_id) VALUES (?, ?)", userId, roleIds.get(0));
        }

        Map<String, Object> result = new HashMap<>();
        result.put("userId", userId);
        result.put("username", account);
        result.put("message", "注册成功");
        return ApiResponse.ok(result);
    }

    /**
     * @Author tangxinglin
     * @Description //Web 端用户登出接口，客户端清除本地令牌即可，服务端返回成功响应
     * @Date 2026/04/18 09:30
     * @Param []
     * @return ApiResponse<Void> 登出成功响应
     */
    @PostMapping("/logout")
    public ApiResponse<Void> logout() {
        return ApiResponse.ok(null);
    }
}
