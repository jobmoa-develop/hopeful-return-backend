package com.jobmoa.hopefulreturn.attendance.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Schema(description = "출석 등록 응답")
public record AttendanceResponse(
        @Schema(description = "출석 ID", example = "31")
        Long attendanceId,

        @Schema(description = "수강 정보 ID", example = "15")
        Long courseParticipantId,

        @Schema(description = "수업 차수(일차)", example = "1")
        Integer dayNo,

        @Schema(description = "입실 시각", example = "08:55:23")
        LocalTime checkInTime,

        @Schema(description = "퇴실 시각", example = "18:02:10")
        LocalTime checkOutTime,

        @Schema(description = "출결 상태", example = "ATTEND")
        String status,

        @Schema(description = "결석 사유(결석이 아니면 null)", example = "개인 사정")
        String absenceReason,

        @Schema(description = "생성 시각", example = "2026-08-18T09:00:01")
        LocalDateTime createdAt
) {
}
