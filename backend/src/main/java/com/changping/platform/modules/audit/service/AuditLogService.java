package com.changping.platform.modules.audit.service;

import com.changping.platform.modules.audit.entity.AuditLogEntity;
import com.changping.platform.modules.audit.mapper.AuditLogMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class AuditLogService {

    private final AuditLogMapper mapper;
    private final ObjectMapper objectMapper;
    private final JdbcTemplate jdbcTemplate;

    public AuditLogService(AuditLogMapper mapper, ObjectMapper objectMapper, JdbcTemplate jdbcTemplate) {
        this.mapper = mapper;
        this.objectMapper = objectMapper;
        this.jdbcTemplate = jdbcTemplate;
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
     * 分页查询
     */
    public Map<String, Object> queryPaged(String tableName, String recordId, int page, int size) {
        List<AuditLogEntity> items = mapper.findByPage(tableName, recordId, page, size);
        long total = mapper.count(tableName, recordId);
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
     * 回滚到指定版本
     */
    public boolean rollbackToVersion(Long auditLogId) throws JsonProcessingException {
        AuditLogEntity log = mapper.findById(auditLogId);
        if (log == null || log.getOldValues() == null) {
            return false;
        }
        String tableName = log.getTableName();
        String recordId = log.getRecordId();
        Map<String, Object> oldValues = objectMapper.readValue(log.getOldValues(),
                objectMapper.getTypeFactory().constructMapType(Map.class, String.class, Object.class));

        if (oldValues.isEmpty()) return false;

        // 构建UPDATE SQL
        StringBuilder sql = new StringBuilder("UPDATE `").append(tableName).append("` SET ");
        List<Object> params = new ArrayList<>();
        List<String> setClauses = new ArrayList<>();
        for (Map.Entry<String, Object> entry : oldValues.entrySet()) {
            if ("id".equals(entry.getKey())) continue;
            setClauses.add("`" + entry.getKey() + "` = ?");
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
}
