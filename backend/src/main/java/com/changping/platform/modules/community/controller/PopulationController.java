package com.changping.platform.modules.community.controller;

import com.changping.platform.common.response.ApiResponse;
import com.changping.platform.modules.auth.security.PermissionCodes;
import com.changping.platform.modules.auth.security.PermissionGuard;
import com.changping.platform.modules.auth.service.AuthService;
import com.changping.platform.modules.auth.service.CurrentUserService;
import com.changping.platform.modules.community.entity.PopulationEntity;
import com.changping.platform.modules.community.service.PopulationService;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/community/population")
public class PopulationController {

    private final PopulationService populationService;
    private final CurrentUserService currentUserService;
    private final PermissionGuard permissionGuard;

    public PopulationController(
            PopulationService populationService,
            CurrentUserService currentUserService,
            PermissionGuard permissionGuard) {
        this.populationService = populationService;
        this.currentUserService = currentUserService;
        this.permissionGuard = permissionGuard;
    }

    @GetMapping
    public ApiResponse<List<PopulationEntity>> list(@RequestParam(required = false) Long gridId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String householdType) {
        requirePopulationPermission();
        // 带搜索条件时走台账条件查询（模糊搜索 + 户籍类型/网格筛选）
        if ((keyword != null && !keyword.isBlank()) || (householdType != null && !householdType.isBlank()) || gridId != null) {
            return ApiResponse.ok(populationService.search(keyword, householdType, gridId));
        }
        return ApiResponse.ok(populationService.list(null));
    }

    /** 户籍类型枚举→中文（与 web 端 HOUSEHOLD_TYPES 映射保持一致） */
    private static final Map<String, String> HOUSEHOLD_LABELS = Map.of(
            "LOCAL", "本地户籍", "NON_LOCAL", "外地户籍", "FLOATING", "流动人口",
            "LOW_INCOME", "低保户", "SPECIAL_CARE", "优抚对象", "OTHER", "其他");

    /**
     * 导出实有人口台账 Excel（与社区提供表格格式对齐：全字段列）
     * 支持按户籍类型、网格筛选导出
     */
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportPopulation(@RequestParam(required = false) String keyword,
            @RequestParam(required = false) String householdType,
            @RequestParam(required = false) Long gridId) throws Exception {
        requirePopulationPermission();
        List<PopulationEntity> rows = populationService.search(keyword, householdType, gridId);

        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("实有人口台账");
            CellStyle headerStyle = wb.createCellStyle();
            Font font = wb.createFont();
            font.setBold(true);
            headerStyle.setFont(font);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            String[] headers = {"序号", "姓名", "性别", "出生日期", "身份证号", "联系电话", "户籍类型", "居住地址", "楼栋/房号", "所属网格", "备注", "登记时间"};
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell c = headerRow.createCell(i);
                c.setCellValue(headers[i]);
                c.setCellStyle(headerStyle);
            }
            int rn = 1;
            for (PopulationEntity p : rows) {
                Row row = sheet.createRow(rn);
                row.createCell(0).setCellValue(rn);
                row.createCell(1).setCellValue(nvl(p.getName()));
                row.createCell(2).setCellValue(nvl(p.getGender()));
                row.createCell(3).setCellValue(p.getBirthday() != null ? p.getBirthday().toString() : "");
                row.createCell(4).setCellValue(nvl(p.getIdCard()));
                row.createCell(5).setCellValue(nvl(p.getPhone()));
                row.createCell(6).setCellValue(p.getHouseholdType() != null
                        ? HOUSEHOLD_LABELS.getOrDefault(p.getHouseholdType(), p.getHouseholdType()) : "");
                row.createCell(7).setCellValue(nvl(p.getAddress()));
                String room = (nvl(p.getBuildingNo()) + (p.getRoomNo() != null ? "-" + p.getRoomNo() : "")).trim();
                row.createCell(8).setCellValue(room.isEmpty() ? "-" : room);
                row.createCell(9).setCellValue(nvl(p.getGridName()));
                row.createCell(10).setCellValue(nvl(p.getRemark()));
                row.createCell(11).setCellValue(p.getCreatedAt() != null ? p.getCreatedAt().toString().replace('T', ' ') : "");
                rn++;
            }
            for (int i = 0; i < headers.length; i++) sheet.autoSizeColumn(i);
            wb.write(bos);

            String fileName = URLEncoder.encode("实有人口台账.xlsx", StandardCharsets.UTF_8).replace("+", "%20");
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + fileName)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(bos.toByteArray());
        }
    }

    private static String nvl(String s) {
        return s == null ? "" : s;
    }

    @GetMapping("/{id}")
    public ApiResponse<PopulationEntity> detail(@PathVariable Long id) {
        requirePopulationPermission();
        return ApiResponse.ok(populationService.detail(id));
    }

    @PostMapping
    public ApiResponse<Boolean> create(@RequestBody PopulationEntity entity) {
        requirePopulationPermission();
        return ApiResponse.ok(populationService.create(entity));
    }

    @PutMapping("/{id}")
    public ApiResponse<Boolean> update(@PathVariable Long id, @RequestBody PopulationEntity entity) {
        requirePopulationPermission();
        entity.setId(id);
        return ApiResponse.ok(populationService.update(entity));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Boolean> delete(@PathVariable Long id) {
        requirePopulationPermission();
        return ApiResponse.ok(populationService.delete(id));
    }

    private void requirePopulationPermission() {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.MENU_COMMUNITY_POPULATION);
    }
}
