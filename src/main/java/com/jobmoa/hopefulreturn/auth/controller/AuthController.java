package com.jobmoa.hopefulreturn.auth.controller;

import com.jobmoa.hopefulreturn.auth.model.dto.LoginRequest;
import com.jobmoa.hopefulreturn.auth.model.dto.LoginResponse;
import com.jobmoa.hopefulreturn.auth.model.dto.MeResponse;
import com.jobmoa.hopefulreturn.auth.model.dto.RefreshResponse;
import com.jobmoa.hopefulreturn.auth.service.AuthService;
import com.jobmoa.hopefulreturn.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Auth")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "Login")
    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response) {
        return ApiResponse.success(authService.login(request, response));
    }

    @Operation(summary = "Refresh access token")
    @PostMapping("/refresh")
    public ApiResponse<RefreshResponse> refresh(HttpServletRequest request) {
        return ApiResponse.success(authService.refresh(request));
    }

    @Operation(summary = "Logout")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        authService.logout(request, response);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Get my profile")
    @GetMapping("/me")
    public ApiResponse<MeResponse> me() {
        return ApiResponse.success(authService.me());
    }
}
