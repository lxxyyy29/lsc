package com.changping.platform.modules.community.controller;

import com.changping.platform.common.response.ApiResponse;
import com.changping.platform.modules.community.entity.GridEntity;
import com.changping.platform.modules.community.service.GridService;
import com.changping.platform.modules.community.vo.GridTreeVo;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/community/grids")
public class GridController {

    private final GridService gridService;

    public GridController(GridService gridService) {
        this.gridService = gridService;
    }

    @GetMapping("/tree")
    public ApiResponse<List<GridTreeVo>> tree() {
        return ApiResponse.ok(gridService.tree());
    }

    @GetMapping("/{id}")
    public ApiResponse<GridEntity> detail(@PathVariable Long id) {
        return ApiResponse.ok(gridService.detail(id));
    }

    @GetMapping("/{id}/children")
    public ApiResponse<List<GridEntity>> children(@PathVariable Long id) {
        return ApiResponse.ok(gridService.children(id));
    }

    @PostMapping
    public ApiResponse<Boolean> create(@RequestBody GridEntity entity) {
        return ApiResponse.ok(gridService.create(entity));
    }

    @PutMapping("/{id}")
    public ApiResponse<Boolean> update(@PathVariable Long id, @RequestBody GridEntity entity) {
        entity.setId(id);
        return ApiResponse.ok(gridService.updateGrid(entity));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Boolean> delete(@PathVariable Long id) {
        return ApiResponse.ok(gridService.delete(id));
    }
}
