package com.changping.platform.modules.community.service;

import com.changping.platform.modules.community.entity.OrgMemberEntity;
import java.util.List;

public interface OrgMemberService {
    List<OrgMemberEntity> list(Long gridId);
    OrgMemberEntity detail(Long id);
    boolean create(OrgMemberEntity entity);
    boolean update(OrgMemberEntity entity);
    boolean delete(Long id);
    /** 组长候选人列表（职务含组长/网格长或社区领导） */
    List<OrgMemberEntity> leaderCandidates();
    /** 批量将成员划分到某组长名下（leaderId 为 null 取消划分） */
    int assignLeader(List<Long> memberIds, Long leaderId);
}
