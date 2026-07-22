package com.changping.platform.modules.community.controller;

import com.changping.platform.common.response.ApiResponse;
import com.changping.platform.modules.community.entity.BuildingEntity;
import com.changping.platform.modules.community.service.BuildingService;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/community/buildings")
public class BuildingController {

    private final BuildingService service;
    public BuildingController(BuildingService service) { this.service = service; }

    @GetMapping
    public ApiResponse<List<BuildingEntity>> list(@RequestParam(required = false) Long gridId) {
        return ApiResponse.ok(service.list(gridId));
    }
    @GetMapping("/{id}")
    public ApiResponse<BuildingEntity> detail(@PathVariable Long id) {
        return ApiResponse.ok(service.detail(id));
    }
    @PostMapping
    public ApiResponse<Boolean> create(@RequestBody BuildingEntity entity) {
        return ApiResponse.ok(service.create(entity));
    }
    @PutMapping("/{id}")
    public ApiResponse<Boolean> update(@PathVariable Long id, @RequestBody BuildingEntity entity) {
        entity.setId(id);
        return ApiResponse.ok(service.update(entity));
    }
    @DeleteMapping("/{id}")
    public ApiResponse<Boolean> delete(@PathVariable Long id) {
        return ApiResponse.ok(service.delete(id));
    }
}
