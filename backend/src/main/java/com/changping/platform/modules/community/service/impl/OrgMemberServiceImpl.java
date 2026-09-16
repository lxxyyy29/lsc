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
        // 回填主键：Controller 需要用它把新建的系统账号回写到 cmn_org_member.sys_user_id
        // （此前丢弃返回值，导致组织人员与账号脱钩、编辑岗位时角色同步被跳过）
        e.setId(mapper.insert(e));
        return true;
    }
    @Override
    public boolean update(OrgMemberEntity e) {
        // 未传字段保留原值（与人口/网格/场所等模块一致）。
        // 原实现会把未传字段直接写成 NULL：部分更新会撞 name NOT NULL 报 500，
        // 且 sys_user_id 被清空后组织人员与系统账号脱钩，后续岗位改角色同步全部失效。
        OrgMemberEntity old = e.getId() == null ? null : mapper.findById(e.getId());
        if (old != null) {
            if (e.getName() == null) e.setName(old.getName());
            if (e.getPhone() == null) e.setPhone(old.getPhone());
            if (e.getMemberType() == null) e.setMemberType(old.getMemberType());
            if (e.getPosition() == null) e.setPosition(old.getPosition());
            if (e.getGridId() == null) e.setGridId(old.getGridId());
            if (e.getStatus() == null) e.setStatus(old.getStatus());
            if (e.getSysUserId() == null) e.setSysUserId(old.getSysUserId());
            // 注意：remark / leader_id 不清空补旧值 —— leaderId=null 是「取消划分」的合法语义
        }
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
