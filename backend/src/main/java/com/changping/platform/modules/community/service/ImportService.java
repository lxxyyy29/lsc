package com.changping.platform.modules.community.service;

import com.changping.platform.common.exception.BusinessException;
import com.changping.platform.modules.community.entity.BuildingEntity;
import com.changping.platform.modules.community.entity.PlaceEntity;
import com.changping.platform.modules.community.entity.PopulationEntity;
import com.changping.platform.modules.community.mapper.BuildingMapper;
import com.changping.platform.modules.community.mapper.PlaceMapper;
import com.changping.platform.modules.community.mapper.PopulationMapper;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.*;

@Service
public class ImportService {

    private final PopulationMapper populationMapper;
    private final BuildingMapper buildingMapper;
    private final PlaceMapper placeMapper;
    private final JdbcTemplate jdbcTemplate;

    public ImportService(PopulationMapper populationMapper, BuildingMapper buildingMapper,
                         PlaceMapper placeMapper, JdbcTemplate jdbcTemplate) {
        this.populationMapper = populationMapper;
        this.buildingMapper = buildingMapper;
        this.placeMapper = placeMapper;
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 预览导入数据（解析前 N 行，不写入 DB）
     */
    public Map<String, Object> previewImport(String type, MultipartFile file, int previewRows) {
        try (InputStream is = file.getInputStream(); Workbook wb = new XSSFWorkbook(is)) {
            Sheet sheet = wb.getSheetAt(0);
            return switch (type) {
                case "population" -> previewPopulation(sheet, previewRows);
                case "buildings" -> previewBuildings(sheet, previewRows);
                case "places" -> previewPlaces(sheet, previewRows);
                default -> throw new BusinessException("IMPORT_TYPE_INVALID", "不支持的导入类型: " + type);
            };
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("IMPORT_PARSE_FAILED", "文件解析失败: " + e.getMessage());
        }
    }

    /**
     * 执行导入
     */
    public Map<String, Object> executeImport(String type, MultipartFile file) {
        try (InputStream is = file.getInputStream(); Workbook wb = new XSSFWorkbook(is)) {
            Sheet sheet = wb.getSheetAt(0);
            return switch (type) {
                case "population" -> importPopulation(sheet);
                case "buildings" -> importBuildings(sheet);
                case "places" -> importPlaces(sheet);
                default -> throw new BusinessException("IMPORT_TYPE_INVALID", "不支持的导入类型: " + type);
            };
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("IMPORT_FAILED", "导入失败: " + e.getMessage());
        }
    }

    // ==================== 实有人口 ====================

    private Map<String, Object> previewPopulation(Sheet sheet, int previewRows) {
        List<Map<String, Object>> rows = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        int total = 0;

        int headerRow = findPopulationHeaderRow(sheet);
        if (headerRow < 0) {
            throw new BusinessException("IMPORT_HEADER_NOT_FOUND",
                    "未识别到表头列，请确认 Excel 包含：序号/队别/户主/姓名/年龄/性别/住址/手机/与户主关系/备注");
        }

        for (int i = headerRow + 1; i <= sheet.getLastRowNum() && total < previewRows; i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;
            total++;
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("row", i);
            data.put("gridName", getCellString(row, 1));
            data.put("householdFlag", getCellString(row, 2));
            data.put("name", getCellString(row, 3));
            data.put("age", getCellString(row, 4));
            data.put("gender", getCellString(row, 5));
            data.put("address", getCellString(row, 6));
            data.put("phone", getCellString(row, 7));
            data.put("relation", getCellString(row, 8));
            data.put("remark", getCellString(row, 9));

            String err = validatePopulationRow(data);
            if (err != null) {
                data.put("error", err);
                errors.add("第" + i + "行: " + err);
            }
            rows.add(data);
        }

        return buildResult("population", rows, errors, sheet.getLastRowNum(), total);
    }

    private Map<String, Object> importPopulation(Sheet sheet) {
        int success = 0;
        int fail = 0;
        List<String> errorList = new ArrayList<>();

        int headerRow = findPopulationHeaderRow(sheet);
        if (headerRow < 0) {
            throw new BusinessException("IMPORT_HEADER_NOT_FOUND",
                    "未识别到表头列，请确认 Excel 包含：序号/队别/户主/姓名/年龄/性别/住址/手机/与户主关系/备注");
        }

        for (int i = headerRow + 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;
            try {
                // 新模板：序号|队别|户主|姓名|年龄|性别|住址|手机|与户主关系|备注
                String name = getCellString(row, 3);
                if (name.isBlank()) {
                    fail++;
                    errorList.add("第" + i + "行: 姓名为必填项");
                    continue;
                }

                String address = getCellString(row, 6);
                // 重复检测：姓名+地址（屋主通讯录无身份证号）
                if (!address.isBlank() && personExists(name, address)) {
                    fail++;
                    errorList.add("第" + i + "行: 相同姓名+地址已存在");
                    continue;
                }

                PopulationEntity entity = new PopulationEntity();
                entity.setName(name);
                entity.setPhone(getCellString(row, 7));
                entity.setAddress(address);
                entity.setGender(getCellString(row, 5));

                // 年龄转换（去除非数字字符后解析）
                String ageStr = getCellString(row, 4);
                if (!ageStr.isBlank()) {
                    try {
                        entity.setAge(Integer.parseInt(ageStr.replaceAll("[^0-9]", "")));
                    } catch (NumberFormatException ignored) {
                        // 年龄格式异常时忽略，不阻断导入
                    }
                }

                // 户主标记：户主列填"户主"则 relation=户主；否则用"与户主关系"列
                String householdFlag = getCellString(row, 2);
                String relation = "户主".equals(householdFlag) ? "户主" : getCellString(row, 8);
                entity.setRelation(relation);

                entity.setRemark(getCellString(row, 9));
                entity.setStatus("ACTIVE");
                // special_population 列为 NOT NULL DEFAULT 0，导入时显式置 0 避免插入失败
                entity.setSpecialPopulation(0);

                // 根据队别查找 grid_id
                String gridName = getCellString(row, 1);
                Long gridId = findGridIdByName(gridName);
                entity.setGridId(gridId);

                populationMapper.insert(entity);
                success++;
            } catch (Exception e) {
                fail++;
                errorList.add("第" + i + "行: " + e.getMessage());
            }
        }

        return Map.of("success", success, "fail", fail, "errors", errorList);
    }

    private String validatePopulationRow(Map<String, Object> data) {
        if (data.get("name") == null || data.get("name").toString().isBlank()) return "姓名不能为空";
        return null;
    }

    // ==================== 房屋 ====================

    private Map<String, Object> previewBuildings(Sheet sheet, int previewRows) {
        List<Map<String, Object>> rows = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        int total = 0;

        for (int i = 1; i <= sheet.getLastRowNum() && total < previewRows; i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;
            total++;
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("row", i);
            data.put("buildingNo", getCellString(row, 0));
            data.put("address", getCellString(row, 1));
            data.put("landlordName", getCellString(row, 2));
            data.put("landlordPhone", getCellString(row, 3));
            data.put("fireRiskLevel", getCellString(row, 4));
            data.put("isGroupRental", getCellString(row, 5));
            data.put("gridName", getCellString(row, 6));

            if (data.get("buildingNo") == null || data.get("buildingNo").toString().isBlank()) {
                data.put("error", "楼栋编号不能为空");
                errors.add("第" + i + "行: 楼栋编号不能为空");
            }
            rows.add(data);
        }

        return buildResult("buildings", rows, errors, sheet.getLastRowNum(), total);
    }

    private Map<String, Object> importBuildings(Sheet sheet) {
        int success = 0;
        int fail = 0;
        List<String> errorList = new ArrayList<>();

        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;
            try {
                String buildingNo = getCellString(row, 0);
                String address = getCellString(row, 1);
                if (buildingNo.isBlank()) {
                    fail++;
                    errorList.add("第" + i + "行: 楼栋编号不能为空");
                    continue;
                }

                BuildingEntity entity = new BuildingEntity();
                entity.setBuildingNo(buildingNo);
                entity.setAddress(address);
                entity.setLandlordName(getCellString(row, 2));
                entity.setLandlordPhone(getCellString(row, 3));
                entity.setFireRiskLevel(getCellString(row, 4));
                String groupRental = getCellString(row, 5);
                entity.setIsGroupRental("是".equals(groupRental) || "1".equals(groupRental) ? 1 : 0);
                entity.setStatus("ACTIVE");

                String gridName = getCellString(row, 6);
                Long gridId = findGridIdByName(gridName);
                entity.setGridId(gridId);

                buildingMapper.insert(entity);
                success++;
            } catch (Exception e) {
                fail++;
                errorList.add("第" + i + "行: " + e.getMessage());
            }
        }

        return Map.of("success", success, "fail", fail, "errors", errorList);
    }

