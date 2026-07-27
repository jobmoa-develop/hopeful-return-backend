package com.jobmoa.hopefulreturn.courseparticipant.controller;

import com.jobmoa.hopefulreturn.common.ApiResponse;
import com.jobmoa.hopefulreturn.courseparticipant.model.dto.AssignSlotCounselorRequest;
import com.jobmoa.hopefulreturn.courseparticipant.model.dto.AssignableCounselorResponse;
import com.jobmoa.hopefulreturn.courseparticipant.model.dto.BulkAssignCounselorRequest;
import com.jobmoa.hopefulreturn.courseparticipant.model.dto.BulkCompleteCourseParticipantRequest;
import com.jobmoa.hopefulreturn.courseparticipant.model.dto.BulkCompletionResponse;
import com.jobmoa.hopefulreturn.courseparticipant.model.dto.BulkCounselorAssignResponse;
import com.jobmoa.hopefulreturn.courseparticipant.model.dto.BulkImportCommitRequest;
import com.jobmoa.hopefulreturn.courseparticipant.model.dto.BulkImportPreviewResponse;
import com.jobmoa.hopefulreturn.courseparticipant.model.dto.BulkImportResultResponse;
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
import com.jobmoa.hopefulreturn.courseparticipant.scope.ParticipantScope;
import com.jobmoa.hopefulreturn.courseparticipant.scope.ParticipantScopeResolver;
import com.jobmoa.hopefulreturn.courseparticipant.service.CourseParticipantService;
import com.jobmoa.hopefulreturn.courseparticipant.service.ParticipantBulkImportService;
import com.jobmoa.hopefulreturn.security.AuthScopeSupport;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "CourseParticipant")
@RestController
@RequestMapping("/api/course-participants")
@RequiredArgsConstructor
public class CourseParticipantController {

    private final CourseParticipantService courseParticipantService;
    private final ParticipantBulkImportService participantBulkImportService;
    private final ParticipantScopeResolver participantScopeResolver;

