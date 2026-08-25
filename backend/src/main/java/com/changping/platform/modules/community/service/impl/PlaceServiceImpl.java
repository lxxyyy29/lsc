package com.changping.platform.modules.community.service.impl;

import com.changping.platform.modules.community.entity.PlaceEntity;
import com.changping.platform.modules.community.mapper.PlaceMapper;
import com.changping.platform.modules.community.service.PlaceService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class PlaceServiceImpl implements PlaceService {

    private final PlaceMapper mapper;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    public PlaceServiceImpl(PlaceMapper mapper, JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.mapper = mapper;
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<PlaceEntity> list(Long gridId) {
        List<PlaceEntity> result = new ArrayList<>(gridId != null ? mapper.findByGridId(gridId) : mapper.findAllActive());
        // 场所资源库同时展示场所台账（cmn_place_ledger）导入的真实数据，避免台账与资源库数据割裂
        if (gridId == null) {
            List<Map<String, Object>> ledgerRows = jdbcTemplate.queryForList(
                    "SELECT place_name, place_category, address, responsible_person, responsible_phone "
                            + "FROM cmn_place_ledger ORDER BY id");
            for (Map<String, Object> row : ledgerRows) {
                PlaceEntity e = new PlaceEntity();
                e.setPlaceName((String) row.get("place_name"));
                e.setPlaceType(categoryLabel((String) row.get("place_category")));
                e.setAddress((String) row.get("address"));
                e.setContactName((String) row.get("responsible_person"));
                e.setContactPhone((String) row.get("responsible_phone"));
                e.setStatus("ACTIVE");
                result.add(e);
            }
        }
        // risk_tags 在库中是 JSON 数组，回显给前端时统一转为逗号分隔字符串
        for (PlaceEntity e : result) {
            e.setRiskTags(jsonToRiskTags(e.getRiskTags()));
        }
        return result;
    }

    /** 台账场所分类代码 → 中文展示名 */
    private String categoryLabel(String category) {
        if (category == null) return null;
        switch (category) {
            case "RENTAL_HOUSE": return "出租屋";
            case "SMALL_SHOP": return "小档口";
            case "SMALL_ENTERTAINMENT": return "小娱乐场所";
            case "SMALL_WORKSHOP": return "小作坊";
            case "INDUSTRIAL_PARK": return "工业园";
            case "RESIDENTIAL": return "住宅小区";
            case "OTHER": return "其他场所";
            default: return category;
        }
    }
    @Override
    public PlaceEntity detail(Long id) {
        PlaceEntity e = mapper.findById(id);
        if (e != null) {
            e.setRiskTags(jsonToRiskTags(e.getRiskTags()));
        }
        return e;
    }
    @Override
    public boolean create(PlaceEntity e) {
        if (e.getStatus() == null) e.setStatus("ACTIVE");
        // cmn_place.risk_tags 是 JSON 列，写入前把逗号分隔字符串转为 JSON 数组
        e.setRiskTags(riskTagsToJson(e.getRiskTags()));
        mapper.insert(e);
        return true;
    }
    @Override
    public boolean update(PlaceEntity e) {
        e.setRiskTags(riskTagsToJson(e.getRiskTags()));
        return mapper.update(e) > 0;
    }
    @Override
    public boolean delete(Long id) { return mapper.deleteById(id) > 0; }

    /**
     * 用户输入的逗号/顿号分隔字符串 → JSON 数组字符串（写入 cmn_place.risk_tags JSON 列）
     * 空字符串或纯空白返回 null，避免 MySQL 拒绝非法 JSON
     */
    private String riskTagsToJson(String csv) {
        if (csv == null || csv.trim().isEmpty()) return null;
        String[] parts = csv.trim().split("[，,、]");
        List<String> tags = new ArrayList<>();
        for (String p : parts) {
            String t = p.trim();
            if (!t.isEmpty()) tags.add(t);
        }
        if (tags.isEmpty()) return null;
        try {
            return objectMapper.writeValueAsString(tags);
        } catch (Exception ex) {
            return null;
        }
    }

    /**
     * 数据库 JSON 数组字符串 → 逗号分隔字符串（前端展示/编辑回显用）
     * 兼容历史脏数据（非 JSON 字符串）：解析失败时原样返回
     */
    private String jsonToRiskTags(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            List<String> tags = objectMapper.readValue(json, new TypeReference<List<String>>() {});
            return tags == null || tags.isEmpty() ? null : String.join(",", tags);
        } catch (Exception ex) {
            return json;
        }
    }
}
