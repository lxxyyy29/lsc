package com.changping.platform.modules.community.service;

import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.awt.Color;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;

@Service
public class PdfExportService {

    /**
     * 导出事件台账 PDF
     */
    public byte[] exportEventPdf(List<Map<String, Object>> events) throws Exception {
        Document document = new Document(PageSize.A4.rotate(), 20, 20, 30, 20);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, bos);

        document.open();

        // 标题
        Font titleFont = new Font(Font.HELVETICA, 16, Font.BOLD);
        Paragraph title = new Paragraph("事件台账报告", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(20);
        document.add(title);

        // 统计信息
        Font infoFont = new Font(Font.HELVETICA, 10, Font.NORMAL);
        document.add(new Paragraph("导出时间: " + java.time.LocalDateTime.now().toString(), infoFont));
        document.add(new Paragraph("事件总数: " + events.size(), infoFont));
        document.add(new Paragraph(" ", infoFont));

        // 表格
        PdfPTable table = new PdfPTable(6);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{2, 3, 2, 2, 2, 3});

        // 表头
        String[] headers = {"事件编号", "标题", "类型", "状态", "紧急程度", "创建时间"};
        for (String h : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(h, new Font(Font.HELVETICA, 9, Font.BOLD)));
            cell.setBackgroundColor(new Color(220, 220, 220));
            cell.setPadding(5);
            table.addCell(cell);
        }

        // 数据行
        for (Map<String, Object> event : events) {
            table.addCell(new Phrase(getString(event, "event_code"), infoFont));
            table.addCell(new Phrase(getString(event, "title"), infoFont));
            table.addCell(new Phrase(getString(event, "event_type"), infoFont));
            table.addCell(new Phrase(getString(event, "status"), infoFont));
            table.addCell(new Phrase(getString(event, "urgency_level"), infoFont));
            table.addCell(new Phrase(getString(event, "created_at"), infoFont));
        }

        document.add(table);
        document.close();
        return bos.toByteArray();
    }

    /**
     * 导出工单台账 PDF
     */
    public byte[] exportWorkOrderPdf(List<Map<String, Object>> workOrders) throws Exception {
        Document document = new Document(PageSize.A4.rotate(), 20, 20, 30, 20);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, bos);

        document.open();

        Font titleFont = new Font(Font.HELVETICA, 16, Font.BOLD);
        Paragraph title = new Paragraph("工单台账报告", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(20);
        document.add(title);

        Font infoFont = new Font(Font.HELVETICA, 10, Font.NORMAL);
        document.add(new Paragraph("导出时间: " + java.time.LocalDateTime.now().toString(), infoFont));
        document.add(new Paragraph("工单总数: " + workOrders.size(), infoFont));
        document.add(new Paragraph(" ", infoFont));

        PdfPTable table = new PdfPTable(6);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{2, 2, 2, 2, 2, 3});

        String[] headers = {"工单编号", "状态", "网格员", "派发人", "紧急程度", "创建时间"};
        for (String h : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(h, new Font(Font.HELVETICA, 9, Font.BOLD)));
            cell.setBackgroundColor(new Color(220, 220, 220));
            cell.setPadding(5);
            table.addCell(cell);
        }

        for (Map<String, Object> wo : workOrders) {
            table.addCell(new Phrase(getString(wo, "work_order_no"), infoFont));
            table.addCell(new Phrase(getString(wo, "status"), infoFont));
            table.addCell(new Phrase(getString(wo, "assignee_name"), infoFont));
            table.addCell(new Phrase(getString(wo, "dispatcher_name"), infoFont));
            table.addCell(new Phrase(getString(wo, "urgency_level"), infoFont));
            table.addCell(new Phrase(getString(wo, "created_at"), infoFont));
        }

        document.add(table);
        document.close();
        return bos.toByteArray();
    }

    private String getString(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return val != null ? val.toString() : "";
    }
}
