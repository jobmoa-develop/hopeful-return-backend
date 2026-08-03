package com.jobmoa.hopefulreturn.userrole.controller;

import com.jobmoa.hopefulreturn.userrole.model.dto.UserRoleResponseDto;
import com.jobmoa.hopefulreturn.userrole.service.UserRoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "User Role")
@RestController
@RequestMapping("/api/user-roles")
@RequiredArgsConstructor
public class UserRoleController {

    private final UserRoleService userRoleService;

    @Operation(summary = "사용자 권한 전체 조회")
    @GetMapping
    public List<UserRoleResponseDto> findAll() {

        return userRoleService.findAll();
    }

    @Operation(summary = "특정 사용자 권한 조회")
    @GetMapping("/{userId}")
    public List<UserRoleResponseDto> findByUserId(
            @PathVariable Long userId) {

        return userRoleService.findByUserId(userId);
    }
}