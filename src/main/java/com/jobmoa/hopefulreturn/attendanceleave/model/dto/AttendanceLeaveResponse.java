package com.jobmoa.hopefulreturn.attendanceleave.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Schema(description = "조퇴·외출 등록 응답")
public record AttendanceLeaveResponse(
        @Schema(description = "조퇴·외출 ID", example = "5")
        Long attendanceLeaveId,

        @Schema(description = "출석 ID", example = "31")
        Long attendanceId,

        @Schema(description = "외출(조퇴) 시각", example = "14:30:00")
        LocalTime leaveTime,

        @Schema(description = "복귀 시각", example = "15:20:00")
        LocalTime returnTime,

        @Schema(description = "사유", example = "병원 진료")
        String reason,

        @Schema(description = "생성 시각", example = "2026-08-18T14:31:02")
        LocalDateTime createdAt
) {
}
