package com.changping.platform.modules.system.service;

import com.changping.platform.common.exception.BusinessException;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * @Description //系统字典服务：字典类型与字典项的增删改查，业务下拉选项统一由字典驱动
 */
@Service
public class SystemDictService {

    private final JdbcTemplate jdbcTemplate;

    public SystemDictService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /** 查询全部字典类型（含字典项数量），按编码排序 */
    @Transactional(readOnly = true)
    public List<DictTypeItem> listTypes() {
        return jdbcTemplate.query(
                "SELECT t.id, t.dict_code, t.dict_name, t.status, t.remark, COUNT(i.id) AS item_count "
                        + "FROM sys_dict_type t LEFT JOIN sys_dict_item i ON i.dict_code = t.dict_code "
                        + "GROUP BY t.id, t.dict_code, t.dict_name, t.status, t.remark ORDER BY t.dict_code ASC",
                (rs, rowNum) -> new DictTypeItem(
                        rs.getLong("id"),
                        rs.getString("dict_code"),
                        rs.getString("dict_name"),
                        rs.getString("status"),
                        rs.getString("remark"),
                        rs.getInt("item_count")));
    }

    /** 查询指定字典下的字典项；activeOnly=true 时仅返回启用项（业务下拉使用） */
    @Transactional(readOnly = true)
    public List<DictItem> listItems(String dictCode, boolean activeOnly) {
        requireType(dictCode);
        String sql = "SELECT id, dict_code, item_value, item_label, sort_order, status, remark FROM sys_dict_item "
                + "WHERE dict_code = ?" + (activeOnly ? " AND status = 'ACTIVE'" : "")
                + " ORDER BY sort_order ASC, id ASC";
        return jdbcTemplate.query(sql, (rs, rowNum) -> new DictItem(
                rs.getLong("id"),
                rs.getString("dict_code"),
                rs.getString("item_value"),
                rs.getString("item_label"),
                rs.getInt("sort_order"),
                rs.getString("status"),
                rs.getString("remark")), dictCode);
    }

    /** 创建字典类型，编码唯一；前端未传编码时自动生成（DICT_ + 时间戳） */
    @Transactional
    public DictTypeItem createType(UpsertTypeRequest request) {
        String dictCode = StringUtils.hasText(request.dictCode())
                ? request.dictCode().trim()
                : "DICT_" + System.currentTimeMillis();
        String dictName = requireText(request.dictName(), "字典名称不能为空").trim();
        Integer exists = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sys_dict_type WHERE dict_code = ?", Integer.class, dictCode);
        if (exists != null && exists > 0) {
            throw new BusinessException("SYSTEM_DICT_CODE_EXISTS", "字典编码已存在: " + dictCode);
        }
        jdbcTemplate.update(
                "INSERT INTO sys_dict_type (dict_code, dict_name, status, remark, created_at, updated_at) VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                dictCode, dictName, normalizeStatus(request.status()), normalizeRemark(request.remark()));
        return listTypes().stream().filter(t -> t.dictCode().equals(dictCode)).findFirst()
                .orElseThrow(() -> new BusinessException("SYSTEM_DICT_CREATE_FAILED", "创建字典失败"));
    }

    /** 更新字典类型：编码不可改，仅调整名称/状态/备注 */
    @Transactional
    public DictTypeItem updateType(Long id, UpsertTypeRequest request) {
        DictTypeItem existing = requireTypeById(id);
        jdbcTemplate.update(
                "UPDATE sys_dict_type SET dict_name = ?, status = ?, remark = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?",
                requireText(request.dictName(), "字典名称不能为空").trim(),
                normalizeStatus(request.status()),
                normalizeRemark(request.remark()),
                id);
        return listTypes().stream().filter(t -> t.dictCode().equals(existing.dictCode())).findFirst()
                .orElseThrow(() -> new BusinessException("SYSTEM_DICT_NOT_FOUND", "字典未找到"));
    }

    /** 删除字典类型及其全部字典项（历史业务数据已落库的值不受影响） */
    @Transactional
    public void deleteType(Long id) {
        DictTypeItem existing = requireTypeById(id);
        jdbcTemplate.update("DELETE FROM sys_dict_item WHERE dict_code = ?", existing.dictCode());
        jdbcTemplate.update("DELETE FROM sys_dict_type WHERE id = ?", id);
    }

    /** 新增字典项，同一字典下值唯一；前端未传值时自动生成（ITEM_ + 时间戳） */
    @Transactional
    public DictItem createItem(String dictCode, UpsertItemRequest request) {
        requireType(dictCode);
        String itemValue = StringUtils.hasText(request.itemValue())
                ? request.itemValue().trim()
                : "ITEM_" + System.currentTimeMillis();
        String itemLabel = requireText(request.itemLabel(), "字典项名称不能为空").trim();
        Integer exists = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sys_dict_item WHERE dict_code = ? AND item_value = ?",
                Integer.class, dictCode, itemValue);
        if (exists != null && exists > 0) {
            throw new BusinessException("SYSTEM_DICT_ITEM_EXISTS", "该字典下已存在相同值: " + itemValue);
        }
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            var ps = connection.prepareStatement(
                    "INSERT INTO sys_dict_item (dict_code, item_value, item_label, sort_order, status, remark, created_at, updated_at) "
                            + "VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                    new String[]{"id"});
            ps.setString(1, dictCode);
            ps.setString(2, itemValue);
            ps.setString(3, itemLabel);
            ps.setInt(4, request.sortOrder() == null ? 0 : request.sortOrder());
            ps.setString(5, normalizeStatus(request.status()));
            ps.setString(6, normalizeRemark(request.remark()));
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        if (key == null) {
            throw new BusinessException("SYSTEM_DICT_ITEM_CREATE_FAILED", "创建字典项失败");
        }
        return getItem(key.longValue());
    }

    /** 更新字典项：值/名称/排序/状态/备注均可调，但值在同字典内须保持唯一 */
    @Transactional
    public DictItem updateItem(Long itemId, UpsertItemRequest request) {
        DictItem existing = getItem(itemId);
        String itemValue = StringUtils.hasText(request.itemValue()) ? request.itemValue().trim() : existing.itemValue();
        Integer exists = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sys_dict_item WHERE dict_code = ? AND item_value = ? AND id <> ?",
                Integer.class, existing.dictCode(), itemValue, itemId);
        if (exists != null && exists > 0) {
            throw new BusinessException("SYSTEM_DICT_ITEM_EXISTS", "该字典下已存在相同值: " + itemValue);
        }
        jdbcTemplate.update(
                "UPDATE sys_dict_item SET item_value = ?, item_label = ?, sort_order = ?, status = ?, remark = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?",
                itemValue,
                StringUtils.hasText(request.itemLabel()) ? request.itemLabel().trim() : existing.itemLabel(),
                request.sortOrder() == null ? existing.sortOrder() : request.sortOrder(),
                StringUtils.hasText(request.status()) ? normalizeStatus(request.status()) : existing.status(),
                request.remark() == null ? existing.remark() : normalizeRemark(request.remark()),
                itemId);
        return getItem(itemId);
    }

