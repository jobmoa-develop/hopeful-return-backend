package com.jobmoa.hopefulreturn.counselingschedule.controller;

import com.jobmoa.hopefulreturn.common.ApiResponse;
import com.jobmoa.hopefulreturn.counselingschedule.model.dto.CounselingScheduleResponse;
import com.jobmoa.hopefulreturn.counselingschedule.service.CounselingScheduleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "CounselingSchedule")
@RestController
@RequestMapping("/api/counseling-schedules")
@RequiredArgsConstructor
public class CounselingScheduleController {

    private final CounselingScheduleService counselingScheduleService;

    @Operation(summary = "상담 일정 조회",
            description = "상담 시작일 기준으로 전 회차·전 상담사의 상담 일정을 조회한다(스코프 미적용). "
                    + "권한: ADMIN, OPERATOR, COUNSELOR")
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'COUNSELOR')")
    public ApiResponse<CounselingScheduleResponse> findSchedules(
            @Parameter(description = "조회 시작일(YYYY-MM-DD, 포함)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @Parameter(description = "조회 종료일(YYYY-MM-DD, 포함)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @Parameter(description = "지역 필터 — 하위 지역(양천) 단일")
            @RequestParam(required = false) Long regionId,
            @Parameter(description = "상위 지역 필터 — 상위 지역(서울) 산하 하위 지역 전체")
            @RequestParam(required = false) Long parentRegionId,
            @Parameter(description = "전체회차(course_number) — 전체 지역 조회 시 사용")
            @RequestParam(required = false) Integer courseNumber,
            @Parameter(description = "지역회차(local_course_number) — 지역 선택 조회 시 사용")
            @RequestParam(required = false) Integer localCourseNumber,
            @Parameter(description = "상담사명 부분일치 필터")
            @RequestParam(required = false) String counselorName) {
        return ApiResponse.success(counselingScheduleService.findSchedules(
                from, to, regionId, parentRegionId, courseNumber, localCourseNumber, counselorName));
    }
}
