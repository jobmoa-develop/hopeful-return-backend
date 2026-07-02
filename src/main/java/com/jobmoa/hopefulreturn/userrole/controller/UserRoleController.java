package com.jobmoa.hopefulreturn.userrole.controller;

import com.jobmoa.hopefulreturn.userrole.model.dto.UserRoleRequestDto;
import com.jobmoa.hopefulreturn.userrole.model.dto.UserRoleResponseDto;
import com.jobmoa.hopefulreturn.userrole.service.UserRoleService;
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

@Tag(name = "UserRole")
@RestController
@RequestMapping("/userrole")
@RequiredArgsConstructor
public class UserRoleController {

    private final UserRoleService userRoleService;

    @Operation(summary = "Create user role")
    @PostMapping
    public UserRoleResponseDto create(@RequestBody UserRoleRequestDto requestDto) {
        return userRoleService.create(requestDto);
    }

    @Operation(summary = "Find user role by id")
    @GetMapping("/{id}")
    public UserRoleResponseDto findById(@PathVariable Long id) {
        return userRoleService.findById(id);
    }

    @Operation(summary = "Find all user roles")
    @GetMapping
    public List<UserRoleResponseDto> findAll() {
        return userRoleService.findAll();
    }

    @Operation(summary = "Update user role")
    @PutMapping("/{id}")
    public UserRoleResponseDto update(@PathVariable Long id, @RequestBody UserRoleRequestDto requestDto) {
        return userRoleService.update(id, requestDto);
    }

    @Operation(summary = "Delete user role")
    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        userRoleService.delete(id);
    }
}
