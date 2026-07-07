package com.jobmoa.hopefulreturn.attendance.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "출석 삭제 응답")
public record AttendanceDeletedResponse(
        @Schema(description = "삭제 성공 여부", example = "true")
        boolean deleted
) {
}
