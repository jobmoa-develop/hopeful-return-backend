package com.jobmoa.hopefulreturn.participant.controller;

import com.jobmoa.hopefulreturn.common.ApiResponse;
import com.jobmoa.hopefulreturn.courseparticipant.scope.ParticipantScope;
import com.jobmoa.hopefulreturn.courseparticipant.scope.ParticipantScopeResolver;
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

@Tag(name = "Participant")
@RestController
@RequestMapping("/api/participants")
@RequiredArgsConstructor
public class ParticipantController {

    private final ParticipantService participantService;
    private final ParticipantScopeResolver participantScopeResolver;

    @Operation(summary = "참여자 등록", description = "권한: ADMIN, HEAD_OFFICE, REGIONAL_MANAGER, PROJECT_MANAGER, PROJECT_LEADER")
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'HEAD_OFFICE', 'REGIONAL_MANAGER', 'PROJECT_MANAGER', 'PROJECT_LEADER')")
    public ApiResponse<ParticipantCreatedResponse> create(
            @Valid @RequestBody CreateParticipantRequest request) {
        return ApiResponse.success(participantService.create(request));
    }

    @Operation(summary = "참여자 목록 조회",
            description = "권한: ADMIN, HEAD_OFFICE, REGIONAL_MANAGER, PROJECT_MANAGER, PROJECT_LEADER, OPERATOR, COUNSELOR, STAFF")
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'HEAD_OFFICE', 'REGIONAL_MANAGER', 'PROJECT_MANAGER', 'PROJECT_LEADER',"
            + " 'OPERATOR', 'COUNSELOR', 'STAFF')")
    public ApiResponse<ParticipantListResponse> findAll(
            @Parameter(description = "페이지 번호") @RequestParam(required = false) Integer page,
            @Parameter(description = "페이지 크기") @RequestParam(required = false) Integer size,
            @Parameter(description = "참여자명") @RequestParam(required = false) String name,
            @Parameter(description = "전화번호") @RequestParam(required = false) String phone,
            @Parameter(description = "지역 ID (최신 수강건 기준 회차 필터)") @RequestParam(required = false) Long regionId,
            @Parameter(description = "회차(course_number) (최신 수강건 기준 회차 필터)")
            @RequestParam(required = false) Integer courseNumber,
            @RequestAttribute(name = "userId", required = false) Long userId,
            Authentication authentication) {
        // 역할별 조회 스코프를 서버측에서 강제한다 — 진행자(STAFF)=배정 회차 참여자, 관리자급=제한 없음.
        ParticipantScope scope = participantScopeResolver.resolve(authentication, userId);
        return ApiResponse.success(participantService.findAll(
                page, size, name, phone, regionId, courseNumber, scope.participantIds()));
    }

    @Operation(summary = "참여자 전화번호 중복 확인",
            description = "등록 플로우 전용. 권한: ADMIN, HEAD_OFFICE, REGIONAL_MANAGER, PROJECT_MANAGER, PROJECT_LEADER")
    @GetMapping("/check-phone")
    @PreAuthorize("hasAnyRole('ADMIN', 'HEAD_OFFICE', 'REGIONAL_MANAGER', 'PROJECT_MANAGER', 'PROJECT_LEADER')")
    public ApiResponse<CheckPhoneResponse> checkPhone(
            @Parameter(description = "전화번호") @RequestParam String phone) {
        return ApiResponse.success(participantService.checkPhone(phone));
    }

    @Operation(summary = "참여자 상세 조회",
            description = "권한: ADMIN, HEAD_OFFICE, REGIONAL_MANAGER, PROJECT_MANAGER, PROJECT_LEADER, OPERATOR, COUNSELOR, STAFF")
    @GetMapping("/{participantId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HEAD_OFFICE', 'REGIONAL_MANAGER', 'PROJECT_MANAGER', 'PROJECT_LEADER',"
            + " 'OPERATOR', 'COUNSELOR', 'STAFF')")
    public ApiResponse<ParticipantResponse> findById(
            @PathVariable Long participantId,
            @RequestAttribute(name = "userId", required = false) Long userId,
            Authentication authentication) {
        // 상세 조회도 목록과 동일 스코프를 강제한다 — 배정 외 참여자 ID 직접 조회 우회 차단.
        ParticipantScope scope = participantScopeResolver.resolve(authentication, userId);
        return ApiResponse.success(participantService.findById(participantId, scope.participantIds()));
    }

    @Operation(summary = "참여자 수정",
            description = "권한: ADMIN, HEAD_OFFICE, REGIONAL_MANAGER, PROJECT_MANAGER, PROJECT_LEADER, OPERATOR")
    @PutMapping("/{participantId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HEAD_OFFICE', 'REGIONAL_MANAGER', 'PROJECT_MANAGER', 'PROJECT_LEADER', 'OPERATOR')")
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
