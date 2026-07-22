package com.changping.platform.modules.community.service;

import com.changping.platform.modules.community.entity.PlaceEntity;
import java.util.List;

public interface PlaceService {
    List<PlaceEntity> list(Long gridId);
    PlaceEntity detail(Long id);
    boolean create(PlaceEntity entity);
    boolean update(PlaceEntity entity);
    boolean delete(Long id);
}
