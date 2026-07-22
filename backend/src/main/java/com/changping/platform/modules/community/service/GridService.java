package com.changping.platform.modules.community.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.changping.platform.modules.community.entity.GridEntity;
import com.changping.platform.modules.community.vo.GridTreeVo;

import java.util.List;

public interface GridService extends IService<GridEntity> {

    List<GridTreeVo> tree();

    List<GridEntity> children(Long parentId);

    GridEntity detail(Long id);

    boolean create(GridEntity entity);

    boolean updateGrid(GridEntity entity);

    boolean delete(Long id);
}
