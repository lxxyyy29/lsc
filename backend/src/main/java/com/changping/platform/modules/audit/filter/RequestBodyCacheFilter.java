package com.changping.platform.modules.audit.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 请求体缓存过滤器
 * 使请求体可被多次读取，供审计日志切面使用
 * 注意：仅缓存有 body 的请求（POST/PUT），且排除文件上传（multipart）
 */
@Component
@org.springframework.core.annotation.Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class RequestBodyCacheFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String contentType = request.getContentType();
        boolean shouldCache = request.getMethod().equals("POST") || request.getMethod().equals("PUT");
        // 排除文件上传和空 body
        if (shouldCache && contentType != null && !contentType.contains("multipart")) {
            CachedBodyHttpServletRequest wrappedRequest = new CachedBodyHttpServletRequest(request);
            // 将请求体存入 attribute，供 AuditLogAspect 使用
            wrappedRequest.setAttribute("cachedRequestBody", wrappedRequest.getBody());
            filterChain.doFilter(wrappedRequest, response);
        } else {
            request.setAttribute("cachedRequestBody", "");
            filterChain.doFilter(request, response);
        }
    }
}
