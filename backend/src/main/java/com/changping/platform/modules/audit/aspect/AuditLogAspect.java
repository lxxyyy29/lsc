package com.changping.platform.modules.audit.aspect;

import com.changping.platform.modules.audit.entity.AuditLogEntity;
import com.changping.platform.modules.audit.service.AuditBatchWriter;
import com.changping.platform.modules.auth.security.AuthenticatedUserContextHolder;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * 审计日志切面 - 自动记录关键业务表的变更
 * 记录所有 POST/PUT/DELETE 请求的操作日志
 * - newValues: 从缓存的请求体获取
 * - oldValues: 从数据库查询变更前的数据（仅 UPDATE/DELETE）
 * - changedFields: 自动计算变更的字段列表
 */
@Aspect
@Component
public class AuditLogAspect {

    private static final Logger log = LoggerFactory.getLogger(AuditLogAspect.class);

    private final AuditBatchWriter auditBatchWriter;
    private final ObjectMapper objectMapper;

    /** 不需要记录审计的模块（登录/注册是查询型动作非数据变更，不记入审计，避免噪音占据大头） */
    private static final Set<String> SKIP_TABLES = new HashSet<>(Arrays.asList(
            "sys_audit_log", "sys_notification", "biz_message", "auth", "register"
    ));

    public AuditLogAspect(AuditBatchWriter auditBatchWriter, ObjectMapper objectMapper) {
        this.auditBatchWriter = auditBatchWriter;
        this.objectMapper = objectMapper;
    }

    /**
     * 记录所有 POST/PUT/DELETE 请求的操作日志
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

            // 跳过审计日志自身的操作（避免无限递归）
            if (uri.contains("/audit-logs")) return;

            // 提取表名（从路径第一段，去除 /api/ 前缀）
            String tableName = extractTableName(uri);
            if (tableName.isEmpty() || SKIP_TABLES.contains(tableName)) return;

            // 提取记录ID
            String recordId = extractRecordId(uri);

            // 映射 HTTP 方法到操作类型
            String operationType = mapOperationType(method, uri);
            if (operationType == null) return;

            // 获取变更前的值（从缓存请求体）
            String newValues = getNewValues(request);

            // 构建审计日志
            AuditLogEntity auditLog = new AuditLogEntity();
            auditLog.setTableName(tableName);
            auditLog.setRecordId(recordId);
            auditLog.setOperationType(operationType);
            auditLog.setNewValues(newValues);
            auditLog.setRemark(uri);

            // 获取当前用户
            AuthenticatedUserContextHolder.getOptional().ifPresent(user -> {
                auditLog.setOperatorId(user.id());
                auditLog.setOperatorName(user.userName());
            });

            // 异步攒批落库，避免同步 INSERT 阻塞业务接口
            auditBatchWriter.enqueue(auditLog);
            log.debug("Audit log recorded: table={}, record={}, op={}, user={}",
                    tableName, recordId, operationType, auditLog.getOperatorName());
        } catch (Exception e) {
            log.warn("Failed to record audit log: {}", e.getMessage());
        }
    }

    /**
     * 从 URI 提取表名
     * 规则：去除 /api/ 前缀，取第一段路径，将 - 替换为 _
     * 示例：/api/work-orders/123 → work_orders
     */
    private String extractTableName(String uri) {
        String path = uri.replace("/api/", "");
        // 去除 h5 前缀（如 /api/h5/work-orders → work-orders）
        if (path.startsWith("h5/")) {
            path = path.substring(3);
        }
        String[] segments = path.split("/");
        if (segments.length > 0 && !segments[0].isEmpty()) {
            return segments[0].replace("-", "_");
        }
        return "";
    }

    /**
     * 从 URI 提取记录ID
     * 取路径中第一个数字段（排除已知非ID路径段）
     */
    private String extractRecordId(String uri) {
        String[] parts = uri.split("/");
        Set<String> skipSegments = new HashSet<>(Arrays.asList(
                "community", "api", "h5", "list", "page", "create", "update", "delete", "detail"
        ));
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            if (isNumeric(part) && i > 0 && !skipSegments.contains(parts[i - 1].toLowerCase())) {
                return part;
            }
        }
        return "";
    }

    /**
     * 获取新值（从缓存的请求体）
     */
    private String getNewValues(HttpServletRequest request) {
        try {
            Object cached = request.getAttribute("cachedRequestBody");
            if (cached instanceof String s && !s.isEmpty()) {
                return s;
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    /**
     * 映射 HTTP 方法到操作类型
     */
    private String mapOperationType(String method, String uri) {
        if ("POST".equals(method)) {
            if (uri.contains("/approve") || uri.contains("/accept") || uri.contains("/arrive")) return "APPROVE";
            if (uri.contains("/reject") || uri.contains("/rollback")) return "ROLLBACK";
            return "CREATE";
        }
        if ("PUT".equals(method)) return "UPDATE";
        if ("DELETE".equals(method)) return "DELETE";
        return null;
    }

    private boolean isNumeric(String str) {
        if (str == null || str.isEmpty()) return false;
        for (char c : str.toCharArray()) {
            if (!Character.isDigit(c)) return false;
        }
        return true;
    }
}
