package com.changping.platform.modules.community.service.impl;

import com.changping.platform.modules.community.entity.PopulationEntity;
import com.changping.platform.modules.community.mapper.PopulationMapper;
import com.changping.platform.modules.community.service.PopulationService;
import com.changping.platform.modules.community.vo.PopulationTreeVo;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class PopulationServiceImpl implements PopulationService {

    private final PopulationMapper populationMapper;

    public PopulationServiceImpl(PopulationMapper populationMapper) {
        this.populationMapper = populationMapper;
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
        List<PopulationTreeVo> houses = new ArrayList<>();
        Map<String, PopulationTreeVo> byAddress = new LinkedHashMap<>();
        int houseIdx = 0;
        for (PopulationEntity p : rows) {
            String addr = p.getAddress() == null || p.getAddress().isBlank() ? "" : p.getAddress().trim();
            PopulationTreeVo house = byAddress.get(addr);
            if (house == null) {
                house = new PopulationTreeVo();
                house.setId("house-" + (houseIdx++));
                house.setLabel(addr.isEmpty() ? "未填写地址" : addr);
                house.setIsHouse(true);
                house.setAddress(addr);
                house.setChildren(new ArrayList<>());
                byAddress.put(addr, house);
                houses.add(house);
            }
            boolean isHead = "户主".equals(p.getRelation());
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
    public boolean create(PopulationEntity entity) {
        if (entity.getStatus() == null) {
            entity.setStatus("ACTIVE");
        }
        populationMapper.insert(entity);
        return true;
    }

    @Override
    public boolean update(PopulationEntity entity) {
        return populationMapper.update(entity) > 0;
    }

    @Override
    public boolean delete(Long id) {
        return populationMapper.deleteById(id) > 0;
    }
}
