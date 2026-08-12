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
        GridEntity parent = null;
        if (entity.getParentId() == null && entity.getGridLevel() == null) {
            entity.setGridLevel(1);
        } else if (entity.getParentId() != null) {
            parent = gridMapper.findById(entity.getParentId());
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
        if (entity.getGridCode() == null || entity.getGridCode().isBlank()) {
            entity.setGridCode(generateGridCode(parent, entity.getParentId()));
        }
        gridMapper.insert(entity);
        return true;
    }

    @Override
    public boolean updateGrid(GridEntity entity) {
        if (entity.getId() == null) {
            throw new BusinessException("GRID_ID_REQUIRED", "网格ID不能为空");
        }
        GridEntity exists = gridMapper.findById(entity.getId());
        // 未传字段保留原值，避免被覆盖为空（编辑网格时只传可改字段）
        if (entity.getGridCode() == null || entity.getGridCode().isBlank()) {
            entity.setGridCode(exists.getGridCode());
        }
        if (entity.getGridLevel() == null) {
            entity.setGridLevel(exists.getGridLevel());
        }
        if (entity.getParentId() == null) {
            entity.setParentId(exists.getParentId());
        }
        if (entity.getPopulation() == null) {
            entity.setPopulation(exists.getPopulation());
        }
        if (entity.getBuildingCount() == null) {
            entity.setBuildingCount(exists.getBuildingCount());
        }
        if (entity.getSortOrder() == null) {
            entity.setSortOrder(exists.getSortOrder());
        }
        if (entity.getStatus() == null || entity.getStatus().isBlank()) {
            entity.setStatus(exists.getStatus());
        }
        return gridMapper.update(entity) > 0;
    }

    /**
     * 按现有编码规则自动生成网格编码：
     * 顶层 BJW-001；level2 子网格 BJW-G01；level3 子网格 BJW-G01-A；更深层级 父code-NN
     */
    private String generateGridCode(GridEntity parent, Long parentId) {
        long siblingCount = gridMapper.countChildren(parentId == null ? -1L : parentId);
        if (parent == null) {
            return String.format("BJW-%03d", siblingCount + 1);
        }
        String parentCode = parent.getGridCode();
        if (parent.getGridLevel() == 1) {
            return String.format("%s-G%02d", parentCode, siblingCount + 1);
        }
        if (parent.getGridLevel() == 2 && siblingCount < 26) {
            return String.format("%s-%c", parentCode, (char) ('A' + siblingCount));
        }
        return String.format("%s-%02d", parentCode, siblingCount + 1);
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
