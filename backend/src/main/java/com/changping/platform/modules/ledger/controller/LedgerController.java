package com.changping.platform.modules.ledger.controller;

import com.changping.platform.common.response.ApiResponse;
import com.changping.platform.modules.auth.security.PermissionCodes;
import com.changping.platform.modules.auth.security.PermissionGuard;
import com.changping.platform.modules.auth.service.AuthService;
import com.changping.platform.modules.auth.service.CurrentUserService;
import com.changping.platform.modules.ledger.entity.LedgerTemplateEntity;
import com.changping.platform.modules.ledger.service.LedgerService;
import com.changping.platform.modules.community.service.ExportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

@RestController
@RequestMapping("/ledger")
public class LedgerController {

    private final LedgerService ledgerService;
    private final ExportService exportService;
    private final CurrentUserService currentUserService;
    private final PermissionGuard permissionGuard;

    public LedgerController(
            LedgerService ledgerService,
            ExportService exportService,
            CurrentUserService currentUserService,
            PermissionGuard permissionGuard) {
        this.ledgerService = ledgerService;
        this.exportService = exportService;
        this.currentUserService = currentUserService;
        this.permissionGuard = permissionGuard;
    }

    @GetMapping("/templates")
    public ApiResponse<List<LedgerTemplateEntity>> getTemplates() {
        requireLedgerPermission();
        return ApiResponse.ok(ledgerService.getAllTemplates());
    }

    @GetMapping("/templates/{type}")
    public ApiResponse<List<LedgerTemplateEntity>> getTemplatesByType(@PathVariable String type) {
        requireLedgerPermission();
        return ApiResponse.ok(ledgerService.getTemplatesByType(type));
    }

    @PostMapping("/templates")
    public ApiResponse<Boolean> saveTemplate(@RequestBody LedgerTemplateEntity entity) {
        requireLedgerPermission();
        ledgerService.saveTemplate(entity);
        return ApiResponse.ok(true);
    }

    @DeleteMapping("/templates/{id}")
    public ApiResponse<Boolean> deleteTemplate(@PathVariable Long id) {
        requireLedgerPermission();
        ledgerService.deleteTemplate(id);
        return ApiResponse.ok(true);
    }

    @GetMapping("/data/{type}")
    public ApiResponse<List<Map<String, Object>>> getData(@PathVariable String type,
            @RequestParam(required = false) String gridId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        requireLedgerPermission();
        Map<String, String> filters = new HashMap<>();
        if (gridId != null) filters.put("gridId", gridId);
        if (status != null) filters.put("status", status);
        if (startDate != null) filters.put("startDate", startDate);
        if (endDate != null) filters.put("endDate", endDate);
        return ApiResponse.ok(ledgerService.getLedgerData(type, filters));
    }

    @GetMapping("/export/{type}")
    public ResponseEntity<byte[]> exportLedger(@PathVariable String type,
            @RequestParam(required = false) String templateId,
            @RequestParam(required = false) String gridId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) throws Exception {
        requireLedgerPermission();
        Map<String, String> filters = new HashMap<>();
        if (gridId != null) filters.put("gridId", gridId);
        if (status != null) filters.put("status", status);
        if (startDate != null) filters.put("startDate", startDate);
        if (endDate != null) filters.put("endDate", endDate);
        List<Map<String, Object>> data = ledgerService.getLedgerData(type, filters);

        // 按台账类型定义列(字段名 + 中文表头),与 LedgerService 各 SQL 的 SELECT 列一一对应
        String[][] def = TYPE_COLUMNS.getOrDefault(type, new String[][]{
                {"name", "名称"}, {"phone", "电话"}, {"address", "地址"}, {"status", "状态"}, {"created_at", "创建时间"}});
        List<String> fields = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        for (String[] pair : def) {
            fields.add(pair[0]);
            labels.add(pair[1]);
        }
        String sheetName = TYPE_NAMES.getOrDefault(type, type) + "台账";

        byte[] excel = exportService.exportLedger(sheetName, fields, labels, translateValues(data));

        String fileName = URLEncoder.encode(sheetName + ".xlsx", StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + fileName)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(excel);
    }

    /** 台账类型中文名 */
    private static final Map<String, String> TYPE_NAMES = Map.of(
            "EVENT", "事件", "POPULATION", "人口", "BUILDING", "房屋",
            "MERCHANT", "商户", "PATROL", "巡查");

