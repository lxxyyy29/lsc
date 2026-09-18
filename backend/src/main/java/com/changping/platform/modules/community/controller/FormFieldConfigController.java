package com.changping.platform.modules.community.controller;

import com.changping.platform.common.response.ApiResponse;
import com.changping.platform.common.exception.BusinessException;
import com.changping.platform.modules.auth.security.PermissionCodes;
import com.changping.platform.modules.auth.security.PermissionGuard;
import com.changping.platform.modules.auth.service.AuthService;
import com.changping.platform.modules.auth.service.CurrentUserService;
import com.changping.platform.modules.community.entity.FormFieldConfigEntity;
import com.changping.platform.modules.community.mapper.FormFieldConfigMapper;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 表单字段配置器（仅人口库）：查询/批量保存字段配置
 */
@RestController
@RequestMapping("/community/form-field-config")
public class FormFieldConfigController {

    private final FormFieldConfigMapper mapper;
    private final CurrentUserService currentUserService;
    private final PermissionGuard permissionGuard;

    public FormFieldConfigController(FormFieldConfigMapper mapper,
                                     CurrentUserService currentUserService,
                                     PermissionGuard permissionGuard) {
        this.mapper = mapper;
        this.currentUserService = currentUserService;
        this.permissionGuard = permissionGuard;
    }

    /** 查询某模块的字段配置 */
    @GetMapping
    public ApiResponse<List<FormFieldConfigEntity>> list(@RequestParam String module) {
        requirePermission();
        return ApiResponse.ok(mapper.listByModule(module));
    }

    /** 字段配置模块（防止越权改动其它模块） */
    private static final String MODULE = "population";

    /** 允许的字段类型：需与前端表单渲染分支一致（text/textarea/select/date/checkbox） */
    private static final Set<String> ALLOWED_TYPES = Set.of("text", "textarea", "select", "date", "checkbox");

    /**
     * 保存字段配置（对账式批量）：本次提交名单里的字段「有 id 则更新、无 id 则新增」，
     * 库中存在但本次名单里缺失的字段视为删除。
     *
     * <p>之所以采用「对账」而不是再开一个删除接口：前端弹窗本来就是整表编辑，
     * 一次提交即可表达最终状态，避免「删了但没保存」这类中间态。
     */
    @PostMapping
    public ApiResponse<Boolean> save(@RequestBody List<FormFieldConfigEntity> fields) {
        requirePermission();
        List<FormFieldConfigEntity> existing = mapper.listByModule(MODULE);
        Map<Long, FormFieldConfigEntity> existingById = new LinkedHashMap<>();
        Set<String> knownKeys = new HashSet<>();
        for (FormFieldConfigEntity e : existing) {
            existingById.put(e.getId(), e);
            knownKeys.add(FormFieldConfigMapper.normalizeKey(e.getFieldKey()));
        }

        // 第一遍：只做校验（新增字段合法性、键重复、系统必需字段不可删除）。
        // 校验全部通过后才在第二遍写库，避免"报了错但前面几条已经改了"的半成品状态。
        Set<Long> keptIds = new HashSet<>();
        for (FormFieldConfigEntity f : fields) {
            if (f.getId() == null) {
                validateNewField(f, knownKeys);
            } else if (existingById.containsKey(f.getId())) {
                keptIds.add(f.getId());
            }
            // id 不为空但不在本模块库中：忽略，避免越权改动其它模块的配置
        }
        List<FormFieldConfigEntity> toDelete = new ArrayList<>();
        for (FormFieldConfigEntity e : existing) {
            if (keptIds.contains(e.getId())) continue;
            if (FormFieldConfigMapper.isSystemRequired(e.getFieldKey())) {
                throw new BusinessException("FIELD_SYSTEM_REQUIRED",
                        "「" + e.getFieldLabel() + "」为系统必需字段，不能删除");
            }
            toDelete.add(e);
        }

        // 第二遍：按提交顺序落库（顺序即界面上看到的顺序）
        int order = 0;
        for (FormFieldConfigEntity f : fields) {
            order++;
            f.setModule(MODULE);
            if (f.getEnabled() == null) f.setEnabled(1);
            if (f.getRequired() == null) f.setRequired(0);
            f.setSortOrder(order);
            if (f.getId() == null) {
                mapper.insert(f);
            } else if (existingById.containsKey(f.getId())) {
                mapper.update(f);
            }
        }
        if (!toDelete.isEmpty()) {
            mapper.deleteByIds(toDelete.stream().map(FormFieldConfigEntity::getId).toList());
        }
        return ApiResponse.ok(true);
    }

    /** 新增字段校验：字段键、显示名、类型与选项；通过后把键登记进 knownKeys 防本批次重复 */
    private void validateNewField(FormFieldConfigEntity f, Set<String> knownKeys) {
        String key = f.getFieldKey() == null ? "" : f.getFieldKey().trim();
        if (key.isEmpty()) {
            throw new BusinessException("FIELD_KEY_REQUIRED", "请填写字段键");
        }
        if (!key.matches("^[A-Za-z][A-Za-z0-9_]{0,31}$")) {
            throw new BusinessException("FIELD_KEY_INVALID", "字段键需以字母开头，只能包含字母、数字、下划线，最长 32 位");
        }
        if (f.getFieldLabel() == null || f.getFieldLabel().isBlank()) {
            throw new BusinessException("FIELD_LABEL_REQUIRED", "请填写字段显示名");
        }
        String type = (f.getFieldType() == null || f.getFieldType().isBlank()) ? "text" : f.getFieldType().trim();
        if (!ALLOWED_TYPES.contains(type)) {
            throw new BusinessException("FIELD_TYPE_INVALID", "不支持的字段类型：" + type);
        }
        if ("select".equals(type) && (f.getOptions() == null || f.getOptions().isBlank())) {
            throw new BusinessException("FIELD_OPTIONS_REQUIRED", "下拉类型必须填写选项");
        }
        if (!knownKeys.add(FormFieldConfigMapper.normalizeKey(key)) || mapper.existsKey(MODULE, key)) {
            throw new BusinessException("FIELD_KEY_DUPLICATE", "字段键「" + key + "」已存在");
        }
        // 归一化后回写，第二遍落库直接使用
        f.setFieldKey(key);
        f.setFieldType(type);
    }

    private void requirePermission() {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.MENU_COMMUNITY_POPULATION);
    }
}
