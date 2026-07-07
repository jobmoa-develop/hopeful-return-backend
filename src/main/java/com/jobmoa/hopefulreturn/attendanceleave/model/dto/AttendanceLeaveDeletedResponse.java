package com.jobmoa.hopefulreturn.attendanceleave.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "조퇴·외출 삭제 응답")
public record AttendanceLeaveDeletedResponse(
        @Schema(description = "삭제 성공 여부", example = "true")
        boolean deleted
) {
}
