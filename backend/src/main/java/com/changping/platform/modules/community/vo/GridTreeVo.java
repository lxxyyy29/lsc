package com.changping.platform.modules.community.vo;

import com.changping.platform.modules.community.entity.GridEntity;
import lombok.Data;

import java.util.List;

@Data
public class GridTreeVo extends GridEntity {

    private List<GridTreeVo> children;
}
