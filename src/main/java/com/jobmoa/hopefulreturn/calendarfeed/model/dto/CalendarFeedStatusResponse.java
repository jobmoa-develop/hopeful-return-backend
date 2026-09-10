package com.jobmoa.hopefulreturn.calendarfeed.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "내 일정 ICS 구독 피드 상태")
public record CalendarFeedStatusResponse(
        @Schema(description = "연동 여부(토큰 발급됨)", example = "true")
        boolean enabled,

        @Schema(description = "구독 피드 URL. 미연동이면 null", example = "http://localhost:3434/api/calendar/feed/abc123.ics")
        String feedUrl
) {
}
