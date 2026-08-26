package com.changping.platform.modules.biz.controller;

import com.changping.platform.common.response.ApiResponse;
import com.changping.platform.modules.auth.security.PermissionCodes;
import com.changping.platform.modules.auth.security.PermissionGuard;
import com.changping.platform.modules.auth.service.AuthService;
import com.changping.platform.modules.auth.service.CurrentUserService;
import com.changping.platform.modules.biz.service.BizManagementService;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author lxy
 * @Description //H5端业务管理控制器，为移动端字段人员提供商户和摊贩的增删改查接口及辖区选项查询，仅限 H5 端访问
 * @Date 2026/04/18 10:15
 */
@RestController
@RequestMapping("/h5")
public class H5BizManagementController {

    private final BizManagementService bizManagementService;
    private final CurrentUserService currentUserService;
    private final PermissionGuard permissionGuard;

    /**
     * @Author lxy
     * @Description //构造函数注入业务管理服务、当前用户服务和权限守卫
     * @Date 2026/04/18 10:15
     * @Param [bizManagementService 业务管理服务, currentUserService 当前用户服务, permissionGuard 权限守卫]
     * @return void
     */
    public H5BizManagementController(
            BizManagementService bizManagementService,
            CurrentUserService currentUserService,
            PermissionGuard permissionGuard) {
        this.bizManagementService = bizManagementService;
        this.currentUserService = currentUserService;
        this.permissionGuard = permissionGuard;
    }

    // ─── Area Options (for merchant form) ─────────────────────────────────────

    /**
     * @Author lxy
     * @Description //查询辖区下拉选项列表，供 H5 端商户表单使用，需要 API_H5_MERCHANT_LIST 权限
     * @Date 2026/04/18 10:15
     * @Param []
     * @return ApiResponse<List<BizManagementService.AreaOptionItem>> 辖区选项列表
     */
    @GetMapping("/areas/options")
    public ApiResponse<List<BizManagementService.AreaOptionItem>> listAreaOptions() {
        currentUserService.requireClientType(AuthService.ClientType.H5);
        permissionGuard.require(PermissionCodes.API_H5_MERCHANT_LIST);
        return ApiResponse.ok(bizManagementService.listAreaOptions());
    }

    // ─── Merchants ────────────────────────────────────────────────────────────

    /**
     * @Author lxy
     * @Description //H5端查询全部商户列表，需要 API_H5_MERCHANT_LIST 权限
     * @Date 2026/04/18 10:15
     * @Param []
     * @return ApiResponse<List<BizManagementService.MerchantItem>> 商户列表
     */
    @GetMapping("/merchants")
    public ApiResponse<List<BizManagementService.MerchantItem>> listMerchants() {
        currentUserService.requireClientType(AuthService.ClientType.H5);
        permissionGuard.require(PermissionCodes.API_H5_MERCHANT_LIST);
        return ApiResponse.ok(bizManagementService.listMerchants());
    }

    /**
     * @Author lxy
     * @Description //H5端根据ID查询商户详情，需要 API_H5_MERCHANT_DETAIL 权限
     * @Date 2026/04/18 10:15
     * @Param [id 商户ID]
     * @return ApiResponse<BizManagementService.MerchantItem> 商户详情
     */
    @GetMapping("/merchants/{id}")
    public ApiResponse<BizManagementService.MerchantItem> getMerchant(@PathVariable Long id) {
        currentUserService.requireClientType(AuthService.ClientType.H5);
        permissionGuard.require(PermissionCodes.API_H5_MERCHANT_DETAIL);
        return ApiResponse.ok(bizManagementService.getMerchant(id));
    }

    /**
     * @Author lxy
     * @Description //H5端创建新商户，需要 API_H5_MERCHANT_CREATE 权限
     * @Date 2026/04/18 10:15
     * @Param [request 创建商户请求，包含商户名称、经纬度、法人信息等]
     * @return ApiResponse<BizManagementService.MerchantItem> 创建后的商户详情
     */
    @PostMapping("/merchants")
    public ApiResponse<BizManagementService.MerchantItem> createMerchant(
            @RequestBody BizManagementService.CreateMerchantRequest request) {
        currentUserService.requireClientType(AuthService.ClientType.H5);
        permissionGuard.require(PermissionCodes.API_H5_MERCHANT_CREATE);
        return ApiResponse.ok(bizManagementService.createMerchant(request));
    }

