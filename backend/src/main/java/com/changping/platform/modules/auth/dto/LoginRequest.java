package com.changping.platform.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * @Author lxy
 * @Description //登录请求 DTO，携带用户账号和密码，均不允许为空
 * @Date 2026/04/18 09:35
 */
public record LoginRequest(
        @NotBlank(message = "请输入账号") String account,
        @NotBlank(message = "请输入密码") String password) {
}
