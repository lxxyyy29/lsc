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

        for (int i = 1; i <= sheet.getLastRowNum() && total < previewRows; i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;
            total++;
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("row", i);
            data.put("name", getCellString(row, 0));
            data.put("idCard", getCellString(row, 1));
            data.put("phone", getCellString(row, 2));
            data.put("householdType", getCellString(row, 3));
            data.put("address", getCellString(row, 4));
            data.put("gridName", getCellString(row, 5));
            data.put("tags", getCellString(row, 6));

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

        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;
            try {
                String name = getCellString(row, 0);
                String idCard = getCellString(row, 1);
                String phone = getCellString(row, 2);
                if (name.isBlank() || idCard.isBlank() || phone.isBlank()) {
                    fail++;
                    errorList.add("第" + i + "行: 必填项为空");
                    continue;
                }

                // 重复检测
                if (idCardExists(idCard)) {
                    fail++;
                    errorList.add("第" + i + "行: 身份证号已存在");
                    continue;
                }

                PopulationEntity entity = new PopulationEntity();
                entity.setName(name);
                entity.setIdCard(idCard);
                entity.setPhone(phone);
                entity.setHouseholdType(getCellString(row, 3));
                entity.setAddress(getCellString(row, 4));
                entity.setTags(getCellString(row, 6));
                entity.setStatus("ACTIVE");

                // 根据网格名查找 grid_id
                String gridName = getCellString(row, 5);
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
        if (data.get("idCard") == null || data.get("idCard").toString().isBlank()) return "身份证号不能为空";
        if (data.get("phone") == null || data.get("phone").toString().isBlank()) return "手机号不能为空";
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

    private String getCellString(Row row, int col) {
        Cell cell = row.getCell(col);
        if (cell == null) return "";
        DataFormatter formatter = new DataFormatter();
        return formatter.formatCellValue(cell).trim();
    }

    private boolean idCardExists(String idCard) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM cmn_population WHERE id_card = ? AND status = 'ACTIVE'",
                Long.class, idCard);
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
