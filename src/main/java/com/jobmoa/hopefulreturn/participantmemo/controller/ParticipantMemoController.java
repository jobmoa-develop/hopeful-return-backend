package com.jobmoa.hopefulreturn.participantmemo.controller;

import com.jobmoa.hopefulreturn.common.ApiResponse;
import com.jobmoa.hopefulreturn.participantmemo.model.dto.CreateParticipantMemoRequest;
import com.jobmoa.hopefulreturn.participantmemo.model.dto.ParticipantMemoCreatedResponse;
import com.jobmoa.hopefulreturn.participantmemo.model.dto.ParticipantMemoDeletedResponse;
import com.jobmoa.hopefulreturn.participantmemo.model.dto.ParticipantMemoDetailResponse;
import com.jobmoa.hopefulreturn.participantmemo.model.dto.ParticipantMemoListResponse;
import com.jobmoa.hopefulreturn.participantmemo.model.dto.ParticipantMemoUpdatedResponse;
import com.jobmoa.hopefulreturn.participantmemo.model.dto.UpdateParticipantMemoRequest;
import com.jobmoa.hopefulreturn.participantmemo.service.ParticipantMemoService;
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
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "ParticipantMemo")
@RestController
@RequestMapping("/api/participant-memos")
@RequiredArgsConstructor
public class ParticipantMemoController {

    private final ParticipantMemoService participantMemoService;

    @Operation(summary = "상담 메모 등록",
            description = "권한: ADMIN, HEAD_OFFICE, REGIONAL_MANAGER, PROJECT_MANAGER, PROJECT_LEADER, OPERATOR, COUNSELOR, STAFF")
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'HEAD_OFFICE', 'REGIONAL_MANAGER', 'PROJECT_MANAGER', 'PROJECT_LEADER',"
            + " 'OPERATOR', 'COUNSELOR', 'STAFF')")
    public ApiResponse<ParticipantMemoCreatedResponse> create(
            @RequestAttribute("userId") Long userId,
            @Valid @RequestBody CreateParticipantMemoRequest request) {
        return ApiResponse.success(participantMemoService.create(userId, request));
    }

    @Operation(summary = "상담 메모 목록 조회",
            description = "권한: ADMIN, HEAD_OFFICE, REGIONAL_MANAGER, PROJECT_MANAGER, PROJECT_LEADER, OPERATOR, COUNSELOR, STAFF")
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'HEAD_OFFICE', 'REGIONAL_MANAGER', 'PROJECT_MANAGER', 'PROJECT_LEADER',"
            + " 'OPERATOR', 'COUNSELOR', 'STAFF')")
    public ApiResponse<ParticipantMemoListResponse> findAll(
            @Parameter(description = "수강생 ID") @RequestParam Long courseParticipantId) {
        return ApiResponse.success(participantMemoService.findAll(courseParticipantId));
    }

    @Operation(summary = "상담 메모 상세 조회",
            description = "권한: ADMIN, HEAD_OFFICE, REGIONAL_MANAGER, PROJECT_MANAGER, PROJECT_LEADER, OPERATOR, COUNSELOR, STAFF")
    @GetMapping("/{memoId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HEAD_OFFICE', 'REGIONAL_MANAGER', 'PROJECT_MANAGER', 'PROJECT_LEADER',"
            + " 'OPERATOR', 'COUNSELOR', 'STAFF')")
    public ApiResponse<ParticipantMemoDetailResponse> findById(@PathVariable Long memoId) {
        return ApiResponse.success(participantMemoService.findById(memoId));
    }

    @Operation(summary = "상담 메모 수정",
            description = "권한: ADMIN, HEAD_OFFICE, REGIONAL_MANAGER, PROJECT_MANAGER, PROJECT_LEADER, OPERATOR, COUNSELOR, STAFF")
    @PutMapping("/{memoId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HEAD_OFFICE', 'REGIONAL_MANAGER', 'PROJECT_MANAGER', 'PROJECT_LEADER',"
            + " 'OPERATOR', 'COUNSELOR', 'STAFF')")
    public ApiResponse<ParticipantMemoUpdatedResponse> update(
            @PathVariable Long memoId,
            @Valid @RequestBody UpdateParticipantMemoRequest request) {
        return ApiResponse.success(participantMemoService.update(memoId, request));
    }

    @Operation(summary = "상담 메모 삭제", description = "권한: ADMIN")
    @DeleteMapping("/{memoId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<ParticipantMemoDeletedResponse> delete(@PathVariable Long memoId) {
        return ApiResponse.success(participantMemoService.delete(memoId));
    }
}
