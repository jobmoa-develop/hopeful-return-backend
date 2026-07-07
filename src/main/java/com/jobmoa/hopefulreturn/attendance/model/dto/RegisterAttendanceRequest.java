package com.jobmoa.hopefulreturn.attendance.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalTime;

@Schema(description = "출석 등록 요청")
public record RegisterAttendanceRequest(
        @Schema(description = "수강 정보 ID", example = "15")
        @NotNull
        Long courseParticipantId,

        @Schema(description = "수업 차수(일차)", example = "1")
        @NotNull
        Integer dayNo,

        @Schema(description = "입실 시각", example = "08:55:23")
        LocalTime checkInTime,

        @Schema(description = "퇴실 시각", example = "18:02:10")
        LocalTime checkOutTime,

        @Schema(description = "출결 상태(ATTEND/LATE/ABSENT)", example = "ATTEND")
        @NotBlank
        String status
) {
}
