package com.changping.platform.modules.audit.service;

import com.changping.platform.common.exception.BusinessException;
import com.changping.platform.modules.audit.entity.AuditLogEntity;
import com.changping.platform.modules.audit.mapper.AuditLogMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class AuditLogService {

    private static final Set<String> ROLLBACK_TABLE_ALLOWLIST = Set.of(
            "cmn_grid",
            "cmn_population",
            "cmn_building",
            "cmn_place",
            "cmn_place_ledger",
            "cmn_org_member",
            "cmn_patrol_record",
            "cmn_patrol_task",
            "cmn_resident_report",
            "biz_event",
            "biz_work_order");

    private final AuditLogMapper mapper;
    private final ObjectMapper objectMapper;
    private final JdbcTemplate jdbcTemplate;

    public AuditLogService(AuditLogMapper mapper, ObjectMapper objectMapper, JdbcTemplate jdbcTemplate) {
        this.mapper = mapper;
        this.objectMapper = objectMapper;
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 快速记录变更（直接传入 old/new Map，避免实体类依赖）
     */
    public void logQuickChange(String tableName, String recordId, String operationType,
                               Map<String, Object> oldMap, Map<String, Object> newMap,
                               Long operatorId, String operatorName, String remark) {
        try {
            AuditLogEntity log = new AuditLogEntity();
            log.setTableName(tableName);
            log.setRecordId(recordId);
            log.setOperationType(operationType);
            log.setOperatorId(operatorId);
            log.setOperatorName(operatorName);
            log.setRemark(remark);

            if (oldMap != null && !oldMap.isEmpty()) {
                log.setOldValues(objectMapper.writeValueAsString(oldMap));
            }
            if (newMap != null && !newMap.isEmpty()) {
                log.setNewValues(objectMapper.writeValueAsString(newMap));
            }

            // 计算变更字段
            if (oldMap != null && newMap != null) {
                Set<String> changed = new LinkedHashSet<>();
                for (String key : newMap.keySet()) {
                    Object oldVal = oldMap.get(key);
                    Object newVal = newMap.get(key);
                    if (!java.util.Objects.equals(oldVal, newVal)) {
                        changed.add(key);
                    }
                }
                log.setChangedFields(objectMapper.writeValueAsString(new ArrayList<>(changed)));
            }

            mapper.insert(log);
        } catch (JsonProcessingException e) {
            // 忽略序列化失败
        }
    }

    /**
     * 记录变更日志
     */
    public void logChange(String tableName, String recordId, String operationType,
                          Object oldObj, Object newObj, Long operatorId, String operatorName) {
        try {
            AuditLogEntity log = new AuditLogEntity();
            log.setTableName(tableName);
            log.setRecordId(recordId);
            log.setOperationType(operationType);
            log.setOperatorId(operatorId);
            log.setOperatorName(operatorName);

            if (oldObj != null) {
                log.setOldValues(objectMapper.writeValueAsString(oldObj));
            }
            if (newObj != null) {
                log.setNewValues(objectMapper.writeValueAsString(newObj));
            }

            // 计算变更字段
            if (oldObj != null && newObj != null) {
                Set<String> changed = new LinkedHashSet<>();
                Map<String, Object> oldMap = objectMapper.convertValue(oldObj, Map.class);
                Map<String, Object> newMap = objectMapper.convertValue(newObj, Map.class);
                for (String key : newMap.keySet()) {
                    Object oldVal = oldMap.get(key);
                    Object newVal = newMap.get(key);
                    if (!java.util.Objects.equals(oldVal, newVal)) {
                        changed.add(key);
                    }
                }
                log.setChangedFields(objectMapper.writeValueAsString(new ArrayList<>(changed)));
            }

            mapper.insert(log);
        } catch (JsonProcessingException e) {
            // 忽略序列化失败
        }
    }

    /**
     * 查询变更历史
     */
    public List<AuditLogEntity> getHistory(String tableName, String recordId) {
        return mapper.findByTableAndRecord(tableName, recordId);
    }

    /**
     * 分页查询（增强版：支持操作类型/操作人/时间范围筛选）
     */
    public Map<String, Object> queryPaged(String tableName, String recordId, String operationType,
                                          Long operatorId, String startTime, String endTime,
                                          int page, int size) {
        List<AuditLogEntity> items = mapper.findByPageWithFilters(tableName, recordId, operationType, operatorId, startTime, endTime, page, size);
        long total = mapper.countWithFilters(tableName, recordId, operationType, operatorId, startTime, endTime);
        Map<String, Object> result = new HashMap<>();
        result.put("items", items);
        result.put("total", total);
        result.put("page", page);
        result.put("size", size);
        return result;
    }

    /**
     * 获取所有表名
     */
    public List<String> getTables() {
        return mapper.findDistinctTables();
    }

    /**
     * 根据ID获取单条审计日志
     */
    public AuditLogEntity getById(Long id) {
        AuditLogEntity log = mapper.findById(id);
        if (log == null) {
            throw new BusinessException("AUDIT_LOG_NOT_FOUND", "审计日志不存在");
        }
        return log;
    }

    /**
     * 获取字段级变更详情（用于前端 Diff 展示）
     */
    public Map<String, Object> getDiffDetail(Long auditLogId) {
        AuditLogEntity log = mapper.findById(auditLogId);
        if (log == null) {
            throw new BusinessException("AUDIT_LOG_NOT_FOUND", "审计日志不存在");
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", log.getId());
        result.put("tableName", log.getTableName());
        result.put("recordId", log.getRecordId());
        result.put("operationType", log.getOperationType());
        result.put("operatorName", log.getOperatorName());
        result.put("operationTime", log.getOperationTime());
        result.put("remark", log.getRemark());

        // 解析变更字段
        List<String> fields = parseJsonArray(log.getChangedFields());
        if (fields == null) fields = List.of();

        Map<String, Object> oldMap = parseJsonObject(log.getOldValues());
        Map<String, Object> newMap = parseJsonObject(log.getNewValues());

        List<Map<String, Object>> diffRows = new ArrayList<>();
        // 合并所有出现过的字段
        Set<String> allFields = new LinkedHashSet<>(fields);
        if (oldMap != null) allFields.addAll(oldMap.keySet());
        if (newMap != null) allFields.addAll(newMap.keySet());

        for (String field : allFields) {
            if ("id".equals(field)) continue;
            Object oldVal = oldMap != null ? oldMap.get(field) : null;
            Object newVal = newMap != null ? newMap.get(field) : null;
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("field", field);
            row.put("oldValue", oldVal);
            row.put("newValue", newVal);
            row.put("changed", !java.util.Objects.equals(oldVal, newVal));
            diffRows.add(row);
        }
        result.put("diffRows", diffRows);
        return result;
    }

    /**
     * 预览回滚结果（返回将被修改的字段和值，不实际执行）
     */
    public Map<String, Object> previewRollback(Long auditLogId) throws JsonProcessingException {
        AuditLogEntity log = mapper.findById(auditLogId);
        if (log == null || log.getOldValues() == null) {
            throw new BusinessException("AUDIT_LOG_NOT_FOUND", "审计日志不存在或无旧值");
        }
        validateRollbackTable(log.getTableName());
        Map<String, Object> oldValues = objectMapper.readValue(log.getOldValues(),
                objectMapper.getTypeFactory().constructMapType(Map.class, String.class, Object.class));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("auditLogId", log.getId());
        result.put("tableName", log.getTableName());
        result.put("recordId", log.getRecordId());
        result.put("operationType", log.getOperationType());

        // 查询当前值
        Map<String, Object> currentValues = jdbcTemplate.queryForMap(
                "SELECT * FROM `" + log.getTableName() + "` WHERE id = ?",
                Long.parseLong(log.getRecordId()));

        List<Map<String, Object>> previewRows = new ArrayList<>();
        for (Map.Entry<String, Object> entry : oldValues.entrySet()) {
            String field = entry.getKey();
            if ("id".equals(field)) continue;
            Object oldVal = entry.getValue();
            Object currentVal = currentValues.get(field);
            if (!java.util.Objects.equals(oldVal, currentVal)) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("field", field);
                row.put("currentValue", currentVal);
                row.put("rollbackToValue", oldVal);
                previewRows.add(row);
            }
        }
        result.put("previewRows", previewRows);
        return result;
    }

    private List<String> parseJsonArray(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            List<String> result = objectMapper.readValue(json,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
            return result;
        } catch (Exception e) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseJsonObject(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            return objectMapper.readValue(json,
                    objectMapper.getTypeFactory().constructMapType(Map.class, String.class, Object.class));
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 回滚到指定版本
     */
    public boolean rollbackToVersion(Long auditLogId) throws JsonProcessingException {
        AuditLogEntity log = mapper.findById(auditLogId);
        if (log == null || log.getOldValues() == null) {
            return false;
        }
        String tableName = log.getTableName();
        String recordId = log.getRecordId();
        validateRollbackTable(tableName);
        Map<String, Object> oldValues = objectMapper.readValue(log.getOldValues(),
                objectMapper.getTypeFactory().constructMapType(Map.class, String.class, Object.class));

        if (oldValues.isEmpty()) return false;

        // 构建UPDATE SQL
        StringBuilder sql = new StringBuilder("UPDATE `").append(tableName).append("` SET ");
        List<Object> params = new ArrayList<>();
        List<String> setClauses = new ArrayList<>();
        for (Map.Entry<String, Object> entry : oldValues.entrySet()) {
            String columnName = entry.getKey();
            if ("id".equals(columnName)) continue;
            validateIdentifier(columnName, "审计字段不允许回滚");
            setClauses.add("`" + columnName + "` = ?");
            params.add(entry.getValue());
        }
        if (setClauses.isEmpty()) return false;
        sql.append(String.join(", ", setClauses));
        sql.append(" WHERE id = ?");
        params.add(Long.parseLong(recordId));

        // 使用JdbcTemplate执行
        jdbcTemplate.update(sql.toString(), params.toArray());

        // 记录回滚操作
        AuditLogEntity rollbackLog = new AuditLogEntity();
        rollbackLog.setTableName(tableName);
        rollbackLog.setRecordId(recordId);
        rollbackLog.setOperationType("ROLLBACK");
        rollbackLog.setOldValues(log.getNewValues());
        rollbackLog.setNewValues(log.getOldValues());
        rollbackLog.setRemark("回滚到审计日志 #" + auditLogId);
        mapper.insert(rollbackLog);

        return true;
    }

    private void validateRollbackTable(String tableName) {
        validateIdentifier(tableName, "审计表不允许回滚");
        if (!ROLLBACK_TABLE_ALLOWLIST.contains(tableName)) {
            throw new BusinessException("AUDIT_ROLLBACK_TABLE_FORBIDDEN", "该表不允许通过审计日志回滚");
        }
    }

    private void validateIdentifier(String identifier, String message) {
        if (identifier == null || !identifier.matches("[A-Za-z0-9_]+")) {
            throw new BusinessException("AUDIT_ROLLBACK_IDENTIFIER_INVALID", message);
        }
    }
}
