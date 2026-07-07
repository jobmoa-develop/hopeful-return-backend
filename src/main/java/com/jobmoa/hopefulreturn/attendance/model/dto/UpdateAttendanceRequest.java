package com.jobmoa.hopefulreturn.attendance.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalTime;

@Schema(description = "출석 수정 요청")
public record UpdateAttendanceRequest(
        @Schema(description = "입실 시각", example = "09:03:10")
        LocalTime checkInTime,

        @Schema(description = "퇴실 시각", example = "18:00:00")
        LocalTime checkOutTime,

        @Schema(description = "출결 상태(ATTEND/LATE/ABSENT)", example = "LATE")
        @NotBlank
        String status
) {
}
