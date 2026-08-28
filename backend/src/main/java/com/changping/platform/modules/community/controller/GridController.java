package com.changping.platform.modules.community.controller;

import com.changping.platform.common.response.ApiResponse;
import com.changping.platform.modules.auth.model.AuthenticatedUser;
import com.changping.platform.modules.auth.security.PermissionCodes;
import com.changping.platform.modules.auth.security.PermissionGuard;
import com.changping.platform.modules.auth.service.AuthService;
import com.changping.platform.modules.auth.service.CurrentUserService;
import com.changping.platform.modules.community.entity.GridEntity;
import com.changping.platform.modules.community.mapper.OrgMemberMapper;
import com.changping.platform.modules.community.service.GridService;
import com.changping.platform.modules.community.vo.GridTreeVo;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/community/grids")
public class GridController {

    private final GridService gridService;
    private final CurrentUserService currentUserService;
    private final PermissionGuard permissionGuard;
    private final OrgMemberMapper orgMemberMapper;

    public GridController(GridService gridService, CurrentUserService currentUserService,
                          PermissionGuard permissionGuard, OrgMemberMapper orgMemberMapper) {
        this.gridService = gridService;
        this.currentUserService = currentUserService;
        this.permissionGuard = permissionGuard;
        this.orgMemberMapper = orgMemberMapper;
    }

    /**
     * H5 移动端 GIS：全量网格树（社区/大网格/小网格含边界坐标），登录即可访问
     */
    @GetMapping("/h5/tree")
    public ApiResponse<List<GridTreeVo>> treeH5() {
        currentUserService.requireClientType(AuthService.ClientType.H5);
        return ApiResponse.ok(gridService.tree());
    }

    /**
     * H5 移动端 GIS：当前用户负责的网格（网格员/网格长），用于“我的网格”定位
     */
    @GetMapping("/h5/my-grid")
    public ApiResponse<List<GridEntity>> myGridH5() {
        AuthenticatedUser user = currentUserService.requireClientType(AuthService.ClientType.H5);
        List<Long> gridIds = orgMemberMapper.findGridIdsByUserId(user.id());
        List<GridEntity> grids = new ArrayList<>();
        for (Long gridId : gridIds) {
            try {
                GridEntity grid = gridService.detail(gridId);
                if (grid != null) grids.add(grid);
            } catch (Exception ignored) {
            }
        }
        return ApiResponse.ok(grids);
    }

    @GetMapping("/tree")
    public ApiResponse<List<GridTreeVo>> tree() {
        requireGridReadPermission();
        return ApiResponse.ok(gridService.tree());
    }

    @GetMapping("/{id}")
    public ApiResponse<GridEntity> detail(@PathVariable Long id) {
        requireGridReadPermission();
        return ApiResponse.ok(gridService.detail(id));
    }

    @GetMapping("/{id}/children")
    public ApiResponse<List<GridEntity>> children(@PathVariable Long id) {
        requireGridReadPermission();
        return ApiResponse.ok(gridService.children(id));
    }

    @PostMapping
    public ApiResponse<Boolean> create(@RequestBody GridEntity entity) {
        requireGridManagePermission();
        return ApiResponse.ok(gridService.create(entity));
    }

    @PutMapping("/{id}")
    public ApiResponse<Boolean> update(@PathVariable Long id, @RequestBody GridEntity entity) {
        requireGridManagePermission();
        entity.setId(id);
        return ApiResponse.ok(gridService.updateGrid(entity));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Boolean> delete(@PathVariable Long id) {
        requireGridManagePermission();
        return ApiResponse.ok(gridService.delete(id));
    }

    private void requireGridReadPermission() {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.requireAny(
                PermissionCodes.MENU_COMMUNITY_GRID,
                PermissionCodes.MENU_COMMUNITY_POPULATION,
                PermissionCodes.MENU_COMMUNITY_BUILDING,
                PermissionCodes.MENU_COMMUNITY_PLACE,
                PermissionCodes.MENU_COMMUNITY_ORG_MEMBER,
                PermissionCodes.MENU_COMMUNITY_DASHBOARD,
                PermissionCodes.MENU_BIG_SCREEN_VIEW);
    }

    private void requireGridManagePermission() {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.MENU_COMMUNITY_GRID);
    }
}
