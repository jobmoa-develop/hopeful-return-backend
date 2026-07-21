package com.jobmoa.hopefulreturn.dashboard.controller;

import com.jobmoa.hopefulreturn.common.ApiResponse;
import com.jobmoa.hopefulreturn.dashboard.model.dto.DashboardCalendarResponse;
import com.jobmoa.hopefulreturn.dashboard.model.dto.DashboardRegionStatsResponse;
import com.jobmoa.hopefulreturn.dashboard.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Dashboard")
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @Operation(summary = "지역별 회차 현황", description = "권한: 로그인 사용자")
    @GetMapping("/region-stats")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<DashboardRegionStatsResponse> getRegionStats() {
        return ApiResponse.success(dashboardService.getRegionStats());
    }

    @Operation(summary = "캘린더용 월별 일정(모집 시작/마감·계획서 제출 + 경고)", description = "권한: 로그인 사용자")
    @GetMapping("/calendar")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<DashboardCalendarResponse> getCalendar(
            @Parameter(description = "조회 연도(미지정 시 오늘 기준)") @RequestParam(required = false) Integer year,
            @Parameter(description = "조회 월 1~12(미지정 시 오늘 기준)") @RequestParam(required = false) Integer month) {
        return ApiResponse.success(dashboardService.getCalendar(year, month));
    }
}