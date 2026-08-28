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
    /** 创建组长并绑定网格，返回新增成员 ID；调用方随后用 assignGridWorkersToLeader 自动划分下属 */
    Long createLeader(OrgMemberEntity entity);
    /** 绑定网格时自动划分：该网格下全部在岗网格员划入组长名下，返回影响行数 */
    int assignGridWorkersToLeader(Long leaderId, Long gridId);
    /** 统计某网格下在岗网格员数量（用于前端创建组长时预提示） */
    int countGridWorkers(Long gridId);
}
