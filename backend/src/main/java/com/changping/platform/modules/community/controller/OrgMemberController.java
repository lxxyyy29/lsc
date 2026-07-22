package com.changping.platform.modules.community.controller;

import com.changping.platform.common.response.ApiResponse;
import com.changping.platform.modules.community.entity.OrgMemberEntity;
import com.changping.platform.modules.community.service.OrgMemberService;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/community/org-members")
public class OrgMemberController {

    private final OrgMemberService service;
    public OrgMemberController(OrgMemberService service) { this.service = service; }

    @GetMapping
    public ApiResponse<List<OrgMemberEntity>> list(@RequestParam(required = false) Long gridId) {
        return ApiResponse.ok(service.list(gridId));
    }
    @GetMapping("/{id}")
    public ApiResponse<OrgMemberEntity> detail(@PathVariable Long id) {
        return ApiResponse.ok(service.detail(id));
    }
    @PostMapping
    public ApiResponse<Boolean> create(@RequestBody OrgMemberEntity entity) {
        return ApiResponse.ok(service.create(entity));
    }
    @PutMapping("/{id}")
    public ApiResponse<Boolean> update(@PathVariable Long id, @RequestBody OrgMemberEntity entity) {
        entity.setId(id);
        return ApiResponse.ok(service.update(entity));
    }
    @DeleteMapping("/{id}")
    public ApiResponse<Boolean> delete(@PathVariable Long id) {
        return ApiResponse.ok(service.delete(id));
    }
}
