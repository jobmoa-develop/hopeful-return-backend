package com.jobmoa.hopefulreturn.users.controller;

import com.jobmoa.hopefulreturn.common.ApiResponse;
import com.jobmoa.hopefulreturn.users.model.dto.CheckLoginIdResponse;
import com.jobmoa.hopefulreturn.users.model.dto.CreateUserRequest;
import com.jobmoa.hopefulreturn.users.model.dto.UpdateSmsPermissionRequest;
import com.jobmoa.hopefulreturn.users.model.dto.UpdateUserRequest;
import com.jobmoa.hopefulreturn.users.model.dto.UserListResponse;
import com.jobmoa.hopefulreturn.users.model.dto.UserResponse;
import com.jobmoa.hopefulreturn.users.service.UsersService;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Users")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UsersController {

    private final UsersService usersService;

    @Operation(summary = "직원 등록", description = "권한: ADMIN")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<UserResponse> create(@Valid @RequestBody CreateUserRequest request) {
        return ApiResponse.success(usersService.create(request));
    }

    @Operation(summary = "직원 목록 조회", description = "권한: ADMIN, HEAD_OFFICE")
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'HEAD_OFFICE')")
    public ApiResponse<UserListResponse> findAll(
            @Parameter(description = "페이지 번호") @RequestParam(required = false) Integer page,
            @Parameter(description = "페이지 크기") @RequestParam(required = false) Integer size,
            @Parameter(description = "직원명") @RequestParam(required = false) String name,
            @Parameter(description = "역할명") @RequestParam(required = false) String roleName,
            @Parameter(description = "활성 여부") @RequestParam(required = false) Boolean enabled) {
        return ApiResponse.success(usersService.findAll(page, size, name, roleName, enabled));
    }

    @Operation(summary = "직원 상세 조회", description = "권한: ADMIN, HEAD_OFFICE")
    @GetMapping("/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HEAD_OFFICE')")
    public ApiResponse<UserResponse> findById(@PathVariable Long userId) {
        return ApiResponse.success(usersService.findById(userId));
    }

    @Operation(summary = "직원 수정", description = "권한: ADMIN")
    @PutMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Map<String, String>> update(
            @PathVariable Long userId,
            @Valid @RequestBody UpdateUserRequest request) {
        usersService.update(userId, request);
        return ApiResponse.success(Map.of("message", "직원 정보가 수정되었습니다."));
    }

    @Operation(summary = "문자 발송 권한 설정", description = "권한: ADMIN")
    @PatchMapping("/{userId}/sms-permission")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Map<String, String>> updateSmsPermission(
            @PathVariable Long userId,
            @Valid @RequestBody UpdateSmsPermissionRequest request) {
        usersService.updateSmsPermission(userId, request.canSendSms());
        return ApiResponse.success(Map.of("message", "문자 발송 권한이 변경되었습니다."));
    }

    @Operation(summary = "직원 삭제", description = "권한: ADMIN")
    @DeleteMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Map<String, String>> delete(@PathVariable Long userId) {
        usersService.delete(userId);
        return ApiResponse.success(Map.of("message", "직원이 삭제되었습니다."));
    }

    @Operation(summary = "로그인 ID 중복 확인", description = "권한: ADMIN")
    @GetMapping("/check-login-id")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<CheckLoginIdResponse> checkLoginId(
            @Parameter(description = "로그인 ID") @RequestParam String loginId) {
        return ApiResponse.success(usersService.checkLoginId(loginId));
    }
}
