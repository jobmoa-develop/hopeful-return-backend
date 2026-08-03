package com.jobmoa.hopefulreturn.dashboard.controller;

import com.jobmoa.hopefulreturn.common.ApiResponse;
import com.jobmoa.hopefulreturn.dashboard.model.dto.DashboardCalendarResponse;
import com.jobmoa.hopefulreturn.dashboard.model.dto.DashboardRegionStatsResponse;
import com.jobmoa.hopefulreturn.dashboard.model.dto.DashboardTaskCompletionRequest;
import com.jobmoa.hopefulreturn.dashboard.model.dto.DashboardTaskCompletionResponse;
import com.jobmoa.hopefulreturn.dashboard.service.DashboardService;
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
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
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

    @Operation(summary = "대시보드 일정 체크 목록 조회(전역 공유)",
            description = "체크 표시는 대시보드 접근 권한이 있는 모든 사용자에게 동일하게 보인다. 권한: 로그인 사용자")
    @GetMapping("/task-completions")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<DashboardTaskCompletionResponse> getTaskCompletions() {
        return ApiResponse.success(dashboardService.getTaskCompletions());
    }

    @Operation(summary = "대시보드 일정 체크 표시(전역 공유)",
            description = "이미 다른 사용자가 체크했다면 그대로 유지된다(idempotent). 권한: 로그인 사용자")
    @PostMapping("/task-completions")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Void> completeTask(
            @RequestAttribute(name = "userId") Long userId,
            @Valid @RequestBody DashboardTaskCompletionRequest request) {
        dashboardService.completeTask(userId, request.taskId());
        return ApiResponse.success(null);
    }

    @Operation(summary = "대시보드 일정 체크 해제(전역 공유)",
            description = "누가 체크했는지와 무관하게 해제 가능하다. 권한: 로그인 사용자")
    @DeleteMapping("/task-completions/{taskId}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Void> uncompleteTask(@PathVariable String taskId) {
        dashboardService.uncompleteTask(taskId);
        return ApiResponse.success(null);
    }
}