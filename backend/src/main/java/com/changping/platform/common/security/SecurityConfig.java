package com.changping.platform.common.security;

import com.changping.platform.common.response.ApiResponse;
import com.changping.platform.modules.auth.security.BearerTokenAuthenticationFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * @Author tangxinglin
 * @Description //Spring Security 安全配置类，配置无状态会话、请求鉴权规则、Bearer Token 过滤器及异常处理
 * @Date 2026/04/18 09:25
 */
@Configuration
public class SecurityConfig {

    /**
     * @Author tangxinglin
     * @Description //配置安全过滤器链，禁用 CSRF、设置无状态会话、注册 Bearer Token 过滤器并定义各路径的认证规则
     * @Date 2026/04/18 09:25
     * @Param [http Spring Security HTTP 安全构建器, bearerTokenAuthenticationFilter Bearer Token 认证过滤器, objectMapper JSON 序列化器]
     * @return SecurityFilterChain 构建完成的安全过滤器链
     */
    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            BearerTokenAuthenticationFilter bearerTokenAuthenticationFilter,
            ObjectMapper objectMapper) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(request -> {
                    org.springframework.web.cors.CorsConfiguration cfg = new org.springframework.web.cors.CorsConfiguration();
                    // 明确指定允许的源，不使用通配符
                    cfg.addAllowedOrigin("http://localhost:5173");
                    cfg.addAllowedOrigin("http://localhost:5174");
                    cfg.addAllowedOrigin("http://localhost:5175");
                    cfg.addAllowedOrigin("http://localhost:5176");
                    cfg.addAllowedOrigin("http://127.0.0.1:5173");
                    cfg.addAllowedOrigin("http://127.0.0.1:5174");
                    cfg.addAllowedOrigin("http://127.0.0.1:5175");
                    cfg.addAllowedOrigin("http://127.0.0.1:5176");
                    cfg.addAllowedMethod("*");
                    cfg.addAllowedHeader("*");
                    cfg.setAllowCredentials(true);
                    return cfg;
                }))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, authException) -> writeFailure(response, objectMapper, HttpServletResponse.SC_UNAUTHORIZED, "AUTH_TOKEN_REQUIRED", "请提供认证令牌"))
                        .accessDeniedHandler((request, response, accessDeniedException) -> writeFailure(response, objectMapper, HttpServletResponse.SC_FORBIDDEN, "AUTH_PERMISSION_DENIED", "当前用户没有所需权限")))
                .authorizeHttpRequests(authorize -> authorize
                        // 登录接口公开（注意：Spring Security 自动去掉 context-path /api 前缀）
                        .requestMatchers("/auth/login", "/h5/auth/login", "/auth/logout")
                        .permitAll()
                        // 媒体文件访问公开（用于前端展示）
                        .requestMatchers("/media/files/**")
                        .permitAll()
                        // 群众上报接口公开
                        .requestMatchers("/events/public-report")
                        .permitAll()
                        // 网格员注册申请提交公开（未登录用户可提交）
                        .requestMatchers("/registration/submit")
                        .permitAll()
                        // WebSocket 端点公开（浏览器无法发送认证头）
                        .requestMatchers("/ws/**")
                        .permitAll()
                        // 告警回调需要签名验证（在 Controller 层校验）
                        .requestMatchers("/integrations/alarms/callback")
                        .permitAll()
                        // 测试接口需要认证（配合 @ConditionalOnProperty 控制是否启用）
                        .requestMatchers("/test/**")
                        .authenticated()
                        // 其他所有接口都需要认证
                        .anyRequest()
                        .authenticated())
                .addFilterBefore(bearerTokenAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    /**
     * @Author tangxinglin
     * @Description //创建 BCrypt 密码编码器 Bean，用于用户密码的加密与校验
     * @Date 2026/04/18 09:25
     * @Param []
     * @return PasswordEncoder BCrypt 密码编码器实例
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * @Author tangxinglin
     * @Description //向 HTTP 响应中写入认证/授权失败的 JSON 响应体，若响应已提交则跳过
     * @Date 2026/04/18 09:25
     * @Param [response HTTP 响应对象, objectMapper JSON 序列化器, status HTTP 状态码, code 业务错误码, message 错误描述信息]
     * @return void
     */
    private static void writeFailure(HttpServletResponse response, ObjectMapper objectMapper, int status, String code, String message)
            throws java.io.IOException {
        if (response.isCommitted()) {
            return;
        }
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), ApiResponse.fail(code, message));
    }
}
