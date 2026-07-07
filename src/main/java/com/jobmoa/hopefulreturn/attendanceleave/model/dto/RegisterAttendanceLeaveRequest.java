package com.jobmoa.hopefulreturn.attendanceleave.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalTime;

@Schema(description = "조퇴·외출 등록 요청")
public record RegisterAttendanceLeaveRequest(
        @Schema(description = "출석 ID", example = "31")
        @NotNull
        Long attendanceId,

        @Schema(description = "외출(조퇴) 시각", example = "14:30:00")
        LocalTime leaveTime,

        @Schema(description = "복귀 시각", example = "15:20:00")
        LocalTime returnTime,

        @Schema(description = "사유", example = "병원 진료")
        @Size(max = 255)
        String reason
) {
}
