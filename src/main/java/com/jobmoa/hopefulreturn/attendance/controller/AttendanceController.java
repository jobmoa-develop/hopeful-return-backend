package com.jobmoa.hopefulreturn.attendance.controller;

import com.jobmoa.hopefulreturn.attendance.model.dto.AttendanceDeletedResponse;
import com.jobmoa.hopefulreturn.attendance.model.dto.AttendanceDetailResponse;
import com.jobmoa.hopefulreturn.attendance.model.dto.AttendanceListResponse;
import com.jobmoa.hopefulreturn.attendance.model.dto.AttendanceResponse;
import com.jobmoa.hopefulreturn.attendance.model.dto.AttendanceUpdatedResponse;
import com.jobmoa.hopefulreturn.attendance.model.dto.BulkAttendanceRequest;
import com.jobmoa.hopefulreturn.attendance.model.dto.BulkAttendanceResponse;
import com.jobmoa.hopefulreturn.attendance.model.dto.RegisterAttendanceRequest;
import com.jobmoa.hopefulreturn.attendance.model.dto.UpdateAttendanceRequest;
import com.jobmoa.hopefulreturn.attendance.service.AttendanceService;
import com.jobmoa.hopefulreturn.common.ApiResponse;
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

    @Operation(summary = "출석 목록 조회", description = "권한: OPERATOR, STAFF, HEAD_OFFICE, REGIONAL_MANAGER, COUNSELOR")
    @GetMapping
    @PreAuthorize("hasAnyRole('OPERATOR', 'STAFF', 'HEAD_OFFICE', 'REGIONAL_MANAGER', 'COUNSELOR')")
    public ApiResponse<AttendanceListResponse> findAll(
            @Parameter(description = "강좌 ID") @RequestParam(required = false) Long courseId,
            @Parameter(description = "수업 차수(일차)") @RequestParam(required = false) Integer dayNo,
            @Parameter(description = "출결 상태") @RequestParam(required = false) String status,
            @Parameter(description = "페이지 번호") @RequestParam(required = false) Integer page,
            @Parameter(description = "페이지 크기") @RequestParam(required = false) Integer size) {
        return ApiResponse.success(attendanceService.findAll(courseId, dayNo, status, page, size));
    }

    @Operation(summary = "출석 상세 조회", description = "권한: OPERATOR, STAFF, HEAD_OFFICE, REGIONAL_MANAGER, COUNSELOR")
    @GetMapping("/{attendanceId}")
    @PreAuthorize("hasAnyRole('OPERATOR', 'STAFF', 'HEAD_OFFICE', 'REGIONAL_MANAGER', 'COUNSELOR')")
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
}
