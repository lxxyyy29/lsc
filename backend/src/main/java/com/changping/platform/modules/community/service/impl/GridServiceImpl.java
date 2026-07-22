package com.changping.platform.modules.community.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.changping.platform.common.exception.BusinessException;
import com.changping.platform.modules.community.entity.GridEntity;
import com.changping.platform.modules.community.mapper.GridMapper;
import com.changping.platform.modules.community.service.GridService;
import com.changping.platform.modules.community.vo.GridTreeVo;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class GridServiceImpl extends ServiceImpl<GridMapper, GridEntity> implements GridService {

    @Override
    public List<GridTreeVo> tree() {
        List<GridEntity> all = list(new LambdaQueryWrapper<GridEntity>()
                .orderByAsc(GridEntity::getSortOrder)
                .orderByAsc(GridEntity::getId));

        return buildTree(all, null);
    }

    @Override
    public List<GridEntity> children(Long parentId) {
        return list(new LambdaQueryWrapper<GridEntity>()
                .eq(GridEntity::getParentId, parentId)
                .orderByAsc(GridEntity::getSortOrder));
    }

    @Override
    public GridEntity detail(Long id) {
        return getById(id);
    }

    @Override
    public boolean create(GridEntity entity) {
        if (entity.getParentId() == null && entity.getGridLevel() == null) {
            entity.setGridLevel(1);
        } else if (entity.getParentId() != null) {
            GridEntity parent = getById(entity.getParentId());
            if (parent == null) {
                throw new BusinessException("GRID_PARENT_NOT_FOUND", "父网格不存在");
            }
            entity.setGridLevel(parent.getGridLevel() + 1);
        }
        if (entity.getStatus() == null) {
            entity.setStatus("ACTIVE");
        }
        if (entity.getSortOrder() == null) {
            entity.setSortOrder(0);
        }
        return save(entity);
    }

    @Override
    public boolean updateGrid(GridEntity entity) {
        if (entity.getId() == null) {
            throw new BusinessException("GRID_ID_REQUIRED", "网格ID不能为空");
        }
        return updateById(entity);
    }

    @Override
    public boolean delete(Long id) {
        long childCount = count(new LambdaQueryWrapper<GridEntity>().eq(GridEntity::getParentId, id));
        if (childCount > 0) {
            throw new BusinessException("GRID_HAS_CHILDREN", "该网格下存在子网格，无法删除");
        }
        return removeById(id);
    }

    private List<GridTreeVo> buildTree(List<GridEntity> all, Long parentId) {
        return all.stream()
                .filter(g -> (parentId == null && g.getParentId() == null)
                        || (parentId != null && parentId.equals(g.getParentId())))
                .map(g -> {
                    GridTreeVo vo = new GridTreeVo();
                    BeanUtils.copyProperties(g, vo);
                    vo.setChildren(buildTree(all, g.getId()));
                    return vo;
                })
                .sorted(Comparator.comparing(GridTreeVo::getSortOrder, Comparator.nullsLast(Integer::compareTo)))
                .collect(Collectors.toList());
    }
}
