package com.changping.platform.modules.community.service.impl;

import com.changping.platform.modules.community.entity.OrgMemberEntity;
import com.changping.platform.modules.community.mapper.OrgMemberMapper;
import com.changping.platform.modules.community.service.OrgMemberService;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class OrgMemberServiceImpl implements OrgMemberService {

    private final OrgMemberMapper mapper;
    public OrgMemberServiceImpl(OrgMemberMapper mapper) { this.mapper = mapper; }

    @Override
    public List<OrgMemberEntity> list(Long gridId) {
        return gridId != null ? mapper.findByGridId(gridId) : mapper.findAllActive();
    }
    @Override
    public OrgMemberEntity detail(Long id) { return mapper.findById(id); }
    @Override
    public boolean create(OrgMemberEntity e) {
        if (e.getStatus() == null) e.setStatus("ACTIVE");
        fillDefaultPosition(e);
        mapper.insert(e);
        return true;
    }
    @Override
    public boolean update(OrgMemberEntity e) {
        fillDefaultPosition(e);
        return mapper.update(e) > 0;
    }
    @Override
    public boolean delete(Long id) { return mapper.deleteById(id) > 0; }

    @Override
    public List<OrgMemberEntity> leaderCandidates() { return mapper.findLeaderCandidates(); }

    @Override
    public int assignLeader(List<Long> memberIds, Long leaderId) { return mapper.assignLeader(memberIds, leaderId); }

    @Override
    public Long createLeader(OrgMemberEntity e) {
        if (e.getStatus() == null) e.setStatus("ACTIVE");
        e.setMemberType("LEADER");
        if (e.getPosition() == null || e.getPosition().isBlank()) e.setPosition("网格长");
        return mapper.insert(e);
    }

    @Override
    public int assignGridWorkersToLeader(Long leaderId, Long gridId) {
        return mapper.assignGridWorkersToLeader(leaderId, gridId);
    }

    @Override
    public int countGridWorkers(Long gridId) {
        return mapper.countGridWorkers(gridId);
    }

    private void fillDefaultPosition(OrgMemberEntity e) {
        if (e.getPosition() != null && !e.getPosition().isBlank()) {
            return;
        }
        if ("GRID_WORKER".equals(e.getMemberType())) {
            e.setPosition("网格员");
        }
    }
}