    /**
     * @Author lxy
     * @Description //H5端更新指定商户信息，需要 API_H5_MERCHANT_UPDATE 权限
     * @Date 2026/04/18 10:15
     * @Param [id 商户ID, request 更新商户请求]
     * @return ApiResponse<BizManagementService.MerchantItem> 更新后的商户详情
     */
    @PutMapping("/merchants/{id}")
    public ApiResponse<BizManagementService.MerchantItem> updateMerchant(
            @PathVariable Long id,
            @RequestBody BizManagementService.UpdateMerchantRequest request) {
        currentUserService.requireClientType(AuthService.ClientType.H5);
        permissionGuard.require(PermissionCodes.API_H5_MERCHANT_UPDATE);
        return ApiResponse.ok(bizManagementService.updateMerchant(id, request));
    }

    /**
     * @Author lxy
     * @Description //H5端删除指定商户，需要 API_H5_MERCHANT_DELETE 权限
     * @Date 2026/04/18 10:15
     * @Param [id 商户ID]
     * @return ApiResponse<Void> 删除成功响应
     */
    @DeleteMapping("/merchants/{id}")
    public ApiResponse<Void> deleteMerchant(@PathVariable Long id) {
        currentUserService.requireClientType(AuthService.ClientType.H5);
        permissionGuard.require(PermissionCodes.API_H5_MERCHANT_DELETE);
        bizManagementService.deleteMerchant(id);
        return ApiResponse.ok(null);
    }

    // ─── Mobile Vendors ───────────────────────────────────────────────────────

    /**
     * @Author lxy
     * @Description //H5端查询全部摊贩列表，需要 API_H5_VENDOR_LIST 权限
     * @Date 2026/04/18 10:15
     * @Param []
     * @return ApiResponse<List<BizManagementService.VendorItem>> 摊贩列表
     */
    @GetMapping("/mobile-vendors")
    public ApiResponse<List<BizManagementService.VendorItem>> listVendors() {
        currentUserService.requireClientType(AuthService.ClientType.H5);
        permissionGuard.require(PermissionCodes.API_H5_VENDOR_LIST);
        return ApiResponse.ok(bizManagementService.listVendors());
    }

    /**
     * @Author lxy
     * @Description //H5端根据ID查询摊贩详情，需要 API_H5_VENDOR_DETAIL 权限
     * @Date 2026/04/18 10:15
     * @Param [id 摊贩ID]
     * @return ApiResponse<BizManagementService.VendorItem> 摊贩详情
     */
    @GetMapping("/mobile-vendors/{id}")
    public ApiResponse<BizManagementService.VendorItem> getVendor(@PathVariable Long id) {
        currentUserService.requireClientType(AuthService.ClientType.H5);
        permissionGuard.require(PermissionCodes.API_H5_VENDOR_DETAIL);
        return ApiResponse.ok(bizManagementService.getVendor(id));
    }

    /**
     * @Author lxy
     * @Description //H5端创建新摊贩，需要 API_H5_VENDOR_CREATE 权限
     * @Date 2026/04/18 10:15
     * @Param [request 创建摊贩请求，包含摊贩名称、法人信息等]
     * @return ApiResponse<BizManagementService.VendorItem> 创建后的摊贩详情
     */
    @PostMapping("/mobile-vendors")
    public ApiResponse<BizManagementService.VendorItem> createVendor(
            @RequestBody BizManagementService.CreateVendorRequest request) {
        currentUserService.requireClientType(AuthService.ClientType.H5);
        permissionGuard.require(PermissionCodes.API_H5_VENDOR_CREATE);
        return ApiResponse.ok(bizManagementService.createVendor(request));
    }

    /**
     * @Author lxy
     * @Description //H5端更新指定摊贩信息，需要 API_H5_VENDOR_UPDATE 权限
     * @Date 2026/04/18 10:15
     * @Param [id 摊贩ID, request 更新摊贩请求]
     * @return ApiResponse<BizManagementService.VendorItem> 更新后的摊贩详情
     */
    @PutMapping("/mobile-vendors/{id}")
    public ApiResponse<BizManagementService.VendorItem> updateVendor(
            @PathVariable Long id,
            @RequestBody BizManagementService.UpdateVendorRequest request) {
        currentUserService.requireClientType(AuthService.ClientType.H5);
        permissionGuard.require(PermissionCodes.API_H5_VENDOR_UPDATE);
        return ApiResponse.ok(bizManagementService.updateVendor(id, request));
    }

    /**
     * @Author lxy
     * @Description //H5端删除指定摊贩，需要 API_H5_VENDOR_DELETE 权限
     * @Date 2026/04/18 10:15
     * @Param [id 摊贩ID]
     * @return ApiResponse<Void> 删除成功响应
     */
    @DeleteMapping("/mobile-vendors/{id}")
    public ApiResponse<Void> deleteVendor(@PathVariable Long id) {
        currentUserService.requireClientType(AuthService.ClientType.H5);
        permissionGuard.require(PermissionCodes.API_H5_VENDOR_DELETE);
        bizManagementService.deleteVendor(id);
        return ApiResponse.ok(null);
    }
}
