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
            "SELECT e.event_code, e.title, e.event_type, " +
            "CASE e.report_source WHEN 'GRID_MEMBER' THEN '网格员' WHEN 'PUBLIC_REPORT' THEN '居民随手拍' WHEN 'RESIDENT_REPORT' THEN '居民随手拍' WHEN '12345' THEN '12345热线' WHEN 'PROPERTY' THEN '物业上报' WHEN 'MANUAL' THEN '平台录入' ELSE COALESCE(e.report_source, '-') END AS report_source, " +
            "CASE e.status WHEN 'PENDING_AUDIT' THEN '待审核' WHEN 'IN_AUDIT' THEN '审核中' WHEN 'AUDIT_APPROVED' THEN '已通过' WHEN 'AUDIT_REJECTED' THEN '已驳回' WHEN 'WAITING_DISPATCH' THEN '待派单' WHEN 'WAITING_LEADER_REVIEW' THEN '组长审核' WHEN 'DISPATCHED_TO_WORK_ORDER' THEN '已派单' WHEN 'CLOSED' THEN '已关闭' WHEN 'IGNORED' THEN '已忽略' ELSE e.status END AS status, " +
            "CASE e.urgency_level WHEN 'GREEN' THEN '一般（绿）' WHEN 'YELLOW' THEN '重点（黄）' WHEN 'RED' THEN '紧急（红）' ELSE e.urgency_level END AS urgency_level, " +
            "g.grid_name, e.incident_address, e.occurred_at, e.created_at " +
            "FROM biz_event e LEFT JOIN cmn_grid g ON g.id = e.grid_id WHERE COALESCE(e.archived, 0) = 0");
        List<Object> params = new ArrayList<>();
        applyFilter(sql, params, filters);
        sql.append(" ORDER BY e.created_at DESC LIMIT 500");
        return templateMapper.getJdbc().queryForList(sql.toString(), params.toArray());
    }

    private List<Map<String, Object>> getPopulationData(Map<String, String> filters) {
        StringBuilder sql = new StringBuilder(
            "SELECT p.name, p.phone, " +
            "CASE p.household_type WHEN 'LOCAL' THEN '本地户籍' WHEN 'NON_LOCAL' THEN '外地户籍' WHEN 'FLOATING' THEN '流动人口' WHEN 'LOW_INCOME' THEN '低保户' WHEN 'SPECIAL_CARE' THEN '优抚对象' WHEN 'OTHER' THEN '其他' ELSE COALESCE(p.household_type, '-') END AS household_type, " +
            "p.address, g.grid_name, p.created_at " +
            "FROM cmn_population p LEFT JOIN cmn_grid g ON g.id = p.grid_id WHERE 1=1");
        List<Object> params = new ArrayList<>();
        applyFilter(sql, params, filters);
        sql.append(" ORDER BY p.created_at DESC LIMIT 500");
        return templateMapper.getJdbc().queryForList(sql.toString(), params.toArray());
    }

    private List<Map<String, Object>> getBuildingData(Map<String, String> filters) {
        StringBuilder sql = new StringBuilder(
            "SELECT b.building_no, b.address, b.landlord_name, b.landlord_phone, " +
            "CASE b.fire_risk_level WHEN 'LOW' THEN '低' WHEN 'MEDIUM' THEN '中' WHEN 'HIGH' THEN '高' ELSE COALESCE(b.fire_risk_level, '-') END AS fire_risk_level, g.grid_name, b.created_at " +
            "FROM cmn_building b LEFT JOIN cmn_grid g ON g.id = b.grid_id WHERE 1=1");
        List<Object> params = new ArrayList<>();
        applyFilter(sql, params, filters);
        sql.append(" ORDER BY b.created_at DESC LIMIT 500");
        return templateMapper.getJdbc().queryForList(sql.toString(), params.toArray());
    }

    private List<Map<String, Object>> getMerchantData(Map<String, String> filters) {
        StringBuilder sql = new StringBuilder(
            "SELECT m.merchant_name, m.legal_person_name, m.legal_person_phone, " +
            "m.remark, " +
            "CASE m.status WHEN 'ACTIVE' THEN '启用中' WHEN 'DISABLED' THEN '已停用' ELSE COALESCE(m.status, '-') END AS status, " +
            "CASE m.fire_risk_level WHEN 'LOW' THEN '低' WHEN 'MEDIUM' THEN '中' WHEN 'HIGH' THEN '高' ELSE COALESCE(m.fire_risk_level, '-') END AS fire_risk_level, " +
            "g.grid_name, m.created_at " +
            "FROM biz_merchant m LEFT JOIN cmn_grid g ON g.id = m.area_id WHERE 1=1");
        List<Object> params = new ArrayList<>();
        applyFilter(sql, params, filters);
        sql.append(" ORDER BY m.created_at DESC LIMIT 500");
        return templateMapper.getJdbc().queryForList(sql.toString(), params.toArray());
    }

    private List<Map<String, Object>> getPatrolData(Map<String, String> filters) {
        StringBuilder sql = new StringBuilder(
            "SELECT g.grid_name, " +
            "CASE pr.patrol_type WHEN 'REGULAR' THEN '日常巡查' WHEN 'SPECIAL' THEN '专项巡查' WHEN 'EMERGENCY' THEN '应急巡查' ELSE COALESCE(pr.patrol_type, '-') END AS patrol_type, " +
            "pr.content, " +
            "CASE pr.status WHEN 'COMPLETED' THEN '已完成' WHEN 'PENDING' THEN '待完成' WHEN 'OVERDUE' THEN '超期' WHEN 'SUBMITTED' THEN '已提交' ELSE COALESCE(pr.status, '-') END AS status, pr.created_at " +
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
