package com.changping.platform.modules.auth.controller;

import com.changping.platform.common.exception.BusinessException;
import com.changping.platform.common.response.ApiResponse;
import com.changping.platform.modules.auth.dto.LoginRequest;
import com.changping.platform.modules.auth.dto.PhoneLoginRequest;
import com.changping.platform.modules.auth.dto.SmsCodeRequest;
import com.changping.platform.modules.auth.security.PermissionCodes;
import com.changping.platform.modules.auth.service.AuthService;
import com.changping.platform.modules.auth.service.CurrentUserService;
import com.changping.platform.modules.auth.service.SmsService;
import com.changping.platform.modules.auth.vo.CurrentUserVo;
import com.changping.platform.modules.auth.vo.LoginResponse;
import com.changping.platform.modules.common.security.RateLimit;
import jakarta.validation.Valid;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
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
    private final StringRedisTemplate stringRedisTemplate;
    private final SmsService smsService;

    /** 验证码 Redis key 前缀 */
    private static final String SMS_CODE_KEY_PREFIX = "sms:code:";
    /** 验证码有效期（分钟） */
    private static final Duration SMS_CODE_TTL = Duration.ofMinutes(5);

    /**
     * @Author tangxinglin
     * @Description //构造函数注入认证服务和当前用户服务
     * @Date 2026/04/18 09:30
     * @Param [authService 认证服务, currentUserService 当前用户服务]
     * @return void
     */
    public AuthController(AuthService authService, CurrentUserService currentUserService,
                          JdbcTemplate jdbcTemplate, PasswordEncoder passwordEncoder,
                          StringRedisTemplate stringRedisTemplate, SmsService smsService) {
        this.authService = authService;
        this.currentUserService = currentUserService;
        this.jdbcTemplate = jdbcTemplate;
        this.passwordEncoder = passwordEncoder;
        this.stringRedisTemplate = stringRedisTemplate;
        this.smsService = smsService;
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
     * @Description //获取当前登录用户的可见菜单树（名称/排序/显隐实时以数据库为准，供侧边栏刷新）
     * @Param []
     * @return ApiResponse 菜单树
     */
    @GetMapping("/menu-tree")
    public ApiResponse<java.util.List<com.changping.platform.modules.system.service.SystemPermissionService.PermissionTreeNode>> menuTree() {
        var current = currentUserService.requireClientType(AuthService.ClientType.WEB);
        var fresh = authService.loadAuthenticatedUser(current.id(), current.clientType());
        return ApiResponse.ok(authService.resolveMenuTreeFor(fresh));
    }

    /**
     * @Author tangxinglin
     * @Description //发送手机号验证码（阿里云短信真实发送，随机 6 位验证码存 Redis 5 分钟）
     * @Date 2026/08/11 18:00
     * @Param [request 验证码请求，携带手机号]
     * @return ApiResponse<Map<String, Object>> 发送结果
     */
    @RateLimit(limit = 10, window = 60, type = RateLimit.RateLimitType.IP, message = "发送过于频繁，请稍后再试")
    @PostMapping("/sms-code")
    public ApiResponse<Map<String, Object>> sendSmsCode(@Valid @RequestBody SmsCodeRequest request) {
        String phone = request.phone();

        // 校验手机号绑定账号的状态：仅 ACTIVE（已激活）账号可发送验证码
        List<Map<String, Object>> users = jdbcTemplate.queryForList(
                "SELECT username, status FROM sys_user WHERE phone = ? AND deleted = 0", phone);
        if (users.isEmpty()) {
            return ApiResponse.fail("PHONE_NOT_BOUND", "该手机号未绑定账号");
        }
        String status = users.get(0).get("status") != null ? String.valueOf(users.get(0).get("status")) : "";
        if ("PENDING".equalsIgnoreCase(status)) {
            return ApiResponse.fail("ACCOUNT_PENDING", "账号待审批，请等待管理员审批后登录");
        }
        if (!"ACTIVE".equalsIgnoreCase(status)) {
            return ApiResponse.fail("ACCOUNT_DISABLED", "账号已被禁用，请联系管理员");
        }

        // 生成随机验证码并发送阿里云短信
        String code = smsService.generateCode();
        boolean sent = smsService.sendCode(phone, code);
        if (!sent) {
            return ApiResponse.fail("SMS_SEND_FAILED", "验证码发送失败，请稍后重试");
        }
        // 发送成功后才写入 Redis（5 分钟有效）
        stringRedisTemplate.opsForValue().set(SMS_CODE_KEY_PREFIX + phone, code, SMS_CODE_TTL);

        Map<String, Object> result = new HashMap<>();
        result.put("phone", phone);
        result.put("expireMinutes", SMS_CODE_TTL.toMinutes());
        result.put("message", "验证码已发送");
        return ApiResponse.ok(result);
    }

    /**
     * @Author tangxinglin
     * @Description //手机号验证码登录：校验验证码后按手机号查用户，按角色自动决定客户端类型（网格员=H5/居民=WEB）并返回令牌
     * @Date 2026/08/11 14:00
     * @Param [request 登录请求，携带手机号和验证码]
     * @return ApiResponse<LoginResponse> 登录成功响应
     */
    @RateLimit(limit = 10, window = 60, type = RateLimit.RateLimitType.IP, message = "登录尝试过于频繁，请稍后再试")
    @PostMapping("/phone-login")
    public ApiResponse<LoginResponse> phoneLogin(@Valid @RequestBody PhoneLoginRequest request) {
        String phone = request.phone();
        String redisKey = SMS_CODE_KEY_PREFIX + phone;
        String cachedCode = stringRedisTemplate.opsForValue().get(redisKey);

        if (cachedCode == null) {
            return ApiResponse.fail("SMS_CODE_EXPIRED", "验证码已过期，请重新获取");
        }
        if (!cachedCode.equals(request.code())) {
            return ApiResponse.fail("SMS_CODE_INVALID", "验证码错误");
        }
        // 验证码校验通过即删除，防止重放
        stringRedisTemplate.delete(redisKey);

        try {
            return ApiResponse.ok(authService.loginByPhone(phone));
        } catch (BusinessException e) {
            return ApiResponse.fail(e.getCode(), e.getMessage());
        }
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
        // 每个账号必须绑定手机号（手机号登录的前提）
        if (phone == null || !phone.matches("^1[3-9]\\d{9}$")) {
            return ApiResponse.fail("VALIDATION_ERROR", "请输入正确的手机号");
        }

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sys_user WHERE username = ? AND deleted = 0", Integer.class, account);
        if (count != null && count > 0) {
            return ApiResponse.fail("DUPLICATE_ACCOUNT", "账号已存在");
        }

        Integer phoneCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sys_user WHERE phone = ? AND deleted = 0", Integer.class, phone);
        if (phoneCount != null && phoneCount > 0) {
            return ApiResponse.fail("DUPLICATE_PHONE", "该手机号已被绑定");
        }

        String hashedPassword = passwordEncoder.encode(password);
        try {
            jdbcTemplate.update(
                    "INSERT INTO sys_user (username, password_hash, real_name, phone, status, password_version, deleted, created_at, updated_at) VALUES (?, ?, ?, ?, 'ACTIVE', 1, 0, NOW(), NOW())",
                    account, hashedPassword, realName, phone);
        } catch (DuplicateKeyException e) {
            // 并发注册或账号/手机号被软删记录占用时，唯一索引兜底，转可读业务错而非 500
            String msg = e.getMessage() != null && e.getMessage().contains("uk_sys_user_phone")
                    ? "该手机号已被绑定" : "账号已存在";
            return ApiResponse.fail("DUPLICATE_ACCOUNT", msg);
        }

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
     * 当前登录用户自助修改密码：需校验旧密码；成功后 password_version 提升，
     * 存量令牌（含当前会话）全部失效，前端需引导重新登录
     */
    @PutMapping("/change-password")
    public ApiResponse<Void> changeOwnPassword(@RequestBody ChangeOwnPasswordRequest body) {
        var user = currentUserService.requireClientType(AuthService.ClientType.WEB);
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT password_hash FROM sys_user WHERE id = ? AND deleted = 0", user.id());
        if (rows.isEmpty()) {
            throw new BusinessException("AUTH_USER_NOT_FOUND", "用户不存在");
        }
        String currentHash = rows.get(0).get("password_hash") != null ? String.valueOf(rows.get(0).get("password_hash")) : "";
        if (body.oldPassword() == null || !passwordEncoder.matches(body.oldPassword(), currentHash)) {
            throw new BusinessException("AUTH_INVALID_CREDENTIALS", "旧密码不正确");
        }
        String newPwd = body.newPassword() != null ? body.newPassword().trim() : "";
        if (newPwd.length() < 6 || newPwd.length() > 64) {
            throw new BusinessException("VALIDATION_ERROR", "新密码长度须在 6 到 64 位之间");
        }
        if (passwordEncoder.matches(newPwd, currentHash)) {
            throw new BusinessException("VALIDATION_ERROR", "新密码不能与旧密码相同");
        }
        jdbcTemplate.update(
                "UPDATE sys_user SET password_hash = ?, password_version = password_version + 1, updated_at = NOW() WHERE id = ?",
                passwordEncoder.encode(newPwd), user.id());
        return ApiResponse.ok(null);
    }

    public record ChangeOwnPasswordRequest(String oldPassword, String newPassword) {
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
