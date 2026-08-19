package com.jobmoa.hopefulreturn.staffschedule.controller;

import com.jobmoa.hopefulreturn.common.ApiResponse;
import com.jobmoa.hopefulreturn.staffschedule.model.dto.BulkStaffScheduleRequest;
import com.jobmoa.hopefulreturn.staffschedule.model.dto.BulkStaffScheduleResponse;
import com.jobmoa.hopefulreturn.staffschedule.model.dto.CreateStaffScheduleRequest;
import com.jobmoa.hopefulreturn.staffschedule.model.dto.StaffScheduleDeletedResponse;
import com.jobmoa.hopefulreturn.staffschedule.model.dto.StaffScheduleListResponse;
import com.jobmoa.hopefulreturn.staffschedule.model.dto.StaffScheduleResponse;
import com.jobmoa.hopefulreturn.staffschedule.model.dto.UpdateStaffScheduleRequest;
import com.jobmoa.hopefulreturn.staffschedule.service.StaffScheduleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
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

@Tag(name = "StaffSchedule")
@RestController
@RequestMapping("/api/staff-schedules")
@RequiredArgsConstructor
public class StaffScheduleController {

    // 소유권 우회(타인 등록·타인 수정/삭제)가 허용되는 관리자 역할
    private static final String ROLE_ADMIN = "ROLE_ADMIN";
    private static final String ROLE_OPERATOR = "ROLE_OPERATOR";

    private final StaffScheduleService staffScheduleService;

    @Operation(summary = "스태프 일정 단건 등록",
            description = "본인 일정은 모든 스태프가 등록. 타인 지정(userId)은 ADMIN·OPERATOR만 가능")
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<StaffScheduleResponse> create(
            @RequestAttribute("userId") Long userId,
            Authentication authentication,
            @Valid @RequestBody CreateStaffScheduleRequest request) {
        return ApiResponse.success(staffScheduleService.create(userId, isManager(authentication), request));
    }

    @Operation(summary = "스태프 일정 캘린더 일괄 등록",
            description = "UNIQUE 충돌 항목은 스킵. 타인 지정은 ADMIN·OPERATOR만 가능")
    @PostMapping("/bulk")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<BulkStaffScheduleResponse> createBulk(
            @RequestAttribute("userId") Long userId,
            Authentication authentication,
            @Valid @RequestBody BulkStaffScheduleRequest request) {
        return ApiResponse.success(staffScheduleService.createBulk(userId, isManager(authentication), request));
    }

    @Operation(summary = "스태프 일정 목록/범위 조회",
            description = "권한: ADMIN, OPERATOR, HEAD_OFFICE, REGIONAL_MANAGER")
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'HEAD_OFFICE', 'REGIONAL_MANAGER')")
    public ApiResponse<StaffScheduleListResponse> findAll(
            @Parameter(description = "스태프 ID") @RequestParam(required = false) Long userId,
            @Parameter(description = "조회 시작일") @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @Parameter(description = "조회 종료일") @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @Parameter(description = "시간대(AM/PM/FULL)") @RequestParam(required = false) String sessionType,
            @Parameter(description = "페이지 번호") @RequestParam(required = false) Integer page,
            @Parameter(description = "페이지 크기") @RequestParam(required = false) Integer size) {
        return ApiResponse.success(
                staffScheduleService.findAll(userId, fromDate, toDate, sessionType, page, size));
    }

    @Operation(summary = "내 캘린더 조회", description = "인증 사용자 본인의 일정만 반환")
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<StaffScheduleListResponse> findMy(
            @RequestAttribute("userId") Long userId,
            @Parameter(description = "조회 시작일") @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @Parameter(description = "조회 종료일") @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        return ApiResponse.success(staffScheduleService.findMy(userId, fromDate, toDate));
    }

    @Operation(summary = "스태프 일정 상세 조회", description = "권한: 인증 사용자")
    @GetMapping("/{staffScheduleId}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<StaffScheduleResponse> findById(@PathVariable Long staffScheduleId) {
        return ApiResponse.success(staffScheduleService.findById(staffScheduleId));
    }

    @Operation(summary = "스태프 일정 수정",
            description = "소유자 또는 ADMIN·OPERATOR만 수정. sessionType·isAvailable·memo만 변경")
    @PutMapping("/{staffScheduleId}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<StaffScheduleResponse> update(
            @PathVariable Long staffScheduleId,
            @RequestAttribute("userId") Long userId,
            Authentication authentication,
            @Valid @RequestBody UpdateStaffScheduleRequest request) {
        return ApiResponse.success(
                staffScheduleService.update(staffScheduleId, userId, isManager(authentication), request));
    }

    @Operation(summary = "스태프 일정 삭제(하드)",
            description = "소유자 또는 ADMIN·OPERATOR만 삭제. 배정된 날짜(course_staff) 삭제 시 reason 을 담으면 "
                    + "배정 관리자에게 재배정 알림 메일이 발송된다(사유 포함).")
    @DeleteMapping("/{staffScheduleId}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<StaffScheduleDeletedResponse> delete(
            @PathVariable Long staffScheduleId,
            @RequestAttribute("userId") Long userId,
            Authentication authentication,
            @Parameter(description = "삭제 사유(배정된 날짜 삭제 시 알림 메일 본문에 포함)")
            @RequestParam(required = false) String reason) {
        return ApiResponse.success(
                staffScheduleService.delete(staffScheduleId, userId, isManager(authentication), reason));
    }

    // 소유권 우회가 가능한 관리자(ADMIN·OPERATOR) 여부를 인증 권한에서 판별한다.
    private boolean isManager(Authentication authentication) {
        if (authentication == null || authentication.getAuthorities() == null) {
            return false;
        }
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            String role = authority.getAuthority();
            if (ROLE_ADMIN.equals(role) || ROLE_OPERATOR.equals(role)) {
                return true;
            }
        }
        return false;
    }
}
