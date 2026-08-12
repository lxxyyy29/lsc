package com.changping.platform.modules.auth.controller;

import com.changping.platform.common.response.ApiResponse;
import com.changping.platform.modules.auth.dto.LoginRequest;
import com.changping.platform.modules.auth.dto.PhoneLoginRequest;
import com.changping.platform.modules.auth.dto.SmsCodeRequest;
import com.changping.platform.modules.auth.security.PermissionCodes;
import com.changping.platform.modules.auth.service.AuthService;
import com.changping.platform.modules.auth.service.CurrentUserService;
import com.changping.platform.modules.auth.vo.CurrentUserVo;
import com.changping.platform.modules.auth.vo.LoginResponse;
import com.changping.platform.modules.common.security.RateLimit;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * @Author tangxinglin
 * @Description //H5 移动端认证控制器，提供 H5 端用户登录、获取当前用户信息及登出接口
 * @Date 2026/04/18 09:30
 */
@RestController
@RequestMapping("/h5/auth")
public class H5AuthController {

    private final AuthService authService;
    private final CurrentUserService currentUserService;
    private final AuthController authController;

    /**
     * @Author tangxinglin
     * @Description //构造函数注入认证服务、当前用户服务及基础认证控制器（复用验证码/注册逻辑）
     * @Date 2026/04/18 09:30
     * @Param [authService 认证服务, currentUserService 当前用户服务, authController 基础认证控制器]
     * @return void
     */
    public H5AuthController(AuthService authService, CurrentUserService currentUserService, AuthController authController) {
        this.authService = authService;
        this.currentUserService = currentUserService;
        this.authController = authController;
    }

    /**
     * @Author tangxinglin
     * @Description //H5 端用户登录接口，校验账号密码后返回 JWT 令牌及用户权限信息
     * @Date 2026/04/18 09:30
     * @Param [request 登录请求对象，包含账号和密码]
     * @return ApiResponse<LoginResponse> 登录成功响应，包含令牌和用户信息
     */
    @RateLimit(limit = 5, window = 60, type = RateLimit.RateLimitType.IP, message = "登录尝试过于频繁，请稍后再试")
    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(authService.login(request.account(), request.password(), AuthService.ClientType.H5));
    }

    /**
     * @Author tangxinglin
     * @Description //获取 H5 端当前登录用户信息，包含角色和权限列表
     * @Date 2026/04/18 09:30
     * @Param []
     * @return ApiResponse<CurrentUserVo> 当前用户信息响应
     */
    @GetMapping("/me")
    public ApiResponse<CurrentUserVo> me() {
        return ApiResponse.ok(currentUserService.getCurrentUser(AuthService.ClientType.H5, PermissionCodes.API_AUTH_H5_ME));
    }

    /**
     * @Author tangxinglin
     * @Description //H5 端用户登出接口，客户端清除本地令牌即可，服务端返回成功响应
     * @Date 2026/04/18 09:30
     * @Param []
     * @return ApiResponse<Void> 登出成功响应
     */
    @PostMapping("/logout")
    public ApiResponse<Void> logout() {
        return ApiResponse.ok(null);
    }

    /**
     * @Author tangxinglin
     * @Description //发送手机号验证码（小程序端入口，复用基础认证逻辑：阿里云短信真实发送，随机 6 位验证码存 Redis 5 分钟）
     * @Date 2026/08/11 18:00
     * @Param [request 验证码请求，携带手机号]
     * @return ApiResponse<Map<String, Object>> 发送结果
     */
    @PostMapping("/sms-code")
    public ApiResponse<Map<String, Object>> sendSmsCode(@Valid @RequestBody SmsCodeRequest request) {
        return authController.sendSmsCode(request);
    }

    /**
     * @Author tangxinglin
     * @Description //手机号验证码登录（小程序端入口，复用基础认证逻辑：校验验证码后按角色自动决定客户端类型）
     * @Date 2026/08/11 14:00
     * @Param [request 登录请求，携带手机号和验证码]
     * @return ApiResponse<LoginResponse> 登录成功响应
     */
    @PostMapping("/phone-login")
    public ApiResponse<LoginResponse> phoneLogin(@Valid @RequestBody PhoneLoginRequest request) {
        return authController.phoneLogin(request);
    }

    /**
     * @Author tangxinglin
     * @Description //群众注册接口（小程序端匿名注册入口，复用基础认证逻辑：统一绑定 PUBLIC 角色）
     * @Date 2026/08/06 10:30
     * @Param [request 注册请求，包含账号、密码、姓名、手机号]
     * @return ApiResponse<Map<String, Object>> 注册结果
     */
    @PostMapping("/register")
    public ApiResponse<Map<String, Object>> register(@RequestBody Map<String, Object> request) {
        return authController.register(request);
    }
}
