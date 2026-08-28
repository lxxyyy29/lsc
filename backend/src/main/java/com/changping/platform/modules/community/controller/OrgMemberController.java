package com.changping.platform.modules.community.controller;

import com.changping.platform.common.response.ApiResponse;
import com.changping.platform.modules.auth.security.PermissionCodes;
import com.changping.platform.modules.auth.security.PermissionGuard;
import com.changping.platform.modules.auth.service.AuthService;
import com.changping.platform.modules.auth.service.CurrentUserService;
import com.changping.platform.modules.community.entity.OrgMemberEntity;
import com.changping.platform.modules.community.service.OrgMemberService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/community/org-members")
public class OrgMemberController {

    private final OrgMemberService service;
    private final JdbcTemplate jdbcTemplate;
    private final CurrentUserService currentUserService;
    private final PermissionGuard permissionGuard;

    public OrgMemberController(
            OrgMemberService service,
            JdbcTemplate jdbcTemplate,
            CurrentUserService currentUserService,
            PermissionGuard permissionGuard) {
        this.service = service;
        this.jdbcTemplate = jdbcTemplate;
        this.currentUserService = currentUserService;
        this.permissionGuard = permissionGuard;
    }

    @GetMapping
    public ApiResponse<List<OrgMemberEntity>> list(@RequestParam(required = false) Long gridId) {
        requireOrgMemberPermission();
        return ApiResponse.ok(service.list(gridId));
    }
    @GetMapping("/{id}")
    public ApiResponse<OrgMemberEntity> detail(@PathVariable Long id) {
        requireOrgMemberPermission();
        return ApiResponse.ok(service.detail(id));
    }

    /**
     * 组长候选人（职务含组长/网格长或社区领导）
     */
    @GetMapping("/leader-candidates")
    public ApiResponse<List<OrgMemberEntity>> leaderCandidates() {
        requireOrgMemberPermission();
        return ApiResponse.ok(service.leaderCandidates());
    }

