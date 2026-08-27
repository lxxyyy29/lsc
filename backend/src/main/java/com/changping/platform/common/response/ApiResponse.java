package com.changping.platform.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * @Author lxy
 * @Description //统一 API 响应封装体，所有接口均通过此 record 返回标准化的成功或失败结果
 * @Date 2026/04/18 09:15
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(boolean success, String code, String message, T data) {

    /**
     * @Author lxy
     * @Description //构造操作成功的响应，携带业务数据
     * @Date 2026/04/18 09:15
     * @Param [data 响应业务数据]
     * @return ApiResponse<T> 成功响应对象
     */
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, "OK", "操作成功", data);
    }

    /**
     * @Author lxy
     * @Description //构造操作失败的响应，携带错误码和错误描述，data 为 null
     * @Date 2026/04/18 09:15
     * @Param [code 业务错误码, message 错误描述信息]
     * @return ApiResponse<T> 失败响应对象
     */
    public static <T> ApiResponse<T> fail(String code, String message) {
        return new ApiResponse<>(false, code, message, null);
    }
}
