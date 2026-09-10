package com.jobmoa.hopefulreturn.calendarfeed.controller;

import com.jobmoa.hopefulreturn.calendarfeed.model.dto.CalendarFeedStatusResponse;
import com.jobmoa.hopefulreturn.calendarfeed.service.CalendarFeedService;
import com.jobmoa.hopefulreturn.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "CalendarFeed")
@RestController
@RequestMapping("/api/calendar/feed")
@RequiredArgsConstructor
public class CalendarFeedController {

    private final CalendarFeedService calendarFeedService;

    @Operation(summary = "내 일정 ICS 구독 피드(공개)",
            description = "비밀 토큰 URL 로 Google Calendar 등 외부 캘린더가 익명 GET 한다. "
                    + "토큰이 유효하지 않으면 404. 인증 불필요(토큰이 본인 식별).")
    @GetMapping("/{token}.ics")
    public ResponseEntity<String> feed(@PathVariable String token) {
        return calendarFeedService.buildIcsFeed(token)
                .map(ics -> ResponseEntity.ok()
                        .contentType(new MediaType("text", "calendar", StandardCharsets.UTF_8))
                        .body(ics))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @Operation(summary = "내 일정 연동 상태 조회", description = "현재 사용자의 ICS 피드 연동 여부와 URL")
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<CalendarFeedStatusResponse> status(@RequestAttribute("userId") Long userId) {
        return ApiResponse.success(calendarFeedService.getStatus(userId));
    }

    @Operation(summary = "내 일정 연동(토큰 발급/재발급)",
            description = "피드 URL 을 발급해 연동을 활성화한다. 재호출 시 토큰 재발급(기존 URL 무효화).")
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<CalendarFeedStatusResponse> enable(@RequestAttribute("userId") Long userId) {
        return ApiResponse.success(calendarFeedService.enable(userId));
    }

    @Operation(summary = "내 일정 연동 해제", description = "토큰을 제거해 구독 URL 을 무효화한다.")
    @DeleteMapping
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<CalendarFeedStatusResponse> disable(@RequestAttribute("userId") Long userId) {
        calendarFeedService.disable(userId);
        return ApiResponse.success(calendarFeedService.getStatus(userId));
    }
}
