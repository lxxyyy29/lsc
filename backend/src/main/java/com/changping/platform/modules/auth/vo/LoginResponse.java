package com.changping.platform.modules.auth.vo;

import com.changping.platform.modules.system.service.SystemPermissionService;
import java.util.List;

/**
 * @Author lxy
 * @Description //登录响应视图对象，包含 JWT 访问令牌、用户基本信息及权限列表，登录成功后返回给前端
 * @Date 2026/04/18 09:35
 */
public record LoginResponse(
        String token,
        Long userId,
        String userName,
        String account,
        List<String> roleCodes,
        List<String> permissionCodes,
        List<SystemPermissionService.PermissionTreeNode> menuTree) {
}
