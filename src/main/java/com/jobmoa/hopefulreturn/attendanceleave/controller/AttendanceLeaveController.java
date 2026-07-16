package com.jobmoa.hopefulreturn.attendanceleave.controller;

import com.jobmoa.hopefulreturn.attendanceleave.model.dto.AttendanceLeaveDeletedResponse;
import com.jobmoa.hopefulreturn.attendanceleave.model.dto.AttendanceLeaveDetailResponse;
import com.jobmoa.hopefulreturn.attendanceleave.model.dto.AttendanceLeaveResponse;
import com.jobmoa.hopefulreturn.attendanceleave.model.dto.AttendanceLeaveUpdatedResponse;
import com.jobmoa.hopefulreturn.attendanceleave.model.dto.RegisterAttendanceLeaveRequest;
import com.jobmoa.hopefulreturn.attendanceleave.model.dto.UpdateAttendanceLeaveRequest;
import com.jobmoa.hopefulreturn.attendanceleave.service.AttendanceLeaveService;
import com.jobmoa.hopefulreturn.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
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
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "AttendanceLeave")
@RestController
@RequestMapping("/api/attendance-leaves")
@RequiredArgsConstructor
public class AttendanceLeaveController {

    private final AttendanceLeaveService attendanceLeaveService;

    @Operation(summary = "조퇴·외출 등록", description = "권한: OPERATOR")
    @PostMapping
    @PreAuthorize("hasRole('OPERATOR')")
    public ApiResponse<AttendanceLeaveResponse> register(
            @Valid @RequestBody RegisterAttendanceLeaveRequest request) {
        return ApiResponse.success(attendanceLeaveService.register(request));
    }

    @Operation(summary = "조퇴·외출 상세 조회",
            description = "권한: ADMIN, HEAD_OFFICE, REGIONAL_MANAGER, PROJECT_MANAGER, PROJECT_LEADER, OPERATOR, COUNSELOR, STAFF")
    @GetMapping("/{attendanceLeaveId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HEAD_OFFICE', 'REGIONAL_MANAGER', 'PROJECT_MANAGER', 'PROJECT_LEADER',"
            + " 'OPERATOR', 'COUNSELOR', 'STAFF')")
    public ApiResponse<AttendanceLeaveDetailResponse> findById(@PathVariable Long attendanceLeaveId) {
        return ApiResponse.success(attendanceLeaveService.findById(attendanceLeaveId));
    }

    @Operation(summary = "조퇴·외출 수정", description = "권한: OPERATOR")
    @PutMapping("/{attendanceLeaveId}")
    @PreAuthorize("hasRole('OPERATOR')")
    public ApiResponse<AttendanceLeaveUpdatedResponse> update(
            @PathVariable Long attendanceLeaveId,
            @Valid @RequestBody UpdateAttendanceLeaveRequest request) {
        return ApiResponse.success(attendanceLeaveService.update(attendanceLeaveId, request));
    }

    @Operation(summary = "조퇴·외출 삭제(하드)", description = "권한: ADMIN")
    @DeleteMapping("/{attendanceLeaveId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<AttendanceLeaveDeletedResponse> delete(@PathVariable Long attendanceLeaveId) {
        return ApiResponse.success(attendanceLeaveService.delete(attendanceLeaveId));
    }
}
