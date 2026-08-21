package com.changping.platform.modules.community.service.impl;

import com.changping.platform.modules.community.entity.PlaceEntity;
import com.changping.platform.modules.community.mapper.PlaceMapper;
import com.changping.platform.modules.community.service.PlaceService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class PlaceServiceImpl implements PlaceService {

    private final PlaceMapper mapper;
    private final JdbcTemplate jdbcTemplate;
    public PlaceServiceImpl(PlaceMapper mapper, JdbcTemplate jdbcTemplate) {
        this.mapper = mapper;
        this.jdbcTemplate = jdbcTemplate;
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
    public PlaceEntity detail(Long id) { return mapper.findById(id); }
    @Override
    public boolean create(PlaceEntity e) {
        if (e.getStatus() == null) e.setStatus("ACTIVE");
        mapper.insert(e);
        return true;
    }
    @Override
    public boolean update(PlaceEntity e) { return mapper.update(e) > 0; }
    @Override
    public boolean delete(Long id) { return mapper.deleteById(id) > 0; }
}
