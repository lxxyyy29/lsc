package com.changping.platform.modules.community.service.impl;

import com.changping.platform.common.exception.BusinessException;
import com.changping.platform.modules.community.entity.GridEntity;
import com.changping.platform.modules.community.mapper.GridMapper;
import com.changping.platform.modules.community.service.GridService;
import com.changping.platform.modules.community.vo.GridTreeVo;
import org.springframework.beans.BeanUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class GridServiceImpl implements GridService {

    /**
     * 引用网格的业务表 → 提示用的中文名（顺序即提示顺序）。
     * cmn_grid 上有外键引用，被引用时无法直接删除；此处用于删除前给出可读提示，
     * 与 GridMapper.countReferences 的表清单保持一致。
     */
    private static final Map<String, String> GRID_REFERENCE_LABELS = new LinkedHashMap<>();

    static {
        GRID_REFERENCE_LABELS.put("biz_event", "事件");
        GRID_REFERENCE_LABELS.put("cmn_population", "人口");
        GRID_REFERENCE_LABELS.put("cmn_building", "房屋");
        GRID_REFERENCE_LABELS.put("cmn_place", "场所");
        GRID_REFERENCE_LABELS.put("cmn_household", "户");
        GRID_REFERENCE_LABELS.put("cmn_resident_report", "居民上报");
        GRID_REFERENCE_LABELS.put("cmn_patrol_record", "巡查记录");
        GRID_REFERENCE_LABELS.put("cmn_patrol_task", "巡查任务");
        GRID_REFERENCE_LABELS.put("cmn_org_member", "组织人员");
    }

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
        } else if (gridMapper.existsByCode(entity.getGridCode())) {
            throw new BusinessException("GRID_CODE_DUPLICATE", "网格编码「" + entity.getGridCode() + "」已被占用，请换一个或留空自动编号");
        }
        // 同级重名校验：grid_name 无唯一索引，重名会在网格树上出现重复节点
        if (entity.getGridName() != null && !entity.getGridName().isBlank()
                && gridMapper.existsSameNameUnderParent(entity.getGridName().trim(), entity.getParentId(), null)) {
            throw new BusinessException("GRID_NAME_DUPLICATE",
                    "同级下已存在名为「" + entity.getGridName().trim() + "」的网格，请换一个名称");
        }
        try {
            gridMapper.insert(entity);
        } catch (DuplicateKeyException e) {
            // 并发或唯一索引兜底：转为可读业务错，避免前端只看到 500
            throw new BusinessException("GRID_CODE_DUPLICATE", "网格编码「" + entity.getGridCode() + "」已被占用，请换一个或留空自动编号");
        }
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
        } else if (!entity.getGridCode().equals(exists.getGridCode()) && gridMapper.existsByCode(entity.getGridCode())) {
            throw new BusinessException("GRID_CODE_DUPLICATE", "网格编码「" + entity.getGridCode() + "」已被占用，请换一个");
        }
        if (entity.getGridLevel() == null) {
            entity.setGridLevel(exists.getGridLevel());
        }
        if (entity.getParentId() != null && !entity.getParentId().equals(exists.getParentId())) {
            // 父级调整校验：不能指向自身，也不能指向自己的下级，否则 parent_id 成环，
            // 后续按层级递归收集子网格/树查询会无限递归（栈溢出）
            if (entity.getParentId().equals(entity.getId())) {
                throw new BusinessException("GRID_PARENT_INVALID", "网格不能以自身作为父级");
            }
            try {
                GridEntity parent = gridMapper.findById(entity.getParentId());
                if (parent == null) {
                    throw new BusinessException("GRID_PARENT_NOT_FOUND", "父网格不存在");
                }
            } catch (org.springframework.dao.EmptyResultDataAccessException e) {
                throw new BusinessException("GRID_PARENT_NOT_FOUND", "父网格不存在");
            }
            if (gridMapper.collectSubtreeIds(entity.getId()).contains(entity.getParentId())) {
                throw new BusinessException("GRID_PARENT_INVALID", "父网格不能是当前网格的下级网格");
            }
        } else if (entity.getParentId() == null) {
            entity.setParentId(exists.getParentId());
        }
        // 同级重名校验（排除自身）：改名或改父级都可能撞上同级同名网格
        if (entity.getGridName() != null && !entity.getGridName().isBlank()
                && gridMapper.existsSameNameUnderParent(entity.getGridName().trim(), entity.getParentId(), entity.getId())) {
            throw new BusinessException("GRID_NAME_DUPLICATE",
                    "同级下已存在名为「" + entity.getGridName().trim() + "」的网格，请换一个名称");
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
     * 顶层 BJW-001；level2 子网格 BJW-G01；level3 子网格 BJW-G01-A；更深层级 父code-NN。
     * 序号从“兄弟数+1”开始递增，跳过已被占用的编码（删除过的网格会留下空位，防止撞唯一索引）
     */
    private String generateGridCode(GridEntity parent, Long parentId) {
        long siblingCount = gridMapper.countChildren(parentId == null ? -1L : parentId);
        long seq = siblingCount + 1;
        for (int attempt = 0; attempt < 1000; attempt++) {
            String candidate;
            if (parent == null) {
                candidate = String.format("BJW-%03d", seq);
            } else if (parent.getGridLevel() == 1) {
                candidate = String.format("%s-G%02d", parent.getGridCode(), seq);
            } else if (parent.getGridLevel() == 2 && seq <= 26) {
                candidate = String.format("%s-%c", parent.getGridCode(), (char) ('A' + seq - 1));
            } else {
                candidate = String.format("%s-%02d", parent.getGridCode(), seq);
            }
            if (!gridMapper.existsByCode(candidate)) {
                return candidate;
            }
            seq++;
        }
        throw new BusinessException("GRID_CODE_EXHAUSTED", "自动编号尝试次数过多，请手动指定网格编码");
    }

    @Override
    public boolean delete(Long id) {
        if (gridMapper.countChildren(id) > 0) {
            throw new BusinessException("GRID_HAS_CHILDREN", "该网格下存在子网格，无法删除");
        }
        // cmn_grid 被人口/房屋/场所/事件/组织人员等表通过 grid_id 外键引用。
        // 不先校验就直接 DELETE，会抛外键约束异常，被全局兜底成 500「服务器内部错误」——
        // 用户完全不知道是哪些数据挡住了。这里先统计引用量，给出可读提示。
        String references = describeReferences(gridMapper.countReferences(id));
        if (references != null) {
            throw new BusinessException("GRID_IN_USE",
                    "该网格下还有" + references + "，请先迁移或删除这些数据后再删除网格");
        }
        try {
            return gridMapper.deleteById(id) > 0;
        } catch (DataIntegrityViolationException e) {
            // 兜底：表清单若有遗漏（新增了引用网格的表），仍返回可读错误而不是 500
            throw new BusinessException("GRID_IN_USE", "该网格仍被其它业务数据引用，无法删除");
        }
    }

    /** 汇总引用明细，如「人口 3 条、事件 1 条」；无引用返回 null */
    private String describeReferences(Map<String, Long> references) {
        List<String> parts = new ArrayList<>();
        for (Map.Entry<String, String> entry : GRID_REFERENCE_LABELS.entrySet()) {
            Long count = references.get(entry.getKey());
            if (count != null && count > 0) {
                parts.add(entry.getValue() + " " + count + " 条");
            }
        }
        return parts.isEmpty() ? null : String.join("、", parts);
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
