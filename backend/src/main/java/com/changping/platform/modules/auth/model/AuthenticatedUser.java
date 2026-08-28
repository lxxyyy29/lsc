package com.changping.platform.modules.auth.model;

import java.util.List;

/**
 * @Author lxy
 * @Description //已认证用户模型，承载通过 JWT 令牌解析或从数据库加载的完整用户身份信息，在请求生命周期内通过 ThreadLocal 传递
 * @Date 2026/04/18 09:35
 */
public record AuthenticatedUser(
        Long id,
        String account,
        String userName,
        String clientType,
        List<String> roleCodes,
        List<String> permissionCodes,
        int passwordVersion) {
}
