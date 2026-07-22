package com.changping.platform.modules.qwen;

import com.changping.platform.common.response.ApiResponse;
import com.changping.platform.common.response.PagedResult;
import com.changping.platform.modules.auth.security.PermissionCodes;
import com.changping.platform.modules.auth.security.PermissionGuard;
import com.changping.platform.modules.auth.service.AuthService;
import com.changping.platform.modules.auth.service.CurrentUserService;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author tangxinglin
 * @Description //千问算法模型控制器，提供模型的分页查询、启用列表、详情、创建、更新及删除接口
 * @Date 2026/04/18 10:25
 */
@RestController
@RequestMapping("/qwen-models")
public class QwenAlgorithmModelController {

    private final QwenAlgorithmModelService qwenAlgorithmModelService;
    private final CurrentUserService currentUserService;
    private final PermissionGuard permissionGuard;

    /**
     * @Author tangxinglin
     * @Description //构造函数，注入千问模型服务、当前用户服务和权限校验
     * @Date 2026/04/18 10:25
     * @Param [qwenAlgorithmModelService 千问模型服务, currentUserService 当前用户服务, permissionGuard 权限校验]
     * @return
     */
    public QwenAlgorithmModelController(
            QwenAlgorithmModelService qwenAlgorithmModelService,
            CurrentUserService currentUserService,
            PermissionGuard permissionGuard) {
        this.qwenAlgorithmModelService = qwenAlgorithmModelService;
        this.currentUserService = currentUserService;
        this.permissionGuard = permissionGuard;
    }

    /**
     * @Author tangxinglin
     * @Description //分页查询千问算法模型列表
     * @Date 2026/04/18 10:25
     * @Param [page 页码，默认1, pageSize 每页条数，默认10]
     * @return ApiResponse<PagedResult<QwenAlgorithmModelService.ModelItem>> 分页模型列表
     */
    @GetMapping
    public ApiResponse<PagedResult<QwenAlgorithmModelService.ModelItem>> listPaged(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.API_QWEN_MODEL_LIST);
        return ApiResponse.ok(qwenAlgorithmModelService.listPaged(page, pageSize));
    }

    /**
     * @Author tangxinglin
     * @Description //查询所有已启用（status=1）的千问算法模型列表
     * @Date 2026/04/18 10:25
     * @Param []
     * @return ApiResponse<List<QwenAlgorithmModelService.ModelItem>> 已启用模型列表
     */
    @GetMapping("/enabled")
    public ApiResponse<List<QwenAlgorithmModelService.ModelItem>> listEnabled() {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.API_QWEN_MODEL_LIST);
        return ApiResponse.ok(qwenAlgorithmModelService.listEnabled());
    }

    /**
     * @Author tangxinglin
     * @Description //根据 ID 获取千问算法模型详情
     * @Date 2026/04/18 10:25
     * @Param [id 模型ID]
     * @return ApiResponse<QwenAlgorithmModelService.ModelItem> 模型详情
     */
    @GetMapping("/{id}")
    public ApiResponse<QwenAlgorithmModelService.ModelItem> getById(@PathVariable Long id) {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.API_QWEN_MODEL_LIST);
        return ApiResponse.ok(qwenAlgorithmModelService.getById(id));
    }

    /**
     * @Author tangxinglin
     * @Description //创建新的千问算法模型
     * @Date 2026/04/18 10:25
     * @Param [request 创建模型请求，包含名称、标签、采集间隔、状态和描述]
     * @return ApiResponse<QwenAlgorithmModelService.ModelItem> 新建的模型详情
     */
    @PostMapping
    public ApiResponse<QwenAlgorithmModelService.ModelItem> create(
            @RequestBody QwenAlgorithmModelService.CreateQwenModelRequest request) {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.API_QWEN_MODEL_CREATE);
        return ApiResponse.ok(qwenAlgorithmModelService.create(request));
    }

    /**
     * @Author tangxinglin
     * @Description //更新指定千问算法模型的信息
     * @Date 2026/04/18 10:25
     * @Param [id 模型ID, request 更新模型请求]
     * @return ApiResponse<QwenAlgorithmModelService.ModelItem> 更新后的模型详情
     */
    @PutMapping("/{id}")
    public ApiResponse<QwenAlgorithmModelService.ModelItem> update(
            @PathVariable Long id,
            @RequestBody QwenAlgorithmModelService.UpdateQwenModelRequest request) {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.API_QWEN_MODEL_UPDATE);
        return ApiResponse.ok(qwenAlgorithmModelService.update(id, request));
    }

    /**
     * @Author tangxinglin
     * @Description //逻辑删除指定千问算法模型（软删除，设置 deleted=1）
     * @Date 2026/04/18 10:25
     * @Param [id 模型ID]
     * @return ApiResponse<Void> 无返回数据
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.API_QWEN_MODEL_DELETE);
        qwenAlgorithmModelService.delete(id);
        return ApiResponse.ok(null);
    }
}
