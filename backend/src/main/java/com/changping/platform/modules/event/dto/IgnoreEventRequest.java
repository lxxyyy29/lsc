package com.changping.platform.modules.event.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * @Author lxy
 * @Description //忽略事件请求DTO，包含标记事件为误报时必填的忽略原因
 * @Date 2026/04/18 10:00
 */
public record IgnoreEventRequest(
        @NotBlank(message = "忽略原因不能为空")
        String reason) {
}
