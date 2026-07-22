package com.changping.platform.common.exception;

import com.changping.platform.common.response.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * @Author tangxinglin
 * @Description //全局异常处理器，统一捕获并处理业务异常、参数校验异常及未知异常，返回标准化响应体
 * @Date 2026/04/18 09:10
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * @Author tangxinglin
     * @Description //处理业务异常，根据错误码映射对应的 HTTP 状态码并返回失败响应
     * @Date 2026/04/18 09:10
     * @Param [exception 业务异常对象]
     * @return ResponseEntity<ApiResponse<Void>> 包含错误码和错误信息的响应实体
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException exception) {
        HttpStatus status = resolveBusinessStatus(exception.getCode());
        return ResponseEntity.status(status)
                .body(ApiResponse.fail(exception.getCode(), exception.getMessage()));
    }

    /**
     * @Author tangxinglin
     * @Description //处理请求体参数校验失败异常，收集所有字段错误信息并返回 400 响应
     * @Date 2026/04/18 09:10
     * @Param [exception 方法参数校验失败异常对象]
     * @return ResponseEntity<ApiResponse<Void>> 包含校验错误信息的响应实体
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentNotValid(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        return ResponseEntity.badRequest()
                .body(ApiResponse.fail("VALIDATION_ERROR", message));
    }

    /**
     * @Author tangxinglin
     * @Description //处理约束违反异常（如路径变量、请求参数校验失败），返回 400 响应
     * @Date 2026/04/18 09:10
     * @Param [exception 约束违反异常对象]
     * @return ResponseEntity<ApiResponse<Void>> 包含约束错误信息的响应实体
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(ConstraintViolationException exception) {
        return ResponseEntity.badRequest()
                .body(ApiResponse.fail("VALIDATION_ERROR", exception.getMessage()));
    }

    /**
     * @Author tangxinglin
     * @Description //兜底处理所有未预期异常，记录日志并返回 500 内部服务器错误响应
     * @Date 2026/04/18 09:10
     * @Param [exception 未预期异常对象]
     * @return ResponseEntity<ApiResponse<Void>> 包含服务器内部错误信息的响应实体
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpectedException(Exception exception) {
        log.error("Unhandled exception caught by global handler", exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.fail("INTERNAL_SERVER_ERROR", "服务器内部错误"));
    }

    /**
     * @Author tangxinglin
     * @Description //根据业务错误码解析对应的 HTTP 状态码，认证类错误返回 401/403，其余返回 400
     * @Date 2026/04/18 09:10
     * @Param [code 业务错误码]
     * @return HttpStatus 对应的 HTTP 状态码
     */
    private HttpStatus resolveBusinessStatus(String code) {
        return switch (code) {
            case "AUTH_TOKEN_REQUIRED", "AUTH_TOKEN_INVALID" -> HttpStatus.UNAUTHORIZED;
            case "AUTH_PERMISSION_DENIED", "AUTH_CLIENT_TYPE_FORBIDDEN" -> HttpStatus.FORBIDDEN;
            default -> HttpStatus.BAD_REQUEST;
        };
    }
}
