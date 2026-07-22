package com.changping.platform.modules.community.controller;

import com.changping.platform.common.response.ApiResponse;
import com.changping.platform.modules.community.entity.PopulationEntity;
import com.changping.platform.modules.community.service.PopulationService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/community/population")
public class PopulationController {

    private final PopulationService populationService;

    public PopulationController(PopulationService populationService) {
        this.populationService = populationService;
    }

    @GetMapping
    public ApiResponse<List<PopulationEntity>> list(@RequestParam(required = false) Long gridId) {
        return ApiResponse.ok(populationService.list(gridId));
    }

    @GetMapping("/{id}")
    public ApiResponse<PopulationEntity> detail(@PathVariable Long id) {
        return ApiResponse.ok(populationService.detail(id));
    }

    @PostMapping
    public ApiResponse<Boolean> create(@RequestBody PopulationEntity entity) {
        return ApiResponse.ok(populationService.create(entity));
    }

    @PutMapping("/{id}")
    public ApiResponse<Boolean> update(@PathVariable Long id, @RequestBody PopulationEntity entity) {
        entity.setId(id);
        return ApiResponse.ok(populationService.update(entity));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Boolean> delete(@PathVariable Long id) {
        return ApiResponse.ok(populationService.delete(id));
    }
}