    /** 删除字典项 */
    @Transactional
    public void deleteItem(Long itemId) {
        getItem(itemId);
        jdbcTemplate.update("DELETE FROM sys_dict_item WHERE id = ?", itemId);
    }

    @Transactional(readOnly = true)
    public DictItem getItem(Long itemId) {
        List<DictItem> items = jdbcTemplate.query(
                "SELECT id, dict_code, item_value, item_label, sort_order, status, remark FROM sys_dict_item WHERE id = ?",
                (rs, rowNum) -> new DictItem(
                        rs.getLong("id"),
                        rs.getString("dict_code"),
                        rs.getString("item_value"),
                        rs.getString("item_label"),
                        rs.getInt("sort_order"),
                        rs.getString("status"),
                        rs.getString("remark")),
                itemId);
        if (items.isEmpty()) {
            throw new BusinessException("SYSTEM_DICT_ITEM_NOT_FOUND", "字典项未找到");
        }
        return items.get(0);
    }

    private void requireType(String dictCode) {
        if (!StringUtils.hasText(dictCode)) {
            throw new BusinessException("VALIDATION_ERROR", "字典编码不能为空");
        }
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sys_dict_type WHERE dict_code = ?", Integer.class, dictCode);
        if (count == null || count == 0) {
            throw new BusinessException("SYSTEM_DICT_NOT_FOUND", "字典未找到: " + dictCode);
        }
    }

    private DictTypeItem requireTypeById(Long id) {
        List<DictTypeItem> types = jdbcTemplate.query(
                "SELECT t.id, t.dict_code, t.dict_name, t.status, t.remark, COUNT(i.id) AS item_count "
                        + "FROM sys_dict_type t LEFT JOIN sys_dict_item i ON i.dict_code = t.dict_code "
                        + "WHERE t.id = ? GROUP BY t.id, t.dict_code, t.dict_name, t.status, t.remark",
                (rs, rowNum) -> new DictTypeItem(
                        rs.getLong("id"),
                        rs.getString("dict_code"),
                        rs.getString("dict_name"),
                        rs.getString("status"),
                        rs.getString("remark"),
                        rs.getInt("item_count")),
                id);
        if (types.isEmpty()) {
            throw new BusinessException("SYSTEM_DICT_NOT_FOUND", "字典未找到");
        }
        return types.get(0);
    }

    private String requireText(String value, String errorMessage) {
        if (!StringUtils.hasText(value)) {
            throw new BusinessException("VALIDATION_ERROR", errorMessage);
        }
        return value;
    }

    private String normalizeStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return "ACTIVE";
        }
        String normalized = status.trim().toUpperCase();
        if (!java.util.Set.of("ACTIVE", "DISABLED").contains(normalized)) {
            throw new BusinessException("VALIDATION_ERROR", "不支持的状态: " + status);
        }
        return normalized;
    }

    private String normalizeRemark(String remark) {
        return StringUtils.hasText(remark) ? remark.trim() : null;
    }

    public record UpsertTypeRequest(String dictCode, String dictName, String status, String remark) {
    }

    public record UpsertItemRequest(String itemValue, String itemLabel, Integer sortOrder, String status, String remark) {
    }

    public record DictTypeItem(Long id, String dictCode, String dictName, String status, String remark, Integer itemCount) {
    }

    public record DictItem(Long id, String dictCode, String itemValue, String itemLabel, Integer sortOrder, String status, String remark) {
    }
}
