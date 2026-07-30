package com.changping.platform.modules.community.service;

import com.changping.platform.modules.community.entity.PolicyResourceEntity;
import com.changping.platform.modules.community.mapper.PolicyResourceMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;

@Service
public class PolicyResourceService extends ServiceImpl<PolicyResourceMapper, PolicyResourceEntity> {

    private final JdbcTemplate jdbcTemplate;

    public PolicyResourceService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<PolicyResourceEntity> listAll() {
        return list(new LambdaQueryWrapper<PolicyResourceEntity>()
                .orderByDesc(PolicyResourceEntity::getCreatedAt));
    }

    public List<PolicyResourceEntity> listActive() {
        return baseMapper.findAllActive();
    }

    public PolicyResourceEntity create(PolicyResourceEntity entity) {
        if (entity.getPolicyCode() == null || entity.getPolicyCode().isBlank()) {
            entity.setPolicyCode("POL-" + System.currentTimeMillis());
        }
        if (entity.getStatus() == null) {
            entity.setStatus("ACTIVE");
        }
        save(entity);
        return entity;
    }

    /**
     * 政策找人：根据 PolicyType 匹配 cmn_population 中符合条件的人群
     * 匹配逻辑：户籍类型对应 + 标签模糊匹配
     */
    public List<Map<String, Object>> findMatchingPeople(Long policyId) {
        PolicyResourceEntity policy = getById(policyId);
        if (policy == null) {
            return Collections.emptyList();
        }
        String policyType = policy.getPolicyType();

        // 根据政策类型确定匹配的户籍类型
        List<String> householdTypes = resolveHouseholdTypes(policyType);
        if (householdTypes.isEmpty()) {
            return Collections.emptyList();
        }

        StringBuilder sql = new StringBuilder(
                "SELECT p.id, p.name, p.phone, p.household_type, p.address, p.tags, g.grid_name " +
                "FROM cmn_population p LEFT JOIN cmn_grid g ON g.id = p.grid_id " +
                "WHERE p.status = 'ACTIVE' AND p.household_type IN (");
        List<Object> params = new ArrayList<>();
        for (String ht : householdTypes) {
            sql.append("?,");
            params.add(ht);
        }
        sql.setLength(sql.length() - 1);
        sql.append(")");

        // 标签匹配（如果有标签）
        if (StringUtils.hasText(policy.getTags())) {
            String[] tagArr = policy.getTags().split(",");
            sql.append(" AND (");
            for (int i = 0; i < tagArr.length; i++) {
                if (i > 0) sql.append(" OR ");
                sql.append("p.tags LIKE ?");
                params.add("%" + tagArr[i].trim() + "%");
            }
            sql.append(")");
        }

        sql.append(" ORDER BY p.grid_id, p.name LIMIT 500");
        return jdbcTemplate.queryForList(sql.toString(), params.toArray());
    }

    /**
     * 政策类型 → 户籍类型映射
     */
    private List<String> resolveHouseholdTypes(String policyType) {
        if (policyType == null) return Collections.emptyList();
        return switch (policyType) {
            case "LOW_INCOME" -> List.of("LOW_INCOME");
            case "ELDERLY" -> List.of("SPECIAL_CARE", "LOCAL", "NON_LOCAL");
            case "RESCUE" -> List.of("LOW_INCOME", "SPECIAL_CARE", "FLOATING");
            case "MEDICAL" -> List.of("LOCAL", "NON_LOCAL", "FLOATING");
            case "BENEFIT" -> List.of("LOCAL", "NON_LOCAL", "FLOATING", "LOW_INCOME", "SPECIAL_CARE");
            default -> List.of("LOCAL", "NON_LOCAL");
        };
    }
}
