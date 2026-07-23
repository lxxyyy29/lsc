package com.changping.platform.modules.community.mapper;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;

@Component
public class ExportMapper {

    private final JdbcTemplate jdbcTemplate;

    public ExportMapper(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Map<String, Object>> getEventLedger() {
        String sql = "SELECT e.event_code, e.title, e.event_type, e.report_source, e.status, " +
                "e.urgency_level, g.grid_name, e.incident_address, e.occurred_at, e.created_at " +
                "FROM biz_event e LEFT JOIN cmn_grid g ON e.grid_id = g.id " +
                "ORDER BY e.created_at DESC";
        return jdbcTemplate.queryForList(sql);
    }

    public List<Map<String, Object>> getPopulationLedger() {
        String sql = "SELECT p.name, p.id_card, p.phone, p.household_type, p.address, " +
                "g.grid_name, p.tags, p.created_at " +
                "FROM cmn_population p LEFT JOIN cmn_grid g ON p.grid_id = g.id " +
                "WHERE p.status = 'ACTIVE' ORDER BY p.created_at DESC";
        return jdbcTemplate.queryForList(sql);
    }

    public List<Map<String, Object>> getBuildingLedger() {
        String sql = "SELECT b.building_no, b.address, b.landlord_name, b.landlord_phone, " +
                "b.fire_risk_level, b.is_group_rental, g.grid_name " +
                "FROM cmn_building b LEFT JOIN cmn_grid g ON b.grid_id = g.id " +
                "WHERE b.status = 'ACTIVE' ORDER BY b.created_at DESC";
        return jdbcTemplate.queryForList(sql);
    }

    public List<Map<String, Object>> getPatrolLedger() {
        String sql = "SELECT g.grid_name, pr.patrol_type, pr.content, pr.status, " +
                "pr.created_at, pr.longitude, pr.latitude " +
                "FROM cmn_patrol_record pr LEFT JOIN cmn_grid g ON pr.grid_id = g.id " +
                "ORDER BY pr.created_at DESC";
        return jdbcTemplate.queryForList(sql);
    }

    public List<Map<String, Object>> getMerchantLedger() {
        String sql = "SELECT m.merchant_name, m.legal_person_name, m.legal_person_phone, " +
                "m.remark, m.created_at " +
                "FROM biz_merchant m ORDER BY m.id ASC";
        return jdbcTemplate.queryForList(sql);
    }
}