    /**
     * 网格员划分：将选中成员批量划入某组长名下（leaderId 为 null 表示取消划分）
     */
    @PostMapping("/assign")
    public ApiResponse<Integer> assign(@RequestBody Map<String, Object> body) {
        requireOrgMemberPermission();
        Object leaderIdRaw = body.get("leaderId");
        Long leaderId = leaderIdRaw == null ? null : Long.valueOf(String.valueOf(leaderIdRaw));
        @SuppressWarnings("unchecked")
        List<Object> rawIds = (List<Object>) body.get("memberIds");
        if (rawIds == null || rawIds.isEmpty()) {
            return ApiResponse.ok(0);
        }
        List<Long> memberIds = rawIds.stream().map(o -> Long.valueOf(String.valueOf(o))).toList();
        return ApiResponse.ok(service.assignLeader(memberIds, leaderId));
    }
    /**
     * 一体化创建组长：创建组织人员(LEADER)并绑定网格，随后自动将该网格下全部在岗网格员
     * 划入其名下（绑定网格即自动划分下属）。
     */
    @PostMapping("/create-leader")
    public ApiResponse<Map<String, Object>> createLeader(@RequestBody OrgMemberEntity entity) {
        requireOrgMemberPermission();
        if (entity.getName() == null || entity.getName().isBlank()) {
            return ApiResponse.fail("INVALID_PARAM", "组长姓名不能为空");
        }
        if (entity.getGridId() == null) {
            return ApiResponse.fail("INVALID_PARAM", "请选择组长所属小网格");
        }
        // 幂等校验①：该网格下已存在组长（避免一网格多组长导致划分冲突）
        Long existingLeaders = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM cmn_org_member WHERE grid_id = ? AND status = 'ACTIVE' " +
                        "AND (position LIKE '%组长%' OR position LIKE '%网格长%' OR member_type = 'LEADER')",
                Long.class, entity.getGridId());
        if (existingLeaders != null && existingLeaders > 0) {
            return ApiResponse.fail("INVALID_PARAM", "该网格已存在组长，请使用「人员划分」调整或更换网格");
        }
        // 幂等校验②：同名组长已存在（姓名去空格后的账号/成员均不允许重复创建）
        String username = entity.getName().replaceAll("\\s+", "");
        Long sameLeader = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM cmn_org_member WHERE status = 'ACTIVE' AND name = ? " +
                        "AND (position LIKE '%组长%' OR position LIKE '%网格长%' OR member_type = 'LEADER')",
                Long.class, entity.getName().trim());
        if (sameLeader != null && sameLeader > 0) {
            return ApiResponse.fail("INVALID_PARAM", "已存在同名组长，请勿重复创建");
        }
        Long memberId = service.createLeader(entity);
        // 自动创建系统账号并分配「网格组长」角色，供登录与权限使用
        syncLeaderToSysUser(memberId, entity, username);
        int assigned = service.assignGridWorkersToLeader(memberId, entity.getGridId());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("memberId", memberId);
        result.put("assignedCount", assigned);
        result.put("leaderName", entity.getName());
        result.put("username", username);
        result.put("password", "123456");
        return ApiResponse.ok(result);
    }

    @PostMapping
    public ApiResponse<Boolean> create(@RequestBody OrgMemberEntity entity) {
        requireOrgMemberPermission();
        // 1. 创建组织人员
        service.create(entity);
        // 2. 如果是网格员，同步创建系统用户（用于考核研判和派单）
        if ("GRID_WORKER".equals(entity.getMemberType())) {
            syncToSysUser(entity);
        }
        return ApiResponse.ok(true);
    }
    @PutMapping("/{id}")
    public ApiResponse<Boolean> update(@PathVariable Long id, @RequestBody OrgMemberEntity entity) {
        requireOrgMemberPermission();
        entity.setId(id);
        boolean ok = service.update(entity);
        // 职位与系统角色绑定：保存后按职位自动同步账号角色（组长/网格长→网格组长，网格员→网格员，社区工作人员→管理员）
        if (ok) {
            syncOrgMemberRoleToUser(entity);
        }
        return ApiResponse.ok(ok);
    }
    @DeleteMapping("/{id}")
    public ApiResponse<Boolean> delete(@PathVariable Long id) {
        requireOrgMemberPermission();
        return ApiResponse.ok(service.delete(id));
    }

    /**
     * 将现有网格员（sys_user）同步到组织人员表
     */
    @PostMapping("/sync-from-users")
    public ApiResponse<Map<String, Object>> syncFromUsers() {
        requireOrgMemberPermission();
        Map<String, Object> result = new java.util.HashMap<>();
        int synced = 0;
        int skipped = 0;

        try {
            // 获取所有 GRID_WORKER 角色的用户
            List<Map<String, Object>> gridUsers = jdbcTemplate.queryForList(
                "SELECT u.id, u.real_name, u.phone FROM sys_user u " +
                "JOIN sys_user_role ur ON ur.user_id = u.id " +
                "JOIN sys_role r ON r.id = ur.role_id " +
                "WHERE r.role_code = 'GRID_WORKER' AND u.deleted = 0");

            for (Map<String, Object> user : gridUsers) {
                String name = (String) user.get("real_name");
                Long userId = ((Number) user.get("id")).longValue();
                // 检查是否已存在（按 sys_user_id 或 name）
                Integer exists = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM cmn_org_member WHERE sys_user_id = ? OR name = ?", Integer.class, userId, name);
                if (exists != null && exists > 0) {
                    skipped++;
                    continue;
                }
                // 创建组织人员
                jdbcTemplate.update(
                    "INSERT INTO cmn_org_member (grid_id, sys_user_id, member_type, name, phone, position, status, remark, created_at, updated_at) " +
                    "VALUES (?, ?, 'GRID_WORKER', ?, ?, '网格员', 'ACTIVE', '从系统用户同步', NOW(), NOW())",
                    null, user.get("id"), name, user.get("phone"));
                synced++;
            }
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "同步失败: " + e.getMessage());
            return ApiResponse.ok(result);
        }

        result.put("success", true);
        result.put("synced", synced);
        result.put("skipped", skipped);
        result.put("message", String.format("同步完成：新增 %d 条，跳过 %d 条（已存在）", synced, skipped));
        return ApiResponse.ok(result);
    }

    /**
     * 同步组长到系统用户表：创建/复用 sys_user 并分配「网格组长」内置角色，回填 org_member.sys_user_id。
     * 用户名 = 姓名去空格；密码默认 123456；手机号与已有账号冲突时置空避免唯一约束冲突。
     */
    private void syncLeaderToSysUser(Long memberId, OrgMemberEntity entity, String username) {
        try {
            Long roleId = ensureRole("GRID_LEADER", "网格组长", "内置角色：负责所辖网格的派单与审核");
            List<Long> ids = jdbcTemplate.queryForList(
                    "SELECT id FROM sys_user WHERE username = ? AND deleted = 0", Long.class, username);
            Long userId;
            if (ids.isEmpty()) {
                String phone = entity.getPhone();
                if (phone != null && !phone.isBlank()) {
                    Integer phoneExists = jdbcTemplate.queryForObject(
                            "SELECT COUNT(*) FROM sys_user WHERE phone = ? AND deleted = 0", Integer.class, phone);
                    if (phoneExists != null && phoneExists > 0) {
                        phone = null;
                    }
                }
                BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
                jdbcTemplate.update(
                        "INSERT INTO sys_user (username, password_hash, real_name, phone, status, password_version, deleted, created_at, updated_at) " +
                                "VALUES (?, ?, ?, ?, 'ACTIVE', 1, 0, NOW(), NOW())",
                        username, encoder.encode("123456"), entity.getName(), phone);
                userId = jdbcTemplate.queryForObject("SELECT id FROM sys_user WHERE username = ?", Long.class, username);
            } else {
                userId = ids.get(0);
            }
            jdbcTemplate.update("INSERT IGNORE INTO sys_user_role (user_id, role_id) VALUES (?, ?)", userId, roleId);
            jdbcTemplate.update("UPDATE cmn_org_member SET sys_user_id = ? WHERE id = ?", userId, memberId);
        } catch (Exception e) {
            // 账号同步失败不影响组长本身创建成功，仅记录日志
            org.slf4j.LoggerFactory.getLogger(OrgMemberController.class).warn("同步组长到系统用户失败: memberId={}, err={}", memberId, e.getMessage());
        }
    }

    /**
     * 职位与系统角色绑定：根据组织人员职位/类型解析对应角色并覆盖账号角色。
     * 组长/网格长/LEADER → 网格组长(GRID_LEADER)；网格员 → GRID_WORKER；社区工作人员 → EVENT_OPERATOR。
     * 未关联账号或无法解析角色时跳过（如志愿者）。
     */
    private void syncOrgMemberRoleToUser(OrgMemberEntity entity) {
        try {
            if (entity.getSysUserId() == null) return;
            String roleCode = null;
            String pos = entity.getPosition() == null ? "" : entity.getPosition();
            if ("LEADER".equals(entity.getMemberType()) || pos.contains("组长") || pos.contains("网格长")) {
                roleCode = "GRID_LEADER";
            } else if ("GRID_WORKER".equals(entity.getMemberType()) || pos.contains("网格员")) {
                roleCode = "GRID_WORKER";
            } else if ("STAFF".equals(entity.getMemberType())) {
                roleCode = "EVENT_OPERATOR";
            }
            if (roleCode == null) return;
            String roleName = switch (roleCode) {
                case "GRID_LEADER" -> "网格组长";
                case "GRID_WORKER" -> "网格员";
                default -> "管理员";
            };
            Long roleId = ensureRole(roleCode, roleName, "内置角色");
            // 每个账号仅保留一个角色（与 V101 规则一致）：先清后设
            jdbcTemplate.update("DELETE FROM sys_user_role WHERE user_id = ?", entity.getSysUserId());
            jdbcTemplate.update("INSERT INTO sys_user_role (user_id, role_id, created_at, updated_at) VALUES (?, ?, NOW(), NOW())",
                    entity.getSysUserId(), roleId);
        } catch (Exception e) {
            // 角色同步失败不影响成员信息保存，仅记录日志
            org.slf4j.LoggerFactory.getLogger(OrgMemberController.class).warn("同步组织成员角色失败: memberId={}, err={}", entity.getId(), e.getMessage());
        }
    }

    private Long ensureRole(String roleCode, String roleName, String remark) {
        Integer roleCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sys_role WHERE role_code = ?", Integer.class, roleCode);
        if (roleCount == null || roleCount == 0) {
            jdbcTemplate.update(
                    "INSERT INTO sys_role (role_code, role_name, status, remark, created_at, updated_at) " +
                            "VALUES (?, ?, 'ACTIVE', ?, NOW(), NOW())",
                    roleCode, roleName, remark);
        }
        return jdbcTemplate.queryForObject("SELECT id FROM sys_role WHERE role_code = ?", Long.class, roleCode);
    }

    /**
     * 同步组织人员到系统用户表（考核研判数据来源）
     */
    private void syncToSysUser(OrgMemberEntity entity) {
        try {
            // 检查是否已存在同名用户
            String username = entity.getName().replaceAll("\\s+", "");
            Integer exists = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sys_user WHERE username = ? AND deleted = 0", Integer.class, username);
            if (exists != null && exists > 0) return;

            // 确保 GRID_WORKER 角色存在
            Integer roleCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sys_role WHERE role_code = 'GRID_WORKER'", Integer.class);
            if (roleCount == null || roleCount == 0) {
                jdbcTemplate.update(
                    "INSERT INTO sys_role (role_code, role_name, status, remark, created_at, updated_at) VALUES ('GRID_WORKER', '网格员', 'ACTIVE', '网格巡查与事件处置人员', NOW(), NOW())");
            }
            Long roleId = jdbcTemplate.queryForObject("SELECT id FROM sys_role WHERE role_code = 'GRID_WORKER'", Long.class);

            // 创建系统用户
            BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
            jdbcTemplate.update(
                "INSERT INTO sys_user (username, password_hash, real_name, phone, status, password_version, deleted, created_at, updated_at) " +
                "VALUES (?, ?, ?, ?, 'ACTIVE', 1, 0, NOW(), NOW())",
                username, encoder.encode("123456"), entity.getName(), entity.getPhone());
            Long userId = jdbcTemplate.queryForObject("SELECT id FROM sys_user WHERE username = ?", Long.class, username);
            // 分配 GRID_WORKER 角色
            jdbcTemplate.update("INSERT IGNORE INTO sys_user_role (user_id, role_id) VALUES (?, ?)", userId, roleId);
        } catch (Exception e) {
            // 同步失败不影响主流程
            org.slf4j.LoggerFactory.getLogger(OrgMemberController.class).warn("同步网格员到系统用户失败: {}", e.getMessage());
        }
    }

    private void requireOrgMemberPermission() {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.MENU_COMMUNITY_ORG_MEMBER);
    }
}
