package com.jobmoa.hopefulreturn.role.controller;

import com.jobmoa.hopefulreturn.role.model.dto.RoleResponseDto;
import com.jobmoa.hopefulreturn.role.service.RoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Role")
@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    @Operation(summary = "권한 목록 조회")
    @GetMapping
    public List<RoleResponseDto> findAll() {
        return roleService.findAll();
    }

    @Operation(summary = "권한 상세 조회")
    @GetMapping("/{roleId}")
    public RoleResponseDto findById(@PathVariable Long roleId) {
        return roleService.findById(roleId);
    }
}