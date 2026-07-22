package com.changping.platform.modules.community.controller;

import com.changping.platform.common.response.ApiResponse;
import com.changping.platform.modules.community.entity.PlaceEntity;
import com.changping.platform.modules.community.service.PlaceService;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/community/places")
public class PlaceController {

    private final PlaceService service;
    public PlaceController(PlaceService service) { this.service = service; }

    @GetMapping
    public ApiResponse<List<PlaceEntity>> list(@RequestParam(required = false) Long gridId) {
        return ApiResponse.ok(service.list(gridId));
    }
    @GetMapping("/{id}")
    public ApiResponse<PlaceEntity> detail(@PathVariable Long id) {
        return ApiResponse.ok(service.detail(id));
    }
    @PostMapping
    public ApiResponse<Boolean> create(@RequestBody PlaceEntity entity) {
        return ApiResponse.ok(service.create(entity));
    }
    @PutMapping("/{id}")
    public ApiResponse<Boolean> update(@PathVariable Long id, @RequestBody PlaceEntity entity) {
        entity.setId(id);
        return ApiResponse.ok(service.update(entity));
    }
    @DeleteMapping("/{id}")
    public ApiResponse<Boolean> delete(@PathVariable Long id) {
        return ApiResponse.ok(service.delete(id));
    }
}
