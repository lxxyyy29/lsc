package com.changping.platform.modules.ledger.service;

import com.changping.platform.modules.ledger.entity.LedgerTemplateEntity;
import com.changping.platform.modules.ledger.mapper.LedgerTemplateMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class LedgerService {

    private final LedgerTemplateMapper templateMapper;
    private final ObjectMapper objectMapper;

    public LedgerService(LedgerTemplateMapper templateMapper, ObjectMapper objectMapper) {
        this.templateMapper = templateMapper;
        this.objectMapper = objectMapper;
    }

    public List<LedgerTemplateEntity> getAllTemplates() {
        return templateMapper.findAll();
    }

    public List<LedgerTemplateEntity> getTemplatesByType(String type) {
        return templateMapper.findByType(type);
    }

    public LedgerTemplateEntity getTemplate(Long id) {
        return templateMapper.findById(id);
    }

    public void saveTemplate(LedgerTemplateEntity entity) {
        if (entity.getId() != null) {
            templateMapper.update(entity);
        } else {
            templateMapper.insert(entity);
        }
    }

    public void deleteTemplate(Long id) {
        templateMapper.delete(id);
    }

    /**
     * 根据模板类型获取台账数据
     */
    public List<Map<String, Object>> getLedgerData(String type, Map<String, String> filters) {
        switch (type) {
            case "EVENT":
                return getEventData(filters);
            case "POPULATION":
                return getPopulationData(filters);
            case "BUILDING":
                return getBuildingData(filters);
            case "MERCHANT":
                return getMerchantData(filters);
            case "PATROL":
                return getPatrolData(filters);
            default:
                return new ArrayList<>();
        }
    }

    private List<Map<String, Object>> getEventData(Map<String, String> filters) {
        // 与事件列表页口径一致：仅展示未归档的活跃事件
        StringBuilder sql = new StringBuilder(
            "SELECT e.event_code, e.title, e.event_type, e.report_source, e.status, " +
            "e.urgency_level, g.grid_name, e.incident_address, e.occurred_at, e.created_at " +
            "FROM biz_event e LEFT JOIN cmn_grid g ON g.id = e.grid_id WHERE COALESCE(e.archived, 0) = 0");
        List<Object> params = new ArrayList<>();
        applyFilter(sql, params, filters);
        sql.append(" ORDER BY e.created_at DESC LIMIT 500");
        return templateMapper.getJdbc().queryForList(sql.toString(), params.toArray());
    }

    private List<Map<String, Object>> getPopulationData(Map<String, String> filters) {
        StringBuilder sql = new StringBuilder(
            "SELECT p.name, p.phone, p.household_type, p.address, g.grid_name, p.created_at " +
            "FROM cmn_population p LEFT JOIN cmn_grid g ON g.id = p.grid_id WHERE 1=1");
        List<Object> params = new ArrayList<>();
        applyFilter(sql, params, filters);
        sql.append(" ORDER BY p.created_at DESC LIMIT 500");
        return templateMapper.getJdbc().queryForList(sql.toString(), params.toArray());
    }

    private List<Map<String, Object>> getBuildingData(Map<String, String> filters) {
        StringBuilder sql = new StringBuilder(
            "SELECT b.building_no, b.address, b.landlord_name, b.landlord_phone, " +
            "b.fire_risk_level, g.grid_name, b.created_at " +
            "FROM cmn_building b LEFT JOIN cmn_grid g ON g.id = b.grid_id WHERE 1=1");
        List<Object> params = new ArrayList<>();
        applyFilter(sql, params, filters);
        sql.append(" ORDER BY b.created_at DESC LIMIT 500");
        return templateMapper.getJdbc().queryForList(sql.toString(), params.toArray());
    }

    private List<Map<String, Object>> getMerchantData(Map<String, String> filters) {
        StringBuilder sql = new StringBuilder(
            "SELECT m.merchant_name, m.legal_person_name, m.legal_person_phone, " +
            "m.remark, m.status, m.fire_risk_level, g.grid_name, m.created_at " +
            "FROM biz_merchant m LEFT JOIN cmn_grid g ON g.id = m.area_id WHERE 1=1");
        List<Object> params = new ArrayList<>();
        applyFilter(sql, params, filters);
        sql.append(" ORDER BY m.created_at DESC LIMIT 500");
        return templateMapper.getJdbc().queryForList(sql.toString(), params.toArray());
    }

    private List<Map<String, Object>> getPatrolData(Map<String, String> filters) {
        StringBuilder sql = new StringBuilder(
            "SELECT g.grid_name, pr.patrol_type, pr.content, pr.status, pr.created_at " +
            "FROM cmn_patrol_record pr LEFT JOIN cmn_grid g ON g.id = pr.grid_id WHERE 1=1");
        List<Object> params = new ArrayList<>();
        applyFilter(sql, params, filters);
        sql.append(" ORDER BY pr.created_at DESC LIMIT 500");
        return templateMapper.getJdbc().queryForList(sql.toString(), params.toArray());
    }

    private void applyFilter(StringBuilder sql, List<Object> params, Map<String, String> filters) {
        if (filters == null) return;
        String gridId = filters.get("gridId");
        if (gridId != null && !gridId.isEmpty()) {
            sql.append(" AND g.id = ?");
            params.add(Long.parseLong(gridId));
        }
        String status = filters.get("status");
        if (status != null && !status.isEmpty()) {
            sql.append(" AND e.status = ?");
            params.add(status);
        }
        String startDate = filters.get("startDate");
        if (startDate != null && !startDate.isEmpty()) {
            sql.append(" AND e.created_at >= ?");
            params.add(startDate);
        }
        String endDate = filters.get("endDate");
        if (endDate != null && !endDate.isEmpty()) {
            sql.append(" AND e.created_at <= ?");
            params.add(endDate + " 23:59:59");
        }
    }
}
