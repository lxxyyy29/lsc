package com.changping.platform.modules.process.controller;

import com.changping.platform.common.response.ApiResponse;
import com.changping.platform.common.response.PagedResult;
import com.changping.platform.modules.auth.security.PermissionCodes;
import com.changping.platform.modules.auth.security.PermissionGuard;
import com.changping.platform.modules.process.entity.ProcessTemplateEntity;
import com.changping.platform.modules.process.service.ProcessTemplateService;
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
 * @Author lxy
 * @Description //流程模板控制器，提供流程模板的创建、查询、分页查询、更新和删除接口
 * @Date 2026/04/18 10:00
 */
@RestController
@RequestMapping("/processes/templates")
public class ProcessTemplateController {

    private final ProcessTemplateService processTemplateService;
    private final PermissionGuard permissionGuard;

    /**
     * @Author lxy
     * @Description //构造器，注入流程模板服务和权限守卫
     * @Date 2026/04/18 10:00
     * @Param [processTemplateService 流程模板服务, permissionGuard 权限守卫]
     * @return void
     */
    public ProcessTemplateController(ProcessTemplateService processTemplateService, PermissionGuard permissionGuard) {
        this.processTemplateService = processTemplateService;
        this.permissionGuard = permissionGuard;
    }

    /**
     * @Author lxy
     * @Description //创建流程模板接口
     * @Date 2026/04/18 10:00
     * @Param [request 创建模板请求对象，包含模板名称、节点配置等信息]
     * @return ApiResponse<ProcessTemplateEntity> 新建的流程模板实体
     */
    @PostMapping
    public ApiResponse<ProcessTemplateEntity> createTemplate(@RequestBody ProcessTemplateService.CreateTemplateRequest request) {
        permissionGuard.require(PermissionCodes.API_PROCESS_TEMPLATE_CREATE);
        return ApiResponse.ok(processTemplateService.createTemplate(request));
    }

    /**
     * @Author lxy
     * @Description //查询所有流程模板列表（不分页）
     * @Date 2026/04/18 10:00
     * @Param []
     * @return ApiResponse<List<ProcessTemplateEntity>> 流程模板列表
     */
    @GetMapping
    public ApiResponse<List<ProcessTemplateEntity>> listTemplates() {
        permissionGuard.require(PermissionCodes.API_PROCESS_TEMPLATE_LIST);
        return ApiResponse.ok(processTemplateService.listTemplates());
    }

    /**
     * @Author lxy
     * @Description //分页查询流程模板列表，支持按关键词过滤
     * @Date 2026/04/18 10:00
     * @Param [page 页码（默认1）, pageSize 每页条数（默认10）, keyword 关键词（可选，按模板名称模糊匹配）]
     * @return ApiResponse<PagedResult<ProcessTemplateEntity>> 分页流程模板列表
     */
    @GetMapping("/paged")
    public ApiResponse<PagedResult<ProcessTemplateEntity>> listTemplatesPaged(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String keyword) {
        permissionGuard.require(PermissionCodes.API_PROCESS_TEMPLATE_LIST);
        return ApiResponse.ok(processTemplateService.listTemplatesPaged(page, pageSize, keyword));
    }

    /**
     * @Author lxy
     * @Description //根据ID获取流程模板详情
     * @Date 2026/04/18 10:00
     * @Param [id 流程模板主键ID]
     * @return ApiResponse<ProcessTemplateEntity> 流程模板详情
     */
    @GetMapping("/{id}")
    public ApiResponse<ProcessTemplateEntity> getTemplate(@PathVariable Long id) {
        permissionGuard.require(PermissionCodes.API_PROCESS_TEMPLATE_DETAIL);
        return ApiResponse.ok(processTemplateService.getTemplate(id));
    }

    /**
     * @Author lxy
     * @Description //更新流程模板接口，会重建节点配置
     * @Date 2026/04/18 10:00
     * @Param [id 流程模板主键ID, request 更新模板请求对象]
     * @return ApiResponse<ProcessTemplateEntity> 更新后的流程模板实体
     */
    @PutMapping("/{id}")
    public ApiResponse<ProcessTemplateEntity> updateTemplate(
            @PathVariable Long id,
            @RequestBody ProcessTemplateService.UpdateTemplateRequest request) {
        permissionGuard.require(PermissionCodes.API_PROCESS_TEMPLATE_UPDATE);
        return ApiResponse.ok(processTemplateService.updateTemplate(id, request));
    }

    /**
     * @Author lxy
     * @Description //删除流程模板及其所有节点配置接口
     * @Date 2026/04/18 10:00
     * @Param [id 流程模板主键ID]
     * @return ApiResponse<Void> void
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteTemplate(@PathVariable Long id) {
        permissionGuard.require(PermissionCodes.API_PROCESS_TEMPLATE_DELETE);
        processTemplateService.deleteTemplate(id);
        return ApiResponse.ok(null);
    }
}
