package com.changping.platform.common.exception;

/**
 * @Author lxy
 * @Description //业务异常类，用于封装业务逻辑中发生的可预期异常，携带错误码和错误信息
 * @Date 2026/04/18 09:05
 */
public class BusinessException extends RuntimeException {

    /** 业务错误码 */
    private final String code;

    /**
     * @Author lxy
     * @Description //构造业务异常，指定错误码和错误描述信息
     * @Date 2026/04/18 09:05
     * @Param [code 业务错误码, message 错误描述信息]
     * @return void
     */
    public BusinessException(String code, String message) {
        super(message);
        this.code = code;
    }

    /**
     * @Author lxy
     * @Description //获取业务错误码
     * @Date 2026/04/18 09:05
     * @Param []
     * @return String 业务错误码
     */
    public String getCode() {
        return code;
    }
}
