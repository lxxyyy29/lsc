package com.changping.platform.modules.community.service.impl;

import com.changping.platform.common.exception.BusinessException;
import com.changping.platform.modules.community.entity.HouseholdEntity;
import com.changping.platform.modules.community.entity.PopulationEntity;
import com.changping.platform.modules.community.mapper.HouseholdMapper;
import com.changping.platform.modules.community.mapper.PopulationMapper;
import com.changping.platform.modules.community.service.HouseholdRelationResolver;
import com.changping.platform.modules.community.service.HouseholdService;
import com.changping.platform.modules.community.vo.HouseholdVo;
import com.changping.platform.modules.community.vo.MemberChangeVo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class HouseholdServiceImpl implements HouseholdService {

    private final HouseholdMapper householdMapper;
    private final PopulationMapper populationMapper;

    public HouseholdServiceImpl(HouseholdMapper householdMapper, PopulationMapper populationMapper) {
        this.householdMapper = householdMapper;
        this.populationMapper = populationMapper;
    }

    @Override
    public List<HouseholdVo> list(String keyword, Long gridId) {
        return householdMapper.list(keyword, gridId);
    }

    @Override
    public Map<String, Object> detail(Long id) {
        HouseholdEntity h = requireHousehold(id);
        List<PopulationEntity> members = populationMapper.findByHouseholdId(id);
        HouseholdVo vo = toVo(h, members);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("household", vo);
        result.put("members", members);
        return result;
    }

    @Override
    @Transactional
    public Long create(HouseholdEntity entity) {
        if (entity.getAddress() == null || entity.getAddress().isBlank()) {
            throw new BusinessException("HOUSEHOLD_ADDRESS_REQUIRED", "请填写户地址");
        }
        entity.setAddress(entity.getAddress().trim());
        if (entity.getStatus() == null || entity.getStatus().isBlank()) {
            entity.setStatus("ACTIVE");
        }
        Long exist = householdMapper.findIdByAddress(entity.getGridId(), entity.getAddress());
        if (exist != null) {
            throw new BusinessException("HOUSEHOLD_DUPLICATE", "该网格下已存在相同地址的户（户ID：" + exist + "）");
        }
        return householdMapper.insert(entity);
    }

    @Override
    @Transactional
    public boolean update(HouseholdEntity entity) {
        HouseholdEntity current = requireHousehold(entity.getId());
        if (entity.getAddress() != null && !entity.getAddress().isBlank()) {
            entity.setAddress(entity.getAddress().trim());
            Long exist = householdMapper.findIdByAddress(entity.getGridId(), entity.getAddress());
            if (exist != null && !exist.equals(entity.getId())) {
                throw new BusinessException("HOUSEHOLD_DUPLICATE", "该网格下已存在相同地址的户（户ID：" + exist + "）");
            }
        } else {
            entity.setAddress(current.getAddress());
        }
        if (entity.getGridId() == null) {
            entity.setGridId(current.getGridId());
        }
        if (entity.getStatus() == null || entity.getStatus().isBlank()) {
            entity.setStatus(current.getStatus());
        }
        boolean ok = householdMapper.update(entity) > 0;
        // 户地址/网格变更后同步到户内成员，避免成员地址与户不一致
        populationMapper.syncHouseholdFields(entity.getId(), entity.getAddress(), entity.getGridId());
        return ok;
    }

    @Override
    @Transactional
    public boolean delete(Long id) {
        requireHousehold(id);
        int count = householdMapper.countActiveMembers(id);
        if (count > 0) {
            throw new BusinessException("HOUSEHOLD_NOT_EMPTY", "该户还有 " + count + " 名成员，请先移出成员");
        }
        return householdMapper.deleteById(id) > 0;
    }

    @Override
    @Transactional
    public void addMember(Long householdId, Long populationId, String relation) {
        HouseholdEntity h = requireHousehold(householdId);
        PopulationEntity p = populationMapper.findById(populationId);
        if (p == null) {
            throw new BusinessException("POPULATION_NOT_FOUND", "人员不存在");
        }
        if ("FLOATING".equals(p.getHouseholdType())) {
            throw new BusinessException("HOUSEHOLD_FLOATING_NOT_ALLOWED", "流动人口不参与归户");
        }
        String rel = relation == null || relation.isBlank() ? null : relation.trim();
        if (rel == null) {
            rel = h.getHeadPopulationId() == null ? HouseholdRelationResolver.HEAD : HouseholdRelationResolver.OTHER;
        }
        if (HouseholdRelationResolver.HEAD.equals(rel)
                && h.getHeadPopulationId() != null
                && !h.getHeadPopulationId().equals(populationId)) {
            throw new BusinessException("HOUSEHOLD_HEAD_EXISTS", "该户已有户主，如需更换请使用「变更户主」");
        }
        populationMapper.updateHousehold(populationId, householdId, rel, h.getAddress(), h.getGridId());
        if (HouseholdRelationResolver.HEAD.equals(rel)) {
            householdMapper.updateHead(householdId, populationId);
        }
    }

    @Override
    @Transactional
    public void removeMember(Long householdId, Long populationId) {
        HouseholdEntity h = requireHousehold(householdId);
        PopulationEntity p = populationMapper.findById(populationId);
        if (p == null || !householdId.equals(p.getHouseholdId())) {
            throw new BusinessException("HOUSEHOLD_MEMBER_NOT_FOUND", "该人员不属于此户");
        }
        populationMapper.clearHousehold(populationId);
        if (h.getHeadPopulationId() != null && h.getHeadPopulationId().equals(populationId)) {
            householdMapper.updateHead(householdId, null);
        }
    }

    @Override
    public List<MemberChangeVo> previewChangeHead(Long householdId, Long newHeadId, String relationToOldHead) {
        HouseholdEntity h = requireHousehold(householdId);
        List<PopulationEntity> members = populationMapper.findByHouseholdId(householdId);
        if (members.isEmpty()) {
            throw new BusinessException("HOUSEHOLD_EMPTY", "该户暂无成员");
        }
        PopulationEntity newHead = findMember(members, newHeadId);
        PopulationEntity oldHead = resolveOldHead(h, members);
        return buildChangePlan(members, oldHead, newHead, relationToOldHead);
    }

    @Override
    @Transactional
    public Map<String, Object> changeHead(Long householdId, Long newHeadId, String relationToOldHead) {
        HouseholdEntity h = requireHousehold(householdId);
        List<PopulationEntity> members = populationMapper.findByHouseholdId(householdId);
        if (members.isEmpty()) {
            throw new BusinessException("HOUSEHOLD_EMPTY", "该户暂无成员");
        }
        PopulationEntity newHead = findMember(members, newHeadId);
        PopulationEntity oldHead = resolveOldHead(h, members);

        List<MemberChangeVo> plan = buildChangePlan(members, oldHead, newHead, relationToOldHead);
        for (MemberChangeVo c : plan) {
            if (Boolean.TRUE.equals(c.getChanged())) {
                populationMapper.updateRelation(c.getId(), c.getNewRelation());
            }
        }
        householdMapper.updateHead(householdId, newHeadId);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("householdId", householdId);
        result.put("newHeadId", newHeadId);
        result.put("oldHeadId", oldHead == null ? null : oldHead.getId());
        result.put("members", plan);
        // 存在需要人工核对的成员（推算结果为「其他」，或原户主缺失导致无法推算）
        result.put("manualReview", plan.stream().anyMatch(c -> HouseholdRelationResolver.OTHER.equals(c.getNewRelation())));
        return result;
    }

    @Override
    @Transactional
    public Long ensureHousehold(Long gridId, String address, String householdType) {
        if (address == null || address.isBlank()) {
            return null;
        }
        String addr = address.trim();
        Long exist = householdMapper.findIdByAddress(gridId, addr);
        if (exist != null) {
            return exist;
        }
        HouseholdEntity h = new HouseholdEntity();
        h.setAddress(addr);
        h.setGridId(gridId);
        h.setHouseholdType(householdType);
        h.setStatus("ACTIVE");
        return householdMapper.insert(h);
    }

    // ==================== 内部方法 ====================

    /**
     * 计算户主变更后每个成员的新关系。
     * anchorRelation 为空时，用新户主当前 relation 自动推断（其原本就是相对旧户主的称谓）。
     */
    private List<MemberChangeVo> buildChangePlan(List<PopulationEntity> members, PopulationEntity oldHead,
            PopulationEntity newHead, String relationToOldHead) {
        boolean oldHeadValid = oldHead != null && !oldHead.getId().equals(newHead.getId());
        String anchor = relationToOldHead == null || relationToOldHead.isBlank() ? null : relationToOldHead.trim();
        if (anchor == null && oldHeadValid) {
            anchor = HouseholdRelationResolver.inferAnchorRelation(newHead.getRelation());
        }
        if (oldHeadValid && anchor == null) {
            throw new BusinessException("HOUSEHOLD_ANCHOR_REQUIRED", "请指定新户主与原户主的关系");
        }
        boolean canResolve = oldHeadValid && anchor != null;
        String oldHeadGender = oldHead == null ? null : oldHead.getGender();

        List<MemberChangeVo> plan = new ArrayList<>();
        for (PopulationEntity m : members) {
            MemberChangeVo c = new MemberChangeVo();
            c.setId(m.getId());
            c.setName(m.getName());
            c.setGender(m.getGender());
            c.setOldRelation(m.getRelation());
            boolean isNewHead = m.getId().equals(newHead.getId());
            c.setNewHead(isNewHead);

            String newRelation;
            if (isNewHead) {
                newRelation = HouseholdRelationResolver.HEAD;
            } else if (canResolve) {
                boolean isOldHead = m.getId().equals(oldHead.getId());
                newRelation = HouseholdRelationResolver.resolve(
                        m.getRelation(), isOldHead, anchor, oldHeadGender, m.getGender());
            } else {
                // 原户主缺失，无从推算：保留原关系，由操作员人工核对
                newRelation = m.getRelation();
            }
            c.setNewRelation(newRelation);
            c.setChanged(!Objects.equals(trim(m.getRelation()), trim(newRelation)));
            plan.add(c);
        }
        return plan;
    }

    /** 户主：优先取户表 head_population_id，回退取首个 relation='户主' 的成员 */
    private PopulationEntity resolveOldHead(HouseholdEntity h, List<PopulationEntity> members) {
        if (h != null && h.getHeadPopulationId() != null) {
            for (PopulationEntity m : members) {
                if (m.getId().equals(h.getHeadPopulationId())) {
                    return m;
                }
            }
        }
        for (PopulationEntity m : members) {
            if (HouseholdRelationResolver.HEAD.equals(m.getRelation())) {
                return m;
            }
        }
        return null;
    }

    private PopulationEntity findMember(List<PopulationEntity> members, Long id) {
        if (id == null) {
            throw new BusinessException("HOUSEHOLD_MEMBER_REQUIRED", "请选择新户主");
        }
        for (PopulationEntity m : members) {
            if (m.getId().equals(id)) {
                return m;
            }
        }
        throw new BusinessException("HOUSEHOLD_MEMBER_NOT_FOUND", "所选人员不属于该户");
    }

    private HouseholdEntity requireHousehold(Long id) {
        HouseholdEntity h = id == null ? null : householdMapper.findById(id);
        if (h == null || !"ACTIVE".equals(h.getStatus())) {
            throw new BusinessException("HOUSEHOLD_NOT_FOUND", "户不存在");
        }
        return h;
    }

    private HouseholdVo toVo(HouseholdEntity h, List<PopulationEntity> members) {
        HouseholdVo vo = new HouseholdVo();
        vo.setId(h.getId());
        vo.setHouseholdNo(h.getHouseholdNo());
        vo.setAddress(h.getAddress());
        vo.setGridId(h.getGridId());
        vo.setHouseholdType(h.getHouseholdType());
        vo.setRemark(h.getRemark());
        vo.setMemberCount(members.size());
        long headCount = members.stream()
                .filter(m -> HouseholdRelationResolver.HEAD.equals(m.getRelation())).count();
        PopulationEntity head = resolveOldHead(h, members);
        if (head != null) {
            vo.setHeadPopulationId(head.getId());
            vo.setHeadName(head.getName());
            vo.setHeadGender(head.getGender());
        }
        vo.setHasHead(head != null);
        vo.setMultipleHeads(headCount > 1);
        return vo;
    }

    private static String trim(String s) {
        return s == null ? null : s.trim();
    }
}
