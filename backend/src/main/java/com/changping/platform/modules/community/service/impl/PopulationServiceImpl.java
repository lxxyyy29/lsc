package com.changping.platform.modules.community.service.impl;

import com.changping.platform.common.exception.BusinessException;
import com.changping.platform.modules.community.entity.HouseholdEntity;
import com.changping.platform.modules.community.entity.PopulationEntity;
import com.changping.platform.modules.community.mapper.HouseholdMapper;
import com.changping.platform.modules.community.mapper.PopulationMapper;
import com.changping.platform.modules.community.service.HouseholdRelationResolver;
import com.changping.platform.modules.community.service.PopulationService;
import com.changping.platform.modules.community.vo.PopulationTreeVo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class PopulationServiceImpl implements PopulationService {

    private final PopulationMapper populationMapper;
    private final HouseholdMapper householdMapper;

    public PopulationServiceImpl(PopulationMapper populationMapper, HouseholdMapper householdMapper) {
        this.populationMapper = populationMapper;
        this.householdMapper = householdMapper;
    }

    @Override
    public List<PopulationEntity> list(Long gridId) {
        if (gridId != null) {
            return populationMapper.findByGridId(gridId);
        }
        return populationMapper.findAllActive();
    }

    @Override
    public List<PopulationEntity> search(String keyword, String householdType, Long gridId, String populationType) {
        return populationMapper.search(keyword, householdType, gridId, populationType);
    }

    @Override
    public List<PopulationTreeVo> tree(String keyword, String householdType, Long gridId) {
        List<PopulationEntity> rows = populationMapper.search(keyword, householdType, gridId, "RESIDENT");
        // 关键字搜索：命中成员后整户带出。已归户的按 household_id 扩展，未归户的回退按地址扩展，
        // 保证搜索结果展示整户（所有人）而非仅命中的单个成员。
        if (keyword != null && !keyword.isBlank() && !rows.isEmpty()) {
            List<PopulationEntity> noHouse = new ArrayList<>();
            Set<Long> hitHouseholdIds = new LinkedHashSet<>();
            Set<String> hitAddresses = new LinkedHashSet<>();
            for (PopulationEntity p : rows) {
                if (p.getHouseholdId() != null) {
                    hitHouseholdIds.add(p.getHouseholdId());
                } else {
                    String addr = blankToEmpty(p.getAddress());
                    if (addr.isEmpty()) {
                        noHouse.add(p);
                    } else {
                        hitAddresses.add(addr);
                    }
                }
            }
            List<PopulationEntity> expanded = new ArrayList<>(noHouse);
            if (!hitHouseholdIds.isEmpty()) {
                expanded.addAll(populationMapper.findByHouseholdIds(new ArrayList<>(hitHouseholdIds)));
            }
            if (!hitAddresses.isEmpty()) {
                expanded.addAll(populationMapper.findResidentsByAddresses(new ArrayList<>(hitAddresses), gridId));
            }
            rows = expanded;
        }

        List<PopulationTreeVo> houses = new ArrayList<>();
        Map<String, PopulationTreeVo> byKey = new LinkedHashMap<>();
        int houseIdx = 0;
        for (PopulationEntity p : rows) {
            String address = blankToEmpty(p.getAddress());
            String householdAddress = blankToEmpty(p.getHouseholdAddress());
            // 分组键：已归户按 household_id，未归户回退按地址
            String key = p.getHouseholdId() != null ? "h-" + p.getHouseholdId() : "a-" + address;
            PopulationTreeVo house = byKey.get(key);
            if (house == null) {
                house = new PopulationTreeVo();
                house.setId(p.getHouseholdId() != null ? "household-" + p.getHouseholdId() : "house-" + (houseIdx++));
                house.setHouseholdId(p.getHouseholdId());
                house.setHouseholdNo(p.getHouseholdNo());
                String label = !householdAddress.isEmpty() ? householdAddress : address;
                house.setLabel(label.isEmpty() ? "未填写地址" : label);
                house.setIsHouse(true);
                house.setAddress(label);
                house.setChildren(new ArrayList<>());
                byKey.put(key, house);
                houses.add(house);
            }
            boolean isHead = HouseholdRelationResolver.HEAD.equals(p.getRelation());
            PopulationTreeVo member = new PopulationTreeVo();
            member.setId("person-" + p.getId());
            member.setLabel(buildMemberLabel(p));
            member.setIsHouse(false);
            member.setIsHead(isHead);
            member.setPerson(p);
            house.getChildren().add(member);
            if (isHead && house.getHead() == null) {
                house.setHead(p);
            }
        }
        // 户主排本组首位（无户主组保持原序）
        for (PopulationTreeVo house : houses) {
            if (house.getHead() != null) {
                house.getChildren().sort((a, b) -> Boolean.compare(Boolean.TRUE.equals(b.getIsHead()),
                        Boolean.TRUE.equals(a.getIsHead())));
            }
        }
        return houses;
    }

    /** 成员节点展示文本：姓名（性别 年龄 · 关系），与 web 端树节点格式保持一致 */
    private String buildMemberLabel(PopulationEntity p) {
        StringBuilder sb = new StringBuilder(p.getName() == null || p.getName().isBlank() ? "-" : p.getName());
        sb.append("（").append(p.getGender() == null || p.getGender().isBlank() ? "未知" : p.getGender());
        if (p.getAge() != null) {
            sb.append(' ').append(p.getAge()).append("岁");
        }
        if (p.getRelation() != null && !p.getRelation().isBlank()) {
            sb.append(" · ").append(p.getRelation());
        }
        return sb.append('）').toString();
    }

    @Override
    public PopulationEntity detail(Long id) {
        return populationMapper.findById(id);
    }

    @Override
    @Transactional
    public boolean create(PopulationEntity entity) {
        if (entity.getStatus() == null || entity.getStatus().isBlank()) {
            entity.setStatus("ACTIVE");
        }
        Long headHouseholdId = applyHousehold(entity, true);
        Long newId = populationMapper.insert(entity);
        if (headHouseholdId != null && newId != null) {
            householdMapper.updateHead(headHouseholdId, newId);
        }
        return true;
    }

    @Override
    @Transactional
    public boolean update(PopulationEntity entity) {
        PopulationEntity current = entity.getId() == null ? null : populationMapper.findById(entity.getId());
        if ("FLOATING".equals(entity.getHouseholdType())) {
            // 改为流动人口时移出户
            entity.setHouseholdId(null);
            if (entity.getRelation() == null) {
                entity.setRelation("");
            }
        } else if (entity.getHouseholdId() == null && current != null) {
            // 编辑表单未携带所属户时保留原归属，避免全字段更新把 household_id 清空
            entity.setHouseholdId(current.getHouseholdId());
        }
        Long headHouseholdId = applyHousehold(entity, false);
        boolean ok = populationMapper.update(entity) > 0;
        if (ok && headHouseholdId != null) {
            householdMapper.updateHead(headHouseholdId, entity.getId());
        }
        return ok;
    }

    @Override
    @Transactional
    public boolean delete(Long id) {
        PopulationEntity p = populationMapper.findById(id);
        if (p == null) {
            return false;
        }
        boolean ok = populationMapper.deleteById(id) > 0;
        // 删除的是户主时清空户的户主指向，避免悬空引用
        if (ok && p.getHouseholdId() != null) {
            HouseholdEntity h = householdMapper.findById(p.getHouseholdId());
            if (h != null && id.equals(h.getHeadPopulationId())) {
                householdMapper.updateHead(p.getHouseholdId(), null);
            }
        }
        return ok;
    }

    /**
     * 归户处理与「一户一主」校验。
     *
     * @return 需要同步更新户主的户ID（该成员被设为户主时），否则 null
     */
    private Long applyHousehold(PopulationEntity entity, boolean isCreate) {
        Long householdId = entity.getHouseholdId();
        if (householdId == null) {
            return null;
        }
        HouseholdEntity house = householdMapper.findById(householdId);
        if (house == null || !"ACTIVE".equals(house.getStatus())) {
            throw new BusinessException("HOUSEHOLD_NOT_FOUND", "所选户不存在");
        }
        if ("FLOATING".equals(entity.getHouseholdType())) {
            throw new BusinessException("HOUSEHOLD_FLOATING_NOT_ALLOWED", "流动人口不参与归户");
        }
        String rel = entity.getRelation() == null || entity.getRelation().isBlank() ? null : entity.getRelation().trim();
        if (rel == null) {
            // 户内无户主时，第一个加入的成员自动成为户主
            rel = house.getHeadPopulationId() == null
                    ? HouseholdRelationResolver.HEAD : HouseholdRelationResolver.OTHER;
        }
        boolean isHead = HouseholdRelationResolver.HEAD.equals(rel);
        if (isHead && house.getHeadPopulationId() != null
                && (isCreate || !house.getHeadPopulationId().equals(entity.getId()))) {
            throw new BusinessException("HOUSEHOLD_HEAD_EXISTS", "该户已有户主，如需更换请使用「变更户主」");
        }
        entity.setRelation(rel);
        // 未填地址/网格时继承户上的值，保持成员与户一致
        if (entity.getAddress() == null || entity.getAddress().isBlank()) {
            entity.setAddress(house.getAddress());
        }
        if (entity.getGridId() == null) {
            entity.setGridId(house.getGridId());
        }
        return isHead ? householdId : null;
    }

    private static String blankToEmpty(String s) {
        return s == null || s.isBlank() ? "" : s.trim();
    }
}
