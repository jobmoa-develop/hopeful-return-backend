package com.jobmoa.hopefulreturn.coursestaffsms.controller;

import com.jobmoa.hopefulreturn.common.ApiResponse;
import com.jobmoa.hopefulreturn.coursestaffsms.model.dto.CourseStaffSmsPageResponse;
import com.jobmoa.hopefulreturn.coursestaffsms.service.CourseStaffSmsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "CourseStaffSms")
@RestController
@RequestMapping("/api/course-staff-sms")
@RequiredArgsConstructor
public class CourseStaffSmsController {

    private final CourseStaffSmsService courseStaffSmsService;

    @Operation(summary = "강좌 담당자 안내 문자 발송 내역 조회(전역·페이지)",
            description = "필터(수신자/상태/종류/지역/회차/기간)+페이지네이션. "
                    + "ADMIN·HEAD_OFFICE 는 전체, 그 외 계정은 본인이 트리거한 발송분만. "
                    + "권한: ADMIN, HEAD_OFFICE, REGIONAL_MANAGER")
    @GetMapping("/history")
    @PreAuthorize("hasAnyRole('ADMIN', 'HEAD_OFFICE', 'REGIONAL_MANAGER')")
    public ApiResponse<CourseStaffSmsPageResponse> history(
            @RequestAttribute("userId") Long userId,
            Authentication authentication,
            @Parameter(description = "수신자명/전화 검색어") @RequestParam(required = false) String keyword,
            @Parameter(description = "알림 종류(STATUS_CHANGE/SCHEDULE_CHANGE)")
            @RequestParam(required = false) String notifyType,
            @Parameter(description = "발송 상태(SUCCESS/FAIL)") @RequestParam(required = false) String sendStatus,
            @Parameter(description = "전체회차(course_number)") @RequestParam(required = false) Integer courseNumber,
            @Parameter(description = "지역회차(local_course_number)") @RequestParam(required = false) Integer localCourseNumber,
            @Parameter(description = "지역 ID(하위 지역)") @RequestParam(required = false) Long regionId,
            @Parameter(description = "상위 지역 ID(해당 상위지역의 모든 하위지역 포함 조회)")
            @RequestParam(required = false) Long parentRegionId,
            @Parameter(description = "발송일 시작(YYYY-MM-DD)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate sentDateFrom,
            @Parameter(description = "발송일 종료(YYYY-MM-DD)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate sentDateTo,
            @Parameter(description = "페이지 번호(0-base)") @RequestParam(required = false) Integer page,
            @Parameter(description = "페이지 크기(≤100)") @RequestParam(required = false) Integer size) {
        boolean seeAll = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a.equals("ROLE_ADMIN") || a.equals("ROLE_HEAD_OFFICE"));
        Long effectiveSentBy = seeAll ? null : userId;
        return ApiResponse.success(courseStaffSmsService.findHistoryPage(
                effectiveSentBy, notifyType, sendStatus, courseNumber, localCourseNumber,
                regionId, parentRegionId, sentDateFrom, sentDateTo, keyword, page, size));
    }
}