package com.jobmoa.hopefulreturn.auth.controller;

import com.jobmoa.hopefulreturn.auth.dto.EmailSendRequest;
import com.jobmoa.hopefulreturn.auth.dto.EmailVerifyRequest;
import com.jobmoa.hopefulreturn.auth.dto.LoginRequest;
import com.jobmoa.hopefulreturn.auth.dto.MemberResponse;
import com.jobmoa.hopefulreturn.auth.dto.RefreshRequest;
import com.jobmoa.hopefulreturn.auth.dto.SignupRequest;
import com.jobmoa.hopefulreturn.auth.dto.TokenResponse;
import com.jobmoa.hopefulreturn.auth.service.AuthService;
import com.jobmoa.hopefulreturn.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<MemberResponse>> signup(@Valid @RequestBody SignupRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(authService.signup(request)));
    }

    @PostMapping("/login")
    public ApiResponse<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(authService.login(request));
    }

    @PostMapping("/refresh")
    public ApiResponse<TokenResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ApiResponse.success(authService.refresh(request));
    }

    @PostMapping("/email/send")
    public ApiResponse<Void> sendEmail(@Valid @RequestBody EmailSendRequest request) {
        authService.sendEmailVerification(request);
        return ApiResponse.success(null);
    }

    @PostMapping("/email/verify")
    public ApiResponse<Void> verifyEmail(@Valid @RequestBody EmailVerifyRequest request) {
        authService.verifyEmail(request);
        return ApiResponse.success(null);
    }
}
