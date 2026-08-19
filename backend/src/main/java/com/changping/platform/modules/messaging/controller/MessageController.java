package com.changping.platform.modules.messaging.controller;

import com.changping.platform.common.response.ApiResponse;
import com.changping.platform.modules.auth.service.AuthService;
import com.changping.platform.modules.auth.service.CurrentUserService;
import com.changping.platform.modules.messaging.entity.MessageEntity;
import com.changping.platform.modules.messaging.service.MessageService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 消息互通 REST 控制器：会话列表、历史消息、标记已读、网格员列表
 */
@RestController
@RequestMapping("/messaging")
public class MessageController {

    private final MessageService service;
    private final CurrentUserService currentUserService;

    public MessageController(MessageService service, CurrentUserService currentUserService) {
        this.service = service;
        this.currentUserService = currentUserService;
    }

    /** 当前用户的会话列表 */
    @GetMapping("/conversations")
    public ApiResponse<List<Map<String, Object>>> conversations() {
        return ApiResponse.ok(service.conversations(currentUserService.requireUserIdAllowH5()));
    }

    /** 与某用户的历史消息 */
    @GetMapping("/history/{partnerId}")
    public ApiResponse<List<MessageEntity>> history(
            @PathVariable Long partnerId,
            @RequestParam(defaultValue = "50") int limit) {
        return ApiResponse.ok(service.history(currentUserService.requireUserIdAllowH5(), partnerId, limit));
    }

    /** 标记与某用户的对话为已读 */
    @PostMapping("/read/{partnerId}")
    public ApiResponse<Integer> markRead(@PathVariable Long partnerId) {
        return ApiResponse.ok(service.markRead(currentUserService.requireUserIdAllowH5(), partnerId));
    }

    /** 所有网格员列表（发起新会话用，仅 Web 管理员可访问） */
    @GetMapping("/workers")
    public ApiResponse<List<Map<String, Object>>> workers() {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        return ApiResponse.ok(service.gridWorkers());
    }
}
