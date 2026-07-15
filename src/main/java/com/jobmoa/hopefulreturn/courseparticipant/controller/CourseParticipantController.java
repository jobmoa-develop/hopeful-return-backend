package com.jobmoa.hopefulreturn.courseparticipant.controller;

import com.jobmoa.hopefulreturn.common.ApiResponse;
import com.jobmoa.hopefulreturn.courseparticipant.model.dto.CancelCourseParticipantRequest;
import com.jobmoa.hopefulreturn.courseparticipant.model.dto.ChangeCounselorRequest;
import com.jobmoa.hopefulreturn.courseparticipant.model.dto.ChangeCourseParticipantStatusRequest;
import com.jobmoa.hopefulreturn.courseparticipant.model.dto.CompleteCourseParticipantRequest;
import com.jobmoa.hopefulreturn.courseparticipant.model.dto.ContactAttemptResponse;
import com.jobmoa.hopefulreturn.courseparticipant.model.dto.CounselingSessionResponse;
import com.jobmoa.hopefulreturn.courseparticipant.model.dto.CounselorChangedResponse;
import com.jobmoa.hopefulreturn.courseparticipant.model.dto.CourseParticipantCanceledResponse;
import com.jobmoa.hopefulreturn.courseparticipant.model.dto.CourseParticipantCompletionResponse;
import com.jobmoa.hopefulreturn.courseparticipant.model.dto.CourseParticipantCreatedResponse;
import com.jobmoa.hopefulreturn.courseparticipant.model.dto.CourseParticipantDeletedResponse;
import com.jobmoa.hopefulreturn.courseparticipant.model.dto.CourseParticipantDetailResponse;
import com.jobmoa.hopefulreturn.courseparticipant.model.dto.CourseParticipantListResponse;
import com.jobmoa.hopefulreturn.courseparticipant.model.dto.CourseParticipantStatusChangedResponse;
import com.jobmoa.hopefulreturn.courseparticipant.model.dto.CourseParticipantUpdatedResponse;
import com.jobmoa.hopefulreturn.courseparticipant.model.dto.CreateCourseParticipantRequest;
import com.jobmoa.hopefulreturn.courseparticipant.model.dto.RecordCounselingSessionRequest;
import com.jobmoa.hopefulreturn.courseparticipant.model.dto.UpdateCourseParticipantRequest;
import com.jobmoa.hopefulreturn.courseparticipant.service.CourseParticipantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "CourseParticipant")
@RestController
@RequestMapping("/api/course-participants")
@RequiredArgsConstructor
public class CourseParticipantController {

    private final CourseParticipantService courseParticipantService;

    @Operation(summary = "수강 등록", description = "권한: OPERATOR")
    @PostMapping
    @PreAuthorize("hasRole('OPERATOR')")
    public ApiResponse<CourseParticipantCreatedResponse> create(
            @Valid @RequestBody CreateCourseParticipantRequest request) {
        return ApiResponse.success(courseParticipantService.create(request));
    }

    @Operation(summary = "수강생 목록 조회", description = "권한: OPERATOR, COUNSELOR, STAFF, HEAD_OFFICE")
    @GetMapping
    @PreAuthorize("hasAnyRole('OPERATOR', 'COUNSELOR', 'STAFF', 'HEAD_OFFICE')")
    public ApiResponse<CourseParticipantListResponse> findAll(
            @Parameter(description = "강좌 ID") @RequestParam(required = false) Long courseId,
            @Parameter(description = "수강 상태") @RequestParam(required = false) String status,
            @Parameter(description = "검색어(참여자명/전화번호)") @RequestParam(required = false) String keyword,
            @Parameter(description = "페이지 번호") @RequestParam(required = false) Integer page,
            @Parameter(description = "페이지 크기") @RequestParam(required = false) Integer size) {
        return ApiResponse.success(courseParticipantService.findAll(courseId, status, keyword, page, size));
    }

    @Operation(summary = "수강생 상세 조회", description = "권한: HEAD_OFFICE, REGIONAL_MANAGER, OPERATOR, COUNSELOR, STAFF")
    @GetMapping("/{courseParticipantId}")
    @PreAuthorize("hasAnyRole('HEAD_OFFICE', 'REGIONAL_MANAGER', 'OPERATOR', 'COUNSELOR', 'STAFF')")
    public ApiResponse<CourseParticipantDetailResponse> findById(@PathVariable Long courseParticipantId) {
        return ApiResponse.success(courseParticipantService.findById(courseParticipantId));
    }

