package com.changping.platform.modules.report.controller;

import com.changping.platform.common.response.ApiResponse;
import com.changping.platform.modules.auth.security.PermissionCodes;
import com.changping.platform.modules.auth.security.PermissionGuard;
import com.changping.platform.modules.auth.service.AuthService;
import com.changping.platform.modules.auth.service.CurrentUserService;
import com.changping.platform.modules.report.service.DistrictReportService;
import com.changping.platform.modules.report.service.DistrictSummaryVo;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author tangxinglin
 * @Description //区域报告控制器，提供按区域汇总的事件和工单统计数据查询及导出接口，仅限Web端用户操作
 * @Date 2026/04/18 10:00
 */
@RestController
@RequestMapping("/reports")
public class DistrictReportController {

    private final DistrictReportService districtReportService;
    private final CurrentUserService currentUserService;
    private final PermissionGuard permissionGuard;

    /**
     * @Author tangxinglin
     * @Description //构造器，注入区域报告服务、当前用户服务和权限守卫
     * @Date 2026/04/18 10:00
     * @Param [districtReportService 区域报告服务, currentUserService 当前用户服务, permissionGuard 权限守卫]
     * @return void
     */
    public DistrictReportController(
            DistrictReportService districtReportService,
            CurrentUserService currentUserService,
            PermissionGuard permissionGuard) {
        this.districtReportService = districtReportService;
        this.currentUserService = currentUserService;
        this.permissionGuard = permissionGuard;
    }

    /**
     * @Author tangxinglin
     * @Description //查询区域汇总报告，按区域聚合事件和工单统计数据，仅限Web端用户操作
     * @Date 2026/04/18 10:00
     * @Param [startDate 开始日期（可选，ISO格式）, endDate 结束日期（可选，ISO格式）, areaId 区域ID（可选）]
     * @return ApiResponse<List<DistrictSummaryVo>> 区域汇总统计列表
     */
    @GetMapping("/district-summary")
    public ApiResponse<List<DistrictSummaryVo>> getDistrictSummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long areaId) {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.API_REPORT_DISTRICT);
        return ApiResponse.ok(districtReportService.getDistrictSummary(startDate, endDate, areaId));
    }

    /**
     * @Author tangxinglin
     * @Description //导出区域汇总报告（当前与查询接口返回相同数据），仅限Web端用户操作
     * @Date 2026/04/18 10:00
     * @Param [startDate 开始日期（可选，ISO格式）, endDate 结束日期（可选，ISO格式）, areaId 区域ID（可选）]
     * @return ApiResponse<List<DistrictSummaryVo>> 区域汇总统计列表
     */
    @GetMapping("/district-summary/export")
    public ApiResponse<List<DistrictSummaryVo>> exportDistrictSummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long areaId) {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.API_REPORT_DISTRICT);
        return ApiResponse.ok(districtReportService.getDistrictSummary(startDate, endDate, areaId));
    }
}