    @Operation(summary = "수강 등록", description = "권한: ADMIN, HEAD_OFFICE, REGIONAL_MANAGER, PROJECT_MANAGER, PROJECT_LEADER")
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'HEAD_OFFICE', 'REGIONAL_MANAGER', 'PROJECT_MANAGER', 'PROJECT_LEADER')")
    public ApiResponse<CourseParticipantCreatedResponse> create(
            @Valid @RequestBody CreateCourseParticipantRequest request) {
        return ApiResponse.success(courseParticipantService.create(request));
    }

    @Operation(summary = "수강생 목록 조회",
            description = "검색 필터(지역/회차/상태/검색어) 지원. COUNSELOR 는 본인이 배정된 수강건만 조회된다(서버측 강제). "
                    + "권한: ADMIN, HEAD_OFFICE, REGIONAL_MANAGER, PROJECT_MANAGER, PROJECT_LEADER, OPERATOR, COUNSELOR, STAFF")
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'HEAD_OFFICE', 'REGIONAL_MANAGER', 'PROJECT_MANAGER', 'PROJECT_LEADER',"
            + " 'OPERATOR', 'COUNSELOR', 'STAFF')")
    public ApiResponse<CourseParticipantListResponse> findAll(
            @Parameter(description = "강좌 ID") @RequestParam(required = false) Long courseId,
            @Parameter(description = "지역 ID") @RequestParam(required = false) Long regionId,
            @Parameter(description = "회차(course_number)") @RequestParam(required = false) Integer courseNumber,
            @Parameter(description = "수강 상태") @RequestParam(required = false) String status,
            @Parameter(description = "검색어(참여자명/전화번호)") @RequestParam(required = false) String keyword,
            @Parameter(description = "등록일(전산 등록일) 시작 YYYY-MM-DD")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate registerDateFrom,
            @Parameter(description = "등록일(전산 등록일) 종료 YYYY-MM-DD")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate registerDateTo,
            @Parameter(description = "페이지 번호") @RequestParam(required = false) Integer page,
            @Parameter(description = "페이지 크기") @RequestParam(required = false) Integer size,
            @RequestAttribute(name = "userId", required = false) Long userId,
            Authentication authentication) {
        // 역할별 조회 스코프를 서버측에서 강제한다(FE 우회 불가).
        // 진행자(STAFF)=배정 회차 참여자, 상담사(COUNSELOR)=개별 배정 상담 건, 관리자급=제한 없음.
        ParticipantScope scope = participantScopeResolver.resolve(authentication, userId);
        return ApiResponse.success(courseParticipantService.findAll(
                courseId, regionId, courseNumber, status, keyword, scope.courseParticipantIds(),
                registerDateFrom, registerDateTo, page, size));
    }

    @Operation(summary = "수강생 상세 조회",
            description = "권한: ADMIN, HEAD_OFFICE, REGIONAL_MANAGER, PROJECT_MANAGER, PROJECT_LEADER, OPERATOR, COUNSELOR, STAFF")
    @GetMapping("/{courseParticipantId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HEAD_OFFICE', 'REGIONAL_MANAGER', 'PROJECT_MANAGER', 'PROJECT_LEADER',"
            + " 'OPERATOR', 'COUNSELOR', 'STAFF')")
    public ApiResponse<CourseParticipantDetailResponse> findById(
            @PathVariable Long courseParticipantId,
            @RequestAttribute(name = "userId", required = false) Long userId,
            Authentication authentication) {
        // 상세 조회도 목록과 동일 스코프를 강제한다 — 배정 외 수강건 ID 직접 조회 우회 차단.
        ParticipantScope scope = participantScopeResolver.resolve(authentication, userId);
        return ApiResponse.success(
                courseParticipantService.findById(courseParticipantId, scope.courseParticipantIds()));
    }

    @Operation(summary = "수강 정보 수정",
            description = "권한: ADMIN, HEAD_OFFICE, REGIONAL_MANAGER, PROJECT_MANAGER, PROJECT_LEADER, OPERATOR")
    @PutMapping("/{courseParticipantId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HEAD_OFFICE', 'REGIONAL_MANAGER', 'PROJECT_MANAGER', 'PROJECT_LEADER', 'OPERATOR')")
    public ApiResponse<CourseParticipantUpdatedResponse> update(
            @PathVariable Long courseParticipantId,
            @Valid @RequestBody UpdateCourseParticipantRequest request) {
        return ApiResponse.success(courseParticipantService.update(courseParticipantId, request));
    }

    @Operation(summary = "수강 삭제(하드)", description = "권한: ADMIN, OPERATOR")
    @DeleteMapping("/{courseParticipantId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ApiResponse<CourseParticipantDeletedResponse> delete(@PathVariable Long courseParticipantId) {
        return ApiResponse.success(courseParticipantService.delete(courseParticipantId));
    }

    @Operation(summary = "수강 취소",
            description = "권한: ADMIN, HEAD_OFFICE, REGIONAL_MANAGER, PROJECT_MANAGER, PROJECT_LEADER, OPERATOR")
    @PostMapping("/{courseParticipantId}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN', 'HEAD_OFFICE', 'REGIONAL_MANAGER', 'PROJECT_MANAGER', 'PROJECT_LEADER', 'OPERATOR')")
    public ApiResponse<CourseParticipantCanceledResponse> cancel(
            @PathVariable Long courseParticipantId,
            @Valid @RequestBody CancelCourseParticipantRequest request) {
        return ApiResponse.success(courseParticipantService.cancel(courseParticipantId, request));
    }

    @Operation(summary = "수료 처리",
            description = "권한: ADMIN, HEAD_OFFICE, REGIONAL_MANAGER, PROJECT_MANAGER, PROJECT_LEADER, OPERATOR")
    @PatchMapping("/{courseParticipantId}/completion")
    @PreAuthorize("hasAnyRole('ADMIN', 'HEAD_OFFICE', 'REGIONAL_MANAGER', 'PROJECT_MANAGER', 'PROJECT_LEADER', 'OPERATOR')")
    public ApiResponse<CourseParticipantCompletionResponse> complete(
            @PathVariable Long courseParticipantId,
            @Valid @RequestBody CompleteCourseParticipantRequest request) {
        return ApiResponse.success(courseParticipantService.complete(courseParticipantId, request));
    }

    @Operation(summary = "일괄 수료 처리",
            description = "선택한 수강건들에 동일한 수료/미수료 상태·수료일·미수료 사유를 일괄 적용한다. "
                    + "권한: ADMIN, HEAD_OFFICE, REGIONAL_MANAGER, PROJECT_MANAGER, PROJECT_LEADER, OPERATOR")
    @PatchMapping("/completion/bulk")
    @PreAuthorize("hasAnyRole('ADMIN', 'HEAD_OFFICE', 'REGIONAL_MANAGER', 'PROJECT_MANAGER', 'PROJECT_LEADER', 'OPERATOR')")
    public ApiResponse<BulkCompletionResponse> bulkComplete(
            @Valid @RequestBody BulkCompleteCourseParticipantRequest request) {
        return ApiResponse.success(courseParticipantService.bulkComplete(request));
    }

    @Operation(summary = "상담 슬롯 상담사 일괄 배정",
            description = "선택한 수강건들에 동일 상담 구분(PRE_SESSION/POST_SESSION_1/POST_SESSION_2)의 상담사를 일괄 지정한다. "
                    + "지정 대상은 각 수강건 회차에 인력 배치된 상담사여야 하며, 한 건이라도 실패하면 전체 롤백된다. "
                    + "권한: ADMIN, HEAD_OFFICE, REGIONAL_MANAGER, PROJECT_MANAGER, PROJECT_LEADER, OPERATOR")
    @PatchMapping("/counselors/bulk")
    @PreAuthorize("hasAnyRole('ADMIN', 'HEAD_OFFICE', 'REGIONAL_MANAGER', 'PROJECT_MANAGER', 'PROJECT_LEADER', 'OPERATOR')")
    public ApiResponse<BulkCounselorAssignResponse> bulkAssignCounselor(
            @Valid @RequestBody BulkAssignCounselorRequest request) {
        return ApiResponse.success(courseParticipantService.bulkAssignCounselor(request));
    }

    @Operation(summary = "일괄 등록 미리보기",
            description = "정부식 참여자 XLSX 를 업로드하면 교육과정명별 그룹·행을 파싱해 반환한다(DB 쓰기 없음). "
                    + "회차 미존재/오류 행은 커밋 단계에서 스킵되며, 이 응답으로 매핑을 준비한다. "
                    + "권한: ADMIN, HEAD_OFFICE, REGIONAL_MANAGER, PROJECT_MANAGER, PROJECT_LEADER")
    @PostMapping(value = "/bulk-import/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'HEAD_OFFICE', 'REGIONAL_MANAGER', 'PROJECT_MANAGER', 'PROJECT_LEADER')")
    public ApiResponse<BulkImportPreviewResponse> bulkImportPreview(
            @RequestPart("file") MultipartFile file) {
        return ApiResponse.success(participantBulkImportService.preview(file));
    }

    @Operation(summary = "일괄 등록 커밋",
            description = "미리보기 후 운영자가 확인·수정한 행 목록(각 행의 대상 회차 포함)을 받아 참여자를 등록한다. "
                    + "서버가 전화 정규화·필수값·상태·회차 존재·중복을 재검증하며, 참여자는 중복(matchKey/전화) 시 재사용하고 "
                    + "같은 회차 중복 등록·미매핑·오류 행은 스킵해 리포트한다. "
                    + "권한: ADMIN, HEAD_OFFICE, REGIONAL_MANAGER, PROJECT_MANAGER, PROJECT_LEADER")
    @PostMapping("/bulk-import/commit")
    @PreAuthorize("hasAnyRole('ADMIN', 'HEAD_OFFICE', 'REGIONAL_MANAGER', 'PROJECT_MANAGER', 'PROJECT_LEADER')")
    public ApiResponse<BulkImportResultResponse> bulkImportCommit(
            @Valid @RequestBody BulkImportCommitRequest request) {
        return ApiResponse.success(participantBulkImportService.commit(request));
    }

    @Operation(summary = "진행상태 변경",
            description = "진행상태를 지정한 값으로 변경한다 (APPLIED/CONFIRMED/CANCELED/COMPLETED/INCOMPLETE). "
                    + "권한: ADMIN, HEAD_OFFICE, REGIONAL_MANAGER, PROJECT_MANAGER, PROJECT_LEADER, OPERATOR")
    @PatchMapping("/{courseParticipantId}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'HEAD_OFFICE', 'REGIONAL_MANAGER', 'PROJECT_MANAGER', 'PROJECT_LEADER', 'OPERATOR')")
    public ApiResponse<CourseParticipantStatusChangedResponse> changeStatus(
            @PathVariable Long courseParticipantId,
            @Valid @RequestBody ChangeCourseParticipantStatusRequest request) {
        return ApiResponse.success(courseParticipantService.changeStatus(courseParticipantId, request));
    }

    @Operation(summary = "연락 시도 횟수 증가",
            description = "권한: ADMIN, HEAD_OFFICE, REGIONAL_MANAGER, PROJECT_MANAGER, PROJECT_LEADER, OPERATOR, COUNSELOR")
    @PatchMapping("/{courseParticipantId}/contact-attempt")
    @PreAuthorize("hasAnyRole('ADMIN', 'HEAD_OFFICE', 'REGIONAL_MANAGER', 'PROJECT_MANAGER', 'PROJECT_LEADER',"
            + " 'OPERATOR', 'COUNSELOR')")
    public ApiResponse<ContactAttemptResponse> increaseContactAttempt(@PathVariable Long courseParticipantId) {
        return ApiResponse.success(courseParticipantService.increaseContactAttempt(courseParticipantId));
    }

    @Operation(summary = "상담사 변경",
            description = "권한: ADMIN, HEAD_OFFICE, REGIONAL_MANAGER, PROJECT_MANAGER, PROJECT_LEADER, OPERATOR")
    @PatchMapping("/{courseParticipantId}/counselor")
    @PreAuthorize("hasAnyRole('ADMIN', 'HEAD_OFFICE', 'REGIONAL_MANAGER', 'PROJECT_MANAGER', 'PROJECT_LEADER', 'OPERATOR')")
    public ApiResponse<CounselorChangedResponse> changeCounselor(
            @PathVariable Long courseParticipantId,
            @Valid @RequestBody ChangeCounselorRequest request) {
        return ApiResponse.success(courseParticipantService.changeCounselor(courseParticipantId, request));
    }

    @Operation(summary = "상담 세션 기록",
            description = "상담 시작/종료 일시·메모를 기록한다. 종료 일시 입력 시 해당 상담은 완료로 간주. "
                    + "권한: ADMIN, HEAD_OFFICE, REGIONAL_MANAGER, PROJECT_MANAGER, PROJECT_LEADER, OPERATOR, COUNSELOR")
    @PatchMapping("/{courseParticipantId}/counselors/{counselingType}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HEAD_OFFICE', 'REGIONAL_MANAGER', 'PROJECT_MANAGER', 'PROJECT_LEADER',"
            + " 'OPERATOR', 'COUNSELOR')")
    public ApiResponse<CounselingSessionResponse> recordCounselingSession(
            @PathVariable Long courseParticipantId,
            @Parameter(description = "상담 구분 — PRE_SESSION / POST_SESSION_1 / POST_SESSION_2")
            @PathVariable String counselingType,
            @Valid @RequestBody RecordCounselingSessionRequest request,
            @RequestAttribute(name = "userId", required = false) Long userId,
            Authentication authentication) {
        return ApiResponse.success(courseParticipantService.recordCounselingSession(
                courseParticipantId, counselingType, request, userId, AuthScopeSupport.isCounselorOnly(authentication)));
    }

    @Operation(summary = "배정 가능 상담사 조회",
            description = "해당 수강건의 회차에 인력 배치된 상담사 목록을 반환한다(상담사 지정 드롭다운용). "
                    + "권한: ADMIN, HEAD_OFFICE, REGIONAL_MANAGER, PROJECT_MANAGER, PROJECT_LEADER, OPERATOR, COUNSELOR")
    @GetMapping("/{courseParticipantId}/assignable-counselors")
    @PreAuthorize("hasAnyRole('ADMIN', 'HEAD_OFFICE', 'REGIONAL_MANAGER', 'PROJECT_MANAGER', 'PROJECT_LEADER',"
            + " 'OPERATOR', 'COUNSELOR')")
    public ApiResponse<AssignableCounselorResponse> findAssignableCounselors(
            @PathVariable Long courseParticipantId) {
        return ApiResponse.success(courseParticipantService.findAssignableCounselors(courseParticipantId));
    }

    @Operation(summary = "상담 슬롯 상담사 지정",
            description = "특정 상담 구분(PRE_SESSION/POST_SESSION_1/POST_SESSION_2)의 상담사를 지정·변경한다. "
                    + "COUNSELOR 는 본인이 배정된 참여자에 한해 지정 가능하며, 지정 대상은 해당 회차에 인력 배치된 상담사여야 한다. "
                    + "권한: ADMIN, HEAD_OFFICE, REGIONAL_MANAGER, PROJECT_MANAGER, PROJECT_LEADER, OPERATOR, COUNSELOR")
    @PatchMapping("/{courseParticipantId}/counselors/{counselingType}/counselor")
    @PreAuthorize("hasAnyRole('ADMIN', 'HEAD_OFFICE', 'REGIONAL_MANAGER', 'PROJECT_MANAGER', 'PROJECT_LEADER',"
            + " 'OPERATOR', 'COUNSELOR')")
    public ApiResponse<CounselorChangedResponse> assignSlotCounselor(
            @PathVariable Long courseParticipantId,
            @Parameter(description = "상담 구분 — PRE_SESSION / POST_SESSION_1 / POST_SESSION_2")
            @PathVariable String counselingType,
            @Valid @RequestBody AssignSlotCounselorRequest request,
            @RequestAttribute(name = "userId", required = false) Long userId,
            Authentication authentication) {
        return ApiResponse.success(courseParticipantService.assignSlotCounselor(
                courseParticipantId, counselingType, request, userId, AuthScopeSupport.isCounselorOnly(authentication)));
    }
}
