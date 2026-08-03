package com.jobmoa.hopefulreturn.attendance.controller;

import com.jobmoa.hopefulreturn.attendance.model.dto.*;
import com.jobmoa.hopefulreturn.attendance.service.AttendanceService;
import com.jobmoa.hopefulreturn.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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
import com.jobmoa.hopefulreturn.security.AuthScopeSupport;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RequestAttribute;

@Tag(name = "Attendance")
@RestController
@RequestMapping("/api/attendances")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService attendanceService;

    @Operation(summary = "출석 등록", description = "권한: OPERATOR, STAFF")
    @PostMapping
    @PreAuthorize("hasAnyRole('OPERATOR', 'STAFF')")
    public ApiResponse<AttendanceResponse> register(@Valid @RequestBody RegisterAttendanceRequest request) {
        return ApiResponse.success(attendanceService.register(request));
    }

    @Operation(summary = "일차별 출석 일괄 등록", description = "권한: ADMIN, OPERATOR, STAFF")
    @PostMapping("/bulk")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'STAFF')")
    public ApiResponse<BulkAttendanceResponse> registerBulk(@Valid @RequestBody BulkAttendanceRequest request) {
        return ApiResponse.success(attendanceService.registerBulk(request));
    }

    @Operation(summary = "출석 목록 조회", description = "권한: ADMIN, HEAD_OFFICE, REGIONAL_MANAGER, PROJECT_MANAGER, PROJECT_LEADER, OPERATOR, COUNSELOR, STAFF")
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'HEAD_OFFICE', 'REGIONAL_MANAGER', 'PROJECT_MANAGER', 'PROJECT_LEADER'," +
            " 'OPERATOR', 'COUNSELOR', 'STAFF')")
    public ApiResponse<AttendanceListResponse> findAll(
            @RequestParam(required = false) Long courseId,
            @RequestParam(required = false) Long courseParticipantId,
            @RequestParam(required = false) Integer dayNo,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestAttribute(name = "userId", required = false) Long userId,
            Authentication authentication) {

        Long staffScopeId =
                AuthScopeSupport.isStaffOnly(authentication)
                        ? userId
                        : null;

        return ApiResponse.success(
                attendanceService.findAll(
                        courseId,
                        courseParticipantId,
                        dayNo,
                        status,
                        staffScopeId,
                        page,
                        size));
    }

    @Operation(summary = "출석 상세 조회", description = "권한: ADMIN, HEAD_OFFICE, REGIONAL_MANAGER, PROJECT_MANAGER, PROJECT_LEADER, OPERATOR, COUNSELOR, STAFF")
    @GetMapping("/{attendanceId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HEAD_OFFICE', 'REGIONAL_MANAGER', 'PROJECT_MANAGER', 'PROJECT_LEADER'," +
            " 'OPERATOR', 'COUNSELOR', 'STAFF')")
    public ApiResponse<AttendanceDetailResponse> findById(@PathVariable Long attendanceId) {
        return ApiResponse.success(attendanceService.findById(attendanceId));
    }

    @Operation(summary = "출석 수정", description = "권한: OPERATOR, STAFF")
    @PutMapping("/{attendanceId}")
    @PreAuthorize("hasAnyRole('OPERATOR', 'STAFF')")
    public ApiResponse<AttendanceUpdatedResponse> update(
            @PathVariable Long attendanceId,
            @Valid @RequestBody UpdateAttendanceRequest request) {
        return ApiResponse.success(attendanceService.update(attendanceId, request));
    }

    @Operation(summary = "출석 삭제(하드)", description = "권한: ADMIN")
    @DeleteMapping("/{attendanceId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<AttendanceDeletedResponse> delete(@PathVariable Long attendanceId) {
        return ApiResponse.success(attendanceService.delete(attendanceId));
    }

    @GetMapping("/courses/{courseId}/completion-risk")
    @Operation(summary = "수료 위험도 조회")
    public ResponseEntity<CompletionRiskListResponse> findCompletionRisk(
            @PathVariable Long courseId) {
        return ResponseEntity.ok(
                attendanceService.findCompletionRisk(courseId)
        );
    }
}