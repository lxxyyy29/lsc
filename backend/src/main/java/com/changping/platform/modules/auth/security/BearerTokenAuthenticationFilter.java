package com.changping.platform.modules.auth.security;

import com.changping.platform.common.exception.BusinessException;
import com.changping.platform.common.response.ApiResponse;
import com.changping.platform.modules.auth.model.AuthenticatedUser;
import com.changping.platform.modules.auth.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * @Author tangxinglin
 * @Description //Bearer Token 认证过滤器，每次请求时从 Authorization 头解析 JWT 令牌，验证有效性并将认证用户写入安全上下文和线程本地变量
 * @Date 2026/04/18 09:45
 */
@Component
public class BearerTokenAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenService jwtTokenService;
    private final AuthService authService;
    private final ObjectMapper objectMapper;

    /**
     * @Author tangxinglin
     * @Description //构造函数注入 JWT 令牌服务、认证服务和 JSON 序列化器
     * @Date 2026/04/18 09:45
     * @Param [jwtTokenService JWT 令牌服务, authService 认证服务, objectMapper JSON 序列化器]
     * @return void
     */
    public BearerTokenAuthenticationFilter(
            JwtTokenService jwtTokenService,
            AuthService authService,
            ObjectMapper objectMapper) {
        this.jwtTokenService = jwtTokenService;
        this.authService = authService;
        this.objectMapper = objectMapper;
    }

    /**
     * @Author tangxinglin
     * @Description //执行每次请求的 Token 认证逻辑：解析 Bearer Token、校验密码版本、写入安全上下文，认证失败则直接返回错误响应
     * @Date 2026/04/18 09:45
     * @Param [request HTTP 请求对象, response HTTP 响应对象, filterChain 过滤器链]
     * @return void
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
            if (authorizationHeader != null && !authorizationHeader.isBlank()) {
                if (!authorizationHeader.startsWith("Bearer ")) {
                    writeFailure(response, HttpServletResponse.SC_UNAUTHORIZED, "AUTH_TOKEN_INVALID", "认证令牌无效");
                    return;
                }

                String token = authorizationHeader.substring(7).trim();
                if (token.isEmpty()) {
                    writeFailure(response, HttpServletResponse.SC_UNAUTHORIZED, "AUTH_TOKEN_INVALID", "认证令牌无效");
                    return;
                }

                AuthenticatedUser tokenUser = jwtTokenService.parseAuthenticatedUser(token);
                var currentUser = authService.loadAuthenticatedUser(tokenUser.id(), tokenUser.clientType());
                if (tokenUser.passwordVersion() != currentUser.passwordVersion()) {
                    throw new BusinessException("AUTH_PASSWORD_CHANGED", "密码已被修改，请重新登录");
                }
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        currentUser,
                        null,
                        toAuthorities(currentUser));
                SecurityContextHolder.getContext().setAuthentication(authentication);
                AuthenticatedUserContextHolder.set(currentUser);
            }

            filterChain.doFilter(request, response);
        } catch (BusinessException exception) {
            int status = resolveStatus(exception.getCode());
            writeFailure(response, status, exception.getCode(), exception.getMessage());
        } finally {
            AuthenticatedUserContextHolder.clear();
            SecurityContextHolder.clearContext();
        }
    }

    /**
     * @Author tangxinglin
     * @Description //将已认证用户的角色码和权限码转换为 Spring Security 的 GrantedAuthority 集合
     * @Date 2026/04/18 09:45
     * @Param [authenticatedUser 已认证用户对象]
     * @return Collection<? extends GrantedAuthority> Spring Security 权限集合
     */
    private Collection<? extends GrantedAuthority> toAuthorities(AuthenticatedUser authenticatedUser) {
        List<GrantedAuthority> roleAuthorities = authenticatedUser.roleCodes().stream()
                .map(roleCode -> new SimpleGrantedAuthority("ROLE_" + roleCode))
                .map(GrantedAuthority.class::cast)
                .toList();
        List<GrantedAuthority> permissionAuthorities = authenticatedUser.permissionCodes().stream()
                .map(SimpleGrantedAuthority::new)
                .map(GrantedAuthority.class::cast)
                .toList();
        return java.util.stream.Stream.concat(roleAuthorities.stream(), permissionAuthorities.stream()).toList();
    }

    /**
     * @Author tangxinglin
     * @Description //根据业务错误码解析对应的 HTTP 状态码，权限类返回 403，其余返回 401
     * @Date 2026/04/18 09:45
     * @Param [code 业务错误码]
     * @return int HTTP 状态码
     */
    private int resolveStatus(String code) {
        return switch (code) {
            case "AUTH_PERMISSION_DENIED", "AUTH_CLIENT_TYPE_FORBIDDEN" -> HttpServletResponse.SC_FORBIDDEN;
            default -> HttpServletResponse.SC_UNAUTHORIZED;
        };
    }

    /**
     * @Author tangxinglin
     * @Description //向 HTTP 响应写入认证失败的 JSON 响应体，若响应已提交则跳过
     * @Date 2026/04/18 09:45
     * @Param [response HTTP 响应对象, status HTTP 状态码, code 业务错误码, message 错误描述信息]
     * @return void
     */
    private void writeFailure(HttpServletResponse response, int status, String code, String message) throws IOException {
        if (response.isCommitted()) {
            return;
        }
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), ApiResponse.fail(code, message));
    }
}
