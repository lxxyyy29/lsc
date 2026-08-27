package com.changping.platform.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * @Author lxy
 * @Description //验证码发送请求 DTO，携带手机号
 * @Date 2026/08/11 14:00
 */
public record SmsCodeRequest(
        @NotBlank(message = "请输入手机号")
        @Pattern(regexp = "^1[3-9]\\d{9}$", message = "请输入正确的手机号")
        String phone) {
}