    // ==================== 场所 ====================

    private Map<String, Object> previewPlaces(Sheet sheet, int previewRows) {
        List<Map<String, Object>> rows = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        int total = 0;

        for (int i = 1; i <= sheet.getLastRowNum() && total < previewRows; i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;
            total++;
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("row", i);
            data.put("placeName", getCellString(row, 0));
            data.put("contactName", getCellString(row, 1));
            data.put("contactPhone", getCellString(row, 2));
            data.put("address", getCellString(row, 3));
            data.put("remark", getCellString(row, 4));
            data.put("gridName", getCellString(row, 5));

            if (data.get("placeName") == null || data.get("placeName").toString().isBlank()) {
                data.put("error", "场所名称不能为空");
                errors.add("第" + i + "行: 场所名称不能为空");
            }
            rows.add(data);
        }

        return buildResult("places", rows, errors, sheet.getLastRowNum(), total);
    }

    private Map<String, Object> importPlaces(Sheet sheet) {
        int success = 0;
        int fail = 0;
        List<String> errorList = new ArrayList<>();

        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;
            try {
                String placeName = getCellString(row, 0);
                if (placeName.isBlank()) {
                    fail++;
                    errorList.add("第" + i + "行: 场所名称不能为空");
                    continue;
                }

                PlaceEntity entity = new PlaceEntity();
                entity.setPlaceName(placeName);
                entity.setContactName(getCellString(row, 1));
                entity.setContactPhone(getCellString(row, 2));
                entity.setAddress(getCellString(row, 3));
                entity.setRemark(getCellString(row, 4));
                entity.setStatus("ACTIVE");

                String gridName = getCellString(row, 5);
                Long gridId = findGridIdByName(gridName);
                entity.setGridId(gridId);

                placeMapper.insert(entity);
                success++;
            } catch (Exception e) {
                fail++;
                errorList.add("第" + i + "行: " + e.getMessage());
            }
        }

