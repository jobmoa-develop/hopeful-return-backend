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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
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

    @Operation(summary = "사후관리 목록 조회", description = "권한: ADMIN, COUNSELOR, HEAD_OFFICE, OPERATOR, REGIONAL_MANAGER")
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'COUNSELOR', 'HEAD_OFFICE', 'OPERATOR', 'REGIONAL_MANAGER')")
    public ApiResponse<FollowUpListResponse> findAll(
            @Parameter(description = "수강생 ID") @RequestParam Long courseParticipantId) {
        return ApiResponse.success(followUpService.findAll(courseParticipantId));
    }

    @Operation(summary = "사후관리 상세 조회", description = "권한: ADMIN, COUNSELOR, HEAD_OFFICE, OPERATOR, REGIONAL_MANAGER")
    @GetMapping("/{followUpId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'COUNSELOR', 'HEAD_OFFICE', 'OPERATOR', 'REGIONAL_MANAGER')")
    public ApiResponse<FollowUpDetailResponse> findById(@PathVariable Long followUpId) {
        return ApiResponse.success(followUpService.findById(followUpId));
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
