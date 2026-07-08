package com.jobmoa.hopefulreturn.staffschedule.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "스태프 일정 삭제 응답")
public record StaffScheduleDeletedResponse(
        @Schema(description = "삭제 성공 여부", example = "true")
        boolean deleted
) {
}
