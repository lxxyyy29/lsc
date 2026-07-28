package com.changping.platform.modules.audit.aspect;

import com.changping.platform.modules.audit.entity.AuditLogEntity;
import com.changping.platform.modules.audit.mapper.AuditLogMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * 审计日志切面 - 自动记录关键业务表的变更
 * 注意：这是一个简化版本，实际项目中可以根据需要精确配置切点
 */
@Aspect
@Component
public class AuditLogAspect {

    private final AuditLogMapper auditLogMapper;
    private final ObjectMapper objectMapper;

    public AuditLogAspect(AuditLogMapper auditLogMapper, ObjectMapper objectMapper) {
        this.auditLogMapper = auditLogMapper;
        this.objectMapper = objectMapper;
    }

    /**
     * 记录所有 POST/PUT/DELETE 请求的操作日志（简化版）
     */
    @AfterReturning(pointcut = "execution(* com.changping.platform.modules..controller..*(..)) && " +
            "(@annotation(org.springframework.web.bind.annotation.PostMapping) || " +
            "@annotation(org.springframework.web.bind.annotation.PutMapping) || " +
            "@annotation(org.springframework.web.bind.annotation.DeleteMapping))", returning = "result")
    public void logOperation(JoinPoint joinPoint, Object result) {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) return;
            HttpServletRequest request = attrs.getRequest();

            String method = request.getMethod();
            String uri = request.getRequestURI();

            // 提取路径中的ID
            String[] parts = uri.split("/");
            String recordId = "";
            for (int i = 0; i < parts.length; i++) {
                if (i > 0 && isNumeric(parts[i]) && !parts[i - 1].equals("community")) {
                    recordId = parts[i];
                    break;
                }
            }

            // 提取表名（从路径第一段）
            String tableName = "";
            String[] pathSegments = uri.replace("/api/", "").split("/");
            if (pathSegments.length > 0) {
                tableName = pathSegments[0].replace("-", "_");
            }

            if (tableName.isEmpty()) return;

            AuditLogEntity log = new AuditLogEntity();
            log.setTableName(tableName);
            log.setRecordId(recordId);
            log.setOperationType(method);
            log.setRemark(uri);

            // 尝试获取当前用户
            try {
                Object authHeader = request.getAttribute("authenticatedUser");
                if (authHeader instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> userMap = (Map<String, Object>) authHeader;
                    Object userId = userMap.get("userId");
                    if (userId != null) {
                        log.setOperatorId(Long.parseLong(userId.toString()));
                    }
                    Object userName = userMap.get("userName");
                    if (userName != null) {
                        log.setOperatorName(userName.toString());
                    }
                }
            } catch (Exception ignored) {}

            auditLogMapper.insert(log);
        } catch (Exception e) {
            // 审计日志记录失败不应影响主业务
        }
    }

    private boolean isNumeric(String str) {
        if (str == null || str.isEmpty()) return false;
        for (char c : str.toCharArray()) {
            if (!Character.isDigit(c)) return false;
        }
        return true;
    }
}
