package com.changping.platform.modules.community.service.impl;

import com.changping.platform.common.exception.BusinessException;
import com.changping.platform.modules.community.entity.GridEntity;
import com.changping.platform.modules.community.mapper.GridMapper;
import com.changping.platform.modules.community.service.GridService;
import com.changping.platform.modules.community.vo.GridTreeVo;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class GridServiceImpl implements GridService {

    private final GridMapper gridMapper;

    public GridServiceImpl(GridMapper gridMapper) {
        this.gridMapper = gridMapper;
    }

    @Override
    public List<GridTreeVo> tree() {
        List<GridEntity> all = gridMapper.findAllActive();
        return buildTree(all, null);
    }

    @Override
    public List<GridEntity> children(Long parentId) {
        return gridMapper.findByParentId(parentId);
    }

    @Override
    public GridEntity detail(Long id) {
        return gridMapper.findById(id);
    }

    @Override
    public boolean create(GridEntity entity) {
        if (entity.getParentId() == null && entity.getGridLevel() == null) {
            entity.setGridLevel(1);
        } else if (entity.getParentId() != null) {
            GridEntity parent = gridMapper.findById(entity.getParentId());
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
        gridMapper.insert(entity);
        return true;
    }

    @Override
    public boolean updateGrid(GridEntity entity) {
        if (entity.getId() == null) {
            throw new BusinessException("GRID_ID_REQUIRED", "网格ID不能为空");
        }
        return gridMapper.update(entity) > 0;
    }

    @Override
    public boolean delete(Long id) {
        if (gridMapper.countChildren(id) > 0) {
            throw new BusinessException("GRID_HAS_CHILDREN", "该网格下存在子网格，无法删除");
        }
        return gridMapper.deleteById(id) > 0;
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
