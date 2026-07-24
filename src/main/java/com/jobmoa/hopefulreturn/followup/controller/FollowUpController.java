package com.jobmoa.hopefulreturn.followup.controller;

import com.jobmoa.hopefulreturn.common.ApiResponse;
import com.jobmoa.hopefulreturn.followup.model.dto.CreateFollowUpRequest;
import com.jobmoa.hopefulreturn.followup.model.dto.CreateFollowUpResponse;
import com.jobmoa.hopefulreturn.followup.model.dto.DeleteFollowUpResponse;
import com.jobmoa.hopefulreturn.followup.model.dto.FollowUpDetailResponse;
import com.jobmoa.hopefulreturn.followup.model.dto.FollowUpListResponse;
import com.jobmoa.hopefulreturn.followup.model.dto.UpdateFollowUpRequest;
import com.jobmoa.hopefulreturn.followup.model.dto.UpdateFollowUpResponse;
import com.jobmoa.hopefulreturn.followup.service.FollowUpService;
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

@Tag(name = "FollowUp")
@RestController
@RequestMapping("/api/follow-ups")
@RequiredArgsConstructor
public class FollowUpController {

    private final FollowUpService followUpService;

    @Operation(summary = "사후관리 등록", description = "권한: ADMIN, COUNSELOR")
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'COUNSELOR')")
    public ApiResponse<CreateFollowUpResponse> create(@Valid @RequestBody CreateFollowUpRequest request) {
        return ApiResponse.success(followUpService.create(request));
    }

    @Operation(summary = "사후관리 목록 조회",
            description = "수료(COMPLETED) 참여자 + follow_up 스냅샷 + 상담 요약을 페이지로 반환한다. "
                    + "COUNSELOR 는 본인이 배정된 수료 참여자만 조회된다(서버측 강제). "
                    + "권한: ADMIN, COUNSELOR, HEAD_OFFICE, OPERATOR, REGIONAL_MANAGER")
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'COUNSELOR', 'HEAD_OFFICE', 'OPERATOR', 'REGIONAL_MANAGER')")
    public ApiResponse<FollowUpListResponse> findAll(
            @Parameter(description = "참여자명 검색") @RequestParam(required = false) String name,
            @Parameter(description = "지역 ID") @RequestParam(required = false) Long regionId,
            @Parameter(description = "지역 내 회차(localCourseNumber)") @RequestParam(required = false) Integer courseNumber,
            @Parameter(description = "페이지 번호") @RequestParam(required = false) Integer page,
            @Parameter(description = "페이지 크기") @RequestParam(required = false) Integer size,
            @RequestAttribute(name = "userId", required = false) Long userId,
            Authentication authentication) {
        // COUNSELOR 는 배정받은 참여자만 조회 — 서버측에서 스코프를 강제한다(FE 우회 불가).
        Long counselorScopeId = AuthScopeSupport.isCounselorOnly(authentication) ? userId : null;
        return ApiResponse.success(followUpService.findAll(name, regionId, courseNumber, counselorScopeId, page, size));
    }

    @Operation(summary = "사후관리 상세 조회",
            description = "COUNSELOR 는 본인이 배정된 수강건만 조회된다(서버측 강제). "
                    + "권한: ADMIN, COUNSELOR, HEAD_OFFICE, OPERATOR, REGIONAL_MANAGER")
    @GetMapping("/{followUpId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'COUNSELOR', 'HEAD_OFFICE', 'OPERATOR', 'REGIONAL_MANAGER')")
    public ApiResponse<FollowUpDetailResponse> findById(
            @PathVariable Long followUpId,
            @RequestAttribute(name = "userId", required = false) Long userId,
            Authentication authentication) {
        Long counselorScopeId = AuthScopeSupport.isCounselorOnly(authentication) ? userId : null;
        return ApiResponse.success(followUpService.findById(followUpId, counselorScopeId));
    }

    @Operation(summary = "사후관리 수정", description = "권한: ADMIN, COUNSELOR")
    @PutMapping("/{followUpId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'COUNSELOR')")
    public ApiResponse<UpdateFollowUpResponse> update(
            @PathVariable Long followUpId,
            @Valid @RequestBody UpdateFollowUpRequest request) {
        return ApiResponse.success(followUpService.update(followUpId, request));
    }

    @Operation(summary = "사후관리 삭제", description = "권한: ADMIN")
    @DeleteMapping("/{followUpId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<DeleteFollowUpResponse> delete(@PathVariable Long followUpId) {
        return ApiResponse.success(followUpService.delete(followUpId));
    }
}
