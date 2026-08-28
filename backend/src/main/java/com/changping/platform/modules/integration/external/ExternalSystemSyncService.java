package com.changping.platform.modules.integration.external;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * 外部系统数据同步服务
 */
@Service
public class ExternalSystemSyncService {

    private static final Logger log = LoggerFactory.getLogger(ExternalSystemSyncService.class);
    private final JdbcTemplate jdbcTemplate;
    private final Random random = new Random();

    public ExternalSystemSyncService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 同步指定外部系统的数据
     */
    public Map<String, Object> syncSystem(String systemCode) {
        log.info("开始同步外部系统: {}", systemCode);
        LocalDateTime startedAt = LocalDateTime.now();

        // 获取系统配置
        List<Map<String, Object>> systems = jdbcTemplate.queryForList(
            "SELECT * FROM biz_external_system WHERE system_code = ? AND sync_enabled = 1", systemCode);

        if (systems.isEmpty()) {
            return Map.of("success", false, "message", "系统不存在或未启用同步");
        }

        Map<String, Object> system = systems.get(0);

        try {
            // 执行同步
            SyncResult result = executeSync(systemCode, (String) system.get("system_type"));

            // 更新系统同步状态
            jdbcTemplate.update(
                "UPDATE biz_external_system SET last_sync_at = NOW(), last_sync_status = ?, last_sync_message = ? WHERE system_code = ?",
                result.success ? "SUCCESS" : "FAILURE",
                result.message,
                systemCode);

            // 记录同步日志
            jdbcTemplate.update(
                "INSERT INTO biz_sync_log (system_code, sync_type, sync_action, records_total, records_success, records_failed, status, error_message, started_at, finished_at) " +
                "VALUES (?, 'PULL', 'auto_sync', ?, ?, ?, ?, ?, ?, NOW())",
                systemCode, result.total, result.successCount, result.failedCount,
                result.success ? "SUCCESS" : "FAILURE",
                result.errorMessage,
                Timestamp.valueOf(startedAt));

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("success", result.success);
            response.put("total", result.total);
            response.put("successCount", result.successCount);
            response.put("failedCount", result.failedCount);
            response.put("message", result.message);
            return response;

        } catch (Exception e) {
            log.error("同步外部系统 {} 失败: {}", systemCode, e.getMessage(), e);

            jdbcTemplate.update(
                "UPDATE biz_external_system SET last_sync_at = NOW(), last_sync_status = 'FAILURE', last_sync_message = ? WHERE system_code = ?",
                e.getMessage(), systemCode);

            jdbcTemplate.update(
                "INSERT INTO biz_sync_log (system_code, sync_type, sync_action, records_total, records_success, records_failed, status, error_message, started_at, finished_at) " +
                "VALUES (?, 'PULL', 'auto_sync', 0, 0, 0, 'FAILURE', ?, ?, NOW())",
                systemCode, e.getMessage(), Timestamp.valueOf(startedAt));

            return Map.of("success", false, "message", "同步失败: " + e.getMessage());
        }
    }

    /**
     * 执行同步逻辑（模拟外部系统数据拉取）
     */
    private SyncResult executeSync(String systemCode, String systemType) {
        SyncResult result = new SyncResult();

        // 模拟从外部系统拉取数据
        // 实际部署时替换为真实的 HTTP API 调用
        List<Map<String, Object>> externalData = mockExternalData(systemCode);

        result.total = externalData.size();

        for (Map<String, Object> data : externalData) {
            try {
                // 存储原始数据
                String externalId = (String) data.get("externalId");
                String dataType = (String) data.get("dataType");

                // 检查是否已存在
                Integer exists = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM biz_external_data WHERE system_code = ? AND external_id = ? AND data_type = ?",
                    Integer.class, systemCode, externalId, dataType);

                if (exists != null && exists > 0) {
                    // 更新
                    jdbcTemplate.update(
                        "UPDATE biz_external_data SET raw_data = ?, normalized_data = ?, sync_status = 'SYNCED', updated_at = NOW() " +
                        "WHERE system_code = ? AND external_id = ? AND data_type = ?",
                        data.get("rawData"), data.get("normalizedData"),
                        systemCode, externalId, dataType);
                } else {
                    // 插入
                    jdbcTemplate.update(
                        "INSERT INTO biz_external_data (system_code, external_id, data_type, raw_data, normalized_data, sync_status) " +
                        "VALUES (?, ?, ?, ?, ?, 'SYNCED')",
                        systemCode, externalId, dataType,
                        data.get("rawData"), data.get("normalizedData"));
                }

                result.successCount++;
            } catch (Exception e) {
                result.failedCount++;
                log.warn("同步单条数据失败: {}", e.getMessage());
            }
        }

