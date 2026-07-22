package com.changping.platform.modules.auth.vo;

import com.changping.platform.modules.system.service.SystemPermissionService;
import java.util.List;

/**
 * @Author tangxinglin
 * @Description //当前登录用户视图对象，包含用户基本信息、角色列表、全量权限码及菜单权限码，用于前端路由鉴权
 * @Date 2026/04/18 09:35
 */
public record CurrentUserVo(
        Long id,
        String username,
        String realName,
        String phone,
        List<String> roleCodes,
        List<String> permissionCodes,
        List<String> menuPermissionCodes,
        List<SystemPermissionService.PermissionTreeNode> menuTree) {
}