    /** 各台账类型的导出列:{字段名, 中文表头} */
    private static final Map<String, String[][]> TYPE_COLUMNS = Map.of(
            "EVENT", new String[][]{
                    {"event_code", "事件编号"}, {"title", "标题"}, {"event_type", "事件类型"},
                    {"report_source", "来源"}, {"status", "状态"}, {"urgency_level", "紧急程度"},
                    {"grid_name", "网格"}, {"incident_address", "事发地址"},
                    {"occurred_at", "发生时间"}, {"created_at", "创建时间"}},
            "POPULATION", new String[][]{
                    {"name", "姓名"}, {"phone", "联系电话"}, {"household_type", "户籍类型"},
                    {"address", "居住地址"}, {"grid_name", "网格"}, {"created_at", "创建时间"}},
            "BUILDING", new String[][]{
                    {"building_no", "楼栋号"}, {"address", "地址"}, {"landlord_name", "房东姓名"},
                    {"landlord_phone", "房东电话"}, {"fire_risk_level", "消防风险等级"},
                    {"grid_name", "网格"}, {"created_at", "创建时间"}},
            "MERCHANT", new String[][]{
                    {"merchant_name", "商户名称"}, {"legal_person_name", "法人姓名"},
                    {"legal_person_phone", "法人电话"}, {"remark", "备注"}, {"status", "状态"},
                    {"fire_risk_level", "消防风险等级"}, {"grid_name", "网格"}, {"created_at", "创建时间"}},
            "PATROL", new String[][]{
                    {"grid_name", "网格"}, {"patrol_type", "巡查类型"}, {"content", "巡查内容"},
                    {"status", "状态"}, {"created_at", "创建时间"}});

    /** 导出台账枚举值→中文映射(仅命中字典才翻译,否则保留原值) */
    private static final Map<String, Map<String, String>> VALUE_LABELS = Map.of(
            "event_type", Map.ofEntries(
                    Map.entry("ROAD", "道路损坏"), Map.entry("LIGHT", "路灯故障"),
                    Map.entry("PIPE", "管道破损"), Map.entry("ENV", "环境卫生"),
                    Map.entry("ENVIRONMENT", "环境卫生"), Map.entry("SAFE", "安全隐患"),
                    Map.entry("SAFETY", "安全隐患"), Map.entry("NOISE", "噪音扰民"),
                    Map.entry("OTHER", "其他问题"), Map.entry("FIRE", "消防安全"),
                    Map.entry("FIRE_SAFETY", "消防安全"), Map.entry("ILLEGAL_BUILDING", "违章建筑"),
                    Map.entry("PUBLIC_SAFETY", "公共安全"), Map.entry("DRONE_ALARM", "无人机告警"),
                    Map.entry("COMPLAINT", "市民投诉"), Map.entry("REPAIR", "物业报修"),
                    Map.entry("HEALTH", "卫生事件"), Map.entry("LOW_INCOME", "低保户")),
            "urgency_level", Map.of("GREEN", "绿色(一般)", "YELLOW", "黄色(重点)", "RED", "红色(紧急)"),
            "status", Map.ofEntries(
                    Map.entry("PENDING_AUDIT", "待审核"), Map.entry("IN_AUDIT", "审核中"),
                    Map.entry("WAITING_DISPATCH", "待派单"), Map.entry("DISPATCHED_TO_WORK_ORDER", "已转工单"),
                    Map.entry("PROCESSING", "处理中"), Map.entry("PENDING_REVIEW", "待复核"),
                    Map.entry("COMPLETED", "已办结"), Map.entry("CLOSED", "已办结"),
                    Map.entry("ARCHIVED", "已归档"), Map.entry("IGNORED", "已忽略"),
                    Map.entry("NORMAL", "正常"), Map.entry("PENDING", "待处理")),
            "report_source", Map.of(
                    "RESIDENT", "居民上报", "GRID_MEMBER", "网格员", "12345", "12345 热线",
                    "AI_CAMERA", "智能摄像头", "H5_APP", "移动端", "WEB", "管理端", "H5", "移动端"),
            "patrol_type", Map.of("NORMAL", "日常巡查", "GRID", "网格巡查", "SPECIAL", "专项巡查"),
            "household_type", Map.of("LOCAL", "本地户籍", "FLOATING", "流动人口"),
            "fire_risk_level", Map.of("LOW", "低", "MEDIUM", "中", "HIGH", "高"));

    /** 把导出数据中的枚举值翻译为中文 */
    private static List<Map<String, Object>> translateValues(List<Map<String, Object>> data) {
        for (Map<String, Object> row : data) {
            for (Map.Entry<String, Map<String, String>> entry : VALUE_LABELS.entrySet()) {
                Object raw = row.get(entry.getKey());
                if (raw == null) continue;
                String label = entry.getValue().get(String.valueOf(raw));
                if (label != null) {
                    row.put(entry.getKey(), label);
                }
            }
        }
        return data;
    }

    private void requireLedgerPermission() {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.MENU_BIZ_LEDGER);
    }
}
