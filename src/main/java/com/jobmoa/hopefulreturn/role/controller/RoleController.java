package com.jobmoa.hopefulreturn.role.controller;

import com.jobmoa.hopefulreturn.role.model.dto.RoleRequestDto;
import com.jobmoa.hopefulreturn.role.model.dto.RoleResponseDto;
import com.jobmoa.hopefulreturn.role.service.RoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Role")
@RestController
@RequestMapping("/role")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    @Operation(summary = "Create role")
    @PostMapping
    public RoleResponseDto create(@RequestBody RoleRequestDto requestDto) {
        return roleService.create(requestDto);
    }

    @Operation(summary = "Find role by id")
    @GetMapping("/{id}")
    public RoleResponseDto findById(@PathVariable Long id) {
        return roleService.findById(id);
    }

    @Operation(summary = "Find all roles")
    @GetMapping
    public List<RoleResponseDto> findAll() {
        return roleService.findAll();
    }

    @Operation(summary = "Update role")
    @PutMapping("/{id}")
    public RoleResponseDto update(@PathVariable Long id, @RequestBody RoleRequestDto requestDto) {
        return roleService.update(id, requestDto);
    }

    @Operation(summary = "Delete role")
    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        roleService.delete(id);
    }
}