    @Operation(summary = "수강 정보 수정", description = "권한: HEAD_OFFICE, REGIONAL_MANAGER, OPERATOR")
    @PutMapping("/{courseParticipantId}")
    @PreAuthorize("hasAnyRole('HEAD_OFFICE', 'REGIONAL_MANAGER', 'OPERATOR')")
    public ApiResponse<CourseParticipantUpdatedResponse> update(
            @PathVariable Long courseParticipantId,
            @Valid @RequestBody UpdateCourseParticipantRequest request) {
        return ApiResponse.success(courseParticipantService.update(courseParticipantId, request));
    }

    @Operation(summary = "수강 삭제(하드)", description = "권한: OPERATOR")
    @DeleteMapping("/{courseParticipantId}")
    @PreAuthorize("hasRole('OPERATOR')")
    public ApiResponse<CourseParticipantDeletedResponse> delete(@PathVariable Long courseParticipantId) {
        return ApiResponse.success(courseParticipantService.delete(courseParticipantId));
    }

    @Operation(summary = "수강 취소", description = "권한: ADMIN, HEAD_OFFICE, OPERATOR")
    @PostMapping("/{courseParticipantId}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN', 'HEAD_OFFICE', 'OPERATOR')")
    public ApiResponse<CourseParticipantCanceledResponse> cancel(
            @PathVariable Long courseParticipantId,
            @Valid @RequestBody CancelCourseParticipantRequest request) {
        return ApiResponse.success(courseParticipantService.cancel(courseParticipantId, request));
    }

    @Operation(summary = "수료 처리", description = "권한: ADMIN, HEAD_OFFICE, OPERATOR")
    @PatchMapping("/{courseParticipantId}/completion")
    @PreAuthorize("hasAnyRole('ADMIN', 'HEAD_OFFICE', 'OPERATOR')")
    public ApiResponse<CourseParticipantCompletionResponse> complete(
            @PathVariable Long courseParticipantId,
            @Valid @RequestBody CompleteCourseParticipantRequest request) {
        return ApiResponse.success(courseParticipantService.complete(courseParticipantId, request));
    }

    @Operation(summary = "진행상태 변경",
            description = "진행상태를 지정한 값으로 변경한다 (APPLIED/CONFIRMED/CANCELED/COMPLETED/INCOMPLETE). "
                    + "권한: HEAD_OFFICE, REGIONAL_MANAGER, OPERATOR")
    @PatchMapping("/{courseParticipantId}/status")
    @PreAuthorize("hasAnyRole('HEAD_OFFICE', 'REGIONAL_MANAGER', 'OPERATOR')")
    public ApiResponse<CourseParticipantStatusChangedResponse> changeStatus(
            @PathVariable Long courseParticipantId,
            @Valid @RequestBody ChangeCourseParticipantStatusRequest request) {
        return ApiResponse.success(courseParticipantService.changeStatus(courseParticipantId, request));
    }

    @Operation(summary = "연락 시도 횟수 증가", description = "권한: ADMIN, COUNSELOR, OPERATOR")
    @PatchMapping("/{courseParticipantId}/contact-attempt")
    @PreAuthorize("hasAnyRole('ADMIN', 'COUNSELOR', 'OPERATOR')")
    public ApiResponse<ContactAttemptResponse> increaseContactAttempt(@PathVariable Long courseParticipantId) {
        return ApiResponse.success(courseParticipantService.increaseContactAttempt(courseParticipantId));
    }

    @Operation(summary = "상담사 변경", description = "권한: HEAD_OFFICE, REGIONAL_MANAGER, OPERATOR")
    @PatchMapping("/{courseParticipantId}/counselor")
    @PreAuthorize("hasAnyRole('HEAD_OFFICE', 'REGIONAL_MANAGER', 'OPERATOR')")
    public ApiResponse<CounselorChangedResponse> changeCounselor(
            @PathVariable Long courseParticipantId,
            @Valid @RequestBody ChangeCounselorRequest request) {
        return ApiResponse.success(courseParticipantService.changeCounselor(courseParticipantId, request));
    }

    @Operation(summary = "상담 세션 기록",
            description = "상담 시작/종료 일시·메모를 기록한다. 종료 일시 입력 시 해당 상담은 완료로 간주. "
                    + "권한: HEAD_OFFICE, REGIONAL_MANAGER, OPERATOR, COUNSELOR")
    @PatchMapping("/{courseParticipantId}/counselors/{counselingType}")
    @PreAuthorize("hasAnyRole('HEAD_OFFICE', 'REGIONAL_MANAGER', 'OPERATOR', 'COUNSELOR')")
    public ApiResponse<CounselingSessionResponse> recordCounselingSession(
            @PathVariable Long courseParticipantId,
            @Parameter(description = "상담 구분 — PRE_SESSION / POST_SESSION_1 / POST_SESSION_2")
            @PathVariable String counselingType,
            @Valid @RequestBody RecordCounselingSessionRequest request) {
        return ApiResponse.success(
                courseParticipantService.recordCounselingSession(courseParticipantId, counselingType, request));
    }
}
