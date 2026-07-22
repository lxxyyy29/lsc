package com.changping.platform.modules.community.service;

import com.changping.platform.modules.community.mapper.ExportMapper;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;

@Service
public class ExportService {

    private final ExportMapper exportMapper;

    public ExportService(ExportMapper exportMapper) {
        this.exportMapper = exportMapper;
    }

    /**
     * 导出事件台账为 Excel
     */
    public byte[] exportEventLedger() throws Exception {
        List<Map<String, Object>> events = exportMapper.getEventLedger();

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("事件台账");

            // 创建表头样式
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            // 表头
            String[] headers = {"事件编号", "标题", "类型", "来源", "状态", "紧急程度", "网格", "事发地址", "发生时间", "创建时间"};
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // 数据行
            int rowNum = 1;
            for (Map<String, Object> event : events) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(getString(event, "event_code"));
                row.createCell(1).setCellValue(getString(event, "title"));
                row.createCell(2).setCellValue(getString(event, "event_type"));
                row.createCell(3).setCellValue(getString(event, "report_source"));
                row.createCell(4).setCellValue(getString(event, "status"));
                row.createCell(5).setCellValue(getString(event, "urgency_level"));
                row.createCell(6).setCellValue(getString(event, "grid_name"));
                row.createCell(7).setCellValue(getString(event, "incident_address"));
                row.createCell(8).setCellValue(getString(event, "occurred_at"));
                row.createCell(9).setCellValue(getString(event, "created_at"));
            }

            // 自动调整列宽
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(bos);
            return bos.toByteArray();
        }
    }

    /**
     * 导出台账（通用）
     */
    public byte[] exportLedger(String sheetName, List<String> columns, List<Map<String, Object>> data) throws Exception {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet(sheetName);

            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < columns.size(); i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns.get(i));
                cell.setCellStyle(headerStyle);
            }

            int rowNum = 1;
            for (Map<String, Object> rowData : data) {
                Row row = sheet.createRow(rowNum++);
                for (int i = 0; i < columns.size(); i++) {
                    row.createCell(i).setCellValue(getString(rowData, columns.get(i)));
                }
            }

            for (int i = 0; i < columns.size(); i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(bos);
            return bos.toByteArray();
        }
    }

    private String getString(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return val != null ? val.toString() : "";
    }
}