        result.success = result.failedCount == 0;
        result.message = String.format("同步完成: 总计 %d 条, 成功 %d 条, 失败 %d 条",
            result.total, result.successCount, result.failedCount);

        return result;
    }

    /**
     * 模拟外部系统数据（实际部署时替换为真实 API 调用）
     */
    private List<Map<String, Object>> mockExternalData(String systemCode) {
        List<Map<String, Object>> dataList = new ArrayList<>();

        // 模拟生成 3-8 条数据
        int count = 3 + random.nextInt(6);

        switch (systemCode) {
            case "EMERGENCY":
                for (int i = 0; i < count; i++) {
                    Map<String, Object> data = new LinkedHashMap<>();
                    data.put("externalId", "EMG-" + System.currentTimeMillis() + "-" + i);
                    data.put("dataType", "EVENT");
                    data.put("rawData", String.format("{\"title\":\"应急事件%d\",\"level\":\"二级\",\"location\":\"网格%d区\",\"reporter\":\"应急系统\"}", i + 1, i + 1));
                    data.put("normalizedData", String.format("{\"title\":\"应急事件%d\",\"urgencyLevel\":\"YELLOW\",\"sourceType\":\"EXTERNAL\",\"location\":\"网格%d区\"}", i + 1, i + 1));
                    dataList.add(data);
                }
                break;
            case "HEALTH":
                for (int i = 0; i < count; i++) {
                    Map<String, Object> data = new LinkedHashMap<>();
                    data.put("externalId", "HLT-" + System.currentTimeMillis() + "-" + i);
                    data.put("dataType", "EVENT");
                    data.put("rawData", String.format("{\"title\":\"卫生事件%d\",\"type\":\"传染病\",\"location\":\"商铺%d号\",\"status\":\"待处理\"}", i + 1, i + 1));
                    data.put("normalizedData", String.format("{\"title\":\"卫生事件%d\",\"eventType\":\"HEALTH\",\"sourceType\":\"EXTERNAL\",\"status\":\"PENDING\"}", i + 1));
                    dataList.add(data);
                }
                break;
            case "CIVIL_AFFAIRS":
                for (int i = 0; i < count; i++) {
                    Map<String, Object> data = new LinkedHashMap<>();
                    data.put("externalId", "CVL-" + System.currentTimeMillis() + "-" + i);
                    data.put("dataType", "POPULATION");
                    data.put("rawData", String.format("{\"name\":\"群众%d\",\"type\":\"低保户\",\"address\":\"小区%d栋\",\"phone\":\"1380000%04d\"}", i + 1, i + 1, 1000 + i));
                    data.put("normalizedData", String.format("{\"name\":\"群众%d\",\"category\":\"LOW_INCOME\",\"address\":\"小区%d栋\",\"phone\":\"1380000%04d\"}", i + 1, i + 1, 1000 + i));
                    dataList.add(data);
                }
                break;
            case "PROPERTY":
                for (int i = 0; i < count; i++) {
                    Map<String, Object> data = new LinkedHashMap<>();
                    data.put("externalId", "PRP-" + System.currentTimeMillis() + "-" + i);
                    data.put("dataType", "EVENT");
                    data.put("rawData", String.format("{\"title\":\"物业报修%d\",\"type\":\"漏水\",\"unit\":\"栋%d单元\",\"reporter\":\"物业系统\"}", i + 1, i + 1));
                    data.put("normalizedData", String.format("{\"title\":\"物业报修%d\",\"eventType\":\"REPAIR\",\"sourceType\":\"EXTERNAL\",\"location\":\"栋%d单元\"}", i + 1, i + 1));
                    dataList.add(data);
                }
                break;
            case "12345":
                for (int i = 0; i < count; i++) {
                    Map<String, Object> data = new LinkedHashMap<>();
                    data.put("externalId", "12345-" + System.currentTimeMillis() + "-" + i);
                    data.put("dataType", "EVENT");
                    data.put("rawData", String.format("{\"title\":\"市民投诉%d\",\"type\":\"噪音\",\"location\":\"路段%d\",\"caller\":\"市民热线\"}", i + 1, i + 1));
                    data.put("normalizedData", String.format("{\"title\":\"市民投诉%d\",\"eventType\":\"COMPLAINT\",\"sourceType\":\"12345\",\"location\":\"路段%d\"}", i + 1, i + 1));
                    dataList.add(data);
                }
                break;
            default:
                // 通用模拟数据
                for (int i = 0; i < count; i++) {
                    Map<String, Object> data = new LinkedHashMap<>();
                    data.put("externalId", systemCode + "-" + System.currentTimeMillis() + "-" + i);
                    data.put("dataType", "EVENT");
                    data.put("rawData", String.format("{\"title\":\"外部事件%d\",\"source\":\"%s\"}", i + 1, systemCode));
                    data.put("normalizedData", String.format("{\"title\":\"外部事件%d\",\"sourceType\":\"EXTERNAL\"}", i + 1));
                    dataList.add(data);
                }
        }

        return dataList;
    }

    /**
     * 定时同步任务 - 每30分钟执行一次（带重试）
     */
    @Scheduled(cron = "0 0/30 * * * ?")
    public void scheduledSync() {
        log.info("定时同步任务开始执行");

        List<Map<String, Object>> enabledSystems = jdbcTemplate.queryForList(
            "SELECT system_code FROM biz_external_system WHERE sync_enabled = 1 AND status = 'ACTIVE'");

        for (Map<String, Object> system : enabledSystems) {
            String systemCode = (String) system.get("system_code");
            // 重试机制：最多重试3次
            int maxRetries = 3;
            for (int attempt = 1; attempt <= maxRetries; attempt++) {
                try {
                    syncSystem(systemCode);
                    break; // 成功则跳出重试循环
                } catch (Exception e) {
                    if (attempt == maxRetries) {
                        log.error("定时同步 {} 失败，已重试{}次: {}", systemCode, maxRetries, e.getMessage());
                    } else {
                        log.warn("定时同步 {} 第{}次失败，{}ms后重试: {}", systemCode, attempt, attempt * 5000, e.getMessage());
                        try { Thread.sleep(attempt * 5000L); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
                    }
                }
            }
        }

        log.info("定时同步任务执行完成，共同步 {} 个系统", enabledSystems.size());
    }

    /**
     * 每天凌晨3:00 清理30天前的同步日志
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanupOldSyncLogs() {
        try {
            int deleted = jdbcTemplate.update(
                "DELETE FROM biz_sync_log WHERE created_at < DATE_SUB(NOW(), INTERVAL 30 DAY)");
            log.info("清理同步日志完成，删除 {} 条记录", deleted);
        } catch (Exception e) {
            log.error("清理同步日志异常: {}", e.getMessage(), e);
        }
    }

    private static class SyncResult {
        int total = 0;
        int successCount = 0;
        int failedCount = 0;
        boolean success = true;
        String message = "";
        String errorMessage;
    }
}
