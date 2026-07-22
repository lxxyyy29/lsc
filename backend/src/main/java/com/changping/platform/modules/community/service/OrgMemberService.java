package com.changping.platform.modules.community.service;

import com.changping.platform.modules.community.entity.OrgMemberEntity;
import java.util.List;

public interface OrgMemberService {
    List<OrgMemberEntity> list(Long gridId);
    OrgMemberEntity detail(Long id);
    boolean create(OrgMemberEntity entity);
    boolean update(OrgMemberEntity entity);
    boolean delete(Long id);
}
