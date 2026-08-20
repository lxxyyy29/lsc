package com.changping.platform.modules.community.service;

import com.changping.platform.modules.community.entity.PolicyResourceEntity;
import com.changping.platform.modules.community.mapper.PolicyResourceMapper;
import com.changping.platform.modules.notification.entity.NotificationEntity;
import com.changping.platform.modules.notification.service.NotificationBatchWriter;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;

@Service
public class PolicyResourceService extends ServiceImpl<PolicyResourceMapper, PolicyResourceEntity> {

    private final JdbcTemplate jdbcTemplate;
    private final NotificationBatchWriter notificationBatchWriter;

    public PolicyResourceService(JdbcTemplate jdbcTemplate, NotificationBatchWriter notificationBatchWriter) {
        this.jdbcTemplate = jdbcTemplate;
        this.notificationBatchWriter = notificationBatchWriter;
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
     * 定向推送：将政策以站内通知形式推送给匹配人群中有居民账号的人
     * 匹配人群按手机号关联 sys_user（PUBLIC 角色）；无账号者跳过并计数
     */
    public Map<String, Object> pushToResidents(Long policyId) {
        PolicyResourceEntity policy = getById(policyId);
        Map<String, Object> result = new HashMap<>();
        if (policy == null) {
            result.put("matched", 0);
            result.put("pushed", 0);
            result.put("noAccount", 0);
            result.put("message", "政策不存在");
            return result;
        }
        List<Map<String, Object>> people = findMatchingPeople(policyId);

        // 收集去重手机号，批量查出对应的居民账号
        List<String> phones = people.stream()
                .map(p -> p.get("phone"))
                .filter(Objects::nonNull)
                .map(String::valueOf)
                .filter(s -> !s.isBlank())
                .distinct()
                .toList();
        Map<String, Long> phoneToUserId = new HashMap<>();
        if (!phones.isEmpty()) {
            String placeholders = String.join(",", phones.stream().map(x -> "?").toList());
            List<Map<String, Object>> users = jdbcTemplate.queryForList(
                    "SELECT u.id, u.phone FROM sys_user u " +
                    "JOIN sys_role r ON r.id = u.role_id " +
                    "WHERE u.deleted = 0 AND u.status = 'ACTIVE' AND r.role_code = 'PUBLIC' AND u.phone IN (" + placeholders + ")",
                    phones.toArray());
            for (Map<String, Object> u : users) {
                phoneToUserId.put(String.valueOf(u.get("phone")), ((Number) u.get("id")).longValue());
            }
        }

        // 逐人写入站内通知（标题取政策标题、内容取政策说明摘要，携带政策 ID 便于跳转）
        int pushed = 0;
        int noAccount = 0;
        String content = policy.getDescription();
        if (content != null && content.length() > 200) {
            content = content.substring(0, 200) + "…";
        }
        for (Map<String, Object> person : people) {
            Object phoneRaw = person.get("phone");
            Long userId = phoneRaw == null ? null : phoneToUserId.get(String.valueOf(phoneRaw));
            if (userId == null) {
                noAccount++;
                continue;
            }
            NotificationEntity n = new NotificationEntity();
            n.setUserId(userId);
            n.setTitle(policy.getTitle());
            n.setContent(content != null ? content : "您有一项新政策可了解，点击查看政策详情。");
            n.setType("POLICY");
            n.setLevel("NORMAL");
            n.setRelatedType("POLICY");
            n.setRelatedId(policyId);
            n.setIsRead(0);
            notificationBatchWriter.enqueue(n);
            pushed++;
        }

        result.put("matched", people.size());
        result.put("pushed", pushed);
        result.put("noAccount", noAccount);
        result.put("message", String.format("推送完成：匹配 %d 人，成功推送 %d 人，%d 人无居民账号已跳过", people.size(), pushed, noAccount));
        return result;
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
