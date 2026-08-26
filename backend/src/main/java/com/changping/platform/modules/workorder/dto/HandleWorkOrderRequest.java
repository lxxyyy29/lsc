package com.changping.platform.modules.workorder.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;

/**
 * @Author lxy
 * @Description //工单处理请求DTO，包含处理结果、备注、附件列表及关联主体信息
 * @Date 2026/04/18 09:17
 */
public record HandleWorkOrderRequest(
        @NotBlank(message = "处理结果不能为空")
        String result,
        String remark,
        @Valid List<HandleAttachmentRequest> attachments,
        String subjectType,
        Long subjectId) {

    public record HandleAttachmentRequest(
            @NotBlank(message = "文件名不能为空") String fileName,
            @NotBlank(message = "文件地址不能为空") String fileUrl,
            String fileType,
            String mimeType) {
    }
}
