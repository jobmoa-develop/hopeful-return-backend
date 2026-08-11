package com.jobmoa.hopefulreturn.course.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "강좌 일정/장소 변경 안내 문자 발송 응답")
public record NotifyCourseChangeResponse(
        @Schema(description = "실제 발송된 인원 수", example = "2")
        int notifiedCount
) {
}