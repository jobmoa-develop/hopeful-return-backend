package com.jobmoa.hopefulreturn.followupcounsel.controller;

import com.jobmoa.hopefulreturn.common.ApiResponse;
import com.jobmoa.hopefulreturn.followupcounsel.model.dto.CreateFollowUpCounselRequest;
import com.jobmoa.hopefulreturn.followupcounsel.model.dto.FollowUpCounselCreatedResponse;
import com.jobmoa.hopefulreturn.followupcounsel.model.dto.FollowUpCounselDeletedResponse;
import com.jobmoa.hopefulreturn.followupcounsel.model.dto.FollowUpCounselDetailResponse;
import com.jobmoa.hopefulreturn.followupcounsel.model.dto.FollowUpCounselListResponse;
import com.jobmoa.hopefulreturn.followupcounsel.model.dto.FollowUpCounselUpdatedResponse;
import com.jobmoa.hopefulreturn.followupcounsel.model.dto.UpdateFollowUpCounselRequest;
import com.jobmoa.hopefulreturn.followupcounsel.service.FollowUpCounselService;
import com.jobmoa.hopefulreturn.security.AuthScopeSupport;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "FollowUpCounsel")
@RestController
@RequestMapping("/api/follow-up-counsels")
@RequiredArgsConstructor
public class FollowUpCounselController {

    private final FollowUpCounselService followUpCounselService;

    @Operation(summary = "사후관리 상담 등록", description = "권한: ADMIN, COUNSELOR")
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'COUNSELOR')")
    public ApiResponse<FollowUpCounselCreatedResponse> create(
            @Valid @RequestBody CreateFollowUpCounselRequest request) {
        return ApiResponse.success(followUpCounselService.create(request));
    }

    @Operation(summary = "사후관리 상담 목록 조회",
            description = "COUNSELOR 는 본인이 배정된 수강건만 조회된다(서버측 강제). "
                    + "권한: ADMIN, COUNSELOR, HEAD_OFFICE, REGIONAL_MANAGER, OPERATOR")
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'COUNSELOR', 'HEAD_OFFICE', 'REGIONAL_MANAGER', 'OPERATOR')")
    public ApiResponse<FollowUpCounselListResponse> findAll(
            @Parameter(description = "수강생 ID") @RequestParam Long courseParticipantId,
            @RequestAttribute(name = "userId", required = false) Long userId,
            Authentication authentication) {
        // COUNSELOR 는 배정받은 참여자만 조회 — 서버측에서 스코프를 강제한다(FE 우회 불가).
        Long counselorScopeId = AuthScopeSupport.isCounselorOnly(authentication) ? userId : null;
        return ApiResponse.success(followUpCounselService.findAll(courseParticipantId, counselorScopeId));
    }

    @Operation(summary = "사후관리 상담 상세 조회",
            description = "COUNSELOR 는 본인이 배정된 수강건만 조회된다(서버측 강제). "
                    + "권한: ADMIN, COUNSELOR, HEAD_OFFICE, REGIONAL_MANAGER, OPERATOR")
    @GetMapping("/{followUpCounselId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'COUNSELOR', 'HEAD_OFFICE', 'REGIONAL_MANAGER', 'OPERATOR')")
    public ApiResponse<FollowUpCounselDetailResponse> findById(
            @PathVariable Long followUpCounselId,
            @RequestAttribute(name = "userId", required = false) Long userId,
            Authentication authentication) {
        Long counselorScopeId = AuthScopeSupport.isCounselorOnly(authentication) ? userId : null;
        return ApiResponse.success(followUpCounselService.findById(followUpCounselId, counselorScopeId));
    }

    @Operation(summary = "사후관리 상담 수정", description = "권한: ADMIN, COUNSELOR")
    @PutMapping("/{followUpCounselId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'COUNSELOR')")
    public ApiResponse<FollowUpCounselUpdatedResponse> update(
            @PathVariable Long followUpCounselId,
            @Valid @RequestBody UpdateFollowUpCounselRequest request) {
        return ApiResponse.success(followUpCounselService.update(followUpCounselId, request));
    }

    @Operation(summary = "사후관리 상담 삭제", description = "권한: ADMIN")
    @DeleteMapping("/{followUpCounselId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<FollowUpCounselDeletedResponse> delete(@PathVariable Long followUpCounselId) {
        return ApiResponse.success(followUpCounselService.delete(followUpCounselId));
    }
}
