package com.jobmoa.hopefulreturn.participant.controller;

import com.jobmoa.hopefulreturn.common.ApiResponse;
import com.jobmoa.hopefulreturn.participant.model.dto.CheckPhoneResponse;
import com.jobmoa.hopefulreturn.participant.model.dto.CreateParticipantRequest;
import com.jobmoa.hopefulreturn.participant.model.dto.ParticipantCreatedResponse;
import com.jobmoa.hopefulreturn.participant.model.dto.ParticipantDeletedResponse;
import com.jobmoa.hopefulreturn.participant.model.dto.ParticipantListResponse;
import com.jobmoa.hopefulreturn.participant.model.dto.ParticipantResponse;
import com.jobmoa.hopefulreturn.participant.model.dto.ParticipantUpdatedResponse;
import com.jobmoa.hopefulreturn.participant.model.dto.UpdateParticipantRequest;
import com.jobmoa.hopefulreturn.participant.service.ParticipantService;
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

@Tag(name = "Participant")
@RestController
@RequestMapping("/api/participants")
@RequiredArgsConstructor
public class ParticipantController {

    private final ParticipantService participantService;

    @Operation(summary = "참여자 등록", description = "권한: OPERATOR")
    @PostMapping
    @PreAuthorize("hasRole('OPERATOR')")
    public ApiResponse<ParticipantCreatedResponse> create(
            @Valid @RequestBody CreateParticipantRequest request) {
        return ApiResponse.success(participantService.create(request));
    }

    @Operation(summary = "참여자 목록 조회", description = "권한: OPERATOR, COUNSELOR, HEAD_OFFICE, REGIONAL_MANAGER")
    @GetMapping
    @PreAuthorize("hasAnyRole('OPERATOR', 'COUNSELOR', 'HEAD_OFFICE', 'REGIONAL_MANAGER')")
    public ApiResponse<ParticipantListResponse> findAll(
            @Parameter(description = "페이지 번호") @RequestParam(required = false) Integer page,
            @Parameter(description = "페이지 크기") @RequestParam(required = false) Integer size,
            @Parameter(description = "참여자명") @RequestParam(required = false) String name,
            @Parameter(description = "전화번호") @RequestParam(required = false) String phone) {
        return ApiResponse.success(participantService.findAll(page, size, name, phone));
    }

    @Operation(summary = "참여자 전화번호 중복 확인", description = "권한: OPERATOR")
    @GetMapping("/check-phone")
    @PreAuthorize("hasRole('OPERATOR')")
    public ApiResponse<CheckPhoneResponse> checkPhone(
            @Parameter(description = "전화번호") @RequestParam String phone) {
        return ApiResponse.success(participantService.checkPhone(phone));
    }

    @Operation(summary = "참여자 상세 조회", description = "권한: 인증된 모든 사용자")
    @GetMapping("/{participantId}")
    public ApiResponse<ParticipantResponse> findById(@PathVariable Long participantId) {
        return ApiResponse.success(participantService.findById(participantId));
    }

    @Operation(summary = "참여자 수정", description = "권한: OPERATOR")
    @PutMapping("/{participantId}")
    @PreAuthorize("hasRole('OPERATOR')")
    public ApiResponse<ParticipantUpdatedResponse> update(
            @PathVariable Long participantId,
            @Valid @RequestBody UpdateParticipantRequest request) {
        return ApiResponse.success(participantService.update(participantId, request));
    }

    @Operation(summary = "참여자 삭제", description = "권한: ADMIN")
    @DeleteMapping("/{participantId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<ParticipantDeletedResponse> delete(@PathVariable Long participantId) {
        return ApiResponse.success(participantService.delete(participantId));
    }
}
