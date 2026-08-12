package com.jobmoa.hopefulreturn.course.controller;

import com.jobmoa.hopefulreturn.common.ApiResponse;
import com.jobmoa.hopefulreturn.course.model.dto.CourseDetailResponse;
import com.jobmoa.hopefulreturn.course.model.dto.CourseListResponse;
import com.jobmoa.hopefulreturn.course.model.dto.CourseParticipantListResponse;
import com.jobmoa.hopefulreturn.course.model.dto.CourseStaffListResponse;
import com.jobmoa.hopefulreturn.course.model.dto.CourseStaffSmsHistoryResponse;
import com.jobmoa.hopefulreturn.course.model.dto.CreateCourseRequest;
import com.jobmoa.hopefulreturn.course.model.dto.CreateCourseResponse;
import com.jobmoa.hopefulreturn.course.model.dto.DeleteCourseResponse;
import com.jobmoa.hopefulreturn.course.model.dto.NotifyCourseChangeRequest;
import com.jobmoa.hopefulreturn.course.model.dto.NotifyCourseChangeResponse;
import com.jobmoa.hopefulreturn.course.model.dto.UpdateCourseRequest;
import com.jobmoa.hopefulreturn.course.model.dto.UpdateCourseResponse;
import com.jobmoa.hopefulreturn.course.model.dto.UpdateCourseStatusRequest;
import com.jobmoa.hopefulreturn.course.model.dto.UpdateCourseStatusResponse;
import com.jobmoa.hopefulreturn.course.scope.CourseScope;
import com.jobmoa.hopefulreturn.course.scope.CourseScopeResolver;
import com.jobmoa.hopefulreturn.course.service.CourseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Course")
@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;
    private final CourseScopeResolver courseScopeResolver;

    @Operation(summary = "강좌 등록", description = "권한: ADMIN, HEAD_OFFICE, REGIONAL_MANAGER")
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'HEAD_OFFICE', 'REGIONAL_MANAGER')")
    public ApiResponse<CreateCourseResponse> create(
            @Valid @RequestBody CreateCourseRequest request,
            @RequestAttribute("userId") Long userId) {
        return ApiResponse.success(courseService.create(request, userId));
    }

    @Operation(summary = "강좌 목록 조회", description = "권한: ADMIN, HEAD_OFFICE, REGIONAL_MANAGER, OPERATOR")
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<CourseListResponse> findAll(
            @Parameter(description = "지역 ID") @RequestParam(required = false) Long regionId,
            @Parameter(description = "상위 지역 ID (해당 상위지역의 모든 하위지역 포함 조회)") @RequestParam(required = false) Long parentRegionId,
            @Parameter(description = "강좌 상태") @RequestParam(required = false) String status,
            @Parameter(description = "검색어") @RequestParam(required = false) String keyword,
            @Parameter(description = "정렬 키(courseName/regionName/courseNumber/localCourseNumber/capacity/status/day1Date/planSubmitDate)")
            @RequestParam(required = false) String sortBy,
            @Parameter(description = "정렬 방향(asc/desc, 기본 asc)") @RequestParam(required = false) String sortOrder,
            @Parameter(description = "페이지 번호") @RequestParam(required = false) Integer page,
            @Parameter(description = "페이지 크기") @RequestParam(required = false) Integer size,
            @RequestAttribute(name = "userId", required = false) Long userId,
            Authentication authentication) {
        CourseScope scope = courseScopeResolver.resolve(authentication, userId);
        return ApiResponse.success(
                courseService.findAll(regionId, parentRegionId, status, keyword, scope, sortBy, sortOrder, page, size));
    }

    @Operation(summary = "강좌 상세 조회", description = "권한: 로그인 사용자")
    @GetMapping("/{courseId}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<CourseDetailResponse> findById(@PathVariable Long courseId) {
        return ApiResponse.success(courseService.findById(courseId));
    }

    @Operation(summary = "강좌 수정", description = "권한: ADMIN, HEAD_OFFICE, REGIONAL_MANAGER")
    @PutMapping("/{courseId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HEAD_OFFICE', 'REGIONAL_MANAGER')")
    public ApiResponse<UpdateCourseResponse> update(
            @PathVariable Long courseId,
            @Valid @RequestBody UpdateCourseRequest request) {
        return ApiResponse.success(courseService.update(courseId, request));
    }

    @Operation(summary = "강좌 상태 변경", description = "권한: ADMIN, HEAD_OFFICE")
    @PatchMapping("/{courseId}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'HEAD_OFFICE')")
    public ApiResponse<UpdateCourseStatusResponse> updateStatus(
            @PathVariable Long courseId,
            @Valid @RequestBody UpdateCourseStatusRequest request,
            @RequestAttribute("userId") Long userId) {
        return ApiResponse.success(courseService.updateStatus(courseId, request, userId));
    }

    @Operation(summary = "강좌 일정/장소 변경 안내 문자 발송",
            description = "교육일·교육시간·휴게시간·교육장 변경 시 선택한 담당자(PM 제외)에게 안내 문자를 발송한다. "
                    + "권한: ADMIN, HEAD_OFFICE, REGIONAL_MANAGER")
    @PostMapping("/{courseId}/notify-schedule-change")
    @PreAuthorize("hasAnyRole('ADMIN', 'HEAD_OFFICE', 'REGIONAL_MANAGER')")
    public ApiResponse<NotifyCourseChangeResponse> notifyScheduleChange(
            @PathVariable Long courseId,
            @Valid @RequestBody NotifyCourseChangeRequest request,
            @RequestAttribute("userId") Long userId) {
        return ApiResponse.success(courseService.notifyScheduleChange(courseId, request, userId));
    }

    @Operation(summary = "강좌 담당자 안내 문자 발송 이력 조회",
            description = "강좌 상태변경(STATUS_CHANGE)·일정변경(SCHEDULE_CHANGE) 시 담당자에게 보낸 안내 문자 발송 이력을 조회한다. "
                    + "권한: 로그인 사용자")
    @GetMapping("/{courseId}/staff-sms-history")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<CourseStaffSmsHistoryResponse> findStaffSmsHistory(@PathVariable Long courseId) {
        return ApiResponse.success(courseService.findStaffSmsHistory(courseId));
    }

    @Operation(summary = "강좌 삭제", description = "권한: ADMIN")
    @DeleteMapping("/{courseId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<DeleteCourseResponse> delete(@PathVariable Long courseId) {
        return ApiResponse.success(courseService.delete(courseId));
    }

    @Operation(summary = "강좌 참여자 목록 조회",
            description = "권한: ADMIN, HEAD_OFFICE, REGIONAL_MANAGER, PROJECT_MANAGER, PROJECT_LEADER, OPERATOR, COUNSELOR, STAFF")
    @GetMapping("/{courseId}/participants")
    @PreAuthorize("hasAnyRole('ADMIN', 'HEAD_OFFICE', 'REGIONAL_MANAGER', 'PROJECT_MANAGER', 'PROJECT_LEADER',"
            + " 'OPERATOR', 'COUNSELOR', 'STAFF')")
    public ApiResponse<CourseParticipantListResponse> findParticipants(
            @PathVariable Long courseId,
            @Parameter(description = "참여 상태") @RequestParam(required = false) String status,
            @Parameter(description = "참여자명 검색어") @RequestParam(required = false) String keyword,
            @Parameter(description = "페이지 번호") @RequestParam(required = false) Integer page,
            @Parameter(description = "페이지 크기") @RequestParam(required = false) Integer size,
            @RequestAttribute(name = "userId", required = false) Long userId,
            Authentication authentication) {
        CourseScope scope = courseScopeResolver.resolve(authentication, userId);
        return ApiResponse.success(courseService.findParticipants(courseId, status, keyword, scope, page, size));
    }

    @Operation(summary = "강좌 담당자 목록 조회", description = "권한: 로그인 사용자")
    @GetMapping("/{courseId}/staffs")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<CourseStaffListResponse> findStaffs(@PathVariable Long courseId) {
        return ApiResponse.success(courseService.findStaffs(courseId));
    }
}