package com.changping.platform.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * @Author lxy
 * @Description //手机号验证码登录请求 DTO，携带手机号和验证码
 * @Date 2026/08/11 14:00
 */
public record PhoneLoginRequest(
        @NotBlank(message = "请输入手机号")
        @Pattern(regexp = "^1[3-9]\\d{9}$", message = "请输入正确的手机号")
        String phone,
        @NotBlank(message = "请输入验证码")
        String code) {
}
