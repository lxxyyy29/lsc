package com.changping.platform.modules.biz.controller;

import com.changping.platform.common.response.ApiResponse;
import com.changping.platform.common.response.PagedResult;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author lxy
 * @Description //Web端业务管理控制器，提供辖区、商户、摊贩和违规区域的增删改查接口，仅限 Web 端访问
 * @Date 2026/04/18 10:15
 */
@RestController
@RequestMapping
public class BizManagementController {

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
    public BizManagementController(
            BizManagementService bizManagementService,
            CurrentUserService currentUserService,
            PermissionGuard permissionGuard) {
        this.bizManagementService = bizManagementService;
        this.currentUserService = currentUserService;
        this.permissionGuard = permissionGuard;
    }

    /**
     * @Author lxy
     * @Description //查询全部辖区列表（不分页），需要 API_BIZ_AREA_LIST 权限
     * @Date 2026/04/18 10:15
     * @Param []
     * @return ApiResponse<List<BizManagementService.AreaItem>> 辖区列表
     */
    @GetMapping("/areas")
    public ApiResponse<List<BizManagementService.AreaItem>> listAreas() {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.API_BIZ_AREA_LIST);
        return ApiResponse.ok(bizManagementService.listAreas());
    }

    /**
     * @Author lxy
     * @Description //分页查询辖区列表，支持关键字和状态过滤，需要 API_BIZ_AREA_LIST 权限
     * @Date 2026/04/18 10:15
     * @Param [page 当前页码, pageSize 每页大小, keyword 辖区名称关键字（可选）, status 状态过滤（可选）]
     * @return ApiResponse<PagedResult<BizManagementService.AreaItem>> 分页辖区列表
     */
    @GetMapping("/areas/paged")
    public ApiResponse<PagedResult<BizManagementService.AreaItem>> listAreasPaged(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status) {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.API_BIZ_AREA_LIST);
        return ApiResponse.ok(bizManagementService.listAreasPaged(page, pageSize, keyword, status));
    }

    /**
     * @Author lxy
     * @Description //查询辖区下拉选项列表（仅返回ID和名称），需要 API_BIZ_AREA_LIST 权限
     * @Date 2026/04/18 10:15
     * @Param []
     * @return ApiResponse<List<BizManagementService.AreaOptionItem>> 辖区选项列表
     */
    @GetMapping("/areas/options")
    public ApiResponse<List<BizManagementService.AreaOptionItem>> listAreaOptions() {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.API_BIZ_AREA_LIST);
        return ApiResponse.ok(bizManagementService.listAreaOptions());
    }

    /**
     * @Author lxy
     * @Description //根据ID查询辖区详情，需要 API_BIZ_AREA_DETAIL 权限
     * @Date 2026/04/18 10:15
     * @Param [id 辖区ID]
     * @return ApiResponse<BizManagementService.AreaItem> 辖区详情
     */
    @GetMapping("/areas/{id}")
    public ApiResponse<BizManagementService.AreaItem> getArea(@PathVariable Long id) {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.API_BIZ_AREA_DETAIL);
        return ApiResponse.ok(bizManagementService.getArea(id));
    }

    /**
     * @Author lxy
     * @Description //创建新辖区，需要 API_BIZ_AREA_CREATE 权限
     * @Date 2026/04/18 10:15
     * @Param [request 创建辖区请求，包含辖区名称、负责人、ROI坐标等信息]
     * @return ApiResponse<BizManagementService.AreaItem> 创建后的辖区详情
     */
    @PostMapping("/areas")
    public ApiResponse<BizManagementService.AreaItem> createArea(@RequestBody BizManagementService.CreateAreaRequest request) {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.API_BIZ_AREA_CREATE);
        return ApiResponse.ok(bizManagementService.createArea(request));
    }

    /**
     * @Author lxy
     * @Description //更新指定辖区信息，需要 API_BIZ_AREA_UPDATE 权限
     * @Date 2026/04/18 10:15
     * @Param [id 辖区ID, request 更新辖区请求]
     * @return ApiResponse<BizManagementService.AreaItem> 更新后的辖区详情
     */
    @PutMapping("/areas/{id}")
    public ApiResponse<BizManagementService.AreaItem> updateArea(
            @PathVariable Long id,
            @RequestBody BizManagementService.UpdateAreaRequest request) {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.API_BIZ_AREA_UPDATE);
        return ApiResponse.ok(bizManagementService.updateArea(id, request));
    }

    /**
     * @Author lxy
     * @Description //删除指定辖区，若辖区下存在商户则禁止删除，需要 API_BIZ_AREA_DELETE 权限
     * @Date 2026/04/18 10:15
     * @Param [id 辖区ID]
     * @return ApiResponse<Void> 删除成功响应
     */
    @DeleteMapping("/areas/{id}")
    public ApiResponse<Void> deleteArea(@PathVariable Long id) {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.API_BIZ_AREA_DELETE);
        bizManagementService.deleteArea(id);
        return ApiResponse.ok(null);
    }

    /**
     * @Author lxy
     * @Description //查询全部商户列表（不分页），需要 API_BIZ_MERCHANT_LIST 权限
     * @Date 2026/04/18 10:15
     * @Param []
     * @return ApiResponse<List<BizManagementService.MerchantItem>> 商户列表
     */
    @GetMapping("/merchants")
    public ApiResponse<List<BizManagementService.MerchantItem>> listMerchants() {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.API_BIZ_MERCHANT_LIST);
        return ApiResponse.ok(bizManagementService.listMerchants());
    }

    /**
     * @Author lxy
     * @Description //分页查询商户列表，支持关键字、辖区和状态过滤，需要 API_BIZ_MERCHANT_LIST 权限
     * @Date 2026/04/18 10:15
     * @Param [page 当前页码, pageSize 每页大小, keyword 商户名称关键字（可选）, areaId 辖区ID（可选）, status 状态过滤（可选）]
     * @return ApiResponse<PagedResult<BizManagementService.MerchantItem>> 分页商户列表
     */
    @GetMapping("/merchants/paged")
    public ApiResponse<PagedResult<BizManagementService.MerchantItem>> listMerchantsPaged(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long areaId,
            @RequestParam(required = false) String status) {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.API_BIZ_MERCHANT_LIST);
        return ApiResponse.ok(bizManagementService.listMerchantsPaged(page, pageSize, keyword, areaId, status));
    }

    /**
     * @Author lxy
     * @Description //根据ID查询商户详情，需要 API_BIZ_MERCHANT_DETAIL 权限
     * @Date 2026/04/18 10:15
     * @Param [id 商户ID]
     * @return ApiResponse<BizManagementService.MerchantItem> 商户详情
     */
    @GetMapping("/merchants/{id}")
    public ApiResponse<BizManagementService.MerchantItem> getMerchant(@PathVariable Long id) {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.API_BIZ_MERCHANT_DETAIL);
        return ApiResponse.ok(bizManagementService.getMerchant(id));
    }

    /**
     * @Author lxy
     * @Description //创建新商户，需要 API_BIZ_MERCHANT_CREATE 权限
     * @Date 2026/04/18 10:15
     * @Param [request 创建商户请求，包含商户名称、经纬度、法人信息等]
     * @return ApiResponse<BizManagementService.MerchantItem> 创建后的商户详情
     */
    @PostMapping("/merchants")
    public ApiResponse<BizManagementService.MerchantItem> createMerchant(@RequestBody BizManagementService.CreateMerchantRequest request) {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.API_BIZ_MERCHANT_CREATE);
        return ApiResponse.ok(bizManagementService.createMerchant(request));
    }

    /**
     * @Author lxy
     * @Description //更新指定商户信息，需要 API_BIZ_MERCHANT_UPDATE 权限
     * @Date 2026/04/18 10:15
     * @Param [id 商户ID, request 更新商户请求]
     * @return ApiResponse<BizManagementService.MerchantItem> 更新后的商户详情
     */
    @PutMapping("/merchants/{id}")
    public ApiResponse<BizManagementService.MerchantItem> updateMerchant(
            @PathVariable Long id,
            @RequestBody BizManagementService.UpdateMerchantRequest request) {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.API_BIZ_MERCHANT_UPDATE);
        return ApiResponse.ok(bizManagementService.updateMerchant(id, request));
    }

    /**
     * @Author lxy
     * @Description //删除指定商户，需要 API_BIZ_MERCHANT_DELETE 权限
     * @Date 2026/04/18 10:15
     * @Param [id 商户ID]
     * @return ApiResponse<Void> 删除成功响应
     */
    @DeleteMapping("/merchants/{id}")
    public ApiResponse<Void> deleteMerchant(@PathVariable Long id) {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.API_BIZ_MERCHANT_DELETE);
        bizManagementService.deleteMerchant(id);
        return ApiResponse.ok(null);
    }

    /**
     * @Author lxy
     * @Description //查询全部摊贩列表（不分页），需要 API_BIZ_VENDOR_LIST 权限
     * @Date 2026/04/18 10:15
     * @Param []
     * @return ApiResponse<List<BizManagementService.VendorItem>> 摊贩列表
     */
    @GetMapping("/mobile-vendors")
    public ApiResponse<List<BizManagementService.VendorItem>> listVendors() {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.API_BIZ_VENDOR_LIST);
        return ApiResponse.ok(bizManagementService.listVendors());
    }

    /**
     * @Author lxy
     * @Description //分页查询摊贩列表，支持关键字和状态过滤，需要 API_BIZ_VENDOR_LIST 权限
     * @Date 2026/04/18 10:15
     * @Param [page 当前页码, pageSize 每页大小, keyword 摊贩名称关键字（可选）, status 状态过滤（可选）]
     * @return ApiResponse<PagedResult<BizManagementService.VendorItem>> 分页摊贩列表
     */
    @GetMapping("/mobile-vendors/paged")
    public ApiResponse<PagedResult<BizManagementService.VendorItem>> listVendorsPaged(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status) {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.API_BIZ_VENDOR_LIST);
        return ApiResponse.ok(bizManagementService.listVendorsPaged(page, pageSize, keyword, status));
    }

    /**
     * @Author lxy
     * @Description //根据ID查询摊贩详情，需要 API_BIZ_VENDOR_DETAIL 权限
     * @Date 2026/04/18 10:15
     * @Param [id 摊贩ID]
     * @return ApiResponse<BizManagementService.VendorItem> 摊贩详情
     */
    @GetMapping("/mobile-vendors/{id}")
    public ApiResponse<BizManagementService.VendorItem> getVendor(@PathVariable Long id) {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.API_BIZ_VENDOR_DETAIL);
        return ApiResponse.ok(bizManagementService.getVendor(id));
    }

    /**
     * @Author lxy
     * @Description //创建新摊贩，需要 API_BIZ_VENDOR_CREATE 权限
     * @Date 2026/04/18 10:15
     * @Param [request 创建摊贩请求，包含摊贩名称、法人信息等]
     * @return ApiResponse<BizManagementService.VendorItem> 创建后的摊贩详情
     */
    @PostMapping("/mobile-vendors")
    public ApiResponse<BizManagementService.VendorItem> createVendor(@RequestBody BizManagementService.CreateVendorRequest request) {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.API_BIZ_VENDOR_CREATE);
        return ApiResponse.ok(bizManagementService.createVendor(request));
    }

    /**
     * @Author lxy
     * @Description //更新指定摊贩信息，需要 API_BIZ_VENDOR_UPDATE 权限
     * @Date 2026/04/18 10:15
     * @Param [id 摊贩ID, request 更新摊贩请求]
     * @return ApiResponse<BizManagementService.VendorItem> 更新后的摊贩详情
     */
    @PutMapping("/mobile-vendors/{id}")
    public ApiResponse<BizManagementService.VendorItem> updateVendor(
            @PathVariable Long id,
            @RequestBody BizManagementService.UpdateVendorRequest request) {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.API_BIZ_VENDOR_UPDATE);
        return ApiResponse.ok(bizManagementService.updateVendor(id, request));
    }

    /**
     * @Author lxy
     * @Description //删除指定摊贩，需要 API_BIZ_VENDOR_DELETE 权限
     * @Date 2026/04/18 10:15
     * @Param [id 摊贩ID]
     * @return ApiResponse<Void> 删除成功响应
     */
    @DeleteMapping("/mobile-vendors/{id}")
    public ApiResponse<Void> deleteVendor(@PathVariable Long id) {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.API_BIZ_VENDOR_DELETE);
        bizManagementService.deleteVendor(id);
        return ApiResponse.ok(null);
    }

    // ---- Violation Area ----

    /**
     * @Author lxy
     * @Description //查询全部违规区域列表（不分页），需要 API_BIZ_VIOLATION_AREA_LIST 权限
     * @Date 2026/04/18 10:15
     * @Param []
     * @return ApiResponse<List<BizManagementService.ViolationAreaItem>> 违规区域列表
     */
    @GetMapping("/violation-areas")
    public ApiResponse<List<BizManagementService.ViolationAreaItem>> listViolationAreas() {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.API_BIZ_VIOLATION_AREA_LIST);
        return ApiResponse.ok(bizManagementService.listViolationAreas());
    }

    /**
     * @Author lxy
     * @Description //分页查询违规区域列表，支持关键字和状态过滤，需要 API_BIZ_VIOLATION_AREA_LIST 权限
     * @Date 2026/04/18 10:15
     * @Param [page 当前页码, pageSize 每页大小, keyword 区域名称关键字（可选）, status 状态过滤（可选）]
     * @return ApiResponse<PagedResult<BizManagementService.ViolationAreaItem>> 分页违规区域列表
     */
    @GetMapping("/violation-areas/paged")
    public ApiResponse<PagedResult<BizManagementService.ViolationAreaItem>> listViolationAreasPaged(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status) {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.API_BIZ_VIOLATION_AREA_LIST);
        return ApiResponse.ok(bizManagementService.listViolationAreasPaged(page, pageSize, keyword, status));
    }

    /**
     * @Author lxy
     * @Description //根据ID查询违规区域详情，需要 API_BIZ_VIOLATION_AREA_DETAIL 权限
     * @Date 2026/04/18 10:15
     * @Param [id 违规区域ID]
     * @return ApiResponse<BizManagementService.ViolationAreaItem> 违规区域详情
     */
    @GetMapping("/violation-areas/{id}")
    public ApiResponse<BizManagementService.ViolationAreaItem> getViolationArea(@PathVariable Long id) {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.API_BIZ_VIOLATION_AREA_DETAIL);
        return ApiResponse.ok(bizManagementService.getViolationArea(id));
    }

    /**
     * @Author lxy
     * @Description //创建新违规区域，需要 API_BIZ_VIOLATION_AREA_CREATE 权限
     * @Date 2026/04/18 10:15
     * @Param [request 创建违规区域请求，包含区域名称、类型和ROI坐标等]
     * @return ApiResponse<BizManagementService.ViolationAreaItem> 创建后的违规区域详情
     */
    @PostMapping("/violation-areas")
    public ApiResponse<BizManagementService.ViolationAreaItem> createViolationArea(@RequestBody BizManagementService.CreateViolationAreaRequest request) {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.API_BIZ_VIOLATION_AREA_CREATE);
        return ApiResponse.ok(bizManagementService.createViolationArea(request));
    }

    /**
     * @Author lxy
     * @Description //更新指定违规区域信息，需要 API_BIZ_VIOLATION_AREA_UPDATE 权限
     * @Date 2026/04/18 10:15
     * @Param [id 违规区域ID, request 更新违规区域请求]
     * @return ApiResponse<BizManagementService.ViolationAreaItem> 更新后的违规区域详情
     */
    @PutMapping("/violation-areas/{id}")
    public ApiResponse<BizManagementService.ViolationAreaItem> updateViolationArea(
            @PathVariable Long id,
            @RequestBody BizManagementService.UpdateViolationAreaRequest request) {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.API_BIZ_VIOLATION_AREA_UPDATE);
        return ApiResponse.ok(bizManagementService.updateViolationArea(id, request));
    }

    /**
     * @Author lxy
     * @Description //删除指定违规区域，需要 API_BIZ_VIOLATION_AREA_DELETE 权限
     * @Date 2026/04/18 10:15
     * @Param [id 违规区域ID]
     * @return ApiResponse<Void> 删除成功响应
     */
    @DeleteMapping("/violation-areas/{id}")
    public ApiResponse<Void> deleteViolationArea(@PathVariable Long id) {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.API_BIZ_VIOLATION_AREA_DELETE);
        bizManagementService.deleteViolationArea(id);
        return ApiResponse.ok(null);
    }
}