        return Map.of("success", success, "fail", fail, "errors", errorList);
    }

    // ==================== 工具方法 ====================

    /**
     * 定位人口通讯录表头行：Excel 顶部可能有标题行/合并大标题，逐行扫描前若干行，
     * 找到包含 队别/姓名/手机 的表头行返回其行号；未找到返回 -1。
     */
    private int findPopulationHeaderRow(Sheet sheet) {
        int scanEnd = Math.min(sheet.getLastRowNum(), 10);
        for (int r = 0; r <= scanEnd; r++) {
            Row row = sheet.getRow(r);
            if (row == null) continue;
            String teamCol = getCellString(row, 1);
            String nameCol = getCellString(row, 3);
            String phoneCol = getCellString(row, 7);
            if ("队别".equals(teamCol) && "姓名".equals(nameCol) && "手机".equals(phoneCol)) {
                return r;
            }
        }
        return -1;
    }

    private String getCellString(Row row, int col) {
        Cell cell = row.getCell(col);
        if (cell == null) return "";
        DataFormatter formatter = new DataFormatter();
        String value = formatter.formatCellValue(cell);
        // 通讯录列宽对齐常在单元格内引入各类空白字符（ASCII/全角/不间断/细空格等），
        // 按 Unicode 空白类与常用空格码点逐一剔除，避免影响队别网格匹配与姓名查重
        StringBuilder sb = new StringBuilder(value.length());
        for (int k = 0; k < value.length(); k++) {
            char ch = value.charAt(k);
            if (ch <= ' ' || ch == 0x00A0 || ch == 0x1680 || (ch >= 0x2000 && ch <= 0x200B)
                    || ch == 0x2028 || ch == 0x2029 || ch == 0x202F || ch == 0x205F
                    || ch == 0x3000 || ch == 0xFEFF || Character.isWhitespace(ch)) {
                continue;
            }
            sb.append(ch);
        }
        return sb.toString();
    }

    private boolean idCardExists(String idCard) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM cmn_population WHERE id_card = ? AND status = 'ACTIVE'",
                Long.class, idCard);
        return count != null && count > 0;
    }

    /**
     * 屋主通讯录查重：姓名+地址（无身份证号场景）
     */
    private boolean personExists(String name, String address) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM cmn_population WHERE name = ? AND address = ? AND status = 'ACTIVE'",
                Long.class, name, address);
        return count != null && count > 0;
    }

    private Long findGridIdByName(String gridName) {
        if (gridName == null || gridName.isBlank()) return null;
        try {
            return jdbcTemplate.queryForObject(
                    "SELECT id FROM cmn_grid WHERE grid_name = ? LIMIT 1",
                    Long.class, gridName);
        } catch (Exception e) {
            return null;
        }
    }

    private Map<String, Object> buildResult(String type, List<Map<String, Object>> rows,
                                             List<String> errors, int totalRows, int previewed) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("type", type);
        result.put("rows", rows);
        result.put("totalRows", totalRows);
        result.put("previewed", previewed);
        result.put("errorCount", errors.size());
        result.put("errors", errors);
        return result;
    }
}
