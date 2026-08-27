package com.changping.platform.modules.workorder.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * @Author lxy
 * @Description //工单核查请求DTO，包含核查结果和备注
 * @Date 2026/04/18 09:18
 */
public record VerifyWorkOrderRequest(
        @NotBlank(message = "处理结果不能为空")
        String result,
        String remark) {
}
