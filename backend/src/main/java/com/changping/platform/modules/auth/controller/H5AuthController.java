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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

    /**
     * @Author tangxinglin
     * @Description //构造函数注入认证服务和当前用户服务
     * @Date 2026/04/18 09:30
     * @Param [authService 认证服务, currentUserService 当前用户服务]
     * @return void
     */
    public H5AuthController(AuthService authService, CurrentUserService currentUserService) {
        this.authService = authService;
        this.currentUserService = currentUserService;
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
}
