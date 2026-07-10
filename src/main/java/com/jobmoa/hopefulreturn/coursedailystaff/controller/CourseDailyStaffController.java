package com.jobmoa.hopefulreturn.coursedailystaff.controller;

import com.jobmoa.hopefulreturn.common.ApiResponse;
import com.jobmoa.hopefulreturn.coursedailystaff.model.dto.CourseDailyStaffCandidateResponse;
import com.jobmoa.hopefulreturn.coursedailystaff.model.dto.CourseDailyStaffListResponse;
import com.jobmoa.hopefulreturn.coursedailystaff.model.dto.SaveCourseDailyStaffRequest;
import com.jobmoa.hopefulreturn.coursedailystaff.model.dto.SaveCourseDailyStaffResponse;
import com.jobmoa.hopefulreturn.coursedailystaff.service.CourseDailyStaffService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "CourseDailyStaff")
@RestController
@RequestMapping("/api/course-daily-staffs")
@RequiredArgsConstructor
public class CourseDailyStaffController {

    private final CourseDailyStaffService courseDailyStaffService;

    @Operation(summary = "회차 날짜별 인력 배정 조회",
            description = "회차의 날짜×역할×시간대 배정 목록. 권한: 로그인 사용자")
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<CourseDailyStaffListResponse> findAll(
            @Parameter(description = "회차 ID") @RequestParam Long courseId) {
        return ApiResponse.success(courseDailyStaffService.findAll(courseId));
    }

    @Operation(summary = "회차 날짜별 인력 배정 저장",
            description = "그리드 단위 저장(회차 기존 배정 전량 교체). 권한: ADMIN, OPERATOR, REGIONAL_MANAGER")
    @PutMapping("/bulk")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'REGIONAL_MANAGER')")
    public ApiResponse<SaveCourseDailyStaffResponse> save(
            @Valid @RequestBody SaveCourseDailyStaffRequest request) {
        return ApiResponse.success(courseDailyStaffService.save(request));
    }

    @Operation(summary = "회차 날짜별 배정 후보 직원 조회",
            description = "회차 교육일 범위의 가용 스태프와 배정 가능 역할·시간대. "
                    + "권한: ADMIN, OPERATOR, REGIONAL_MANAGER")
    @GetMapping("/candidates")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'REGIONAL_MANAGER')")
    public ApiResponse<CourseDailyStaffCandidateResponse> findCandidates(
            @Parameter(description = "회차 ID") @RequestParam Long courseId) {
        return ApiResponse.success(courseDailyStaffService.findCandidates(courseId));
    }
}
